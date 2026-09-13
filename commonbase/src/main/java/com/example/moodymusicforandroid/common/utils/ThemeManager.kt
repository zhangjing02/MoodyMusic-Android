package com.example.moodymusicforandroid.common.utils

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * 主题管理类
 * 管理应用的主题切换
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "theme_mode"

    /**
     * 主题模式
     */
    enum class ThemeMode(val value: Int) {
        DEFAULT(0),    // 默认绿色
        OCEAN(1),      // 海洋蓝
        SUNSET(2),     // 日落橙
        NIGHT(3);      // 暗夜紫

        companion object {
            private val VALUES = values()
            fun fromValue(value: Int): ThemeMode = VALUES.firstOrNull { it.value == value } ?: DEFAULT
        }
    }

    private var cachedThemeMode: ThemeMode? = null

    /**
     * 获取当前主题
     */
    fun getTheme(context: Context): ThemeMode {
        cachedThemeMode?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val themeValue = prefs.getInt(KEY_THEME, ThemeMode.DEFAULT.value)
        val mode = ThemeMode.fromValue(themeValue)
        cachedThemeMode = mode
        return mode
    }

    /**
     * 设置主题
     */
    fun setTheme(context: Context, themeMode: ThemeMode) {
        cachedThemeMode = themeMode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME, themeMode.value).apply()

        // 应用主题
        applyTheme(themeMode)
    }

    /**
     * 应用主题
     */
    private fun applyTheme(themeMode: ThemeMode) {
        when (themeMode) {
            ThemeMode.DEFAULT -> {
                // 使用默认主题
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ThemeMode.OCEAN -> {
                // 海洋蓝主题
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ThemeMode.SUNSET -> {
                // 日落橙主题
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            ThemeMode.NIGHT -> {
                // 暗夜紫主题（暗黑模式）
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    /**
     * 初始化主题
     */
    fun initTheme(context: Context) {
        val themeMode = getTheme(context)
        applyTheme(themeMode)
    }

    /**
     * 在 Activity 的 onCreate 中，调用 super.onCreate 之前应用保存的主题
     * @deprecated 使用 initTheme() 代替
     */
    fun applyTheme(activity: Activity) {
        val themeMode = getTheme(activity)
        if (themeMode == ThemeMode.NIGHT) {
            activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat_DayNight)
        }
    }

    /**
     * 切换主题，保存并重启 Activity
     * @deprecated 使用 setTheme() 代替
     */
    fun switchTheme(activity: Activity, themeResId: Int) {
        // 这个方法已废弃，保留是为了兼容性
        activity.recreate()
    }
}
