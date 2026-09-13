package com.example.moodymusicforandroid.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.moodymusicforandroid.data.model.LibraryAlbumItem
import com.example.moodymusicforandroid.data.model.LibraryArtistItem
import com.example.moodymusicforandroid.data.model.User

/**
 * Android 本地原生 SQLite 数据库助手类 (MoodyUserDbHelper)
 * 作为客户端用户个人信息与偏好的单一可信源 (Single Source of Truth)
 */
class MoodyUserDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "moody_user.db"
        const val DATABASE_VERSION = 1

        // 表名
        const val TABLE_USER_PROFILE = "user_profile"
        const val TABLE_FAVORITE_SONGS = "user_favorite_songs"
        const val TABLE_FAVORITE_ALBUMS = "user_favorite_albums"
        const val TABLE_FOLLOWED_ARTISTS = "user_followed_artists"
        const val TABLE_USER_PREFERENCES = "user_preferences"

        // 列名 - 用户资料
        const val COL_USER_ID = "user_id"
        const val COL_USERNAME = "username"
        const val COL_NICKNAME = "nickname"
        const val COL_EMAIL = "email"
        const val COL_BIO = "bio"
        const val COL_AVATAR_URL = "avatar_url"
        const val COL_PLAY_MODE = "play_mode"
        const val COL_FAV_SONGS_COUNT = "favorite_songs_count"
        const val COL_FAV_ALBUMS_COUNT = "favorite_albums_count"
        const val COL_FOLLOWED_ARTISTS_COUNT = "followed_artists_count"
        const val COL_UPDATED_AT = "updated_at"

        // 列名 - 偏好表
        const val COL_PREF_KEY = "pref_key"
        const val COL_PREF_VALUE = "pref_val"

        @Volatile
        private var instance: MoodyUserDbHelper? = null

        fun getInstance(context: Context): MoodyUserDbHelper {
            return instance ?: synchronized(this) {
                instance ?: MoodyUserDbHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. 用户资料表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_PROFILE (
                $COL_USER_ID INTEGER PRIMARY KEY,
                $COL_USERNAME TEXT NOT NULL,
                $COL_NICKNAME TEXT,
                $COL_EMAIL TEXT,
                $COL_BIO TEXT,
                $COL_AVATAR_URL TEXT,
                $COL_PLAY_MODE TEXT DEFAULT 'SEQUENTIAL',
                $COL_FAV_SONGS_COUNT INTEGER DEFAULT 0,
                $COL_FAV_ALBUMS_COUNT INTEGER DEFAULT 0,
                $COL_FOLLOWED_ARTISTS_COUNT INTEGER DEFAULT 0,
                $COL_UPDATED_AT INTEGER
            )
            """.trimIndent()
        )

        // 2. 收藏歌曲表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITE_SONGS (
                song_id INTEGER PRIMARY KEY,
                title TEXT,
                artist TEXT,
                cover_url TEXT,
                audio_url TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // 3. 收藏专辑表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FAVORITE_ALBUMS (
                album_id TEXT PRIMARY KEY,
                title TEXT,
                cover TEXT,
                artist_id TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // 4. 关注歌手表
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FOLLOWED_ARTISTS (
                artist_id TEXT PRIMARY KEY,
                name TEXT,
                avatar TEXT,
                created_at INTEGER
            )
            """.trimIndent()
        )

        // 5. 偏好设置表（用于游客或全局持久化键值）
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_PREFERENCES (
                $COL_PREF_KEY TEXT PRIMARY KEY,
                $COL_PREF_VALUE TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 后续升级迁移处理
    }

    // ==================== 用户资料操作 ====================

    fun saveUserProfile(user: User) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, user.userId)
            put(COL_USERNAME, user.username)
            put(COL_NICKNAME, user.nickname)
            put(COL_EMAIL, user.email)
            put(COL_BIO, user.bio)
            put(COL_AVATAR_URL, user.avatarUrl)
            put(COL_PLAY_MODE, user.getEffectivePlayMode())
            put(COL_FAV_SONGS_COUNT, user.favoriteSongsCount)
            put(COL_FAV_ALBUMS_COUNT, user.favoriteAlbumsCount)
            put(COL_FOLLOWED_ARTISTS_COUNT, user.followedArtistsCount)
            put(COL_UPDATED_AT, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_USER_PROFILE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserProfile(): User? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_USER_PROFILE LIMIT 1", null)
        return cursor.use {
            if (it.moveToFirst()) {
                val userId = it.getLong(it.getColumnIndexOrThrow(COL_USER_ID))
                val username = it.getString(it.getColumnIndexOrThrow(COL_USERNAME))
                val nickname = it.getString(it.getColumnIndexOrThrow(COL_NICKNAME)) ?: ""
                val email = it.getString(it.getColumnIndexOrThrow(COL_EMAIL))
                val bio = it.getString(it.getColumnIndexOrThrow(COL_BIO)) ?: "在音信里听风的声音"
                val avatarUrl = it.getString(it.getColumnIndexOrThrow(COL_AVATAR_URL))
                val playMode = it.getString(it.getColumnIndexOrThrow(COL_PLAY_MODE)) ?: "SEQUENTIAL"
                val favSongsCount = it.getInt(it.getColumnIndexOrThrow(COL_FAV_SONGS_COUNT))
                val favAlbumsCount = it.getInt(it.getColumnIndexOrThrow(COL_FAV_ALBUMS_COUNT))
                val followedArtistsCount = it.getInt(it.getColumnIndexOrThrow(COL_FOLLOWED_ARTISTS_COUNT))

                User(
                    userId = userId,
                    username = username,
                    nickname = nickname,
                    avatarUrl = avatarUrl,
                    email = email,
                    bio = bio,
                    playMode = playMode,
                    favoriteSongsCount = favSongsCount,
                    favoriteAlbumsCount = favAlbumsCount,
                    followedArtistsCount = followedArtistsCount
                )
            } else {
                null
            }
        }
    }

    fun clearUserProfile() {
        val db = writableDatabase
        db.delete(TABLE_USER_PROFILE, null, null)
    }

    // ==================== 偏好设置操作 ====================

    fun setPreference(key: String, value: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PREF_KEY, key)
            put(COL_PREF_VALUE, value)
        }
        db.insertWithOnConflict(TABLE_USER_PREFERENCES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPreference(key: String, defaultValue: String? = null): String? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_PREF_VALUE FROM $TABLE_USER_PREFERENCES WHERE $COL_PREF_KEY = ? LIMIT 1", arrayOf(key))
        return cursor.use {
            if (it.moveToFirst()) {
                it.getString(0) ?: defaultValue
            } else {
                defaultValue
            }
        }
    }

    // ==================== 收藏歌曲操作 ====================

    fun replaceFavoriteSongIds(songIds: Collection<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FAVORITE_SONGS, null, null)
            val stmt = db.compileStatement("INSERT OR REPLACE INTO $TABLE_FAVORITE_SONGS (song_id, created_at) VALUES (?, ?)")
            val now = System.currentTimeMillis()
            for (id in songIds) {
                stmt.bindLong(1, id)
                stmt.bindLong(2, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addFavoriteSong(songId: Long, title: String? = null, artist: String? = null, coverUrl: String? = null, audioUrl: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("song_id", songId)
            put("title", title)
            put("artist", artist)
            put("cover_url", coverUrl)
            put("audio_url", audioUrl)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_FAVORITE_SONGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFavoriteSong(songId: Long) {
        val db = writableDatabase
        db.delete(TABLE_FAVORITE_SONGS, "song_id = ?", arrayOf(songId.toString()))
    }

    fun getFavoriteSongIds(): Set<Long> {
        val db = readableDatabase
        val set = mutableSetOf<Long>()
        val cursor = db.rawQuery("SELECT song_id FROM $TABLE_FAVORITE_SONGS", null)
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getLong(0))
            }
        }
        return set
    }

    fun isSongFavorite(songId: Long): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_FAVORITE_SONGS WHERE song_id = ? LIMIT 1", arrayOf(songId.toString()))
        return cursor.use { it.count > 0 }
    }

    // ==================== 收藏专辑操作 ====================

    fun replaceFavoriteAlbums(albums: Collection<LibraryAlbumItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FAVORITE_ALBUMS, null, null)
            val stmt = db.compileStatement("INSERT OR REPLACE INTO $TABLE_FAVORITE_ALBUMS (album_id, title, cover, artist_id, created_at) VALUES (?, ?, ?, ?, ?)")
            val now = System.currentTimeMillis()
            for (album in albums) {
                stmt.bindString(1, album.albumId)
                stmt.bindString(2, album.title ?: "")
                stmt.bindString(3, album.cover ?: "")
                stmt.bindString(4, album.artistId ?: "")
                stmt.bindLong(5, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addFavoriteAlbum(albumId: String, title: String? = null, cover: String? = null, artistId: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("album_id", albumId)
            put("title", title)
            put("cover", cover)
            put("artist_id", artistId)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_FAVORITE_ALBUMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFavoriteAlbum(albumId: String) {
        val db = writableDatabase
        db.delete(TABLE_FAVORITE_ALBUMS, "album_id = ?", arrayOf(albumId))
    }

    fun getFavoriteAlbumIds(): Set<String> {
        val db = readableDatabase
        val set = mutableSetOf<String>()
        val cursor = db.rawQuery("SELECT album_id FROM $TABLE_FAVORITE_ALBUMS", null)
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getString(0))
            }
        }
        return set
    }

    fun isAlbumFavorite(albumId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_FAVORITE_ALBUMS WHERE album_id = ? LIMIT 1", arrayOf(albumId))
        return cursor.use { it.count > 0 }
    }

    // ==================== 关注歌手操作 ====================

    fun replaceFollowedArtists(artists: Collection<LibraryArtistItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FOLLOWED_ARTISTS, null, null)
            val stmt = db.compileStatement("INSERT OR REPLACE INTO $TABLE_FOLLOWED_ARTISTS (artist_id, name, avatar, created_at) VALUES (?, ?, ?, ?)")
            val now = System.currentTimeMillis()
            for (artist in artists) {
                stmt.bindString(1, artist.artistId)
                stmt.bindString(2, artist.name ?: "")
                stmt.bindString(3, artist.avatar ?: "")
                stmt.bindLong(4, now)
                stmt.executeInsert()
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun addFollowedArtist(artistId: String, name: String? = null, avatar: String? = null) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("artist_id", artistId)
            put("name", name)
            put("avatar", avatar)
            put("created_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_FOLLOWED_ARTISTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFollowedArtist(artistId: String) {
        val db = writableDatabase
        db.delete(TABLE_FOLLOWED_ARTISTS, "artist_id = ?", arrayOf(artistId))
    }

    fun getFollowedArtistIds(): Set<String> {
        val db = readableDatabase
        val set = mutableSetOf<String>()
        val cursor = db.rawQuery("SELECT artist_id FROM $TABLE_FOLLOWED_ARTISTS", null)
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getString(0))
            }
        }
        return set
    }

    fun isArtistFollowed(artistId: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_FOLLOWED_ARTISTS WHERE artist_id = ? LIMIT 1", arrayOf(artistId))
        return cursor.use { it.count > 0 }
    }

    // ==================== 退出登录一键清理用户数据 ====================

    fun clearAllUserData() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_USER_PROFILE, null, null)
            db.delete(TABLE_FAVORITE_SONGS, null, null)
            db.delete(TABLE_FAVORITE_ALBUMS, null, null)
            db.delete(TABLE_FOLLOWED_ARTISTS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
