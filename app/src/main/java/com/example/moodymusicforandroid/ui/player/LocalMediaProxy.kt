package com.example.moodymusicforandroid.ui.player

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.moodymusicforandroid.common.config.AppConfig
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * 本地极速音频流媒体代理 (Local Media Streaming Proxy)
 *
 * 核心设计原理:
 * 1. Android 原生 MediaPlayer (C++ NuPlayer 引擎) 强行绕过 Java 发起 IPv6 导致 Anycast 丢包假死，
 *    通过 127.0.0.1 环回代理阻断 IPv6 黑洞，强制走优选纯 IPv4 通道；
 * 2. 数据流动感知 (Data-Flow Heartbeat): 实时跟踪底层 OkHttp 数据吞吐，防止看门狗误杀正在全速缓冲的大音频；
 * 3. 本地磁盘 LRU 智能缓存 (Disk Cache): 一旦下载完成自动落盘，切歌/单曲循环/跳转时 0ms 瞬间秒开；
 * 4. 连接池热身 (Connection Prewarming): 消除首首歌冷启动的 DNS 与 TLS 握手延时。
 */
object LocalMediaProxy {
    private const val TAG = "LocalMediaProxy"

    private var serverSocket: ServerSocket? = null
    private var port: Int = 0

    // A-B1 修复: 将 newCachedThreadPool() 无界线程池替换为有界线程池
    // 规避极端弱网环境下请求积压导致线程无限制创建引发的 OOM/ANR
    private val executor = ThreadPoolExecutor(
        4,                          // 核心线程数
        16,                         // 最大线程数
        60L, TimeUnit.SECONDS,      // 空闲线程存活时间
        LinkedBlockingQueue(64),    // 有界排队队列
        ThreadPoolExecutor.CallerRunsPolicy() // 队列饱和时由调用线程执行，平滑限流降级
    )

    @Volatile
    private var isRunning = false

    // 数据流动感知指标 (供 Watchdog 动态心跳探测)
    @Volatile
    var lastDataTransferTime: Long = 0L
        private set

    @Volatile
    var totalBytesTransferred: Long = 0L
        private set

    @Volatile
    var isStreamingActive: Boolean = false
        private set

    /**
     * 判断当前是否正在持续传输数据（非卡死断流状态）
     */
    fun isActivelyTransferring(withinMs: Long = 4000L): Boolean {
        return isStreamingActive && (System.currentTimeMillis() - lastDataTransferTime <= withinMs)
    }

    // 本地磁盘缓存目录（按 500MB LRU 上限维护）
    private var cacheDir: File? = null
    private const val MAX_CACHE_SIZE_BYTES = 500L * 1024 * 1024 // 500 MB

    fun init(context: Context) {
        if (cacheDir == null) {
            cacheDir = File(context.cacheDir, "audio_cache").apply {
                if (!exists()) mkdirs()
            }
            Log.i(TAG, "LocalMediaProxy cache initialized at: ${cacheDir?.absolutePath}")
        }
        com.example.moodymusicforandroid.data.manager.OfflineDownloadManager.init(context)
    }

    private fun getCacheKey(url: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(url.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            url.hashCode().toString()
        }
    }

    // 本地长效 DNS 缓存（避免 4G 下反复经历系统 DNS 的 408 超时）
    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()

    // R2 存储桶专用的 Cloudflare Anycast 优质 IPv4 节点池
    private val R2_FALLBACK_IPS by lazy {
        listOf(
            "104.18.50.34",
            "104.18.54.45",
            "172.64.32.1",
            "104.16.1.1"
        ).mapNotNull {
            try { InetAddress.getByName(it) } catch (_: Exception) { null }
        }
    }

    // 专用于流媒体代理的纯 IPv4 OkHttpClient
    private val okHttpClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 20
        }
        val pool = ConnectionPool(20, 5, TimeUnit.MINUTES)

        OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectionPool(pool)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    // 1. 优先命中内存长效 DNS 缓存
                    dnsCache[hostname]?.let { cached ->
                        if (cached.isNotEmpty()) {
                            return cached
                        }
                    }

                    // 2. 尝试系统 DNS 并严格过滤纯 IPv4
                    val result = try {
                        val addresses = Dns.SYSTEM.lookup(hostname)
                        val ipv4List = addresses.filterIsInstance<Inet4Address>()
                        if (ipv4List.isNotEmpty()) ipv4List else addresses
                    } catch (e: Exception) {
                        Log.w(TAG, "System DNS lookup failed/timed out for $hostname: ${e.message}")
                        emptyList()
                    }

                    if (result.isNotEmpty()) {
                        dnsCache[hostname] = result
                        return result
                    }

                    // 3. 若为 R2 桶域名且系统 DNS 失败，启用 Cloudflare Anycast 优质 IPv4 兜底
                    if (hostname.endsWith("r2.dev", ignoreCase = true) && R2_FALLBACK_IPS.isNotEmpty()) {
                        Log.i(TAG, "Using R2 Anycast IPv4 fallback pool for $hostname")
                        dnsCache[hostname] = R2_FALLBACK_IPS
                        return R2_FALLBACK_IPS
                    }

                    return Dns.SYSTEM.lookup(hostname)
                }
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Synchronized
    fun start(): Int {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
            return port
        }
        try {
            serverSocket = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            port = serverSocket!!.localPort
            isRunning = true
            Log.i(TAG, "LocalMediaProxy successfully started on 127.0.0.1:$port")

            executor.execute {
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        executor.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client socket", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LocalMediaProxy", e)
        }
        return port
    }

    /**
     * 将远端媒体 URL 转换为本地代理 URL
     */
    fun getProxyUrl(rawUrl: String): String {
        if (rawUrl.isBlank()) return rawUrl
        if (rawUrl.startsWith("http://127.0.0.1") || rawUrl.startsWith("file://") || rawUrl.startsWith("/")) {
            return rawUrl
        }
        val currentPort = start()
        if (currentPort <= 0) return rawUrl

        return "http://127.0.0.1:$currentPort/proxy?url=" + URLEncoder.encode(rawUrl, "UTF-8")
    }

    /**
     * 异步预热连接池与 DNS 解析，消灭首曲冷启动 2~3 秒延迟
     */
    fun prewarmConnection(url: String? = null) {
        if (!url.isNullOrBlank() && com.example.moodymusicforandroid.data.manager.OfflineDownloadManager.isDownloaded(url)) {
            Log.d(TAG, "Prewarm skipped: audio is already downloaded locally")
            return
        }
        executor.execute {
            try {
                val warmUrl = if (!url.isNullOrBlank()) {
                    AppConfig.safeEncodeUrl(url)
                } else {
                    "${AppConfig.apiBaseUrl}api/home/feed"
                }
                val req = Request.Builder()
                    .url(warmUrl)
                    .head()
                    .header("User-Agent", "MoodyMusic-Android/1.0 (Prewarm)")
                    .header("X-App-Platform", "android")
                    .header("X-Client-Type", "android")
                    .header("Referer", AppConfig.apiBaseUrl)
                    .build()
                okHttpClient.newCall(req).execute().close()
                Log.d(TAG, "Prewarm connection established to: $warmUrl")
            } catch (e: Exception) {
                Log.d(TAG, "Prewarm completed: ${e.message}")
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        var tempPartFile: File? = null
        try {
            clientSocket.soTimeout = 45000 // 放宽本地套接字超时至 45 秒，避免慢速网络卡顿断开
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream(), Charsets.US_ASCII))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0] // GET 或 HEAD
            val requestPath = parts[1]

            // 解析客户端 Range 请求头
            var rangeHeader: String? = null
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.startsWith("Range:", ignoreCase = true)) {
                    rangeHeader = line.substring(6).trim()
                }
                line = reader.readLine()
            }

            val uri = Uri.parse("http://127.0.0.1" + requestPath)
            val rawTargetUrl = uri.getQueryParameter("url")
            if (rawTargetUrl.isNullOrBlank()) {
                sendError(clientSocket, 400, "Bad Request: missing target url")
                return
            }

            // 规整 URL（彻底消除可能存在的 %25 多重编码隐患）
            var cleanTargetUrl: String = rawTargetUrl
            while (cleanTargetUrl.contains("%25")) {
                try {
                    val decoded = java.net.URLDecoder.decode(cleanTargetUrl, "UTF-8")
                    if (!decoded.isNullOrEmpty()) {
                        cleanTargetUrl = decoded
                    } else {
                        break
                    }
                } catch (_: Exception) { break }
            }

            val safeTargetUrl = AppConfig.safeEncodeUrl(cleanTargetUrl)

            // 0. 本地离线下载安全私有文件命中检查 (Offline-First, 零网络流量秒播)
            val offlineFile = com.example.moodymusicforandroid.data.manager.OfflineDownloadManager.getDownloadedLocalFile(cleanTargetUrl)
                ?: com.example.moodymusicforandroid.data.manager.OfflineDownloadManager.getDownloadedLocalFile(rawTargetUrl)
            if (offlineFile != null && offlineFile.exists() && offlineFile.length() > com.example.moodymusicforandroid.data.manager.SecureAudioStorage.HEADER_SIZE) {
                Log.i(TAG, "Offline Secure Download HIT for: $cleanTargetUrl, serving from local encrypted file: ${offlineFile.name}")
                serveFromMoodyFile(clientSocket, offlineFile, method, rangeHeader)
                return
            }

            // 1. 本地磁盘缓存命中检查 (Cache-First)
            val cacheKey = getCacheKey(safeTargetUrl)
            val cachedFile = cacheDir?.let { File(it, "$cacheKey.mp3") }
            if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                Log.i(TAG, "Cache HIT for: $safeTargetUrl, serving from disk (${cachedFile.length()} bytes)")
                serveFromCache(clientSocket, cachedFile, method, rangeHeader)
                return
            }

            Log.i(TAG, "Proxying $method for: $safeTargetUrl (Range: $rangeHeader)")

            // 2. 上游网络抓取
            fun buildRequest(url: String): Request {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", "MoodyMusic-Android/1.0 (Android Native Player Proxy)")
                    .header("X-App-Platform", "android")
                    .header("X-Client-Type", "android")
                    .header("Referer", AppConfig.apiBaseUrl)
                if (rangeHeader != null) {
                    reqBuilder.header("Range", rangeHeader)
                }
                if (method.equals("HEAD", ignoreCase = true)) {
                    reqBuilder.head()
                } else {
                    reqBuilder.get()
                }
                return reqBuilder.build()
            }

            val response = try {
                okHttpClient.newCall(buildRequest(safeTargetUrl)).execute()
            } catch (e: Exception) {
                if (safeTargetUrl != cleanTargetUrl) {
                    Log.w(TAG, "Relay request failed (${e.message}), falling back to: $cleanTargetUrl")
                    okHttpClient.newCall(buildRequest(cleanTargetUrl)).execute()
                } else {
                    throw e
                }
            }

            response.use { resp ->
                val out = clientSocket.getOutputStream()
                val writer = OutputStreamWriter(out, Charsets.US_ASCII)

                val statusCode = resp.code
                val statusMessage = resp.message.ifBlank {
                    when (statusCode) {
                        200 -> "OK"
                        206 -> "Partial Content"
                        else -> "Response"
                    }
                }

                writer.write("HTTP/1.1 $statusCode $statusMessage\r\n")
                var upstreamContentLength = -1L

                resp.headers.forEach { (name, value) ->
                    val lowerName = name.lowercase()
                    if (lowerName == "content-type" ||
                        lowerName == "content-length" ||
                        lowerName == "content-range" ||
                        lowerName == "accept-ranges" ||
                        lowerName == "etag" ||
                        lowerName == "last-modified"
                    ) {
                        writer.write("$name: $value\r\n")
                        if (lowerName == "content-length") {
                            upstreamContentLength = value.toLongOrNull() ?: -1L
                        }
                    }
                }
                writer.write("Connection: close\r\n")
                writer.write("\r\n")
                writer.flush()

                if (!method.equals("HEAD", ignoreCase = true)) {
                    // 若为全量请求，同步写入临时文件进行本地持久缓存
                    val shouldCache = (statusCode == 200) && (rangeHeader == null || rangeHeader == "bytes=0-")
                    val tempFile = if (shouldCache && cacheDir != null) File(cacheDir, "$cacheKey.part") else null
                    tempPartFile = tempFile

                    val fileOut = if (tempFile != null) {
                        try { FileOutputStream(tempFile) } catch (_: Exception) { null }
                    } else null

                    isStreamingActive = true
                    lastDataTransferTime = System.currentTimeMillis()
                    var bytesWritten = 0L

                    try {
                        resp.body?.byteStream()?.use { input ->
                            val buffer = ByteArray(65536)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                out.write(buffer, 0, bytesRead)
                                fileOut?.write(buffer, 0, bytesRead)
                                lastDataTransferTime = System.currentTimeMillis()
                                totalBytesTransferred += bytesRead
                                bytesWritten += bytesRead
                            }
                        }
                        out.flush()
                        fileOut?.flush()
                    } finally {
                        fileOut?.close()
                    }

                    // 校验并完成缓存落盘
                    if (shouldCache && tempFile != null && tempFile.exists()) {
                        val isComplete = (upstreamContentLength <= 0L) || (bytesWritten >= upstreamContentLength)
                        if (isComplete && cachedFile != null) {
                            if (tempFile.renameTo(cachedFile)) {
                                Log.i(TAG, "Song successfully cached to disk: ${cachedFile.name} (${cachedFile.length()} bytes)")
                                tempPartFile = null
                                trimCacheIfNeeded()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 切歌或快速切换时播放器主动断开是正常行为，清理未写完的临时文件
            tempPartFile?.let { if (it.exists()) it.delete() }
            Log.d(TAG, "Proxy client connection closed: ${e.message}")
        } finally {
            isStreamingActive = false
            try {
                clientSocket.close()
            } catch (_: Exception) {}
        }
    }

    private fun serveFromCache(
        clientSocket: Socket,
        file: File,
        method: String,
        rangeHeader: String?
    ) {
        val fileLength = file.length()
        val out = clientSocket.getOutputStream()
        val writer = OutputStreamWriter(out, Charsets.US_ASCII)

        isStreamingActive = true
        lastDataTransferTime = System.currentTimeMillis()

        try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val rangeSpec = rangeHeader.substring(6).trim()
                val rangeParts = rangeSpec.split("-")
                val start = rangeParts[0].toLongOrNull() ?: 0L
                val end = if (rangeParts.size > 1 && rangeParts[1].isNotEmpty()) {
                    rangeParts[1].toLongOrNull() ?: (fileLength - 1)
                } else {
                    fileLength - 1
                }
                val lengthToServe = (end - start + 1).coerceAtLeast(0)

                writer.write("HTTP/1.1 206 Partial Content\r\n")
                writer.write("Content-Type: audio/mpeg\r\n")
                writer.write("Content-Length: $lengthToServe\r\n")
                writer.write("Content-Range: bytes $start-$end/$fileLength\r\n")
                writer.write("Accept-Ranges: bytes\r\n")
                writer.write("Connection: close\r\n\r\n")
                writer.flush()

                if (!method.equals("HEAD", ignoreCase = true)) {
                    RandomAccessFile(file, "r").use { raf ->
                        raf.seek(start)
                        val buffer = ByteArray(65536)
                        var remaining = lengthToServe
                        while (remaining > 0) {
                            val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            val read = raf.read(buffer, 0, toRead)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            remaining -= read
                            lastDataTransferTime = System.currentTimeMillis()
                            totalBytesTransferred += read
                        }
                        out.flush()
                    }
                }
            } else {
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: audio/mpeg\r\n")
                writer.write("Content-Length: $fileLength\r\n")
                writer.write("Accept-Ranges: bytes\r\n")
                writer.write("Connection: close\r\n\r\n")
                writer.flush()

                if (!method.equals("HEAD", ignoreCase = true)) {
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(65536)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            out.write(buffer, 0, read)
                            lastDataTransferTime = System.currentTimeMillis()
                            totalBytesTransferred += read
                        }
                        out.flush()
                    }
                }
            }
        } finally {
            isStreamingActive = false
        }
    }

    /**
     * 专属流式输出本地已下载的 .moody 安全混淆文件 (0ms 秒开 + 零网络消耗 + 支持 Range 206 快进)
     */
    private fun serveFromMoodyFile(
        clientSocket: Socket,
        file: File,
        method: String,
        rangeHeader: String?
    ) {
        val audioLength = com.example.moodymusicforandroid.data.manager.SecureAudioStorage.getOriginalAudioLength(file)
        val out = clientSocket.getOutputStream()
        val writer = OutputStreamWriter(out, Charsets.US_ASCII)

        isStreamingActive = true
        lastDataTransferTime = System.currentTimeMillis()

        try {
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                val rangeSpec = rangeHeader.substring(6).trim()
                val rangeParts = rangeSpec.split("-")
                val start = rangeParts[0].toLongOrNull() ?: 0L
                val end = if (rangeParts.size > 1 && rangeParts[1].isNotEmpty()) {
                    rangeParts[1].toLongOrNull() ?: (audioLength - 1)
                } else {
                    audioLength - 1
                }
                val lengthToServe = (end - start + 1).coerceAtLeast(0)

                writer.write("HTTP/1.1 206 Partial Content\r\n")
                writer.write("Content-Type: audio/mpeg\r\n")
                writer.write("Content-Length: $lengthToServe\r\n")
                writer.write("Content-Range: bytes $start-$end/$audioLength\r\n")
                writer.write("Accept-Ranges: bytes\r\n")
                writer.write("Connection: close\r\n\r\n")
                writer.flush()

                if (!method.equals("HEAD", ignoreCase = true)) {
                    RandomAccessFile(file, "r").use { raf ->
                        val buffer = ByteArray(65536)
                        var remaining = lengthToServe
                        var currentOffset = start
                        while (remaining > 0) {
                            val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            val read = com.example.moodymusicforandroid.data.manager.SecureAudioStorage.readAudioRange(
                                raf = raf,
                                audioOffset = currentOffset,
                                buffer = buffer,
                                bufferOffset = 0,
                                length = toRead
                            )
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            remaining -= read
                            currentOffset += read
                            lastDataTransferTime = System.currentTimeMillis()
                            totalBytesTransferred += read
                        }
                        out.flush()
                    }
                }
            } else {
                writer.write("HTTP/1.1 200 OK\r\n")
                writer.write("Content-Type: audio/mpeg\r\n")
                writer.write("Content-Length: $audioLength\r\n")
                writer.write("Accept-Ranges: bytes\r\n")
                writer.write("Connection: close\r\n\r\n")
                writer.flush()

                if (!method.equals("HEAD", ignoreCase = true)) {
                    RandomAccessFile(file, "r").use { raf ->
                        val buffer = ByteArray(65536)
                        var remaining = audioLength
                        var currentOffset = 0L
                        while (remaining > 0) {
                            val toRead = remaining.coerceAtMost(buffer.size.toLong()).toInt()
                            val read = com.example.moodymusicforandroid.data.manager.SecureAudioStorage.readAudioRange(
                                raf = raf,
                                audioOffset = currentOffset,
                                buffer = buffer,
                                bufferOffset = 0,
                                length = toRead
                            )
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                            remaining -= read
                            currentOffset += read
                            lastDataTransferTime = System.currentTimeMillis()
                            totalBytesTransferred += read
                        }
                        out.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Offline streaming client disconnected: ${e.message}")
        } finally {
            isStreamingActive = false
        }
    }

    private fun trimCacheIfNeeded() {
        val dir = cacheDir ?: return
        executor.execute {
            try {
                val files = dir.listFiles { f -> f.extension == "mp3" } ?: return@execute
                var totalSize = files.sumOf { it.length() }
                if (totalSize > MAX_CACHE_SIZE_BYTES) {
                    files.sortBy { it.lastModified() }
                    for (f in files) {
                        if (totalSize <= MAX_CACHE_SIZE_BYTES * 0.8) break
                        val len = f.length()
                        if (f.delete()) {
                            totalSize -= len
                            Log.d(TAG, "LRU Evicted cache file: ${f.name}")
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendError(socket: Socket, code: Int, msg: String) {
        try {
            val writer = OutputStreamWriter(socket.getOutputStream(), Charsets.US_ASCII)
            writer.write("HTTP/1.1 $code $msg\r\nConnection: close\r\n\r\n$msg")
            writer.flush()
            socket.close()
        } catch (_: Exception) {}
    }
}
