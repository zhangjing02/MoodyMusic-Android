package com.example.moodymusicforandroid.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.moodymusicforandroid.data.model.Playlist
import com.example.moodymusicforandroid.data.model.PlaylistSong

@Entity(tableName = "user_playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "theme_color")
    val themeColor: String = "DEFAULT",

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "song_count")
    val songCount: Int = 0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_playlist_songs",
    primaryKeys = ["playlist_id", "song_id"]
)
data class PlaylistSongEntity(
    @ColumnInfo(name = "playlist_id")
    val playlistId: Long,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String? = null,

    @ColumnInfo(name = "album_title")
    val albumTitle: String? = null,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "duration")
    val duration: Int = 0,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

fun PlaylistEntity.toPlaylist(): Playlist {
    return Playlist(
        id = id,
        userId = userId,
        name = name,
        description = description,
        themeColor = themeColor,
        coverUrl = coverUrl,
        songCount = songCount,
        updatedAt = updatedAt.toString()
    )
}

fun PlaylistSongEntity.toPlaylistSong(): PlaylistSong {
    return PlaylistSong(
        playlistId = playlistId,
        songId = songId,
        title = title,
        artistName = artistName,
        albumTitle = albumTitle,
        coverUrl = coverUrl,
        filePath = filePath,
        duration = duration,
        sortOrder = sortOrder,
        addedAt = addedAt.toString()
    )
}
