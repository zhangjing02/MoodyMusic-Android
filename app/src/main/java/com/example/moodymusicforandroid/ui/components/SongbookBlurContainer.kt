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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * 现代颂歌 硬件级动态毛玻璃容器 (SongbookBlurContainer)
 *
 * 核心设计：
 * 1. 【Compose 原生硬件高斯模糊】：基于 dev.chrisbanes.haze，在 Android 12+ / 15 / 16 (API 31+) 真机上
 *    直接调用系统 RenderEffect GPU 硬件模糊，对底层 LazyColumn/封面等 Compose 节点进行原生高斯雾化；
 * 2. 【智能优雅降级】：在低版本系统（如 Android 9/11）或无 hazeState 时，平滑降级为 Scrim 半透明磨砂遮罩；
 * 3. 【拟真液态高光】：顶层附加 25%->5% 迎光面漫反射微光渐变，呈现出苹果级别清透莹润的液态水晶玻璃质感。
 */
@Composable
fun SongbookBlurContainer(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    blurRadius: Dp = 20.dp,
    cornerRadius: Dp = 18.dp,
    shape: Shape? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    overlayColor: Color = Color(0x6EF8F9FA), // 约 43% 清透淡灰液态磨砂玻璃（透光通透，清晰映现底层图像轮廓与色彩）
    borderColor: Color = Color.Transparent, // 默认无描边
    borderWidth: Dp = 0.dp,
    elevation: Dp = 10.dp,
    content: @Composable () -> Unit
) {
    val effectiveShape = shape ?: remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val borderModifier = if (borderWidth > 0.dp && borderColor != Color.Transparent && borderColor.alpha > 0f) {
        Modifier.border(borderWidth, borderColor, effectiveShape)
    } else Modifier

    val hazeModifier = if (hazeState != null) {
        Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = backgroundColor,
                blurRadius = blurRadius,
                tint = HazeTint(overlayColor),
                fallbackTint = HazeTint(overlayColor)
            )
        )
    } else {
        Modifier.background(overlayColor)
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
                            Color.White.copy(alpha = 0.12f),
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
