package com.example.moodymusicforandroid.ui.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.moodymusicforandroid.common.utils.AppThemeManager

/**
 * The Modern Songbook 全局主题组件
 *
 * 改造点（相比旧版）：
 * 1. 由 isSystemInDarkTheme() 切换为订阅 AppThemeManager.config StateFlow
 * 2. 深色/浅色由用户主动控制，不再跟随系统
 * 3. colorScheme 根据 isDark + accentColor 动态组合，无需 recreate
 * 4. SideEffect 同步 StatusBar / NavigationBar 颜色
 */
@Composable
fun SongbookTheme(
    content: @Composable () -> Unit
) {
    val themeConfig by AppThemeManager.config.collectAsState()
    val isDark = themeConfig.isDark

    // 根据当前配置组合出 ColorScheme
    val colorScheme = buildColorScheme(themeConfig)

    val extendedColors = ExtendedColors(
        paperBackground = if (isDark) SongbookColors.PaperBackgroundDark else SongbookColors.PaperBackground,
        softCharcoal    = if (isDark) SongbookColors.SoftCharcoalDark    else SongbookColors.SoftCharcoal,
        terracotta      = SongbookColors.TerracottaBrown,
        mutedOlive      = SongbookColors.MutedOlive,
        ghostBorder     = SongbookColors.GhostBorder
    )

    // 同步系统状态栏 / 导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor  = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars    = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = SongbookTypography,
            shapes      = SongbookShapes,
            content     = content
        )
    }
}

/**
 * 根据 ThemeConfig 动态组合 ColorScheme
 *
 * 步骤：
 * 1. 选取基础方案（Light / Dark）
 * 2. 用强调色 Palette 覆写 primary / onPrimary / primaryContainer / onPrimaryContainer
 */
private fun buildColorScheme(config: AppThemeManager.ThemeConfig): ColorScheme {
    val palette = SongbookAccentPalettes.of(config.accentColor)
    return if (config.isDark) {
        SongbookDarkColorScheme.copy(
            primary              = palette.darkPrimary,
            onPrimary            = palette.darkOnPrimary,
            primaryContainer     = palette.darkPrimaryContainer,
            onPrimaryContainer   = palette.darkOnPrimaryContainer,
        )
    } else {
        SongbookLightColorScheme.copy(
            primary              = palette.lightPrimary,
            onPrimary            = palette.lightOnPrimary,
            primaryContainer     = palette.lightPrimaryContainer,
            onPrimaryContainer   = palette.lightOnPrimaryContainer,
        )
    }
}

object SongbookTheme {
    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
