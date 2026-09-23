package com.example.moodymusicforandroid.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 现代颂歌 (The Modern Songbook) 露出式极简下拉刷新布局容器
 *
 * 采用经典"整体下移露出头部"交互规范：
 * 1. 手势下拉时，内容列表整体平滑向下滑移，在上方露出独立呼吸空间。
 * 2. 头部采用固定高度容器 + graphicsLayer 滑入，全程零额外 Measure/Layout pass。
 * 3. 内容区同样通过 graphicsLayer translationY 驱动，全程 GPU-only 流畅动画。
 * 4. 头部始终保留在 Composition 树中（不做条件渲染），用 alpha 控制可见性，
 *    避免频繁进出 Composition 带来的抖动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongbookPullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    headerTopPadding: Dp = 0.dp,
    headerHeight: Dp = 58.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val minThresholdPx = with(density) { 10.dp.toPx() }
    val haptic = LocalHapticFeedback.current

    // 上次更新时间（仅在刷新完成时更新，完全不随下拉帧变动）
    var lastUpdatedText by remember {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        mutableStateOf("上次更新 ${sdf.format(Date())}")
    }
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            lastUpdatedText = "上次更新 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}"
        }
    }

    val currentRefreshing by rememberUpdatedState(isRefreshing)

    // 触达临界点状态：手势下拉距离是否触达 1.0f 阈值
    // 彻底解耦 isRefreshing 脏状态，确保首次进入无论处于何种加载态，手势下拉均能 100% 触发箭头翻转动效
    val isThresholdReached by remember {
        derivedStateOf {
            state.distanceFraction >= 1.0f
        }
    }

    // 触达临界点轻微震动反馈
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(isThresholdReached) {
        if (isThresholdReached && !hasTriggeredHaptic && !currentRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasTriggeredHaptic = true
        } else if (!isThresholdReached) {
            hasTriggeredHaptic = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {} // 禁用官方圆形悬浮指示器
    ) {
        // ── 1. 露出式头部（完全在 graphicsLayer 绘制阶段计算位移与透明度，零 Recomposition）──────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = headerTopPadding)
                .height(headerHeight)
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 绘制阶段延迟读取状态，跳过 Composition 和 Layout 阶段
                        val f = if (currentRefreshing) maxOf(state.distanceFraction, 1f) else state.distanceFraction
                        val pullOffset = if (f <= 1f) {
                            f * headerHeightPx
                        } else {
                            headerHeightPx + (f - 1f) * headerHeightPx * 0.35f
                        }
                        translationY = pullOffset.coerceAtMost(headerHeightPx) - headerHeightPx
                        alpha = if (currentRefreshing) {
                            1f
                        } else {
                            ((pullOffset - minThresholdPx) / (headerHeightPx * 0.6f)).coerceIn(0f, 1f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                SongbookRevealHeaderContent(
                    isThresholdReached = isThresholdReached,
                    isRefreshing = isRefreshing,
                    lastUpdatedText = lastUpdatedText
                )
            }
        }

        // ── 2. 主内容（同样在 graphicsLayer 内部延迟读取位移，回弹全程零 Recomposition！）────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = if (currentRefreshing) maxOf(state.distanceFraction, 1f) else state.distanceFraction
                    val pullOffset = if (f <= 1f) {
                        f * headerHeightPx
                    } else {
                        headerHeightPx + (f - 1f) * headerHeightPx * 0.35f
                    }
                    translationY = pullOffset
                }
        ) {
            content()
        }
    }
}

/**
 * 露出式头部内容：旋转细线箭头 + 状态文案 + 上次更新时间
 * 仅在 isThresholdReached / isRefreshing 改变时重组，微下拉回弹时保持完全稳定
 */
@Composable
private fun SongbookRevealHeaderContent(
    isThresholdReached: Boolean,
    isRefreshing: Boolean,
    lastUpdatedText: String
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (isThresholdReached) 180f else 0f,
        animationSpec = tween(200),
        label = "arrowRotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 状态图标（加载中圆环 vs 下拉箭头）
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = isRefreshing,
                animationSpec = tween(200),
                label = "indicatorCrossfade"
            ) { refreshing ->
                if (refreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = SongbookColors.BurntOrange,
                        strokeWidth = 1.8.dp
                    )
                } else {
                    MinimalistHairlineArrow(rotation = arrowRotation)
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 状态文案 + 上次更新时间
        Column(verticalArrangement = Arrangement.Center) {
            val statusTitle = when {
                isRefreshing -> "正在更新..."
                isThresholdReached -> "释放立即刷新"
                else -> "下拉翻阅手札"
            }
            Text(
                text = statusTitle,
                style = MaterialTheme.typography.labelMedium,
                color = SongbookColors.SoftCharcoal,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = lastUpdatedText,
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.45f),
                fontSize = 10.5.sp
            )
        }
    }
}

/**
 * 极简线条小箭头：随手势阈值 180° 平滑回转
 */
@Composable
private fun MinimalistHairlineArrow(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(16.dp)
            .rotate(rotation)
    ) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(w / 2f, 1.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(2.5.dp.toPx(), h - 6.5.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(w - 2.5.dp.toPx(), h - 6.5.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
