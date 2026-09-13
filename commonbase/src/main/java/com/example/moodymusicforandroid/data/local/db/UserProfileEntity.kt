package com.example.moodymusicforandroid.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 用户资料实体（单表设计）
 * 字段与服务器 /api/user/profile 返回的 JSON 字段对应，
 * 结合 @ColumnInfo 注解方便直接映射，避免阻抗失配。
 * 本地仅持久化此一张表；收藏歌曲/专辑/关注歌手均通过服务器查询。
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    /**
     * 服务器 user.id（长整型）
     */
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "nickname")
    val nickname: String? = null,

    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String? = null,

    @ColumnInfo(name = "email")
    val email: String? = null,

    @ColumnInfo(name = "bio")
    val bio: String? = null,

    /** 播放模式: SEQUENTIAL / LIST_LOOP / SINGLE_LOOP / SHUFFLE */
    @ColumnInfo(name = "play_mode")
    val playMode: String = "SEQUENTIAL",

    /** 卡片点击播放偏好：true 直接播放，false 进入详情不直接播放 */
    @ColumnInfo(name = "card_click_direct_play")
    val cardClickDirectPlay: Boolean = true,

    /** 字体显示大小比例：0.85f 小号, 1.0f 标准, 1.15f 大号, 1.30f 超大号 */
    @ColumnInfo(name = "font_scale")
    val fontScale: Float = 1.0f,

    /** 主题模式：0 默认绿, 1 海洋蓝, 2 日落橙, 3 暗夜紫 */
    @ColumnInfo(name = "theme_mode")
    val themeMode: Int = 0,

    /** 播放详情页卡带/磁带样式 (预留字段，默认 DEFAULT) */
    @ColumnInfo(name = "cassette_style")
    val cassetteStyle: String = "DEFAULT",

    /** 预留样式扩展字段 1 (如黑胶盘面贴纸/风格主题) */
    @ColumnInfo(name = "reserved_style_1")
    val reservedStyle1: String? = null,

    /** 预留样式扩展字段 2 (如播放界面微光动效风格) */
    @ColumnInfo(name = "reserved_style_2")
    val reservedStyle2: String? = null,

    /** 预留整型偏好 */
    @ColumnInfo(name = "reserved_pref_int")
    val reservedPrefInt: Int = 0,

    /** 预留字符串偏好 */
    @ColumnInfo(name = "reserved_pref_str")
    val reservedPrefStr: String? = null,

    @ColumnInfo(name = "favorite_songs_count")
    val favoriteSongsCount: Int = 0,

    @ColumnInfo(name = "favorite_albums_count")
    val favoriteAlbumsCount: Int = 0,

    @ColumnInfo(name = "followed_artists_count")
    val followedArtistsCount: Int = 0,

    /** 会话 Token（本地存储，不上传服务器） */
    @ColumnInfo(name = "token")
    val token: String? = null,

    /** 刷新 Token（本地存储） */
    @ColumnInfo(name = "refresh_token")
    val refreshToken: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L
)
