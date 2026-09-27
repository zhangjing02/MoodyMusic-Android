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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    // ── 语音点歌与控制状态 ─────────────────────────────────────
    private val _isVoiceEnabled = MutableStateFlow(false)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _topBarTitleState = MutableStateFlow<TopBarTitleState>(TopBarTitleState.Default)
    val topBarTitleState: StateFlow<TopBarTitleState> = _topBarTitleState.asStateFlow()

    private var voiceAiManager: VoiceAiManager? = null
    private var voiceRecordingManager: VoiceRecordingManager? = null
    private var titleResetJob: Job? = null

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
                val prevIdx = _playState.value.playlistIndex
                val wasRoaming = _playState.value.isRoamingMode
                val wasLoading = _playState.value.isRoamingLoading
                _playState.value = incoming.copy(isRoamingMode = wasRoaming, isRoamingLoading = wasLoading)

                // 漫游模式下，切歌自动检查储备队列
                if (wasRoaming && incoming.playlistIndex != prevIdx) {
                    checkAndRefillRoamingQueue()
                }
            }
        }
    }

    /**
     * 播放指定歌单（从第 index 首开始）
     */
    fun play(songs: List<SongItem>, index: Int, artistName: String, albumTitle: String, coverUrl: String) {
        val ctx = getApplication<Application>()
        val playlist = ArrayList(songs.mapIndexed { i, song ->
            PlayQueueItem(
                songTitle = song.title,
                artistName = artistName,
                albumTitle = albumTitle,
                coverUrl = coverUrl,
                audioUrl = buildAudioUrl(song.path),
                lrcPath = song.lrcPath
            )
        })

        var targetIndex = index
        if (targetIndex in playlist.indices && playlist[targetIndex].audioUrl.isBlank()) {
            val firstPlayable = playlist.indexOfFirst { it.audioUrl.isNotBlank() }
            if (firstPlayable != -1) {
                targetIndex = firstPlayable
            } else {
                Toast.makeText(ctx, "《${playlist[index].songTitle}》暂无可用音频文件", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // 立即乐观更新播放状态，悬浮迷你播放器与底栏瞬时响应
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
                position = 0,
                playlistIndex = targetIndex,
                playMode = _playState.value.playMode,
                queue = playlist
            )
        }

        val intent = Intent(ctx, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_PLAYLIST, playlist)
            putExtra(MusicPlayerService.EXTRA_INDEX, targetIndex)
        }
        ctx.startForegroundService(intent)
    }

    /**
     * 播放手札/自建歌单（支持每首曲目拥有独立的歌手名、专辑名与封面）
     */
    fun playPlaylistSongs(songs: List<com.example.moodymusicforandroid.data.local.db.PlaylistSongEntity>, index: Int, playlistName: String) {
        if (songs.isEmpty()) return
        val ctx = getApplication<Application>()
        val playlist = ArrayList(songs.map { song ->
            PlayQueueItem(
                songTitle = song.title,
                artistName = song.artistName?.takeIf { it.isNotBlank() } ?: "未知歌手",
                albumTitle = song.albumTitle?.takeIf { it.isNotBlank() } ?: playlistName,
                coverUrl = song.coverUrl ?: "",
                audioUrl = buildAudioUrl(song.filePath),
                lrcPath = null
            )
        })

        var targetIndex = index.coerceIn(0, playlist.size - 1)
        if (targetIndex in playlist.indices && playlist[targetIndex].audioUrl.isBlank()) {
            val firstPlayable = playlist.indexOfFirst { it.audioUrl.isNotBlank() }
            if (firstPlayable != -1) {
                targetIndex = firstPlayable
            } else {
                Toast.makeText(ctx, "《${playlist[index].songTitle}》暂无可用音频文件", Toast.LENGTH_SHORT).show()
                return
            }
        }

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
                position = 0,
                playlistIndex = targetIndex,
                playMode = _playState.value.playMode,
                queue = playlist
            )
        }

        val intent = Intent(ctx, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_PLAYLIST, playlist)
            putExtra(MusicPlayerService.EXTRA_INDEX, targetIndex)
        }
        ctx.startForegroundService(intent)
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
        lrcPath: String? = null
    ) {
        val ctx = getApplication<Application>()
        val item = PlayQueueItem(
            songTitle = songTitle,
            artistName = artistName,
            albumTitle = albumTitle,
            coverUrl = coverUrl,
            audioUrl = audioUrl,
            lrcPath = lrcPath
        )
        val playlist = arrayListOf(item)

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
            queue = playlist
        )

        val intent = Intent(ctx, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_PLAYLIST, playlist)
            putExtra(MusicPlayerService.EXTRA_INDEX, 0)
        }
        ctx.startForegroundService(intent)
    }

    /**
     * 播放语音点歌/智能调度生成的队列
     */
    fun playVoiceResult(result: VoiceDispatchResult) {
        if (result.playlist.isEmpty()) return
        val ctx = getApplication<Application>()
        val playlist = ArrayList(result.playlist)
        val targetIndex = result.startIndex.coerceIn(0, playlist.size - 1)
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
            position = 0,
            playlistIndex = targetIndex,
            playMode = _playState.value.playMode,
            queue = playlist,
            isRoamingMode = false
        )

        val intent = Intent(ctx, MusicPlayerService::class.java).apply {
            action = MusicPlayerService.ACTION_PLAY
            putExtra(MusicPlayerService.EXTRA_PLAYLIST, playlist)
            putExtra(MusicPlayerService.EXTRA_INDEX, targetIndex)
        }
        ctx.startForegroundService(intent)
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
            val detail = cachedArtistDetails[artist.id] ?: run {
                try {
                    val resp = MoodyApiProvider.apiService.getArtistDetail(artist.id)
                    val d = resp.data?.firstOrNull()
                    if (d != null) {
                        cachedArtistDetails[artist.id] = d
                        // 缓存上限控制在 50 位艺人，防止内存泄漏
                        if (cachedArtistDetails.size > 50) {
                            val firstKey = cachedArtistDetails.keys.first()
                            cachedArtistDetails.remove(firstKey)
                        }
                    }
                    d
                } catch (_: Exception) {
                    null
                }
            } ?: continue

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
        _playState.value = _playState.value.copy(isRoamingMode = true)
        viewModelScope.launch {
            try {
                val firstTrack = pickOneRandomSong()
                if (firstTrack == null) {
                    Toast.makeText(getApplication(), "全库暂无可漫游曲目，请检查网络", Toast.LENGTH_SHORT).show()
                    _playState.value = _playState.value.copy(isRoamingMode = false)
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
                    isRoamingMode = true
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
                _playState.value = _playState.value.copy(isRoamingMode = false)
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
            _playState.value = _playState.value.copy(isRoamingMode = false)
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

    /** 未开启语音开关时长按底栏提示 */
    fun promptVoiceNeedOpen() {
        _topBarTitleState.value = TopBarTitleState.Error("语音功能未开启，请先在右上角开启")
        scheduleTitleReset(3000L)
    }

    override fun onCleared() {
        super.onCleared()
        titleResetJob?.cancel()
        voiceRecordingManager?.cancelRecording()
        EventBusManager.unregister(this)
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

