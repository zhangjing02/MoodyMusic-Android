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
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 收藏歌曲展示区域 (FavoriteSongsSection)
 * 显示用户收藏的歌曲列表，每首歌展示封面、歌名、歌手。
 * 空状态引导用户去探索音乐。
 */
@Composable
fun FavoriteSongsSection(
    songs: List<FavoriteSong>,
    songCount: Int,
    modifier: Modifier = Modifier,
    onSongClick: (FavoriteSong) -> Unit = {},
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
                text = "收藏的歌曲",
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

        if (songs.isEmpty()) {
            EmptySongsPlaceholder()
        } else {
            // 最多展示 3 首
            val displaySongs = songs.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                displaySongs.forEach { song ->
                    FavoriteSongRow(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSongRow(
    song: FavoriteSong,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 封面
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            SongbookImage(
                model = song.coverUrl,
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 歌名 + 歌手
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title ?: "未知歌曲",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val artistName = song.artistName
            if (!artistName.isNullOrBlank()) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = SongbookColors.Outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 已收藏心形图标
        Icon(
            painter = painterResource(id = R.drawable.ic_favorite_filled),
            contentDescription = "已收藏",
            tint = SongbookColors.BurntOrange,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EmptySongsPlaceholder() {
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
                painter = painterResource(id = R.drawable.ic_favorite_border),
                contentDescription = null,
                tint = SongbookColors.Outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "还没有收藏的歌曲",
                style = MaterialTheme.typography.bodyMedium,
                color = SongbookColors.Outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "在播放界面点击♥收藏你喜欢的歌曲",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.Outline.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
