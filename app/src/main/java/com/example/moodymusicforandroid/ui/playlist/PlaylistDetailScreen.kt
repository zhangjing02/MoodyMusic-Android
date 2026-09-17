package com.example.moodymusicforandroid.ui.playlist

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.local.db.PlaylistEntity
import com.example.moodymusicforandroid.data.local.db.PlaylistSongEntity
import com.example.moodymusicforandroid.data.manager.PlaylistManager
import com.example.moodymusicforandroid.data.model.SongItem
import com.example.moodymusicforandroid.ui.album.AnimatedEqualizer
import com.example.moodymusicforandroid.ui.player.MusicPlayState
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 歌单/手札详情页面 (PlaylistDetailScreen)
 * 采用与歌手专辑 (AlbumDetailScreen) 一致的高级感曲目列表呈现
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    initialPlaylistName: String,
    playerViewModel: PlayerViewModel? = null,
    playState: MusicPlayState = MusicPlayState(),
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPlayAll: (songs: List<PlaylistSongEntity>, startIndex: Int, isShuffle: Boolean) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val allPlaylists by PlaylistManager.playlists.collectAsState()
    val playlist = remember(allPlaylists, playlistId) {
        allPlaylists.find { it.id == playlistId }
    }
    val currentName = playlist?.name ?: initialPlaylistName

    val songsFlow = remember(playlistId) { PlaylistManager.getSongsFlow(playlistId) }
    val songs by songsFlow.collectAsState(initial = emptyList())

    // 播放状态监听
    val livePlayState by (playerViewModel?.playState ?: remember { kotlinx.coroutines.flow.MutableStateFlow(playState) }).collectAsState()
    val currentPlay = if (playerViewModel != null) livePlayState else playState

    // 正在播放的歌曲下标计算
    val activeTrackIndex = remember(songs, currentPlay.songTitle, currentPlay.audioUrl) {
        if (currentPlay.songTitle.isBlank() && currentPlay.audioUrl.isBlank()) -1
        else {
            val pathMatch = songs.indexOfFirst { s ->
                val path = s.filePath?.trim()?.removePrefix("/") ?: return@indexOfFirst false
                path.isNotBlank() && currentPlay.audioUrl.contains(path, ignoreCase = true)
            }
            if (pathMatch != -1) pathMatch
            else {
                songs.indexOfFirst { s ->
                    s.title.trim().equals(currentPlay.songTitle.trim(), ignoreCase = true)
                }
            }
        }
    }

    // 首次进入时静默加载云端可能有的歌曲
    LaunchedEffect(playlistId) {
        PlaylistManager.getSongs(playlistId)
    }

    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "音乐手札",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
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
                    IconButton(onClick = { showMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "更多选项")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑手札信息") },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("归档删除手札", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. 顶部手札信息区（专辑风骨）
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "PLAYLIST • ${songs.size} TRACKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.MutedOlive,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentName,
                        style = MaterialTheme.typography.displayMedium,
                        color = SongbookColors.BurntOrange,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium
                    )
                    if (!playlist?.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = playlist!!.description!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    // 操作按钮区：全部播放 + 随机播放
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (songs.isEmpty()) {
                                    Toast.makeText(context, "手札暂无歌曲", Toast.LENGTH_SHORT).show()
                                } else {
                                    onPlayAll(songs, 0, false)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SongbookColors.BurntOrange,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier.weight(1.2f),
                            enabled = songs.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "全部播放",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (songs.isEmpty()) {
                                    Toast.makeText(context, "手札暂无歌曲", Toast.LENGTH_SHORT).show()
                                } else {
                                    onPlayAll(songs, 0, true)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(1.dp, SongbookColors.GhostBorder),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            modifier = Modifier.weight(0.8f),
                            enabled = songs.isNotEmpty()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shuffle),
                                contentDescription = "Shuffle",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "随机",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 2. 曲目列表头（与专辑一致）
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
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
                            text = "${songs.size} TRACKS",
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

            // 3. 歌曲列表（呈现为与歌手专辑一致的简洁 TrackRowItem 风格）
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "手札暂无曲目，可在播放歌曲时收录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(songs, key = { _, s -> "${s.playlistId}_${s.songId}" }) { index, song ->
                    val isCurrentSong = (index == activeTrackIndex)
                    val hasAudio = !song.filePath.isNullOrBlank()
                    PlaylistTrackRowItem(
                        song = song,
                        index = index,
                        isPlaying = isCurrentSong,
                        isAudioPlaying = isCurrentSong && currentPlay.isPlaying,
                        hasAudio = hasAudio,
                        onClick = {
                            if (!hasAudio) {
                                Toast.makeText(context, "《${song.title}》暂无音频文件", Toast.LENGTH_SHORT).show()
                            } else {
                                onPlayAll(songs, index, false)
                            }
                        },
                        onRemove = {
                            PlaylistManager.toggleSongInPlaylist(
                                playlistId = playlistId,
                                songId = song.songId,
                                title = song.title,
                                isAdd = false
                            )
                        }
                    )
                }
            }

            // 4. 底部留白
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // 编辑弹窗
    if (showEditDialog && playlist != null) {
        EditPlaylistDialog(
            playlist = playlist,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName, newDesc, newColor ->
                PlaylistManager.updatePlaylist(playlistId, newName, newDesc, newColor)
                showEditDialog = false
            }
        )
    }

    // 删除确认弹窗
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("归档并删除手札", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = { Text("确定要删除这本手札吗？原曲库里的音乐依然完整保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        PlaylistManager.deletePlaylist(playlistId)
                        showDeleteConfirmDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("取消")
                }
            },
            containerColor = SongbookColors.SurfaceLow,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 仿歌手专辑曲目列表的优雅单行组件
 */
@Composable
private fun PlaylistTrackRowItem(
    song: PlaylistSongEntity,
    index: Int,
    isPlaying: Boolean,
    isAudioPlaying: Boolean = false,
    hasAudio: Boolean = true,
    onClick: () -> Unit,
    onRemove: () -> Unit
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
            text = String.format("%02d", index + 1),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isPlaying -> activeColor
                !hasAudio -> SongbookColors.Outline.copy(alpha = 0.35f)
                else -> SongbookColors.Outline.copy(alpha = 0.65f)
            },
            fontWeight = if (isPlaying) FontWeight.Medium else FontWeight.Normal,
            modifier = Modifier.width(32.dp)
        )

        // 歌曲标题与歌手（轻字重，播放时高亮）
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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

                // 动态均衡器跳动动画图标（紧跟标题）
                if (isPlaying) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedEqualizer(
                        tint = activeColor,
                        isAnimating = isAudioPlaying
                    )
                }
            }

            if (!song.artistName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artistName!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPlaying) activeColor.copy(alpha = 0.8f) else SongbookColors.Outline,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 右侧操作按钮：从手札移除
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "移出手札",
                tint = if (isPlaying) activeColor.copy(alpha = 0.6f) else SongbookColors.Outline.copy(alpha = 0.35f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * 编辑歌单弹窗
 */
@Composable
private fun EditPlaylistDialog(
    playlist: PlaylistEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, desc: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var desc by remember { mutableStateOf(playlist.description ?: "") }
    var selectedColor by remember { mutableStateOf(playlist.themeColor) }

    val colors = listOf(
        "DEFAULT" to Color(0xFF8D6E63),
        "SUNSET_ORANGE" to Color(0xFFE65100),
        "FOREST_GREEN" to Color(0xFF2E7D32),
        "MIDNIGHT_BLUE" to Color(0xFF1565C0),
        "DEEP_DARK" to Color(0xFF212121),
        "CHERRY_VINTAGE" to Color(0xFFAD1457)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑手札属性", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("手札名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("手札寄语 / 描述") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), desc.trim(), selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
            ) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = SongbookColors.SurfaceLow,
        shape = RoundedCornerShape(16.dp)
    )
}