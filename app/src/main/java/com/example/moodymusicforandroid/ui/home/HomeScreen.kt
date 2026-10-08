package com.example.moodymusicforandroid.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.moodymusicforandroid.ui.player.PlayerViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import com.example.moodymusicforandroid.common.utils.AppThemeManager
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
    onVoiceSearchClick: () -> Unit = {},
    onRequestAudioPermission: () -> Unit = {},
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onArticleClick: (String) -> Unit = {},
    onThemeClick: (themeId: String, title: String, audioUrl: String, coverUrl: String, artistName: String, storyUrl: String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val topBarTitleState by playerViewModel.topBarTitleState.collectAsState()
    val isVoiceEnabled by playerViewModel.isVoiceEnabled.collectAsState()

    val feedItems by viewModel.homeFeedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()
    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val playState by playerViewModel.playState.collectAsState()

    // 订阅全局主题配置，区分浅色暖砂与深色暗调纸质微渐变
    val themeConfig by AppThemeManager.config.collectAsState()
    val isDark = themeConfig.isDark

    // 首页自适应渐变背景
    val homeBackgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF191A18),
                    0.25f to Color(0xFF161715),
                    1.0f to Color(0xFF141513)
                )
            )
        } else {
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFFF5E8D8),
                    0.25f to Color(0xFFFAF3EA),
                    1.0f to Color(0xFFFBF9F5)
                )
            )
        }
    }

    // TopBar 规格：内容高度 44.dp，加上状态栏总高度
    val topBarContentHeight = 44.dp
    val topBarTotalHeight = statusBarTop + topBarContentHeight

    // 滚动驱动的 TopBar 背景 Alpha (0f -> 1f)：
    // 在顶部未滚动时：alpha = 0f，完全透明，文字图标直接浮在 homeBackgroundBrush 上，100% 融合，零色差！
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
            .background(homeBackgroundBrush)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                                onItemClick = { item -> onAlbumClick(item.actionTarget, item.title) }
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
            val topBarBg = if (isDark) Color(0xFF191A18) else Color(0xFFF5E8D8)
            val topBarDividerColor = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f) else Color(0xFFE8D5BE)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(topBarBg.copy(alpha = topBarAlpha))
                    .padding(top = statusBarTop)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    SocietyWeeklyTopBar(
                        isRoamingMode = playState.isRoamingMode,
                        isVoiceEnabled = isVoiceEnabled,
                        titleState = topBarTitleState,
                        onMenuClick = onMenuClick,
                        onAvatarClick = onAvatarClick,
                        onRoamingClick = onRoamingClick,
                        onToggleVoice = {
                            val currentlyEnabled = playerViewModel.isVoiceEnabled.value
                            if (!currentlyEnabled) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    onRequestAudioPermission()
                                    false
                                } else {
                                    playerViewModel.setVoiceEnabled(true)
                                    android.widget.Toast.makeText(context, "语音点歌已开启，长按底部「发现」即可说话 🎙️", android.widget.Toast.LENGTH_SHORT).show()
                                    true
                                }
                            } else {
                                playerViewModel.setVoiceEnabled(false)
                                android.widget.Toast.makeText(context, "语音点歌已关闭", android.widget.Toast.LENGTH_SHORT).show()
                                false
                            }
                        }
                    )
                }
                // 底部细分割线，同样跟随 topBarAlpha 淡入淡出
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = topBarDividerColor.copy(alpha = topBarAlpha),
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
    isRoamingMode: Boolean = false,
    isVoiceEnabled: Boolean = false,
    titleState: TopBarTitleState = TopBarTitleState.Default,
    onMenuClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onRoamingClick: () -> Unit = {},
    onToggleVoice: () -> Boolean = { false }
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var showAvatarMenu by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    fun dismissMenu() {
        isMenuExpanded = false
        coroutineScope.launch {
            delay(120)
            showAvatarMenu = false
        }
    }

    // 漫游模式下头像彩虹流光溢彩若隐若现动效 (Ethereal Iridescent Rainbow Sweep Halo - 低调纤细珠光版)
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
    // 4. 漫游开关平滑渐变
    val haloVisibility by animateFloatAsState(
        targetValue = if (isRoamingMode) 1f else 0f,
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
                tint = MaterialTheme.colorScheme.primary
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

        // 头像与折叠胶囊菜单容器
        Box(
            contentAlignment = Alignment.TopEnd
        ) {
            // 头像按钮（与左侧 Menu 按钮尺寸完全对称 36dp，低调优雅）
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
                // 1. 头像主体（32dp 精准居中）
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (haloVisibility <= 0.01f) {
                                Modifier.border(
                                    width = 0.8.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                            } else Modifier
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    SongbookImage(
                        model = "/storage/avatars/user_avatar_default.jpg",
                        contentDescription = "User Avatar",
                        fallbackRes = R.drawable.user_avatar_default,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 2. 紧紧贴合头像外沿的低调彩虹流光环 (与 32dp 头像零空隙严密贴合)
                if (haloVisibility > 0.01f) {
                    Canvas(
                        modifier = Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                rotationZ = haloRotation
                            }
                    ) {
                        val canvasCenter = this.center
                        // 紧贴 32dp 头像外周边缘（半径严格对齐头像边界，无任何空隙）
                        val radius = (size.minDimension / 2f)
                        val sweepBrush = Brush.sweepGradient(RainbowHaloColors, canvasCenter)

                        // 柔和微晕层 (Subtle Feather Glow - 2.0dp, 极低半透明微漫反射)
                        drawCircle(
                            brush = sweepBrush,
                            radius = radius * haloGlowScale,
                            style = Stroke(width = 2.0.dp.toPx()),
                            alpha = haloAlpha * 0.15f * haloVisibility
                        )

                        // 核心紧贴彩虹环 (Tight Prismatic Ring - 1.3dp, 零空隙咬合包裹头像)
                        drawCircle(
                            brush = sweepBrush,
                            radius = radius,
                            style = Stroke(width = 1.3.dp.toPx()),
                            alpha = (0.50f + haloAlpha * 0.35f) * haloVisibility
                        )
                    }
                }
            }

            // 折叠卡片垂直展开胶囊 (Vertical Action Capsule - 严格垂直居中对齐上方头像)
            if (showAvatarMenu) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(x = 0, y = with(LocalDensity.current) { 40.dp.roundToPx() }),
                    onDismissRequest = { dismissMenu() },
                    properties = PopupProperties(focusable = true)
                ) {
                    CapsuleActionMenu(
                        isExpanded = isMenuExpanded,
                        isRoamingMode = isRoamingMode,
                        isVoiceEnabled = isVoiceEnabled,
                        onRoamingClick = {
                            dismissMenu()
                            onRoamingClick()
                        },
                        onAvatarClick = {
                            dismissMenu()
                            onAvatarClick()
                        },
                        onToggleVoice = {
                            val res = onToggleVoice()
                            dismissMenu()
                            res
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
 */
@Composable
private fun CapsuleActionMenu(
    isExpanded: Boolean,
    isRoamingMode: Boolean,
    isVoiceEnabled: Boolean,
    onRoamingClick: () -> Unit,
    onAvatarClick: () -> Unit,
    onToggleVoice: () -> Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeConfig by AppThemeManager.config.collectAsState()
    val isDark = themeConfig.isDark

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
                .width(40.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0x334A2810),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color(0x1F5C3318)
                ),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // ── 1. 随机漫游图标 ──
                RoamActionButton(
                    isRoamingMode = isRoamingMode,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onRoamingClick()
                    }
                )

                // 细微纸质分割线
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(0.6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )

                // ── 2. 语音功能总开关图标（点击切换开启/关闭） ──
                VoiceToggleSwitchButton(
                    isEnabled = isVoiceEnabled,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleVoice()
                    }
                )

                // 细微纸质分割线
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(0.6.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )

                // ── 3. 个人中心/账户图标 ──
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            onDismissRequest()
                            onAvatarClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_user_profile),
                        contentDescription = "User Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * 漫游模式按钮：
 * - 漫游激活时：火漆焦橙色高亮 + 浅杏橙暖色圆底背景 + 细腻微呼吸
 * - 未激活时：置灰石板铅印色（无底色）
 */
@Composable
private fun RoamActionButton(
    isRoamingMode: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "roam_active_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "roam_pulse_scale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (isRoamingMode) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_roam_dice),
            contentDescription = "随心漫游",
            tint = if (isRoamingMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .size(24.dp)
                .then(
                    if (isRoamingMode) {
                        Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    } else Modifier
                )
        )
    }
}

/**
 * 语音功能总开关按钮：
 * - 开启状态：经典火漆焦橙色高亮 + 浅杏橙暖色圆底背景 + 极细微微光呼吸
 * - 关闭状态：置灰石板铅印色（无底色），低调安静
 */
@Composable
private fun VoiceToggleSwitchButton(
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_toggle_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_toggle_scale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .then(
                if (isEnabled) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic_voice),
            contentDescription = if (isEnabled) "语音功能已开启" else "语音功能已关闭",
            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .then(
                    if (isEnabled) {
                        Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    } else Modifier
                )
        )
    }
}
