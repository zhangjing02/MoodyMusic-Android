package com.example.moodymusicforandroid.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 数据库单例
 * 版本 2 — 整合应用设置与预留卡带样式字段
 * 数据库文件名 moody_room.db（与旧版 moody_user.db 区分，避免迁移冲突）
 */
@Database(
    entities = [UserProfileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class MoodyDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: MoodyDatabase? = null

        fun getInstance(context: Context): MoodyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MoodyDatabase::class.java,
                    "moody_room.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
