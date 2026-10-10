package com.example.moodymusicforandroid.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.moodymusicforandroid.ui.components.SongbookPullToRefreshLayout
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.data.local.db.PlaylistEntity
import com.example.moodymusicforandroid.data.manager.PlaylistManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.data.model.User
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.home.components.FavoriteAlbumsSection
import com.example.moodymusicforandroid.ui.home.components.FavoriteSongsSection
import com.example.moodymusicforandroid.ui.home.components.FollowedArtistsSection
import com.example.moodymusicforandroid.ui.home.components.PlaylistsSection
import com.example.moodymusicforandroid.ui.home.viewmodel.LibraryViewModel
import com.example.moodymusicforandroid.ui.home.voice.TopBarTitleState
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 音信页面主屏幕 (LibraryScreen)
 * 显示用户真实收藏/关注内容，游客模式显示推荐占位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onSongClick: (FavoriteSong) -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onPlaylistClick: (Long, String) -> Unit = { _, _ -> },
    onPlayPlaylistClick: (PlaylistEntity) -> Unit = {},
    onOpenCollectionManager: (initialTab: Int) -> Unit = {},
    onAuthClick: () -> Unit = {}
) {
    val isUserLoggedIn by UserManager.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.observeAsState()
    val userLibrary by viewModel.userLibrary.observeAsState()
    val favoriteSongs by viewModel.favoriteSongs.observeAsState(emptyList())
    val playlists by PlaylistManager.playlists.collectAsState()
    val isRefreshing by viewModel.isRefreshing.observeAsState(false)
    val pullToRefreshState = rememberPullToRefreshState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    // 统一订阅 Room 本地收藏 StateFlow（未登录和登录态单一事实源）
    val roomSongs by UserManager.favoriteSongsList.collectAsState()
    val roomAlbums by UserManager.favoriteAlbumsList.collectAsState()
    val roomArtists by UserManager.followedArtistsList.collectAsState()

    // 监听语音状态，用于在音信页顶部展示浮动标题条
    val topBarTitleState by playerViewModel.topBarTitleState.collectAsState()
    val isVoiceTitleVisible = topBarTitleState != TopBarTitleState.Default

    // 进入页面时加载数据
    LaunchedEffect(Unit) {
        viewModel.loadData()
        PlaylistManager.refreshPlaylists()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 语音浮动标题条高度：44.dp（与首页/发现页 TopBar 内容高度对齐）
    val voiceTitleBarHeight = 44.dp
    // 有语音标题时，LazyColumn 顶部 padding 多出一个标题条高度；用动画平滑过渡，避免内容跳位
    val listTopPaddingExtra by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isVoiceTitleVisible) voiceTitleBarHeight else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "library_list_top_padding_anim"
    )
    val listTopPadding = statusBarTop + listTopPaddingExtra + 6.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SongbookPullToRefreshLayout(
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.loadData()
                PlaylistManager.refreshPlaylists()
            },
            state = pullToRefreshState,
            headerTopPadding = statusBarTop,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(
                    top = listTopPadding,
                    bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户资料卡片（已登录显示资料名片，未登录显示访客名片）
                item {
                    if (isUserLoggedIn && userProfile != null) {
                        val localSongsCount = roomSongs.size
                        val safeUser = if (userProfile!!.favoriteSongsCount < localSongsCount) {
                            userProfile!!.copy(favoriteSongsCount = localSongsCount)
                        } else {
                            userProfile!!
                        }
                        UserProfileHeaderCard(user = safeUser)
                    } else {
                        GuestProfileHeaderCard(
                            guestSongsCount = roomSongs.size,
                            guestAlbumsCount = roomAlbums.size,
                            guestArtistsCount = roomArtists.size,
                            onAuthClick = onAuthClick
                        )
                    }
                }

                // 我的私藏磁带区 (自建播放列表)
                item {
                    PlaylistsSection(
                        playlists = playlists,
                        onPlaylistClick = onPlaylistClick,
                        onPlayPlaylistClick = onPlayPlaylistClick,
                        onCreateNewClick = { showCreatePlaylistDialog = true }
                    )
                }

                // 收藏歌曲区
                item {
                    val effectiveSongs = if (isUserLoggedIn && !favoriteSongs.isNullOrEmpty()) {
                        favoriteSongs!!
                    } else {
                        roomSongs
                    }
                    val effectiveCount = maxOf(userProfile?.favoriteSongsCount ?: 0, effectiveSongs.size)
                    FavoriteSongsSection(
                        songs = effectiveSongs,
                        songCount = effectiveCount,
                        onSongClick = onSongClick,
                        onViewAllClick = { onOpenCollectionManager(0) }
                    )
                }

                // 收藏专辑区
                item {
                    val effectiveAlbums = if (isUserLoggedIn) {
                        userLibrary?.favoriteAlbums?.takeIf { it.isNotEmpty() } ?: roomAlbums
                    } else {
                        roomAlbums
                    }
                    FavoriteAlbumsSection(
                        albums = effectiveAlbums,
                        onAlbumClick = onAlbumClick,
                        onViewAllClick = { onOpenCollectionManager(1) }
                    )
                }

                // 关注歌手区
                item {
                    val effectiveArtists = if (isUserLoggedIn) {
                        userLibrary?.followedArtists?.takeIf { it.isNotEmpty() } ?: roomArtists
                    } else {
                        roomArtists
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FollowedArtistsSection(
                        artists = effectiveArtists,
                        onArtistClick = onArtistClick,
                        onBrowseAllClick = { onOpenCollectionManager(2) }
                    )
                }
            }

            if (showCreatePlaylistDialog) {
                CreatePlaylistQuickDialog(
                    onDismiss = { showCreatePlaylistDialog = false },
                    onConfirm = { name, color ->
                        PlaylistManager.createPlaylist(name = name, themeColor = color)
                        showCreatePlaylistDialog = false
                    }
                )
            }
        }

        // ── 语音状态浮动标题条（非 Default 时从顶部滑入，恢复后顺滑收起）──
        // 音信页本身没有固定 TopBar，因此用 AnimatedVisibility + AnimatedContent 组合：
        // 非 Default 状态 → 标题条出现在状态栏正下方；回到 Default → 顺滑上移收起。
        AnimatedVisibility(
            visible = isVoiceTitleVisible,
            enter = fadeIn(tween(220)) + expandVertically(tween(280), expandFrom = Alignment.Top),
            exit = fadeOut(tween(200)) + shrinkVertically(tween(260), shrinkTowards = Alignment.Top),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                    .padding(top = statusBarTop)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(voiceTitleBarHeight)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = topBarTitleState,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 })
                                .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 })
                        },
                        label = "library_voice_title_anim"
                    ) { state ->
                        when (state) {
                            TopBarTitleState.Default -> {
                                // 不会真正渲染（AnimatedVisibility 外层已隐藏），此处占位
                                Spacer(modifier = Modifier.height(1.dp))
                            }
                            TopBarTitleState.Listening -> {
                                Text(
                                    text = "🎙️ 正在倾听...",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            is TopBarTitleState.Searching -> {
                                val displayText = if (state.queryText.isNullOrBlank()) {
                                    "⏳ 正在识别您的语音..."
                                } else {
                                    "⏳ 寻找「${state.queryText}」..."
                                }
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 400
                                    )
                                )
                            }
                            is TopBarTitleState.Success -> {
                                Text(
                                    text = "🎵 ${state.message}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 400
                                    )
                                )
                            }
                            is TopBarTitleState.Error -> {
                                Text(
                                    text = "🔍 ${state.message}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB53B2A),
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 400
                                    )
                                )
                            }
                        }
                    }
                }
                // 底部细分割线
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

/**
 * 用户个人资料名片组件
 */
@Composable
private fun UserProfileHeaderCard(
    user: User,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户头像
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.avatarUrl.isNullOrBlank()) {
                        SongbookImage(
                            model = user.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = user.getDisplayName().take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = user.bio ?: "在音信里听风的声音",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据（恢复3个统计项）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserStatItem(count = user.favoriteSongsCount, label = "收藏单曲")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )
                UserStatItem(count = user.favoriteAlbumsCount, label = "收藏专辑")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )
                UserStatItem(count = user.followedArtistsCount, label = "关注歌手")
            }
        }
    }
}

@Composable
private fun UserStatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 访客未登录状态名片组件（音信页）
 */
@Composable
private fun GuestProfileHeaderCard(
    onAuthClick: () -> Unit,
    guestSongsCount: Int = 0,
    guestAlbumsCount: Int = 0,
    guestArtistsCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 访客头像
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "访客",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "访客未登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "登录同步收藏、关注与原声手札",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }

                Button(
                    onClick = onAuthClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "登录 / 注册",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据（访客状态展示3项，真实绑定本地 Room 数据）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserStatItem(count = guestSongsCount, label = "收藏单曲")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )
                UserStatItem(count = guestAlbumsCount, label = "收藏专辑")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                )
                UserStatItem(count = guestArtistsCount, label = "关注歌手")
            }
        }
    }
}

/**
 * 快速创建磁带弹窗
 */
@Composable
private fun CreatePlaylistQuickDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("DEFAULT") }

    val colors = listOf(
        "DEFAULT" to androidx.compose.ui.graphics.Color(0xFF8D6E63),
        "SUNSET_ORANGE" to androidx.compose.ui.graphics.Color(0xFFE65100),
        "FOREST_GREEN" to androidx.compose.ui.graphics.Color(0xFF2E7D32),
        "MIDNIGHT_BLUE" to androidx.compose.ui.graphics.Color(0xFF1565C0),
        "DEEP_DARK" to androidx.compose.ui.graphics.Color(0xFF212121),
        "CHERRY_VINTAGE" to androidx.compose.ui.graphics.Color(0xFFAD1457)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "新建手札",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("例如：交响乐、民谣、说唱...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "手札主题色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { (code, col) ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectedColor == code) 2.5.dp else 1.dp,
                                    color = if (selectedColor == code) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = code }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedColor)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("完成", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp)
    )
}

