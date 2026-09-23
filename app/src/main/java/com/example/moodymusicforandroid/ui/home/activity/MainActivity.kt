package com.example.moodymusicforandroid.ui.home.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.moodymusicforandroid.MoodyMusicApplication
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.common.utils.ActivityTransitionUtils
import com.example.moodymusicforandroid.common.utils.AppFlags
import com.example.moodymusicforandroid.common.utils.FontManager
import com.example.moodymusicforandroid.common.utils.ThemeManager
import com.example.moodymusicforandroid.ui.album.AlbumDetailScreen
import com.example.moodymusicforandroid.ui.artist.ArtistDetailScreen
import com.example.moodymusicforandroid.ui.auth.activity.LoginActivity
import com.example.moodymusicforandroid.ui.collection.CollectionManagerScreen
import com.example.moodymusicforandroid.ui.home.DiscoverScreen
import com.example.moodymusicforandroid.ui.home.HomeScreen
import com.example.moodymusicforandroid.ui.home.LibraryScreen
import com.example.moodymusicforandroid.ui.theme_detail.ThemeDetailScreen
import com.example.moodymusicforandroid.ui.components.SongbookBlurContainer
import com.example.moodymusicforandroid.ui.home.components.AppDrawerContent
import com.example.moodymusicforandroid.ui.home.components.FloatingMiniPlayer
import com.example.moodymusicforandroid.ui.home.components.FloatingMiniPlayerContent
import com.example.moodymusicforandroid.ui.home.components.MainBottomBar
import com.example.moodymusicforandroid.ui.home.components.MainBottomBarContent
import com.example.moodymusicforandroid.ui.home.viewmodel.MainViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.moodymusicforandroid.ui.navigation.*
import com.example.moodymusicforandroid.ui.player.MusicPlayerService
import com.example.moodymusicforandroid.ui.settings.SettingsScreen
import com.example.moodymusicforandroid.common.utils.DeviceInfoUtils
import com.example.moodymusicforandroid.common.update.PgyerUpdateManager
import com.example.moodymusicforandroid.data.model.AppVersionData
import com.example.moodymusicforandroid.ui.version.AppUpdateDialog
import com.example.moodymusicforandroid.ui.version.VersionUpdateScreen
import com.example.moodymusicforandroid.ui.player.NowPlayingScreen
import com.example.moodymusicforandroid.ui.player.PlayQueueItem
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookTheme
import com.example.moodymusicforandroid.ui.community.CommunityViewModel
import com.example.moodymusicforandroid.ui.community.MessageBoardScreen
import com.example.moodymusicforandroid.ui.community.PostDetailScreen
import com.example.moodymusicforandroid.ui.notice.NoticeBoardScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d(TAG, "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied, media notification may not be visible")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MoodyMusicApplication.currentThemeResId)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        EventBusManager.register(this)
        ThemeManager.initTheme(this)

        setContent {
            val owner = this@MainActivity as androidx.navigationevent.NavigationEventDispatcherOwner
            CompositionLocalProvider(
                androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner provides owner
            ) {
                SongbookTheme {
                    MainScreen(
                        onAuthClick = {
                            startActivity(Intent(this, LoginActivity::class.java))
                            ActivityTransitionUtils.overrideOpenTransition(this)
                        },
                        onThemeClick = { mode ->
                            UserManager.updateThemeMode(mode.value, this)
                            (application as MoodyMusicApplication).updateTheme()
                            Toast.makeText(this, "已切换主题", Toast.LENGTH_SHORT).show()
                            recreate()
                        },
                        onFontClick = { style ->
                            FontManager.setFontStyle(this, style)
                            Toast.makeText(this, "已切换字体", Toast.LENGTH_SHORT).show()
                            recreate()
                        },
                        onLogoutClick = {
                            UserManager.onLogout()
                            Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventReceived(event: BaseEvent) {
        if (event.eventType == EventType.AUTH_TOKEN_EXPIRED) {
            val isKickedOut = event.eventData == "KICKED_OUT"
            val message = if (isKickedOut) {
                "您的账号已在其他设备登录，当前已退出登录"
            } else {
                "登录状态已失效，当前已转为未登录模式"
            }
            com.example.moodymusicforandroid.common.utils.ToastUtils.showShort(this, message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBusManager.unregister(this)
        Log.d(TAG, "MainActivity destroyed")
    }
}

@Composable
fun MainScreen(
    onAuthClick: () -> Unit,
    onThemeClick: (ThemeManager.ThemeMode) -> Unit,
    onFontClick: (FontManager.FontStyle) -> Unit,
    onLogoutClick: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val userProfile by UserManager.userProfile.collectAsState()
    val isLoggedIn by UserManager.isLoggedIn.collectAsState()
    val userName = userProfile?.getDisplayName() ?: PreferencesManager.getUserName() ?: "同学"

    // 全局 PlayerViewModel
    val playerViewModel: PlayerViewModel = viewModel()
    val playState by playerViewModel.playState.collectAsState()
    val communityViewModel: CommunityViewModel = viewModel()

    val navigationState = rememberNavigationState(
        startRoute = RouteHome,
        topLevelRoutes = setOf(RouteHome, RouteDiscover, RouteLibrary)
    )
    val navigator = remember { Navigator(navigationState) }

    val currentVersionName = remember { DeviceInfoUtils.getVersionName(context) }
    val updateVersionData by PgyerUpdateManager.versionState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    // 监听全局版本数据变化，仅当强制更新时才弹窗
    LaunchedEffect(updateVersionData) {
        val vData = updateVersionData
        if (vData != null && vData.hasUpdate && vData.isForceUpdate) {
            showUpdateDialog = true
        }
    }

    // 1. App 启动时异步静默检测蒲公英是否有更新版本
    LaunchedEffect(Unit) {
        PgyerUpdateManager.triggerSilentCheck(context)
    }

    // 2. 双保险：用户每次滑开或点击展开左侧抽屉时，自动触发一次静默检测，确保红点 100% 实时
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            PgyerUpdateManager.triggerSilentCheck(context)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                AppDrawerContent(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    currentVersionName = currentVersionName,
                    hasUpdate = updateVersionData?.hasUpdate == true,
                    onCloseClick = { coroutineScope.launch { drawerState.close() } },
                    onAuthClick = {
                        coroutineScope.launch { drawerState.close() }
                        onAuthClick()
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        onLogoutClick()
                    },
                    onNoticeBoardClick = {
                        coroutineScope.launch { drawerState.close() }
                        navigator.navigate(RouteNoticeBoard)
                    },
                    onMessageBoardClick = {
                        coroutineScope.launch { drawerState.close() }
                        if (!isLoggedIn) {
                            android.widget.Toast.makeText(context, "留言板需登录后方可进入", android.widget.Toast.LENGTH_SHORT).show()
                            onAuthClick()
                        } else {
                            navigator.navigate(RouteMessageBoard)
                        }
                    },
                    onStylePreferenceClick = {
                        coroutineScope.launch { drawerState.close() }
                        android.widget.Toast.makeText(context, "风格喜好设置正在筹备中", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onSettingsClick = {
                        coroutineScope.launch { drawerState.close() }
                        navigator.navigate(RouteSettings)
                    },
                    onVersionClick = {
                        coroutineScope.launch { drawerState.close() }
                        navigator.navigate(RouteVersion)
                    },
                    onAboutClick = {
                        coroutineScope.launch { drawerState.close() }
                        android.widget.Toast.makeText(context, "音信 · TunePost v$currentVersionName\nThe Modern Songbook © 2026", android.widget.Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val entryProvider = remember(playState, updateVersionData) {
                entryProvider {
                    entry<RouteHome> {
                        HomeScreen(
                            playerViewModel = playerViewModel,
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onAvatarClick = onAuthClick,
                            onAlbumClick = { id, title ->
                                if (id == "butterfly_lovers_album") {
                                    val isBottomPlayerVisible = playState.songTitle.isNotBlank()
                                    if (!isBottomPlayerVisible && UserManager.isCardClickDirectPlay()) {
                                        playerViewModel.playSingleUrl(
                                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3",
                                            songTitle = "梁祝小提琴协奏曲",
                                            artistName = "何占豪 & 陈钢 (宋知垣 小提琴独奏)",
                                            albumTitle = "深度名作解析 · 东方交响",
                                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/albums/butterfly_lovers_cover_clean.jpg"
                                        )
                                    }
                                    navigator.navigate(
                                        RouteThemeDetail(
                                            themeId = "butterfly_lovers_deep_dive",
                                            title = "《梁祝》小提琴协奏曲",
                                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3",
                                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/albums/butterfly_lovers_cover_clean.jpg",
                                            artistName = "何占豪 & 陈钢 (宋知垣 小提琴独奏)"
                                        )
                                    )
                                } else {
                                    navigator.navigate(RouteAlbumDetail(id, title))
                                }
                            },
                            onArtistClick = { id, name, avatar ->
                                navigator.navigate(RouteArtistDetail(id, name, avatar))
                            },
                            onArticleClick = { articleId ->
                                if (articleId == "butterfly_lovers_deep_dive") {
                                    navigator.navigate(
                                        RouteThemeDetail(
                                            themeId = "butterfly_lovers_deep_dive",
                                            title = "《梁祝》小提琴协奏曲",
                                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3",
                                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/hero/butterfly_lovers_hero_clean.jpg",
                                            artistName = "何占豪 & 陈钢 (宋知垣 小提琴独奏)"
                                        )
                                    )
                                } else {
                                    navigator.navigate(RouteAlbumDetail("vinyl_soul", "回响：寻找消失的黑胶灵魂"))
                                }
                            },
                            onThemeClick = { themeId, title, audioUrl, coverUrl, artistName, storyUrl ->
                                // 当下面播放器显示时（无论是正在播放，还是暂停播放），进入卡片详情都不自动播放，把选择权交给用户，尽量不要打断用户的播放流畅性
                                val isBottomPlayerVisible = playState.songTitle.isNotBlank()
                                if (!isBottomPlayerVisible && UserManager.isCardClickDirectPlay() && audioUrl.isNotBlank()) {
                                    val cleanAlbumTitle = if (title.contains("—")) title.substringAfter("—").trim() else "今日胶片精选 · 慢调专栏"
                                    playerViewModel.playSingleUrl(
                                        audioUrl = audioUrl,
                                        songTitle = title.substringBefore("—").replace("《", "").replace("》", "").trim(),
                                        artistName = artistName,
                                        albumTitle = cleanAlbumTitle,
                                        coverUrl = coverUrl
                                    )
                                }
                                navigator.navigate(
                                    RouteThemeDetail(
                                        themeId = themeId,
                                        title = title,
                                        audioUrl = audioUrl,
                                        coverUrl = coverUrl,
                                        artistName = artistName,
                                        storyUrl = storyUrl
                                    )
                                )
                            }

                        )
                    }

                    entry<RouteDiscover> {
                        DiscoverScreen(
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onArtistClick = { id, name, avatar ->
                                navigator.navigate(RouteArtistDetail(id, name, avatar))
                            }
                        )
                    }

                    entry<RouteLibrary> {
                        LibraryScreen(
                            onSongClick = { song ->
                                val path = song.filePath
                                if (!path.isNullOrBlank()) {
                                    playerViewModel.playSingleUrl(
                                        audioUrl = path,
                                        songTitle = song.title,
                                        artistName = song.artistName ?: "未知歌手",
                                        albumTitle = song.albumTitle ?: "",
                                        coverUrl = song.coverUrl ?: ""
                                    )
                                }
                            },
                            onAlbumClick = { id, title ->
                                navigator.navigate(RouteAlbumDetail(id, title))
                            },
                            onArtistClick = { id, name, avatar ->
                                navigator.navigate(RouteArtistDetail(id, name, avatar))
                            },
                            onPlaylistClick = { playlistId, name ->
                                navigator.navigate(RoutePlaylistDetail(playlistId = playlistId, playlistName = name))
                            },
                            onPlayPlaylistClick = { playlist ->
                                coroutineScope.launch {
                                    val songs = com.example.moodymusicforandroid.data.manager.PlaylistManager.getSongs(playlist.id)
                                    if (songs.isNotEmpty()) {
                                        playerViewModel.playPlaylistSongs(songs, 0, playlist.name)
                                    }
                                }
                            },
                            onOpenCollectionManager = { initialTab ->
                                navigator.navigate(RouteCollectionManager(initialTab = initialTab))
                            },
                            onAuthClick = onAuthClick
                        )
                    }

                    entry<RoutePlaylistDetail> { key ->
                        com.example.moodymusicforandroid.ui.playlist.PlaylistDetailScreen(
                            playlistId = key.playlistId,
                            initialPlaylistName = key.playlistName,
                            playerViewModel = playerViewModel,
                            onBackClick = { navigator.goBack() },
                            onPlayAll = { songs, startIndex, isShuffle ->
                                if (songs.isNotEmpty()) {
                                    playerViewModel.playPlaylistSongs(songs, startIndex, key.playlistName)
                                    if (isShuffle && playerViewModel.playState.value.playMode != com.example.moodymusicforandroid.ui.player.PlayMode.SHUFFLE) {
                                        playerViewModel.togglePlayMode()
                                    }
                                }
                            }
                        )
                    }

                    entry<RouteCollectionManager> { key ->
                        CollectionManagerScreen(
                            initialTab = key.initialTab,
                            onBackClick = { navigator.goBack() },
                            onSongClick = { song ->
                                val path = song.filePath
                                if (!path.isNullOrBlank()) {
                                    playerViewModel.playSingleUrl(
                                        audioUrl = path,
                                        songTitle = song.title,
                                        artistName = song.artistName ?: "未知歌手",
                                        albumTitle = song.albumTitle ?: "",
                                        coverUrl = song.coverUrl ?: ""
                                    )
                                }
                            },
                            onAlbumClick = { id, title ->
                                navigator.navigate(RouteAlbumDetail(id, title))
                            },
                            onArtistClick = { id, name, avatar ->
                                navigator.navigate(RouteArtistDetail(id, name, avatar))
                            }
                        )
                    }

                    entry<RouteArtistDetail> { key ->
                        ArtistDetailScreen(
                            artistId = key.artistId,
                            artistName = key.artistName,
                            artistAvatar = key.avatarUrl,
                            onBackClick = { navigator.goBack() },
                            onAlbumClick = { artistId, albumTitle ->
                                navigator.navigate(
                                    RouteAlbumDetail(
                                        albumId = albumTitle,
                                        albumTitle = albumTitle,
                                        artistId = artistId,
                                        artistName = key.artistName
                                    )
                                )
                            },
                            onPlayAllClick = {
                                // 播放该艺人第一首歌（占位，由 ViewModel 实际处理）
                            }
                        )
                    }

                    entry<RouteAlbumDetail> { key ->
                        AlbumDetailScreen(
                            albumId = key.albumId,
                            albumTitle = key.albumTitle,
                            artistId = key.artistId,
                            artistName = key.artistName.ifBlank { key.albumTitle },
                            playerViewModel = playerViewModel,
                            playState = playState,
                            currentPlayingTitle = playState.songTitle,
                            isPlayingAudio = playState.isPlaying,
                            onBackClick = { navigator.goBack() },
                            onTrackClick = { songs, index, coverUrl ->
                                playerViewModel.play(
                                    songs = songs,
                                    index = index,
                                    artistName = key.artistName.ifBlank { key.albumTitle },
                                    albumTitle = key.albumTitle,
                                    coverUrl = coverUrl
                                )
                            },
                            onPlayAllClick = { songs, coverUrl ->
                                if (songs.isNotEmpty()) {
                                    playerViewModel.play(
                                        songs = songs,
                                        index = 0,
                                        artistName = key.artistName.ifBlank { key.albumTitle },
                                        albumTitle = key.albumTitle,
                                        coverUrl = coverUrl
                                    )
                                }
                            }
                        )
                    }

                    entry<RouteMusicDetail> { key ->
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎵 音乐详情\n\n当前歌曲 ID: ${key.songId}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    entry<RouteThemeDetail> { key ->
                        val isThisThemePlaying = playState.isPlaying && playState.audioUrl == key.audioUrl
                        val isThisThemeActive = playState.audioUrl == key.audioUrl && playState.songTitle.isNotBlank()
                        val isMiniPlayerVisible = playState.songTitle.isNotBlank()
                        ThemeDetailScreen(
                            themeId = key.themeId,
                            title = key.title,
                            audioUrl = key.audioUrl,
                            coverUrl = key.coverUrl,
                            artistName = key.artistName,
                            storyUrl = key.storyUrl,
                            isPlaying = isThisThemePlaying,
                            isThisThemeActive = isThisThemeActive,
                            isMiniPlayerVisible = isMiniPlayerVisible,
                            onBackClick = { navigator.goBack() },
                            onPlayToggle = {
                                if (isThisThemePlaying) {
                                    playerViewModel.togglePlayPause()
                                } else if (isThisThemeActive) {
                                    playerViewModel.togglePlayPause()
                                } else {
                                    val cleanAlbumTitle = if (key.title.contains("—")) key.title.substringAfter("—").trim() else "今日胶片精选 · 慢调专栏"
                                    playerViewModel.playSingleUrl(
                                        audioUrl = key.audioUrl,
                                        songTitle = key.title.substringBefore("—").replace("《", "").replace("》", "").trim(),
                                        artistName = key.artistName,
                                        albumTitle = cleanAlbumTitle,
                                        coverUrl = key.coverUrl
                                    )
                                }
                            }
                        )
                    }

                    entry<RouteVersion> {
                        VersionUpdateScreen(
                            onBackClick = { navigator.goBack() }
                        )
                    }

                    entry<RouteSettings> {
                        SettingsScreen(
                            onBackClick = { navigator.goBack() }
                        )
                    }

                    entry<RouteNoticeBoard> {
                        NoticeBoardScreen(
                            viewModel = communityViewModel,
                            onBackClick = { navigator.goBack() }
                        )
                    }

                    entry<RouteMessageBoard> {
                        MessageBoardScreen(
                            viewModel = communityViewModel,
                            onBackClick = { navigator.goBack() },
                            onPostClick = { postId ->
                                navigator.navigate(RoutePostDetail(postId))
                            },
                            onNavigateToAuth = onAuthClick
                        )
                    }

                    entry<RoutePostDetail> { key ->
                        PostDetailScreen(
                            postId = key.postId,
                            viewModel = communityViewModel,
                            onBackClick = { navigator.goBack() },
                            onNavigateToAuth = onAuthClick
                        )
                    }
                }
            }

            val isBottomBarVisible = remember { mutableStateOf(true) }
            val isTopLevel = navigationState.isTopLevel
            var isNowPlayingOpen by rememberSaveable { mutableStateOf(false) }

            // 切换页面时重置底栏显示状态
            LaunchedEffect(navigationState.topLevelRoute, isTopLevel) {
                isBottomBarVisible.value = true
            }

            // YouTube 风格：手指向下滑动浏览内容时收起底栏，向上回看时顺滑滑出
            val nestedScrollConnection = remember(isTopLevel) {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // 仅在一级页面（有底栏）时响应滑动隐现底栏
                        if (!isTopLevel) return Offset.Zero
                        val delta = available.y
                        if (delta < -15f) {
                            // 浏览下方内容 -> 隐藏底栏以释放最大全屏视界
                            isBottomBarVisible.value = false
                        } else if (delta > 15f) {
                            // 回看上方内容 -> 顺滑滑出底栏
                            isBottomBarVisible.value = true
                        }
                        return Offset.Zero
                    }
                }
            }

            val bottomBarOffset by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isBottomBarVisible.value) 0.dp else 140.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "BottomBarOffset"
            )

            // 悬浮播放器底部间距：
            // 一级页面且底栏显示时：浮于底栏上方 (62.dp + navigationBarsPadding)
            // 一级页面且底栏隐藏时：顺滑贴近底部 (10.dp + navigationBarsPadding)
            // 二级页面（无底栏）：常驻贴近底部 (10.dp + navigationBarsPadding)
            // 只要有音乐在播放，播放悬浮窗永远常驻屏幕内，绝不滑出隐藏！
            val miniPlayerBottomPadding by androidx.compose.animation.core.animateDpAsState(
                targetValue = if (isTopLevel && isBottomBarVisible.value) 62.dp else 10.dp,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "MiniPlayerBottomPadding"
            )

            val hazeState = remember { HazeState() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
            ) {
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = { navigator.goBack() },
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = hazeState)
                )

                // 1. 一级页面全宽毛玻璃贴底底栏（随列表滑动自适应隐现）
                if (isTopLevel) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .offset(y = bottomBarOffset)
                    ) {
                        MainBottomBar(
                            currentRoute = navigationState.topLevelRoute,
                            onNavigate = { route ->
                                navigationState.topLevelRoute = route as androidx.navigation3.runtime.NavKey
                                navigator.navigate(route as androidx.navigation3.runtime.NavKey)
                            },
                            hazeState = hazeState
                        )
                    }
                }

                // 2. 全局悬浮迷你播放器：只要有音乐播放，无论一级/二级页面均常驻显示，绝不随列表滑动隐藏
                if (playState.songTitle.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = miniPlayerBottomPadding)
                    ) {
                        FloatingMiniPlayer(
                            trackTitle = playState.songTitle,
                            artistName = playState.artistName,
                            coverUrl = playState.coverUrl,
                            isPlaying = playState.isPlaying,
                            position = playState.position,
                            duration = playState.duration,
                            hazeState = hazeState,
                            onPlayerClick = {
                                isNowPlayingOpen = true
                            },
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onPreviousClick = { playerViewModel.playPrevious() },
                            onNextClick = { playerViewModel.playNext() },
                            onSeekTo = { posMs -> playerViewModel.seekTo(posMs) }
                        )
                    }
                }

                // 3. 全屏沉浸式专属播放页面 (Apple Music 风格自底向上滑出)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isNowPlayingOpen && playState.songTitle.isNotBlank(),
                    enter = androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(250)),
                    exit = androidx.compose.animation.slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(200)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    NowPlayingScreen(
                        playState = playState,
                        onCollapse = { isNowPlayingOpen = false },
                        onClose = {
                            isNowPlayingOpen = false
                            playerViewModel.stop()
                        },
                        onPlayPauseToggle = { playerViewModel.togglePlayPause() },
                        onPrevious = { playerViewModel.playPrevious() },
                        onNext = { playerViewModel.playNext() },
                        onSeekTo = { posMs -> playerViewModel.seekTo(posMs) },
                        onTogglePlayMode = { playerViewModel.togglePlayMode() },
                        onSelectQueueItem = { index -> playerViewModel.playTrackInQueue(index) },
                        onRemoveQueueItem = { index -> playerViewModel.removeFromQueue(index) },
                        onClearQueue = { playerViewModel.clearQueue() },
                        onAddToCurrentQueue = {
                            playerViewModel.addToQueue(
                                audioUrl   = playState.audioUrl,
                                songTitle  = playState.songTitle,
                                artistName = playState.artistName,
                                albumTitle = playState.albumTitle,
                                coverUrl   = playState.coverUrl,
                                lrcPath    = playState.lrcPath
                            )
                        }
                    )
                }
            }
        }

        // 蒲公英新版本强制升级提醒小弹窗 (仅在属于强制更新时才弹出紧凑小矩形)
        if (showUpdateDialog && updateVersionData != null && updateVersionData!!.isForceUpdate) {
            AppUpdateDialog(
                versionData = updateVersionData!!,
                onConfirmUpdate = {
                    showUpdateDialog = false
                    navigator.navigate(RouteVersion)
                }
            )
        }
    }
}
