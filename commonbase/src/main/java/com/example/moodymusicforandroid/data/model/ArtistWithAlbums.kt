package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * /api/songs 接口返回的单首歌曲条目
 */
data class SongItem(
    @SerializedName("title")
    val title: String,

    @SerializedName("path")
    val path: String? = null,

    @SerializedName("lrc_path")
    val lrcPath: String? = null,

    @SerializedName("TrackIndex")
    val trackIndex: Int? = null
)

/**
 * /api/songs 接口返回的专辑（含歌曲列表）
 */
data class AlbumWithSongs(
    @SerializedName("title")
    val title: String,

    @SerializedName("year")
    val year: String = "",

    @SerializedName("cover")
    val cover: String = "",

    @SerializedName("songs")
    val songs: List<SongItem> = emptyList()
)

/**
 * /api/songs 接口返回的艺人（含专辑+歌曲树）
 */
data class ArtistWithAlbums(
    @SerializedName("id")
    val id: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("category")
    val category: String? = null,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("group")
    val group: String? = null,

    @SerializedName("albums")
    val albums: List<AlbumWithSongs> = emptyList()
)
