package com.example.moodymusicforandroid.ui.components

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 现代高定唱片级 — 抽象极简纯线条离线徽标 (DownloadedTrackBadge)
 *
 * 核心审美规范：
 * 1. 彻底剔除生硬填色圆盘色块，采用 100% 纯线条几何构造 (Fine-line Minimalist Vector)；
 * 2. 极简空心细线圆环 (1.15dp 笔触) + 抽象悬浮微型下行折线，通透轻盈、留白极佳；
 * 3. 低饱和度素雅配色：正常离线契合排版基准轮廓色 (Outline)，云端有新母带则微显焦橙色 (BurntOrange)；
 * 4. 布局融入右侧操作区，与三点菜单垂直成列对齐，还给曲目名称纯粹的阅读呼吸感。
 */
@Composable
fun DownloadedTrackBadge(
    isHashOutdated: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color = if (isHashOutdated) SongbookColors.BurntOrange else SongbookColors.Outline.copy(alpha = 0.6f),
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val defaultTip = if (isHashOutdated) {
        "云端母带已更新，可重新下载以同步最新版本"
    } else {
        "已下载至本地（高品质离线）"
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (onClick != null) {
                    onClick()
                } else {
                    Toast.makeText(context, defaultTip, Toast.LENGTH_SHORT).show()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = minOf(w, h) / 2f - 0.75.dp.toPx()
            val stroke = 1.15.dp.toPx()

            // 1. 极简空心外圆环 (纯线条、无实心底色，轻盈通透)
            drawCircle(
                color = tint,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
            )

            // 2. 内部抽象极简微折线
            if (isHashOutdated) {
                // 有更新：极简顺时针微圆弧 + 箭头折线
                val arcR = r * 0.48f
                drawArc(
                    color = tint,
                    startAngle = 40f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(cx - arcR, cy - arcR),
                    size = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val tipX = cx + arcR * 0.65f
                val tipY = cy - arcR * 0.75f
                drawLine(
                    color = tint,
                    start = Offset(tipX - 1.8.dp.toPx(), tipY),
                    end = Offset(tipX, tipY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(tipX, tipY + 1.8.dp.toPx()),
                    end = Offset(tipX, tipY),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
            } else {
                // 正常离线：极简抽象下行折线与中轴 (轻盈呼吸感)
                drawLine(
                    color = tint,
                    start = Offset(cx, cy - r * 0.40f),
                    end = Offset(cx, cy + r * 0.36f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round
                )
                val chevronPath = Path().apply {
                    moveTo(cx - r * 0.30f, cy + r * 0.08f)
                    lineTo(cx, cy + r * 0.36f)
                    lineTo(cx + r * 0.30f, cy + r * 0.08f)
                }
                drawPath(
                    path = chevronPath,
                    color = tint,
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
