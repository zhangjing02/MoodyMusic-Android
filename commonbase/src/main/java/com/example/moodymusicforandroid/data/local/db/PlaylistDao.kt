package com.example.moodymusicforandroid.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM user_playlists WHERE user_id = :userId ORDER BY updated_at DESC, id DESC")
    fun getUserPlaylistsFlow(userId: Long): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM user_playlists WHERE user_id = :userId ORDER BY updated_at DESC, id DESC")
    suspend fun getUserPlaylists(userId: Long): List<PlaylistEntity>

    @Query("SELECT * FROM user_playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM user_playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM user_playlists WHERE user_id = :userId")
    suspend fun deletePlaylistsByUserId(userId: Long)

    @Query("SELECT * FROM user_playlist_songs WHERE playlist_id = :playlistId ORDER BY sort_order ASC, added_at DESC")
    fun getSongsByPlaylistIdFlow(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT * FROM user_playlist_songs WHERE playlist_id = :playlistId ORDER BY sort_order ASC, added_at DESC")
    suspend fun getSongsByPlaylistId(playlistId: Long): List<PlaylistSongEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaylistSong(song: PlaylistSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePlaylistSongs(songs: List<PlaylistSongEntity>)

    @Query("DELETE FROM user_playlist_songs WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun deletePlaylistSong(playlistId: Long, songId: Long)

    @Query("DELETE FROM user_playlist_songs WHERE playlist_id = :playlistId")
    suspend fun deleteSongsByPlaylistId(playlistId: Long)

    @Query("""
        SELECT s.playlist_id 
        FROM user_playlist_songs s
        INNER JOIN user_playlists p ON s.playlist_id = p.id
        WHERE p.user_id = :userId AND s.song_id = :songId
    """)
    suspend fun getPlaylistIdsForSong(userId: Long, songId: Long): List<Long>

    @Query("UPDATE user_playlists SET song_count = :count, updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistCount(playlistId: Long, count: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_playlists SET cover_url = :coverUrl, updated_at = :updatedAt WHERE id = :playlistId")
    suspend fun updatePlaylistCover(playlistId: Long, coverUrl: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE user_playlists SET user_id = :newUserId WHERE id = :playlistId")
    suspend fun updatePlaylistUserId(playlistId: Long, newUserId: Long)
}
