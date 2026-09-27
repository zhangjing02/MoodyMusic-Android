package com.example.moodymusicforandroid.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.moodymusicforandroid.data.model.Artist

/**
 * Room 艺人名录骨架实体
 * 字段与服务端 /api/skeleton 接口返回内容对齐，
 * 支持本地离线秒开展示与随心漫游零堆内存常驻随机选取。
 */
@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_char")
    val group: String? = null,

    @ColumnInfo(name = "category")
    val category: String? = null,

    @ColumnInfo(name = "avatar")
    val avatar: String? = null,

    @ColumnInfo(name = "album_count")
    val albumCount: Int = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 转换扩展函数：Entity -> Domain Model
 */
fun ArtistEntity.toArtist(): Artist {
    return Artist(
        id = id,
        name = name,
        group = group,
        category = category,
        avatar = avatar,
        albumCount = albumCount
    )
}

/**
 * 转换扩展函数：Domain Model -> Entity
 */
fun Artist.toEntity(): ArtistEntity {
    return ArtistEntity(
        id = id,
        name = name,
        group = group,
        category = category,
        avatar = avatar,
        albumCount = albumCount
    )
}
