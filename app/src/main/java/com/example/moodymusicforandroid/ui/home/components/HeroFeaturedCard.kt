package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 首页 Hero 深度专题卡片
 * 杂志风错落层级排版（负 Margin 重叠），展示《回响：寻找消失的黑胶灵魂》与吉他海报。
 */
@Composable
fun HeroFeaturedCard(
    modifier: Modifier = Modifier,
    imageUrl: String = "/storage/covers/hero/hero_acoustic_guitar.jpg",
    title: String = "回响：寻找消失的黑胶灵魂",
    tag: String = "深度专题 / DEEP DIVE",
    summary: String = "在数字化的浪潮中，我们如何通过物理的震动重新找回听觉的本真？这是一场跨越半个世纪的模拟信号寻访。",
    primaryActionText: String = "阅读专题",
    secondaryActionText: String = "聆听专辑",
    onReadArticleClick: () -> Unit = {},
    onPlayAlbumClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 吉他海报 (4:5 比例高质感黑白/胶片处理)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onReadArticleClick() }
        ) {
            val colorFilter = remember(imageUrl) {
                if (imageUrl.contains("guitar")) {
                    val matrix = ColorMatrix().apply {
                        setToSaturation(0.2f) // 经典黑白胶片感
                    }
                    ColorFilter.colorMatrix(matrix)
                } else {
                    null
                }
            }

            val fallback = if (imageUrl.contains("butterfly")) {
                R.drawable.hero_butterfly_lovers
            } else {
                R.drawable.hero_acoustic_guitar
            }

            SongbookImage(
                model = imageUrl,
                contentDescription = title,
                fallbackRes = fallback,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )

            // 底部柔和过渡渐变 - 使用 remember 避免每帧重新创建 Brush 对象
            val surfaceColor = MaterialTheme.colorScheme.surface
            val gradientBrush = remember(surfaceColor) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        surfaceColor.copy(alpha = 0.8f)
                    )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
            )
        }

        // 错落层级文字说明区 (负 Margin 上移重叠)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-36).dp)
                .padding(start = 16.dp, end = 8.dp),
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp)
            ) {
                // 专题标签
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.TerracottaBrown,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 核心大标题 (Serif)
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 文艺导语
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 操作按钮组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onReadArticleClick,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SongbookColors.TerracottaBrown,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = primaryActionText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    OutlinedButton(
                        onClick = onPlayAlbumClick,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SongbookColors.BurntOrange
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.OutlineVariant),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = secondaryActionText,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
