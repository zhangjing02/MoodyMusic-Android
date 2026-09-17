package com.example.moodymusicforandroid.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.CommunityComment
import com.example.moodymusicforandroid.data.model.CommunityPost
import com.example.moodymusicforandroid.data.model.ResourceHelper
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 社区帖子详情与全员盖楼评论界面 (The Modern Songbook 现代颂歌风格)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Long,
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val post = posts.firstOrNull { it.id == postId }

    val commentsMap by viewModel.commentsMap.collectAsState()
    val comments = commentsMap[postId].orEmpty()

    val isLoggedIn by UserManager.isLoggedIn.collectAsState()
    val userProfile by UserManager.userProfile.collectAsState()
    val isMasterOrAdmin = userProfile?.isAdmin() == true || userProfile?.isMaster() == true

    var commentInput by remember { mutableStateOf("") }
    var commentToDelete by remember { mutableStateOf<CommunityComment?>(null) }
    var postToDelete by remember { mutableStateOf<CommunityPost?>(null) }

    LaunchedEffect(postId) {
        viewModel.fetchComments(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "讨论详情",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.SoftCharcoal
                    )
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
                    if (post != null && (isMasterOrAdmin || (isLoggedIn && post.userId == userProfile?.userId))) {
                        IconButton(onClick = { postToDelete = post }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除帖子",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SongbookColors.PaperBackground
                )
            )
        },
        bottomBar = {
            // 底部常驻评论输入栏
            Surface(
                color = SongbookColors.SurfaceLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isLoggedIn) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SongbookColors.SurfaceHighest,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clickable(onClick = onNavigateToAuth)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "登录后参与讨论与回帖",
                                    color = SongbookColors.BurntOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("撰写你的回响与评论...", fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp, max = 100.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SongbookColors.BurntOrange,
                                unfocusedBorderColor = SongbookColors.GhostBorder,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                val text = commentInput.trim()
                                if (text.isNotBlank()) {
                                    viewModel.createComment(postId, text) {
                                        commentInput = ""
                                    }
                                }
                            },
                            enabled = commentInput.isNotBlank(),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (commentInput.isNotBlank()) SongbookColors.BurntOrange else SongbookColors.SurfaceHighest
                                )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "发送",
                                tint = if (commentInput.isNotBlank()) Color.White else SongbookColors.SoftCharcoal.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = SongbookColors.PaperBackground
    ) { innerPadding ->
        if (post == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "帖子不存在或已被移除",
                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                )
            }
        } else {
            val isCompleted = post.isCompleted()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                // ── 1. 主贴卡片 ─────────────────────────────────────────
                item(key = "main_post") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SongbookColors.GhostBorder, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // 分类徽章与状态
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SongbookColors.BurntOrange.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = post.getCategoryDisplayName(),
                                            color = SongbookColors.BurntOrange,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }

                                    if (post.isTask) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        val statusBg = when (post.status) {
                                            "completed" -> Color(0xFFE8F5E9)
                                            "in_progress" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFECEFF1)
                                        }
                                        val statusTextCol = when (post.status) {
                                            "completed" -> Color(0xFF2E7D32)
                                            "in_progress" -> Color(0xFFEF6C00)
                                            else -> Color(0xFF546E7A)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = statusBg,
                                            modifier = Modifier.clickable(enabled = isMasterOrAdmin) {
                                                viewModel.toggleTaskStatus(post)
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (post.status == "completed") {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = statusTextCol,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                }
                                                Text(
                                                    text = post.getStatusDisplayName(),
                                                    color = statusTextCol,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (isMasterOrAdmin) {
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text(text = "✎", color = statusTextCol, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = post.createdAt?.take(16) ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 标题
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted && post.isTask) SongbookColors.SoftCharcoal.copy(alpha = 0.5f) else SongbookColors.SoftCharcoal,
                                textDecoration = if (isCompleted && post.isTask) TextDecoration.LineThrough else TextDecoration.None,
                                lineHeight = 26.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val resourceData = remember(post.content) {
                                if (post.category == "resource") ResourceHelper.decode(post.content) else null
                            }

                            if (resourceData != null) {
                                ResourceAlignmentTable(
                                    data = resourceData,
                                    isCompleted = isCompleted,
                                    isCompact = false
                                )
                            } else {
                                // 正文
                                Text(
                                    text = post.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
                                    lineHeight = 24.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 作者信息
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(SongbookColors.BurntOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = SongbookColors.BurntOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = post.authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SongbookColors.SoftCharcoal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 讨论分隔标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "全部讨论 (${comments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── 2. 评论盖楼列表 ─────────────────────────────────────
                if (comments.isEmpty()) {
                    item(key = "empty_comments") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无评论，留下你的第一声回响吧",
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.45f),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        val canDeleteComment = isMasterOrAdmin || (isLoggedIn && comment.userId == userProfile?.userId)
                        CommentItemRow(
                            comment = comment,
                            canDelete = canDeleteComment,
                            onDelete = { commentToDelete = comment }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // 删除帖子确认弹窗
        if (postToDelete != null) {
            AlertDialog(
                onDismissRequest = { postToDelete = null },
                title = { Text("确认删除帖子") },
                text = { Text("删除后不可恢复，确定要彻底删除吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            postToDelete?.let {
                                viewModel.deletePost(it.id) {
                                    onBackClick()
                                }
                            }
                            postToDelete = null
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { postToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }

        // 删除评论确认弹窗
        if (commentToDelete != null) {
            AlertDialog(
                onDismissRequest = { commentToDelete = null },
                title = { Text("确认删除评论") },
                text = { Text("确定要删除这条回复吗？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            commentToDelete?.let {
                                viewModel.deleteComment(postId, it.id)
                            }
                            commentToDelete = null
                        }
                    ) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { commentToDelete = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: CommunityComment,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SongbookColors.SurfaceLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SongbookColors.BurntOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comment.authorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = SongbookColors.SoftCharcoal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comment.createdAt?.take(16) ?: "",
                        fontSize = 11.sp,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.45f)
                    )
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除评论",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.content,
                fontSize = 13.5.sp,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
                lineHeight = 20.sp
            )
        }
    }
}
