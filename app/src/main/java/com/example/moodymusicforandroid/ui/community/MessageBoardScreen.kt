package com.example.moodymusicforandroid.ui.community

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.moodymusicforandroid.data.model.CommunityPost
import com.example.moodymusicforandroid.data.model.ResourceAlignmentData
import com.example.moodymusicforandroid.data.model.ResourceHelper
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 社区多板块留言板与待办任务清单界面 (The Modern Songbook 现代颂歌风格)
 * 支持分类选择、资源补齐 5 维二级 Tab，以及页面内就地可编辑沉浸式工作流
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBoardScreen(
    viewModel: CommunityViewModel,
    onBackClick: () -> Unit,
    onPostClick: (Long) -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val posts by viewModel.posts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedResourceSubTab by viewModel.selectedResourceSubTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isLoggedIn by UserManager.isLoggedIn.collectAsState()
    val userProfile by UserManager.userProfile.collectAsState()
    val isMasterOrAdmin = userProfile?.isAdmin() == true || userProfile?.isMaster() == true

    // 就地页面内编辑态（彻底取代弹窗，提升操作整体性与沉浸感）
    var isEditingMode by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<CommunityPost?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        if (posts.isEmpty()) {
            viewModel.fetchPosts(selectedCategory)
        }
    }

    // 分类筛选选项定义（已移除“全部”选项，避免异构内容无序混排）
    val categories = remember {
        listOf(
            "resource" to "资源补齐 📌",
            "chat" to "吐槽闲聊 💬",
            "bug" to "Bug提示 🐛",
            "ui" to "页面优化 🎨",
            "feature" to "功能缺失 💡"
        )
    }



    // 浏览时只按大分类过滤，不再按二级 subTab 细分（用户可在编辑时指定子类型）
    val displayPosts = remember(posts, selectedCategory) {
        if (selectedCategory == "all" || selectedCategory.isBlank()) posts
        else posts.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditingMode) "WRITING MEMOIR" else "COMMUNITY & VOICES",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.BurntOrange,
                            letterSpacing = 2.sp,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isEditingMode) "书写信笺 · 读者回响" else "留言板 · 读者回响",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isEditingMode) {
                                isEditingMode = false
                            } else {
                                onBackClick()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditingMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditingMode) "取消编辑" else "返回",
                            tint = SongbookColors.SoftCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SongbookColors.PaperBackground
                )
            )
        },
        floatingActionButton = {
            // 处于编辑模式时隐藏悬浮按钮
            if (!isEditingMode) {
                FloatingActionButton(
                    onClick = {
                        if (!isLoggedIn) {
                            onNavigateToAuth()
                        } else {
                            isEditingMode = true
                        }
                    },
                    containerColor = SongbookColors.BurntOrange,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "发帖",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLoggedIn) "书写信笺" else "登录发帖",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SongbookColors.PaperBackground
    ) { innerPadding ->
        if (isEditingMode) {
            // ── 模式 A：页面内可编辑窗口 (In-Place Editor View) ───────────
            MessageBoardEditorView(
                initialCategory = selectedCategory,
                initialSubTab = selectedResourceSubTab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onDismiss = { isEditingMode = false },
                onSubmit = { cat, title, content ->
                    viewModel.createPost(cat, title, content) {
                        isEditingMode = false
                        viewModel.selectCategory(cat)
                    }
                }
            )
        } else {
            // ── 模式 B：标准内容展示页 ──────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // ── 1. 板块分类横滑筛选栏 (Pill Filters) ──────────────────
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { (catKey, catName) ->
                        val isSelected = selectedCategory == catKey
                        val bgCol by animateColorAsState(
                            targetValue = if (isSelected) SongbookColors.BurntOrange else SongbookColors.SurfaceLow,
                            animationSpec = tween(200),
                            label = "pill_bg"
                        )
                        val textCol = if (isSelected) Color.White else SongbookColors.SoftCharcoal.copy(alpha = 0.85f)

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = bgCol,
                            border = if (isSelected) null else BorderStroke(1.dp, SongbookColors.GhostBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { viewModel.selectCategory(catKey) }
                        ) {
                            Text(
                                text = catName,
                                color = textCol,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                // ── 2. 游客防护与帖子待办任务列表 ──────────────────────────
                if (!isLoggedIn) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                            border = BorderStroke(1.dp, SongbookColors.GhostBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = SongbookColors.BurntOrange,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "留言板需登录后方可进入",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    color = SongbookColors.SoftCharcoal
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "登录音信账号后，可查看全部板块讨论、提交功能与待办反馈，并与同频听众交流互动。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.65f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = onNavigateToAuth,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Text("立即登录 / 注册", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (isLoading && posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SongbookColors.BurntOrange,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.5.dp
                        )
                    }
                } else if (displayPosts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val catName = categories.firstOrNull { it.first == selectedCategory }?.second ?: "该板块"
                            val emptyNotice = "当前【${catName.replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z]"), "")}】暂无内容"
                            Text(
                                text = emptyNotice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "快来点击右下方书写信笺，成为第一个留下回响的人吧",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.BurntOrange.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 80.dp)
                    ) {
                        items(displayPosts, key = { it.id }) { post ->
                            val canDelete = isMasterOrAdmin || (isLoggedIn && post.userId == userProfile?.userId)
                            CommunityPostCard(
                                post = post,
                                isMasterOrAdmin = isMasterOrAdmin,
                                canDelete = canDelete,
                                onToggleStatus = { viewModel.toggleTaskStatus(post) },
                                onClick = { onPostClick(post.id) },
                                onDeleteClick = { postToDelete = post }
                            )
                        }
                    }
                }
            }
        }

        // 删除确认对话框
        if (postToDelete != null) {
            AlertDialog(
                onDismissRequest = { postToDelete = null },
                title = { Text("确认删除", fontWeight = FontWeight.Bold) },
                text = { Text("确定要彻底删除这条内容吗？关联的讨论回复也将一并清除。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            postToDelete?.let { viewModel.deletePost(it.id) {} }
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
    }
}

/**
 * 社区卡片组件（针对任务属性提供 Todo 待办事项格式与 Master 一键打钩）
 */
@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    isMasterOrAdmin: Boolean,
    canDelete: Boolean,
    onToggleStatus: () -> Unit,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isCompleted = post.isCompleted()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, SongbookColors.GhostBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted && post.isTask) SongbookColors.SurfaceHigh.copy(alpha = 0.5f) else SongbookColors.SurfaceLow
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── 头部：板块徽章 + 待办状态胶囊 + 删除垃圾桶 ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 板块小标签
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SongbookColors.BurntOrange.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = post.getCategoryDisplayName(),
                            color = SongbookColors.BurntOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }

                    // 如果是任务属性板块，展示待办状态指示器胶囊
                    if (post.isTask) {
                        val statusBg = when (post.status) {
                            "completed" -> Color(0xFFE8F5E9) // 淡绿
                            "in_progress" -> Color(0xFFFFF3E0) // 淡琥珀
                            else -> Color(0xFFECEFF1) // 沉静浅灰
                        }
                        val statusTextCol = when (post.status) {
                            "completed" -> Color(0xFF2E7D32)
                            "in_progress" -> Color(0xFFEF6C00)
                            else -> Color(0xFF546E7A)
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg,
                            modifier = Modifier.clickable(enabled = isMasterOrAdmin, onClick = onToggleStatus)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (post.status == "completed") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已完成",
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

                // 右侧：删除操作（Master/管理员可删任意，作者可删自己的）
                if (canDelete) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 标题主体行（带任务 Todo Checkbox 打钩区域）─────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // 如果是任务板块，放置待办 Checkbox
                if (post.isTask) {
                    Box(
                        modifier = Modifier
                            .padding(end = 10.dp, top = 2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) Color(0xFF2E7D32) else Color.Transparent
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isCompleted) Color(0xFF2E7D32) else SongbookColors.SoftCharcoal.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable(enabled = isMasterOrAdmin, onClick = onToggleStatus),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "打钩已完成",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // 标题与正文（若为资源补齐则格式化展示对齐清单表格）
                Column(modifier = Modifier.weight(1f)) {
                    val resourceData = remember(post.content) {
                        if (post.category == "resource") ResourceHelper.decode(post.content) else null
                    }

                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted && post.isTask) SongbookColors.SoftCharcoal.copy(alpha = 0.45f) else SongbookColors.SoftCharcoal,
                        textDecoration = if (isCompleted && post.isTask) TextDecoration.LineThrough else TextDecoration.None,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (resourceData != null) {
                        ResourceAlignmentTable(
                            data = resourceData,
                            isCompleted = isCompleted,
                            isCompact = true
                        )
                    } else {
                        Text(
                            text = post.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCompleted && post.isTask) SongbookColors.SoftCharcoal.copy(alpha = 0.45f) else SongbookColors.SoftCharcoal.copy(alpha = 0.8f),
                            maxLines = 3,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 底部作者与评论讨论统计 ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${post.authorName} · ${post.createdAt?.take(16) ?: "近期"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                    fontSize = 11.5.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "讨论评论",
                        tint = SongbookColors.BurntOrange.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (post.commentCount > 0) "${post.commentCount} 条讨论" else "参与讨论",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.BurntOrange,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

/**
 * 页面内就地信笺书写工作流组件 (取代原弹窗模式，提供更具有整体性的人文纸张编辑体验)
 */
@Composable
private fun MessageBoardEditorView(
    initialCategory: String,
    initialSubTab: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onSubmit: (category: String, title: String, content: String) -> Unit
) {
    val selectedCat = if (initialCategory == "all") "resource" else initialCategory
    var selectedSub by remember { mutableStateOf(initialSubTab) }

    // 通用常规板块字段
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // 资源补齐 5 维结构化专属字段
    var resArtist by remember { mutableStateOf("") }
    var resAlbum by remember { mutableStateOf("") }
    var resSongs by remember { mutableStateOf("") }
    var resYear by remember { mutableStateOf("") }
    var resDesc by remember { mutableStateOf("") }
    var resSongName by remember { mutableStateOf("") }
    var resGenre by remember { mutableStateOf("") }
    var resConcertName by remember { mutableStateOf("") }
    var resQualityIssue by remember { mutableStateOf("") }
    var resQualitySpec by remember { mutableStateOf("") }
    var resLink by remember { mutableStateOf("") }

    val isResourceMode = selectedCat == "resource"

    // 校验
    val canSubmit = if (isResourceMode) {
        when (selectedSub) {
            "song" -> resArtist.isNotBlank() && resSongName.isNotBlank()
            "artist" -> resArtist.isNotBlank() && resSongs.isNotBlank()
            "concert" -> resArtist.isNotBlank() && resConcertName.isNotBlank()
            "hires" -> resArtist.isNotBlank() && (resSongName.isNotBlank() || resAlbum.isNotBlank()) && resQualityIssue.isNotBlank()
            else -> resArtist.isNotBlank() && resAlbum.isNotBlank() && resSongs.isNotBlank()
        }
    } else {
        title.isNotBlank() && content.isNotBlank()
    }

    val resourceSubTabs = listOf(
        "album" to ("专辑补齐" to "💿"),
        "song" to ("歌曲补齐" to "🎵"),
        "artist" to ("歌手补齐" to "🎤"),
        "concert" to ("演唱会补齐" to "🎸"),
        "hires" to ("音质升级" to "🎧")
    )

    // 当前分类显示名（用于提示卡片）
    val catDisplayName = when (selectedCat) {
        "resource" -> "资源补齐 📌"
        "chat" -> "吐槽闲聊 💬"
        "bug" -> "Bug提示 🐛"
        "ui" -> "页面优化 🎨"
        "feature" -> "功能缺失 💡"
        else -> "留言"
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. 顶部提示卡片（显示当前所在板块，信息清晰，不可更改） ──────────
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SongbookColors.BurntOrange.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, SongbookColors.BurntOrange.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isResourceMode) "📋" else "✍️",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isResourceMode) "资源补齐精准对齐清单" else "读者信笺书写室",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )
                    Text(
                        text = if (isResourceMode)
                            "请按照对应类目填写规范信息，开发者将据此进行曲库对齐、音源核验与线上点亮。"
                        else
                            "您的反馈、灵感与感悟将同步至全站留言板，支持所有同频听众讨论盖楼。",
                        style = MaterialTheme.typography.labelSmall,
                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                }
                // 当前板块标签（只读）
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SongbookColors.BurntOrange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = catDisplayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // ── 2. 若为资源补齐，展示 5 维二级 Tab 切换（编辑时唯一的分类维度） ────
        if (isResourceMode) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "补齐类目 / RESOURCE TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(resourceSubTabs) { (subKey, subInfo) ->
                        val (subLabel, subIcon) = subInfo
                        val isSubSel = selectedSub == subKey
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSubSel) SongbookColors.BurntOrange.copy(alpha = 0.15f) else SongbookColors.SurfaceLow,
                            border = BorderStroke(1.dp, if (isSubSel) SongbookColors.BurntOrange else SongbookColors.GhostBorder),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedSub = subKey }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = subIcon, fontSize = 13.sp, modifier = Modifier.padding(end = 4.dp))
                                Text(
                                    text = subLabel,
                                    color = if (isSubSel) SongbookColors.BurntOrange else SongbookColors.SoftCharcoal,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSubSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = SongbookColors.GhostBorder, thickness = 0.8.dp)

        // ── 4. 动态表单字段渲染 ──────────────────────────────────────
        if (isResourceMode) {
            when (selectedSub) {
                "song" -> {
                    // 单曲补齐表单
                    OutlinedTextField(
                        value = resSongName,
                        onValueChange = { resSongName = it },
                        label = { Text("歌曲名称 (Track Name) *必填") },
                        placeholder = { Text("例如：反方向的钟 / 遇见") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resArtist,
                        onValueChange = { resArtist = it },
                        label = { Text("演唱歌手 (Artist) *必填") },
                        placeholder = { Text("例如：周杰伦 / 孙燕姿") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resAlbum,
                        onValueChange = { resAlbum = it },
                        label = { Text("所属专辑 / EP [选填]") },
                        placeholder = { Text("例如：Jay / The Moment") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resYear,
                        onValueChange = { resYear = it },
                        label = { Text("发行年份 [选填]") },
                        placeholder = { Text("例如：2000") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resLink,
                        onValueChange = { resLink = it },
                        label = { Text("试听参考链接 [选填]") },
                        placeholder = { Text("例如：网易云/QQ音乐/B站视频链接") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("补充说明 [选填]") },
                        placeholder = { Text("例如：希望补充录音室原版音频，不要Live版...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                "artist" -> {
                    // 歌手补齐表单
                    OutlinedTextField(
                        value = resArtist,
                        onValueChange = { resArtist = it },
                        label = { Text("歌手姓名 / 乐队名称 (Artist/Band) *必填") },
                        placeholder = { Text("例如：草东没有派对 / 椎名林檎") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resGenre,
                        onValueChange = { resGenre = it },
                        label = { Text("音乐流派 / 风格 (Genre) [选填]") },
                        placeholder = { Text("例如：独立摇滚 / 流行 / 爵士民谣") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resSongs,
                        onValueChange = { resSongs = it },
                        label = { Text("代表作品 (Key Works) *必填") },
                        placeholder = { Text("例如：大风吹、山海、烂泥") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resAlbum,
                        onValueChange = { resAlbum = it },
                        label = { Text("代表专辑 (Key Album) [选填]") },
                        placeholder = { Text("例如：丑奴儿") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("生平简介 / 推荐理由 [选填]") },
                        placeholder = { Text("分享推荐该歌手的理由或希望全集收录的愿望...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                "concert" -> {
                    // 演唱会补齐表单
                    OutlinedTextField(
                        value = resConcertName,
                        onValueChange = { resConcertName = it },
                        label = { Text("巡演 / 演唱会名称 (Concert Title) *必填") },
                        placeholder = { Text("例如：地表最强世界巡回演唱会 / 五月天诺亚方舟") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resArtist,
                        onValueChange = { resArtist = it },
                        label = { Text("表演歌手 (Artist) *必填") },
                        placeholder = { Text("例如：周杰伦 / 五月天") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resYear,
                        onValueChange = { resYear = it },
                        label = { Text("举办年份 / 巡演场次 [选填]") },
                        placeholder = { Text("例如：2016 台北站") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resSongs,
                        onValueChange = { resSongs = it },
                        label = { Text("核心现场曲目 (Key Tracks) [选填]") },
                        placeholder = { Text("例如：晴天 Live、开不了口 Live...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("现场说明 / 音频版本说明 [选填]") },
                        placeholder = { Text("例如：希望收录蓝光原盘提取的高保真无损音源...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                "hires" -> {
                    // 音质升级表单
                    OutlinedTextField(
                        value = resSongName,
                        onValueChange = { resSongName = it },
                        label = { Text("目标曲目或专辑名称 (Title) *必填") },
                        placeholder = { Text("例如：晴天 / 范特西整张") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resArtist,
                        onValueChange = { resArtist = it },
                        label = { Text("所属歌手 (Artist) *必填") },
                        placeholder = { Text("例如：周杰伦") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resQualityIssue,
                        onValueChange = { resQualityIssue = it },
                        label = { Text("当前音质问题 (Issue) *必填") },
                        placeholder = { Text("例如：当前为128k低码率、有杂音底噪、音量忽大忽小...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    OutlinedTextField(
                        value = resQualitySpec,
                        onValueChange = { resQualitySpec = it },
                        label = { Text("期望升级规格 [选填，默认 FLAC]") },
                        placeholder = { Text("例如：FLAC 16bit / 24bit Hi-Res / 官方母带重制") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("补充说明 [选填]") },
                        placeholder = { Text("例如：已有高品质源文件可提供或建议参考特定发行版本...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                else -> {
                    // 默认：专辑补齐表单
                    OutlinedTextField(
                        value = resArtist,
                        onValueChange = { resArtist = it },
                        label = { Text("歌手 (Artist) *必填") },
                        placeholder = { Text("例如：周杰伦 / 孙燕姿") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resAlbum,
                        onValueChange = { resAlbum = it },
                        label = { Text("专辑名 (Album Name) *必填") },
                        placeholder = { Text("例如：叶惠美 / 风筝") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resSongs,
                        onValueChange = { resSongs = it },
                        label = { Text("代表歌曲 (Key Songs) *必填") },
                        placeholder = { Text("例如：晴天、以父之名、三年二班") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resYear,
                        onValueChange = { resYear = it },
                        label = { Text("发行年份 (Release Year) [选填]") },
                        placeholder = { Text("例如：2003") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = resDesc,
                        onValueChange = { resDesc = it },
                        label = { Text("专辑介绍 / 补齐说明 [选填]") },
                        placeholder = { Text("例如：希望能收录 320k 高品质音源或正版首发专辑完整音轨...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
        } else {
            // ── 其他常规板块表单 ──────────────────────────────────────
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("信笺主题 / 核心诉求 *必填") },
                placeholder = {
                    Text(
                        when (selectedCat) {
                            "chat" -> "分享此时此刻的听歌感受..."
                            "bug" -> "简述遇到的问题现象..."
                            "ui" -> "建议优化的页面或动效..."
                            "feature" -> "期待新增的功能特性..."
                            else -> "请输入主题..."
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("详细内容 *必填") },
                placeholder = {
                    Text(
                        when (selectedCat) {
                            "chat" -> "写下任何与音乐、心情、回忆相关的文字..."
                            "bug" -> "请说明复现步骤、手机型号以及具体表现..."
                            "ui" -> "详细描述理想中的视觉样式、字体、排版或微交互细节..."
                            "feature" -> "描述使用场景以及该功能将带来的便利..."
                            else -> "畅所欲言，我们认真倾听每一位读者的回响..."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                maxLines = 8
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── 5. 底部操作按钮 ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SongbookColors.GhostBorder),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Text("放弃并返回", color = SongbookColors.SoftCharcoal)
            }

            Button(
                onClick = {
                    if (isResourceMode) {
                        val finalTitle: String
                        val finalContent: String
                        when (selectedSub) {
                            "song" -> {
                                val sName = resSongName.trim().removePrefix("《").removeSuffix("》")
                                finalTitle = "【单曲补齐】${resArtist.trim()} - 《$sName》"
                                finalContent = ResourceHelper.encode(
                                    ResourceAlignmentData(
                                        subType = "song",
                                        artist = resArtist.trim(),
                                        songName = sName,
                                        album = resAlbum.trim().removePrefix("《").removeSuffix("》"),
                                        year = resYear.trim().takeIf { it.isNotBlank() },
                                        link = resLink.trim().takeIf { it.isNotBlank() },
                                        desc = resDesc.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                            "artist" -> {
                                finalTitle = "【歌手补齐】${resArtist.trim()}${if (resGenre.isNotBlank()) " (${resGenre.trim()})" else ""}"
                                finalContent = ResourceHelper.encode(
                                    ResourceAlignmentData(
                                        subType = "artist",
                                        artist = resArtist.trim(),
                                        genre = resGenre.trim().takeIf { it.isNotBlank() },
                                        songs = resSongs.trim(),
                                        album = resAlbum.trim().removePrefix("《").removeSuffix("》"),
                                        desc = resDesc.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                            "concert" -> {
                                finalTitle = "【演唱会补齐】${resArtist.trim()} - ${resConcertName.trim()}"
                                finalContent = ResourceHelper.encode(
                                    ResourceAlignmentData(
                                        subType = "concert",
                                        concertName = resConcertName.trim(),
                                        artist = resArtist.trim(),
                                        year = resYear.trim().takeIf { it.isNotBlank() },
                                        songs = resSongs.trim(),
                                        desc = resDesc.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                            "hires" -> {
                                val tAudio = resSongName.trim().ifBlank { resAlbum.trim() }
                                finalTitle = "【音质升级】${resArtist.trim()} - $tAudio"
                                finalContent = ResourceHelper.encode(
                                    ResourceAlignmentData(
                                        subType = "hires",
                                        songName = tAudio,
                                        artist = resArtist.trim(),
                                        qualityIssue = resQualityIssue.trim(),
                                        qualitySpec = resQualitySpec.trim().takeIf { it.isNotBlank() },
                                        desc = resDesc.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                            else -> {
                                val albumClean = resAlbum.trim().removePrefix("《").removeSuffix("》")
                                finalTitle = "【专辑补齐】${resArtist.trim()} - 《$albumClean》"
                                finalContent = ResourceHelper.encode(
                                    ResourceAlignmentData(
                                        subType = "album",
                                        artist = resArtist.trim(),
                                        album = albumClean,
                                        songs = resSongs.trim(),
                                        year = resYear.trim().takeIf { it.isNotBlank() },
                                        desc = resDesc.trim().takeIf { it.isNotBlank() }
                                    )
                                )
                            }
                        }
                        onSubmit(selectedCat, finalTitle, finalContent)
                    } else {
                        onSubmit(selectedCat, title.trim(), content.trim())
                    }
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange),
                modifier = Modifier
                    .weight(1.5f)
                    .height(44.dp)
            ) {
                Text(
                    text = if (isResourceMode) "完成并提交对齐表" else "完成并投递信笺",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
