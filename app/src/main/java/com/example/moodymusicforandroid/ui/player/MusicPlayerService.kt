package com.example.moodymusicforandroid.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.ui.home.activity.MainActivity
import android.widget.Toast
import java.io.IOException

class MusicPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "moody_music_player"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.example.moodymusicforandroid.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.moodymusicforandroid.ACTION_PAUSE"
        const val ACTION_TOGGLE = "com.example.moodymusicforandroid.ACTION_TOGGLE"
        const val ACTION_NEXT = "com.example.moodymusicforandroid.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.moodymusicforandroid.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.example.moodymusicforandroid.ACTION_STOP"
        const val ACTION_SEEK = "com.example.moodymusicforandroid.ACTION_SEEK"
        const val ACTION_TOGGLE_MODE = "com.example.moodymusicforandroid.ACTION_TOGGLE_MODE"
        const val ACTION_PLAY_INDEX = "com.example.moodymusicforandroid.ACTION_PLAY_INDEX"
        const val ACTION_REMOVE_INDEX = "com.example.moodymusicforandroid.ACTION_REMOVE_INDEX"
        const val ACTION_CLEAR_QUEUE = "com.example.moodymusicforandroid.ACTION_CLEAR_QUEUE"
        const val ACTION_ADD_TO_QUEUE = "com.example.moodymusicforandroid.ACTION_ADD_TO_QUEUE"

        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
        const val EXTRA_QUEUE_ITEM = "extra_queue_item"

        val AUDIO_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()
    }

    private val TAG = "MusicPlayerService"
    private var mediaPlayer: MediaPlayer? = null
    private var playlist: ArrayList<PlayQueueItem> = arrayListOf()
    private var currentIndex: Int = 0
    private var currentPlayMode: PlayMode = PlayMode.fromString(UserManager.getCurrentPlayMode())
    private var mediaSession: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentCoverBitmap: Bitmap? = null
    private var currentCoverUrl: String? = null
    @Volatile
    private var isPreparing: Boolean = false // 音频正在异步缓冲准备中
    @Volatile
    private var isMediaPlayerPrepared: Boolean = false // 只有为 true 时才允许调用 duration/position
    @Volatile
    private var userWantsToPlay: Boolean = true // 用户意图是否保持播放

    private var retryCount: Int = 0
    private val retryHandler = Handler(Looper.getMainLooper())
    private val MAX_RETRY_COUNT = 2

    // 媒体流准备看门狗 (LocalMediaProxy 纯 IPv4 极速代理加持，1~2秒即可秒开)
    private val prepareTimeoutHandler = Handler(Looper.getMainLooper())
    private var prepareTimeoutRunnable: Runnable? = null
    private val PREPARE_TIMEOUT_MS = 15000L

    private val notificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }

    private val openPendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val togglePendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this, 1,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val prevPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this, 2,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val nextPendingIntent: PendingIntent by lazy {
        PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlayerService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    inner class PlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    private val binder = PlayerBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSessionCompat(this, "MoodyMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    playNext()
                }

                override fun onSkipToPrevious() {
                    playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toInt())
                }

                override fun onStop() {
                    stopPlayback()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                @Suppress("UNCHECKED_CAST")
                val newPlaylist = intent.getSerializableExtra(EXTRA_PLAYLIST) as? ArrayList<PlayQueueItem>
                val newIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                if (newPlaylist != null) {
                    playlist = newPlaylist
                    currentIndex = newIndex
                    retryCount = 0
                    playCurrentSong()
                } else {
                    resumePlayback()
                }
            }
            ACTION_PAUSE -> pausePlayback()
            ACTION_TOGGLE -> togglePlayPause()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                seekTo(pos)
            }
            ACTION_TOGGLE_MODE -> togglePlayMode()
            ACTION_PLAY_INDEX -> {
                val idx = intent.getIntExtra(EXTRA_INDEX, 0)
                playIndex(idx)
            }
            ACTION_REMOVE_INDEX -> {
                val idx = intent.getIntExtra(EXTRA_INDEX, -1)
                if (idx >= 0) removeItem(idx)
            }
            ACTION_CLEAR_QUEUE -> clearQueue()
            ACTION_ADD_TO_QUEUE -> {
                @Suppress("UNCHECKED_CAST")
                val item = intent.getSerializableExtra(EXTRA_QUEUE_ITEM) as? PlayQueueItem
                if (item != null) addToQueue(item)
            }
        }
        return START_STICKY
    }

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying()) {
                broadcastPlayState(isPlaying = true)
                progressHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun getPlayableAudioUrl(rawUrl: String): String {
        // 默认直接使用 R2 官方直连 CDN，跳过 Cloudflare Worker 代理中间层，实现首次点击即秒开
        if (rawUrl.startsWith("https://m-api.changgepd.ccwu.cc/storage/")) {
            val directUrl = rawUrl.replace("https://m-api.changgepd.ccwu.cc/storage/", "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/")
            Log.i(TAG, "Direct R2 audio URL: $directUrl")
            return directUrl
        }
        return rawUrl
    }

    private fun playCurrentSong() {
        if (playlist.isEmpty() || currentIndex !in playlist.indices) return
        val item = playlist[currentIndex]

        if (item.audioUrl.isBlank()) {
            isPreparing = false
            isMediaPlayerPrepared = false
            Log.w(TAG, "Song ${item.songTitle} has no audio URL, searching next playable...")
            val nextPlayable = (currentIndex + 1 until playlist.size).firstOrNull { playlist[it].audioUrl.isNotBlank() }
                ?: (0 until currentIndex).firstOrNull { playlist[it].audioUrl.isNotBlank() }
            if (nextPlayable != null && nextPlayable != currentIndex) {
                currentIndex = nextPlayable
                retryCount = 0
                playCurrentSong()
            } else {
                broadcastPlayState(isPlaying = false)
            }
            return
        }

        val playableUrl = getPlayableAudioUrl(item.audioUrl)
        // 核心突破: 通过本地纯 IPv4 流媒体代理加载，彻底阻断 Android 原生 MediaPlayer 直连海外 Cloudflare IPv6 节点的握手黑洞
        val targetUrl = LocalMediaProxy.getProxyUrl(playableUrl)
        userWantsToPlay = true
        isPreparing = true
        isMediaPlayerPrepared = false
        requestAudioFocus()
        broadcastPlayState(isPlaying = true)
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        updateMetadata()
        loadCoverBitmap(item.coverUrl)

        retryHandler.removeCallbacksAndMessages(null)
        prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AUDIO_ATTRIBUTES)
            try {
                Log.i(TAG, "Playing song [${currentIndex + 1}/${playlist.size}]: ${item.songTitle}, url=$targetUrl (retryCount=$retryCount)")
                setDataSource(targetUrl)

                // 启动 15 秒准备看门狗（在纯 IPv4 本地代理加速下，通常 1~2 秒即可准备完毕）
                val songNameForTimeout = item.songTitle
                val indexForTimeout = currentIndex

                val watchdog = Runnable {
                    if (isPreparing && !isMediaPlayerPrepared && userWantsToPlay && currentIndex == indexForTimeout) {
                        Log.w(TAG, "MediaPlayer prepareAsync watchdog timeout (${PREPARE_TIMEOUT_MS}ms) for: $songNameForTimeout (retryCount=$retryCount)")
                        isPreparing = false
                        isMediaPlayerPrepared = false
                        try {
                            mediaPlayer?.reset()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resetting mediaPlayer on timeout", e)
                        }

                        if (retryCount < 2) {
                            retryCount++
                            Log.i(TAG, "Timeout on attempt #$retryCount, retrying for $songNameForTimeout...")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "正在优化网络线路，请稍候...", Toast.LENGTH_SHORT).show()
                            }
                            playCurrentSong()
                        } else {
                            retryCount = 0
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "《$songNameForTimeout》多次加载超时，跳至下一首", Toast.LENGTH_SHORT).show()
                            }
                            playNext()
                        }
                    }
                }
                prepareTimeoutRunnable = watchdog
                prepareTimeoutHandler.postDelayed(watchdog, PREPARE_TIMEOUT_MS)

                prepareAsync()
                setOnPreparedListener {
                    prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                    Log.i(TAG, "MediaPlayer prepared: ${item.songTitle}, userWantsToPlay=$userWantsToPlay")
                    retryCount = 0 // 播放成功，重置重试计数器
                    isMediaPlayerPrepared = true
                    isPreparing = false
                    if (userWantsToPlay) {
                        start()
                        progressHandler.removeCallbacks(progressRunnable)
                        progressHandler.post(progressRunnable)
                        broadcastPlayState(isPlaying = true)
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        updateNotification()
                    } else {
                        pausePlayback()
                    }
                    updateMetadata()
                }
                setOnCompletionListener {
                    prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                    isMediaPlayerPrepared = false
                    isPreparing = false
                    onSongCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra, retryCount=$retryCount")
                    isMediaPlayerPrepared = false
                    isPreparing = false
                    progressHandler.removeCallbacks(progressRunnable)

                    if (userWantsToPlay && retryCount < MAX_RETRY_COUNT) {
                        retryCount++
                        Log.w(TAG, "Attempting auto-retry $retryCount/$MAX_RETRY_COUNT in 1500ms...")
                        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
                        retryHandler.postDelayed({
                            if (userWantsToPlay && !isMediaPlayerPrepared) {
                                playCurrentSong()
                            }
                        }, 1500)
                    } else {
                        broadcastPlayState(isPlaying = false)
                        updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext, "音频连接超时，轻触可重新加载", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
            } catch (e: Exception) {
                prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                Log.e(TAG, "setDataSource failed: ${e.message}", e)
                isMediaPlayerPrepared = false
                isPreparing = false
                progressHandler.removeCallbacks(progressRunnable)

                if (userWantsToPlay && retryCount < MAX_RETRY_COUNT) {
                    retryCount++
                    Log.w(TAG, "Attempting auto-retry $retryCount/$MAX_RETRY_COUNT after exception in 1500ms...")
                    retryHandler.postDelayed({
                        if (userWantsToPlay && !isMediaPlayerPrepared) {
                            playCurrentSong()
                        }
                    }, 1500)
                } else {
                    broadcastPlayState(isPlaying = false)
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(applicationContext, "音频加载失败，轻触可重新加载", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        startForegroundServiceWithNotification()
    }

    /**
     * 单曲播放自然结束时的调度策略
     */
    private fun onSongCompleted() {
        if (playlist.isEmpty()) return
        when (currentPlayMode) {
            PlayMode.SINGLE_LOOP -> {
                // 单曲循环：从头重播当前歌曲
                playCurrentSong()
            }
            PlayMode.SHUFFLE -> {
                // 随机播放：随机选取非当前歌曲
                if (playlist.size > 1) {
                    var nextIdx = (0 until playlist.size).random()
                    if (nextIdx == currentIndex) {
                        nextIdx = (nextIdx + 1) % playlist.size
                    }
                    currentIndex = nextIdx
                }
                playCurrentSong()
            }
            PlayMode.SEQUENTIAL -> {
                // 顺序播放：若未到末尾继续下一首；整张专辑/列表播放完毕后，自动收起底部播放悬浮窗并释放资源
                if (currentIndex + 1 < playlist.size) {
                    currentIndex++
                    playCurrentSong()
                } else {
                    Log.i(TAG, "[onSongCompleted] 顺序播放已播完全部曲目，调用 stopPlayback 收起底部悬浮窗并释放资源")
                    stopPlayback()
                }
            }
            PlayMode.LIST_LOOP -> {
                // 列表循环：到达末尾回到开头
                currentIndex = (currentIndex + 1) % playlist.size
                playCurrentSong()
            }
        }
    }

    private var hasAudioFocus: Boolean = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        Log.i(TAG, "onAudioFocusChange: $focusChange")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                resumePlayback()
            }
        }
    }

    private fun requestAudioFocus() {
        if (hasAudioFocus) return
        val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(AUDIO_ATTRIBUTES)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
            audioManager?.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasAudioFocus = (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        Log.i(TAG, "requestAudioFocus result=$res, hasAudioFocus=$hasAudioFocus")
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    fun resumePlayback() {
        userWantsToPlay = true
        if (isMediaPlayerPrepared && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.post(progressRunnable)
            broadcastPlayState(isPlaying = true)
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            updateNotification()
        } else if (isPreparing) {
            broadcastPlayState(isPlaying = true)
            updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
            updateNotification()
        } else if (!isMediaPlayerPrepared && playlist.isNotEmpty() && currentIndex in playlist.indices) {
            // 出错停滞或未就绪时，用户点击播放主动重新触发加载
            Log.i(TAG, "resumePlayback: MediaPlayer not prepared, re-initiating playCurrentSong()")
            retryCount = 0
            playCurrentSong()
        }
    }

    fun pausePlayback() {
        userWantsToPlay = false
        isPreparing = false
        prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
        if (isMediaPlayerPrepared && mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
        progressHandler.removeCallbacks(progressRunnable)
        broadcastPlayState(isPlaying = false)
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        updateNotification()
    }

    fun togglePlayPause() {
        if (isPlaying()) pausePlayback() else resumePlayback()
    }

    fun togglePlayMode(): PlayMode {
        currentPlayMode = currentPlayMode.next()
        UserManager.updatePlayMode(currentPlayMode.name)
        broadcastPlayState(isPlaying = isPlaying())
        return currentPlayMode
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        when (currentPlayMode) {
            PlayMode.SHUFFLE -> {
                if (playlist.size > 1) {
                    var nextIdx = (0 until playlist.size).random()
                    if (nextIdx == currentIndex) {
                        nextIdx = (nextIdx + 1) % playlist.size
                    }
                    currentIndex = nextIdx
                }
            }
            else -> {
                currentIndex = (currentIndex + 1) % playlist.size
            }
        }
        playCurrentSong()
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        when (currentPlayMode) {
            PlayMode.SHUFFLE -> {
                if (playlist.size > 1) {
                    var prevIdx = (0 until playlist.size).random()
                    if (prevIdx == currentIndex) {
                        prevIdx = (prevIdx + playlist.size - 1) % playlist.size
                    }
                    currentIndex = prevIdx
                }
            }
            else -> {
                currentIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            }
        }
        playCurrentSong()
    }

    fun playIndex(index: Int) {
        if (playlist.isEmpty() || index !in playlist.indices) return
        currentIndex = index
        playCurrentSong()
    }

    fun removeItem(index: Int) {
        if (index !in playlist.indices) return
        val wasPlaying = currentIndex == index
        playlist.removeAt(index)
        if (playlist.isEmpty()) {
            stopPlayback()
        } else {
            if (currentIndex > index) {
                currentIndex--
            } else if (currentIndex == index) {
                if (currentIndex >= playlist.size) {
                    currentIndex = 0
                }
                if (wasPlaying) {
                    playCurrentSong()
                    return
                }
            }
            broadcastPlayState(isPlaying = isPlaying())
            updatePlaybackState()
        }
    }

    fun clearQueue() {
        stopPlayback()
    }

    /**
     * 将一首歌追加到当前播放队列末尾。
     *
     * - 队列为空时：以该曲新建单曲播放会话，返回 STARTED_NEW
     * - 队列中已存在相同 audioUrl：跳过，返回 DUPLICATE
     * - 其他情况：追加到末尾并广播，返回 ADDED
     */
    fun addToQueue(item: PlayQueueItem): AddToQueueResult {
        return if (playlist.isEmpty()) {
            // 空队列 → 直接开播
            playlist.add(item)
            currentIndex = 0
            retryCount = 0
            playCurrentSong()
            AddToQueueResult.STARTED_NEW
        } else if (playlist.any { it.audioUrl == item.audioUrl }) {
            // 已存在 → 去重
            AddToQueueResult.DUPLICATE
        } else {
            // 追加末尾
            playlist.add(item)
            broadcastPlayState(isPlaying = isPlaying())
            AddToQueueResult.ADDED
        }
    }

    fun seekTo(posMs: Int) {
        if (isMediaPlayerPrepared) {
            try {
                mediaPlayer?.seekTo(posMs)
                broadcastPlayState(isPlaying = isPlaying())
                updatePlaybackState()
            } catch (e: Exception) {
                Log.e(TAG, "seekTo failed: ${e.message}", e)
            }
        }
    }

    fun stopPlayback() {
        userWantsToPlay = false
        isPreparing = false
        isMediaPlayerPrepared = false
        retryCount = 0
        retryHandler.removeCallbacksAndMessages(null)
        prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
        abandonAudioFocus()
        progressHandler.removeCallbacks(progressRunnable)
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        playlist.clear()
        currentIndex = 0
        currentCoverBitmap = null
        currentCoverUrl = null
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        EventBusManager.postWithData(EventType.MUSIC_PLAY_STATE_CHANGED, MusicPlayState())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun isPlaying(): Boolean = isPreparing || (isMediaPlayerPrepared && (try {
        mediaPlayer?.isPlaying == true
    } catch (_: Exception) {
        false
    }))

    fun getCurrentState(): MusicPlayState {
        val item = playlist.getOrNull(currentIndex)
        return MusicPlayState(
            songTitle = item?.songTitle ?: "",
            artistName = item?.artistName ?: "",
            albumTitle = item?.albumTitle ?: "",
            coverUrl = item?.coverUrl ?: "",
            audioUrl = item?.audioUrl ?: "",
            lrcPath = item?.lrcPath,
            isPlaying = isPlaying(),
            duration = if (isMediaPlayerPrepared) {
                try { mediaPlayer?.duration?.takeIf { it > 0 } ?: 0 } catch (_: Exception) { 0 }
            } else 0,
            position = if (isMediaPlayerPrepared) {
                try { mediaPlayer?.currentPosition?.takeIf { it > 0 } ?: 0 } catch (_: Exception) { 0 }
            } else 0,
            playlistIndex = currentIndex,
            playMode = currentPlayMode,
            queue = ArrayList(playlist)
        )
    }

    private fun broadcastPlayState(isPlaying: Boolean) {
        val item = playlist.getOrNull(currentIndex)
        val state = MusicPlayState(
            songTitle = item?.songTitle ?: "",
            artistName = item?.artistName ?: "",
            albumTitle = item?.albumTitle ?: "",
            coverUrl = item?.coverUrl ?: "",
            audioUrl = item?.audioUrl ?: "",
            lrcPath = item?.lrcPath,
            isPlaying = isPlaying,
            duration = if (isMediaPlayerPrepared) {
                try { mediaPlayer?.duration?.takeIf { it > 0 } ?: 0 } catch (_: Exception) { 0 }
            } else 0,
            position = if (isMediaPlayerPrepared) {
                try { mediaPlayer?.currentPosition?.takeIf { it > 0 } ?: 0 } catch (_: Exception) { 0 }
            } else 0,
            playlistIndex = currentIndex,
            playMode = currentPlayMode,
            queue = ArrayList(playlist)
        )
        EventBusManager.postWithData(EventType.MUSIC_PLAY_STATE_CHANGED, state)
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updatePlaybackState(state: Int = if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED) {
        val position = if (isMediaPlayerPrepared) {
            try { mediaPlayer?.currentPosition?.toLong()?.takeIf { it > 0 } ?: 0L } catch (_: Exception) { 0L }
        } else 0L
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, position, if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0.0f)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    private fun updateMetadata(coverBitmap: Bitmap? = currentCoverBitmap) {
        val item = playlist.getOrNull(currentIndex) ?: return
        val duration = if (isMediaPlayerPrepared) {
            try { mediaPlayer?.duration?.toLong()?.takeIf { it > 0 } ?: 0L } catch (_: Exception) { 0L }
        } else 0L
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.songTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.artistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.albumTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, item.songTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, item.artistName)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        if (coverBitmap != null) {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, coverBitmap)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, coverBitmap)
        }
        mediaSession?.setMetadata(builder.build())
    }

    private fun loadCoverBitmap(coverUrl: String) {
        if (coverUrl.isBlank()) {
            currentCoverBitmap = null
            currentCoverUrl = null
            updateMetadata(null)
            updateNotification()
            return
        }
        if (coverUrl == currentCoverUrl && currentCoverBitmap != null) {
            updateMetadata(currentCoverBitmap)
            updateNotification()
            return
        }
        currentCoverUrl = coverUrl
        currentCoverBitmap = null

        try {
            Glide.with(applicationContext)
                .asBitmap()
                .load(coverUrl)
                .into(object : CustomTarget<Bitmap>(256, 256) {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        if (currentCoverUrl == coverUrl) {
                            currentCoverBitmap = resource
                            updateMetadata(resource)
                            updateNotification()
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load notification cover: ${e.message}")
        }
    }

    private fun buildNotification(): Notification {
        val item = playlist.getOrNull(currentIndex)
        val isPlaying = isPlaying()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_music)
            .setContentTitle(item?.songTitle?.takeIf { it.isNotBlank() } ?: "Moody Music")
            .setContentText("${item?.artistName ?: ""} - ${item?.albumTitle ?: ""}")
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_skip_previous,
                "上一首",
                prevPendingIntent
            )
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                if (isPlaying) "暂停" else "播放",
                togglePendingIntent
            )
            .addAction(
                R.drawable.ic_skip_next,
                "下一首",
                nextPendingIntent
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession?.sessionToken)
            )
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        currentCoverBitmap?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Moody 音乐播放控制"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        mediaSession?.release()
        abandonAudioFocus()
    }
}

