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
    private const val KEY_CUSTOM_COLOR_ARGB = "custom_color_argb"

    // ─── 主题色枚举 ───────────────────────────────────────────────────────────
    /**
     * 主题色调方案
     * 预设雅致推荐色，并支持自由调色盘 CUSTOM 模式
     */
    enum class AccentColor(val key: String, val displayName: String, val defaultHex: String) {
        /** 经典咖棕（官方推荐 / 原版经典焦橙） */
        MOCHA("mocha", "经典咖棕", "#6C2F00"),
        /** 复古墨绿（森系雅致） */
        FOREST("forest", "复古墨绿", "#2E563E"),
        /** 静谧黛蓝（北欧冷调） */
        OCEAN("ocean", "静谧黛蓝", "#204D74"),
        /** 晚霞暮绯（复古枫红） */
        SUNSET("sunset", "晚霞暮绯", "#9C3636"),
        /** 自由调色盘自定义色彩 */
        CUSTOM("custom", "自由调色", "");

        companion object {
            // 向后兼容旧版 SAGE 引用
            val SAGE: AccentColor get() = FOREST

            fun fromKey(key: String): AccentColor =
                when (key) {
                    "mocha" -> MOCHA
                    "forest", "sage" -> FOREST
                    "ocean" -> OCEAN
                    "sunset" -> SUNSET
                    "custom" -> CUSTOM
                    else -> MOCHA
                }
        }
    }

    // ─── 主题配置数据类 ────────────────────────────────────────────────────────
    /**
     * 主题配置快照
     * @param isDark           true = 黑色主题；false = 白色主题
     * @param accentColor      主题色方案
     * @param customColorArgb  用户通过调色盘自由调制的 ARGB 整数（accentColor 为 CUSTOM 时生效）
     */
    data class ThemeConfig(
        val isDark: Boolean = false,
        val accentColor: AccentColor = AccentColor.MOCHA,
        val customColorArgb: Int? = null
    )

    // ─── StateFlow（Compose 层订阅） ──────────────────────────────────────────
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
        val accentColor = AccentColor.fromKey(accentKey)
        val customArgb = if (prefs.contains(KEY_CUSTOM_COLOR_ARGB)) prefs.getInt(KEY_CUSTOM_COLOR_ARGB, 0) else null
        _config.value = ThemeConfig(
            isDark = isDark,
            accentColor = accentColor,
            customColorArgb = if (accentColor == AccentColor.CUSTOM) customArgb else null
        )
    }

    // ─── 切换深色/浅色 ────────────────────────────────────────────────────────
    fun setDarkMode(isDark: Boolean, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK, isDark).apply()
        _config.value = _config.value.copy(isDark = isDark)
    }

    // ─── 切换预设主题色 ────────────────────────────────────────────────────────
    fun setAccentColor(accentColor: AccentColor, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACCENT_COLOR, accentColor.key).apply()
        _config.value = _config.value.copy(
            accentColor = accentColor,
            customColorArgb = if (accentColor == AccentColor.CUSTOM) _config.value.customColorArgb else null
        )
    }

    // ─── 设置自由调色盘自定义颜色 ──────────────────────────────────────────────
    fun setCustomColor(colorArgb: Int, context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACCENT_COLOR, AccentColor.CUSTOM.key)
            .putInt(KEY_CUSTOM_COLOR_ARGB, colorArgb)
            .apply()
        _config.value = _config.value.copy(
            accentColor = AccentColor.CUSTOM,
            customColorArgb = colorArgb
        )
    }

    // ─── 便捷只读属性（在非 Compose 层使用） ─────────────────────────────────
    val isDark: Boolean get() = _config.value.isDark
    val accentColor: AccentColor get() = _config.value.accentColor
    val customColorArgb: Int? get() = _config.value.customColorArgb
}
