package com.example.moodymusicforandroid.ui.player

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as? MusicPlayerService.PlayerBinder
            musicService = b?.getService()
            isBound = true
            // 同步当前状态
            musicService?.let { service ->
                val serviceState = service.getCurrentState()
                if (serviceState.songTitle.isNotBlank()) {
                    if (_playState.value.isPlaying && _playState.value.songTitle == serviceState.songTitle) {
                        _playState.value = serviceState.copy(isPlaying = true)
                    } else {
                        _playState.value = serviceState
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
            state?.let { _playState.value = it }
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
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val clean = if (path.startsWith("storage/")) path else "storage/$path"
        return AppConfig.resolveUrl(clean)
    }

    override fun onCleared() {
        super.onCleared()
        EventBusManager.unregister(this)
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}

