package com.example.moodymusicforandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

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
 * 单个主题色在浅色 / 深色主题下的 primary 和 onPrimary 色值
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
 * 全量主题色彩映射表
 * 预设官方雅致推荐色，并提供自选调色盘色彩的科学映射生成器
 */
object SongbookAccentPalettes {

    /** 经典咖棕（官方推荐 / 原版默认：焦橙 #6C2F00） */
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

    /** 复古墨绿（森系雅致：#2E563E） */
    val Forest = AccentPalette(
        lightPrimary             = Color(0xFF2E563E),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFF3D6D50),
        lightOnPrimaryContainer  = Color(0xFFD3F5DF),
        darkPrimary              = Color(0xFF85CFA2),
        darkOnPrimary            = Color(0xFF00381C),
        darkPrimaryContainer     = Color(0xFF1B4D35),
        darkOnPrimaryContainer   = Color(0xFFD3F5DF),
    )
    val Sage = Forest // 向后兼容

    /** 静谧黛蓝（北欧冷调：#204D74） */
    val Ocean = AccentPalette(
        lightPrimary             = Color(0xFF204D74),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFF2D6596),
        lightOnPrimaryContainer  = Color(0xFFD2E6FD),
        darkPrimary              = Color(0xFF8EBAE5),
        darkOnPrimary            = Color(0xFF002F53),
        darkPrimaryContainer     = Color(0xFF0F3A60),
        darkOnPrimaryContainer   = Color(0xFFD2E6FD),
    )

    /** 晚霞暮绯（复古枫红：#9C3636） */
    val Sunset = AccentPalette(
        lightPrimary             = Color(0xFF9C3636),
        lightOnPrimary           = Color.White,
        lightPrimaryContainer    = Color(0xFFBC4747),
        lightOnPrimaryContainer  = Color(0xFFFFDAD9),
        darkPrimary              = Color(0xFFFF9E9E),
        darkOnPrimary            = Color(0xFF560007),
        darkPrimaryContainer     = Color(0xFF731B1E),
        darkOnPrimaryContainer   = Color(0xFFFFDAD9),
    )

    /**
     * 基于色彩学第一性原理的自适应 Palette 生成器
     * 将用户在调色盘中自由选择的任意基础色，科学映射为符合《现代颂歌》纸质美学与 Material 3 对比度规范的完整 Palette。
     */
    fun fromCustomColor(baseColor: Color): AccentPalette {
        val argb = baseColor.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        val hue = hsv[0]
        val sat = hsv[1]
        val value = hsv[2]

        // 1. 浅色模式 Primary：
        // 若饱和度过低且亮度极高（近乎纯白），适度微调防止在米白纸质背景上隐形
        val lightPrimary = if (value > 0.88f && sat < 0.25f) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.65f)))
        } else {
            baseColor
        }

        // 浅色模式 onPrimary：根据感知亮度自适应 (Rec. 601)
        val luminance = lightPrimary.red * 0.299f + lightPrimary.green * 0.587f + lightPrimary.blue * 0.114f
        val lightOnPrimary = if (luminance > 0.62f) Color(0xFF1B1C1A) else Color.White

        // 浅色容器色：降低饱和度、适度提高明度
        val lightContainer = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, (sat * 0.45f).coerceIn(0.15f, 0.50f), (value * 0.95f).coerceIn(0.70f, 0.95f))
            )
        )
        val lightOnContainer = if (luminance > 0.62f) Color(0xFF1B1C1A) else Color(0xFF1B1C1A)

        // 2. 深色模式 Primary（核心）：
        // 在纯暗背景下，主色必须具有温润发光的高对比度（提高 Value 至 0.88~0.96，收敛 Saturation 至 0.35~0.55）
        val darkPrimary = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, (sat * 0.55f).coerceIn(0.32f, 0.55f), (value + 0.50f).coerceIn(0.88f, 0.98f))
            )
        )
        val darkOnPrimary = Color(0xFF1E140A)

        // 深色容器色：基色本身或略降明度的沉着色
        val darkContainer = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, (sat * 0.8f).coerceIn(0.40f, 0.80f), (value * 0.5f).coerceIn(0.25f, 0.55f))
            )
        )
        val darkOnContainer = Color(
            android.graphics.Color.HSVToColor(
                floatArrayOf(hue, 0.25f, 0.95f)
            )
        )

        return AccentPalette(
            lightPrimary = lightPrimary,
            lightOnPrimary = lightOnPrimary,
            lightPrimaryContainer = lightContainer,
            lightOnPrimaryContainer = lightOnContainer,
            darkPrimary = darkPrimary,
            darkOnPrimary = darkOnPrimary,
            darkPrimaryContainer = darkContainer,
            darkOnPrimaryContainer = darkOnContainer
        )
    }

    /**
     * 根据 AccentColor 枚举与自选颜色获取对应 Palette
     */
    fun of(
        accentColor: com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor,
        customColorArgb: Int? = null
    ): AccentPalette {
        if (accentColor == com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.CUSTOM && customColorArgb != null) {
            return fromCustomColor(Color(customColorArgb))
        }
        return when (accentColor) {
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.MOCHA  -> Mocha
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.FOREST -> Forest
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.OCEAN  -> Ocean
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.SUNSET -> Sunset
            com.example.moodymusicforandroid.common.utils.AppThemeManager.AccentColor.CUSTOM -> Mocha
        }
    }
}
