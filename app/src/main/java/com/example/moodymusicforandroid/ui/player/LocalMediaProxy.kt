package com.example.moodymusicforandroid.ui.player

import android.net.Uri
import android.util.Log
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLEncoder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 本地极速音频流媒体代理 (Local Media Streaming Proxy)
 *
 * 核心设计原理:
 * Android 原生 MediaPlayer (C++ NuPlayer 引擎) 在连 WiFi 且具备 IPv6 时，会强行绕过 Java 层
 * 优先向远端域名发起 IPv6 握手。国内电信/联通/移动骨干网至 Cloudflare Anycast IPv6 存在严重丢包/黑洞，
 * 导致 Linux 内核必须空等 20 秒超时后才降级到 IPv4。
 *
 * 本代理在 127.0.0.1 启动轻量级本地环回服务:
 * 1. MediaPlayer -> 127.0.0.1 (0.1ms 本地环回握手，彻底杜绝 IPv6 黑洞)
 * 2. 代理服务通过专门定制的纯 IPv4 OkHttpClient 进行上游数据抓取 (支持 Range 分片与流式转发)
 * 3. 完美兼容全平台所有 R2 存储桶直链，首次点击即在 1~2 秒内秒开播放。
 */
object LocalMediaProxy {
    private const val TAG = "LocalMediaProxy"

    private var serverSocket: ServerSocket? = null
    private var port: Int = 0
    private val executor = Executors.newCachedThreadPool()

    @Volatile
    private var isRunning = false

    // 专用于流媒体代理的纯 IPv4 OkHttpClient
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    return try {
                        val addresses = Dns.SYSTEM.lookup(hostname)
                        val ipv4List = addresses.filterIsInstance<Inet4Address>()
                        if (ipv4List.isNotEmpty()) {
                            Log.d(TAG, "DNS IPv4-only for $hostname -> ${ipv4List.map { it.hostAddress }}")
                            ipv4List
                        } else {
                            addresses
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "DNS lookup fallback for $hostname: ${e.message}")
                        Dns.SYSTEM.lookup(hostname)
                    }
                }
            })
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Synchronized
    fun start(): Int {
        if (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
            return port
        }
        try {
            // 绑定 127.0.0.1 环回接口，端口 0 由系统自动分配空闲可用端口
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
        // 如果已经是本地代理 URL 或本地文件则不重复代理
        if (rawUrl.startsWith("http://127.0.0.1") || rawUrl.startsWith("file://") || rawUrl.startsWith("/")) {
            return rawUrl
        }
        val currentPort = start()
        if (currentPort <= 0) return rawUrl

        return "http://127.0.0.1:$currentPort/proxy?url=" + URLEncoder.encode(rawUrl, "UTF-8")
    }

    private fun handleClient(clientSocket: Socket) {
        try {
            clientSocket.soTimeout = 15000
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
            val targetUrl = uri.getQueryParameter("url")
            if (targetUrl.isNullOrBlank()) {
                sendError(clientSocket, 400, "Bad Request: missing target url")
                return
            }

            Log.i(TAG, "Proxying $method for: $targetUrl (Range: $rangeHeader)")

            // 构造上游纯 IPv4 OkHttp 请求
            val reqBuilder = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "MoodyMusic/1.0 (Android Native Player Proxy)")
            if (rangeHeader != null) {
                reqBuilder.header("Range", rangeHeader)
            }
            if (method.equals("HEAD", ignoreCase = true)) {
                reqBuilder.head()
            } else {
                reqBuilder.get()
            }

            val response = okHttpClient.newCall(reqBuilder.build()).execute()
            val out = clientSocket.getOutputStream()
            val writer = OutputStreamWriter(out, Charsets.US_ASCII)

            val statusCode = response.code
            val statusMessage = response.message.ifBlank {
                when (statusCode) {
                    200 -> "OK"
                    206 -> "Partial Content"
                    else -> "Response"
                }
            }

            writer.write("HTTP/1.1 $statusCode $statusMessage\r\n")
            // 转发音频流所需关键响应头
            response.headers.forEach { (name, value) ->
                val lowerName = name.lowercase()
                if (lowerName == "content-type" ||
                    lowerName == "content-length" ||
                    lowerName == "content-range" ||
                    lowerName == "accept-ranges" ||
                    lowerName == "etag" ||
                    lowerName == "last-modified"
                ) {
                    writer.write("$name: $value\r\n")
                }
            }
            writer.write("Connection: close\r\n")
            writer.write("\r\n")
            writer.flush()

            if (!method.equals("HEAD", ignoreCase = true)) {
                response.body?.byteStream()?.use { input ->
                    input.copyTo(out)
                }
            }
            out.flush()
            response.close()
        } catch (e: Exception) {
            // 用户切歌或拖动进度条时，播放器会主动断开前一个连接，属正常行为
            Log.d(TAG, "Proxy client connection closed: ${e.message}")
        } finally {
            try {
                clientSocket.close()
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
