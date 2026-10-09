package com.example.moodymusicforandroid.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.manager.ArtistManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.*
import com.example.moodymusicforandroid.ui.home.voice.TopBarTitleState
import com.example.moodymusicforandroid.ui.home.voice.VoiceAiManager
import com.example.moodymusicforandroid.ui.home.voice.VoiceRecordingManager
import com.example.moodymusicforandroid.ui.home.voice.VoiceProcessException
import com.example.moodymusicforandroid.ui.home.voice.VoiceDispatchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 全局播放状态 ViewModel，在 MainActivity 级别持有，所有页面可复用。
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val _playState = MutableStateFlow(
        MusicPlayState(playMode = PlayMode.fromString(UserManager.getCurrentPlayMode()))
    )
    val playState: StateFlow<MusicPlayState> = _playState.asStateFlow()

    private var musicService: MusicPlayerService? = null
    private var isBound = false

    // ── 随机漫游引擎状态 ─────────────────────────────────────
    /** 艺人详情内存缓存（加速漫游连续抽歌） */
    private val cachedArtistDetails = mutableMapOf<String, ArtistWithAlbums>()
    /** 最近播放过的歌曲 audioUrl，用于去重窗口（保留最近 50 首） */
    private val recentRoamUrls = ArrayDeque<String>(50)
    /** 漫游模式是否正在后台追加队列 */
    private var isRoamAppending = false

    // ── 语音点歌与控制状态（默认常驻开启） ─────────────────────────────────────
    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _topBarTitleState = MutableStateFlow<TopBarTitleState>(TopBarTitleState.Default)
    val topBarTitleState: StateFlow<TopBarTitleState> = _topBarTitleState.asStateFlow()

    private var voiceAiManager: VoiceAiManager? = null
    private var voiceRecordingManager: VoiceRecordingManager? = null
    private var titleResetJob: Job? = null
    private var favBatchPlayJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? MusicPlayerService.PlayerBinder
            musicService = b?.getService()
            isBound = true
            // 同步当前状态
            musicService?.let { service ->
                val serviceState = service.getCurrentState()
                if (serviceState.songTitle.isNotBlank()) {
                    val wasRoaming = _playState.value.isRoamingMode
                    if (_playState.value.isPlaying && _playState.value.songTitle == serviceState.songTitle) {
                        _playState.value = serviceState.copy(isPlaying = true, isRoamingMode = wasRoaming)
                    } else {
                        _playState.value = serviceState.copy(isRoamingMode = wasRoaming)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isBound = false
        }
    }

    init {
        EventBusManager.register(this)
        bindService()
    }

    private fun bindService() {
        val intent = Intent(getApplication(), MusicPlayerService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPlayStateChanged(event: BaseEvent) {
        if (event.eventType == EventType.MUSIC_PLAY_STATE_CHANGED) {
            val state = event.eventData as? MusicPlayState
            state?.let { incoming ->
                // 当底层广播表明播放已彻底停止（歌名为空且队列为空）
                if (incoming.songTitle.isBlank() && incoming.queue.isEmpty()) {
                    favBatchPlayJob?.cancel()
                    isRoamAppending = false
                    _playState.value = incoming.copy(
                        isRoamingMode = false,
                        isRoamingLoading = false,
                        capsuleListeningMode = CapsuleListeningMode.NONE
                    )
                    return
                }

                val prevIdx = _playState.value.playlistIndex
                val wasRoaming = _playState.value.isRoamingMode
                val wasLoading = _playState.value.isRoamingLoading
                val prevListeningMode = _playState.value.capsuleListeningMode
                _playState.value = incoming.copy(
                    isRoamingMode = wasRoaming,
                    isRoamingLoading = wasLoading,
                    capsuleListeningMode = prevListeningMode
                )

                // 漫游模式下，切歌自动检查储备队列
                if (wasRoaming && incoming.playlistIndex != prevIdx) {
                    checkAndRefillRoamingQueue()
                }
            }
        }
    }

    /**
     * 通用播放队列方法：接收已构建好的 PlayQueueItem 列表，支持从 startIndex 开始播放。
     * 自动跳过无效音频，乐观更新底栏与播放状态，并启动 MusicPlayerService。
     */
    fun playQueue(
        queue: List<PlayQueueItem>,
        startIndex: Int = 0,
        listeningMode: CapsuleListeningMode = CapsuleListeningMode.NONE,
        initialSeekMs: Int = 0
    ) {
        if (queue.isEmpty()) return
        // 关键防护：一旦用户手动切歌或启动非快捷批量模式播放，立刻阻断上一次批量异步追加，避免新旧歌单串流踩踏
        if (listeningMode != CapsuleListeningMode.FAVORITE_ALBUMS && listeningMode != CapsuleListeningMode.FOLLOWED_ARTISTS) {
            favBatchPlayJob?.cancel()
        }
        if (listeningMode != CapsuleListeningMode.ROAMING) {
            isRoamAppending = false
        }
        val ctx = getApplication<Application>()
        val playlist = ArrayList(queue)

        var targetIndex = startIndex.coerceIn(0, playlist.size - 1)
        if (targetIndex in playlist.indices && playlist[targetIndex].audioUrl.isBlank()) {
            val firstPlayable = playlist.indexOfFirst { it.audioUrl.isNotBlank() }
            if (firstPlayable != -1) {
                targetIndex = firstPlayable
            } else {
                Toast.makeText(ctx, "列表中暂无可播放的有效音频", Toast.LENGTH_SHORT).show()
                return
            }
        }

        isRoamAppending = false

        if (targetIndex in playlist.indices) {
            val current = playlist[targetIndex]
            _playState.value = MusicPlayState(
                songTitle = current.songTitle,
                artistName = current.artistName,
                albumTitle = current.albumTitle,
                coverUrl = current.coverUrl,
                audioUrl = current.audioUrl,
                lrcPath = current.lrcPath,
                isPlaying = true,
                duration = 0,
                position = initialSeekMs,
                playlistIndex = targetIndex,
                playMode = _playState.value.playMode,
                queue = playlist,
                isRoamingMode = (listeningMode == CapsuleListeningMode.ROAMING),
                capsuleListeningMode = listeningMode
            )
        }

        val intent = Intent(ctx, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_PLAYLIST, playlist)
            putExtra(MusicPlayerService.EXTRA_INDEX, targetIndex)
            putExtra(MusicPlayerService.EXTRA_SEEK_POSITION, initialSeekMs)
        }
        ctx.startForegroundService(intent)
        if (isBound && musicService != null) {
            musicService?.playPlaylist(playlist, targetIndex, initialSeekMs)
        }
    }

    /**
     * 播放指定歌单（从第 index 首开始）
     */
    fun play(songs: List<SongItem>, index: Int, artistName: String, albumTitle: String, coverUrl: String) {
        val playlist = songs.map { song ->
            PlayQueueItem(
                songTitle = song.title,
                artistName = artistName,
                albumTitle = albumTitle,
                coverUrl = coverUrl,
                audioUrl = buildAudioUrl(song.path),
                lrcPath = song.lrcPath
            )
        }
        playQueue(playlist, index)
    }

    /**
     * 播放手札/自建歌单（支持每首曲目拥有独立的歌手名、专辑名与封面）
     */
    fun playPlaylistSongs(songs: List<com.example.moodymusicforandroid.data.local.db.PlaylistSongEntity>, index: Int, playlistName: String) {
        if (songs.isEmpty()) return
        val playlist = songs.map { song ->
            PlayQueueItem(
                songTitle = song.title,
                artistName = song.artistName?.takeIf { it.isNotBlank() } ?: "未知歌手",
                albumTitle = song.albumTitle?.takeIf { it.isNotBlank() } ?: playlistName,
                coverUrl = song.coverUrl ?: "",
                audioUrl = buildAudioUrl(song.filePath),
                lrcPath = null
            )
        }
        playQueue(playlist, index)
    }

    /**
     * 直接播放一个指定 URL 的单曲（首页主题曲、特色单曲等场景）
     */
    fun playSingleUrl(
        audioUrl: String,
        songTitle: String,
        artistName: String = "MoodyMusic",
        albumTitle: String = "主题精选",
        coverUrl: String = "",
        lrcPath: String? = null,
        initialSeekMs: Int = 0
    ) {
        val item = PlayQueueItem(
            songTitle = songTitle,
            artistName = artistName,
            albumTitle = albumTitle,
            coverUrl = coverUrl,
            audioUrl = buildAudioUrl(audioUrl),
            lrcPath = lrcPath
        )
        playQueue(listOf(item), 0, initialSeekMs = initialSeekMs)
    }

    /**
     * 播放语音点歌/智能调度生成的队列
     */
    fun playVoiceResult(result: VoiceDispatchResult) {
        if (result.playlist.isEmpty()) return
        playQueue(result.playlist, result.startIndex)
    }

    /**
     * 播放用户收藏的所有单曲（方案A：首批 50 首极速秒播 + 后台分批流式静默追加，零 Binder 1MB 溢出风险）
     */
    fun playFavoriteSongs() {
        val songs = UserManager.favoriteSongsList.value
        if (songs.isEmpty()) {
            Toast.makeText(getApplication(), "暂无收藏的单曲，快去收藏吧", Toast.LENGTH_SHORT).show()
            return
        }

        val queueItems = songs.mapNotNull { song ->
            val url = buildAudioUrl(song.filePath)
            if (url.isNotBlank()) {
                PlayQueueItem(
                    songTitle = song.title,
                    artistName = song.artistName?.takeIf { it.isNotBlank() } ?: "未知歌手",
                    albumTitle = song.albumTitle?.takeIf { it.isNotBlank() } ?: "收藏歌曲",
                    coverUrl = song.coverUrl?.takeIf { it.isNotBlank() } ?: "",
                    audioUrl = url,
                    lrcPath = null
                )
            } else null
        }

        if (queueItems.isEmpty()) {
            Toast.makeText(getApplication(), "收藏单曲中暂无可播放的音频文件", Toast.LENGTH_SHORT).show()
            return
        }

        favBatchPlayJob?.cancel()
        // 首批 50 首即刻起播，彻底杜绝 Binder 1MB 事务溢出风险
        val initialBatch = queueItems.take(50)
        playQueue(initialBatch, 0, CapsuleListeningMode.FAVORITE_SONGS)
        Toast.makeText(getApplication(), "▶ 开始播放收藏单曲 (共 ${queueItems.size} 首)", Toast.LENGTH_SHORT).show()

        if (queueItems.size > 50) {
            favBatchPlayJob = viewModelScope.launch(Dispatchers.IO) {
                val remaining = queueItems.drop(50)
                for (chunk in remaining.chunked(50)) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_SONGS) break
                    delay(250)
                    withContext(Dispatchers.Main) {
                        if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_SONGS) return@withContext
                        appendTracksToQueue(chunk)
                    }
                }
            }
        }
    }

    /**
     * 播放用户收藏的所有专辑（方案A：首张专辑极速秒播 + 后台按序流式静默追加，零 Binder 溢出，零高并发）
     */
    fun playFavoriteAlbums() {
        val albums = UserManager.favoriteAlbumsList.value
        if (albums.isEmpty()) {
            Toast.makeText(getApplication(), "暂无收藏的专辑，快去收藏吧", Toast.LENGTH_SHORT).show()
            return
        }

        favBatchPlayJob?.cancel()
        _playState.value = _playState.value.copy(
            capsuleListeningMode = CapsuleListeningMode.FAVORITE_ALBUMS
        )

        favBatchPlayJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 极速首播阶段：按序寻找第一张包含有效歌曲的专辑，50ms 级秒开起播
                var firstPlayableIndex = -1
                var initialTracks: List<PlayQueueItem> = emptyList()

                for (i in albums.indices) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_ALBUMS) return@launch
                    val tracks = fetchSongsForAlbum(albums[i])
                    if (tracks.isNotEmpty()) {
                        firstPlayableIndex = i
                        initialTracks = tracks
                        break
                    }
                }

                if (firstPlayableIndex == -1 || initialTracks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "收藏专辑中暂无可播放的曲目", Toast.LENGTH_SHORT).show()
                        _playState.value = _playState.value.copy(capsuleListeningMode = CapsuleListeningMode.NONE)
                    }
                    return@launch
                }

                // 立即在主线程起播第一张专辑！
                withContext(Dispatchers.Main) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_ALBUMS) return@withContext
                    playQueue(initialTracks, 0, CapsuleListeningMode.FAVORITE_ALBUMS)
                    val albumTitle = albums[firstPlayableIndex].title ?: "收藏专辑"
                    Toast.makeText(getApplication(), "▶ 正在播放《$albumTitle》(${initialTracks.size} 首)", Toast.LENGTH_SHORT).show()
                }

                // 2. 后台流式追加阶段：平滑按序拉取剩余专辑，低并发、防穿透、永不超 1MB Binder 限制
                for (i in (firstPlayableIndex + 1) until albums.size) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_ALBUMS) break
                    delay(350)
                    val nextTracks = fetchSongsForAlbum(albums[i])
                    if (nextTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FAVORITE_ALBUMS) return@withContext
                            appendTracksToQueue(nextTracks)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "加载专辑曲目异常，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 后台抓取单张专辑的所有可播曲目
     */
    private suspend fun fetchSongsForAlbum(album: LibraryAlbumItem): List<PlayQueueItem> {
        val albumTitle = album.title.orEmpty().trim()
        val artistId = album.artistId.orEmpty().trim()
        val albumCover = album.cover.orEmpty()

        if (artistId.isNotBlank()) {
            val artistDetail = fetchArtistDetail(artistId)
            if (artistDetail != null) {
                val matched = artistDetail.albums.firstOrNull {
                    it.title.trim().equals(albumTitle, ignoreCase = true)
                }
                if (matched != null && matched.songs.isNotEmpty()) {
                    val cover = if (matched.cover.isNotBlank()) buildAudioUrl(matched.cover) else buildAudioUrl(albumCover)
                    val artistName = artistDetail.name.ifBlank { "未知歌手" }
                    return matched.songs.mapNotNull { song ->
                        val url = buildAudioUrl(song.path)
                        if (url.isNotBlank()) {
                            PlayQueueItem(
                                songTitle = song.title,
                                artistName = artistName,
                                albumTitle = matched.title.ifBlank { albumTitle },
                                coverUrl = cover,
                                audioUrl = url,
                                lrcPath = song.lrcPath
                            )
                        } else null
                    }
                }
            }
        }

        return try {
            val resp = MoodyApiProvider.apiService.getSongsByArtist(
                artistId = artistId.takeIf { it.isNotBlank() },
                album = albumTitle.takeIf { it.isNotBlank() }
            )
            val matchedArtist = resp.data?.firstOrNull()
            val matchedAlbum = matchedArtist?.albums?.firstOrNull()
            val songs = matchedAlbum?.songs ?: emptyList()
            val artistName = matchedArtist?.name?.ifBlank { "未知歌手" } ?: "未知歌手"
            val cover = if (!matchedAlbum?.cover.isNullOrBlank()) buildAudioUrl(matchedAlbum!!.cover) else buildAudioUrl(albumCover)

            songs.mapNotNull { song ->
                val url = buildAudioUrl(song.path)
                if (url.isNotBlank()) {
                    PlayQueueItem(
                        songTitle = song.title,
                        artistName = artistName,
                        albumTitle = matchedAlbum?.title ?: albumTitle,
                        coverUrl = cover,
                        audioUrl = url,
                        lrcPath = song.lrcPath
                    )
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * 播放关注的歌手所有专辑曲目（方案A：首位歌手极速秒播 + 后台按序流式静默追加，零 Binder 溢出，零高并发）
     */
    fun playFollowedArtists() {
        val artists = UserManager.followedArtistsList.value
        if (artists.isEmpty()) {
            Toast.makeText(getApplication(), "暂无关注的歌手，快去关注吧", Toast.LENGTH_SHORT).show()
            return
        }

        favBatchPlayJob?.cancel()
        _playState.value = _playState.value.copy(
            capsuleListeningMode = CapsuleListeningMode.FOLLOWED_ARTISTS
        )

        favBatchPlayJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 极速首播阶段：按序寻找第一位包含有效歌曲的歌手，50ms 级秒开起播
                var firstPlayableIndex = -1
                var initialTracks: List<PlayQueueItem> = emptyList()

                for (i in artists.indices) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FOLLOWED_ARTISTS) return@launch
                    val tracks = fetchSongsForArtist(artists[i])
                    if (tracks.isNotEmpty()) {
                        firstPlayableIndex = i
                        initialTracks = tracks
                        break
                    }
                }

                if (firstPlayableIndex == -1 || initialTracks.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "关注歌手暂无可播放的曲目", Toast.LENGTH_SHORT).show()
                        _playState.value = _playState.value.copy(capsuleListeningMode = CapsuleListeningMode.NONE)
                    }
                    return@launch
                }

                // 立即在主线程起播第一位歌手！
                withContext(Dispatchers.Main) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FOLLOWED_ARTISTS) return@withContext
                    playQueue(initialTracks, 0, CapsuleListeningMode.FOLLOWED_ARTISTS)
                    val artistName = artists[firstPlayableIndex].name ?: "关注歌手"
                    Toast.makeText(getApplication(), "▶ 正在播放：$artistName (${initialTracks.size} 首)", Toast.LENGTH_SHORT).show()
                }

                // 2. 后台流式追加阶段：平滑按序拉取剩余歌手，低并发、防穿透、永不超 1MB Binder 限制
                for (i in (firstPlayableIndex + 1) until artists.size) {
                    if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FOLLOWED_ARTISTS) break
                    delay(400)
                    val nextTracks = fetchSongsForArtist(artists[i])
                    if (nextTracks.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            if (!isActive || _playState.value.capsuleListeningMode != CapsuleListeningMode.FOLLOWED_ARTISTS) return@withContext
                            appendTracksToQueue(nextTracks)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) return@launch
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "加载歌手曲目异常，请重试", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 后台抓取某位关注艺人的所有专辑曲目
     */
    private suspend fun fetchSongsForArtist(artist: LibraryArtistItem): List<PlayQueueItem> {
        val artistId = artist.artistId.trim()
        if (artistId.isBlank()) return emptyList()

        val detail = fetchArtistDetail(artistId) ?: return emptyList()
        val artistName = detail.name.ifBlank { artist.name?.ifBlank { "未知歌手" } ?: "未知歌手" }
        val artistAvatar = detail.avatar?.takeIf { it.isNotBlank() } ?: (artist.avatar ?: "")

        val sortedAlbums = detail.albums.sortedWith(
            compareByDescending<AlbumWithSongs> { album ->
                album.songs.any { !it.path.isNullOrBlank() }
            }.thenBy { album ->
                val yr = album.year.takeIf { it != "未知" && it.isNotBlank() } ?: "9999"
                yr
            }
        )

        val tracks = mutableListOf<PlayQueueItem>()
        for (album in sortedAlbums) {
            val albumCover = if (album.cover.isNotBlank()) buildAudioUrl(album.cover) else buildAudioUrl(artistAvatar)
            for (song in album.songs) {
                val path = song.path
                if (!path.isNullOrBlank()) {
                    val url = buildAudioUrl(path)
                    if (url.isNotBlank()) {
                        tracks.add(
                            PlayQueueItem(
                                songTitle = song.title,
                                artistName = artistName,
                                albumTitle = album.title.ifBlank { "单曲专辑" },
                                coverUrl = albumCover,
                                audioUrl = url,
                                lrcPath = song.lrcPath
                            )
                        )
                    }
                }
            }
        }
        return tracks
    }

    /**
     * 辅助方法：获取艺人详情，优先读内存缓存，未命中则请求网络
     */
    private suspend fun fetchArtistDetail(artistId: String): ArtistWithAlbums? {
        cachedArtistDetails[artistId]?.let { return it }
        return try {
            val resp = MoodyApiProvider.apiService.getArtistDetail(artistId)
            val d = resp.data?.firstOrNull()
            if (d != null) {
                cachedArtistDetails[artistId] = d
                if (cachedArtistDetails.size > 50) {
                    val firstKey = cachedArtistDetails.keys.first()
                    cachedArtistDetails.remove(firstKey)
                }
            }
            d
        } catch (_: Exception) {
            null
        }
    }

    fun togglePlayPause() {
        // 乐观更新：立即翻转 isPlaying，不等 Service 回调
        _playState.value = _playState.value.copy(isPlaying = !_playState.value.isPlaying)

        if (isBound) {
            musicService?.togglePlayPause()
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_TOGGLE
            }
            getApplication<Application>().startService(intent)
        }
    }

    /**
     * 主动暂停当前正在播放的音乐（例如用户开始语音输入时）
     */
    fun pause() {
        if (_playState.value.isPlaying) {
            _playState.value = _playState.value.copy(isPlaying = false)
            if (isBound) {
                musicService?.pausePlayback()
            } else {
                val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                    action = MusicPlayerService.ACTION_PAUSE
                }
                getApplication<Application>().startService(intent)
            }
        }
    }

    fun togglePlayMode(): PlayMode {
        val nextMode = _playState.value.playMode.next()
        UserManager.updatePlayMode(nextMode.name)
        val resMode = if (isBound) {
            musicService?.togglePlayMode() ?: nextMode
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_TOGGLE_MODE
            }
            getApplication<Application>().startService(intent)
            nextMode
        }
        _playState.value = _playState.value.copy(playMode = resMode)
        return resMode
    }

    fun playTrackInQueue(index: Int) {
        // 乐观更新：立即切换到目标曲目并标记为播放中
        val queue = _playState.value.queue
        if (index in queue.indices) {
            val target = queue[index]
            _playState.value = _playState.value.copy(
                songTitle     = target.songTitle,
                artistName    = target.artistName,
                albumTitle    = target.albumTitle,
                coverUrl      = target.coverUrl,
                audioUrl      = target.audioUrl,
                lrcPath       = target.lrcPath,
                isPlaying     = true,
                position      = 0,
                playlistIndex = index
            )
        }

        if (isBound) {
            musicService?.playIndex(index)
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PLAY_INDEX
                putExtra(MusicPlayerService.EXTRA_INDEX, index)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun removeFromQueue(index: Int) {
        if (isBound) {
            musicService?.removeItem(index)
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_REMOVE_INDEX
                putExtra(MusicPlayerService.EXTRA_INDEX, index)
            }
            getApplication<Application>().startService(intent)
        }
    }

    // ── 随机漫游公开 API ───────────────────────────────────────────

    /**
     * 全库真随机抽选一首有效曲目。
     * 直接从本地 Room 数据库中极速随机挑选艺人，拉取其专辑与歌曲，筛选出带有有效音频文件的歌曲。
     * 零堆内存常驻，不占用常驻大对象。
     */
    private suspend fun pickOneRandomSong(): PlayQueueItem? {
        val candidates = ArtistManager.getRandomArtists(15)
        if (candidates.isEmpty()) return null

        for (artist in candidates) {
            val detail = fetchArtistDetail(artist.id) ?: continue

            // 收集所有带可播音源的歌曲
            val playablePairs = mutableListOf<Pair<AlbumWithSongs, SongItem>>()
            for (album in detail.albums) {
                for (song in album.songs) {
                    val p = song.path
                    if (!p.isNullOrBlank()) {
                        val resolvedUrl = AppConfig.resolveStorageUrl(p)
                        if (resolvedUrl.isNotBlank() && !recentRoamUrls.contains(resolvedUrl)) {
                            playablePairs.add(album to song)
                        }
                    }
                }
            }

            if (playablePairs.isNotEmpty()) {
                val (album, song) = playablePairs.random()
                val resolvedUrl = AppConfig.resolveStorageUrl(song.path!!)

                // 去重窗口记录
                if (recentRoamUrls.size >= 50) recentRoamUrls.removeFirst()
                recentRoamUrls.addLast(resolvedUrl)

                val coverUrl = if (!album.cover.isNullOrBlank()) {
                    AppConfig.resolveStorageUrl(album.cover)
                } else ""

                return PlayQueueItem(
                    songTitle = song.title,
                    artistName = detail.name.ifBlank { artist.name },
                    albumTitle = album.title.ifBlank { "漫游精选" },
                    coverUrl = coverUrl,
                    audioUrl = resolvedUrl,
                    lrcPath = song.lrcPath
                )
            }
        }
        return null
    }

    /**
     * 启动随心漫游模式。
     * 秒级响应：先随机抽选 1 首立即起播，随后在后台静默预填 5 首歌到播放队列。
     */
    fun startRoamingMode() {
        Toast.makeText(getApplication(), "✨ 正在全库随心漫游，寻找好歌...", Toast.LENGTH_SHORT).show()
        // 乐观更新：立即将 isRoamingMode 标记为 true，UI 状态秒切高亮
        _playState.value = _playState.value.copy(
            isRoamingMode = true,
            capsuleListeningMode = CapsuleListeningMode.ROAMING
        )
        viewModelScope.launch {
            try {
                val firstTrack = pickOneRandomSong()
                if (firstTrack == null) {
                    Toast.makeText(getApplication(), "全库暂无可漫游曲目，请检查网络", Toast.LENGTH_SHORT).show()
                    _playState.value = _playState.value.copy(
                        isRoamingMode = false,
                        capsuleListeningMode = CapsuleListeningMode.NONE
                    )
                    return@launch
                }

                val initialQueue = arrayListOf(firstTrack)
                _playState.value = MusicPlayState(
                    songTitle = firstTrack.songTitle,
                    artistName = firstTrack.artistName,
                    albumTitle = firstTrack.albumTitle,
                    coverUrl = firstTrack.coverUrl,
                    audioUrl = firstTrack.audioUrl,
                    lrcPath = firstTrack.lrcPath,
                    isPlaying = true,
                    duration = 0,
                    position = 0,
                    playlistIndex = 0,
                    playMode = _playState.value.playMode,
                    queue = initialQueue,
                    isRoamingMode = true,
                    capsuleListeningMode = CapsuleListeningMode.ROAMING
                )

                val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                    action = MusicPlayerService.ACTION_PLAY
                    putExtra(MusicPlayerService.EXTRA_PLAYLIST, initialQueue)
                    putExtra(MusicPlayerService.EXTRA_INDEX, 0)
                }
                getApplication<Application>().startForegroundService(intent)

                // 立即在后台静默预填充队列至 7 首充裕储备
                fillRoamingQueue(targetRemaining = 7)
            } catch (e: Exception) {
                _playState.value = _playState.value.copy(
                    isRoamingMode = false,
                    capsuleListeningMode = CapsuleListeningMode.NONE
                )
                Toast.makeText(getApplication(), "漫游启动异常，请稍后重试", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 停止随机漫游模式。
     * @param destroyPlayer 是否同时销毁播放悬浮窗并完全停止播放
     */
    fun stopRoamingMode(destroyPlayer: Boolean = false) {
        isRoamAppending = false
        if (destroyPlayer) {
            stop()
        } else {
            _playState.value = _playState.value.copy(
                isRoamingMode = false,
                capsuleListeningMode = CapsuleListeningMode.NONE
            )
        }
    }

    /**
     * 检查漫游队列余量，当剩余待播 < 4 首时自动在后台提前补充，保持源源不断的充沛缓冲
     */
    fun checkAndRefillRoamingQueue() {
        val state = _playState.value
        if (!state.isRoamingMode) return
        val remaining = state.queue.size - state.playlistIndex - 1
        if (remaining < 4) {
            fillRoamingQueue(targetRemaining = 7)
        }
    }

    /**
     * 手动触发加载更多漫游歌曲（用户滑到底部或点击探索更多按钮时调用）
     * @param count 额外追加的歌曲数量，默认 5 首
     */
    fun loadMoreRoamingSongs(count: Int = 5) {
        val state = _playState.value
        if (!state.isRoamingMode || state.isRoamingLoading) return
        val currentRemaining = (state.queue.size - state.playlistIndex - 1).coerceAtLeast(0)
        fillRoamingQueue(targetRemaining = currentRemaining + count)
    }

    /**
     * 后台填充漫游队列，保持 targetRemaining 数量的待播歌曲
     */
    private fun fillRoamingQueue(targetRemaining: Int = 7) {
        if (isRoamAppending || !_playState.value.isRoamingMode) return
        isRoamAppending = true
        _playState.value = _playState.value.copy(isRoamingLoading = true)

        viewModelScope.launch {
            try {
                var remaining = _playState.value.queue.size - _playState.value.playlistIndex - 1
                while (_playState.value.isRoamingMode && remaining < targetRemaining) {
                    val nextSong = pickOneRandomSong() ?: break
                    // 添加到底层 Service 队列中
                    if (musicService != null) {
                        musicService?.addToQueue(nextSong)
                    } else {
                        val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                            action = MusicPlayerService.ACTION_ADD_TO_QUEUE
                            putExtra(MusicPlayerService.EXTRA_QUEUE_ITEM, nextSong)
                        }
                        getApplication<Application>().startService(intent)
                    }
                    remaining++
                    delay(150) // 错开微量时间，保障平滑追加
                }
            } catch (_: Exception) {
            } finally {
                isRoamAppending = false
                _playState.value = _playState.value.copy(isRoamingLoading = false)
            }
        }
    }

    /**
     * 将一首歌追加到当前播放队列末尾。
     *
     * 返回值：
     * - ADDED：成功追加
     * - DUPLICATE：已在队列中，未重复添加
     * - STARTED_NEW：原队列为空，已新建单曲播放会话
     */
    fun addToQueue(
        audioUrl: String,
        songTitle: String,
        artistName: String,
        albumTitle: String,
        coverUrl: String,
        lrcPath: String? = null
    ): AddToQueueResult {
        val item = PlayQueueItem(
            songTitle = songTitle,
            artistName = artistName,
            albumTitle = albumTitle,
            coverUrl = coverUrl,
            audioUrl = audioUrl,
            lrcPath = lrcPath
        )

        val result = if (isBound) {
            musicService?.addToQueue(item) ?: run {
                // Service 绑定但引用丢失，降级走 Intent
                sendAddToQueueIntent(item)
                // 本地推断结果
                if (_playState.value.queue.isEmpty()) AddToQueueResult.STARTED_NEW
                else if (_playState.value.queue.any { it.audioUrl == audioUrl }) AddToQueueResult.DUPLICATE
                else AddToQueueResult.ADDED
            }
        } else {
            // 未绑定：先本地判断，再发 Intent
            val localResult = when {
                _playState.value.queue.isEmpty() -> AddToQueueResult.STARTED_NEW
                _playState.value.queue.any { it.audioUrl == audioUrl } -> AddToQueueResult.DUPLICATE
                else -> AddToQueueResult.ADDED
            }
            sendAddToQueueIntent(item)
            localResult
        }

        // 乐观更新 _playState，让 UI 无需等 EventBus 回调即刻响应
        when (result) {
            AddToQueueResult.ADDED -> {
                val newQueue = _playState.value.queue + item
                _playState.value = _playState.value.copy(queue = newQueue)
            }
            AddToQueueResult.STARTED_NEW -> {
                _playState.value = MusicPlayState(
                    songTitle = songTitle,
                    artistName = artistName,
                    albumTitle = albumTitle,
                    coverUrl = coverUrl,
                    audioUrl = audioUrl,
                    lrcPath = lrcPath,
                    isPlaying = true,
                    duration = 0,
                    position = 0,
                    playlistIndex = 0,
                    playMode = _playState.value.playMode,
                    queue = listOf(item)
                )
            }
            AddToQueueResult.DUPLICATE -> { /* 不更新状态 */ }
        }

        return result
    }

    private fun sendAddToQueueIntent(item: PlayQueueItem) {
        val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_ADD_TO_QUEUE
            putExtra(MusicPlayerService.EXTRA_QUEUE_ITEM, item)
        }
        getApplication<Application>().startService(intent)
    }

    /**
     * 将一批曲目流式追加到当前播放队列末尾（静默无感预加载，自动去重，杜绝 IPC 1MB 溢出）
     */
    fun appendTracksToQueue(tracks: List<PlayQueueItem>): Int {
        if (tracks.isEmpty()) return 0
        val currentQueue = _playState.value.queue
        val existingUrls = currentQueue.map { it.audioUrl }.toHashSet()
        val deduped = tracks.filter { it.audioUrl.isNotBlank() && existingUrls.add(it.audioUrl) }
        if (deduped.isEmpty()) return 0

        if (isBound && musicService != null) {
            musicService?.addAllToQueue(deduped)
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_ADD_ALL_TO_QUEUE
                putExtra(MusicPlayerService.EXTRA_PLAYLIST, ArrayList(deduped))
            }
            getApplication<Application>().startService(intent)
        }

        val updatedQueue = currentQueue + deduped
        _playState.value = _playState.value.copy(queue = updatedQueue)
        return deduped.size
    }

    fun clearQueue() {
        stop()
    }

    fun playNext() {
        // 乐观更新：立即跳到队列下一首并标记为播放中
        val queue = _playState.value.queue
        if (queue.isNotEmpty()) {
            val currentIndex = _playState.value.playlistIndex
            val nextIndex = when (_playState.value.playMode) {
                PlayMode.SHUFFLE       -> queue.indices.random()
                PlayMode.SINGLE_LOOP   -> currentIndex
                else                   -> (currentIndex + 1) % queue.size
            }
            val next = queue[nextIndex]
            _playState.value = _playState.value.copy(
                songTitle     = next.songTitle,
                artistName    = next.artistName,
                albumTitle    = next.albumTitle,
                coverUrl      = next.coverUrl,
                audioUrl      = next.audioUrl,
                lrcPath       = next.lrcPath,
                isPlaying     = true,
                position      = 0,
                playlistIndex = nextIndex
            )
        }

        if (isBound) {
            musicService?.playNext()
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_NEXT
            }
            getApplication<Application>().startService(intent)
        }
        // 漫游模式：每次切歌后检查是否需要补充队列
        checkAndRefillRoamingQueue()
    }

    fun playPrevious() {
        // 乐观更新：立即跳到队列上一首并标记为播放中
        val queue = _playState.value.queue
        if (queue.isNotEmpty()) {
            val currentIndex = _playState.value.playlistIndex
            val prevIndex = when (_playState.value.playMode) {
                PlayMode.SHUFFLE     -> queue.indices.random()
                PlayMode.SINGLE_LOOP -> currentIndex
                else                 -> if (currentIndex > 0) currentIndex - 1 else queue.size - 1
            }
            val prev = queue[prevIndex]
            _playState.value = _playState.value.copy(
                songTitle     = prev.songTitle,
                artistName    = prev.artistName,
                albumTitle    = prev.albumTitle,
                coverUrl      = prev.coverUrl,
                audioUrl      = prev.audioUrl,
                lrcPath       = prev.lrcPath,
                isPlaying     = true,
                position      = 0,
                playlistIndex = prevIndex
            )
        }

        if (isBound) {
            musicService?.playPrevious()
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_PREVIOUS
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun seekTo(positionMs: Int) {
        _playState.value = _playState.value.copy(position = positionMs)
        if (isBound) {
            musicService?.seekTo(positionMs)
        } else {
            val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
                action = MusicPlayerService.ACTION_SEEK
                putExtra(MusicPlayerService.EXTRA_SEEK_POSITION, positionMs)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun stop() {
        favBatchPlayJob?.cancel()
        isRoamAppending = false
        _playState.value = MusicPlayState()
        if (isBound) {
            musicService?.stopPlayback()
        }
        val intent = Intent(getApplication(), MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
    }

    private fun buildAudioUrl(path: String?): String {
        return AppConfig.resolveStorageUrl(path)
    }

    // ── 语音输入与点歌控制 ─────────────────────────────────
    private fun getRecordingManager(): VoiceRecordingManager {
        return voiceRecordingManager ?: VoiceRecordingManager(getApplication()).also { voiceRecordingManager = it }
    }

    private fun getAiManager(): VoiceAiManager {
        return voiceAiManager ?: VoiceAiManager(getApplication()).also { voiceAiManager = it }
    }

    /** 切换右上角语音总开关 */
    fun toggleVoiceEnabled(): Boolean {
        val newState = !_isVoiceEnabled.value
        _isVoiceEnabled.value = newState
        if (!newState && _isVoiceListening.value) {
            cancelVoiceRecording()
        }
        return newState
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (!enabled && _isVoiceListening.value) {
            cancelVoiceRecording()
        }
    }

    /** 安排顶栏标题延迟复原到 Default */
    fun scheduleTitleReset(delayMs: Long = 4000L) {
        titleResetJob?.cancel()
        titleResetJob = viewModelScope.launch {
            delay(delayMs)
            _topBarTitleState.value = TopBarTitleState.Default
        }
    }

    /** 开始语音录音（长按触发） */
    fun startVoiceRecording(): Boolean {
        titleResetJob?.cancel()
        // 需求 1: 长按发语音时立即主动暂停正在播放的音乐
        pause()
        val started = getRecordingManager().startRecording()
        if (started) {
            _isVoiceListening.value = true
            _topBarTitleState.value = TopBarTitleState.Listening
            return true
        } else {
            _isVoiceListening.value = false
            _topBarTitleState.value = TopBarTitleState.Error("无法启动录音，请检查权限")
            scheduleTitleReset(3000L)
            return false
        }
    }

    /** 结束语音录音（松开手指触发）并调用模型识别与点歌 */
    fun finishVoiceRecording() {
        if (!_isVoiceListening.value) return
        _isVoiceListening.value = false
        val audioFile = getRecordingManager().stopRecording()
        if (audioFile != null) {
            _topBarTitleState.value = TopBarTitleState.Searching(null)
            viewModelScope.launch {
                val result = getAiManager().processVoiceAudio(audioFile) { liveSpoken ->
                    _topBarTitleState.value = TopBarTitleState.Searching(liveSpoken)
                }
                result.onSuccess { dispatch ->
                    _topBarTitleState.value = TopBarTitleState.Success("正在播放：${dispatch.intentSummary}")
                    playVoiceResult(dispatch)
                    scheduleTitleReset(4000L)
                }.onFailure { err ->
                    val spoken = (err as? VoiceProcessException)?.spokenText
                    val msg = err.message ?: "未在曲库中找到匹配歌曲"
                    val displayErr = if (!spoken.isNullOrBlank()) {
                        "“$spoken” $msg"
                    } else {
                        msg
                    }
                    _topBarTitleState.value = TopBarTitleState.Error(displayErr)
                    scheduleTitleReset(4000L)
                }
            }
        } else {
            _topBarTitleState.value = TopBarTitleState.Error("按住时间太短，请长按说话")
            scheduleTitleReset(3000L)
        }
    }

    /** 取消录音 */
    fun cancelVoiceRecording() {
        titleResetJob?.cancel()
        _isVoiceListening.value = false
        getRecordingManager().cancelRecording()
        _topBarTitleState.value = TopBarTitleState.Default
    }

    /** 未开启语音或未授权录音时长按底栏提示 */
    fun promptVoiceNeedOpen() {
        _topBarTitleState.value = TopBarTitleState.Error("未获取麦克风权限，请先授予权限")
        scheduleTitleReset(3000L)
    }

    override fun onCleared() {
        super.onCleared()
        titleResetJob?.cancel()
        favBatchPlayJob?.cancel()
        voiceRecordingManager?.cancelRecording()
        EventBusManager.unregister(this)
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

