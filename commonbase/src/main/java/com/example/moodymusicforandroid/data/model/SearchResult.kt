package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 搜索结果
 */
data class SearchResult(
    @SerializedName("artists")
    val artists: List<Artist>? = null,
    @SerializedName("albums")
    val albums: List<Album>? = null,
    @SerializedName("songs")
    val songs: List<Song>? = null
)
