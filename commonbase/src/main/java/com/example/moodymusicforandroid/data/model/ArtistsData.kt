package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 艺人数据包装（骨架接口响应）
 */
data class ArtistsData(
    @SerializedName("artists")
    val artists: List<Artist> = emptyList()
)