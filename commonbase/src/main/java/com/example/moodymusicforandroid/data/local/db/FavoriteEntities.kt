package com.example.moodymusicforandroid.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.data.model.LibraryAlbumItem
import com.example.moodymusicforandroid.data.model.LibraryArtistItem

/**
 * 收藏歌曲实体
 * 联合主键：[user_id, song_id]，未登录访客固定为 GUEST_USER_ID (0L)
 */
@Entity(
    tableName = "favorite_songs",
    primaryKeys = ["user_id", "song_id"]
)
data class FavoriteSongEntity(
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "song_id")
    val songId: Long,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "artist_name")
    val artistName: String? = null,

    @ColumnInfo(name = "artist_id")
    val artistId: String? = null,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "duration")
    val duration: Int? = null,

    @ColumnInfo(name = "album_title")
    val albumTitle: String? = null,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

fun FavoriteSongEntity.toFavoriteSong(): FavoriteSong {
    return FavoriteSong(
        songId = songId,
        favoritedAt = addedAt.toString(),
        title = title,
        filePath = filePath,
        coverUrl = coverUrl,
        duration = duration,
        albumTitle = albumTitle,
        artistName = artistName,
        artistId = artistId
    )
}

fun FavoriteSong.toEntity(userId: Long): FavoriteSongEntity {
    return FavoriteSongEntity(
        userId = userId,
        songId = songId,
        title = title,
        artistName = artistName,
        artistId = artistId,
        coverUrl = coverUrl,
        filePath = filePath,
        duration = duration,
        albumTitle = albumTitle,
        addedAt = favoritedAt?.toLongOrNull() ?: System.currentTimeMillis()
    )
}

/**
 * 收藏专辑实体
 * 联合主键：[user_id, album_id]，未登录访客固定为 GUEST_USER_ID (0L)
 */
@Entity(
    tableName = "favorite_albums",
    primaryKeys = ["user_id", "album_id"]
)
data class FavoriteAlbumEntity(
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "album_id")
    val albumId: String,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "cover")
    val cover: String? = null,

    @ColumnInfo(name = "artist_id")
    val artistId: String? = null,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

fun FavoriteAlbumEntity.toLibraryAlbumItem(): LibraryAlbumItem {
    return LibraryAlbumItem(
        albumId = albumId,
        title = title,
        cover = cover,
        artistId = artistId,
        createdAt = addedAt.toString()
    )
}

fun LibraryAlbumItem.toEntity(userId: Long): FavoriteAlbumEntity {
    return FavoriteAlbumEntity(
        userId = userId,
        albumId = albumId,
        title = title,
        cover = cover,
        artistId = artistId,
        addedAt = createdAt?.toLongOrNull() ?: System.currentTimeMillis()
    )
}

/**
 * 关注歌手实体
 * 联合主键：[user_id, artist_id]，未登录访客固定为 GUEST_USER_ID (0L)
 */
@Entity(
    tableName = "followed_artists",
    primaryKeys = ["user_id", "artist_id"]
)
data class FollowedArtistEntity(
    @ColumnInfo(name = "user_id")
    val userId: Long,

    @ColumnInfo(name = "artist_id")
    val artistId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "avatar")
    val avatar: String? = null,

    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)

fun FollowedArtistEntity.toLibraryArtistItem(): LibraryArtistItem {
    return LibraryArtistItem(
        artistId = artistId,
        name = name,
        avatar = avatar,
        createdAt = addedAt.toString()
    )
}

fun LibraryArtistItem.toEntity(userId: Long): FollowedArtistEntity {
    return FollowedArtistEntity(
        userId = userId,
        artistId = artistId,
        name = name ?: "未知歌手",
        avatar = avatar,
        addedAt = createdAt?.toLongOrNull() ?: System.currentTimeMillis()
    )
}
