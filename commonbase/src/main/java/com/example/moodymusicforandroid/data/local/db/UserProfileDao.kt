package com.example.moodymusicforandroid.data.local.db

import androidx.room.*

/**
 * Room DAO — 用户资料单表的所有数据库操作
 */
@Dao
interface UserProfileDao {

    /** 读取本地唯一一条用户资料（LIMIT 1） */
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    /** 插入或覆盖更新用户资料 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(entity: UserProfileEntity)

    /** 仅更新播放模式字段（高频写入优化） */
    @Query("UPDATE user_profile SET play_mode = :mode WHERE user_id = :userId")
    suspend fun updatePlayMode(userId: Long, mode: String)

    /** 更新卡片播放偏好（直接播放 / 不直接播放） */
    @Query("UPDATE user_profile SET card_click_direct_play = :enabled WHERE user_id = :userId")
    suspend fun updateCardClickDirectPlay(userId: Long, enabled: Boolean)

    /** 更新字体显示大小比例 */
    @Query("UPDATE user_profile SET font_scale = :scale WHERE user_id = :userId")
    suspend fun updateFontScale(userId: Long, scale: Float)

    /** 更新主题色彩模式 */
    @Query("UPDATE user_profile SET theme_mode = :mode WHERE user_id = :userId")
    suspend fun updateThemeMode(userId: Long, mode: Int)

    /** 更新播放详情页卡带/磁带样式 */
    @Query("UPDATE user_profile SET cassette_style = :style WHERE user_id = :userId")
    suspend fun updateCassetteStyle(userId: Long, style: String)

    /** 更新预留扩展样式 1 */
    @Query("UPDATE user_profile SET reserved_style_1 = :style WHERE user_id = :userId")
    suspend fun updateReservedStyle1(userId: Long, style: String?)

    /** 更新预留扩展样式 2 */
    @Query("UPDATE user_profile SET reserved_style_2 = :style WHERE user_id = :userId")
    suspend fun updateReservedStyle2(userId: Long, style: String?)

    /** 根据用户 ID 获取资料 */
    @Query("SELECT * FROM user_profile WHERE user_id = :userId LIMIT 1")
    suspend fun getUserProfileById(userId: Long): UserProfileEntity?

    /** 清理游客临时记录 (user_id = 0) */
    @Query("DELETE FROM user_profile WHERE user_id = 0")
    suspend fun deleteGuestProfile()

    /** 退出登录：清空整张表 */
    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}
