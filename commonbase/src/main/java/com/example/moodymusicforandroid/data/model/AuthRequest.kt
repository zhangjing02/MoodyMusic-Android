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

/**
 * 校验当前歌手名是否为无效的占位ID（例如 "db_74", "artist_12", "74" 或与 ID 相同）
 */
fun LibraryArtistItem.isInvalidName(): Boolean {
    if (name.isNullOrBlank()) return true
    val trimmed = name.trim()
    if (trimmed.startsWith("db_", ignoreCase = true) || trimmed.startsWith("artist_", ignoreCase = true)) return true
    val cleanId = artistId.trim().removePrefix("db_").removePrefix("artist_")
    val cleanName = trimmed.removePrefix("db_").removePrefix("artist_")
    if (cleanName.equals(cleanId, ignoreCase = true) || trimmed.equals(artistId.trim(), ignoreCase = true)) return true
    if (trimmed.all { it.isDigit() }) return true
    return false
}

/**
 * 获取可供展示的歌手名，如果是占位ID则返回 fallback (默认 "未知歌手")
 */
fun LibraryArtistItem.getDisplayName(fallback: String = "未知歌手"): String {
    return if (!isInvalidName()) name!!.trim() else fallback
}

/**
 * 使用全量歌手列表对当前歌手信息进行容错补全
 */
fun LibraryArtistItem.enrichedWith(artists: List<Artist>): LibraryArtistItem {
    val cleanId = artistId.trim().removePrefix("db_").removePrefix("artist_")
    val match = artists.find { it.id == artistId }
        ?: artists.find { it.id.trim().removePrefix("db_").removePrefix("artist_") == cleanId }
        ?: artists.find { it.name.trim() == cleanId }

    val realName = if (isInvalidName()) {
        match?.name?.takeIf { it.isNotBlank() } ?: name
    } else {
        name
    }

    val realAvatar = if (avatar.isNullOrBlank()) {
        match?.avatar?.takeIf { it.isNotBlank() } ?: avatar
    } else {
        avatar
    }

    return copy(name = realName, avatar = realAvatar)
}

/**
 * 校验专辑名称是否为无效占位ID（例如 "db_1808", "album_1808" 等）
 */
fun LibraryAlbumItem.isInvalidTitle(): Boolean {
    if (title.isNullOrBlank()) return true
    val trimmed = title.trim()
    if (trimmed.startsWith("db_", ignoreCase = true) || trimmed.startsWith("album_", ignoreCase = true)) return true
    val cleanId = albumId.trim().removePrefix("db_").removePrefix("album_")
    val cleanTitle = trimmed.removePrefix("db_").removePrefix("album_")
    if (cleanTitle.equals(cleanId, ignoreCase = true) || trimmed.equals(albumId.trim(), ignoreCase = true)) return true
    if (trimmed.all { it.isDigit() }) return true
    return false
}

fun LibraryAlbumItem.getDisplayTitle(fallback: String = "精选专辑"): String {
    return if (!isInvalidTitle()) title!!.trim() else fallback
}

