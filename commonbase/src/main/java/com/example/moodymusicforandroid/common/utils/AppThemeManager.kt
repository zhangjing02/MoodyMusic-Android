package com.example.moodymusicforandroid.common.utils

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局主题状态管理中心 (AppThemeManager)
 *
 * 职责：
 * 1. 管理黑/白主题（isDark）切换
 * 2. 管理强调色（AccentColor）切换
 * 3. 通过 StateFlow 向 Compose 层广播变更，实现无 recreate 的即时响应
 * 4. 持久化到 SharedPreferences，重启恢复状态
 *
 * 使用方式：
 *   // 在 Application.onCreate() 初始化
 *   AppThemeManager.init(this)
 *
 *   // 在 SongbookTheme 中观察
 *   val config by AppThemeManager.config.collectAsState()
 *
 *   // 切换深色模式
 *   AppThemeManager.setDarkMode(true, context)
 *
 *   // 切换强调色
 *   AppThemeManager.setAccentColor(AppThemeManager.AccentColor.MOCHA, context)
 */
object AppThemeManager {

    private const val PREFS_NAME = "app_theme_prefs"
    private const val KEY_IS_DARK = "is_dark_mode"
    private const val KEY_ACCENT_COLOR = "accent_color"

    // ─── 强调色枚举 ───────────────────────────────────────────────────────────
    /**
     * 强调色方案
     * 每种颜色包含浅色/深色主题下的 primary 色值，以字符串 key 存入 SharedPreferences
     */
    enum class AccentColor(val key: String, val displayName: String) {
        /** 咖棕（当前默认：焦橙 BurntOrange） */
        MOCHA("mocha", "咖棕"),
        /** 鼠尾草绿（Sage Green） */
        SAGE("sage", "苔绿"),
        /** 海洋蓝（Ocean Blue） */
        OCEAN("ocean", "深蓝"),
        /** 日落橙红（Sunset Red） */
        SUNSET("sunset", "夕橙");

        companion object {
            fun fromKey(key: String): AccentColor =
                entries.firstOrNull { it.key == key } ?: MOCHA
        }
    }

    // ─── 主题配置数据类 ────────────────────────────────────────────────────────
    /**
     * 主题配置快照
     * @param isDark      true = 黑色主题；false = 白色主题
     * @param accentColor 强调色方案
     */
    data class ThemeConfig(
        val isDark: Boolean = false,
        val accentColor: AccentColor = AccentColor.MOCHA
    )

    // ─── StateFlow（Compose 层订阅这个） ──────────────────────────────────────
    private val _config = MutableStateFlow(ThemeConfig())
    val config: StateFlow<ThemeConfig> = _config.asStateFlow()

    // ─── 初始化 ───────────────────────────────────────────────────────────────
    /**
     * 在 Application.onCreate() 中调用，从本地持久化恢复主题配置
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean(KEY_IS_DARK, false)
        val accentKey = prefs.getString(KEY_ACCENT_COLOR, AccentColor.MOCHA.key) ?: AccentColor.MOCHA.key
        _config.value = ThemeConfig(
            isDark = isDark,
            accentColor = AccentColor.fromKey(accentKey)
        )
    }

    // ─── 切换深色/浅色 ────────────────────────────────────────────────────────
    /**
     * 切换黑/白主题
     * @param isDark true = 切换为黑色主题；false = 切换为白色主题
     * @param context 用于写入 SharedPreferences
     */
    fun setDarkMode(isDark: Boolean, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
        _config.value = _config.value.copy(isDark = isDark)
    }

    // ─── 切换强调色 ───────────────────────────────────────────────────────────
    /**
     * 切换强调色
     * @param accentColor 目标强调色方案
     * @param context 用于写入 SharedPreferences
     */
    fun setAccentColor(accentColor: AccentColor, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCENT_COLOR, accentColor.key).apply()
        _config.value = _config.value.copy(accentColor = accentColor)
    }

    // ─── 便捷只读属性（在非 Compose 层使用） ─────────────────────────────────
    val isDark: Boolean get() = _config.value.isDark
    val accentColor: AccentColor get() = _config.value.accentColor
}
