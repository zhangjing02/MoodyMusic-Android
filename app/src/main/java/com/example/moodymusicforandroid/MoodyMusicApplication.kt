package com.example.moodymusicforandroid

import android.app.Application
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.manager.UserManager

import com.example.moodymusicforandroid.common.utils.FontManager
import com.example.moodymusicforandroid.common.utils.ThemeManager

import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application 类
 * 用于初始化全局配置和组件
 */
class MoodyMusicApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(false)
            .allowHardware(false) // 必须设为 false，否则在含 BlurView/软件渲染绘制中会触发 'Software rendering doesn't support hardware bitmaps' 崩溃
            .memoryCache {
                coil.memory.MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(80L * 1024 * 1024)
                    .build()
            }
            .build()
    }

    companion object {
        @JvmStatic
        var currentThemeResId: Int = R.style.Theme_MoodyMusicForAndroid_Green_Handwriting
            private set
    }

    override fun onCreate() {
        super.onCreate()

        // 初始化 PreferencesManager
        PreferencesManager.init(this)

        // 初始化 UserManager (本地 SQLite 数据库与用户状态中枢)
        UserManager.init(this)

        // 初始化 PlaylistManager (自定义播放列表管理中枢)
        com.example.moodymusicforandroid.data.manager.PlaylistManager.init(this)

        // 初始化主题
        ThemeManager.initTheme(this)

        // 应用组合主题（字体 + 颜色）
        applyCombinedTheme()

        // 异步预热默认字体与后台服务
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                androidx.core.content.res.ResourcesCompat.getFont(this@MoodyMusicApplication, R.font.lxgw_wenkai_gb_regular)
            } catch (_: Exception) {}

            try {
                cn.jpush.android.api.JPushInterface.setDebugMode(false)
                cn.jpush.android.api.JPushInterface.init(this@MoodyMusicApplication)

                // 如果用户已登录，主动上报当前 RegistrationId（处理 App 重启场景）
                // onRegister 回调仅在 ID 首次生成或变更时触发，
                // App 重启后 ID 通常已存在，需主动获取并上报
                val regId = cn.jpush.android.api.JPushInterface.getRegistrationID(this@MoodyMusicApplication)
                if (!regId.isNullOrEmpty()) {
                    PreferencesManager.saveJPushRegistrationId(regId)
                    if (PreferencesManager.isLoggedIn()) {
                        try {
                            com.example.moodymusicforandroid.data.api.MoodyApiProvider.apiService
                                .updateJPushRegistrationId(mapOf("jpush_registration_id" to regId))
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * 应用组合主题（字体风格 + 颜色主题）
     * 保持 FontManager 和 ThemeManager 逻辑独立，在此组合
     */
    private fun applyCombinedTheme() {
        val fontStyle = FontManager.getFontStyle(this)
        val colorTheme = ThemeManager.getTheme(this)

        currentThemeResId = when (colorTheme) {
            ThemeManager.ThemeMode.DEFAULT -> {
                when (fontStyle) {
                    FontManager.FontStyle.HANDWRITING -> R.style.Theme_MoodyMusicForAndroid_Green_Handwriting
                    FontManager.FontStyle.MODERN -> R.style.Theme_MoodyMusicForAndroid_Green_Modern
                    FontManager.FontStyle.ELEGANT -> R.style.Theme_MoodyMusicForAndroid_Green_Elegant
                }
            }
            ThemeManager.ThemeMode.OCEAN -> {
                when (fontStyle) {
                    FontManager.FontStyle.HANDWRITING -> R.style.Theme_MoodyMusicForAndroid_Ocean_Handwriting
                    FontManager.FontStyle.MODERN -> R.style.Theme_MoodyMusicForAndroid_Ocean_Modern
                    FontManager.FontStyle.ELEGANT -> R.style.Theme_MoodyMusicForAndroid_Ocean_Elegant
                }
            }
            ThemeManager.ThemeMode.SUNSET -> {
                when (fontStyle) {
                    FontManager.FontStyle.HANDWRITING -> R.style.Theme_MoodyMusicForAndroid_Sunset_Handwriting
                    FontManager.FontStyle.MODERN -> R.style.Theme_MoodyMusicForAndroid_Sunset_Modern
                    FontManager.FontStyle.ELEGANT -> R.style.Theme_MoodyMusicForAndroid_Sunset_Elegant
                }
            }
            ThemeManager.ThemeMode.NIGHT -> {
                when (fontStyle) {
                    FontManager.FontStyle.HANDWRITING -> R.style.Theme_MoodyMusicForAndroid_Night_Handwriting
                    FontManager.FontStyle.MODERN -> R.style.Theme_MoodyMusicForAndroid_Night_Modern
                    FontManager.FontStyle.ELEGANT -> R.style.Theme_MoodyMusicForAndroid_Night_Elegant
                }
            }
        }
    }

    /**
     * 更新主题（供外部调用）
     * 重新计算组合主题
     */
    fun updateTheme() {
        applyCombinedTheme()
    }
}
