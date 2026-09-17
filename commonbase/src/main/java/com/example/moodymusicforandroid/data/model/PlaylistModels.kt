package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户自定义播放列表模型
 */
data class Playlist(
    @SerializedName("id")
    val id: Long,

    @SerializedName("user_id")
    val userId: Long = 0L,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("theme_color")
    val themeColor: String = "DEFAULT",

    @SerializedName("cover_url")
    val coverUrl: String? = null,

    @SerializedName("song_count")
    val songCount: Int = 0,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * 播放列表内的歌曲模型
 */
data class PlaylistSong(
    @SerializedName("id")
    val id: Long = 0L,

    @SerializedName("playlist_id")
    val playlistId: Long,

    @SerializedName("song_id")
    val songId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("artist_name")
    val artistName: String? = null,

    @SerializedName("album_title")
    val albumTitle: String? = null,

    @SerializedName("cover_url")
    val coverUrl: String? = null,

    @SerializedName("file_path")
    val filePath: String? = null,

    @SerializedName("duration")
    val duration: Int = 0,

    @SerializedName("sort_order")
    val sortOrder: Int = 0,

    @SerializedName("added_at")
    val addedAt: String? = null
)

/**
 * 歌单详情返回包装
 */
data class PlaylistDetailData(
    @SerializedName("playlist")
    val playlist: Playlist,

    @SerializedName("songs")
    val songs: List<PlaylistSong> = emptyList()
)

/**
 * 创建歌单请求体
 */
data class CreatePlaylistRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("theme_color")
    val themeColor: String = "DEFAULT",

    @SerializedName("cover_url")
    val coverUrl: String? = null
)

/**
 * 更新歌单请求体
 */
data class UpdatePlaylistRequest(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("theme_color")
    val themeColor: String? = null
)

/**
 * 添加单曲到歌单请求体
 */
data class AddSongToPlaylistRequest(
    @SerializedName("song_id")
    val songId: Long,

    @SerializedName("title")
    val title: String,

    @SerializedName("artist_name")
    val artistName: String? = null,

    @SerializedName("album_title")
    val albumTitle: String? = null,

    @SerializedName("cover_url")
    val coverUrl: String? = null,

    @SerializedName("file_path")
    val filePath: String? = null,

    @SerializedName("duration")
    val duration: Int = 0
)

/**
 * 歌曲所属歌单列表返回
 */
data class SongMembershipsData(
    @SerializedName("song_id")
    val songId: Long,

    @SerializedName("playlist_ids")
    val playlistIds: List<Long> = emptyList()
)
