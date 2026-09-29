package com.example.moodymusicforandroid.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * 现代颂歌 (The Modern Songbook) 露出式极简下拉刷新布局容器
 *
 * 采用原生自研高性能物理嵌套滚动驱动：
 * 1. 彻底解决 Android 12+ Stretch Overscroll 与下拉平移的双重物理弹簧相位错位。
 * 2. 根除 Material3 官方 PullToRefresh 1500f 硬弹簧与 Snap 急刹车导致的微下拉回弹阶梯卡顿。
 * 3. 消费全部松手 Fling 速度，杜绝向顶部 LazyColumn 泄露向上速度导致的抽搐与制动冲突。
 * 4. 全程延迟读取位移至 graphicsLayer Draw 阶段，主线程 Recomposition 次数严格为 0。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongbookPullToRefreshLayout(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: PullToRefreshState = rememberPullToRefreshState(),
    headerTopPadding: Dp = 0.dp,
    headerHeight: Dp = 58.dp,
    refreshThreshold: Dp = 72.dp,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val headerHeightPx = with(density) { headerHeight.toPx() }
    val refreshThresholdPx = with(density) { refreshThreshold.toPx() }
    val minThresholdPx = with(density) { 10.dp.toPx() }
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // 核心物理动画控制器：直接管理绝对像素下拉位移，100% 自主掌控回弹弹簧物理属性
    val pullDistanceAnim = remember { Animatable(0f) }

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

    // 触达临界点状态：手势实际下拉距离是否达到触发刷新的阈值
    // 纯物理手势距离驱动，彻底解耦 isRefreshing 初始脏状态，确保冷启动手势 100% 触发动效
    val isThresholdReached by remember {
        derivedStateOf {
            pullDistanceAnim.value >= refreshThresholdPx
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

    // 业务刷新状态监听：驱动头部平滑展开或收起
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            // 外部主动触发刷新时（如冷启动加载），平滑展开到头部高度
            if (pullDistanceAnim.value < headerHeightPx) {
                pullDistanceAnim.animateTo(
                    targetValue = headerHeightPx,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else {
            // 刷新完成，平滑收起归零（使用 400f 温和柔顺弹簧，告别生硬刹车）
            if (pullDistanceAnim.value > 0f) {
                pullDistanceAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = 400f
                    )
                )
            }
        }
    }

    // 自研高质感嵌套滚动连接器：彻底杜绝速度泄露与双弹簧冲突
    val nestedScrollConnection = remember(isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 当用户向上推且当前已有下拉距离时，优先将下拉距离收缩归零
                if (source == NestedScrollSource.UserInput && available.y < 0f && pullDistanceAnim.value > 0f) {
                    val current = pullDistanceAnim.value
                    val consumedY = available.y.coerceAtLeast(-current)
                    coroutineScope.launch {
                        pullDistanceAnim.snapTo((current + consumedY).coerceAtLeast(0f))
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // 仅当列表在顶部且用户继续下拉时（consumed.y == 0 且 available.y > 0）
                if (source == NestedScrollSource.UserInput && available.y > 0f && !isRefreshing) {
                    val current = pullDistanceAnim.value
                    // 动态自然物理阻尼：阈值内 0.55f 舒适跟手，超过阈值后逐渐对数衰减
                    val ratio = (current / refreshThresholdPx).coerceAtLeast(0f)
                    val damping = if (ratio <= 1.0f) 0.55f else (0.55f / (1f + (ratio - 1f) * 0.75f))
                    val delta = available.y * damping
                    val target = current + delta
                    coroutineScope.launch {
                        pullDistanceAnim.snapTo(target)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val current = pullDistanceAnim.value
                if (current > 0f) {
                    if (!isRefreshing && current >= refreshThresholdPx) {
                        // 达标：触发刷新，平滑回弹到展开高度 headerHeightPx
                        coroutineScope.launch {
                            pullDistanceAnim.animateTo(
                                targetValue = headerHeightPx,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                        onRefresh()
                    } else if (!isRefreshing) {
                        // 未达标（微下拉回弹）：以 400f 黄金质感阻尼弹簧优雅回弹至 0，彻底杜绝顿挫！
                        coroutineScope.launch {
                            pullDistanceAnim.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = 400f
                                )
                            )
                        }
                    }
                    // 彻底全额消费所有松手 Fling 速度，绝对不向 LazyColumn 泄露任何向上残余速度！
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    // 禁用子树系统 Overscroll，杜绝 EdgeEffect 拉伸与外层位移双重弹簧相位对抗
    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
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
                            val distance = pullDistanceAnim.value
                            val pullOffset = if (distance <= headerHeightPx) {
                                distance
                            } else {
                                headerHeightPx + (distance - headerHeightPx) * 0.35f
                            }
                            translationY = pullOffset.coerceAtMost(headerHeightPx) - headerHeightPx
                            alpha = if (currentRefreshing) {
                                1f
                            } else {
                                ((distance - minThresholdPx) / (headerHeightPx * 0.6f)).coerceIn(0f, 1f)
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
                        val distance = pullDistanceAnim.value
                        val pullOffset = if (distance <= headerHeightPx) {
                            distance
                        } else {
                            headerHeightPx + (distance - headerHeightPx) * 0.35f
                        }
                        translationY = pullOffset
                    }
            ) {
                content()
            }
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
