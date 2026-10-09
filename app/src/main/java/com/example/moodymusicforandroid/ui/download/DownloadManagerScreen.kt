package com.example.moodymusicforandroid.ui.download

import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.local.db.DownloadedSongEntity
import com.example.moodymusicforandroid.data.manager.DownloadStatus
import com.example.moodymusicforandroid.data.manager.DownloadTask
import com.example.moodymusicforandroid.data.manager.OfflineDownloadManager
import com.example.moodymusicforandroid.ui.album.AlbumDownloadIcon
import com.example.moodymusicforandroid.ui.components.DownloadedTrackBadge
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 离线音乐下载管理中心 (DownloadManagerScreen)
 *
 * 核心功能：
 * 1. 顶部空间看板：直观展示已下载曲目数量与磁盘占用体积；
 * 2. 双 Tab 分页：
 *    - 「下载中」：排队并发进度、实时网速/百分比、暂停/继续/取消；
 *    - 「已下载」：本地离线曲目列表、本地优先 0ms 秒播、单曲删除与一键清空；
 * 3. 资源状态感知：自动标记绿色 [本地] 与黄色 [可更新] 徽标。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerScreen(
    playerViewModel: PlayerViewModel? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var songToDelete by remember { mutableStateOf<DownloadedSongEntity?>(null) }

    val downloadTasks by OfflineDownloadManager.tasksFlow.collectAsState()
    val downloadedSongs by OfflineDownloadManager.downloadedSongsFlow.collectAsState()

    val downloadingTaskList = remember(downloadTasks) {
        downloadTasks.values.toList()
    }

    val totalStorageBytes = remember(downloadedSongs) {
        downloadedSongs.sumOf { it.fileSize }
    }
    val formattedStorageSize = remember(totalStorageBytes) {
        Formatter.formatFileSize(context, totalStorageBytes)
    }

    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.background
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val outline = MaterialTheme.colorScheme.outline
    val ghostBorder = SongbookColors.GhostBorder

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "下载管理",
                        style = MaterialTheme.typography.titleLarge,
                        color = onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = onSurface
                        )
                    }
                },
                actions = {
                    if (selectedTabIndex == 1 && downloadedSongs.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirmDialog = true }) {
                            Text(
                                text = "清空",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface
                )
            )
        },
        containerColor = background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── 1. 存储空间概览看板 ────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, ghostBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "本地离线曲库空间",
                            style = MaterialTheme.typography.labelSmall,
                            color = outline,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "已离线 ${downloadedSongs.size} 首歌曲 · 占用 $formattedStorageSize",
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AlbumDownloadIcon(
                                tint = primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "私有加密",
                                color = primary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // ── 2. 分页 Tab 栏 ─────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = surface,
                contentColor = primary,
                divider = {
                    HorizontalDivider(color = ghostBorder.copy(alpha = 0.6f), thickness = 0.5.dp)
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "下载中 (${downloadingTaskList.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "已下载 (${downloadedSongs.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }

            // ── 3. Tab 内容区 ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (selectedTabIndex == 0) {
                    DownloadingTabContent(
                        tasks = downloadingTaskList,
                        onPause = { task -> OfflineDownloadManager.pauseTask(task.filePath) },
                        onResume = { task -> OfflineDownloadManager.resumeTask(task.filePath) },
                        onCancel = { task -> OfflineDownloadManager.cancelTask(task.filePath) }
                    )
                } else {
                    DownloadedTabContent(
                        songs = downloadedSongs,
                        onPlaySong = { song ->
                            playerViewModel?.playSingleUrl(
                                audioUrl = song.filePath,
                                songTitle = song.title,
                                artistName = song.artistName,
                                albumTitle = song.albumTitle,
                                coverUrl = song.coverUrl,
                                lrcPath = song.lrcPath
                            )
                        },
                        onDeleteSong = { song -> songToDelete = song }
                    )
                }
            }
        }
    }

    // 单曲删除二次确认
    if (songToDelete != null) {
        val target = songToDelete!!
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text(text = "删除本地离线歌曲", fontWeight = FontWeight.Bold) },
            text = { Text(text = "确定要从本地存储中删除《${target.title}》吗？删除后将释放存储空间。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        OfflineDownloadManager.deleteDownloadedSong(target.filePath)
                        songToDelete = null
                        Toast.makeText(context, "已删除《${target.title}》", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) {
                    Text(text = "取消")
                }
            }
        )
    }

    // 清空全部二次确认
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(text = "清空所有已下载歌曲", fontWeight = FontWeight.Bold) },
            text = { Text(text = "此操作将永久清理所有本地 .moody 私有缓存音频与歌词文件，操作无法撤销。确定要清空吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        OfflineDownloadManager.clearAllDownloads()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "已清空所有离线歌曲", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(text = "全部清空", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

/**
 * 「下载中」Tab 列表
 */
@Composable
private fun DownloadingTabContent(
    tasks: List<DownloadTask>,
    onPause: (DownloadTask) -> Unit,
    onResume: (DownloadTask) -> Unit,
    onCancel: (DownloadTask) -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline
    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AlbumDownloadIcon(
                    tint = outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "当前没有正在排队或下载的任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = outline
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tasks, key = { it.filePath }) { task ->
                DownloadingItemCard(
                    task = task,
                    onPause = { onPause(task) },
                    onResume = { onResume(task) },
                    onCancel = { onCancel(task) }
                )
            }
        }
    }
}

@Composable
private fun DownloadingItemCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val ghostBorder = SongbookColors.GhostBorder

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, ghostBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 封面
                SongbookImage(
                    model = task.coverUrl,
                    contentDescription = task.title,
                    fallbackRes = R.drawable.hero_forest_mist,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${task.artistName} · ${task.albumTitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.5.sp
                    )
                }

                // 暂停 / 继续 / 取消按钮
                when (task.status) {
                    is DownloadStatus.DOWNLOADING -> {
                        IconButton(onClick = onPause, modifier = Modifier.size(32.dp)) {
                            Text(text = "⏸", fontSize = 14.sp)
                        }
                    }
                    is DownloadStatus.PAUSED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "继续",
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    is DownloadStatus.FAILED -> {
                        IconButton(onClick = onResume, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重试",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    else -> {}
                }

                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "取消任务",
                        tint = outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 进度条与状态文案
            when (val status = task.status) {
                is DownloadStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = primary,
                        trackColor = primary.copy(alpha = 0.18f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val currentStr = Formatter.formatFileSize(context, status.currentBytes)
                        val totalStr = if (status.totalBytes > 0) Formatter.formatFileSize(context, status.totalBytes) else "--"
                        Text(
                            text = "$currentStr / $totalStr",
                            fontSize = 10.sp,
                            color = outline
                        )
                        Text(
                            text = "${(status.progress * 100).toInt()}%",
                            fontSize = 10.sp,
                            color = primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is DownloadStatus.QUEUED -> {
                    Text(
                        text = "正在排队中，等待空闲带宽...",
                        fontSize = 11.sp,
                        color = outline
                    )
                }
                is DownloadStatus.PAUSED -> {
                    Text(
                        text = "已暂停下载",
                        fontSize = 11.sp,
                        color = outline
                    )
                }
                is DownloadStatus.FAILED -> {
                    Text(
                        text = "下载失败: ${status.reason} (点击重试)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                DownloadStatus.COMPLETED -> {
                    Text(
                        text = "下载完成",
                        fontSize = 11.sp,
                        color = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

/**
 * 「已下载」Tab 列表
 */
@Composable
private fun DownloadedTabContent(
    songs: List<DownloadedSongEntity>,
    onPlaySong: (DownloadedSongEntity) -> Unit,
    onDeleteSong: (DownloadedSongEntity) -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AlbumDownloadIcon(
                    tint = outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无已下载的离线歌曲",
                    style = MaterialTheme.typography.bodyMedium,
                    color = outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "进入任意专辑页面，点击「下载专辑」即可离线至本地",
                    style = MaterialTheme.typography.bodySmall,
                    color = outline.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(songs, key = { it.filePath }) { song ->
                DownloadedItemCard(
                    song = song,
                    onClick = { onPlaySong(song) },
                    onDelete = { onDeleteSong(song) }
                )
            }
        }
    }
}

@Composable
private fun DownloadedItemCard(
    song: DownloadedSongEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val sizeStr = remember(song.fileSize) {
        Formatter.formatFileSize(context, song.fileSize)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceLow)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SongbookImage(
            model = song.coverUrl,
            contentDescription = song.title,
            fallbackRes = R.drawable.hero_forest_mist,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                DownloadedTrackBadge(
                    isHashOutdated = false
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${song.artistName} · ${song.albumTitle} · $sizeStr",
                style = MaterialTheme.typography.bodySmall,
                color = outline,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "删除歌曲",
                tint = outline.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
