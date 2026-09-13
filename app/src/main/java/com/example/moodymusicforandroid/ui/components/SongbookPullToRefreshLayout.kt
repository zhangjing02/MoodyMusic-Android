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
 * 采用经典“整体下移露出头部”交互规范：
 * 1. 手势下拉时，内容列表整体平滑向下滑移，在上方露出独立呼吸空间。
 * 2. 头部高度与下拉距离精准同步并严格裁切，保证内容与头部 0 重叠、0 穿透。
 * 3. 露出极简内嵌头部：回转细线箭头、状态提示与【上次更新时间】。
 * 4. 释放刷新时，内容固定停留在头部下方，静默加载。
 * 5. 刷新结束后平滑弹回，告别悬浮遮挡内容的粗糙圆形进度条。
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
    val haptic = LocalHapticFeedback.current

    // 上次更新时间格式化
    var lastUpdatedText by remember {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        mutableStateOf("上次更新 ${sdf.format(Date())}")
    }

    // 当刷新完成时更新时间戳
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            lastUpdatedText = "上次更新 ${sdf.format(Date())}"
        }
    }

    // 有效下拉比例：刷新时锁定至少为 1.0f
    val fraction = state.distanceFraction
    val effectiveFraction = if (isRefreshing) maxOf(fraction, 1f) else fraction

    // 触达临界点轻微震动反馈
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    LaunchedEffect(effectiveFraction >= 1.0f) {
        if (effectiveFraction >= 1.0f && !hasTriggeredHaptic && !isRefreshing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasTriggeredHaptic = true
        } else if (effectiveFraction < 1.0f) {
            hasTriggeredHaptic = false
        }
    }

    // 下拉位移计算：阈值内线性跟随，超额时阻尼伸展
    val pullOffsetPx = remember(effectiveFraction, headerHeightPx) {
        if (effectiveFraction <= 1f) {
            effectiveFraction * headerHeightPx
        } else {
            headerHeightPx + (effectiveFraction - 1f) * headerHeightPx * 0.35f
        }
    }

    // 露出区域高度：精准对应下拉位移，上限为 headerHeight
    val revealHeight = with(density) {
        pullOffsetPx.coerceAtMost(headerHeightPx).toDp()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {} // 禁用官方圆形悬浮条
    ) {
        // ── 1. 露出式头部布局 (Reveal Header) ──────────────────────
        // 严格位于状态栏与内容区之间，使用 clipToBounds 确保无内容交叉重叠
        if (pullOffsetPx > 2f || isRefreshing) {
            val headerAlpha = if (isRefreshing) {
                1f
            } else {
                val minThresholdPx = with(density) { 10.dp.toPx() }
                ((pullOffsetPx - minThresholdPx) / (headerHeightPx * 0.6f)).coerceIn(0f, 1f)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = headerTopPadding)
                    .height(revealHeight)
                    .clipToBounds()
                    .graphicsLayer { alpha = headerAlpha },
                contentAlignment = Alignment.Center
            ) {
                SongbookRevealHeaderContent(
                    pullFraction = effectiveFraction,
                    isRefreshing = isRefreshing,
                    lastUpdatedText = lastUpdatedText
                )
            }
        }

        // ── 2. 主内容区域 (随手势整体平滑下移，绝不被头部遮挡) ──────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = pullOffsetPx
                }
        ) {
            content()
        }
    }
}

/**
 * 露出式头部内容：旋转小箭头 + 状态文案 + 上次更新时间
 */
@Composable
private fun SongbookRevealHeaderContent(
    pullFraction: Float,
    isRefreshing: Boolean,
    lastUpdatedText: String
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (pullFraction >= 1.0f) 180f else 0f,
        animationSpec = tween(220),
        label = "arrowRotation"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 1. 状态图标（加载中圆环 vs 下拉指向箭头）
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

        // 2. 状态文案与上次更新时间
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            val statusTitle = when {
                isRefreshing -> "正在更新..."
                pullFraction >= 1.0f -> "释放立即刷新"
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
 * 极简线条小箭头：精准绘制，随手势阈值 180 度平滑回转
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

        // 竖直轴线
        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(w / 2f, 1.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // 左箭头翼
        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(2.5.dp.toPx(), h - 6.5.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        // 右箭头翼
        drawLine(
            color = SongbookColors.BurntOrange,
            start = Offset(w - 2.5.dp.toPx(), h - 6.5.dp.toPx()),
            end = Offset(w / 2f, h - 2.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
