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
import android.media.audiofx.AudioEffect
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
import com.example.moodymusicforandroid.common.utils.SleepTimerManager
import com.example.moodymusicforandroid.ui.home.activity.MainActivity
import android.widget.Toast
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
        const val ACTION_ADD_ALL_TO_QUEUE = "com.example.moodymusicforandroid.ACTION_ADD_ALL_TO_QUEUE"
        const val ACTION_SLEEP_TIMEOUT = "com.example.moodymusicforandroid.ACTION_SLEEP_TIMEOUT"

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
    @Volatile
    private var pendingSeekPositionMs: Int = 0 // 起播/切歌预设跳转位置（乐章时间轴/断点续播）

    private var retryCount: Int = 0
    private val retryHandler = Handler(Looper.getMainLooper())
    private val MAX_RETRY_COUNT = 2

    // 系统级硬件音效控制会话（Dolby Atmos / 华为 Histen / 小米音效 / 车载声学算法）
    private var currentAudioSessionId: Int? = null

    private fun openAudioEffectSession(sessionId: Int) {
        if (sessionId <= 0) return
        try {
            val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            sendBroadcast(intent)
            Log.i(TAG, "AudioEffect session opened for sessionId=$sessionId (Dolby/Histen/车载音效已挂载)")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open AudioEffect session: ${e.message}")
        }
    }

    private fun closeAudioEffectSession(sessionId: Int?) {
        if (sessionId == null || sessionId <= 0) return
        try {
            val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
            sendBroadcast(intent)
            Log.i(TAG, "AudioEffect session closed for sessionId=$sessionId")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close AudioEffect session: ${e.message}")
        }
    }

    // 媒体流准备看门狗 (自适应数据流动感知：常规歌曲基准 35s，长篇大作/整轨特辑 60s)
    private val prepareTimeoutHandler = Handler(Looper.getMainLooper())
    private var prepareTimeoutRunnable: Runnable? = null
    private val BASE_PREPARE_TIMEOUT_MS = 35000L
    private val EXTENDED_PREPARE_TIMEOUT_MS = 60000L

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
        LocalMediaProxy.init(applicationContext)
        createNotificationChannel()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSessionCompat(this, "MoodyMusicSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    SleepTimerManager.recordUserInteraction()
                    resumePlayback()
                }

                override fun onPause() {
                    SleepTimerManager.recordUserInteraction()
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    SleepTimerManager.recordUserInteraction()
                    playNext()
                }

                override fun onSkipToPrevious() {
                    SleepTimerManager.recordUserInteraction()
                    playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    SleepTimerManager.recordUserInteraction()
                    seekTo(pos.toInt())
                }

                override fun onStop() {
                    SleepTimerManager.recordUserInteraction()
                    stopPlayback()
                }
            })
            isActive = true
        }
        EventBusManager.register(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let {
            if (it != ACTION_SLEEP_TIMEOUT) {
                SleepTimerManager.recordUserInteraction()
            }
        }
        when (intent?.action) {
            ACTION_SLEEP_TIMEOUT -> {
                fadeOutAndStop("休眠定时超时")
            }
            ACTION_PLAY -> {
                @Suppress("UNCHECKED_CAST")
                val newPlaylist = intent.getSerializableExtra(EXTRA_PLAYLIST) as? ArrayList<PlayQueueItem>
                val newIndex = intent.getIntExtra(EXTRA_INDEX, 0)
                val initialSeekMs = intent.getIntExtra(EXTRA_SEEK_POSITION, 0)
                if (newPlaylist != null) {
                    val currentItem = playlist.getOrNull(currentIndex)
                    val incomingItem = newPlaylist.getOrNull(newIndex)
                    val isAlreadyPlayingThis = (currentItem != null && incomingItem != null &&
                        currentItem.audioUrl == incomingItem.audioUrl &&
                        (isPreparing || isMediaPlayerPrepared))
                    if (!isAlreadyPlayingThis) {
                        playlist = newPlaylist
                        currentIndex = newIndex
                        pendingSeekPositionMs = initialSeekMs
                        retryCount = 0
                        // 异步预热当前首曲连接
                        newPlaylist.getOrNull(newIndex)?.audioUrl?.let { LocalMediaProxy.prewarmConnection(it) }
                        playCurrentSong()
                    } else if (initialSeekMs > 0) {
                        seekTo(initialSeekMs)
                    }
                } else {
                    if (initialSeekMs > 0) {
                        seekTo(initialSeekMs)
                    }
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
            ACTION_ADD_ALL_TO_QUEUE -> {
                @Suppress("UNCHECKED_CAST")
                val items = intent.getSerializableExtra(EXTRA_PLAYLIST) as? ArrayList<PlayQueueItem>
                if (!items.isNullOrEmpty()) {
                    addAllToQueue(items)
                }
            }
        }
        return START_STICKY
    }

    private var isFadingOut: Boolean = false
    private val fadeHandler = Handler(Looper.getMainLooper())

    private var hasPrewarmedNextSong = false // A-B2: 标记当前歌曲是否已对下一首发起过连接预热

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            if (isPlaying()) {
                broadcastPlayState(isPlaying = true)
                checkAndPrewarmNextSong()
                // 🌙 智能夜间闲置守护检查 (处于 23:00~06:00 且连续无操作达到设定时长)
                if (SleepTimerManager.checkNightIdleTimeout() && !isFadingOut) {
                    Log.i(TAG, "🌙 Night idle timeout reached! Initiating fadeOutAndStop...")
                    SleepTimerManager.recordUserInteraction() // 重置，防止重入
                    fadeOutAndStop("夜间长时间无操作")
                    return
                }
                progressHandler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * A-B2 核心修复: 预测性下一首连接预热
     * 当当前歌曲播放剩余时间小于 25 秒时，根据播放模式计算下一首待播歌曲，
     * 提前在后台发起 DNS 解析与 TLS 握手热身，彻底消除切歌瞬间网络冷启动卡顿。
     */
    private fun checkAndPrewarmNextSong() {
        if (hasPrewarmedNextSong || !isMediaPlayerPrepared || playlist.size <= 1) return
        if (currentPlayMode == PlayMode.SINGLE_LOOP) return

        val player = mediaPlayer ?: return
        try {
            val duration = player.duration
            val currentPos = player.currentPosition
            if (duration > 0 && currentPos > 0) {
                val remainingMs = duration - currentPos
                if (remainingMs in 1..25_000) {
                    val nextIdx = when (currentPlayMode) {
                        PlayMode.SEQUENTIAL -> {
                            if (currentIndex >= playlist.size - 1) return
                            currentIndex + 1
                        }
                        PlayMode.LIST_LOOP -> (currentIndex + 1) % playlist.size
                        PlayMode.SHUFFLE -> {
                            val candidates = playlist.indices.filter { it != currentIndex }
                            if (candidates.isEmpty()) return
                            candidates.random()
                        }
                        else -> (currentIndex + 1) % playlist.size
                    }
                    val nextItem = playlist.getOrNull(nextIdx)
                    if (nextItem != null && nextItem.audioUrl.isNotBlank()) {
                        hasPrewarmedNextSong = true
                        val playableUrl = getPlayableAudioUrl(nextItem.audioUrl)
                        Log.i(TAG, "[Prewarm] 🚀 触发下一首连接预热: \"${nextItem.songTitle}\" (剩余 ${remainingMs / 1000}s)")
                        LocalMediaProxy.prewarmConnection(playableUrl)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun getPlayableAudioUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return rawUrl
        return com.example.moodymusicforandroid.common.config.AppConfig.canonicalizeUrl(rawUrl)
    }

    private fun playCurrentSong() {
        if (playlist.isEmpty() || currentIndex !in playlist.indices) return
        hasPrewarmedNextSong = false // 重置下一首预热标记
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
        currentAudioSessionId?.let {
            closeAudioEffectSession(it)
            currentAudioSessionId = null
        }
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AUDIO_ATTRIBUTES)
            val newSessionId = audioSessionId
            if (newSessionId > 0) {
                currentAudioSessionId = newSessionId
                openAudioEffectSession(newSessionId)
            }
            try {
                Log.i(TAG, "Playing song [${currentIndex + 1}/${playlist.size}]: ${item.songTitle}, url=$targetUrl (retryCount=$retryCount, audioSessionId=$newSessionId)")
                setDataSource(targetUrl)

                // 动态自适应数据感知准备看门狗 (Dynamic Data-Flow Adaptive Watchdog)
                // 1. 常规单曲基准保底 35 秒（自适应数据流动感知，绝不误杀正常下载）
                // 2. 长篇特辑/整轨大碟/交响乐/时长>600秒，放宽至 60 秒
                val isLargeOrLongTrack = item.audioUrl.contains("theme", ignoreCase = true) ||
                    item.audioUrl.contains("collection", ignoreCase = true) ||
                    item.audioUrl.contains("concerto", ignoreCase = true) ||
                    item.audioUrl.contains("live", ignoreCase = true) ||
                    (item.duration != null && item.duration > 600)
                val currentTimeoutMs = if (isLargeOrLongTrack) EXTENDED_PREPARE_TIMEOUT_MS else BASE_PREPARE_TIMEOUT_MS

                val songNameForTimeout = item.songTitle
                val indexForTimeout = currentIndex

                val watchdog = object : Runnable {
                    var elapsedMs = 0L

                    override fun run() {
                        if (!isPreparing || isMediaPlayerPrepared || !userWantsToPlay || currentIndex != indexForTimeout) {
                            return
                        }
                        elapsedMs += 3000L

                        // 核心突破：数据流动感知。如果底层代理正在持续接收传输数据，说明链路正常，绝不误杀！
                        val isActivelyTransferring = LocalMediaProxy.isActivelyTransferring(withinMs = 4000L)
                        if (isActivelyTransferring && elapsedMs < 60000L) {
                            Log.d(TAG, "Watchdog: $songNameForTimeout is actively downloading (${elapsedMs}ms elapsed, totalTransferred=${LocalMediaProxy.totalBytesTransferred}), extending watchdog...")
                            prepareTimeoutHandler.postDelayed(this, 3000L)
                            return
                        }

                        // 如果尚未到达基础超时且没有完全超时，继续周期轮询
                        if (elapsedMs < currentTimeoutMs) {
                            prepareTimeoutHandler.postDelayed(this, 3000L)
                            return
                        }

                        // 真正死锁或上游彻底断流超过阈值，才触发超时容灾处理
                        Log.w(TAG, "MediaPlayer prepareAsync watchdog stall timeout (${elapsedMs}ms) for: $songNameForTimeout (retryCount=$retryCount, transferring=$isActivelyTransferring)")
                        isPreparing = false
                        isMediaPlayerPrepared = false
                        try {
                            mediaPlayer?.reset()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error resetting mediaPlayer on timeout", e)
                        }

                        if (retryCount < MAX_RETRY_COUNT) {
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
                prepareTimeoutHandler.postDelayed(watchdog, 3000L)

                prepareAsync()
                setOnPreparedListener {
                    prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                    prepareTimeoutRunnable = null
                    Log.i(TAG, "MediaPlayer prepared: ${item.songTitle}, userWantsToPlay=$userWantsToPlay, pendingSeek=$pendingSeekPositionMs")
                    retryCount = 0 // 播放成功，重置重试计数器
                    isMediaPlayerPrepared = true
                    isPreparing = false
                    if (pendingSeekPositionMs > 0) {
                        val seekTarget = pendingSeekPositionMs
                        pendingSeekPositionMs = 0
                        try {
                            mediaPlayer?.seekTo(seekTarget)
                        } catch (e: Exception) {
                            Log.e(TAG, "initial seek failed: ${e.message}", e)
                        }
                    }
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
                    prepareTimeoutRunnable = null
                    isMediaPlayerPrepared = false
                    isPreparing = false
                    onSongCompleted()
                }
                setOnErrorListener { _, what, extra ->
                    prepareTimeoutRunnable?.let { prepareTimeoutHandler.removeCallbacks(it) }
                    prepareTimeoutRunnable = null
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
        // 检查主动休眠定时器是否设定为“播完当前单曲后停止”
        if (SleepTimerManager.onSongCompletion(applicationContext)) {
            Log.i(TAG, "[onSongCompleted] 命中播完当前歌曲后停止设定，触发平滑关停")
            fadeOutAndStop("播完当曲停止")
            return
        }
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

    /**
     * 批量追加多首曲目到播放队列末尾（流式滑窗分批预加载，单次广播）
     */
    fun addAllToQueue(items: List<PlayQueueItem>): Int {
        if (items.isEmpty()) return 0
        var addedCount = 0
        val wasEmpty = playlist.isEmpty()
        val existingUrls = playlist.map { it.audioUrl }.toHashSet()
        for (item in items) {
            if (item.audioUrl.isNotBlank() && existingUrls.add(item.audioUrl)) {
                playlist.add(item)
                addedCount++
            }
        }
        if (addedCount > 0) {
            if (wasEmpty) {
                currentIndex = 0
                retryCount = 0
                playCurrentSong()
            } else {
                broadcastPlayState(isPlaying = isPlaying())
            }
        }
        return addedCount
    }

    fun playPlaylist(newPlaylist: List<PlayQueueItem>, newIndex: Int = 0, initialSeekMs: Int = 0) {
        if (newPlaylist.isEmpty()) return
        val currentItem = playlist.getOrNull(currentIndex)
        val incomingItem = newPlaylist.getOrNull(newIndex)
        val isAlreadyPlayingThis = (currentItem != null && incomingItem != null &&
            currentItem.audioUrl == incomingItem.audioUrl &&
            (isPreparing || isMediaPlayerPrepared))
        if (!isAlreadyPlayingThis) {
            playlist = ArrayList(newPlaylist)
            currentIndex = newIndex.coerceIn(0, playlist.size - 1)
            pendingSeekPositionMs = initialSeekMs
            retryCount = 0
            newPlaylist.getOrNull(currentIndex)?.audioUrl?.let { LocalMediaProxy.prewarmConnection(it) }
            playCurrentSong()
        } else if (initialSeekMs > 0) {
            seekTo(initialSeekMs)
        }
    }

    fun seekTo(posMs: Int) {
        if (isMediaPlayerPrepared) {
            try {
                mediaPlayer?.seekTo(posMs)
                broadcastPlayState(isPlaying = isPlaying(), overridePosition = posMs)
                updatePlaybackState()
            } catch (e: Exception) {
                Log.e(TAG, "seekTo failed: ${e.message}", e)
            }
        } else if (isPreparing) {
            pendingSeekPositionMs = posMs
            broadcastPlayState(isPlaying = isPlaying(), overridePosition = posMs)
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
        currentAudioSessionId?.let {
            closeAudioEffectSession(it)
            currentAudioSessionId = null
        }
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

    private fun broadcastPlayState(isPlaying: Boolean, overridePosition: Int? = null) {
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
            position = overridePosition ?: if (isMediaPlayerPrepared) {
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
        if (coverUrl.isBlank() || coverUrl.contains("vinyl_default") || coverUrl.startsWith("/src/assets")) {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.album_vintage_vinyl)
            currentCoverBitmap = bitmap
            currentCoverUrl = coverUrl
            updateMetadata(bitmap)
            updateNotification()
            return
        }
        if (coverUrl == currentCoverUrl && currentCoverBitmap != null) {
            updateMetadata(currentCoverBitmap)
            updateNotification()
            return
        }
        currentCoverUrl = coverUrl
        val safeCoverUrl = com.example.moodymusicforandroid.common.config.AppConfig.resolveUrl(coverUrl)
        if (safeCoverUrl.isBlank()) {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.album_vintage_vinyl)
            currentCoverBitmap = bitmap
            updateMetadata(bitmap)
            updateNotification()
            return
        }
        try {
            Glide.with(applicationContext)
                .asBitmap()
                .load(safeCoverUrl)
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

    /**
     * 平滑淡出音量并彻底停止播放（休眠定时、夜间防沉睡自动停播专用）
     * 3 秒内将音量从 1.0f 线性降为 0.0f，避免突然断音惊醒睡眠中的用户，随后完全释放资源并移除常驻通知
     */
    fun fadeOutAndStop(reason: String = "休眠定时") {
        if (!isPlaying() || isFadingOut) {
            stopPlayback()
            stopSelf()
            return
        }

        isFadingOut = true
        Log.i(TAG, "fadeOutAndStop initiated, reason: $reason")

        val totalSteps = 20
        val stepIntervalMs = 150L // 20 * 150ms = 3000ms
        var currentStep = totalSteps

        val fadeRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer
                if (player != null && isMediaPlayerPrepared && currentStep > 0) {
                    val volume = (currentStep.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
                    try {
                        player.setVolume(volume, volume)
                    } catch (_: Exception) {}
                    currentStep--
                    fadeHandler.postDelayed(this, stepIntervalMs)
                } else {
                    // 淡出完成，彻底关停
                    isFadingOut = false
                    try {
                        player?.setVolume(1.0f, 1.0f) // 恢复基础音量设置，供下次起播使用
                    } catch (_: Exception) {}
                    stopPlayback()
                    stopSelf()
                    Handler(Looper.getMainLooper()).post {
                        val tip = if (reason.contains("夜间")) "🌙 处于夜间深度闲置，已自动停止播放" else "🌙 休眠定时结束，已自动停止播放"
                        Toast.makeText(applicationContext, tip, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        fadeHandler.post(fadeRunnable)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSleepTimerEvent(event: BaseEvent) {
        if (event.eventType == EventType.SLEEP_TIMER_PAUSE) {
            val reason = event.eventMessage.ifBlank { "休眠定时" }
            Log.i(TAG, "Received SLEEP_TIMER_PAUSE event: $reason")
            fadeOutAndStop(reason)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fadeHandler.removeCallbacksAndMessages(null)
        EventBusManager.unregister(this)
        stopPlayback()
        mediaSession?.release()
        abandonAudioFocus()
    }
}

