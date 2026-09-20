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
    overlayColor: Color = Color(0x6EF8F9FA), // 约 43% 清透淡灰液态磨砂玻璃（仅用于支持硬件模糊的正常渲染）
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

    // 低端机或降级模式下的 80%~85% 半透明雅灰调色方案
    val fallbackGreyColor = if (isDark) {
        Color(0xD4222320) // 约 83% 深灰软炭色，不透底字且微透底光
    } else {
        Color(0xD9EAE8E4) // 约 85% 暖调纸质浅灰
    }

    // 细微边框增强轮廓，防止底栏/悬浮窗在低端机上与底层内容粘连
    val fallbackBorderColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color(0x22877369)
    }

    val borderModifier = if (borderWidth > 0.dp && borderColor != Color.Transparent && borderColor.alpha > 0f) {
        Modifier.border(borderWidth, borderColor, effectiveShape)
    } else if (isLowEnd) {
        Modifier.border(0.6.dp, fallbackBorderColor, effectiveShape)
    } else Modifier

    val hazeModifier = if (!isLowEnd && hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                blurRadius = blurRadius,
                tint = HazeTint(overlayColor),
                fallbackTint = HazeTint(fallbackGreyColor) // 确保即使偶发降级，也是 80% 雅灰色，绝不全透明
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
                ambientColor = Color(0x0E000000),
                spotColor = Color(0x16000000)
            )
            .clip(effectiveShape)
            .then(hazeModifier)
            .then(borderModifier)
    ) {
        // 顶层微光漫反射层：赋予磨砂玻璃表面自然的光泽感与立体感
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.14f),
                            Color.White.copy(alpha = 0.02f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 顶层：Compose 内容
        content()
    }
}
