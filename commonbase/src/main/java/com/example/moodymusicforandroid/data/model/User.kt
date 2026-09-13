package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 用户信息
 */
data class User(
    @SerializedName("id")
    val userId: Long,

    @SerializedName("username")
    val username: String,

    @SerializedName("nickname")
    val nickname: String? = "",

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("bio")
    val bio: String? = "在音信里听风的声音",

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("favoriteCount")
    val favoriteCount: Int = 0,

    @SerializedName("followCount")
    val followCount: Int = 0,

    @SerializedName("favorite_songs_count")
    val favoriteSongsCount: Int = 0,

    @SerializedName("favorite_albums_count")
    val favoriteAlbumsCount: Int = 0,

    @SerializedName("followed_artists_count")
    val followedArtistsCount: Int = 0,

    @SerializedName("play_mode")
    val playMode: String? = "SEQUENTIAL",

    @SerializedName("card_click_direct_play")
    val cardClickDirectPlay: Boolean = true,

    @SerializedName("font_scale")
    val fontScale: Float = 1.0f,

    @SerializedName("theme_mode")
    val themeMode: Int = 0,

    @SerializedName("cassette_style")
    val cassetteStyle: String = "DEFAULT",

    @SerializedName("reserved_style_1")
    val reservedStyle1: String? = null,

    @SerializedName("reserved_style_2")
    val reservedStyle2: String? = null,

    @SerializedName("reserved_pref_int")
    val reservedPrefInt: Int = 0,

    @SerializedName("reserved_pref_str")
    val reservedPrefStr: String? = null
) {
    fun getEffectivePlayMode(): String = if (playMode.isNullOrBlank()) "SEQUENTIAL" else playMode
    fun getDisplayName(): String = nickname?.takeIf { it.isNotBlank() } ?: username
}
