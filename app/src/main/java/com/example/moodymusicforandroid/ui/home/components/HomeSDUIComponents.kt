package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.*
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

// =========================================================================
// 稳定颜色常量 — 避免在每次重组时调用 .copy(alpha=...) 生成新对象
// (Color.copy 会创建新的 Color 实例，导致 Compose 无法跳过相等性判断,
//  进而触发 Surface 重绘并在 GPU CommandIssue 阶段产生 40ms 阻塞)
// =========================================================================
private val BorderColorLight  = Color(0xFF9E9E9E).copy(alpha = 0.30f)  // OutlineVariant @ 0.30
private val BorderColorMid    = Color(0xFF9E9E9E).copy(alpha = 0.35f)  // OutlineVariant @ 0.35
private val BorderColorStrong = Color(0xFF9E9E9E).copy(alpha = 0.45f)  // OutlineVariant @ 0.45
private val BorderColorImg    = Color(0xFF9E9E9E).copy(alpha = 0.40f)  // OutlineVariant @ 0.40
private val BorderColorCircle = Color(0xFF9E9E9E).copy(alpha = 0.50f)  // OutlineVariant @ 0.50

private val StableBorderTrack   = androidx.compose.foundation.BorderStroke(1.dp, BorderColorLight)
private val StableBorderArtist  = androidx.compose.foundation.BorderStroke(1.dp, BorderColorMid)
private val StableBorderArchive = androidx.compose.foundation.BorderStroke(1.dp, BorderColorStrong)
private val StableBorderQuick   = androidx.compose.foundation.BorderStroke(1.dp, BorderColorMid)
private val StableBorderTab     = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF9E9E9E).copy(alpha = 0.50f))

// =========================================================================
// 1. 栏目标题与期号切片 (SectionTitleBlock)
// =========================================================================

@Composable
fun SectionTitleBlock(
    data: SectionTitleData,
    modifier: Modifier = Modifier,
    onActionClick: (String?) -> Unit = {}
) {
    val subtitle = data.subtitle
    val actionText = data.actionText

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.TerracottaBrown,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!actionText.isNullOrBlank()) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onActionClick(data.actionRoute) }
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

// =========================================================================
// 2. 分类标签栏切片 (CategoryTabsBlock)
// =========================================================================

@Composable
fun CategoryTabsBlock(
    data: CategoryTabsData,
    modifier: Modifier = Modifier,
    onTabSelect: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.tabs.forEach { tab ->
            val isSelected = tab.isSelected || tab.id == data.selectedId
            val containerColor = if (isSelected) {
                SongbookColors.BurntOrange
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
            val textColor = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onTabSelect(tab.id) },
                shape = RoundedCornerShape(16.dp),
                color = containerColor,
                border = if (!isSelected) StableBorderTab else null
            ) {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// =========================================================================
// 3. 艺术家双列网格切片 (ArtistGridBlock)
// =========================================================================

@Composable
fun ArtistGridBlock(
    data: ArtistGridData,
    modifier: Modifier = Modifier,
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> }
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        for (i in data.artists.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val left = data.artists[i]
                val right = data.artists.getOrNull(i + 1)

                ArtistGridCard(
                    artist = left,
                    modifier = Modifier.weight(1f),
                    onClick = { onArtistClick(left.id, left.name, left.avatarUrl) }
                )

                if (right != null) {
                    ArtistGridCard(
                        artist = right,
                        modifier = Modifier.weight(1f),
                        onClick = { onArtistClick(right.id, right.name, right.avatarUrl) }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ArtistGridCard(
    artist: ArtistBlockItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = StableBorderArtist
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 圆形头像
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, BorderColorCircle, CircleShape)
                    .padding(1.5.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                    SongbookImage(
                        model = artist.avatarUrl,
                        contentDescription = artist.name,
                        fallbackRes = R.drawable.artist_abigail_chen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${artist.albumCount} 张专辑 • ${artist.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 4. 单曲试听列表切片 (TrackListBlock)
// =========================================================================

@Composable
fun TrackListBlock(
    data: TrackListData,
    modifier: Modifier = Modifier,
    onTrackClick: (TrackBlockItem) -> Unit = {},
    onPlayToggle: (String) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.tracks.forEachIndexed { index, track ->
            TrackListItemCard(
                index = index + 1,
                track = track,
                onClick = { onTrackClick(track) },
                onPlayToggle = { onPlayToggle(track.id) }
            )
        }
    }
}

@Composable
fun TrackListItemCard(
    index: Int,
    track: TrackBlockItem,
    onClick: () -> Unit,
    onPlayToggle: () -> Unit
) {
    val album = track.album

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = StableBorderTrack
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放/序号按钮
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (track.isPlaying) SongbookColors.BurntOrange else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                    .clickable { onPlayToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (track.isPlaying) {
                    Text(
                        text = "■",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = SongbookColors.BurntOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 封面图
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                SongbookImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    fallbackRes = R.drawable.album_forest_track,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲与歌手
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (track.isPlaying) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} ${if (!album.isNullOrBlank()) "• $album" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 时长
            Text(
                text = track.duration,
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.Outline.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
    }
}

// =========================================================================
// 5. 时代留声机 / 档案卡片切片 (ArchiveCardBlock)
// =========================================================================

@Composable
fun ArchiveCardBlock(
    data: ArchiveCardData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val badge = data.badge
    val subtitle = data.subtitle
    val description = data.description

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = StableBorderArchive
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 典藏徽标
                if (!badge.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = SongbookColors.TerracottaBrown,
                        contentColor = Color.White
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "ARCHIVE / 1980-2020",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.Outline.copy(alpha = 0.75f),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 正方形黑胶封面
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    SongbookImage(
                        model = data.imageUrl,
                        contentDescription = data.title,
                        fallbackRes = R.drawable.album_hebei_kirin,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.TerracottaBrown,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "探索档案",
                    style = MaterialTheme.typography.labelMedium,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Explore",
                    tint = SongbookColors.BurntOrange,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// =========================================================================
// 6. 杂志摄影视觉大图切片 (ImageFeatureBlock)
// =========================================================================

@Composable
fun ImageFeatureBlock(
    data: ImageFeatureData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val caption = data.caption
    val author = data.author

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(data.aspectRatio.coerceAtLeast(1f))
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, BorderColorImg, RoundedCornerShape(6.dp))
        ) {
            SongbookImage(
                model = data.imageUrl,
                contentDescription = caption ?: "Visual Feature",
                fallbackRes = R.drawable.hero_acoustic_guitar,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!caption.isNullOrBlank() || !author.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.TerracottaBrown,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                if (!author.isNullOrBlank()) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// 7. 随行速达快捷功能入口切片 (QuickActionsBlock)
// =========================================================================

@Composable
fun QuickActionsBlock(
    data: QuickActionsData,
    modifier: Modifier = Modifier,
    onActionClick: (QuickActionItem) -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.actions.forEach { action ->
            val subtitle = action.subtitle

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick(action) },
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = StableBorderQuick
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 图标装饰指示
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (action.icon) {
                                "radio" -> "📻"
                                "vinyl" -> "💿"
                                "seat" -> "🪑"
                                "trend" -> "📈"
                                else -> "🎵"
                            },
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = action.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
