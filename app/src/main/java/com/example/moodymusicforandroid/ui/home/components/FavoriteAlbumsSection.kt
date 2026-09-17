package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.LibraryAlbumItem
import com.example.moodymusicforandroid.data.model.getDisplayTitle
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 收藏专辑展示区域组件 (FavoriteAlbumsSection)
 * 接收服务器返回的真实专辑列表；空状态显示推荐占位。
 */
@Composable
fun FavoriteAlbumsSection(
    albums: List<LibraryAlbumItem>,
    modifier: Modifier = Modifier,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onViewAllClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "收藏的专辑",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "全部",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.BurntOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onViewAllClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (albums.isEmpty()) {
            // 空状态：推荐引导卡片
            EmptyAlbumsPlaceholder()
        } else {
            // 最多展示 3 张精选预览，保持页面精炼
            val displayAlbums = albums.take(3)
            // 双列卡片网格
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                for (i in displayAlbums.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val left = displayAlbums[i]
                        val right = displayAlbums.getOrNull(i + 1)

                        RealAlbumCard(
                            item = left,
                            modifier = Modifier.weight(1f),
                            onClick = { onAlbumClick(left.albumId, left.title ?: left.albumId) }
                        )

                        if (right != null) {
                            RealAlbumCard(
                                item = right,
                                modifier = Modifier.weight(1f),
                                onClick = { onAlbumClick(right.albumId, right.title ?: right.albumId) }
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealAlbumCard(
    item: LibraryAlbumItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            SongbookImage(
                model = item.cover,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val displayTitle = item.getDisplayTitle()
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyAlbumsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_library),
                contentDescription = null,
                tint = SongbookColors.Outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "还没有收藏的专辑",
                style = MaterialTheme.typography.bodyMedium,
                color = SongbookColors.Outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "去发现页面探索你喜爱的音乐吧",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.Outline.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
