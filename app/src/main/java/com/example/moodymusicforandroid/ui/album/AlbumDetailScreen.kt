package com.example.moodymusicforandroid.ui.album

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.moodymusicforandroid.data.manager.UserManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.SongItem
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.player.MusicPlayState
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.playlist.AddToPlaylistSheet
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 专辑详情页 — 真实数据版（通过 AlbumDetailViewModel 从后端获取曲目）
 */
@Composable
fun AlbumDetailScreen(
    albumId: String = "",
    albumTitle: String = "",
    artistId: String = "",
    artistName: String = "",
    playerViewModel: PlayerViewModel? = null,
    playState: MusicPlayState = MusicPlayState(),
    currentPlayingTitle: String = playState.songTitle,
    isPlayingAudio: Boolean = playState.isPlaying,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onTrackClick: (songs: List<SongItem>, index: Int, coverUrl: String) -> Unit = { _, _, _ -> },
    onPlayAllClick: (songs: List<SongItem>, coverUrl: String) -> Unit = { _, _ -> }
) {
    val viewModel: AlbumDetailViewModel = viewModel(
        key = "AlbumDetailViewModel_${artistId}_$albumTitle",
        factory = viewModelFactory {
            initializer {
                val ssh = SavedStateHandle(
                    mapOf("artistId" to artistId, "albumTitle" to albumTitle)
                )
                AlbumDetailViewModel(ssh)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val livePlayState by (playerViewModel?.playState ?: remember { kotlinx.coroutines.flow.MutableStateFlow(playState) }).collectAsState()
    val currentPlay = if (playerViewModel != null) livePlayState else playState

    // 核心：基于全局播放服务状态计算当前专辑正在播放的唯一曲目（严格校对与去重，杜绝多曲目同时高亮与切歌失效）
    val activeTrackIndex = remember(
        uiState.songs,
        currentPlay.songTitle,
        currentPlay.audioUrl,
        currentPlay.albumTitle,
        currentPlay.artistName
    ) {
        findActiveTrackIndex(
            songs = uiState.songs,
            playState = currentPlay,
            currentAlbumTitle = albumTitle,
            currentArtistName = artistName
        )
    }

    val context = LocalContext.current
    var songToAddToPlaylist by remember { mutableStateOf<SongItem?>(null) }

    val listState = rememberLazyListState()
    val isStickyTitleVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 80)
        }
    }

    val iconTintColor by animateColorAsState(
        targetValue = if (isStickyTitleVisible) SongbookColors.BurntOrange else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "AlbumIconTintColor"
    )

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isStickyTitleVisible) {
                            Modifier.background(MaterialTheme.colorScheme.background)
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = iconTintColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
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
                        label = "AlbumTopBarTitle",
                        modifier = Modifier.weight(1f, fill = false)
                    ) { isSticky ->
                        Text(
                            text = if (isSticky) albumTitle else "专辑",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSticky) SongbookColors.BurntOrange else Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (isStickyTitleVisible) {
                    HorizontalDivider(
                        color = SongbookColors.OutlineVariant.copy(alpha = 0.2f),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. 专辑封面（使用 API 返回的 cover）
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    SongbookImage(
                        model = uiState.coverUrl,
                        contentDescription = albumTitle,
                        fallbackRes = R.drawable.hero_forest_mist,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
            }

            // 2. 专辑信息
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-40).dp)
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "ALBUM • ${uiState.releaseYear}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.MutedOlive,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = albumTitle,
                        style = MaterialTheme.typography.displayMedium,
                        color = SongbookColors.BurntOrange,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // 操作按钮区：全部播放 + 收藏专辑
                    val playableSongs = remember(uiState.songs) { uiState.songs.filter { !it.path.isNullOrBlank() } }
                    val favoriteAlbumIds by UserManager.favoriteAlbumIds.collectAsState()
                    val resolvedAlbumId = albumId.ifBlank { "${artistId}_${albumTitle}" }
                    val isAlbumFavorited = (resolvedAlbumId.isNotBlank() && resolvedAlbumId in favoriteAlbumIds) || (albumTitle.isNotBlank() && albumTitle in favoriteAlbumIds)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (playableSongs.isEmpty()) {
                                    Toast.makeText(context, "该专辑暂无可用音频资源", Toast.LENGTH_SHORT).show()
                                } else {
                                    onPlayAllClick(uiState.songs, uiState.coverUrl)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SongbookColors.BurntOrange,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f),
                            enabled = uiState.songs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "全部播放", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                        }

                        OutlinedButton(
                            onClick = {
                                UserManager.toggleFavoriteAlbum(
                                    albumId = resolvedAlbumId,
                                    title = albumTitle,
                                    cover = uiState.coverUrl,
                                    artistId = artistId
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isAlbumFavorited) SongbookColors.MutedOlive.copy(alpha = 0.15f) else Color.Transparent,
                                contentColor = if (isAlbumFavorited) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isAlbumFavorited) SongbookColors.BurntOrange else SongbookColors.GhostBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlbumFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isAlbumFavorited) "已收藏专辑" else "收藏专辑",
                                tint = if (isAlbumFavorited) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAlbumFavorited) "已收藏" else "收藏",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. 曲目列表头
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "曲目目录",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${uiState.songs.size} TRACKS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.Outline,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    HorizontalDivider(color = SongbookColors.OutlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 4. 加载中状态
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

            // 5. 曲目列表（真实数据）
            itemsIndexed(uiState.songs) { index, song ->
                val isCurrentSong = (index == activeTrackIndex)
                val hasAudio = !song.path.isNullOrBlank()
                TrackRowItem(
                    song = song,
                    index = index,
                    isPlaying = isCurrentSong,
                    isAudioPlaying = isCurrentSong && currentPlay.isPlaying,
                    hasAudio = hasAudio,
                    onClick = {
                        if (!hasAudio) {
                            Toast.makeText(context, "《${song.title}》暂无可用音频文件", Toast.LENGTH_SHORT).show()
                        } else {
                            onTrackClick(uiState.songs, index, uiState.coverUrl)
                        }
                    },
                    onMoreClick = {
                        songToAddToPlaylist = song
                    }
                )
            }

            // 6. 空状态（无数据且不在加载）
            if (!uiState.isLoading && uiState.songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.error != null) "加载失败，请下拉刷新" else "暂无曲目",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7. 底部留白，保证曲目列表滚动到底部时不被悬浮迷你播放器遮挡
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    // 点击曲目三点弹出的收录到手札抽屉 (AddToPlaylistSheet)
    if (songToAddToPlaylist != null) {
        val targetSong = songToAddToPlaylist!!
        val songId = remember(targetSong.title, artistName) {
            val key = "${targetSong.title.trim()}_${artistName.trim()}"
            key.hashCode().toLong() and 0x7FFFFFFF
        }
        AddToPlaylistSheet(
            visible = true,
            songId = songId,
            songTitle = targetSong.title,
            artistName = artistName,
            albumTitle = albumTitle,
            coverUrl = uiState.coverUrl,
            filePath = targetSong.path ?: "",
            currentQueue = playState.queue,
            onAddToCurrentQueue = if (playerViewModel != null) {
                {
                    playerViewModel.addToQueue(
                        audioUrl  = com.example.moodymusicforandroid.common.config.AppConfig.resolveStorageUrl(targetSong.path ?: ""),
                        songTitle = targetSong.title,
                        artistName = artistName,
                        albumTitle = albumTitle,
                        coverUrl  = uiState.coverUrl
                    )
                }
            } else null,
            onDismiss = { songToAddToPlaylist = null }
        )
    }
}

@Composable
private fun TrackRowItem(
    song: SongItem,
    index: Int,
    isPlaying: Boolean,
    isAudioPlaying: Boolean = false,
    hasAudio: Boolean = true,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    val activeColor = SongbookColors.BurntOrange
    val inactiveColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isPlaying) activeColor.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 曲目序号（轻字重，播放时显示高亮琥珀色）
        Text(
            text = String.format("%02d", (song.trackIndex ?: (index + 1))),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isPlaying -> activeColor
                !hasAudio -> SongbookColors.Outline.copy(alpha = 0.35f)
                else -> SongbookColors.Outline.copy(alpha = 0.65f)
            },
            fontWeight = if (isPlaying) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )

        // 歌曲标题（轻字重，播放时显示高亮色）
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isPlaying -> activeColor
                !hasAudio -> inactiveColor.copy(alpha = 0.38f)
                else -> inactiveColor
            },
            fontWeight = if (isPlaying) FontWeight.Medium else FontWeight.Normal,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 动态均衡器跳动动画图标（仿网页端，紧跟标题）
        if (isPlaying) {
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedEqualizer(
                tint = activeColor,
                isAnimating = isAudioPlaying
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧状态与操作（点击三点唤起收录到手札）
        if (!hasAudio) {
            Text(
                text = "未收录",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.Outline.copy(alpha = 0.45f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        } else {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "加入手札",
                    tint = if (isPlaying) activeColor.copy(alpha = 0.8f) else SongbookColors.Outline.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedEqualizer(
    tint: Color,
    modifier: Modifier = Modifier,
    isAnimating: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(animation = tween(380, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 0.20f,
        animationSpec = infiniteRepeatable(animation = tween(420, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(340, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.65f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(animation = tween(460, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "h4"
    )

    Canvas(modifier = modifier.size(width = 16.dp, height = 13.dp)) {
        val barCount = 4
        val spacing = 2.dp.toPx()
        val totalSpacing = spacing * (barCount - 1)
        val barWidth = (size.width - totalSpacing) / barCount
        val maxHeight = size.height

        val heights = if (isAnimating) {
            floatArrayOf(h1, h2, h3, h4)
        } else {
            floatArrayOf(0.4f, 0.7f, 0.5f, 0.3f)
        }

        for (i in 0 until barCount) {
            val barH = maxHeight * heights[i]
            val x = i * (barWidth + spacing)
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, maxHeight - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(1.2.dp.toPx())
            )
        }
    }
}

// 保持向后兼容
data class TrackItemData(
    val trackNumber: String,
    val title: String,
    val duration: String,
    val isPlaying: Boolean = false
)

/**
 * 全局统一计算当前专辑中正在播放的曲目索引（严格去重与校对）
 *
 * 校对规则：
 * 1. 基础校验：若全局未播放任何歌曲（songTitle 为空且 audioUrl 为空），返回 -1；
 * 2. 专辑与艺术家校对：如果正在播放的既不是当前专辑，也不是当前歌手，则不属于本专辑，不应高亮任何曲目；
 * 3. 音频 URL / Path 唯一校对（最高权重）：若歌曲的 path 包含在当前全局 playState.audioUrl 中，必定是该歌曲；
 * 4. 歌曲标题精确与模糊匹配（带符号/前缀容错）；
 * 5. 绝对去重：采用 indexOfFirst，整个列表中至多仅有一首歌曲被计算为高亮（返回唯一下标，未匹配返回 -1）。
 */
private fun findActiveTrackIndex(
    songs: List<SongItem>,
    playState: MusicPlayState,
    currentAlbumTitle: String,
    currentArtistName: String
): Int {
    if (playState.songTitle.isBlank() && playState.audioUrl.isBlank()) return -1
    if (songs.isEmpty()) return -1

    // 1. 最高精度：音频 path / audioUrl 路径精确校对
    if (playState.audioUrl.isNotBlank()) {
        val pathIndex = songs.indexOfFirst { song ->
            val path = song.path?.trim()?.removePrefix("/") ?: return@indexOfFirst false
            path.isNotBlank() && playState.audioUrl.contains(path, ignoreCase = true)
        }
        if (pathIndex != -1) return pathIndex
    }

    // 2. 所属专辑与歌手校对：防止同名歌曲在不同专辑中误高亮
    val isAlbumMatch = currentAlbumTitle.isNotBlank() && playState.albumTitle.isNotBlank() &&
        currentAlbumTitle.trim().equals(playState.albumTitle.trim(), ignoreCase = true)
    val isArtistMatch = currentArtistName.isNotBlank() && playState.artistName.isNotBlank() &&
        (playState.artistName.contains(currentArtistName, ignoreCase = true) ||
         currentArtistName.contains(playState.artistName, ignoreCase = true))

    // 如果全局正在播放的既不是当前专辑也不是当前歌手（且全局有明确的专辑或歌手信息），则判定非本专辑歌曲
    val hasContext = playState.albumTitle.isNotBlank() || playState.artistName.isNotBlank()
    if (hasContext && !isAlbumMatch && !isArtistMatch) {
        return -1
    }

    // 3. 歌曲标题精确对比（忽略前后空格与大小写）
    val targetTitle = playState.songTitle.trim()
    val exactTitleIndex = songs.indexOfFirst { song ->
        song.title.trim().equals(targetTitle, ignoreCase = true)
    }
    if (exactTitleIndex != -1) return exactTitleIndex

    // 4. 歌曲标题容错对比（过滤 "01. "、"01 - "、括号内副标题等）
    val cleanTarget = cleanSongTitle(targetTitle)
    val cleanTitleIndex = songs.indexOfFirst { song ->
        cleanSongTitle(song.title).equals(cleanTarget, ignoreCase = true)
    }
    if (cleanTitleIndex != -1) return cleanTitleIndex

    // 5. 包含关系容错
    return songs.indexOfFirst { song ->
        val s = song.title.trim()
        s.isNotBlank() && (targetTitle.contains(s, ignoreCase = true) || s.contains(targetTitle, ignoreCase = true))
    }
}

private val PREFIX_NUMBER_REGEX = Regex("^[0-9]+[\\.\\s_\\-]+")
private val BRACKET_COMMENT_REGEX = Regex("\\(.*?\\)|\\[.*?\\]|（.*?）|【.*?】")

private fun cleanSongTitle(raw: String): String {
    return raw.replace(PREFIX_NUMBER_REGEX, "") // 剥离 "01. " 等数字前缀
        .replace(BRACKET_COMMENT_REGEX, "") // 剥离括号注释
        .trim()
}
