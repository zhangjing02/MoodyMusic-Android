package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.data.local.db.PlaylistEntity
import com.example.moodymusicforandroid.ui.playlist.getPlaylistThemeColor
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 音信页面：音乐手札展架 (PlaylistsSection)
 * 极简克制、高意境设计
 */
@Composable
fun PlaylistsSection(
    playlists: List<PlaylistEntity>,
    modifier: Modifier = Modifier,
    onPlaylistClick: (playlistId: Long, name: String) -> Unit = { _, _ -> },
    onPlayPlaylistClick: (playlist: PlaylistEntity) -> Unit = {},
    onCreateNewClick: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 区域头部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "音乐手札",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (playlists.isNotEmpty()) {
                    Text(
                        text = "${playlists.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.Outline,
                        fontSize = 12.sp
                    )
                }
            }

            // 仅在已有手札时在右上角显示「＋ 新建」
            if (playlists.isNotEmpty()) {
                Text(
                    text = "＋ 新建",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onCreateNewClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 列表区域
        if (playlists.isEmpty()) {
            EmptyPlaylistsCard(onCreateClick = onCreateNewClick)
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistTextCard(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id, playlist.name) }
                    )
                }
            }
        }
    }
}

/**
 * 极简手札卡片
 */
@Composable
private fun PlaylistTextCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    val themeColor = getPlaylistThemeColor(playlist.themeColor)

    Card(
        modifier = Modifier
            .defaultMinSize(minWidth = 120.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
        border = BorderStroke(1.dp, SongbookColors.GhostBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 主题色点
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(themeColor)
            )

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${playlist.songCount} 首",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.Outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * 空状态：极简「新建」卡片
 */
@Composable
private fun EmptyPlaylistsCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCreateClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, SongbookColors.GhostBorderActive)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = SongbookColors.BurntOrange,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "新建",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = SongbookColors.Outline
            )
        }
    }
}