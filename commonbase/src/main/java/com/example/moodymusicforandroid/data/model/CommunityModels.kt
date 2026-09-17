package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 社区帖子与待办任务实体
 */
data class CommunityPost(
    @SerializedName("id")
    val id: Long,

    @SerializedName("category")
    val category: String, // chat, resource, bug, ui, feature

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("author_name")
    val authorName: String,

    @SerializedName("author_avatar")
    val authorAvatar: String? = null,

    @SerializedName("is_task")
    val isTask: Boolean = false,

    @SerializedName("status")
    val status: String = "pending", // pending, in_progress, completed

    @SerializedName("is_pinned")
    val isPinned: Boolean = false,

    @SerializedName("comment_count")
    val commentCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    fun isCompleted(): Boolean = status == "completed"

    fun getCategoryDisplayName(): String = when (category) {
        "chat" -> "吐槽闲聊"
        "resource" -> "资源补齐"
        "bug" -> "Bug提示"
        "ui" -> "页面优化"
        "feature" -> "功能缺失"
        else -> "话题交流"
    }

    fun getStatusDisplayName(): String = when (status) {
        "completed" -> "已完成"
        "in_progress" -> "进行中"
        else -> "待处理"
    }
}

/**
 * 社区跟帖评论实体
 */
data class CommunityComment(
    @SerializedName("id")
    val id: Long,

    @SerializedName("post_id")
    val postId: Long,

    @SerializedName("user_id")
    val userId: Long,

    @SerializedName("author_name")
    val authorName: String,

    @SerializedName("author_avatar")
    val authorAvatar: String? = null,

    @SerializedName("content")
    val content: String,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * 官方系统公告实体
 */
data class SystemNotice(
    @SerializedName("id")
    val id: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("author_name")
    val authorName: String = "音信官方",

    @SerializedName("is_pinned")
    val isPinned: Boolean = false,

    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * 发帖请求
 */
data class CreatePostRequest(
    @SerializedName("category")
    val category: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String
)

/**
 * 修改任务状态请求 (仅管理员/Master)
 */
data class UpdatePostStatusRequest(
    @SerializedName("status")
    val status: String // pending, in_progress, completed
)

/**
 * 发送跟帖评论请求
 */
data class CreateCommunityCommentRequest(
    @SerializedName("content")
    val content: String
)

/**
 * 发布系统公告请求 (仅管理员/Master)
 */
data class CreateNoticeRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("is_pinned")
    val isPinned: Boolean = false
)

/**
 * 资源补齐专用的格式化数据结构 (支持专辑、歌曲、歌手、演唱会、音质升级 5 维精准对齐)
 */
data class ResourceAlignmentData(
    @SerializedName("type")
    val type: String = "resource_alignment",

    @SerializedName("sub_type")
    val subType: String = "album", // album, song, artist, concert, hires

    @SerializedName("artist")
    val artist: String = "",

    @SerializedName("album")
    val album: String = "",

    @SerializedName("songs")
    val songs: String = "",

    @SerializedName("year")
    val year: String? = null,

    @SerializedName("desc")
    val desc: String? = null,

    // 针对单曲补齐
    @SerializedName("song_name")
    val songName: String? = null,

    // 针对歌手补齐
    @SerializedName("genre")
    val genre: String? = null,

    // 针对演唱会补齐
    @SerializedName("concert_name")
    val concertName: String? = null,

    // 针对音质升级
    @SerializedName("quality_issue")
    val qualityIssue: String? = null,

    @SerializedName("quality_spec")
    val qualitySpec: String? = null,

    // 参考链接或试听来源
    @SerializedName("link")
    val link: String? = null
) {
    fun getSubTypeName(): String = when (subType) {
        "song" -> "歌曲补齐"
        "artist" -> "歌手补齐"
        "concert" -> "演唱会补齐"
        "hires" -> "音质升级"
        else -> "专辑补齐"
    }

    fun getSubTypeIcon(): String = when (subType) {
        "song" -> "🎵"
        "artist" -> "🎤"
        "concert" -> "🎸"
        "hires" -> "🎧"
        else -> "💿"
    }
}

// 保持向后兼容的类型别名
typealias ResourceAlbumData = ResourceAlignmentData

object ResourceHelper {
    private val gson by lazy { com.google.gson.Gson() }

    /**
     * 将资源对齐结构体编码为 JSON 存储在 content 中
     */
    fun encode(data: ResourceAlignmentData): String {
        return gson.toJson(data)
    }

    /**
     * 解析 content：优先尝试解析 JSON，其次尝试从结构化文本中提取
     */
    fun decode(content: String?): ResourceAlignmentData? {
        if (content.isNullOrBlank()) return null
        val trimmed = content.trim()

        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val data = gson.fromJson(trimmed, ResourceAlignmentData::class.java)
                if (data != null && (data.artist.isNotBlank() || data.album.isNotBlank() || !data.songName.isNullOrBlank() || !data.concertName.isNullOrBlank())) {
                    val actualSubType = if (data.subType.isNotBlank()) {
                        data.subType
                    } else when {
                        data.type.contains("song") -> "song"
                        data.type.contains("artist") -> "artist"
                        data.type.contains("concert") -> "concert"
                        data.type.contains("hires") -> "hires"
                        else -> "album"
                    }
                    return data.copy(subType = actualSubType)
                }
            } catch (_: Exception) {}
        }

        // 兼容自由文本或手动填写的冒号键值对
        return parseFromPlainText(trimmed)
    }

    private fun parseFromPlainText(text: String): ResourceAlignmentData? {
        var subType = "album"
        var artist = ""
        var album = ""
        var songs = ""
        var year: String? = null
        var desc: String? = null
        var songName: String? = null
        var genre: String? = null
        var concertName: String? = null
        var qualityIssue: String? = null
        var qualitySpec: String? = null

        when {
            text.contains("【歌曲补齐】") || text.contains("单曲补齐") -> subType = "song"
            text.contains("【歌手补齐】") -> subType = "artist"
            text.contains("【演唱会补齐】") || text.contains("现场补齐") -> subType = "concert"
            text.contains("【音质升级】") || text.contains("无损补齐") -> subType = "hires"
        }

        val lines = text.lines()
        for (line in lines) {
            val cleanLine = line.trim()
            when {
                cleanLine.startsWith("歌手") || cleanLine.startsWith("艺人") -> {
                    artist = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("专辑") -> {
                    album = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("单曲") || cleanLine.startsWith("歌曲名") -> {
                    songName = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("代表歌曲") || cleanLine.startsWith("歌曲") || cleanLine.startsWith("曲目") -> {
                    songs = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("流派") || cleanLine.startsWith("风格") -> {
                    genre = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("演唱会") || cleanLine.startsWith("巡演") -> {
                    concertName = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("音质问题") || cleanLine.startsWith("缺陷") -> {
                    qualityIssue = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("期望规格") || cleanLine.startsWith("期望音质") -> {
                    qualitySpec = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                cleanLine.startsWith("年份") || cleanLine.startsWith("发行年份") -> {
                    year = cleanLine.substringAfter("：").substringAfter(":").trim().takeIf { it.isNotBlank() }
                }
                cleanLine.startsWith("介绍") || cleanLine.startsWith("专辑介绍") || cleanLine.startsWith("备注") || cleanLine.startsWith("说明") -> {
                    desc = cleanLine.substringAfter("：").substringAfter(":").trim().takeIf { it.isNotBlank() }
                }
            }
        }

        if (artist.isNotBlank() || album.isNotBlank() || !songName.isNullOrBlank() || !concertName.isNullOrBlank()) {
            return ResourceAlignmentData(
                subType = subType,
                artist = artist,
                album = album,
                songs = songs,
                year = year,
                desc = desc,
                songName = songName,
                genre = genre,
                concertName = concertName,
                qualityIssue = qualityIssue,
                qualitySpec = qualitySpec
            )
        }
        return null
    }
}

