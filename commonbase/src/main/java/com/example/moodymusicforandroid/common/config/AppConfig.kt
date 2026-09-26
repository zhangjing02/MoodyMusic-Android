package com.example.moodymusicforandroid.common.config

import android.net.Uri
import com.example.moodymusicforandroid.commonbase.BuildConfig

/**
 * 全局应用配置中心 (Single Source of Truth)
 * 所有网络基准地址与相对资源解析均由此类统一分发，配置源由 gradle.properties 驱动。
 */
object AppConfig {

    /**
     * 默认本地开发回退地址
     */
    const val BASE_URL_DEV = "http://127.0.0.1:8787/"

    /**
     * 全局 API 生产基准地址（自动以 '/' 结尾）
     * 由 gradle.properties MOODY_API_BASE_URL 驱动
     */
    val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    /**
     * 国内备用 API 端点（反向代理中继，应对运营商 4G 对 Cloudflare 的 SNI 封锁）
     *
     * 当直连 apiBaseUrl 遭遇 SocketException(Connection reset) 时，
     * RetrofitClient 可自动切换到此地址重试。
     *
     * 配置方式：在 gradle.properties 中添加：
     *   MOODY_API_FALLBACK_URL=https://你的国内中继域名/
     *
     * 未配置时为空字符串，不启用容灾切换。
     */
    val apiFallbackUrl: String
        get() {
            val raw = try { BuildConfig.API_FALLBACK_URL } catch (_: Exception) { "" }
            return if (raw.isNullOrBlank()) "" else raw.trimEnd('/') + "/"
        }

    /**
     * 版本更新检测独立生命线端点（第三级容灾保障）
     *
     * 专门用于当主业务网关与备用网关均发生异常时，作为最后底线拉取版本更新信息，
     * 保证客户端永远能够收到新版升级提示或动态域名广播。
     */
    val apiLifelineUrl: String
        get() {
            val raw = try { BuildConfig.API_LIFELINE_URL } catch (_: Exception) { "" }
            return if (raw.isNullOrBlank()) "" else raw.trimEnd('/') + "/"
        }

    /**
     * 需要自动收敛收编的被阻断旧域名列表
     * 历史遗留或数据库存量数据中若含有这些域名，将全自动平移至当前合法 apiBaseUrl
     */
    private val LEGACY_BLOCKED_DOMAINS = listOf(
        "m-api.changgepd.ccwu.cc",
        "r2.changgepd.ccwu.cc",
        "m-api.changgepd.top"
    )

    /**
     * 核心收敛方法：将包含历史被墙域名的绝对 URL 透明平移映射为当前可用的网关地址
     */
    fun canonicalizeUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        var url = rawUrl.trim()
        val targetBase = apiBaseUrl.trimEnd('/')

        for (blockedHost in LEGACY_BLOCKED_DOMAINS) {
            if (url.contains(blockedHost, ignoreCase = true)) {
                url = url.replace("https://$blockedHost", targetBase, ignoreCase = true)
                    .replace("http://$blockedHost", targetBase, ignoreCase = true)
            }
        }
        return url
    }

    /**
     * 辅助方法：对 URL 进行 RFC 3986 安全字符转义，兼容中文字符与空格，避免 Stagefright/MediaPlayer 抛出 I/O 错误。
     */
    fun safeEncodeUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val canonical = canonicalizeUrl(url)
        if (!canonical.startsWith("http://") && !canonical.startsWith("https://")) return canonical
        return Uri.encode(canonical, "@#&=*+-_.,:!?()/~'%")
    }

    /**
     * 辅助方法：将相对资源路径（例如 "/storage/covers/hero.jpg" 或 "storage/..."）
     * 自动拼接为带域名的完整 URL，并进行安全转义与旧域名动态收敛。
     */
    fun resolveUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("file:///") || path.startsWith("content://") ||
            path.startsWith("android.resource://")
        ) {
            return path
        }
        val canonical = canonicalizeUrl(path)
        val fullUrl = if (canonical.startsWith("http://") || canonical.startsWith("https://")) {
            canonical
        } else {
            val cleanPath = if (canonical.startsWith("/")) canonical else "/$canonical"
            "${apiBaseUrl.trimEnd('/')}$cleanPath"
        }
        return safeEncodeUrl(fullUrl)
    }

    /**
     * 辅助方法：将媒体与歌词资源路径（例如 "lyrics/xxx.lrc" 或 "storage/lyrics/xxx.lrc"）
     * 规范化并拼接为带有 /storage/ 的完整访问 URL，与 Web 端保持一致。
     */
    fun resolveStorageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("file:///") || path.startsWith("content://")) {
            return path
        }
        val canonical = canonicalizeUrl(path)
        if (canonical.startsWith("http://") || canonical.startsWith("https://")) {
            return safeEncodeUrl(canonical)
        }
        val trimmed = canonical.trim().trimStart('/')
        val finalPath = if (trimmed.startsWith("storage/")) trimmed else "storage/$trimmed"
        return resolveUrl(finalPath)
    }
}
