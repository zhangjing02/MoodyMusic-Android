package com.example.moodymusicforandroid.ui.notice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.SystemNotice
import com.example.moodymusicforandroid.ui.community.CommunityViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 官方公告栏界面 (The Modern Songbook 现代颂歌风格)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    val notices by viewModel.notices.collectAsState()
    val userProfile by UserManager.userProfile.collectAsState()
    val isMasterOrAdmin = userProfile?.isAdmin() == true || userProfile?.isMaster() == true

    var showCreateDialog by remember { mutableStateOf(false) }
    var noticeToDelete by remember { mutableStateOf<SystemNotice?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchNotices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OFFICIAL NOTICES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.BurntOrange,
                            letterSpacing = 2.sp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "音信公告",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = SongbookColors.SoftCharcoal
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchNotices() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新公告",
                            tint = SongbookColors.SoftCharcoal
                        )
                    }
                    if (isMasterOrAdmin) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "发布新公告",
                                tint = SongbookColors.BurntOrange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SongbookColors.PaperBackground
                )
            )
        },
        containerColor = SongbookColors.PaperBackground
    ) { innerPadding ->
        if (notices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无系统公告",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(notices, key = { it.id }) { notice ->
                    NoticeCard(
                        notice = notice,
                        canDelete = isMasterOrAdmin,
                        onDeleteClick = { noticeToDelete = notice }
                    )
                }
            }
        }

        // 删除确认对话框
        if (noticeToDelete != null) {
            AlertDialog(
                onDismissRequest = { noticeToDelete = null },
                title = { Text("确认删除公告") },
                text = { Text("确定要删除《${noticeToDelete?.title}》吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            noticeToDelete?.let { viewModel.deleteNotice(it.id) {} }
                            noticeToDelete = null
                        }
                    ) {
                        Text("确认删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noticeToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // 发布公告对话框
        if (showCreateDialog) {
            CreateNoticeDialog(
                onDismiss = { showCreateDialog = false },
                onConfirm = { title, content, isPinned ->
                    viewModel.createNotice(title, content, isPinned) {
                        showCreateDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun NoticeCard(
    notice: SystemNotice,
    canDelete: Boolean,
    onDeleteClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(notice.isPinned) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SongbookColors.GhostBorder, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // 顶栏：标签 + 时间 + 删除
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (notice.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SongbookColors.BurntOrange.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "置顶",
                                    tint = SongbookColors.BurntOrange,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "置顶公告",
                                    color = SongbookColors.BurntOrange,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = notice.createdAt?.take(10) ?: "近期",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canDelete) {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除公告",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 标题
            Text(
                text = notice.title,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = SongbookColors.SoftCharcoal,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 正文（支持展开收起）
            Text(
                text = notice.content,
                style = MaterialTheme.typography.bodyMedium,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
                lineHeight = 22.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 发布人署名
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "—— ${notice.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.BurntOrange,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun CreateNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发布官方公告", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("公告标题") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("公告正文内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 6
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it }
                    )
                    Text("设为置顶公告", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onConfirm(title.trim(), content.trim(), isPinned)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
            ) {
                Text("立即发布")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
