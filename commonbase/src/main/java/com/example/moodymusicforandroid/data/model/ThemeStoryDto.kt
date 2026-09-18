package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 专栏故事数据传输对象 (Server-Driven Theme Story)
 * 供二级专栏详情页 (ThemeDetailScreen) 纯动态渲染使用，杜绝前端任何硬编码文案
 */
data class ThemeStoryDto(
    @SerializedName("themeId") val themeId: String = "",
    @SerializedName("issueTag") val issueTag: String = "",
    @SerializedName("categoryTag") val categoryTag: String = "",
    @SerializedName("headline") val headline: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("authorDate") val authorDate: String = "",
    @SerializedName("heroUrl") val heroUrl: String? = null,
    @SerializedName("bodyParagraphs") val bodyParagraphs: List<String> = emptyList(),
    @SerializedName("quoteEn") val quoteEn: String = "",
    @SerializedName("quoteZh") val quoteZh: String = "",
    @SerializedName("scenariosTitle") val scenariosTitle: String = "🎧 适用场景",
    @SerializedName("scenarios") val scenarios: List<String> = emptyList(),
    @SerializedName("benefitsTitle") val benefitsTitle: String = "✨ 专题亮点",
    @SerializedName("benefits") val benefits: List<String> = emptyList(),
    @SerializedName("aboutTitle") val aboutTitle: String = "👉 关于作品与演绎者",
    @SerializedName("aboutDesc") val aboutDesc: String = "",
    @SerializedName("aboutMotto") val aboutMotto: String = "",
    @SerializedName("footerSign") val footerSign: String = "",
    @SerializedName("playPillTextActive") val playPillTextActive: String = "演奏中",
    @SerializedName("playPillTextIdle") val playPillTextIdle: String = "开始播放",
    @SerializedName("isSquareCover") val isSquareCover: Boolean = false,
    @SerializedName("posterAspectRatio") val posterAspectRatio: Float = 1.6f,
    @SerializedName("timelineTitle") val timelineTitle: String = "",
    @SerializedName("timelineSections") val timelineSections: List<TimelineSectionDto> = emptyList()
)

data class TimelineSectionDto(
    @SerializedName("timeLabel") val timeLabel: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("sceneStory") val sceneStory: String = "",
    @SerializedName("emotion") val emotion: String = "",
    @SerializedName("technique") val technique: String = "",
    @SerializedName("performerNote") val performerNote: String = ""
)

val ThemeStoryDto.safeBodyParagraphs: List<String>
    get() = bodyParagraphs ?: emptyList()

val ThemeStoryDto.safeScenarios: List<String>
    get() = scenarios ?: emptyList()

val ThemeStoryDto.safeBenefits: List<String>
    get() = benefits ?: emptyList()

val ThemeStoryDto.safeTimelineSections: List<TimelineSectionDto>
    get() = timelineSections ?: emptyList()
