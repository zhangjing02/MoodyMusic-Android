package com.example.moodymusicforandroid.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.*
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.home.components.*
import com.example.moodymusicforandroid.ui.home.viewmodel.HomeViewModel
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors

import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.moodymusicforandroid.ui.components.SongbookPullToRefreshLayout


/**
 * 现代颂歌 (The Modern Songbook) 首页
 *
 * 采用 Block-Based SDUI (切片化动态首页流) 解析与多布局渲染架构。
 * 支持 9 大多态切片结构：
 * 1. hero_banner -> HeroFeaturedCard (深度专题大卡)
 * 2. category_tabs -> CategoryTabsBlock (分类标签切换)
 * 3. section_title -> SectionTitleBlock (刊头与章节大标题)
 * 4. quick_actions -> QuickActionsBlock (随行速达功能入口)
 * 5. artist_grid -> ArtistGridBlock (艺术家双列网格)
 * 6. track_list -> TrackListBlock (单曲高质试听列表)
 * 7. image_feature -> ImageFeatureBlock (杂志摄影视觉大图)
 * 8. archive_card -> ArchiveCardBlock (时代留声机/典藏黑胶)
 * 9. essay_card -> EssaysSection (文艺选集随笔)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onMenuClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onArticleClick: (String) -> Unit = {},
    onThemeClick: (themeId: String, title: String, audioUrl: String, coverUrl: String, artistName: String, storyUrl: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val feedItems by viewModel.homeFeedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playState by playerViewModel.playState.collectAsState()

    // 首页暖渐变背景：从顶部暖砂米黄到下方纸白，彻底告别纯白
    val warmGradient = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFF5E8D8), // 顶部暖砂色（米黄+陶土调）
                0.25f to Color(0xFFFAF3EA), // 过渡温暖米白
                1.0f to Color(0xFFFBF9F5)  // 底部回归纸白背景色
            )
        )
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
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(
                top = statusBarTop + 6.dp,
                bottom = 140.dp
            )
        ) {
        // 1. 杂志固定刊头栏 (Society Weekly Top Bar)
        item(key = "society_weekly_top_bar") {
            SocietyWeeklyTopBar(
                onMenuClick = onMenuClick,
                onAvatarClick = onAvatarClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. 动态切片流 (Block-Based SDUI Items) - 由服务端接口返回的 type 驱动四大核心布局
        feedItems.forEach { block ->
            when (block.type) {
                HomeBlockType.TOP_RECOMMEND_BANNER -> {
                    item(key = block.id, contentType = block.type) {
                        val banner = block.parsedData as? TopRecommendBannerData ?: block.toTopRecommendBanner()
                        TopRecommendBannerBlock(
                            data = banner,
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
                                onAlbumClick(item.actionTarget, item.title)
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
                    // 具备向后兼容性，优雅忽略未知切片类型
                }
            }
        }
    }
}
}

/**
 * 杂志刊头 TopBar 组件
 */
@Composable
private fun SocietyWeeklyTopBar(
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧抽屉菜单按钮
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

        // 中间刊头大字 (Serif Italic)
        Text(
            text = "SOCIETY WEEKLY",
            style = MaterialTheme.typography.headlineMedium,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )

        // 右侧头像
        IconButton(
            onClick = onAvatarClick,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                SongbookImage(
                    model = "/storage/avatars/user_avatar_default.jpg",
                    contentDescription = "User Avatar",
                    fallbackRes = R.drawable.user_avatar_default,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

