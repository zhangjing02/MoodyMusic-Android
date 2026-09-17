package com.example.moodymusicforandroid.data.local.db

import com.example.moodymusicforandroid.data.model.User

/**
 * User（Gson DTO）↔ UserProfileEntity（Room Entity）双向转换
 * 保持两者字段同步，无需重复定义
 */

/**
 * 将服务器返回的 User DTO 转成 Room Entity 以持久化到本地数据库
 * token 和 refreshToken 单独传入（DTO 中可能为 null，但本地需要持久化登录态）
 */
fun User.toEntity(
    token: String? = this.token,
    refreshToken: String? = this.refreshToken
): UserProfileEntity = UserProfileEntity(
    userId = this.userId,
    username = this.username,
    nickname = this.nickname,
    avatarUrl = this.avatarUrl,
    email = this.email,
    bio = this.bio,
    playMode = this.getEffectivePlayMode(),
    cardClickDirectPlay = this.cardClickDirectPlay,
    fontScale = this.fontScale,
    themeMode = this.themeMode,
    cassetteStyle = this.getEffectiveCassetteStyle(),
    reservedStyle1 = this.reservedStyle1,
    reservedStyle2 = this.reservedStyle2,
    reservedPrefInt = this.reservedPrefInt,
    reservedPrefStr = this.reservedPrefStr,
    favoriteSongsCount = this.favoriteSongsCount,
    favoriteAlbumsCount = this.favoriteAlbumsCount,
    followedArtistsCount = this.followedArtistsCount,
    token = token,
    refreshToken = refreshToken,
    updatedAt = System.currentTimeMillis()
)

/**
 * 将本地 Room Entity 转成 User DTO 供 UI 层使用
 */
fun UserProfileEntity.toUser(): User = User(
    userId = this.userId,
    username = this.username,
    nickname = this.nickname,
    avatarUrl = this.avatarUrl,
    email = this.email,
    bio = this.bio,
    token = this.token,
    refreshToken = this.refreshToken,
    playMode = this.playMode,
    cardClickDirectPlay = this.cardClickDirectPlay,
    fontScale = this.fontScale,
    themeMode = this.themeMode,
    cassetteStyle = this.cassetteStyle,
    reservedStyle1 = this.reservedStyle1,
    reservedStyle2 = this.reservedStyle2,
    reservedPrefInt = this.reservedPrefInt,
    reservedPrefStr = this.reservedPrefStr,
    favoriteSongsCount = this.favoriteSongsCount,
    favoriteAlbumsCount = this.favoriteAlbumsCount,
    followedArtistsCount = this.followedArtistsCount
)

/**
 * 创建标准化的未登录/游客本地数据实体
 */
fun createDefaultGuestUser(
    cardClickDirectPlay: Boolean = true,
    fontScale: Float = 1.0f,
    themeMode: Int = 0,
    playMode: String = "SEQUENTIAL",
    cassetteStyle: String = "DEFAULT"
): User = User(
    userId = 0L,
    username = "guest",
    nickname = "未登录用户",
    bio = "在音信里听风的声音",
    playMode = playMode,
    cardClickDirectPlay = cardClickDirectPlay,
    fontScale = fontScale,
    themeMode = themeMode,
    cassetteStyle = cassetteStyle
)
