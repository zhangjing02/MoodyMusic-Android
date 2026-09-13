package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.*
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

// =========================================================================
// 1. 顶部推荐 Banner 切片 (TopRecommendBannerBlock)
// =========================================================================
@Composable
fun TopRecommendBannerBlock(
    data: TopRecommendBannerData,
    modifier: Modifier = Modifier,
    onClick: (TopRecommendBannerData) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick(data) }
    ) {
        // 背景大图
        SongbookImage(
            model = data.coverUrl,
            contentDescription = data.title,
            fallbackRes = R.drawable.home_vinyl_banner,
            modifier = Modifier.fillMaxSize()
        )

        // 柔和暗角微渐变，衬托角标与氛围
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        // 左上角角标 Badge
        val badge = data.badge
        if (!badge.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.TerracottaBrown,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // 底部标题提示
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = data.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = data.subtitle
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 2. 今日推荐横向滑动卡片切片 (TodayRecommendScrollBlock)
// =========================================================================
@Composable
fun TodayRecommendScrollBlock(
    data: TodayRecommendScrollData,
    modifier: Modifier = Modifier,
    onItemClick: (TodayRecommendItem) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = data.title.ifBlank { "今日推荐" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (!data.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.BurntOrange,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 横向黑胶唱片列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(data.items, key = { it.id }) { item ->
                val fallbackRes = remember(item.id, item.coverUrl) {
                    when {
                        item.id.contains("bach") || item.coverUrl.contains("bach") -> R.drawable.album_bach_cello
                        item.id.contains("jonathan") || item.coverUrl.contains("jonathan") -> R.drawable.album_jonathan_lee
                        item.id.contains("lofi") || item.coverUrl.contains("lofi") -> R.drawable.album_lofi_chill
                        item.id.contains("piano") || item.coverUrl.contains("piano") -> R.drawable.album_pop_piano
                        else -> R.drawable.album_vintage_vinyl
                    }
                }

                Column(
                    modifier = Modifier
                        .width(124.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onItemClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        SongbookImage(
                            model = item.coverUrl,
                            contentDescription = item.title,
                            fallbackRes = fallbackRes,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// 3. 深度解读卡片切片 (DeepDiveFeatureBlock)
// =========================================================================
@Composable
fun DeepDiveFeatureBlock(
    data: DeepDiveFeatureData,
    modifier: Modifier = Modifier,
    onReadArticleClick: () -> Unit = {},
    onPlayAlbumClick: () -> Unit = {}
) {
    HeroFeaturedCard(
        modifier = modifier,
        imageUrl = data.coverUrl,
        title = data.title,
        tag = data.tag ?: "深度名作解析 · DEEP DIVE",
        summary = data.summary ?: "",
        primaryActionText = data.primaryActionText ?: "阅读深度专题",
        secondaryActionText = data.secondaryActionText ?: "聆听全曲",
        onReadArticleClick = onReadArticleClick,
        onPlayAlbumClick = onPlayAlbumClick
    )
}

// =========================================================================
// 4. 音乐综艺双排网格切片 (VarietyShowGridBlock)
// =========================================================================
@Composable
fun VarietyShowGridBlock(
    data: VarietyShowGridData,
    modifier: Modifier = Modifier,
    onItemClick: (VarietyShowItem) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 栏目标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = data.title.ifBlank { "音乐综艺精选" },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (!data.subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = data.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.TerracottaBrown,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 双排网格卡片（两列成对排列，与外层 LazyColumn 完美协同）
        for (i in data.items.indices step 2) {
            val left = data.items[i]
            val right = data.items.getOrNull(i + 1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                VarietyShowCard(
                    item = left,
                    modifier = Modifier.weight(1f),
                    onClick = { onItemClick(left) }
                )
                if (right != null) {
                    VarietyShowCard(
                        item = right,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(right) }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun VarietyShowCard(
    item: VarietyShowItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val fallbackRes = remember(item.id, item.coverUrl) {
        when {
            item.id.contains("voice") || item.coverUrl.contains("voice") -> R.drawable.variety_voice_of_china
            item.id.contains("singer") && !item.id.contains("masked") || item.coverUrl.contains("i_am_singer") -> R.drawable.variety_i_am_singer
            item.id.contains("masked") || item.coverUrl.contains("masked") -> R.drawable.variety_masked_singer
            item.id.contains("band") || item.coverUrl.contains("big_band") -> R.drawable.variety_big_band
            else -> R.drawable.album_vintage_vinyl
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        // 海报卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                )
        ) {
            SongbookImage(
                model = item.coverUrl,
                contentDescription = item.title,
                fallbackRes = fallbackRes,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 节目主标题
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 副标题 / 口号
        val subtitle = item.subtitle
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
