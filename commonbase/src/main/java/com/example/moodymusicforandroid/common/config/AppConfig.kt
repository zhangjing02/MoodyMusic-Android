package com.example.moodymusicforandroid.common.config

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
     */
    val apiBaseUrl: String
        get() = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    /**
     * 辅助方法：将相对资源路径（例如 "/storage/covers/hero.jpg" 或 "storage/..."）
     * 自动拼接为带域名的完整 URL。如果传入的已经是绝对 URL 则原样返回。
     */
    fun resolveUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("file:///") || path.startsWith("content://") ||
            path.startsWith("android.resource://")
        ) {
            return path
        }
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "${BuildConfig.API_BASE_URL.trimEnd('/')}$cleanPath"
    }

    /**
     * 辅助方法：将媒体与歌词资源路径（例如 "lyrics/xxx.lrc" 或 "storage/lyrics/xxx.lrc"）
     * 规范化并拼接为带有 /storage/ 的完整访问 URL，与 Web 端保持一致。
     */
    fun resolveStorageUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("file:///") || path.startsWith("content://")
        ) {
            return path
        }
        val trimmed = path.trim().trimStart('/')
        val finalPath = if (trimmed.startsWith("storage/")) trimmed else "storage/$trimmed"
        return resolveUrl(finalPath)
    }
}
