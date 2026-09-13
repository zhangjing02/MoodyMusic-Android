package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState

import com.example.moodymusicforandroid.ui.components.SongbookBlurContainer

/**
 * 全局悬浮迷你播放器组件 (FloatingMiniPlayer)
 *
 * 特性：
 * 1. 采用项目原生硬件级动态毛玻璃 (SongbookBlurContainer)；
 * 2. 纯粹 56dp 紧凑高度；
 * 3. 黄金比例三联控制按钮；
 * 4. 播放/暂停按钮外圈圆形弧形进度条（对标网易云音乐风格）。
 */
@Composable
fun FloatingMiniPlayer(
    modifier: Modifier = Modifier,
    trackTitle: String = "苔藓上的私语",
    artistName: String = "周深处 & 森林合唱团",
    coverUrl: String = "",
    isPlaying: Boolean = true,
    position: Int = 0,      // 当前播放位置 (ms)
    duration: Int = 0,      // 总时长 (ms)
    hazeState: HazeState? = null,
    onPlayerClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSeekTo: (Int) -> Unit = {}
) {
    SongbookBlurContainer(
        modifier = modifier.fillMaxWidth(),
        hazeState = hazeState,
        cornerRadius = 16.dp,
        elevation = 10.dp,
        overlayColor = Color(0x6EF8F9FA),
        borderColor = Color.Transparent,
        borderWidth = 0.dp
    ) {
        FloatingMiniPlayerContent(
            trackTitle = trackTitle,
            artistName = artistName,
            coverUrl = coverUrl,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            onPlayerClick = onPlayerClick,
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onSeekTo = onSeekTo
        )
    }
}

/**
 * 迷你播放器纯内容布局（供独立悬浮或在底栏一体化容器中紧贴复用）
 */
@Composable
fun FloatingMiniPlayerContent(
    trackTitle: String,
    artistName: String,
    coverUrl: String,
    isPlaying: Boolean,
    position: Int = 0,
    duration: Int = 0,
    onPlayerClick: () -> Unit = {},
    onPlayPauseClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onSeekTo: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 黑胶唱片匀速旋转微动效
    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "VinylRotationAngle"
    )

    // 进度比例 (0f ~ 1f)
    val progressFraction = if (duration > 0) {
        (position.toFloat() / duration).coerceIn(0f, 1f)
    } else 0f

    // 圆弧颜色
    val arcTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val arcFillColor  = SongbookColors.BurntOrange

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onPlayerClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── 黑胶唱片封面圆盘 ──────────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x30000000),
                    spotColor = Color(0x40000000)
                )
                .clip(CircleShape)
                .background(Color(0xFF1B1C1A))
                .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .rotate(if (isPlaying) rotation else 0f)
            ) {
                val discFallback = when {
                    coverUrl.contains("butterfly_lovers") -> R.drawable.cover_butterfly_lovers
                    coverUrl.contains("bach_cello")       -> R.drawable.album_bach_cello
                    coverUrl.contains("lofi_chill")       -> R.drawable.album_lofi_chill
                    coverUrl.contains("pop_piano")        -> R.drawable.album_pop_piano
                    coverUrl.contains("jonathan_lee")     -> R.drawable.album_jonathan_lee
                    else                                  -> R.drawable.album_forest_track
                }
                SongbookImage(
                    model = coverUrl.ifBlank { discFallback },
                    contentDescription = "Playing track vinyl cover",
                    fallbackRes = discFallback,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // ── 曲目与艺术家信息 ──────────────────────────────────────
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ── 播放控制按钮区 ────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 上一首
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_previous),
                    contentDescription = "Previous Track",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // ── 播放/暂停 + 外圈圆形进度弧 ─────────────────────────
            // 外盒比按钮大 8dp（各边 4dp），留给圆弧描边空间
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                // 圆弧进度（Canvas 绘制，不拦截触控，透传给下方按钮）
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 2.5.dp.toPx()
                    val inset    = strokePx / 2f
                    val arcSize  = Size(size.width - strokePx, size.height - strokePx)
                    val topLeft  = Offset(inset, inset)

                    // 轨道底色（完整圆圈，很淡）
                    drawArc(
                        color       = arcTrackColor,
                        startAngle  = -90f,
                        sweepAngle  = 360f,
                        useCenter   = false,
                        topLeft     = topLeft,
                        size        = arcSize,
                        style       = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )

                    // 填充弧（从 12 点方向顺时针扫过进度角度）
                    if (progressFraction > 0f) {
                        drawArc(
                            color      = arcFillColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter  = false,
                            topLeft    = topLeft,
                            size       = arcSize,
                            style      = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }
                }

                // 播放/暂停图标按钮（居中叠在圆弧上）
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = SongbookColors.BurntOrange,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // 下一首
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = "Next Track",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
