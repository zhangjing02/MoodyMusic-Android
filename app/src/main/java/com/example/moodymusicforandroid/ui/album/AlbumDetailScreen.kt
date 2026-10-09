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
import com.example.moodymusicforandroid.data.manager.OfflineDownloadManager
import com.example.moodymusicforandroid.data.manager.DownloadStatus
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.home.components.HeadphonesVinylCanvas
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
import com.example.moodymusicforandroid.ui.components.DownloadedTrackBadge
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

    LaunchedEffect(artistId, albumTitle) {
        if (viewModel.uiState.value.songs.isEmpty() && (albumTitle.isNotBlank() || artistId.isNotBlank())) {
            viewModel.loadAlbumDetail()
        }
    }
    val livePlayState by (playerViewModel?.playState ?: remember { kotlinx.coroutines.flow.MutableStateFlow(playState) }).collectAsState()
    val currentPlay = if (playerViewModel != null) livePlayState else playState
    val downloadTasks by OfflineDownloadManager.tasksFlow.collectAsState()
    val downloadedSongs by OfflineDownloadManager.downloadedSongsFlow.collectAsState()

    // 服务端驱动 Tab 分组：
    // 优先使用服务端返回的 disc_name 字段进行分组（零硬编码，任意多个 Tab）；
    // 若服务端无 disc_name，则回退到兼容条件（disc==2 / trackIndex>=100 / mood=="导师考核与对决"）；
    // 分组后按 disc_name 的首次出现顺序排列 Tab，Tab 标签直接取 disc_name 值。
    val albumTabs = remember(uiState.songs) {
        val hasDsicName = uiState.songs.any { !it.discName.isNullOrBlank() }
        if (hasDsicName) {
            // ─── 路径 A：服务端已提供 disc_name，完全服务端驱动 ───
            // 保持 disc_name 首次出现顺序
            val orderedLabels = uiState.songs
                .mapNotNull { it.discName?.trim()?.takeIf { n -> n.isNotEmpty() } }
                .distinct()
            if (orderedLabels.size <= 1) {
                // 只有一组（或全为 null）→ 不显示 Tab
                emptyList()
            } else {
                orderedLabels.map { label ->
                    label to uiState.songs.filter { it.discName?.trim() == label }
                }
            }
        } else {
            // ─── 路径 B：向后兼容（disc 字段 / trackIndex / mood 条件）───
            val disc2Songs = uiState.songs.filter {
                (it.disc ?: 1) == 2 || (it.trackIndex ?: 0) >= 100 || it.mood == "导师考核与对决"
            }
            if (disc2Songs.isEmpty()) {
                emptyList()
            } else {
                val disc1Songs = uiState.songs.filter { it !in disc2Songs }
                listOf(
                    "第一轮盲选" to disc1Songs,
                    "导师考核与PK" to disc2Songs
                )
            }
        }
    }
    val hasMultipleTabs = albumTabs.size > 1
    var selectedTabIndex by androidx.compose.runtime.saveable.rememberSaveable(albumTitle, artistId) {
        mutableIntStateOf(0)
    }
    val currentDisplaySongs = if (hasMultipleTabs) {
        albumTabs.getOrNull(selectedTabIndex.coerceIn(0, albumTabs.lastIndex))?.second
            ?: uiState.songs
    } else {
        uiState.songs
    }

    // 核心：基于全局播放服务状态计算当前专辑正在播放的唯一曲目（严格校对与去重，杜绝多曲目同时高亮与切歌失效）
    val activeTrackIndex = remember(
        currentDisplaySongs,
        currentPlay.songTitle,
        currentPlay.audioUrl,
        currentPlay.albumTitle,
        currentPlay.artistName
    ) {
        findActiveTrackIndex(
            songs = currentDisplaySongs,
            playState = currentPlay,
            currentAlbumTitle = albumTitle,
            currentArtistName = artistName
        )
    }

    val context = LocalContext.current
    var songToAddToPlaylist by remember { mutableStateOf<SongItem?>(null) }
    var showDownloadAlbumConfirmDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isStickyTitleVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 ||
                (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 80)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val iconTintColor by animateColorAsState(
        targetValue = if (isStickyTitleVisible) primaryColor else Color.White,
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
                            color = if (isSticky) primaryColor else Color.White,
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
                        color = MaterialTheme.colorScheme.primary,
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
                    val playableSongs = remember(currentDisplaySongs) { currentDisplaySongs.filter { !it.path.isNullOrBlank() } }
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
                                    Toast.makeText(context, "当前阶段暂无可用音频资源", Toast.LENGTH_SHORT).show()
                                } else {
                                    onPlayAllClick(currentDisplaySongs, uiState.coverUrl)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1f),
                            enabled = currentDisplaySongs.isNotEmpty()
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
                                containerColor = if (isAlbumFavorited) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = if (isAlbumFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isAlbumFavorited) MaterialTheme.colorScheme.primary else SongbookColors.GhostBorder
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = if (isAlbumFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isAlbumFavorited) "已收藏专辑" else "收藏专辑",
                                tint = if (isAlbumFavorited) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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

            // 3. 曲目列表头（支持单列表与多阶段 Tab 无缝切换）
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    if (hasMultipleTabs) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                albumTabs.forEachIndexed { tabIndex, (tabTitle, _) ->
                                    val isSelected = (tabIndex == selectedTabIndex)
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedTabIndex = tabIndex }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (isSelected) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .height(2.5.dp)
                                                .width(if (isSelected) 36.dp else 0.dp)
                                                .background(
                                                    if (isSelected) SongbookColors.BurntOrange else Color.Transparent,
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                                if (uiState.allowDownload) {
                                    IconButton(
                                        onClick = { showDownloadAlbumConfirmDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        AlbumDownloadIcon(
                                            tint = SongbookColors.BurntOrange,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${currentDisplaySongs.size} TRACKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.Outline,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "曲目目录",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                if (uiState.allowDownload) {
                                    IconButton(
                                        onClick = { showDownloadAlbumConfirmDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        AlbumDownloadIcon(
                                            tint = SongbookColors.BurntOrange,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${uiState.songs.size} TRACKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.Outline,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // 5. 曲目列表（真实数据，按当前 Tab 显示）
            itemsIndexed(currentDisplaySongs) { index, song ->
                val isCurrentSong = (index == activeTrackIndex)
                val hasAudio = !song.path.isNullOrBlank()
                val task = downloadTasks[song.path]
                val isDownloaded = OfflineDownloadManager.isDownloaded(
                    filePath = song.path,
                    songId = song.id,
                    title = song.title,
                    albumTitle = albumTitle
                )
                val isHashOutdated = OfflineDownloadManager.isHashOutdated(
                    filePath = song.path,
                    serverHash = song.fileHash,
                    songId = song.id,
                    title = song.title,
                    albumTitle = albumTitle
                )

                TrackRowItem(
                    song = song,
                    index = index,
                    isPlaying = isCurrentSong,
                    isAudioPlaying = isCurrentSong && currentPlay.isPlaying,
                    hasAudio = hasAudio,
                    downloadStatus = task?.status,
                    isDownloaded = isDownloaded,
                    isHashOutdated = isHashOutdated,
                    onClick = {
                        if (!hasAudio) {
                            Toast.makeText(context, "《${song.title}》暂无可用音频文件", Toast.LENGTH_SHORT).show()
                        } else {
                            onTrackClick(currentDisplaySongs, index, uiState.coverUrl)
                        }
                    },
                    onMoreClick = {
                        songToAddToPlaylist = song
                    }
                )
            }

            // 6. 空状态（无数据且不在加载）
            if (!uiState.isLoading && currentDisplaySongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.error != null) "加载失败，请下拉刷新" else "该阶段暂无收录曲目",
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
            allowDownload = uiState.allowDownload,
            onDownloadClick = {
                OfflineDownloadManager.enqueueSong(
                    song = targetSong,
                    albumTitle = albumTitle,
                    artistName = artistName,
                    coverUrl = uiState.coverUrl
                )
                Toast.makeText(context, "已加入下载队列: 《${targetSong.title}》", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { songToAddToPlaylist = null }
        )
    }

    // 批量下载整张专辑二次确认弹窗
    if (showDownloadAlbumConfirmDialog) {
        val validSongs = currentDisplaySongs.filter { !it.path.isNullOrBlank() }
        AlertDialog(
            onDismissRequest = { showDownloadAlbumConfirmDialog = false },
            title = {
                Text(
                    text = "下载专辑",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SongbookColors.SoftCharcoal
                )
            },
            text = {
                Text(
                    text = "确认下载这 ${validSongs.size} 首歌曲？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDownloadAlbumConfirmDialog = false
                        if (validSongs.isEmpty()) {
                            Toast.makeText(context, "当前暂无可下载音频", Toast.LENGTH_SHORT).show()
                        } else {
                            val count = OfflineDownloadManager.enqueueAlbum(
                                songs = validSongs,
                                albumTitle = albumTitle,
                                artistName = artistName,
                                coverUrl = uiState.coverUrl
                            )
                            if (count > 0) {
                                Toast.makeText(context, "已加入下载队列", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "歌曲已全部下载", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text(
                        text = "下载",
                        color = SongbookColors.BurntOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadAlbumConfirmDialog = false }) {
                    Text(
                        text = "取消",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
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
    downloadStatus: DownloadStatus? = null,
    isDownloaded: Boolean = false,
    isHashOutdated: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {}
) {
    val activeColor = MaterialTheme.colorScheme.primary
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
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

        // 歌曲标题与动态下载进度区
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // 动态均衡器跳动动画图标（仿网页端，紧跟标题）
                if (isPlaying) {
                    Spacer(modifier = Modifier.width(6.dp))
                    AnimatedEqualizer(
                        tint = activeColor,
                        isAnimating = isAudioPlaying
                    )
                }
            }

            // 动态横向细长下载进度条与排队状态
            if (downloadStatus is DownloadStatus.DOWNLOADING) {
                val progress = downloadStatus.progress
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(end = 12.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = SongbookColors.BurntOrange,
                        trackColor = SongbookColors.BurntOrange.copy(alpha = 0.18f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 9.5.sp,
                        color = SongbookColors.BurntOrange,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else if (downloadStatus is DownloadStatus.QUEUED) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "排队下载中...",
                    fontSize = 9.5.sp,
                    color = SongbookColors.BurntOrange.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 右侧状态与操作（离线状态微标 + 三点更多菜单）
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isDownloaded) {
                DownloadedTrackBadge(
                    isHashOutdated = isHashOutdated
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
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
}

/**
 * 现代黑胶唱片风格 — 下载矢量 Canvas 图标
 */
@Composable
fun AlbumDownloadIcon(
    tint: Color = SongbookColors.BurntOrange,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.8.dp.toPx()
        // 箭头主轴
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.15f),
            end = Offset(w * 0.5f, h * 0.62f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // 箭头左羽
        drawLine(
            color = tint,
            start = Offset(w * 0.28f, h * 0.42f),
            end = Offset(w * 0.5f, h * 0.62f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // 箭头右羽
        drawLine(
            color = tint,
            start = Offset(w * 0.72f, h * 0.42f),
            end = Offset(w * 0.5f, h * 0.62f),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        // 托盘底座
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.22f, h * 0.65f)
            lineTo(w * 0.22f, h * 0.85f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.78f, h * 0.65f)
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
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

    // 1. 唯一匹配核心铁律：跨专辑绝对隔离
    // 若当前查看的专辑与正在播放的专辑都具有明确标题且不一致，坚决不高亮任何曲目
    if (currentAlbumTitle.isNotBlank() && playState.albumTitle.isNotBlank()) {
        val cleanCurrentAlbum = cleanAlbumOrTitle(currentAlbumTitle)
        val cleanPlayAlbum = cleanAlbumOrTitle(playState.albumTitle)
        if (!cleanCurrentAlbum.equals(cleanPlayAlbum, ignoreCase = true)) {
            return -1
        }
    }

    // 2. 歌手校对：若当前页面歌手与全局播放状态的歌手都明确存在且不匹配，返回 -1
    if (currentArtistName.isNotBlank() && playState.artistName.isNotBlank()) {
        val cleanCurrentArtist = cleanAlbumOrTitle(currentArtistName)
        val cleanPlayArtist = cleanAlbumOrTitle(playState.artistName)
        val isArtistMatch = cleanPlayArtist.contains(cleanCurrentArtist, ignoreCase = true) ||
                            cleanCurrentArtist.contains(cleanPlayArtist, ignoreCase = true)
        if (!isArtistMatch) {
            return -1
        }
    }

    // 3. 专辑内最高精度校对：音频 path / audioUrl 路径精确校对
    if (playState.audioUrl.isNotBlank()) {
        val pathIndex = songs.indexOfFirst { song ->
            val path = song.path?.trim()?.removePrefix("/") ?: return@indexOfFirst false
            path.isNotBlank() && playState.audioUrl.contains(path, ignoreCase = true)
        }
        if (pathIndex != -1) return pathIndex
    }

    // 4. 歌曲标题精确对比（忽略前后空格与大小写）
    val targetTitle = playState.songTitle.trim()
    val exactTitleIndex = songs.indexOfFirst { song ->
        song.title.trim().equals(targetTitle, ignoreCase = true)
    }
    if (exactTitleIndex != -1) return exactTitleIndex

    // 5. 歌曲标题容错对比（过滤 "01. "、"01 - "、括号内副标题等）
    val cleanTarget = cleanSongTitle(targetTitle)
    val cleanTitleIndex = songs.indexOfFirst { song ->
        cleanSongTitle(song.title).equals(cleanTarget, ignoreCase = true)
    }
    if (cleanTitleIndex != -1) return cleanTitleIndex

    // 6. 包含关系容错
    return songs.indexOfFirst { song ->
        val s = song.title.trim()
        s.isNotBlank() && (targetTitle.contains(s, ignoreCase = true) || s.contains(targetTitle, ignoreCase = true))
    }
}

private fun cleanAlbumOrTitle(raw: String): String {
    return raw.replace(PREFIX_NUMBER_REGEX, "")
        .replace(BRACKET_COMMENT_REGEX, "")
        .replace(Regex("[\\s·•_\\-]+"), "")
        .trim()
}

private val PREFIX_NUMBER_REGEX = Regex("^[0-9]+[\\.\\s_\\-]+")
private val BRACKET_COMMENT_REGEX = Regex("\\(.*?\\)|\\[.*?\\]|（.*?）|【.*?】")

private fun cleanSongTitle(raw: String): String {
    return raw.replace(PREFIX_NUMBER_REGEX, "") // 剥离 "01. " 等数字前缀
        .replace(BRACKET_COMMENT_REGEX, "") // 剥离括号注释
        .trim()
}
