package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.LibraryArtistItem
import com.example.moodymusicforandroid.data.model.getDisplayName
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 关注艺人横向滚动展示组件 (FollowedArtistsSection)
 * 接收服务器返回的真实歌手列表；空状态显示引导卡片。
 */
@Composable
fun FollowedArtistsSection(
    artists: List<LibraryArtistItem>,
    modifier: Modifier = Modifier,
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onBrowseAllClick: () -> Unit = {}
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
                text = "关注的艺术家",
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
                    .clickable { onBrowseAllClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (artists.isEmpty()) {
            // 空状态引导
            EmptyArtistsPlaceholder()
        } else {
            val displayArtists = artists.take(4)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(displayArtists) { artist ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onArtistClick(artist.artistId, artist.getDisplayName(), artist.avatar) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(1.5.dp, SongbookColors.MutedOlive.copy(alpha = 0.3f), CircleShape)
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            ) {
                                SongbookImage(
                                    model = artist.avatar,
                                    contentDescription = artist.getDisplayName(),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = artist.getDisplayName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyArtistsPlaceholder() {
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
                painter = painterResource(id = R.drawable.ic_playlist),
                contentDescription = null,
                tint = SongbookColors.Outline,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "还没有关注的艺术家",
                style = MaterialTheme.typography.bodyMedium,
                color = SongbookColors.Outline,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "去歌手主页点击「关注」吧",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.Outline.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
