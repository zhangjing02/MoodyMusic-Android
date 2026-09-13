package com.example.moodymusicforandroid.data.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * 首页切片流响应数据结构 (SDUI - Server-Driven UI)
 */
data class HomeFeedResponse(
    @SerializedName("title") val title: String? = null,
    @SerializedName("issueNumber") val issueNumber: String? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("items") val items: List<HomeBlockItem> = emptyList()
)

/**
 * 首页切片多态数据项
 */
data class HomeBlockItem(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("data") val data: JsonElement? = null,
    @Transient val parsedData: Any? = null
)

/**
 * 切片类型常量定义
 */
object HomeBlockType {
    const val TOP_RECOMMEND_BANNER = "top_recommend_banner"
    const val TODAY_RECOMMEND_SCROLL = "today_recommend_scroll"
    const val DEEP_DIVE_FEATURE = "deep_dive_feature"
    const val VARIETY_SHOW_GRID = "variety_show_grid"

    const val HERO_BANNER = "hero_banner"
    const val CATEGORY_TABS = "category_tabs"
    const val SECTION_TITLE = "section_title"
    const val ESSAY_CARD = "essay_card"
    const val ARTIST_GRID = "artist_grid"
    const val TRACK_LIST = "track_list"
    const val ARCHIVE_CARD = "archive_card"
    const val IMAGE_FEATURE = "image_feature"
    const val QUICK_ACTIONS = "quick_actions"
}

// ==================== 四大标准布局数据结构 ====================

/**
 * 1. 顶部推荐 Banner 数据 (top_recommend_banner)
 */
data class TopRecommendBannerData(
    @SerializedName("id") val id: String = "snow_cafe_theme",
    @SerializedName("title") val title: String = "《雪天咖啡館的閱讀鋼琴》",
    @SerializedName("subtitle") val subtitle: String = "窗邊熱咖啡、一本書，慢慢過今天",
    @SerializedName("badge") val badge: String = "TOP 推荐",
    @SerializedName("coverUrl") val coverUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/home/snow_cafe_static.jpg",
    @SerializedName("audioUrl") val audioUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/snow_cafe_piano.mp3",
    @SerializedName("artistName") val artistName: String = "放鬆鋼琴 · 慢時光",
    @SerializedName("actionType") val actionType: String = "theme",
    @SerializedName("actionTarget") val actionTarget: String = "snow_cafe_theme"
)

/**
 * 2. 今日推荐横滑列表数据 (today_recommend_scroll)
 */
data class TodayRecommendScrollData(
    @SerializedName("title") val title: String = "今日推荐",
    @SerializedName("subtitle") val subtitle: String = "TODAY'S VINYL SELECTION",
    @SerializedName("items") val items: List<TodayRecommendItem> = emptyList()
)

data class TodayRecommendItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("year") val year: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("coverUrl") val coverUrl: String,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("isTheme") val isTheme: Boolean = false,
    @SerializedName("themeId") val themeId: String? = null
)

/**
 * 3. 深度解读卡片数据 (deep_dive_feature)
 */
data class DeepDiveFeatureData(
    @SerializedName("id") val id: String = "butterfly_lovers_deep_dive",
    @SerializedName("title") val title: String = "《梁祝》小提琴协奏曲：东方交响的化蝶史诗",
    @SerializedName("tag") val tag: String = "深度名作解析 · DEEP DIVE",
    @SerializedName("summary") val summary: String = "以西方交响之弓，引越剧缠绵之韵。何占豪与陈钢笔下的东方绝唱，在草桥结拜、长亭惜别、抗婚哭灵与双双化蝶中，成就半个世纪的传世经典。",
    @SerializedName("coverUrl") val coverUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/hero/butterfly_lovers_hero_clean.jpg",
    @SerializedName("audioUrl") val audioUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/butterfly_lovers_concerto.mp3",
    @SerializedName("articleId") val articleId: String = "butterfly_lovers_deep_dive",
    @SerializedName("albumId") val albumId: String = "butterfly_lovers_album",
    @SerializedName("albumTitle") val albumTitle: String = "《梁祝》小提琴协奏曲",
    @SerializedName("primaryActionText") val primaryActionText: String = "阅读深度专题",
    @SerializedName("secondaryActionText") val secondaryActionText: String = "聆听全曲"
)

/**
 * 4. 音乐综艺双排网格数据 (variety_show_grid)
 */
data class VarietyShowGridData(
    @SerializedName("title") val title: String = "音乐综艺精选",
    @SerializedName("subtitle") val subtitle: String = "POPULAR MUSIC VARIETY",
    @SerializedName("items") val items: List<VarietyShowItem> = emptyList()
)

data class VarietyShowItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("badge") val badge: String? = null,
    @SerializedName("coverUrl") val coverUrl: String,
    @SerializedName("actionType") val actionType: String = "playlist",
    @SerializedName("actionTarget") val actionTarget: String
)

// ==================== 各切片具体数据结构 ====================

/**
 * 1. 深度专题 Hero 卡片数据
 */
data class HeroBannerData(
    @SerializedName("title") val title: String = "《梁祝》小提琴协奏曲：东方交响的化蝶史诗",
    @SerializedName("tag") val tag: String = "深度名作解析 · DEEP DIVE",
    @SerializedName("summary") val summary: String = "以西方交响之弓，引越剧缠绵之韵。何占豪与陈钢笔下的东方绝唱，在草桥结拜、长亭惜别、抗婚哭灵与双双化蝶中，成就半个世纪的传世经典。",
    @SerializedName("imageUrl") val imageUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/hero/butterfly_lovers_hero_clean.jpg",
    @SerializedName("articleId") val articleId: String = "butterfly_lovers_deep_dive",
    @SerializedName("albumId") val albumId: String = "butterfly_lovers_album",
    @SerializedName("albumTitle") val albumTitle: String = "《梁祝》小提琴协奏曲",
    @SerializedName("primaryActionText") val primaryActionText: String = "阅读深度专题",
    @SerializedName("secondaryActionText") val secondaryActionText: String = "聆听全曲"
)

/**
 * 2. 分类标签栏数据
 */
data class CategoryTabsData(
    @SerializedName("tabs") val tabs: List<CategoryTabItem> = emptyList(),
    @SerializedName("selectedId") val selectedId: String = "all"
)

data class CategoryTabItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("isSelected") val isSelected: Boolean = false
)

/**
 * 3. 栏目标题与期号数据
 */
data class SectionTitleData(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("actionText") val actionText: String? = null,
    @SerializedName("actionRoute") val actionRoute: String? = null
)

/**
 * 4. 选集随笔列表数据
 */
data class EssayCardData(
    @SerializedName("title") val title: String = "选集随笔",
    @SerializedName("essays") val essays: List<EssayItemData> = emptyList()
)

data class EssayItemData(
    @SerializedName("id") val id: String,
    @SerializedName("date") val date: String,
    @SerializedName("title") val title: String,
    @SerializedName("summary") val summary: String,
    @SerializedName("readTime") val readTime: String? = null,
    @SerializedName("coverUrl") val coverUrl: String? = null
)

/**
 * 5. 艺术家网格/列表数据
 */
data class ArtistGridData(
    @SerializedName("title") val title: String? = "精选艺术家",
    @SerializedName("actionText") val actionText: String? = "浏览全部",
    @SerializedName("layout") val layout: String = "grid", // "grid", "row", "list"
    @SerializedName("artists") val artists: List<ArtistBlockItem> = emptyList()
)

data class ArtistBlockItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("albumCount") val albumCount: Int = 0,
    @SerializedName("avatarUrl") val avatarUrl: String? = null
)

/**
 * 6. 单曲试听列表数据
 */
data class TrackListData(
    @SerializedName("title") val title: String? = "今日单曲试听",
    @SerializedName("tracks") val tracks: List<TrackBlockItem> = emptyList()
)

data class TrackBlockItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("album") val album: String? = null,
    @SerializedName("duration") val duration: String = "03:45",
    @SerializedName("coverUrl") val coverUrl: String? = null,
    @SerializedName("audioUrl") val audioUrl: String? = null,
    @SerializedName("isPlaying") val isPlaying: Boolean = false
)

/**
 * 7. 时代留声机 / 档案卡片数据
 */
data class ArchiveCardData(
    @SerializedName("id") val id: String = "archive_1",
    @SerializedName("title") val title: String = "冀西南林家铺子",
    @SerializedName("subtitle") val subtitle: String = "万能青年旅店 • 2020",
    @SerializedName("description") val description: String? = "时代留声机：当代华语独立摇滚里程碑之作",
    @SerializedName("imageUrl") val imageUrl: String = "/storage/covers/albums/album_hebei_kirin.jpg",
    @SerializedName("badge") val badge: String? = "典藏黑胶",
    @SerializedName("targetRoute") val targetRoute: String? = null
)

/**
 * 8. 杂志视觉大图数据
 */
data class ImageFeatureData(
    @SerializedName("imageUrl") val imageUrl: String = "/storage/covers/hero/hero_acoustic_guitar.jpg",
    @SerializedName("caption") val caption: String? = "静谧之声 / SOUND OF SILENCE",
    @SerializedName("author") val author: String? = "Photo by Songbook Studio",
    @SerializedName("aspectRatio") val aspectRatio: Float = 1.78f,
    @SerializedName("targetRoute") val targetRoute: String? = null
)

/**
 * 9. 快捷功能入口数据
 */
data class QuickActionsData(
    @SerializedName("title") val title: String? = null,
    @SerializedName("actions") val actions: List<QuickActionItem> = emptyList()
)

data class QuickActionItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("icon") val icon: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("route") val route: String? = null
)

// ==================== 安全解析扩展方法 ====================

object HomeJsonParser {
    val gson: Gson get() = com.example.moodymusicforandroid.common.network.RetrofitClient.defaultGson
}

inline fun <reified T> HomeBlockItem.getParsedData(): T? {
    if (parsedData is T) return parsedData
    if (data == null) return null
    return try {
        HomeJsonParser.gson.fromJson(data, T::class.java)
    } catch (_: Exception) {
        null
    }
}

fun HomeBlockItem.toHeroBanner(): HeroBannerData {
    val parsed = getParsedData<HeroBannerData>()
    if (parsed != null && !parsed.title.contains("黑胶灵魂")) {
        return parsed
    }
    return HeroBannerData()
}
fun HomeBlockItem.toCategoryTabs(): CategoryTabsData = getParsedData<CategoryTabsData>() ?: CategoryTabsData()
fun HomeBlockItem.toSectionTitle(): SectionTitleData = getParsedData<SectionTitleData>() ?: SectionTitleData(title = "")
fun HomeBlockItem.toEssayCard(): EssayCardData = getParsedData<EssayCardData>() ?: EssayCardData()
fun HomeBlockItem.toArtistGrid(): ArtistGridData = getParsedData<ArtistGridData>() ?: ArtistGridData()
fun HomeBlockItem.toTrackList(): TrackListData = getParsedData<TrackListData>() ?: TrackListData()
fun HomeBlockItem.toArchiveCard(): ArchiveCardData = getParsedData<ArchiveCardData>() ?: ArchiveCardData()
fun HomeBlockItem.toImageFeature(): ImageFeatureData = getParsedData<ImageFeatureData>() ?: ImageFeatureData()
fun HomeBlockItem.toQuickActions(): QuickActionsData = getParsedData<QuickActionsData>() ?: QuickActionsData()

// 四大标准切片快捷解析
fun HomeBlockItem.toTopRecommendBanner(): TopRecommendBannerData = getParsedData<TopRecommendBannerData>() ?: TopRecommendBannerData()
fun HomeBlockItem.toTodayRecommendScroll(): TodayRecommendScrollData = getParsedData<TodayRecommendScrollData>() ?: TodayRecommendScrollData()
fun HomeBlockItem.toDeepDiveFeature(): DeepDiveFeatureData = getParsedData<DeepDiveFeatureData>() ?: DeepDiveFeatureData()
fun HomeBlockItem.toVarietyShowGrid(): VarietyShowGridData = getParsedData<VarietyShowGridData>() ?: VarietyShowGridData()
