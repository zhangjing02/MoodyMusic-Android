package com.example.moodymusicforandroid.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.common.network.ApiServiceProvider
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 管理切片化动态首页流 (Block-Based SDUI) 的网络拉取、多态状态解析与离线保底默认数据。
 */
class HomeViewModel : BaseViewModel() {

    private val _homeFeedItems = MutableStateFlow<List<HomeBlockItem>>(getOfflineDefaultFeed())
    val homeFeedItems: StateFlow<List<HomeBlockItem>> = _homeFeedItems.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow("all")
    val selectedCategoryId: StateFlow<String> = _selectedCategoryId.asStateFlow()

    private val _currentlyPlayingTrackId = MutableStateFlow<String?>(null)
    val currentlyPlayingTrackId: StateFlow<String?> = _currentlyPlayingTrackId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchHomeFeed()
    }

    /**
     * 从服务端拉取动态切片流 (SDUI)
     */
    fun fetchHomeFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // 1. 优先请求 ApiService 接口
                val response = ApiServiceProvider.apiService.getHomeFeed()
                if (response.isSuccessful && response.body() != null) {
                    val feed = response.body()!!.data
                    if (feed != null && feed.items.isNotEmpty()) {
                        _homeFeedItems.value = preParseItems(feed.items)
                        _isRefreshing.value = false
                        return@launch
                    }
                }

                // 2. 备用尝试 MoodyApiService 统一接口
                val moodyResponse = MoodyApiProvider.apiService.getHomeFeed()
                if (moodyResponse.code == 200 && moodyResponse.data != null) {
                    val feed = moodyResponse.data!!
                    if (feed.items.isNotEmpty()) {
                        _homeFeedItems.value = preParseItems(feed.items)
                        _isRefreshing.value = false
                        return@launch
                    }
                }
            } catch (e: Exception) {
                // 网络异常或无数据时，静默保留高质量离线保底数据
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun preParseItems(items: List<HomeBlockItem>): List<HomeBlockItem> {
        return items.map { block ->
            if (block.parsedData != null) return@map block
            val parsed = when (block.type) {
                HomeBlockType.TOP_RECOMMEND_BANNER -> block.toTopRecommendBanner()
                HomeBlockType.TODAY_RECOMMEND_SCROLL -> block.toTodayRecommendScroll()
                HomeBlockType.DEEP_DIVE_FEATURE -> block.toDeepDiveFeature()
                HomeBlockType.VARIETY_SHOW_GRID -> block.toVarietyShowGrid()
                HomeBlockType.HERO_BANNER -> block.toHeroBanner()
                HomeBlockType.CATEGORY_TABS -> block.toCategoryTabs()
                HomeBlockType.SECTION_TITLE -> block.toSectionTitle()
                HomeBlockType.ESSAY_CARD -> block.toEssayCard()
                HomeBlockType.ARTIST_GRID -> block.toArtistGrid()
                HomeBlockType.TRACK_LIST -> block.toTrackList()
                HomeBlockType.ARCHIVE_CARD -> block.toArchiveCard()
                HomeBlockType.IMAGE_FEATURE -> block.toImageFeature()
                HomeBlockType.QUICK_ACTIONS -> block.toQuickActions()
                else -> null
            }
            if (parsed != null) block.copy(parsedData = parsed) else block
        }
    }

    /**
     * 切换分类标签状态
     */
    fun selectCategory(categoryId: String) {
        _selectedCategoryId.value = categoryId
        _homeFeedItems.value = _homeFeedItems.value.map { block ->
            if (block.type == HomeBlockType.CATEGORY_TABS) {
                val currentTabsData = block.toCategoryTabs()
                val updatedTabs = currentTabsData.tabs.map { tab ->
                    tab.copy(isSelected = (tab.id == categoryId))
                }
                block.copy(parsedData = currentTabsData.copy(tabs = updatedTabs, selectedId = categoryId))
            } else {
                block
            }
        }
    }

    /**
     * 切换单曲试听播放状态
     */
    fun toggleTrackPlay(trackId: String) {
        val nextPlayingId = if (_currentlyPlayingTrackId.value == trackId) null else trackId
        _currentlyPlayingTrackId.value = nextPlayingId

        _homeFeedItems.value = _homeFeedItems.value.map { block ->
            if (block.type == HomeBlockType.TRACK_LIST) {
                val trackListData = block.toTrackList()
                val updatedTracks = trackListData.tracks.map { track ->
                    track.copy(isPlaying = (track.id == nextPlayingId))
                }
                block.copy(parsedData = trackListData.copy(tracks = updatedTracks))
            } else {
                block
            }
        }
    }

    /**
     * 离线保底默认数据列表（保证网络断开或首次加载时 UI 完美呈现四大核心切片）
     */
    private fun getOfflineDefaultFeed(): List<HomeBlockItem> {
        return listOf(
            // 1. 顶部推荐 Banner (top_recommend_banner)
            HomeBlockItem(
                id = "block_top_recommend_banner",
                type = HomeBlockType.TOP_RECOMMEND_BANNER,
                parsedData = TopRecommendBannerData(
                    id = "snow_cafe_theme",
                    title = "《雪天咖啡館的閱讀鋼琴》",
                    subtitle = "窗邊熱咖啡、一本書，慢慢過今天",
                    badge = "TOP 推荐",
                    coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/home/snow_cafe_static.jpg",
                    audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/snow_cafe_piano.mp3",
                    artistName = "放鬆鋼琴 · 慢時光",
                    actionType = "theme",
                    actionTarget = "snow_cafe_theme"
                )
            ),

            // 2. 今日推荐横滑列表 (today_recommend_scroll)
            HomeBlockItem(
                id = "block_today_recommend_scroll",
                type = HomeBlockType.TODAY_RECOMMEND_SCROLL,
                parsedData = TodayRecommendScrollData(
                    title = "今日推荐",
                    subtitle = "TODAY'S VINYL SELECTION",
                    items = listOf(
                        TodayRecommendItem(
                            id = "bach_cello_theme",
                            title = "Bach Cello Suites",
                            artist = "Lu Dimon & Mu Dimon",
                            year = "1720 / 2026",
                            subtitle = "让巴赫的大提琴安抚浮躁的心",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/albums/bach_cello_cover.jpg",
                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/bach_cello_collection.mp3",
                            isTheme = true,
                            themeId = "bach_cello_theme"
                        ),
                        TodayRecommendItem(
                            id = "jonathan_lee_theme",
                            title = "理性與感性",
                            artist = "李宗盛",
                            year = "2007",
                            subtitle = "30首歲月金曲 · 寫盡人世間的悲歡离合",
                            coverUrl = "https://m-api.changgepd.ccwu.cc/storage/covers/albums/album_jonathan_lee.jpg",
                            audioUrl = "https://m-api.changgepd.ccwu.cc/storage/music/theme/jonathan_lee_30.m4a",
                            isTheme = true,
                            themeId = "jonathan_lee_theme"
                        ),
                        TodayRecommendItem(
                            id = "lofi_chill_theme",
                            title = "忘記時間的旋律",
                            artist = "Lova Radio",
                            year = "2026",
                            subtitle = "Lo-fi Chill 溫柔旋律陪你慢慢回血",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/albums/lofi_chill_cover.jpg",
                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/lofi_chill.mp3",
                            isTheme = true,
                            themeId = "lofi_chill_theme"
                        ),
                        TodayRecommendItem(
                            id = "pop_piano_theme",
                            title = "華語經典鋼琴曲",
                            artist = "Love Piano",
                            year = "2026",
                            subtitle = "流行情歌鋼琴改編，只想靜靜聽音樂",
                            coverUrl = "https://m-api.changgepd.ccwu.cc/storage/covers/albums/pop_piano_cover.jpg",
                            audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/pop_piano.mp3",
                            isTheme = true,
                            themeId = "pop_piano_theme"
                        )
                    )
                )
            ),

            // 3. 深度解读卡片 (deep_dive_feature)
            HomeBlockItem(
                id = "block_deep_dive_feature",
                type = HomeBlockType.DEEP_DIVE_FEATURE,
                parsedData = DeepDiveFeatureData(
                    id = "butterfly_lovers_deep_dive",
                    title = "《梁祝》小提琴协奏曲：东方交响的化蝶史诗",
                    tag = "深度名作解析 · DEEP DIVE",
                    summary = "以西方交响之弓，引越剧缠绵之韵。何占豪与陈钢笔下的东方绝唱，在草桥结拜、长亭惜别、抗婚哭灵与双双化蝶中，成就半个世纪的传世经典。",
                    coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/hero/butterfly_lovers_hero_clean.jpg",
                    audioUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3",
                    articleId = "butterfly_lovers_deep_dive",
                    albumId = "butterfly_lovers_album",
                    albumTitle = "《梁祝》小提琴协奏曲",
                    primaryActionText = "阅读深度专题",
                    secondaryActionText = "聆听全曲"
                )
            ),

            // 4. 音乐综艺双排网格 (variety_show_grid)
            HomeBlockItem(
                id = "block_variety_show_grid",
                type = HomeBlockType.VARIETY_SHOW_GRID,
                parsedData = VarietyShowGridData(
                    title = "音乐综艺精选",
                    subtitle = "POPULAR MUSIC VARIETY",
                    items = listOf(
                        VarietyShowItem(
                            id = "variety_voice_of_china",
                            title = "中国好声音",
                            subtitle = "导师盲选 · 为梦想转身",
                            badge = "现场盲选",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/voice_of_china.jpg",
                            actionType = "playlist",
                            actionTarget = "voice_of_china"
                        ),
                        VarietyShowItem(
                            id = "variety_i_am_singer",
                            title = "我是歌手",
                            subtitle = "殿堂唱将 · 极致交响Live",
                            badge = "殿堂竞演",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/i_am_singer.jpg",
                            actionType = "playlist",
                            actionTarget = "i_am_singer"
                        ),
                        VarietyShowItem(
                            id = "variety_masked_singer",
                            title = "蒙面唱将猜猜猜",
                            subtitle = "面具之下 · 纯粹原声共鸣",
                            badge = "悬念声场",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/masked_singer.jpg",
                            actionType = "playlist",
                            actionTarget = "masked_singer"
                        ),
                        VarietyShowItem(
                            id = "variety_big_band",
                            title = "乐队的夏天",
                            subtitle = "燥热现场 · 独立原创摇滚",
                            badge = "滚烫现场",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/big_band.jpg",
                            actionType = "playlist",
                            actionTarget = "big_band"
                        )
                    )
                )
            )
        )
    }
}
