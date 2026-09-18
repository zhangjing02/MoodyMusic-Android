package com.example.moodymusicforandroid.ui.artist

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.AlbumWithSongs
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

private val NON_DIGIT_REGEX = Regex("\\D")

@Composable
fun ArtistDetailScreen(
    artistId: String = "",
    artistName: String = "",
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onAlbumClick: (artistId: String, albumTitle: String) -> Unit = { _, _ -> },
    onPlayAllClick: () -> Unit = {}
) {
    val viewModel: ArtistDetailViewModel = viewModel(
        key = "ArtistDetailViewModel_$artistId",
        factory = viewModelFactory {
            initializer {
                val ssh = SavedStateHandle(mapOf("artistId" to artistId, "artistName" to artistName))
                ArtistDetailViewModel(ssh)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val followedArtistIds by UserManager.followedArtistIds.collectAsState()
    val isFollowing = artistId.isNotBlank() && artistId in followedArtistIds
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("按时间排序", "按热度排序", "录音室专辑")

    val listState = rememberLazyListState()
    val displayName = uiState.artistName.ifBlank { artistName }
    val isStickyTitleVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 30)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SongbookColors.BurntOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = isStickyTitleVisible,
                        transitionSpec = {
                            if (targetState) {
                                (slideInVertically { height -> height / 2 } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height / 2 } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> height / 2 } + fadeOut())
                            }
                        },
                        label = "ArtistTopBarTitle",
                        modifier = Modifier.weight(1f, fill = false)
                    ) { isSticky ->
                        Text(
                            text = if (isSticky) displayName else "艺术家",
                            style = MaterialTheme.typography.titleLarge,
                            color = SongbookColors.BurntOrange,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isStickyTitleVisible) {
                    HorizontalDivider(color = SongbookColors.OutlineVariant.copy(alpha = 0.2f))
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. 艺术家封面（优先用 API 头像，否则用占位）
            item {
                val currentAvatar = uiState.artistAvatar
                val resolvedHeroModel = remember(currentAvatar, artistId) {
                    val hasCustom = !currentAvatar.isNullOrBlank() &&
                        !currentAvatar.contains("default.png") &&
                        !currentAvatar.contains("landing_cover.png") &&
                        !currentAvatar.startsWith("/src/")
                    if (hasCustom) {
                        com.example.moodymusicforandroid.common.config.AppConfig.resolveUrl(currentAvatar)
                    } else {
                        val rawId = artistId.replace(NON_DIGIT_REGEX, "")
                        if (rawId.isNotBlank()) {
                            "file:///android_asset/avatars/artists/artist_$rawId.jpg"
                        } else {
                            ""
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    SongbookImage(
                        model = resolvedHeroModel,
                        contentDescription = artistName,
                        fallbackRes = R.drawable.artist_abigail_chen,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = "ARTIST",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 艺术家名称
            item {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.displayMedium,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. 数据面板与操作按钮
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
                    ) {
                        Column {
                            Text(
                                text = "专辑数量",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.Outline,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.albums.size} Albums",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column {
                            Text(
                                text = "歌曲总数",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.Outline,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${uiState.albums.sumOf { it.songs.size }} Tracks",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // 关注与播放按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (artistId.isNotBlank()) {
                                    val name = uiState.artistName.ifBlank { artistName }
                                    val avatar = uiState.artistAvatar
                                    UserManager.toggleFollowArtist(artistId, name, avatar)
                                }
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) SongbookColors.MutedOlive else SongbookColors.BurntOrange,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isFollowing) "已关注" else "关注歌手", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = onPlayAllClick,
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(text = "播放全部", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(36.dp))
            }

            // 3. 作品全集标题
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "作品全集",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            tabs.forEachIndexed { index, tabName ->
                                val isSelected = selectedTab == index
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    modifier = Modifier.clickable { selectedTab = index }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SongbookColors.OutlineVariant.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 4. 加载状态
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SongbookColors.BurntOrange)
                    }
                }
            }

            // 5. 错误状态
            if (uiState.error != null && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "加载失败",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 6. 双列专辑网格（真实数据）
            if (!uiState.isLoading && uiState.albums.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        val albums = uiState.albums
                        for (i in albums.indices step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ArtistAlbumCard(
                                    album = albums[i],
                                    modifier = Modifier.weight(1f),
                                    onClick = { onAlbumClick(artistId, albums[i].title) }
                                )
                                val albumRight = albums.getOrNull(i + 1)
                                if (albumRight != null) {
                                    ArtistAlbumCard(
                                        album = albumRight,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onAlbumClick(artistId, albumRight.title) }
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
    }
}

@Composable
private fun ArtistAlbumCard(
    album: AlbumWithSongs,
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
                model = album.cover,
                contentDescription = album.title,
                fallbackRes = R.drawable.album_afternoon_echo,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${album.year} • ${album.songs.size} TRACKS",
            style = MaterialTheme.typography.labelSmall,
            color = SongbookColors.Outline,
            maxLines = 1
        )
    }
}

// 保留旧版数据类型兼容性（如果其他地方引用了 ArtistAlbumItem）
data class ArtistAlbumItem(
    val id: String,
    val title: String,
    val yearAndTracks: String,
    val imageUrl: String,
    val fallbackRes: Int
)
