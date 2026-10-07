package com.example.moodymusicforandroid.data.manager

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.data.local.db.DownloadDao
import com.example.moodymusicforandroid.data.local.db.DownloadedSongEntity
import com.example.moodymusicforandroid.data.local.db.MoodyDatabase
import com.example.moodymusicforandroid.data.model.SongItem
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 下载状态密封类
 */
sealed class DownloadStatus {
    object QUEUED : DownloadStatus()
    data class DOWNLOADING(val progress: Float, val currentBytes: Long, val totalBytes: Long) : DownloadStatus()
    object COMPLETED : DownloadStatus()
    data class FAILED(val reason: String) : DownloadStatus()
    object PAUSED : DownloadStatus()
}

/**
 * 内存中运行的下载任务模型
 */
data class DownloadTask(
    val filePath: String,
    val songId: Long = 0L,
    val title: String,
    val artistName: String = "",
    val albumTitle: String = "",
    val coverUrl: String = "",
    val lrcPath: String? = null,
    val fileHash: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED
)

/**
 * 离线音乐全生命周期下载管理器 (Offline Download Manager)
 *
 * 核心设计：
 * 1. 动态并发调度：支持整张专辑一键入队，内部通过 Semaphore(3) 进行并发流控，防止网络拥塞；
 * 2. 状态实时广播：StateFlow<Map<String, DownloadTask>> 驱动专辑条目横向进度条与已下载标签；
 * 3. 私有安全加密落盘：音频流直接写入 SecureAudioStorage (.moody)，歌词同步本地化；
 * 4. 资源 Hash 智能感知：实时对比服务端 file_hash，支持黄色 [可更新] 感知与增量重下；
 * 5. 双向持久化索引：与 Room DownloadDao 强一致联动，支持离线优先秒开与全量治理。
 */
object OfflineDownloadManager {
    private const val TAG = "OfflineDownloadMgr"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadSemaphore = Semaphore(3) // 最大 3 任务并行下载

    @Volatile
    private var isInitialized = false
    private lateinit var appContext: Context
    private lateinit var downloadDao: DownloadDao

    // 内存中正在排队/下载的任务池
    private val taskMap = ConcurrentHashMap<String, DownloadTask>()
    private val jobMap = ConcurrentHashMap<String, Job>()

    private val _tasksFlow = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val tasksFlow: StateFlow<Map<String, DownloadTask>> = _tasksFlow.asStateFlow()

    // 已下载曲目列表流 (直接观察 Room)
    private val _downloadedSongsFlow = MutableStateFlow<List<DownloadedSongEntity>>(emptyList())
    val downloadedSongsFlow: StateFlow<List<DownloadedSongEntity>> = _downloadedSongsFlow.asStateFlow()

    // 内存快速已下载缓存 (多维联合索引，免疫动态签名与 URL 变更)
    private val completedByPath = ConcurrentHashMap<String, DownloadedSongEntity>()
    private val completedBySongId = ConcurrentHashMap<Long, DownloadedSongEntity>()
    private val completedByCanonicalKey = ConcurrentHashMap<String, DownloadedSongEntity>()
    private val completedByTitleAndAlbum = ConcurrentHashMap<String, DownloadedSongEntity>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            appContext = context.applicationContext
            SecureAudioStorage.init(appContext)
            downloadDao = MoodyDatabase.getInstance(appContext).downloadDao()
            isInitialized = true

            // 监听 Room 数据库变化并同步内存多维索引
            scope.launch {
                downloadDao.getAllDownloadedSongs().collect { list ->
                    _downloadedSongsFlow.value = list
                    completedByPath.clear()
                    completedBySongId.clear()
                    completedByCanonicalKey.clear()
                    completedByTitleAndAlbum.clear()
                    list.forEach { entity ->
                        completedByPath[entity.filePath] = entity
                        if (entity.songId > 0L) {
                            completedBySongId[entity.songId] = entity
                        }
                        val canonical = SecureAudioStorage.extractCanonicalResourceKey(entity.filePath)
                        if (canonical.isNotBlank()) {
                            completedByCanonicalKey[canonical] = entity
                        }
                        if (entity.title.isNotBlank() && entity.albumTitle.isNotBlank()) {
                            completedByTitleAndAlbum["${entity.title.trim()}###${entity.albumTitle.trim()}"] = entity
                        }
                    }
                    Log.d(TAG, "CompletedIndex updated: ${list.size} songs cached across multi-key indices.")
                }
            }
            Log.i(TAG, "OfflineDownloadManager successfully initialized.")
        }
    }

    /**
     * 多维联合定位本地已下载歌曲实体（优先级：songId -> canonicalKey -> rawPath -> 标题与专辑）
     */
    fun findDownloadedEntity(
        filePath: String? = null,
        songId: Long? = null,
        title: String? = null,
        albumTitle: String? = null
    ): DownloadedSongEntity? {
        if (songId != null && songId > 0L) {
            completedBySongId[songId]?.let { return it }
        }
        if (!filePath.isNullOrBlank()) {
            val canonical = SecureAudioStorage.extractCanonicalResourceKey(filePath)
            if (canonical.isNotBlank()) {
                completedByCanonicalKey[canonical]?.let { return it }
            }
            completedByPath[filePath]?.let { return it }
        }
        if (!title.isNullOrBlank() && !albumTitle.isNullOrBlank()) {
            completedByTitleAndAlbum["${title.trim()}###${albumTitle.trim()}"]?.let { return it }
        }
        return null
    }

    /**
     * 判断某个音频是否已经在本地完成下载且物理文件存在（支持按 filePath / songId / 标题与专辑 多维匹配）
     */
    fun isDownloaded(
        filePath: String? = null,
        songId: Long? = null,
        title: String? = null,
        albumTitle: String? = null
    ): Boolean {
        val entity = findDownloadedEntity(filePath, songId, title, albumTitle) ?: return false
        val localFile = File(entity.localFilePath)
        return localFile.exists() && localFile.length() > SecureAudioStorage.HEADER_SIZE
    }

    fun isDownloaded(filePath: String?): Boolean = isDownloaded(filePath = filePath, songId = null)

    /**
     * 判断本地已下载的音频与服务端下发的 hash 是否存在差异（即服务端有新版本）
     */
    fun isHashOutdated(
        filePath: String? = null,
        serverHash: String? = null,
        songId: Long? = null,
        title: String? = null,
        albumTitle: String? = null
    ): Boolean {
        if (serverHash.isNullOrBlank()) return false
        val entity = findDownloadedEntity(filePath, songId, title, albumTitle) ?: return false
        return entity.fileHash.isNotBlank() && entity.fileHash != serverHash
    }

    fun isHashOutdated(filePath: String?, serverHash: String?): Boolean =
        isHashOutdated(filePath = filePath, serverHash = serverHash, songId = null)

    /**
     * 获取指定音频在本地已下载的 .moody 文件（供 LocalMediaProxy 优先离线播放）
     */
    fun getDownloadedLocalFile(
        filePath: String? = null,
        songId: Long? = null,
        title: String? = null,
        albumTitle: String? = null
    ): File? {
        val entity = findDownloadedEntity(filePath, songId, title, albumTitle)
        if (entity != null) {
            val localFile = File(entity.localFilePath)
            if (localFile.exists() && localFile.length() > SecureAudioStorage.HEADER_SIZE) {
                return localFile
            }
        }
        if (!filePath.isNullOrBlank()) {
            val secureFile = SecureAudioStorage.getSecureFile(appContext, filePath)
            if (secureFile.exists() && secureFile.length() > SecureAudioStorage.HEADER_SIZE) {
                return secureFile
            }
        }
        return null
    }

    fun getDownloadedLocalFile(filePath: String?): File? =
        getDownloadedLocalFile(filePath = filePath, songId = null)

    /**
     * 将整张专辑中的所有有效曲目一键加入下载队列
     * @return 成功加入队列的任务数
     */
    fun enqueueAlbum(
        songs: List<SongItem>,
        albumTitle: String,
        artistName: String,
        coverUrl: String
    ): Int {
        var enqueuedCount = 0
        songs.forEach { song ->
            val path = song.path
            if (!path.isNullOrBlank()) {
                // 如果已经下载且没有更新，则跳过
                if (!isDownloaded(path) || isHashOutdated(path, song.fileHash)) {
                    enqueueSong(
                        song = song,
                        albumTitle = albumTitle,
                        artistName = artistName,
                        coverUrl = coverUrl
                    )
                    enqueuedCount++
                }
            }
        }
        return enqueuedCount
    }

    /**
     * 单曲加入下载队列
     */
    fun enqueueSong(
        song: SongItem,
        albumTitle: String,
        artistName: String,
        coverUrl: String
    ) {
        val path = song.path ?: return
        if (taskMap[path]?.status is DownloadStatus.DOWNLOADING) {
            return // 正在下载中，无需重复入队
        }

        val task = DownloadTask(
            filePath = path,
            songId = song.id ?: 0L,
            title = song.title,
            artistName = artistName,
            albumTitle = albumTitle,
            coverUrl = coverUrl,
            lrcPath = song.lrcPath,
            fileHash = song.fileHash ?: "",
            status = DownloadStatus.QUEUED
        )
        taskMap[path] = task
        _tasksFlow.value = taskMap.toMap()

        startDownloadWorker(task)
    }

    private fun startDownloadWorker(task: DownloadTask) {
        val filePath = task.filePath
        jobMap[filePath]?.cancel()

        val job = scope.launch {
            try {
                downloadSemaphore.withPermit {
                    // 更新为 DOWNLOADING 状态
                    updateTaskStatus(filePath, DownloadStatus.DOWNLOADING(0f, 0L, 0L))

                    val resolvedUrl = if (filePath.contains("/api/media/stream") && filePath.contains("target=")) {
                        val target = filePath.substringAfter("target=").substringBefore("&")
                        try {
                            val padLength = (4 - target.length % 4) % 4
                            val padded = target + "=".repeat(padLength)
                            val decodedBytes = try {
                                android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
                            } catch (_: Exception) {
                                android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
                            }
                            val rawStr = String(decodedBytes, Charsets.UTF_8)
                            val decoded = java.net.URLDecoder.decode(rawStr, "UTF-8")
                            if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                                Log.i(TAG, "Unpacked stream target to direct R2 URL: $decoded")
                                decoded
                            } else filePath
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to unpack stream target: ${e.message}")
                            filePath
                        }
                    } else {
                        filePath
                    }
                    val encodedUrl = if (resolvedUrl.startsWith("http://") || resolvedUrl.startsWith("https://")) {
                        // 若已是绝对 CDN 直链（包括从签名流提取出的 R2 直链），直接使用原生 CDN 直连，避免 Netlify 函数/代理 10MB/30s 熔断截断
                        android.net.Uri.encode(resolvedUrl, "@#&=*+-_.,:!?()/~'%")
                    } else {
                        val rawUrl = AppConfig.resolveStorageUrl(resolvedUrl)
                        AppConfig.safeEncodeUrl(rawUrl)
                    }
                    Log.i(TAG, "Starting download for 《${task.title}》 from: $encodedUrl")

                    val request = Request.Builder()
                        .url(encodedUrl)
                        .header("User-Agent", "MoodyMusic/1.0 (Offline Downloader)")
                        .build()

                    val response = httpClient.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}: ${response.message} (url: $encodedUrl)")
                    }

                    val body = response.body ?: throw Exception("Empty response body")
                    val contentLength = body.contentLength()
                    val targetMoodyFile = SecureAudioStorage.getSecureFile(appContext, filePath)

                    // 写入带私有头和 XOR 混淆的 .moody 文件
                    SecureAudioStorage.writeSecureAudio(
                        inputStream = body.byteStream(),
                        targetFile = targetMoodyFile,
                        totalAudioBytes = contentLength,
                        onProgress = { current, total ->
                            val progress = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                            updateTaskStatus(filePath, DownloadStatus.DOWNLOADING(progress, current, total))
                        }
                    )

                    // 伴随下载歌词（若存在）
                    var localLrcPath: String? = null
                    if (!task.lrcPath.isNullOrBlank()) {
                        localLrcPath = downloadLrcFile(task.lrcPath)
                    }

                    // 写入 Room 数据库
                    val entity = DownloadedSongEntity(
                        filePath = filePath,
                        songId = task.songId,
                        title = task.title,
                        artistName = task.artistName,
                        albumTitle = task.albumTitle,
                        coverUrl = task.coverUrl,
                        lrcPath = task.lrcPath,
                        fileHash = task.fileHash,
                        localFilePath = targetMoodyFile.absolutePath,
                        localLrcPath = localLrcPath,
                        fileSize = targetMoodyFile.length(),
                        downloadedAt = System.currentTimeMillis()
                    )
                    downloadDao.insertOrUpdate(entity)
                    completedByPath[filePath] = entity
                    if (entity.songId > 0L) completedBySongId[entity.songId] = entity
                    val canonical = SecureAudioStorage.extractCanonicalResourceKey(filePath)
                    if (canonical.isNotBlank()) completedByCanonicalKey[canonical] = entity
                    if (entity.title.isNotBlank() && entity.albumTitle.isNotBlank()) {
                        completedByTitleAndAlbum["${entity.title.trim()}###${entity.albumTitle.trim()}"] = entity
                    }

                    // 更新任务状态为 COMPLETED 并从活动任务列表移除
                    updateTaskStatus(filePath, DownloadStatus.COMPLETED)
                    delay(500)
                    taskMap.remove(filePath)
                    _tasksFlow.value = taskMap.toMap()
                    Log.i(TAG, "Download finished for 《${task.title}》")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled for 《${task.title}》")
                updateTaskStatus(filePath, DownloadStatus.PAUSED)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for 《${task.title}》: ${e.message}", e)
                updateTaskStatus(filePath, DownloadStatus.FAILED(e.message ?: "下载异常"))
            } finally {
                jobMap.remove(filePath)
            }
        }
        jobMap[filePath] = job
    }

    private suspend fun downloadLrcFile(lrcPath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val fullUrl = AppConfig.resolveStorageUrl(lrcPath)
                val safeUrl = AppConfig.safeEncodeUrl(fullUrl)
                val req = Request.Builder().url(safeUrl).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val lrcText = resp.body?.string()
                    if (!lrcText.isNullOrBlank()) {
                        val dir = File(appContext.getExternalFilesDir("secure_audio") ?: appContext.filesDir, "lrc")
                        if (!dir.exists()) dir.mkdirs()
                        val lrcFile = File(dir, "${SecureAudioStorage.getSecureFileName(lrcPath).substringBefore(".moody")}.lrc")
                        lrcFile.writeText(lrcText)
                        return@withContext lrcFile.absolutePath
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to download lrc: ${e.message}")
            }
            null
        }
    }

    private fun updateTaskStatus(filePath: String, status: DownloadStatus) {
        val currentTask = taskMap[filePath] ?: return
        taskMap[filePath] = currentTask.copy(status = status)
        _tasksFlow.value = taskMap.toMap()
    }

    /**
     * 暂停任务
     */
    fun pauseTask(filePath: String) {
        jobMap[filePath]?.cancel()
        updateTaskStatus(filePath, DownloadStatus.PAUSED)
    }

    /**
     * 恢复/重试任务
     */
    fun resumeTask(filePath: String) {
        val task = taskMap[filePath] ?: return
        startDownloadWorker(task)
    }

    /**
     * 取消并移除排队/下载中的任务
     */
    fun cancelTask(filePath: String) {
        jobMap[filePath]?.cancel()
        taskMap.remove(filePath)
        _tasksFlow.value = taskMap.toMap()
    }

    /**
     * 删除已下载完成的离线歌曲（物理清理 .moody 文件 + 删除 Room 记录）
     */
    fun deleteDownloadedSong(filePath: String) {
        scope.launch {
            try {
                val entity = findDownloadedEntity(filePath = filePath) ?: downloadDao.getDownloadedSongByPath(filePath)
                if (entity != null) {
                    val audioFile = File(entity.localFilePath)
                    if (audioFile.exists()) {
                        audioFile.delete()
                    }
                    if (!entity.localLrcPath.isNullOrBlank()) {
                        val lrcFile = File(entity.localLrcPath)
                        if (lrcFile.exists()) {
                            lrcFile.delete()
                        }
                    }
                    downloadDao.deleteByFilePath(entity.filePath)
                    completedByPath.remove(entity.filePath)
                    if (entity.songId > 0L) completedBySongId.remove(entity.songId)
                    val canonical = SecureAudioStorage.extractCanonicalResourceKey(entity.filePath)
                    if (canonical.isNotBlank()) completedByCanonicalKey.remove(canonical)
                    completedByTitleAndAlbum.remove("${entity.title.trim()}###${entity.albumTitle.trim()}")
                    Log.i(TAG, "Deleted offline song: ${entity.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete offline song: ${e.message}", e)
            }
        }
    }

    /**
     * 一键清空所有已下载歌曲与物理文件
     */
    fun clearAllDownloads() {
        scope.launch {
            try {
                val allSongs = downloadDao.getAllDownloadedSongsList()
                allSongs.forEach { entity ->
                    File(entity.localFilePath).takeIf { it.exists() }?.delete()
                    entity.localLrcPath?.let { File(it).takeIf { f -> f.exists() }?.delete() }
                }
                downloadDao.deleteAll()
                completedByPath.clear()
                completedBySongId.clear()
                completedByCanonicalKey.clear()
                completedByTitleAndAlbum.clear()
                Log.i(TAG, "Cleared all offline downloads.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all downloads: ${e.message}", e)
            }
        }
    }
}
