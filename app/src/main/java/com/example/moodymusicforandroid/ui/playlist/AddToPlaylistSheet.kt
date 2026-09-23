package com.example.moodymusicforandroid.ui.playlist

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.res.painterResource
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.local.db.PlaylistEntity
import com.example.moodymusicforandroid.data.manager.PlaylistManager
import com.example.moodymusicforandroid.ui.player.AddToQueueResult
import com.example.moodymusicforandroid.ui.player.PlayQueueItem
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlinx.coroutines.launch

/**
 * 听歌归档抽屉 (AddToPlaylistSheet)
 * 纯粹、无打扰的半屏多选面板，支持一歌多归属即点即存（Zero-Confirmation）与快速新建清单
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    visible: Boolean,
    songId: Long,
    songTitle: String,
    artistName: String,
    albumTitle: String,
    coverUrl: String,
    filePath: String,
    duration: Int = 0,
    currentQueue: List<PlayQueueItem> = emptyList(),
    onAddToCurrentQueue: (() -> AddToQueueResult)? = null,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val playlists by PlaylistManager.playlists.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // 记录这首歌当前隶属于哪些清单 ID
    var memberPlaylistIds by remember(songId) { mutableStateOf<Set<Long>>(emptySet()) }
    var showCreateInput by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // 每次打开抽屉与切歌时刷新数据与归属状态
    LaunchedEffect(Unit) {
        PlaylistManager.refreshPlaylists()
    }

    // 初始化查询归属状态
    LaunchedEffect(songId) {
        if (songId > 0) {
            memberPlaylistIds = PlaylistManager.getSongMembershipIds(songId)
        }
    }

    // 「加入当前播放列表」勾选状态：从传入 queue 派生，点击成功后乐观翻转，不依赖重开 Sheet 刷新
    // 注意：filePath 可能是相对路径，队列里的 audioUrl 是完整 CDN URL，需归一化后比对
    val context = LocalContext.current
    val resolvedFilePath = remember(filePath) {
        com.example.moodymusicforandroid.common.config.AppConfig.resolveStorageUrl(filePath)
    }
    var isInQueue by remember(currentQueue, resolvedFilePath) {
        mutableStateOf(currentQueue.any { it.audioUrl == resolvedFilePath })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SongbookColors.SurfaceLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = SongbookColors.GhostBorderActive)
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // 顶栏：标题 + 快速新建按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "收录到音乐手札",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = songTitle.ifBlank { "当前歌曲" },
                        style = MaterialTheme.typography.bodySmall,
                        color = SongbookColors.Outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(
                    onClick = { showCreateInput = !showCreateInput },
                    colors = ButtonDefaults.textButtonColors(contentColor = SongbookColors.BurntOrange)
                ) {
                    Icon(
                        imageVector = if (showCreateInput) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCreateInput) "取消" else "新建",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 展开式单行新建输入框
            AnimatedVisibility(
                visible = showCreateInput,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = {
                            Text("例如：交响乐、民谣、深夜自留地...", fontSize = 13.sp, color = SongbookColors.Outline)
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SongbookColors.BurntOrange,
                            unfocusedBorderColor = SongbookColors.GhostBorder
                        )
                    )

                    Button(
                        onClick = {
                            val name = newPlaylistName.trim()
                            if (name.isNotEmpty()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                PlaylistManager.createPlaylist(name) { newId ->
                                    // 新建后自动将当前歌曲勾选加入
                                    PlaylistManager.toggleSongInPlaylist(
                                        playlistId = newId,
                                        songId = songId,
                                        title = songTitle,
                                        artistName = artistName,
                                        albumTitle = albumTitle,
                                        coverUrl = coverUrl,
                                        filePath = filePath,
                                        duration = duration,
                                        isAdd = true
                                    )
                                    memberPlaylistIds = memberPlaylistIds + newId
                                }
                                newPlaylistName = ""
                                showCreateInput = false
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                    ) {
                        Text("创建并收录", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── 固定置顶行：加入当前播放列表 (YouTube 极简操作项风格) ──
            if (onAddToCurrentQueue != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (isInQueue) {
                                // 已在队列：Toast 提示
                                Toast.makeText(context, "「${songTitle.ifBlank { "该歌曲" }}」已在当前播放列表中", Toast.LENGTH_SHORT).show()
                            } else {
                                val result = onAddToCurrentQueue()
                                when (result) {
                                    AddToQueueResult.ADDED, AddToQueueResult.STARTED_NEW -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // 添加成功即刻收起弹窗，顺畅干脆
                                        onDismiss()
                                    }
                                    AddToQueueResult.DUPLICATE -> {
                                        isInQueue = true
                                        Toast.makeText(context, "「${songTitle.ifBlank { "该歌曲" }}」已在当前播放列表中", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 左侧：标准播放列表图标
                    Icon(
                        painter = painterResource(id = R.drawable.ic_playlist),
                        contentDescription = null,
                        tint = if (isInQueue) SongbookColors.BurntOrange else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )

                    // 中间：单行清晰操作选项文本，绝无冗余说明
                    Text(
                        text = "加入当前播放列表",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // 右侧：若已收录在当前待播队列中，显示一个优雅极简的对钩
                    if (isInQueue) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = SongbookColors.BurntOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(color = SongbookColors.GhostBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // 清单列表
            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有音乐手札，点击右上角「新建」开启手札",
                        style = MaterialTheme.typography.bodySmall,
                        color = SongbookColors.Outline
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val isSelected = memberPlaylistIds.contains(playlist.id)

                        PlaylistItemRow(
                            playlist = playlist,
                            isSelected = isSelected,
                            onToggle = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val nextState = !isSelected
                                memberPlaylistIds = if (nextState) {
                                    memberPlaylistIds + playlist.id
                                } else {
                                    memberPlaylistIds - playlist.id
                                }
                                PlaylistManager.toggleSongInPlaylist(
                                    playlistId = playlist.id,
                                    songId = songId,
                                    title = songTitle,
                                    artistName = artistName,
                                    albumTitle = albumTitle,
                                    coverUrl = coverUrl,
                                    filePath = filePath,
                                    duration = duration,
                                    isAdd = nextState
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistItemRow(
    playlist: PlaylistEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val themeColor = getPlaylistThemeColor(playlist.themeColor)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggle() }
            .background(if (isSelected) SongbookColors.SurfaceHigh else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 主题磁带色标点
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(themeColor)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
        )

        // 清单标题与曲数
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${playlist.songCount} 首",
                style = MaterialTheme.typography.bodySmall,
                color = SongbookColors.Outline,
                fontSize = 11.5.sp
            )
        }

        // 极简圆环 Checkbox 勾选器
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isSelected) SongbookColors.BurntOrange else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorderActive,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 辅助获取复古磁带主题预设色
 */
fun getPlaylistThemeColor(code: String): Color {
    return when (code.uppercase()) {
        "SUNSET_ORANGE", "ORANGE" -> Color(0xFFE65100)
        "FOREST_GREEN", "GREEN" -> Color(0xFF2E7D32)
        "MIDNIGHT_BLUE", "BLUE" -> Color(0xFF1565C0)
        "DEEP_DARK", "DARK" -> Color(0xFF212121)
        "CHERRY_VINTAGE", "RED" -> Color(0xFFAD1457)
        "GOLDEN_AMBER", "YELLOW" -> Color(0xFFF57F17)
        else -> Color(0xFF8D6E63) // 默认复古棕
    }
}
