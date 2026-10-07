package com.example.moodymusicforandroid.data.local.db

import androidx.room.*

/**
 * 收藏单曲、收藏专辑、关注歌手统一本地 Room DAO
 * 支持游客模式 (userId = 0L) 与登录用户模式 (userId = 实际UID)
 */
@Dao
interface FavoriteDao {

    // ==================== 1. 歌曲收藏 (favorite_songs) ====================

    @Query("SELECT * FROM favorite_songs WHERE user_id = :userId ORDER BY added_at DESC")
    suspend fun getFavoriteSongs(userId: Long): List<FavoriteSongEntity>

    @Query("SELECT song_id FROM favorite_songs WHERE user_id = :userId")
    suspend fun getFavoriteSongIds(userId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM favorite_songs WHERE user_id = :userId")
    suspend fun getFavoriteSongsCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavoriteSong(entity: FavoriteSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavoriteSongs(entities: List<FavoriteSongEntity>)

    @Query("DELETE FROM favorite_songs WHERE user_id = :userId AND song_id = :songId")
    suspend fun deleteFavoriteSong(userId: Long, songId: Long)

    @Query("DELETE FROM favorite_songs WHERE user_id = :userId AND song_id IN (:songIds)")
    suspend fun deleteFavoriteSongs(userId: Long, songIds: List<Long>)

    @Query("DELETE FROM favorite_songs WHERE user_id = :userId")
    suspend fun clearFavoriteSongs(userId: Long)


    // ==================== 2. 专辑收藏 (favorite_albums) ====================

    @Query("SELECT * FROM favorite_albums WHERE user_id = :userId ORDER BY added_at DESC")
    suspend fun getFavoriteAlbums(userId: Long): List<FavoriteAlbumEntity>

    @Query("SELECT album_id FROM favorite_albums WHERE user_id = :userId")
    suspend fun getFavoriteAlbumIds(userId: Long): List<String>

    @Query("SELECT COUNT(*) FROM favorite_albums WHERE user_id = :userId")
    suspend fun getFavoriteAlbumsCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavoriteAlbum(entity: FavoriteAlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFavoriteAlbums(entities: List<FavoriteAlbumEntity>)

    @Query("DELETE FROM favorite_albums WHERE user_id = :userId AND album_id = :albumId")
    suspend fun deleteFavoriteAlbum(userId: Long, albumId: String)

    @Query("DELETE FROM favorite_albums WHERE user_id = :userId AND album_id IN (:albumIds)")
    suspend fun deleteFavoriteAlbums(userId: Long, albumIds: List<String>)

    @Query("DELETE FROM favorite_albums WHERE user_id = :userId")
    suspend fun clearFavoriteAlbums(userId: Long)


    // ==================== 3. 歌手关注 (followed_artists) ====================

    @Query("SELECT * FROM followed_artists WHERE user_id = :userId ORDER BY added_at DESC")
    suspend fun getFollowedArtists(userId: Long): List<FollowedArtistEntity>

    @Query("SELECT artist_id FROM followed_artists WHERE user_id = :userId")
    suspend fun getFollowedArtistIds(userId: Long): List<String>

    @Query("SELECT COUNT(*) FROM followed_artists WHERE user_id = :userId")
    suspend fun getFollowedArtistsCount(userId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFollowedArtist(entity: FollowedArtistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveFollowedArtists(entities: List<FollowedArtistEntity>)

    @Query("DELETE FROM followed_artists WHERE user_id = :userId AND artist_id = :artistId")
    suspend fun deleteFollowedArtist(userId: Long, artistId: String)

    @Query("DELETE FROM followed_artists WHERE user_id = :userId AND artist_id IN (:artistIds)")
    suspend fun deleteFollowedArtists(userId: Long, artistIds: List<String>)

    @Query("DELETE FROM followed_artists WHERE user_id = :userId")
    suspend fun clearFollowedArtists(userId: Long)


    // ==================== 4. 游客资产合并到登录用户 (Merge Guest Assets) ====================

    @Transaction
    suspend fun mergeGuestAssetsToUser(guestUserId: Long = 0L, targetUserId: Long) {
        if (targetUserId == guestUserId) return

        // 歌曲合并
        val guestSongs = getFavoriteSongs(guestUserId)
        if (guestSongs.isNotEmpty()) {
            val migratedSongs = guestSongs.map { it.copy(userId = targetUserId) }
            saveFavoriteSongs(migratedSongs)
            clearFavoriteSongs(guestUserId)
        }

        // 专辑合并
        val guestAlbums = getFavoriteAlbums(guestUserId)
        if (guestAlbums.isNotEmpty()) {
            val migratedAlbums = guestAlbums.map { it.copy(userId = targetUserId) }
            saveFavoriteAlbums(migratedAlbums)
            clearFavoriteAlbums(guestUserId)
        }

        // 歌手合并
        val guestArtists = getFollowedArtists(guestUserId)
        if (guestArtists.isNotEmpty()) {
            val migratedArtists = guestArtists.map { it.copy(userId = targetUserId) }
            saveFollowedArtists(migratedArtists)
            clearFollowedArtists(guestUserId)
        }
    }
}
