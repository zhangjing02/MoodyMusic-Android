package com.example.moodymusicforandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The Modern Songbook (现代颂歌) 杂志风色彩系统
 * 遵循 Stitch DESIGN.md 规范与天然、低饱和、温暖的纸质排版色彩哲学。
 */
object SongbookColors {
    // 核心基础色 (Core Palette)
    val PaperBackground = Color(0xFFFBF9F5)       // 温暖米白纸质底色
    val PaperBackgroundDark = Color(0xFF141513)   // 温暖暗调纸质底色
    val SoftCharcoal = Color(0xFF1B1C1A)          // 软炭黑正文字体与主图标
    val SoftCharcoalDark = Color(0xFFF2F0ED)      // 暗色模式下的米白正文

    // 标志性主强调色 (Burnt Orange & Terracotta)
    val BurntOrange = Color(0xFF6C2F00)           // 焦橙 - 标志性主操作、高光
    val BurntOrangeLight = Color(0xFFFFB68C)      // 浅焦橙 (Dark mode primary)
    val TerracottaBrown = Color(0xFF8B4513)       // 陶土棕 - 专题标签与深度强调
    val TerracottaLight = Color(0xFFFFC29F)       // 浅陶土棕

    // 次级大地色 (Muted Olive & Natural Accents)
    val MutedOlive = Color(0xFF496800)            // 橄榄绿 - 收藏、自然生态感
    val MutedOliveLight = Color(0xFFADD461)       // 浅橄榄绿
    val OliveContainer = Color(0xFFC8F17A)        // 橄榄绿容器色
    val OnOliveContainer = Color(0xFF4E6E00)

    // 表面层级 (Surface Hierarchy - 替代传统生硬投影)
    val SurfaceLowest = Color(0xFFFFFFFF)         // 纯白高光卡片 (聚焦层)
    val SurfaceLow = Color(0xFFF5F3EF)            // 次级低层容器 (输入框、卡片衬底)
    val Surface = Color(0xFFFBF9F5)               // 标准表面
    val SurfaceHigh = Color(0xFFEAE8E4)           // 高对比卡片表面
    val SurfaceHighest = Color(0xFFE4E2DE)        // 顶层容器 / 标签背景
    val SurfaceDim = Color(0xFFDBDAD6)            // 暗化表面

    // 边框与微结构 (The "Ghost Border" & Outlines)
    val Outline = Color(0xFF877369)               // 轮廓基准色
    val OutlineVariant = Color(0xFFDAC2B6)        // 浅轮廓色
    val GhostBorder = Color(0x26DAC2B6)           // 15% 幽灵边框
    val GhostBorderActive = Color(0x336C2F00)     // 20% 聚焦态焦橙幽灵边框

    // 暗色模式表面层级
    val SurfaceDarkLowest = Color(0xFF0F100E)
    val SurfaceDarkLow = Color(0xFF1B1C1A)
    val SurfaceDark = Color(0xFF222320)
    val SurfaceDarkHigh = Color(0xFF2B2C29)
    val SurfaceDarkHighest = Color(0xFF363733)
    val OutlineDark = Color(0xFFA08C82)
    val OutlineVariantDark = Color(0xFF54433A)
}

/**
 * 浅色主题配色方案
 */
val SongbookLightColorScheme: ColorScheme = lightColorScheme(
    primary = SongbookColors.BurntOrange,
    onPrimary = Color.White,
    primaryContainer = SongbookColors.TerracottaBrown,
    onPrimaryContainer = SongbookColors.TerracottaLight,
    secondary = SongbookColors.MutedOlive,
    onSecondary = Color.White,
    secondaryContainer = SongbookColors.OliveContainer,
    onSecondaryContainer = SongbookColors.OnOliveContainer,
    tertiary = Color(0xFF653400),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF874800),
    onTertiaryContainer = Color(0xFFFFC393),
    background = SongbookColors.PaperBackground,
    onBackground = SongbookColors.SoftCharcoal,
    surface = SongbookColors.Surface,
    onSurface = SongbookColors.SoftCharcoal,
    surfaceVariant = SongbookColors.SurfaceHighest,
    onSurfaceVariant = Color(0xFF54433A),
    surfaceContainerLowest = SongbookColors.SurfaceLowest,
    surfaceContainerLow = SongbookColors.SurfaceLow,
    surfaceContainer = SongbookColors.Surface,
    surfaceContainerHigh = SongbookColors.SurfaceHigh,
    surfaceContainerHighest = SongbookColors.SurfaceHighest,
    outline = SongbookColors.Outline,
    outlineVariant = SongbookColors.OutlineVariant,
    inverseSurface = Color(0xFF30312E),
    inverseOnSurface = Color(0xFFF2F0ED),
    inversePrimary = SongbookColors.BurntOrangeLight
)

/**
 * 暗色主题配色方案
 */
val SongbookDarkColorScheme: ColorScheme = darkColorScheme(
    primary = SongbookColors.BurntOrangeLight,
    onPrimary = Color(0xFF3A1500),
    primaryContainer = Color(0xFF8B4513),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = SongbookColors.MutedOliveLight,
    onSecondary = Color(0xFF131F00),
    secondaryContainer = Color(0xFF364E00),
    onSecondaryContainer = SongbookColors.OliveContainer,
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF2E1500),
    tertiaryContainer = Color(0xFF874800),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = SongbookColors.PaperBackgroundDark,
    onBackground = SongbookColors.SoftCharcoalDark,
    surface = SongbookColors.SurfaceDark,
    onSurface = SongbookColors.SoftCharcoalDark,
    surfaceVariant = SongbookColors.SurfaceDarkHighest,
    onSurfaceVariant = Color(0xFFDAC2B6),
    surfaceContainerLowest = SongbookColors.SurfaceDarkLowest,
    surfaceContainerLow = SongbookColors.SurfaceDarkLow,
    surfaceContainer = SongbookColors.SurfaceDark,
    surfaceContainerHigh = SongbookColors.SurfaceDarkHigh,
    surfaceContainerHighest = SongbookColors.SurfaceDarkHighest,
    outline = SongbookColors.OutlineDark,
    outlineVariant = SongbookColors.OutlineVariantDark,
    inverseSurface = Color(0xFFF2F0ED),
    inverseOnSurface = Color(0xFF1B1C1A),
    inversePrimary = SongbookColors.BurntOrange
)

/**
 * 自定义扩展颜色配置
 */
@Immutable
data class ExtendedColors(
    val paperBackground: Color = SongbookColors.PaperBackground,
    val softCharcoal: Color = SongbookColors.SoftCharcoal,
    val terracotta: Color = SongbookColors.TerracottaBrown,
    val mutedOlive: Color = SongbookColors.MutedOlive,
    val ghostBorder: Color = SongbookColors.GhostBorder
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// ─────────────────────────────────────────────────────────────────────────────
// 强调色 Palette（每种强调色在浅色/深色主题下的配色）
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 单个强调色在浅色 / 深色主题下的 primary 和 onPrimary 色值
 */
data class AccentPalette(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
)

/**
 * 全量强调色映射表
 * 通过 AppThemeManager.AccentColor 枚举 key 获取对应的 Palette
 */
object SongbookAccentPalettes {

    /** 咖棕（默认）—— 焦橙 BurntOrange 系 */
    val Mocha = AccentPalette(
        lightPrimary             = Color(0xFF6C2F00),   // BurntOrange
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFF8B4513),   // TerracottaBrown
        lightOnPrimaryContainer  = Color(0xFFFFDBC9),
        darkPrimary              = Color(0xFFFFB68C),   // BurntOrangeLight
        darkOnPrimary            = Color(0xFF3A1500),
        darkPrimaryContainer     = Color(0xFF8B4513),
        darkOnPrimaryContainer   = Color(0xFFFFDBC9),
    )

    /** 苔绿（Sage）—— 深鼠尾草绿系 */
    val Sage = AccentPalette(
        lightPrimary             = Color(0xFF34614D),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFF4C7965),
        lightOnPrimaryContainer  = Color(0xFFD6FFE9),
        darkPrimary              = Color(0xFF8ACDA0),
        darkOnPrimary            = Color(0xFF003920),
        darkPrimaryContainer     = Color(0xFF1B4D35),
        darkOnPrimaryContainer   = Color(0xFFD6FFE9),
    )

    /** 深蓝（Ocean）—— 海洋蓝系 */
    val Ocean = AccentPalette(
        lightPrimary             = Color(0xFF0052A5),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFF1A6BC4),
        lightOnPrimaryContainer  = Color(0xFFD1E8FF),
        darkPrimary              = Color(0xFF9ECAFF),
        darkOnPrimary            = Color(0xFF003063),
        darkPrimaryContainer     = Color(0xFF004891),
        darkOnPrimaryContainer   = Color(0xFFD1E8FF),
    )

    /** 夕橙（Sunset）—— 日落橙红系 */
    val Sunset = AccentPalette(
        lightPrimary             = Color(0xFFC94B00),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFFE06020),
        lightOnPrimaryContainer  = Color(0xFFFFDCC2),
        darkPrimary              = Color(0xFFFFB691),
        darkOnPrimary            = Color(0xFF5B1900),
        darkPrimaryContainer     = Color(0xFF7D2D00),
        darkOnPrimaryContainer   = Color(0xFFFFDCC2),
    )

    /**
     * 根据 AccentColor 枚举获取对应 Palette
     */
    fun of(accentColor: com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor): AccentPalette {
        return when (accentColor) {
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.MOCHA  -> Mocha
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.SAGE   -> Sage
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.OCEAN  -> Ocean
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.SUNSET -> Sunset
        }
    }
}
