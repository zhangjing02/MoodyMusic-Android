package com.example.moodymusicforandroid.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.data.model.User
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.home.components.CommunitySocialSection
import com.example.moodymusicforandroid.ui.home.components.FavoriteAlbumsSection
import com.example.moodymusicforandroid.ui.home.components.FavoriteSongsSection
import com.example.moodymusicforandroid.ui.home.components.FollowedArtistsSection
import com.example.moodymusicforandroid.ui.home.viewmodel.LibraryViewModel
import com.example.moodymusicforandroid.ui.music.viewmodel.AlbumSocialViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 音信页面主屏幕 (LibraryScreen)
 * 显示用户真实收藏/关注内容，游客模式显示推荐占位。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = viewModel(),
    socialViewModel: AlbumSocialViewModel = viewModel(viewModelStoreOwner = LocalContext.current as ComponentActivity),
    onSongClick: (FavoriteSong) -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (String, String) -> Unit = { _, _ -> },
    onOpenCollectionManager: (initialTab: Int) -> Unit = {},
    onAuthClick: () -> Unit = {}
) {
    val isUserLoggedIn by UserManager.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.observeAsState()
    val userLibrary by viewModel.userLibrary.observeAsState()
    val favoriteSongs by viewModel.favoriteSongs.observeAsState(emptyList())
    val socialContent by socialViewModel.socialContent.observeAsState()
    val errorMessage by socialViewModel.errorMessage.observeAsState()
    val isRefreshingSocial by socialViewModel.isRefreshing.collectAsState()
    val isRefreshingLib by viewModel.isRefreshing.observeAsState(false)
    val isRefreshing = isRefreshingSocial || isRefreshingLib
    val pullToRefreshState = rememberPullToRefreshState()
    var commentText by remember { mutableStateOf("") }

    // 进入页面时加载数据
    LaunchedEffect(Unit) {
        socialViewModel.fetchSocialContent("night_peace")
        viewModel.loadData()
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    SongbookPullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = {
            socialViewModel.fetchSocialContent("night_peace")
            viewModel.loadData()
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
                top = statusBarTop + 6.dp,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户资料卡片（已登录显示资料名片，未登录显示访客名片）
            item {
                if (isUserLoggedIn && userProfile != null) {
                    val localSongsCount = UserManager.favoriteSongsList.collectAsState().value.size
                    val safeUser = if (userProfile!!.favoriteSongsCount < localSongsCount) {
                        userProfile!!.copy(favoriteSongsCount = localSongsCount)
                    } else {
                        userProfile!!
                    }
                    UserProfileHeaderCard(user = safeUser)
                } else {
                    GuestProfileHeaderCard(onAuthClick = onAuthClick)
                }
            }

            // 收藏歌曲区
            item {
                val localSongs by UserManager.favoriteSongsList.collectAsState()
                val effectiveSongs = if (!isUserLoggedIn) {
                    emptyList()
                } else if (!favoriteSongs.isNullOrEmpty()) {
                    favoriteSongs!!
                } else {
                    localSongs
                }
                val effectiveCount = if (!isUserLoggedIn) 0 else maxOf(userProfile?.favoriteSongsCount ?: 0, effectiveSongs.size)
                FavoriteSongsSection(
                    songs = effectiveSongs,
                    songCount = effectiveCount,
                    onSongClick = onSongClick,
                    onViewAllClick = { onOpenCollectionManager(0) }
                )
            }

            // 收藏专辑区
            item {
                val effectiveAlbums = if (isUserLoggedIn) userLibrary?.favoriteAlbums ?: emptyList() else emptyList()
                FavoriteAlbumsSection(
                    albums = effectiveAlbums,
                    onAlbumClick = onAlbumClick,
                    onViewAllClick = { onOpenCollectionManager(1) }
                )
            }

            // 关注歌手区
            item {
                val effectiveArtists = if (isUserLoggedIn) userLibrary?.followedArtists ?: emptyList() else emptyList()
                Spacer(modifier = Modifier.height(8.dp))
                FollowedArtistsSection(
                    artists = effectiveArtists,
                    onArtistClick = onArtistClick,
                    onBrowseAllClick = { onOpenCollectionManager(2) }
                )
            }

            // 社区动态
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CommunitySocialSection(
                    content = socialContent,
                    errorMessage = errorMessage?.toString(),
                    commentText = commentText,
                    onCommentTextChange = { commentText = it },
                    onRetryClick = { socialViewModel.fetchSocialContent("night_peace") },
                    onSendClick = {
                        if (commentText.isNotBlank()) {
                            socialContent?.id?.let {
                                socialViewModel.postReply(it, "night_peace", commentText)
                            }
                            commentText = ""
                        }
                    }
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
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
        border = BorderStroke(1.dp, SongbookColors.GhostBorder)
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
                        .background(SongbookColors.SurfaceHigh),
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
                            color = SongbookColors.BurntOrange
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
                        color = SongbookColors.Outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = SongbookColors.GhostBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据（实时从服务器拉取的数字）
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
                        .background(SongbookColors.GhostBorder)
                )
                UserStatItem(count = user.favoriteAlbumsCount, label = "收藏专辑")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(SongbookColors.GhostBorder)
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
            color = SongbookColors.Outline
        )
    }
}

/**
 * 访客未登录状态名片组件（音信页）
 */
@Composable
private fun GuestProfileHeaderCard(
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
        border = BorderStroke(1.dp, SongbookColors.GhostBorder)
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
                        .background(SongbookColors.GhostBorderActive),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "访客",
                        tint = SongbookColors.BurntOrange,
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
                        color = SongbookColors.Outline,
                        fontSize = 11.5.sp
                    )
                }

                Button(
                    onClick = onAuthClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SongbookColors.BurntOrange,
                        contentColor = androidx.compose.ui.graphics.Color.White
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
            HorizontalDivider(color = SongbookColors.GhostBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // 统计数据（访客状态展示为 0）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserStatItem(count = 0, label = "收藏单曲")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(SongbookColors.GhostBorder)
                )
                UserStatItem(count = 0, label = "收藏专辑")
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(SongbookColors.GhostBorder)
                )
                UserStatItem(count = 0, label = "关注歌手")
            }
        }
    }
}

