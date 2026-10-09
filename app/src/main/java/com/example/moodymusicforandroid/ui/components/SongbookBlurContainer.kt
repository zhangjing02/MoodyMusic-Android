package com.example.moodymusicforandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.common.utils.DeviceInfoUtils
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * 现代颂歌 硬件级动态毛玻璃容器 (SongbookBlurContainer)
 *
 * 核心设计：
 * 1. 【Compose 原生硬件高斯模糊】：基于 dev.chrisbanes.haze，在 Android 12+ / 15 / 16 (API 31+) 高算力真机上
 *    直接调用系统 RenderEffect GPU 硬件模糊，对底层 LazyColumn/封面等 Compose 节点进行原生高斯雾化；
 * 2. 【低端机优雅降级适配】：在 Android 11 及以下系统或低内存/低算力设备上，自动关闭 GPU 模糊计算，
 *    降级为 80%~85% 半透明雅灰（暗色 #D4222320 / 浅色 #D9EAE8E4）磨砂质感卡片，配合 0.6dp 晶透细微边框，
 *    彻底杜绝毛玻璃透底穿帮与滑动重叠问题；
 * 3. 【拟真液态高光】：顶层附加迎光面漫反射微光渐变，呈现苹果级别清透莹润的液态水晶玻璃质感。
 */
@Composable
fun SongbookBlurContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 18.dp,
    shape: Shape? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    overlayColor: Color? = null,
    borderColor: Color = Color.Transparent, // 默认无描边
    borderWidth: Dp = 0.dp,
    elevation: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isLowEnd = remember(context) { DeviceInfoUtils.isLowEndDevice(context) }
    val effectiveShape = shape ?: remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = remember(surfaceColor) {
        (surfaceColor.red * 0.299f + surfaceColor.green * 0.587f + surfaceColor.blue * 0.114f) < 0.5f
    }

    // 自适应磨砂玻璃蒙层：
    // 深色下由原本 78% 死黑调整为通透温润的约 58% 黑曜石晶雾 (0x94181916)，底层内容光影可自然透出高斯漫射光晕；
    // 浅色下维持 43% 清透淡灰液态磨砂玻璃 (0x6EF8F9FA)。
    val effectiveOverlayColor = overlayColor ?: if (isDark) {
        Color(0x94181916) // 约 58% 墨曜石深灰通透蒙层，告别死黑沉闷
    } else {
        Color(0x6EF8F9FA) // 约 43% 清透淡灰液态磨砂玻璃
    }

    // 深色模式下基底色：由死黑 SurfaceDarkLowest (0xFF0F100E) 提升至 SurfaceDarkLow (0xFF1B1C1A)
    // 确保在深暗页面背景 (0xFF141513) 上抬升一个立体层级，形成自然的表面层级阶梯
    val effectiveBackgroundColor = if (isDark) {
        SongbookColors.SurfaceDarkLow // 0xFF1B1C1A 沉稳暗调低层表面，高于页面背景
    } else {
        backgroundColor
    }

    // 低端机或降级模式下的半透明雅灰调色方案
    val fallbackGreyColor = if (isDark) {
        Color(0xF51E201D) // 约 96% 暖调石墨黑，清晰界定浮层高度
    } else {
        Color(0xD9EAE8E4) // 约 85% 暖调纸质浅灰
    }

    // 晶莹微轮廓：深色模式下阴影对比度极低，必须由晶透细微边框提供视觉骨架与边界分离！
    val defaultBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f) // 10% 晶莹白色微光轮廓，在暗色背景上划定清晰通透的玻璃边界
    } else {
        Color(0x18877369) // 浅色下极微弱自然轮廓
    }
    val defaultBorderWidth = 0.8.dp

    val borderModifier = if (borderWidth > 0.dp && borderColor != Color.Transparent && borderColor.alpha > 0f) {
        Modifier.border(borderWidth, borderColor, effectiveShape)
    } else if (isLowEnd) {
        Modifier.border(0.6.dp, defaultBorderColor, effectiveShape)
    } else {
        // 常规硬件毛玻璃模式：自动施加晶莹微边框，确保在深色模式下与底色清晰分离
        Modifier.border(defaultBorderWidth, defaultBorderColor, effectiveShape)
    }

    val hazeModifier = if (!isLowEnd && hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = effectiveBackgroundColor,
                blurRadius = blurRadius,
                tint = HazeTint(effectiveOverlayColor),
                fallbackTint = HazeTint(fallbackGreyColor)
            )
        )
    } else {
        Modifier.background(fallbackGreyColor)
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = effectiveShape,
                ambientColor = if (isDark) Color(0x33000000) else Color(0x0E000000),
                spotColor = if (isDark) Color(0x55000000) else Color(0x16000000)
            )
            .clip(effectiveShape)
            .then(hazeModifier)
            .then(borderModifier)
    ) {
        // 顶层微光漫反射层：在深色模式下呈现 0.08f 迎光面珠宝切面光泽，温润通透
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        // 顶层：Compose 内容
        content()
    }
}
