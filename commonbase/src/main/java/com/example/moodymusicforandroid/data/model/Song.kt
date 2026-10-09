package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 歌曲信息
 */
data class Song(
    @SerializedName("id")
    val id: Long,

    @SerializedName(value = "album_id", alternate = ["Album_ID", "albumId"])
    val albumId: Long = 0,

    @SerializedName(value = "artist_id", alternate = ["ArtistID", "artistId"])
    val artistId: Long? = null,

    @SerializedName("title")
    val title: String,

    @SerializedName(value = "file_path", alternate = ["FilePath", "path"])
    val filePath: String? = null,

    @SerializedName(value = "lrc_path", alternate = ["LrcPath", "lrc"])
    val lrcPath: String? = null,

    @SerializedName("track_index")
    val trackIndex: Int? = null,

    @SerializedName("duration")
    val duration: Int? = null,

    @SerializedName("cover_url")
    val coverUrl: String? = null,

    // 扩展字段
    var artistName: String? = null,
    var albumTitle: String? = null,
    var audioUrl: String? = null,

    @SerializedName(value = "file_hash", alternate = ["fileHash"])
    var fileHash: String? = null
)
