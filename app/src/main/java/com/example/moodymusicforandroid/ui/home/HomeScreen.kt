package com.example.moodymusicforandroid.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.ArchiveCardData
import com.example.moodymusicforandroid.data.model.ArtistGridData
import com.example.moodymusicforandroid.data.model.CategoryTabsData
import com.example.moodymusicforandroid.data.model.DeepDiveFeatureData
import com.example.moodymusicforandroid.data.model.EssayCardData
import com.example.moodymusicforandroid.data.model.HeroBannerData
import com.example.moodymusicforandroid.data.model.HomeBlockType
import com.example.moodymusicforandroid.data.model.ImageFeatureData
import com.example.moodymusicforandroid.data.model.QuickActionsData
import com.example.moodymusicforandroid.data.model.SectionTitleData
import com.example.moodymusicforandroid.data.model.TodayRecommendScrollData
import com.example.moodymusicforandroid.data.model.TopRecommendBannerData
import com.example.moodymusicforandroid.data.model.TrackListData
import com.example.moodymusicforandroid.data.model.VarietyShowGridData
import com.example.moodymusicforandroid.data.model.toArchiveCard
import com.example.moodymusicforandroid.data.model.toArtistGrid
import com.example.moodymusicforandroid.data.model.toCategoryTabs
import com.example.moodymusicforandroid.data.model.toDeepDiveFeature
import com.example.moodymusicforandroid.data.model.toEssayCard
import com.example.moodymusicforandroid.data.model.toHeroBanner
import com.example.moodymusicforandroid.data.model.toImageFeature
import com.example.moodymusicforandroid.data.model.toQuickActions
import com.example.moodymusicforandroid.data.model.toSectionTitle
import com.example.moodymusicforandroid.data.model.toTodayRecommendScroll
import com.example.moodymusicforandroid.data.model.toTopRecommendBanner
import com.example.moodymusicforandroid.data.model.toTrackList
import com.example.moodymusicforandroid.data.model.toVarietyShowGrid
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.components.SongbookPullToRefreshLayout
import com.example.moodymusicforandroid.ui.home.components.ArchiveCardBlock
import com.example.moodymusicforandroid.ui.home.components.ArtistGridCard
import com.example.moodymusicforandroid.ui.home.components.CategoryTabsBlock
import com.example.moodymusicforandroid.ui.home.components.DeepDiveFeatureBlock
import com.example.moodymusicforandroid.ui.home.components.EssayItem
import com.example.moodymusicforandroid.ui.home.components.HeroFeaturedCard
import com.example.moodymusicforandroid.ui.home.components.ImageFeatureBlock
import com.example.moodymusicforandroid.ui.home.components.QuickActionsBlock
import com.example.moodymusicforandroid.ui.home.components.SectionTitleBlock
import com.example.moodymusicforandroid.ui.home.components.TodayRecommendScrollBlock
import com.example.moodymusicforandroid.ui.home.components.TopRecommendBannerBlock
import com.example.moodymusicforandroid.ui.home.components.TrackListItemCard
import com.example.moodymusicforandroid.ui.home.components.VarietyShowGridBlock
import com.example.moodymusicforandroid.ui.home.viewmodel.HomeViewModel
import com.example.moodymusicforandroid.ui.home.voice.TopBarTitleState
import com.example.moodymusicforandroid.ui.player.CapsuleListeningMode
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 现代颂歌 (The Modern Songbook) 首页
 *
 * 采用 Block-Based SDUI (切片化动态首页流) 解析与多布局渲染架构。
 * 顶部导航栏通过 LazyColumn.stickyHeader 实现原生吸顶，无割裂感。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onMenuClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onRoamingClick: () -> Unit = {},
    onPlayFavoriteSongs: () -> Unit = {},
    onPlayFavoriteAlbums: () -> Unit = {},
    onPlayFollowedArtists: () -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onArticleClick: (String) -> Unit = {},
    onThemeClick: (themeId: String, title: String, audioUrl: String, coverUrl: String, artistName: String, storyUrl: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val coroutineScope = rememberCoroutineScope()
    val topBarTitleState by playerViewModel.topBarTitleState.collectAsState()

    val feedItems by viewModel.homeFeedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playState by playerViewModel.playState.collectAsState()

    // 首页暖渐变背景
    val warmGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFF5E8D8),
                0.25f to Color(0xFFFAF3EA),
                1.0f to Color(0xFFFBF9F5)
            )
        )
    }

    // TopBar 规格：内容高度 44.dp，加上状态栏总高度
    val topBarContentHeight = 44.dp
    val topBarTotalHeight = statusBarTop + topBarContentHeight

    // 滚动驱动的 TopBar 背景 Alpha (0f -> 1f)：
    // 在顶部未滚动时：alpha = 0f，完全透明，文字图标直接浮在 warmGradient 上，100% 融合，零色差！
    // 向上滑动前 60dp 过程中：alpha 平滑过渡到 1f，变身为实色吸顶栏，完美遮挡滑过的卡片
    val topBarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 120f).coerceIn(0f, 1f)
            }
        }
    }

    SongbookPullToRefreshLayout(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.fetchHomeFeed() },
        state = pullToRefreshState,
        headerTopPadding = statusBarTop,
        modifier = modifier
            .fillMaxSize()
            .background(warmGradient)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 将音乐综艺板块 (VARIETY_SHOW_GRID) 沉底展示在首页最下方
            val orderedFeedItems = remember(feedItems) {
                val (variety, others) = feedItems.partition { it.type == HomeBlockType.VARIETY_SHOW_GRID }
                others + variety
            }

            // 1. 主列表：第一项自然排列在 TopBar 下方
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(
                    top = topBarTotalHeight + 8.dp,
                    bottom = 140.dp
                )
            ) {
                // 动态切片流 (Block-Based SDUI Items)
                orderedFeedItems.forEach { block ->
                    when (block.type) {
                        HomeBlockType.TOP_RECOMMEND_BANNER -> {
                            item(key = block.id, contentType = block.type) {
                                val banner = block.parsedData as? TopRecommendBannerData ?: block.toTopRecommendBanner()
                            val cleanBannerTitle = banner.title.substringBefore("—").replace("《", "").replace("》", "").trim()
                            val isBannerPlaying = playState.isPlaying && (
                                (playState.songTitle.isNotBlank() && (playState.songTitle.contains(cleanBannerTitle) || cleanBannerTitle.contains(playState.songTitle))) ||
                                (!banner.audioUrl.isNullOrBlank() && (
                                    banner.audioUrl.substringAfterLast('/').substringBefore('?').isNotBlank() &&
                                    banner.audioUrl.substringAfterLast('/').substringBefore('?') == playState.audioUrl.substringAfterLast('/').substringBefore('?')
                                ))
                            )
                            TopRecommendBannerBlock(
                                data = banner,
                                isPlaying = isBannerPlaying,
                                onClick = { item ->
                                    if (item.actionType == "theme" || item.actionTarget.contains("theme")) {
                                        val fullTitle = if (item.subtitle.isNullOrBlank()) item.title else "${item.title} — ${item.subtitle}"
                                        onThemeClick(
                                            item.id.ifBlank { item.actionTarget },
                                            fullTitle,
                                            item.audioUrl ?: "",
                                            item.coverUrl,
                                            item.artistName ?: "",
                                            item.storyUrl
                                        )
                                    } else {
                                        onAlbumClick(item.actionTarget, item.title)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.TODAY_RECOMMEND_SCROLL -> {
                        item(key = block.id, contentType = block.type) {
                            val scrollData = block.parsedData as? TodayRecommendScrollData ?: block.toTodayRecommendScroll()
                            TodayRecommendScrollBlock(
                                data = scrollData,
                                onItemClick = { item ->
                                    if (item.isTheme) {
                                        val themeId = item.themeId ?: item.id
                                        val fullTitle = if (item.subtitle.isNullOrBlank()) "《${item.title}》" else "《${item.title}》— ${item.subtitle}"
                                        onThemeClick(
                                            themeId,
                                            fullTitle,
                                            item.audioUrl ?: "",
                                            item.coverUrl,
                                            item.artist,
                                            item.storyUrl
                                        )
                                    } else {
                                        onAlbumClick(item.id, item.title)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.DEEP_DIVE_FEATURE -> {
                        item(key = block.id, contentType = block.type) {
                            val deepDive = block.parsedData as? DeepDiveFeatureData ?: block.toDeepDiveFeature()
                            val audioUrl = if (!deepDive.audioUrl.isNullOrBlank()) {
                                deepDive.audioUrl
                            } else {
                                "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3"
                            }
                            DeepDiveFeatureBlock(
                                data = deepDive,
                                onReadArticleClick = { onArticleClick(deepDive.articleId ?: deepDive.id) },
                                onPlayAlbumClick = {
                                    if (playState.audioUrl == audioUrl) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSingleUrl(
                                            audioUrl = audioUrl,
                                            songTitle = deepDive.albumTitle ?: "《梁祝》小提琴协奏曲",
                                            artistName = "何占豪 & 陈钢 (宋知垣 小提琴独奏)",
                                            albumTitle = "深度名作解析 · 东方交响",
                                            coverUrl = deepDive.coverUrl
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.VARIETY_SHOW_GRID -> {
                        item(key = block.id, contentType = block.type) {
                            val varietyData = block.parsedData as? VarietyShowGridData ?: block.toVarietyShowGrid()
                            VarietyShowGridBlock(
                                data = varietyData,
                                onItemClick = { item ->
                                    // 依据服务端 SDUI 动作类型与目标自然跳转，无需客户端硬编码判断
                                    if (item.actionType == "album") {
                                        onAlbumClick(item.actionTarget, item.title)
                                    } else {
                                        onArtistClick(item.actionTarget, item.title, item.coverUrl)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.HERO_BANNER -> {
                        item(key = block.id, contentType = block.type) {
                            val hero = block.parsedData as? HeroBannerData ?: block.toHeroBanner()
                            val audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3"
                            HeroFeaturedCard(
                                title = hero.title,
                                tag = hero.tag,
                                summary = hero.summary,
                                imageUrl = hero.imageUrl,
                                primaryActionText = hero.primaryActionText,
                                secondaryActionText = hero.secondaryActionText,
                                onReadArticleClick = { onArticleClick(hero.articleId) },
                                onPlayAlbumClick = {
                                    if (playState.audioUrl == audioUrl) {
                                        playerViewModel.togglePlayPause()
                                    } else {
                                        playerViewModel.playSingleUrl(
                                            audioUrl = audioUrl,
                                            songTitle = hero.albumTitle ?: "《梁祝》小提琴协奏曲",
                                            artistName = "何占豪 & 陈钢 (宋知垣 小提琴独奏)",
                                            albumTitle = "深度名作解析 · 东方交响",
                                            coverUrl = hero.imageUrl
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.CATEGORY_TABS -> {
                        item(key = block.id, contentType = block.type) {
                            val tabsData = block.parsedData as? CategoryTabsData ?: block.toCategoryTabs()
                            CategoryTabsBlock(
                                data = tabsData,
                                onTabSelect = { categoryId -> viewModel.selectCategory(categoryId) }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    HomeBlockType.SECTION_TITLE -> {
                        item(key = block.id, contentType = block.type) {
                            val titleData = block.parsedData as? SectionTitleData ?: block.toSectionTitle()
                            SectionTitleBlock(
                                data = titleData,
                                onActionClick = { route ->
                                    when (route) {
                                        "all_artists" -> onArtistClick("all", "精选艺术家", null)
                                        "play_all_tracks" -> onAlbumClick("daily_tracks", "今日单曲集")
                                        "archive_gallery" -> onAlbumClick("archive_gallery", "时代留声机")
                                        else -> onAlbumClick("society_weekly", "SOCIETY WEEKLY")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    HomeBlockType.ARTIST_GRID -> {
                        val artistData = block.parsedData as? ArtistGridData ?: block.toArtistGrid()
                        for (i in artistData.artists.indices step 2) {
                            val left = artistData.artists[i]
                            val right = artistData.artists.getOrNull(i + 1)
                            item(key = "artist_pair_${left.id}_${right?.id ?: ""}", contentType = "artist_row") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ArtistGridCard(
                                        artist = left,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onArtistClick(left.id, left.name, left.avatarUrl) }
                                    )
                                    if (right != null) {
                                        ArtistGridCard(
                                            artist = right,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onArtistClick(right.id, right.name, right.avatarUrl) }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                        item(key = "artist_bottom_space_${block.id}") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    HomeBlockType.TRACK_LIST -> {
                        val trackData = block.parsedData as? TrackListData ?: block.toTrackList()
                        items(
                            items = trackData.tracks,
                            key = { "track_${it.id}" },
                            contentType = { "track_item" }
                        ) { track ->
                            val index = trackData.tracks.indexOf(track) + 1
                            TrackListItemCard(
                                index = index,
                                track = track,
                                onClick = { onAlbumClick(track.id, track.title) },
                                onPlayToggle = { viewModel.toggleTrackPlay(track.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        item(key = "track_bottom_space_${block.id}") {
                            Spacer(modifier = Modifier.height(18.dp))
                        }
                    }

                    HomeBlockType.ARCHIVE_CARD -> {
                        item(key = block.id, contentType = block.type) {
                            val archiveData = block.parsedData as? ArchiveCardData ?: block.toArchiveCard()
                            ArchiveCardBlock(
                                data = archiveData,
                                onClick = { onAlbumClick(archiveData.id, archiveData.title) }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.IMAGE_FEATURE -> {
                        item(key = block.id, contentType = block.type) {
                            val imageData = block.parsedData as? ImageFeatureData ?: block.toImageFeature()
                            ImageFeatureBlock(
                                data = imageData,
                                onClick = { onArticleClick("visual_feature") }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.ESSAY_CARD -> {
                        val essayData = block.parsedData as? EssayCardData ?: block.toEssayCard()
                        item(key = "essay_header_${block.id}", contentType = "essay_header") {
                            Text(
                                text = essayData.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }
                        items(
                            items = essayData.essays,
                            key = { "essay_${it.id}" },
                            contentType = { "essay_item" }
                        ) { essay ->
                            EssayItem(
                                essay = essay,
                                onClick = { onArticleClick(essay.id) }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    HomeBlockType.QUICK_ACTIONS -> {
                        item(key = block.id, contentType = block.type) {
                            val actionsData = block.parsedData as? QuickActionsData ?: block.toQuickActions()
                            QuickActionsBlock(
                                data = actionsData,
                                onActionClick = { action ->
                                    when (action.id) {
                                        "vinyl_radio" -> onAlbumClick("vinyl_radio", "黑胶电台")
                                        "daily_radar" -> onAlbumClick("daily_radar", "每日随心听")
                                        "new_charts" -> onAlbumClick("new_charts", "新碟排行榜")
                                        else -> onAlbumClick(action.id, action.title)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    else -> {
                        // 向后兼容，优雅忽略未知切片
                    }
                }
            }
        }

            // 2. 吸顶 TopBar（位于列表之上，最高 Z-Order）
            // - 在最顶部未滚动时：topBarAlpha 为 0f，完全透明，透出底层的 warmGradient 暖砂色，0 色差！
            // - 向上滚动时：topBarAlpha 渐变到 1f，平滑变身为实体背景 + 细分割线，无缝吸顶；
            // - 下拉刷新时：随整个内容整体下移，完全不遮挡顶部的刷新文字与旋转指示器。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color(0xFFF5E8D8).copy(alpha = topBarAlpha))
                    .padding(top = statusBarTop)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    SocietyWeeklyTopBar(
                        capsuleListeningMode = playState.capsuleListeningMode,
                        titleState = topBarTitleState,
                        onMenuClick = onMenuClick,
                        onRoamingClick = onRoamingClick,
                        onPlayFavoriteSongs = onPlayFavoriteSongs,
                        onPlayFavoriteAlbums = onPlayFavoriteAlbums,
                        onPlayFollowedArtists = onPlayFollowedArtists
                    )
                }
                // 底部细分割线，同样跟随 topBarAlpha 淡入淡出
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = Color(0xFFE8D5BE).copy(alpha = topBarAlpha),
                    thickness = 0.5.dp
                )
            }
        }
    }
}


/**
 * 杂志刊头 TopBar 组件
 */
// ── 杂志复古纸质强调与状态配色 ───────────────────────────
private val MenuPaperBg = Color(0xFFFCFAF6)          // 典雅羊皮纸白（与暖砂背景天然融合）
private val MenuBorderColor = Color(0xFFE2D6C5)       // 书卷装订暖杏边线
private val IconIdleCharcoal = Color(0xFF5A524A)      // 杂志标题印刷柔和炭墨色（未选中/正常）
private val IconMutedGray = Color(0xFF9E958A)         // 典雅置灰石板铅印色（漫游未激活）
private val RoamActiveOrange = Color(0xFFC85208)      // 杂志经典火漆焦橙色（漫游激活）
private val RoamActivePillBg = Color(0xFFF7E6D7)      // 焦橙专属浅杏圆托底色
private val VoiceActiveHaloBg = Color(0xFFF7E6D7)     // 语音倾听暖杏底托

// 🌈 漫游模式下头像若隐若现低调珠光彩虹色盘（柔雾雅致、低饱和度、杂志纸风柔和配色）
private val RainbowHaloColors = listOf(
    Color(0xFFE57373), // 柔雾玫瑰粉
    Color(0xFFFFB74D), // 柔光浅琥珀
    Color(0xFFFFF176), // 香槟淡金
    Color(0xFF81C784), // 极光浅薄荷
    Color(0xFF4DD0E1), // 柔和冰川蓝
    Color(0xFF7986CB), // 暮色鸢尾紫
    Color(0xFFBA68C8), // 浅调丁香紫
    Color(0xFFE57373)  // 闭合首尾相接
)

@Composable
private fun SocietyWeeklyTopBar(
    capsuleListeningMode: CapsuleListeningMode = CapsuleListeningMode.NONE,
    titleState: TopBarTitleState = TopBarTitleState.Default,
    onMenuClick: () -> Unit,
    onRoamingClick: () -> Unit = {},
    onPlayFavoriteSongs: () -> Unit = {},
    onPlayFavoriteAlbums: () -> Unit = {},
    onPlayFollowedArtists: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by UserManager.userProfile.collectAsState()
    val isLoggedIn by UserManager.isLoggedIn.collectAsState()

    var showAvatarMenu by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    fun dismissMenu() {
        isMenuExpanded = false
        coroutineScope.launch {
            delay(120)
            showAvatarMenu = false
        }
    }

    val isRoamingActive = capsuleListeningMode == CapsuleListeningMode.ROAMING

    // 随机漫游模式激活下头像彩虹流光溢彩若隐若现动效 (Ethereal Iridescent Rainbow Sweep Halo - 低调纤细珠光版)
    val haloInfiniteTransition = rememberInfiniteTransition(label = "rainbow_halo_anim")
    // 1. 流光旋转动效：360° 无限平滑流转 (4.5s 缓慢优雅流转)
    val haloRotation by haloInfiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )
    // 2. 若隐若现呼吸感：透明度在 0.30f 与 0.70f 之间柔和微光呼吸 (2.2s 周期)
    val haloAlpha by haloInfiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )
    // 3. 溢彩漫反射呼吸微缩放 (1.0f ~ 1.025f 极细微自然呼吸)
    val haloGlowScale by haloInfiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_glow_scale"
    )
    // 4. 随机漫游模式激活开关平滑渐变（仅限 ROAMING 漫游模式生效）
    val haloVisibility by animateFloatAsState(
        targetValue = if (isRoamingActive) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "halo_visibility"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = SongbookColors.BurntOrange
            )
        }

        // 刊头居中动态标题（支持默认刊名、实时说话内容、搜索状态、播放跑马灯与未收录提示）
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = titleState,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { -it / 2 })
                        .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { it / 2 })
                },
                label = "topbar_title_anim"
            ) { state ->
                when (state) {
                    TopBarTitleState.Default -> {
                        Text(
                            text = "SOCIETY WEEKLY",
                            style = MaterialTheme.typography.headlineMedium,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                    }
                    TopBarTitleState.Listening -> {
                        Text(
                            text = "🎙️ 正在倾听...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC85208),
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
                            color = Color(0xFF5A524A),
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
                            color = Color(0xFFC85208),
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

        // 右侧胶囊菜单触发器（未激活特定模式或漫游模式时显示用户头像；激活其他听歌模式时显示对应模式的高亮图标）
        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            // 触发按钮（36dp 纯净触控区域，与左侧 Menu 按钮对称，无外围多余圆底背景）
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!showAvatarMenu) {
                            showAvatarMenu = true
                            isMenuExpanded = true
                        } else {
                            dismissMenu()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (capsuleListeningMode) {
                    CapsuleListeningMode.NONE,
                    CapsuleListeningMode.ROAMING -> {
                        // 1. 头像主体（32dp 精准居中，优先使用用户头像或项目默认头像 user_avatar_default）
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .then(
                                    if (haloVisibility <= 0.01f) {
                                        Modifier.border(
                                            width = 0.8.dp,
                                            color = MenuBorderColor.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                    } else Modifier
                                )
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            val avatarModel = if (isLoggedIn && !userProfile?.avatarUrl.isNullOrBlank()) {
                                userProfile?.avatarUrl
                            } else {
                                R.drawable.user_avatar_default
                            }
                            SongbookImage(
                                model = avatarModel,
                                contentDescription = "用户头像",
                                fallbackRes = R.drawable.user_avatar_default,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 2. 漫游模式专属彩虹流光环 (紧贴 32dp 头像外周边缘流转呼吸，仅在漫游激活时呈现)
                        if (haloVisibility > 0.01f) {
                            Canvas(
                                modifier = Modifier
                                    .size(32.dp)
                                    .graphicsLayer { rotationZ = haloRotation }
                            ) {
                                val canvasCenter = this.center
                                val radius = size.minDimension / 2f
                                val sweepBrush = Brush.sweepGradient(RainbowHaloColors, canvasCenter)

                                // 柔和微晕层 (Subtle Feather Glow - 2.0dp, 极低半透明微漫反射)
                                drawCircle(
                                    brush = sweepBrush,
                                    radius = radius * haloGlowScale,
                                    style = Stroke(width = 2.0.dp.toPx()),
                                    alpha = (0.25f + haloAlpha * 0.25f) * haloVisibility
                                )
                                // 实体高光层 (Crisp Core Arc - 1.2dp, 细腻珠宝细光)
                                drawCircle(
                                    brush = sweepBrush,
                                    radius = radius,
                                    style = Stroke(width = 1.2.dp.toPx()),
                                    alpha = (0.50f + haloAlpha * 0.35f) * haloVisibility
                                )
                            }
                        }
                    }

                    CapsuleListeningMode.FAVORITE_SONGS -> {
                        // 播放收藏歌曲：火漆焦橙色纯净图标直出，不加圆底背景
                        Icon(
                            painter = painterResource(R.drawable.ic_headset_fav_song),
                            contentDescription = "播放收藏歌曲",
                            tint = RoamActiveOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    CapsuleListeningMode.FAVORITE_ALBUMS -> {
                        // 播放收藏专辑：火漆焦橙色纯净图标直出，不加圆底背景
                        Icon(
                            painter = painterResource(R.drawable.ic_headset_fav_album),
                            contentDescription = "播放收藏专辑",
                            tint = RoamActiveOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    CapsuleListeningMode.FOLLOWED_ARTISTS -> {
                        // 播放关注歌手：火漆焦橙色纯净图标直出，不加圆底背景
                        Icon(
                            painter = painterResource(R.drawable.ic_headset_fav_artist),
                            contentDescription = "播放关注歌手",
                            tint = RoamActiveOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 折叠胶囊菜单弹出层
            if (showAvatarMenu) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(x = 0, y = with(LocalDensity.current) { 40.dp.roundToPx() }),
                    onDismissRequest = { dismissMenu() },
                    properties = PopupProperties(focusable = true)
                ) {
                    CapsuleActionMenu(
                        isExpanded = isMenuExpanded,
                        currentMode = capsuleListeningMode,
                        onRoamingClick = {
                            dismissMenu()
                            onRoamingClick()
                        },
                        onPlayFavoriteSongs = {
                            dismissMenu()
                            onPlayFavoriteSongs()
                        },
                        onPlayFavoriteAlbums = {
                            dismissMenu()
                            onPlayFavoriteAlbums()
                        },
                        onPlayFollowedArtists = {
                            dismissMenu()
                            onPlayFollowedArtists()
                        },
                        onDismissRequest = { dismissMenu() }
                    )
                }
            }
        }
    }
}

/**
 * 顶部折叠卡片展开胶囊组件 (Vertical Action Capsule - 杂志纸质卡片风)
 *
 * 四个听歌模式：
 *   1. 随机漫游 (ic_roam_dice)
 *   2. 播放收藏歌曲 (ic_headset_fav_song)
 *   3. 播放收藏专辑 (ic_headset_fav_album)
 *   4. 播放关注歌手 (ic_headset_fav_artist)
 *
 * 状态逻辑：
 *   - 未选中时：置灰显示 (IconMutedGray)，无底色无边框
 *   - 选中时：火漆焦橙高亮 (RoamActiveOrange) + 浅杏暖色底托 (RoamActivePillBg) + 边框 + 细腻微呼吸
 */
@Composable
private fun CapsuleActionMenu(
    isExpanded: Boolean,
    currentMode: CapsuleListeningMode,
    onRoamingClick: () -> Unit,
    onPlayFavoriteSongs: () -> Unit,
    onPlayFavoriteAlbums: () -> Unit,
    onPlayFollowedArtists: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    androidx.compose.animation.AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn(tween(160)) + expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(tween(120)) + shrinkVertically(
            animationSpec = tween(120),
            shrinkTowards = Alignment.Top
        )
    ) {
        Surface(
            modifier = Modifier
                .width(48.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color(0x334A2810),
                    ambientColor = Color(0x1F5C3318)
                ),
            shape = RoundedCornerShape(24.dp),
            color = MenuPaperBg.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MenuBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // ── 1. 随心漫游 ──
                CapsuleMenuItem(
                    iconRes = R.drawable.ic_roam_dice,
                    contentDescription = "随心漫游",
                    isSelected = (currentMode == CapsuleListeningMode.ROAMING),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRoamingClick()
                    }
                )

                // 纸质分割线
                Box(modifier = Modifier.width(20.dp).height(0.6.dp).background(Color(0xFFE8DFD3)))

                // ── 2. 播放收藏歌曲（一箭穿心 💘） ──
                CapsuleMenuItem(
                    iconRes = R.drawable.ic_headset_fav_song,
                    contentDescription = "播放收藏歌曲",
                    isSelected = (currentMode == CapsuleListeningMode.FAVORITE_SONGS),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayFavoriteSongs()
                    }
                )

                // 纸质分割线
                Box(modifier = Modifier.width(20.dp).height(0.6.dp).background(Color(0xFFE8DFD3)))

                // ── 3. 播放收藏专辑（黑胶唱片 + 播放三角 ▶） ──
                CapsuleMenuItem(
                    iconRes = R.drawable.ic_headset_fav_album,
                    contentDescription = "播放收藏专辑",
                    isSelected = (currentMode == CapsuleListeningMode.FAVORITE_ALBUMS),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayFavoriteAlbums()
                    }
                )

                // 纸质分割线
                Box(modifier = Modifier.width(20.dp).height(0.6.dp).background(Color(0xFFE8DFD3)))

                // ── 4. 播放关注歌手（复古爵士立麦 🎙️ + 星芒） ──
                CapsuleMenuItem(
                    iconRes = R.drawable.ic_headset_fav_artist,
                    contentDescription = "播放关注歌手",
                    isSelected = (currentMode == CapsuleListeningMode.FOLLOWED_ARTISTS),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayFollowedArtists()
                    }
                )
            }
        }
    }
}

/**
 * 胶囊菜单单项：
 * - 激活/选中状态：火漆焦橙色高亮 + 浅杏橙暖色圆底背景 + 细腻微呼吸
 * - 未激活/未选中状态：置灰石板铅印色 (IconMutedGray)，无底色无边框
 */
@Composable
private fun CapsuleMenuItem(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "capsule_item_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "item_pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier
                        .background(RoamActivePillBg)
                        .border(0.8.dp, Color(0xFFECCEB6), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = if (isSelected) RoamActiveOrange else IconMutedGray,
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isSelected) {
                        Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    } else Modifier
                )
        )
    }
}
