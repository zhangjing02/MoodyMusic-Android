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
                            id = "jacky_cheung_classic_tour_theme",
                            title = "《經典之旅》",
                            artist = "張學友",
                            year = "2018",
                            subtitle = "39首神級現場 · 台北站",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/jacky_cheung_classic_tour_cover.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/jacky_cheung_classic_tour_taipei.m4a",
                            isTheme = true,
                            themeId = "jacky_cheung_classic_tour_theme",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/jacky_cheung_classic_tour_theme.json"
                        ),
                        TodayRecommendItem(
                            id = "pub_heroes_theme",
                            title = "《PUB英雄會》",
                            artist = "動力火車 · 迪克牛仔",
                            year = "1996",
                            subtitle = "十大名團點唱神作 · 原汁原味",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/pub_heroes_cover_v1.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/pub_heroes_1996_theme.mp3",
                            isTheme = true,
                            themeId = "pub_heroes_theme",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/pub_heroes_theme_v1.json"
                        ),
                        TodayRecommendItem(
                            id = "rene_liu_live_theme",
                            title = "《後來》現場精選",
                            artist = "劉若英",
                            year = "2002-2024",
                            subtitle = "一口氣聽完7首神級Live",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/rene_liu_live_cover_v1.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/rene_liu_live_128k.mp3",
                            isTheme = true,
                            themeId = "rene_liu_live_theme",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/rene_liu_live_theme_v1.json"
                        ),
                        TodayRecommendItem(
                            id = "wen_4versions_theme",
                            title = "《問》四版現場",
                            artist = "林憶蓮 · 李宗盛",
                            year = "1994-2014",
                            subtitle = "一曲唱盡四種人生 · 經典對比",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/wen_sandy_cover_v2.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/wen_4versions_128k.mp3",
                            isTheme = true,
                            themeId = "wen_4versions_theme",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/wen_4versions_theme_v2.json"
                        ),
                        TodayRecommendItem(
                            id = "jonathan_lee_theme",
                            title = "《理性與感性》",
                            artist = "李宗盛",
                            year = "2007",
                            subtitle = "30首歲月金曲 · 寫盡悲歡離合",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/album_jonathan_lee.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/jonathan_lee_30.m4a",
                            isTheme = true,
                            themeId = "jonathan_lee_theme",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/jonathan_lee_theme.json"
                        ),
                        TodayRecommendItem(
                            id = "guofeng_flute_theme_v3",
                            title = "《江湖入夢》",
                            artist = "書領了嗎 · 竹笛",
                            year = "2026",
                            subtitle = "竹笛清響，夢回青城煙雨",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/albums/guofeng_flute_collection_cover_v3.jpg",
                            audioUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/music/theme/guofeng_flute_collection_v2.mp3",
                            isTheme = true,
                            themeId = "guofeng_flute_theme_v3",
                            storyUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/themes/guofeng_flute_theme_v3.json"
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
                            actionType = "artist",
                            actionTarget = "133"
                        ),
                        VarietyShowItem(
                            id = "variety_i_am_singer",
                            title = "我是歌手",
                            subtitle = "殿堂唱将 · 极致交响Live",
                            badge = "殿堂竞演",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/i_am_singer.jpg",
                            actionType = "artist",
                            actionTarget = "134"
                        ),
                        VarietyShowItem(
                            id = "variety_masked_singer",
                            title = "蒙面唱将猜猜猜",
                            subtitle = "面具之下 · 纯粹原声共鸣",
                            badge = "悬念声场",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/masked_singer.jpg",
                            actionType = "artist",
                            actionTarget = "135"
                        ),
                        VarietyShowItem(
                            id = "variety_big_band",
                            title = "乐队的夏天",
                            subtitle = "燥热现场 · 独立原创摇滚",
                            badge = "滚烫现场",
                            coverUrl = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/variety/big_band.jpg",
                            actionType = "artist",
                            actionTarget = "178"
                        ),
                        VarietyShowItem(
                            id = "variety_our_songs",
                            title = "我们的歌",
                            subtitle = "跨代合唱 · 岁月金曲新编",
                            badge = "潮音重构",
                            coverUrl = "https://pub-3951bb1f42a440049b8d1eb0575cfdee.r2.dev/covers/variety/our_songs.jpg",
                            actionType = "artist",
                            actionTarget = "191"
                        )
                    )
                )
            )
        )
    }
}
