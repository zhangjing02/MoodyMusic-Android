package com.example.moodymusicforandroid.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 艺人名录数据访问接口 (ArtistDao)
 */
@Dao
interface ArtistDao {

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getAllArtistsFlow(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    suspend fun getAllArtists(): List<ArtistEntity>

    @Query("SELECT * FROM artists ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomArtists(limit: Int): List<ArtistEntity>

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun getArtistCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists")
    suspend fun clearAll()
}
