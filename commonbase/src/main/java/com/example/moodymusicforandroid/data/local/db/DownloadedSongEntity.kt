package com.example.moodymusicforandroid.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 离线下载歌曲实体
 * 主键：file_path（音频网络直链，全局唯一确定一首音频资源）
 */
@Entity(tableName = "downloaded_songs")
data class DownloadedSongEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "song_id")
    val songId: Long = 0L,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String = "",

    @ColumnInfo(name = "album_title")
    val albumTitle: String = "",

    @ColumnInfo(name = "cover_url")
    val coverUrl: String = "",

    @ColumnInfo(name = "lrc_path")
    val lrcPath: String? = null,

    @ColumnInfo(name = "file_hash")
    val fileHash: String = "",

    @ColumnInfo(name = "local_file_path")
    val localFilePath: String,

    @ColumnInfo(name = "local_lrc_path")
    val localLrcPath: String? = null,

    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0L,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis()
)
