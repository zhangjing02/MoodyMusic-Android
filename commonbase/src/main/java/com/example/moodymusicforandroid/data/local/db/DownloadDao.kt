package com.example.moodymusicforandroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 离线下载歌曲数据访问接口 (DownloadDao)
 */
@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloaded_songs ORDER BY downloaded_at DESC")
    fun getAllDownloadedSongs(): Flow<List<DownloadedSongEntity>>

    @Query("SELECT * FROM downloaded_songs ORDER BY downloaded_at DESC")
    suspend fun getAllDownloadedSongsList(): List<DownloadedSongEntity>

    @Query("SELECT * FROM downloaded_songs WHERE file_path = :filePath LIMIT 1")
    suspend fun getDownloadedSongByPath(filePath: String): DownloadedSongEntity?

    @Query("SELECT * FROM downloaded_songs WHERE file_path IN (:filePaths)")
    suspend fun getDownloadedSongsByPaths(filePaths: List<String>): List<DownloadedSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: DownloadedSongEntity)

    @Query("DELETE FROM downloaded_songs WHERE file_path = :filePath")
    suspend fun deleteByFilePath(filePath: String)

    @Query("DELETE FROM downloaded_songs")
    suspend fun deleteAll()

    @Query("SELECT SUM(file_size) FROM downloaded_songs")
    fun getTotalStorageBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM downloaded_songs")
    fun getDownloadedCount(): Flow<Int>
}
