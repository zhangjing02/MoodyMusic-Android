package com.example.moodymusicforandroid.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 苹果 (iOS) 风格列表项左滑显示删除/移除按钮组件。
 *
 * 核心动效规范（对齐原生 Android / iOS 列表删除标准）：
 * 1. 默认状态：与普通列表项无异，右侧无多余图标；
 * 2. 用户向左滑动：内容平滑左移，右侧露出紧凑规范的红色操作按钮（iOS 红 #FF3B30，宽度 76dp）；
 * 3. 释放吸附：滑动超过阈值（35%）自动吸附展开，未达阈值则平滑弹回；
 * 4. 真正删除与连贯退场动画：
 *    - 点击「移除/删除」后，当前 item 迅速向左滑出屏幕（飞走），并伴随透明度渐隐；
 *    - 此时高度在 260ms 内平滑塌缩到 0，让下方 item 顺滑上移填补空位；
 *    - 动画完成后才回调 [onDelete]，列表刷新完全无跳变、无缝连贯！
 * 5. 误触防范：展开状态下轻触内容区或向右轻滑，平滑收起，不会误触发内容点击。
 */
@Composable
fun SwipeToRevealDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    revealWidth: Dp = 76.dp,
    deleteLabel: String = "移除",
    deleteIcon: Painter? = painterResource(R.drawable.ic_trash),
    deleteColor: Color = Color(0xFFFF3B30),
    contentBackgroundColor: Color = Color.Transparent,
    shape: Shape? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealWidthPx = with(density) { revealWidth.toPx() }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val heightFraction = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    var isRevealed by remember { mutableStateOf(false) }
    var isDismissing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (shape != null) Modifier.clip(shape) else Modifier.clipToBounds())
            .graphicsLayer {
                this.alpha = alpha.value
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val currentHeight = (placeable.height * heightFraction.value).roundToInt()
                layout(placeable.width, currentHeight) {
                    placeable.place(0, 0)
                }
            }
    ) {
        // ── 1. 底层：右侧紧凑删除操作按钮（使用 matchParentSize 严格与行高一致） ──
        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(revealWidth)
                    .fillMaxHeight()
                    .background(deleteColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isDismissing
                    ) {
                        if (isDismissing) return@clickable
                        isDismissing = true
                        scope.launch {
                            // 1. 当前 item 向左加速滑出屏幕（跑掉动画）
                            launch {
                                offsetX.animateTo(
                                    targetValue = -screenWidthPx,
                                    animationSpec = tween(durationMillis = 260, easing = FastOutLinearInEasing)
                                )
                            }
                            // 2. 渐隐透明度
                            launch {
                                alpha.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                            }
                            // 3. 高度平滑塌缩，促使下方 item 实时上移填补空缺位置
                            heightFraction.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                            )
                            // 4. 动画结束后安全执行数据移除，此时行高度已为0，数据移除完全无感
                            onDelete()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (deleteIcon != null) {
                        Icon(
                            painter = deleteIcon,
                            contentDescription = deleteLabel,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                    Text(
                        text = deleteLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ── 2. 表层：行内容，offset 优先于 background，平移时背景随之平移露出右侧按钮 ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(contentBackgroundColor)
                .pointerInput(revealWidthPx, isDismissing) {
                    if (isDismissing) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val target = if (isRevealed) {
                                    // 已经展开状态下，如果向右拖动超过 30% 按钮宽度，则收起
                                    if (offsetX.value > -revealWidthPx * 0.70f) 0f else -revealWidthPx
                                } else {
                                    // 未展开状态下，向左拖动超过 35% 按钮宽度，则展开
                                    if (offsetX.value < -revealWidthPx * 0.35f) -revealWidthPx else 0f
                                }
                                offsetX.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                                isRevealed = (target != 0f)
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                val target = if (isRevealed) -revealWidthPx else 0f
                                offsetX.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
                            }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            // 严格限制范围 [-revealWidthPx, 0f]，不允许向右拉伸
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-revealWidthPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    }
                }
        ) {
            content()

            // ── 3. 展开时在内容区覆盖透明拦截层：轻触内容收回，防止误触发点击播放 ──
            if (isRevealed && !isDismissing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                isRevealed = false
                            }
                        }
                )
            }
        }
    }
}
