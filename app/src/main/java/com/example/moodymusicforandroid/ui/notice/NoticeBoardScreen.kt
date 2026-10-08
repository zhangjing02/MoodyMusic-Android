package com.example.moodymusicforandroid.ui.notice

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.SystemNotice
import com.example.moodymusicforandroid.ui.community.CommunityViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 官方公告栏界面 (The Modern Songbook 现代颂歌风格)
 * 1:1 对齐 Flutter NoticeBoardScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeBoardScreen(
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit
) {
    val rawNotices by viewModel.notices.collectAsState()
    val isNoticeLoading by viewModel.isNoticeLoading.collectAsState()
    val notices = remember(rawNotices) { CommunityViewModel.sortNotices(rawNotices) }
    val userProfile by UserManager.userProfile.collectAsState()
    val isMasterOrAdmin = userProfile?.isAdmin() == true || userProfile?.isMaster() == true

    var showCreateDialog by remember { mutableStateOf(false) }
    var noticeToDelete by remember { mutableStateOf<SystemNotice?>(null) }

    // 刷新按钮持续旋转动画（加载中平滑旋转）
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

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
                            color = MaterialTheme.colorScheme.primary,
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
                            tint = if (isNoticeLoading) MaterialTheme.colorScheme.primary else SongbookColors.SoftCharcoal,
                            modifier = Modifier.rotate(if (isNoticeLoading) spinAngle else 0f)
                        )
                    }
                    if (isMasterOrAdmin) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "发布新公告",
                                tint = MaterialTheme.colorScheme.primary
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
        if (isNoticeLoading && notices.isEmpty()) {
            // 首次进页面正在拉取数据时的 Loading 状态，避免闪现“暂无公告”
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "正在获取音信公告...",
                        style = MaterialTheme.typography.bodySmall,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (notices.isEmpty()) {
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
                onConfirm = { title, content, tag, isPinned ->
                    viewModel.createNotice(title, content, tag, isPinned) {
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
    var isExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "arrow_rotation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
        border = BorderStroke(1.dp, SongbookColors.GhostBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isExpanded = !isExpanded
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
                .padding(18.dp)
        ) {
            // 顶栏：标签胶囊 + 时间 + 删除/展开
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NoticeTagBadge(notice = notice)

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
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
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

            // 正文（支持网页链接识别、跟随复制按钮与外链卡片）
            NoticeContent(
                content = notice.content,
                isExpanded = isExpanded
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
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

/**
 * 标签徽章渲染（置顶胶囊 + 业务标签胶囊）
 */
@Composable
private fun NoticeTagBadge(notice: SystemNotice) {
    val tag = if (notice.tag.isNotEmpty()) notice.tag else (if (notice.isPinned) "置顶" else "官方通知")

    val (badgeBg, textColor, borderColor, icon) = when (tag) {
        "置顶" -> TagBadgeStyle(
            bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            textColor = MaterialTheme.colorScheme.primary,
            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            icon = Icons.Default.Star
        )
        "版本信息" -> TagBadgeStyle(
            bgColor = Color(0xFF34C759).copy(alpha = 0.14f),
            textColor = Color(0xFF28A745),
            borderColor = Color(0xFF28A745).copy(alpha = 0.4f),
            icon = Icons.Default.Info
        )
        "新资源预告" -> TagBadgeStyle(
            bgColor = Color(0xFFAF52DE).copy(alpha = 0.14f),
            textColor = Color(0xFFAF52DE),
            borderColor = Color(0xFFAF52DE).copy(alpha = 0.4f),
            icon = Icons.Default.Star
        )
        "系统维护" -> TagBadgeStyle(
            bgColor = Color(0xFFFF9500).copy(alpha = 0.14f),
            textColor = Color(0xFFFF9500),
            borderColor = Color(0xFFFF9500).copy(alpha = 0.4f),
            icon = Icons.Default.Build
        )
        else -> TagBadgeStyle(
            bgColor = SongbookColors.SoftCharcoal.copy(alpha = 0.08f),
            textColor = SongbookColors.SoftCharcoal.copy(alpha = 0.75f),
            borderColor = SongbookColors.SoftCharcoal.copy(alpha = 0.22f),
            icon = Icons.Default.Notifications
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        // 如果被设为置顶且当前标签不是置顶本身，展示专属置顶前缀徽章
        if (notice.isPinned && tag != "置顶") {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "置顶",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "置顶",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 主业务标签徽章
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeBg,
            border = BorderStroke(0.8.dp, borderColor),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = tag,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = tag,
                    color = textColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class TagBadgeStyle(
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val icon: ImageVector
)

/**
 * 公告正文（支持网页链接自动高亮识别、链接后紧跟复制按钮、以及展开时的外链快捷卡片）
 */
@Composable
private fun NoticeContent(
    content: String,
    isExpanded: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val urlRegex = remember { Regex("""(https?://[^\s\u4e00-\u9fa5，。？！（）《》“”\n\r]+)""") }

    if (!urlRegex.containsMatchIn(content)) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
            lineHeight = 22.sp,
            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )
        return
    }

    val lines = remember(content) { content.lines() }
    val displayLines = if (isExpanded) lines else lines.take(3)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        displayLines.forEach { line ->
            val match = urlRegex.find(line)
            if (match == null) {
                if (line.isNotEmpty()) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
                        lineHeight = 22.sp
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                val url = match.value
                val annotatedLine = buildAnnotatedString {
                    val range = match.range
                    if (range.first > 0) {
                        append(line.substring(0, range.first))
                    }
                    val start = length
                    append(url)
                    val end = length
                    addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        ),
                        start = start,
                        end = end
                    )
                    if (range.last + 1 < line.length) {
                        append(line.substring(range.last + 1))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = annotatedLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.85f),
                            lineHeight = 22.sp
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(url))
                            Toast.makeText(context, "已复制链接", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.moodymusicforandroid.R.drawable.ic_copy),
                            contentDescription = "复制链接",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                            modifier = Modifier.size(13.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, tag: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("官方通知") }
    var isPinned by remember { mutableStateOf(false) }

    val tagOptions = listOf("官方通知", "版本信息", "新资源预告", "系统维护", "置顶")

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

                // 标签选择
                Column {
                    Text(
                        text = "公告标签",
                        fontSize = 12.sp,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagOptions.take(3).forEach { tag ->
                            val isSelected = selectedTag == tag
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else SongbookColors.SurfaceLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else SongbookColors.GhostBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedTag = tag
                                        if (tag == "置顶") isPinned = true
                                    }
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.White else SongbookColors.SoftCharcoal,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tagOptions.drop(3).forEach { tag ->
                            val isSelected = selectedTag == tag
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else SongbookColors.SurfaceLow,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else SongbookColors.GhostBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        selectedTag = tag
                                        if (tag == "置顶") isPinned = true
                                    }
                            ) {
                                Text(
                                    text = tag,
                                    color = if (isSelected) Color.White else SongbookColors.SoftCharcoal,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("公告正文内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
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
                        onConfirm(title.trim(), content.trim(), selectedTag, isPinned)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

