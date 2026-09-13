package com.example.moodymusicforandroid.ui.collection

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.data.model.LibraryAlbumItem
import com.example.moodymusicforandroid.data.model.LibraryArtistItem
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 我的收藏与资产全量管理页面
 * 包含单曲、专辑、歌手三个 Tab，支持单条快捷取消与批量编辑删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionManagerScreen(
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSongClick: (FavoriteSong) -> Unit = {},
    onAlbumClick: (albumId: String, title: String) -> Unit = { _, _ -> },
    onArtistClick: (artistId: String, name: String) -> Unit = { _, _ -> }
) {
    var selectedTabIndex by remember { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    var isEditMode by remember { mutableStateOf(false) }

    // 选中状态跟踪
    val selectedSongIds = remember { mutableStateListOf<Long>() }
    val selectedAlbumIds = remember { mutableStateListOf<String>() }
    val selectedArtistIds = remember { mutableStateListOf<String>() }

    // 删除确认弹窗
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 监听数据流
    val favoriteSongs by UserManager.favoriteSongsList.collectAsState()

    // 专辑与歌手数据
    var albumsList by remember { mutableStateOf<List<LibraryAlbumItem>>(emptyList()) }
    var artistsList by remember { mutableStateOf<List<LibraryArtistItem>>(emptyList()) }

    val isLoggedIn by UserManager.isLoggedIn.collectAsState()

    // 从云端或缓存拉取全量 library 列表
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            albumsList = emptyList()
            artistsList = emptyList()
            isEditMode = false
            return@LaunchedEffect
        }
        try {
            val libRes = com.example.moodymusicforandroid.data.api.MoodyApiProvider.apiService.getUserLibrary()
            val rawData = libRes.data
            if (libRes.isSuccess() && rawData != null) {
                var data: com.example.moodymusicforandroid.data.model.UserLibraryResponse = rawData
                // 容错补全歌手名与头像
                val hasMissing = data.followedArtists.any { it.name.isNullOrBlank() || it.avatar.isNullOrBlank() }
                if (hasMissing) {
                    val skeleton = com.example.moodymusicforandroid.data.api.MoodyApiProvider.apiService.getArtists()
                    val skeletonData = skeleton.data
                    if (skeleton.isSuccess() && skeletonData != null) {
                        val map = skeletonData.artists.associateBy { it.id }
                        val enriched = data.followedArtists.map { item ->
                            val match = map[item.artistId]
                            if (match != null) {
                                item.copy(
                                    name = item.name?.takeIf { it.isNotBlank() } ?: match.name,
                                    avatar = item.avatar?.takeIf { it.isNotBlank() } ?: match.avatar
                                )
                            } else item
                        }
                        data = data.copy(followedArtists = enriched)
                    }
                }
                albumsList = data.favoriteAlbums
                artistsList = data.followedArtists
            }
        } catch (_: Exception) {}
    }

    // 切换 Tab 时重置选态
    LaunchedEffect(selectedTabIndex) {
        selectedSongIds.clear()
        selectedAlbumIds.clear()
        selectedArtistIds.clear()
        isEditMode = false
    }

    val tabTitles = listOf(
        "单曲 (${favoriteSongs.size})",
        "专辑 (${albumsList.size})",
        "歌手 (${artistsList.size})"
    )

    val currentItemsCount = when (selectedTabIndex) {
        0 -> favoriteSongs.size
        1 -> albumsList.size
        else -> artistsList.size
    }

    val currentSelectedCount = when (selectedTabIndex) {
        0 -> selectedSongIds.size
        1 -> selectedAlbumIds.size
        else -> selectedArtistIds.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的收藏",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (currentItemsCount > 0) {
                        TextButton(onClick = {
                            isEditMode = !isEditMode
                            if (!isEditMode) {
                                selectedSongIds.clear()
                                selectedAlbumIds.clear()
                                selectedArtistIds.clear()
                            }
                        }) {
                            Text(
                                text = if (isEditMode) "完成" else "编辑",
                                color = SongbookColors.BurntOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isEditMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAllSelected = when (selectedTabIndex) {
                            0 -> selectedSongIds.size == favoriteSongs.size && favoriteSongs.isNotEmpty()
                            1 -> selectedAlbumIds.size == albumsList.size && albumsList.isNotEmpty()
                            else -> selectedArtistIds.size == artistsList.size && artistsList.isNotEmpty()
                        }

                        TextButton(onClick = {
                            if (isAllSelected) {
                                selectedSongIds.clear()
                                selectedAlbumIds.clear()
                                selectedArtistIds.clear()
                            } else {
                                when (selectedTabIndex) {
                                    0 -> {
                                        selectedSongIds.clear()
                                        selectedSongIds.addAll(favoriteSongs.map { it.songId })
                                    }
                                    1 -> {
                                        selectedAlbumIds.clear()
                                        selectedAlbumIds.addAll(albumsList.map { it.albumId })
                                    }
                                    2 -> {
                                        selectedArtistIds.clear()
                                        selectedArtistIds.addAll(artistsList.map { it.artistId })
                                    }
                                }
                            }
                        }) {
                            Text(
                                text = if (isAllSelected) "取消全选" else "全选",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                if (currentSelectedCount > 0) {
                                    showDeleteConfirmDialog = true
                                }
                            },
                            enabled = currentSelectedCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SongbookColors.BurntOrange,
                                disabledContainerColor = SongbookColors.GhostBorder
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            val actionText = if (selectedTabIndex == 2) "取消关注" else "取消收藏"
                            Text(
                                text = "$actionText ($currentSelectedCount)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = SongbookColors.BurntOrange,
                        height = 3.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = SongbookColors.GhostBorder.copy(alpha = 0.5f))
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) SongbookColors.BurntOrange else SongbookColors.Outline
                            )
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> {
                    if (favoriteSongs.isEmpty()) {
                        EmptyCollectionPlaceholder(text = "还没有收藏的单曲")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favoriteSongs, key = { it.songId }) { song ->
                                val isSelected = song.songId in selectedSongIds
                                SongCollectionItemRow(
                                    song = song,
                                    isEditMode = isEditMode,
                                    isSelected = isSelected,
                                    onItemClick = {
                                        if (isEditMode) {
                                            if (isSelected) selectedSongIds.remove(song.songId)
                                            else selectedSongIds.add(song.songId)
                                        } else {
                                            onSongClick(song)
                                        }
                                    },
                                    onSingleDelete = {
                                        UserManager.toggleFavoriteSong(song.songId)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (albumsList.isEmpty()) {
                        EmptyCollectionPlaceholder(text = "还没有收藏的专辑")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(albumsList, key = { it.albumId }) { album ->
                                val isSelected = album.albumId in selectedAlbumIds
                                val albumTitle = album.title?.takeIf { it.isNotBlank() }
                                    ?: album.albumId.takeIf { !it.startsWith("album_") && !it.startsWith("db_") }
                                    ?: "精选专辑"
                                AlbumCollectionItemRow(
                                    album = album,
                                    title = albumTitle,
                                    isEditMode = isEditMode,
                                    isSelected = isSelected,
                                    onItemClick = {
                                        if (isEditMode) {
                                            if (isSelected) selectedAlbumIds.remove(album.albumId)
                                            else selectedAlbumIds.add(album.albumId)
                                        } else {
                                            onAlbumClick(album.albumId, albumTitle)
                                        }
                                    },
                                    onSingleDelete = {
                                        UserManager.toggleFavoriteAlbum(album.albumId)
                                        albumsList = albumsList.filterNot { it.albumId == album.albumId }
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    if (artistsList.isEmpty()) {
                        EmptyCollectionPlaceholder(text = "还没有关注的歌手")
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(artistsList, key = { it.artistId }) { artist ->
                                val isSelected = artist.artistId in selectedArtistIds
                                val artistName = artist.name?.takeIf { it.isNotBlank() } ?: "未知歌手"
                                ArtistCollectionItemRow(
                                    artist = artist,
                                    name = artistName,
                                    isEditMode = isEditMode,
                                    isSelected = isSelected,
                                    onItemClick = {
                                        if (isEditMode) {
                                            if (isSelected) selectedArtistIds.remove(artist.artistId)
                                            else selectedArtistIds.add(artist.artistId)
                                        } else {
                                            onArtistClick(artist.artistId, artistName)
                                        }
                                    },
                                    onSingleUnfollow = {
                                        UserManager.toggleFollowArtist(artist.artistId)
                                        artistsList = artistsList.filterNot { it.artistId == artist.artistId }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val typeDesc = when (selectedTabIndex) {
            0 -> "单曲"
            1 -> "专辑"
            else -> "歌手"
        }
        val actionDesc = if (selectedTabIndex == 2) "取消关注" else "取消收藏"

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(text = "确认$actionDesc", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "确定要$actionDesc 选中的 $currentSelectedCount 个$typeDesc 吗？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        when (selectedTabIndex) {
                            0 -> {
                                UserManager.batchRemoveFavoriteSongs(selectedSongIds.toSet())
                                selectedSongIds.clear()
                            }
                            1 -> {
                                val ids = selectedAlbumIds.toSet()
                                UserManager.batchRemoveFavoriteAlbums(ids)
                                albumsList = albumsList.filterNot { it.albumId in ids }
                                selectedAlbumIds.clear()
                            }
                            2 -> {
                                val ids = selectedArtistIds.toSet()
                                UserManager.batchRemoveFollowedArtists(ids)
                                artistsList = artistsList.filterNot { it.artistId in ids }
                                selectedArtistIds.clear()
                            }
                        }
                        isEditMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                ) {
                    Text("确定", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
private fun SongCollectionItemRow(
    song: FavoriteSong,
    isEditMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onSingleDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SongbookColors.MutedOlive.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SongbookColors.BurntOrange,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            SongbookImage(
                model = song.coverUrl,
                contentDescription = song.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val artist = song.artistName
                if (!artist.isNullOrBlank()) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = SongbookColors.Outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isEditMode) {
                IconButton(onClick = onSingleDelete) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "取消收藏",
                        tint = SongbookColors.BurntOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCollectionItemRow(
    album: LibraryAlbumItem,
    title: String,
    isEditMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onSingleDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SongbookColors.MutedOlive.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SongbookColors.BurntOrange,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            SongbookImage(
                model = album.cover,
                contentDescription = title,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(6.dp))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "专辑",
                    style = MaterialTheme.typography.bodySmall,
                    color = SongbookColors.Outline
                )
            }

            if (!isEditMode) {
                OutlinedButton(
                    onClick = onSingleDelete,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SongbookColors.BurntOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "已收藏",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.BurntOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCollectionItemRow(
    artist: LibraryArtistItem,
    name: String,
    isEditMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onSingleUnfollow: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) SongbookColors.MutedOlive.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorder.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SongbookColors.BurntOrange,
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            SongbookImage(
                model = artist.avatar,
                contentDescription = name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "艺术家",
                    style = MaterialTheme.typography.bodySmall,
                    color = SongbookColors.Outline
                )
            }

            if (!isEditMode) {
                OutlinedButton(
                    onClick = onSingleUnfollow,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SongbookColors.BurntOrange),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "已关注",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.BurntOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCollectionPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = SongbookColors.GhostBorder,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = SongbookColors.Outline
            )
        }
    }
}
