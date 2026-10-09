package com.example.moodymusicforandroid.data.manager

import android.content.Context
import android.util.Log
import java.io.*
import java.security.MessageDigest

/**
 * 离线音频私有安全加密存储引擎 (Secure Audio Storage)
 *
 * 核心架构特性：
 * 1. 专属私有存储：保存在应用沙盒 getExternalFilesDir("secure_audio")，无需多余权限，避免被系统相册/外部播放器扫描；
 * 2. 私有混淆封装 (.moody)：
 *    - 前 16 字节写入专用私有魔数头 "MOODY_SECURE_V1\0\0"
 *    - 前 64KB 音频帧执行位异或 (XOR) 动态掩码混淆，彻底破坏标准 MP3/M4A 头部帧同步字
 *    - 任何外部播放器或文件管理器直接读取均识别为“格式损坏”，保护正版资产防提取
 * 3. 零开销流式解混淆 (Streaming Deobfuscation)：
 *    - 播放时由 LocalMediaProxy 直接按 HTTP Range 206 边读边解混淆输出
 *    - 无需全量在磁盘上解密释放明文，内存占用极低，支持毫秒级拖动快进 (Seek)
 */
object SecureAudioStorage {
    private const val TAG = "SecureAudioStorage"

    // 16 字节私有魔数头
    val MAGIC_HEADER = byteArrayOf(
        'M'.code.toByte(), 'O'.code.toByte(), 'O'.code.toByte(), 'D'.code.toByte(),
        'Y'.code.toByte(), '_'.code.toByte(), 'S'.code.toByte(), 'E'.code.toByte(),
        'C'.code.toByte(), 'U'.code.toByte(), 'R'.code.toByte(), 'E'.code.toByte(),
        '_'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte(), 0.toByte()
    )
    const val HEADER_SIZE = 16

    // 前 64KB 关键帧混淆深度
    const val OBFUSCATE_DEPTH = 65536

    // 混淆掩码盐
    private const val XOR_SALT = 0x5A

    @Volatile
    private var storageDir: File? = null

    fun init(context: Context) {
        if (storageDir == null) {
            val dir = context.getExternalFilesDir("secure_audio")
                ?: File(context.filesDir, "secure_audio")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            storageDir = dir
            Log.i(TAG, "SecureAudioStorage initialized at: ${dir.absolutePath}")
        }
    }

    fun getStorageDirectory(context: Context): File {
        if (storageDir == null) {
            init(context)
        }
        return storageDir ?: File(context.filesDir, "secure_audio")
    }

    /**
     * 提取稳定的唯一资源键（消除 URL 中的动态时间戳 exp 与短效 HMAC 签名 sig 影响）
     */
    fun extractCanonicalResourceKey(url: String?): String {
        if (url.isNullOrBlank()) return ""
        // 1. 如果是流式签名代理地址，提取稳定的 target 参数
        if (url.contains("/api/media/stream") && url.contains("target=")) {
            val query = url.substringAfter('?', "")
            val targetParam = query.split('&').firstOrNull { it.startsWith("target=") }?.substringAfter("target=")
            if (!targetParam.isNullOrBlank()) {
                return "stream_target:$targetParam"
            }
        }
        // 2. 如果包含标准音频直链文件名 (如 s_31688.mp3)
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val fileName = cleanUrl.substringAfterLast('/')
        if (fileName.contains('.')) {
            val ext = fileName.substringAfterLast('.', "")
            if (ext.equals("mp3", ignoreCase = true) || ext.equals("m4a", ignoreCase = true) || ext.equals("flac", ignoreCase = true)) {
                return "file:$fileName"
            }
        }
        return cleanUrl
    }

    /**
     * 将流媒体 target 还原为直链真实地址（如果无法还原或不是流媒体则返回 null）
     */
    fun unpackStreamTarget(url: String?): String? {
        if (url.isNullOrBlank() || !url.contains("/api/media/stream") || !url.contains("target=")) return null
        val query = url.substringAfter('?', "")
        val targetParam = query.split('&').firstOrNull { it.startsWith("target=") }?.substringAfter("target=")
        if (targetParam.isNullOrBlank()) return null
        return try {
            val padLength = (4 - targetParam.length % 4) % 4
            val padded = targetParam + "=".repeat(padLength)
            val decodedBytes = try {
                android.util.Base64.decode(padded, android.util.Base64.URL_SAFE)
            } catch (_: Exception) {
                android.util.Base64.decode(padded, android.util.Base64.DEFAULT)
            }
            val rawStr = String(decodedBytes, Charsets.UTF_8)
            val decoded = java.net.URLDecoder.decode(rawStr, "UTF-8")
            if (decoded.startsWith("http://") || decoded.startsWith("https://")) decoded else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 提取实际音频文件名（如 snow_cafe_piano.mp3），必须包含合法音频后缀，绝不允许 Generic 路径名如 "stream"
     */
    fun extractAudioFileName(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val cleanUrl = (unpackStreamTarget(url) ?: url).substringBefore('?').substringBefore('#')
        val fileName = cleanUrl.substringAfterLast('/')
        if (fileName.contains('.')) {
            val ext = fileName.substringAfterLast('.', "")
            if (ext.equals("mp3", ignoreCase = true) || ext.equals("m4a", ignoreCase = true) ||
                ext.equals("flac", ignoreCase = true) || ext.equals("wav", ignoreCase = true) ||
                ext.equals("aac", ignoreCase = true) || ext.equals("ogg", ignoreCase = true)
            ) {
                return fileName
            }
        }
        return null
    }

    /**
     * 判断两个音频地址是否指向同一个真实音频实体资源
     * 彻底消灭将 Generic API 路径 (如 /api/media/stream) 的 "stream" 误作为文件名引发的全局假阳性对比
     */
    fun isSameAudioResource(url1: String?, url2: String?): Boolean {
        if (url1.isNullOrBlank() || url2.isNullOrBlank()) return false
        if (url1 == url2) return true

        // 1. 尝试比对稳定的 Canonical Key (支持相同签名参数或相同 target)
        val key1 = extractCanonicalResourceKey(url1)
        val key2 = extractCanonicalResourceKey(url2)
        if (key1.isNotBlank() && key2.isNotBlank() && key1 == key2) {
            // 严防任何不带具体参数的通用路径片段碰撞
            if (key1 != "stream" && !key1.endsWith("/stream")) {
                return true
            }
        }

        // 2. 尝试从 target 解包真实文件名对比（跨直链与签名流对比）
        val file1 = extractAudioFileName(url1)
        val file2 = extractAudioFileName(url2)
        if (!file1.isNullOrBlank() && !file2.isNullOrBlank() && file1.equals(file2, ignoreCase = true)) {
            return true
        }

        return false
    }


    /**
     * 根据远程 URL 生成唯一文件名 (.moody)
     */
    fun getSecureFileName(url: String): String {
        val canonicalKey = extractCanonicalResourceKey(url)
        val keyToHash = if (canonicalKey.isNotBlank()) canonicalKey else url
        val md5 = try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(keyToHash.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            keyToHash.hashCode().toString()
        }
        return "$md5.moody"
    }

    /**
     * 获取对应的本地 .moody 文件对象
     */
    fun getSecureFile(context: Context, url: String): File {
        val dir = getStorageDirectory(context)
        return File(dir, getSecureFileName(url))
    }

    /**
     * 校验文件是否为合法的 Moody 私有加密音频文件
     */
    fun isSecureMoodyFile(file: File): Boolean {
        if (!file.exists() || file.length() < HEADER_SIZE) return false
        try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(HEADER_SIZE)
                raf.readFully(header)
                return header.contentEquals(MAGIC_HEADER)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect file magic: ${e.message}")
            return false
        }
    }

    /**
     * 获取原始音频的实际有效字节数 (排除 16 字节私有头)
     */
    fun getOriginalAudioLength(file: File): Long {
        if (!file.exists()) return 0L
        val total = file.length()
        return (total - HEADER_SIZE).coerceAtLeast(0L)
    }

    /**
     * 将输入流写入带有魔数头与头部混淆的 .moody 文件
     */
    @Throws(IOException::class)
    fun writeSecureAudio(
        inputStream: InputStream,
        targetFile: File,
        totalAudioBytes: Long = -1L,
        onProgress: ((currentBytes: Long, totalBytes: Long) -> Unit)? = null
    ) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.downloading")
        var bytesWritten = 0L

        try {
            FileOutputStream(tempFile).use { out ->
                // 1. 写入 16 字节专用魔数头
                out.write(MAGIC_HEADER)

                // 2. 边读边混淆写入音频内容
                val buffer = ByteArray(32768)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    // 判断当前读入区间是否有落在前 64KB 混淆深度内
                    val currentStart = bytesWritten
                    val currentEnd = bytesWritten + read

                    if (currentStart < OBFUSCATE_DEPTH) {
                        val obfuscateLimit = (OBFUSCATE_DEPTH - currentStart).coerceAtMost(read.toLong()).toInt()
                        for (i in 0 until obfuscateLimit) {
                            val audioOffset = (currentStart + i).toInt()
                            // 动态 XOR 掩码：XOR_SALT ^ (audioOffset & 0xFF)
                            val mask = (XOR_SALT xor (audioOffset and 0xFF)).toByte()
                            buffer[i] = (buffer[i].toInt() xor mask.toInt()).toByte()
                        }
                    }

                    out.write(buffer, 0, read)
                    bytesWritten += read
                    onProgress?.invoke(bytesWritten, totalAudioBytes)
                }
                out.flush()
            }

            // 写入完毕后原子重命名
            if (targetFile.exists()) {
                targetFile.delete()
            }
            if (!tempFile.renameTo(targetFile)) {
                throw IOException("Failed to rename temporary download file to ${targetFile.name}")
            }
            Log.i(TAG, "Successfully saved secure audio: ${targetFile.name} (audio $bytesWritten bytes)")
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }
    }

    /**
     * 按 Range 读取并实时流式解混淆 (供 LocalMediaProxy 边读边解边发)
     *
     * @param file 本地 .moody 文件
     * @param audioOffset 原始音频的起始偏移量 (0 代表第 0 字节音频)
     * @param buffer 目标缓冲数组
     * @param bufferOffset 目标缓冲写入偏移
     * @param length 本次期望读取的最大字节数
     * @return 实际读取字节数
     */
    fun readAudioRange(
        raf: RandomAccessFile,
        audioOffset: Long,
        buffer: ByteArray,
        bufferOffset: Int,
        length: Int
    ): Int {
        // 映射到底层物理文件时，seek 需要加上 HEADER_SIZE
        val physicalSeek = HEADER_SIZE + audioOffset
        raf.seek(physicalSeek)
        val read = raf.read(buffer, bufferOffset, length)
        if (read <= 0) return read

        // 若本次读取区间落在了前 64KB 混淆深度内，执行逆向 XOR 还原
        val currentStart = audioOffset
        if (currentStart < OBFUSCATE_DEPTH) {
            val deobfuscateLimit = (OBFUSCATE_DEPTH - currentStart).coerceAtMost(read.toLong()).toInt()
            for (i in 0 until deobfuscateLimit) {
                val offsetInAudio = (currentStart + i).toInt()
                val mask = (XOR_SALT xor (offsetInAudio and 0xFF)).toByte()
                val targetIndex = bufferOffset + i
                buffer[targetIndex] = (buffer[targetIndex].toInt() xor mask.toInt()).toByte()
            }
        }

        return read
    }
}
