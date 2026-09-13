package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 登录请求体
 */
data class LoginRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password_hash")
    val password: String
)

/**
 * 注册请求体
 */
data class RegisterRequest(
    @SerializedName("username")
    val username: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("nickname")
    val nickname: String? = null
)

/**
 * 刷新 Token 请求体
 */
data class RefreshTokenRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

/**
 * 登录响应的 data 字段
 * 后端: { user: {...}, token: "...", refresh_token: "..." }
 */
data class LoginData(
    @SerializedName("user")
    val user: User? = null,

    @SerializedName("token")
    val token: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null
)

/**
 * 发送邮箱验证码请求
 */
data class SendCodeRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("type")
    val type: String = "login" // "login" 或 "reset_password"
)

/**
 * 发送验证码响应
 */
data class SendCodeResponse(
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("debug_code")
    val debugCode: String? = null
)

/**
 * 邮箱验证码登录/一键注册请求
 */
data class VerifyCodeRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("password_hash")
    val passwordHash: String? = null,

    @SerializedName("nickname")
    val nickname: String? = null
)

/**
 * 邮箱验证码设置密码注册请求
 */
data class RegisterWithCodeRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("password_hash")
    val passwordHash: String,

    @SerializedName("username")
    val username: String? = null,

    @SerializedName("nickname")
    val nickname: String? = null
)

/**
 * 用户名/邮箱+密码登录请求
 */
data class PasswordLoginRequest(
    @SerializedName("account")
    val account: String,

    @SerializedName("password_hash")
    val passwordHash: String,

    @SerializedName("email")
    val email: String = account
)

/**
 * 用户名查重响应
 */
data class CheckUsernameResponse(
    @SerializedName("available")
    val available: Boolean = false,

    @SerializedName("message")
    val message: String? = null
)

/**
 * 验证码重置密码请求
 */
data class ResetPasswordRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("new_password_hash")
    val newPasswordHash: String
)

/**
 * 更新用户个人资料请求
 */
data class UpdateProfileRequest(
    @SerializedName("nickname")
    val nickname: String? = null,

    @SerializedName("avatar_url")
    val avatarUrl: String? = null,

    @SerializedName("bio")
    val bio: String? = null,

    @SerializedName("play_mode")
    val playMode: String? = null,

    @SerializedName("card_click_direct_play")
    val cardClickDirectPlay: Boolean? = null,

    @SerializedName("font_scale")
    val fontScale: Float? = null,

    @SerializedName("theme_mode")
    val themeMode: Int? = null,

    @SerializedName("cassette_style")
    val cassetteStyle: String? = null,

    @SerializedName("reserved_style_1")
    val reservedStyle1: String? = null,

    @SerializedName("reserved_style_2")
    val reservedStyle2: String? = null
)

/**
 * 收藏歌曲切换请求体
 */
data class FavoriteSongToggleRequest(
    @SerializedName("song_id")
    val songId: Long
)

/**
 * 收藏歌曲切换响应体
 */
data class FavoriteSongToggleResponse(
    @SerializedName("song_id")
    val songId: Long = 0,

    @SerializedName("is_favorited")
    val isFavorited: Boolean = false
)

/**
 * 用户音乐资产响应
 */
data class UserLibraryResponse(
    @SerializedName("favorite_albums")
    val favoriteAlbums: List<LibraryAlbumItem> = emptyList(),

    @SerializedName("followed_artists")
    val followedArtists: List<LibraryArtistItem> = emptyList(),

    @SerializedName("favorite_song_ids")
    val favoriteSongIds: List<Long> = emptyList(),

    @SerializedName("favorite_songs_count")
    val favoriteSongsCount: Int = 0,

    @SerializedName("favorite_albums_count")
    val favoriteAlbumsCount: Int = 0,

    @SerializedName("followed_artists_count")
    val followedArtistsCount: Int = 0
)

data class LibraryAlbumItem(
    @SerializedName("album_id")
    val albumId: String,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("cover")
    val cover: String? = null,

    @SerializedName("artist_id")
    val artistId: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class LibraryArtistItem(
    @SerializedName("artist_id")
    val artistId: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("created_at")
    val createdAt: String? = null
)

data class BatchRemoveRequest(
    @SerializedName("type")
    val type: String,

    @SerializedName("ids")
    val ids: List<Any>
)
