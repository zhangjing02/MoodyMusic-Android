package com.example.moodymusicforandroid.data.manager

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.common.utils.ThemeManager
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.local.db.MoodyDatabase
import com.example.moodymusicforandroid.data.local.db.createDefaultGuestUser
import com.example.moodymusicforandroid.data.local.db.toEntity
import com.example.moodymusicforandroid.data.local.db.toUser
import com.example.moodymusicforandroid.data.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局用户管理中枢 (UserManager)
 * 采用 Single Source of Truth（单一可信源）模式：
 * - 本地持久化：Room 数据库（单张 user_profile 表，支持游客/登录态）
 * - 远端同步：/api/user/profile + /api/user/library
 * - 全局分发：StateFlow（响应式，零轮询）
 *
 * 收藏歌曲 / 收藏专辑 / 关注歌手：不在本地持久化，仅在内存中维护 ID 集合，
 * 每次从服务器 /api/user/library 全量刷新。
 */
object UserManager {

    private const val TAG = "UserManager"
    const val GUEST_USER_ID = 0L
    const val DEFAULT_PLAY_MODE = "SEQUENTIAL"
    const val DEFAULT_CASSETTE_STYLE = "DEFAULT"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Room DAO（在 init() 中懒初始化）
    private var db: MoodyDatabase? = null
    private val dao get() = db?.userProfileDao()

    // ==================== 全局响应式状态 (StateFlow) ====================

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _playMode = MutableStateFlow(DEFAULT_PLAY_MODE)
    val playMode: StateFlow<String> = _playMode.asStateFlow()

    // 设置项响应式状态（全应用统一订阅自 Room / UserManager）
    private val _cardClickDirectPlay = MutableStateFlow(true)
    val cardClickDirectPlay: StateFlow<Boolean> = _cardClickDirectPlay.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _themeMode = MutableStateFlow(0)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _cassetteStyle = MutableStateFlow(DEFAULT_CASSETTE_STYLE)
    val cassetteStyle: StateFlow<String> = _cassetteStyle.asStateFlow()

    private val _favoriteSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val favoriteSongIds: StateFlow<Set<Long>> = _favoriteSongIds.asStateFlow()

    private val _favoriteSongsList = MutableStateFlow<List<FavoriteSong>>(emptyList())
    val favoriteSongsList: StateFlow<List<FavoriteSong>> = _favoriteSongsList.asStateFlow()

    private val _favoriteAlbumIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteAlbumIds: StateFlow<Set<String>> = _favoriteAlbumIds.asStateFlow()

    private val _followedArtistIds = MutableStateFlow<Set<String>>(emptySet())
    val followedArtistIds: StateFlow<Set<String>> = _followedArtistIds.asStateFlow()

    /**
     * 初始化，在 Application.onCreate() 中调用。
     * 1. 立即从 SharedPreferences 恢复冷启状态（0ms 开屏）
     * 2. 异步从 Room 加载用户数据（未登录则加载或新建 GUEST_USER_ID 数据）
     * 3. 若已登录，静默从服务器同步最新数据
     */
    fun init(context: Context) {
        db = MoodyDatabase.getInstance(context)

        val loggedIn = PreferencesManager.isLoggedIn()
        _isLoggedIn.value = loggedIn

        // 从 SharedPreferences 即时恢复保底状态（用于冷启 0ms 开屏）
        val spMode = PreferencesManager.getPlayMode()
        _playMode.value = if (spMode.isNotBlank()) spMode else DEFAULT_PLAY_MODE
        _cardClickDirectPlay.value = PreferencesManager.isCardClickDirectPlay()
        _fontScale.value = PreferencesManager.getFontScale()
        _themeMode.value = ThemeManager.getTheme(context).value
        _cassetteStyle.value = PreferencesManager.getString("cassette_style", DEFAULT_CASSETTE_STYLE) ?: DEFAULT_CASSETTE_STYLE

        // 恢复本地缓存的已收藏歌曲列表
        val cachedSongsJson = PreferencesManager.getString("KEY_CACHED_FAVORITE_SONGS")
        if (!cachedSongsJson.isNullOrBlank()) {
            try {
                val type = object : TypeToken<List<FavoriteSong>>() {}.type
                val list: List<FavoriteSong> = Gson().fromJson(cachedSongsJson, type)
                _favoriteSongsList.value = list
                _favoriteSongIds.value = list.map { it.songId }.toSet()
            } catch (_: Exception) {}
        }

        scope.launch {
            if (loggedIn) {
                // 从 Room 秒开恢复真实用户数据
                val cachedEntity = dao?.getUserProfile()
                if (cachedEntity != null && cachedEntity.userId != GUEST_USER_ID) {
                    val cachedUser = cachedEntity.toUser()
                    _userProfile.value = cachedUser
                    val mode = cachedUser.getEffectivePlayMode()
                    if (mode.isNotBlank()) _playMode.value = mode
                    _cardClickDirectPlay.value = cachedUser.cardClickDirectPlay
                    _fontScale.value = cachedUser.fontScale
                    _themeMode.value = cachedUser.themeMode
                    _cassetteStyle.value = cachedUser.cassetteStyle
                }
                // 静默从服务器拉取最新数据覆盖
                syncFromServer()
            } else {
                // 游客模式：从 Room 读取 GUEST_USER_ID 实体
                val guestEntity = dao?.getUserProfileById(GUEST_USER_ID) ?: dao?.getUserProfile()
                if (guestEntity != null) {
                    val guestUser = guestEntity.toUser()
                    _userProfile.value = guestUser
                    val mode = guestUser.getEffectivePlayMode()
                    if (mode.isNotBlank()) _playMode.value = mode
                    _cardClickDirectPlay.value = guestUser.cardClickDirectPlay
                    _fontScale.value = guestUser.fontScale
                    _themeMode.value = guestUser.themeMode
                    _cassetteStyle.value = guestUser.cassetteStyle
                } else {
                    // 若无游客记录，生成默认游客 Profile 写入 Room
                    val defaultGuest = createDefaultGuestUser(
                        cardClickDirectPlay = _cardClickDirectPlay.value,
                        fontScale = _fontScale.value,
                        themeMode = _themeMode.value,
                        playMode = _playMode.value,
                        cassetteStyle = _cassetteStyle.value
                    )
                    _userProfile.value = defaultGuest
                    dao?.saveUserProfile(defaultGuest.toEntity())
                }
                _favoriteSongIds.value = emptySet()
                _favoriteSongsList.value = emptyList()
                _favoriteAlbumIds.value = emptySet()
                _followedArtistIds.value = emptySet()
            }
        }
    }

    /**
     * 获取当前生效的设置与偏好
     */
    fun getCurrentPlayMode(): String = _playMode.value
    fun isCardClickDirectPlay(): Boolean = _cardClickDirectPlay.value
    fun getFontScale(): Float = _fontScale.value
    fun getThemeMode(): Int = _themeMode.value
    fun getCassetteStyle(): String = _cassetteStyle.value

    /**
     * 切换播放模式
     * 1. 立即乐观更新内存 StateFlow
     * 2. 写入 Room（更新 play_mode 字段）
     * 3. 写入 SharedPreferences（保底恢复）
     * 4. 若已登录，异步同步到云端 D1
     */
    fun updatePlayMode(mode: String) {
        val targetMode = if (mode.isNotBlank()) mode else DEFAULT_PLAY_MODE
        _playMode.value = targetMode
        PreferencesManager.savePlayMode(targetMode)

        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(playMode = targetMode)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            dao?.updatePlayMode(uid, targetMode)
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(playMode = targetMode)
                    )
                    Log.d(TAG, "PlayMode $targetMode successfully synced to server")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync playMode to server (saved locally)", e)
                }
            }
        }
    }

    /**
     * 切换首页卡片播放偏好（点击直接播放 / 不直接播放）
     */
    fun updateCardClickDirectPlay(enabled: Boolean) {
        _cardClickDirectPlay.value = enabled
        PreferencesManager.saveCardClickDirectPlay(enabled)

        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(cardClickDirectPlay = enabled)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            val existing = dao?.getUserProfileById(uid)
            if (existing != null) {
                dao?.updateCardClickDirectPlay(uid, enabled)
            } else {
                val newUser = (_userProfile.value ?: createDefaultGuestUser()).copy(
                    userId = uid,
                    cardClickDirectPlay = enabled
                )
                dao?.saveUserProfile(newUser.toEntity())
            }
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(cardClickDirectPlay = enabled)
                    )
                    Log.d(TAG, "cardClickDirectPlay $enabled synced to server")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync cardClickDirectPlay to server", e)
                }
            }
        }
    }

    /**
     * 切换字体显示大小比例
     */
    fun updateFontScale(scale: Float) {
        _fontScale.value = scale
        PreferencesManager.saveFontScale(scale)

        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(fontScale = scale)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            val existing = dao?.getUserProfileById(uid)
            if (existing != null) {
                dao?.updateFontScale(uid, scale)
            } else {
                val newUser = (_userProfile.value ?: createDefaultGuestUser()).copy(
                    userId = uid,
                    fontScale = scale
                )
                dao?.saveUserProfile(newUser.toEntity())
            }
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(fontScale = scale)
                    )
                    Log.d(TAG, "fontScale $scale synced to server")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync fontScale to server", e)
                }
            }
        }
    }

    /**
     * 切换主题色彩模式
     */
    fun updateThemeMode(mode: Int, context: Context? = null) {
        _themeMode.value = mode
        context?.let {
            ThemeManager.setTheme(it, ThemeManager.ThemeMode.fromValue(mode))
        }

        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(themeMode = mode)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            val existing = dao?.getUserProfileById(uid)
            if (existing != null) {
                dao?.updateThemeMode(uid, mode)
            } else {
                val newUser = (_userProfile.value ?: createDefaultGuestUser()).copy(
                    userId = uid,
                    themeMode = mode
                )
                dao?.saveUserProfile(newUser.toEntity())
            }
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(themeMode = mode)
                    )
                    Log.d(TAG, "themeMode $mode synced to server")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync themeMode to server", e)
                }
            }
        }
    }

    /**
     * 切换播放详情页卡带/磁带样式
     */
    fun updateCassetteStyle(style: String) {
        val targetStyle = if (style.isNotBlank()) style else DEFAULT_CASSETTE_STYLE
        _cassetteStyle.value = targetStyle
        PreferencesManager.putString("cassette_style", targetStyle)

        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(cassetteStyle = targetStyle)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            val existing = dao?.getUserProfileById(uid)
            if (existing != null) {
                dao?.updateCassetteStyle(uid, targetStyle)
            } else {
                val newUser = (_userProfile.value ?: createDefaultGuestUser()).copy(
                    userId = uid,
                    cassetteStyle = targetStyle
                )
                dao?.saveUserProfile(newUser.toEntity())
            }
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(cassetteStyle = targetStyle)
                    )
                    Log.d(TAG, "cassetteStyle $targetStyle synced to server")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync cassetteStyle to server", e)
                }
            }
        }
    }

    /**
     * 更新预留扩展样式 1
     */
    fun updateReservedStyle1(style: String?) {
        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(reservedStyle1 = style)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            dao?.updateReservedStyle1(uid, style)
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(reservedStyle1 = style)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync reservedStyle1 to server", e)
                }
            }
        }
    }

    /**
     * 更新预留扩展样式 2
     */
    fun updateReservedStyle2(style: String?) {
        val currentUser = _userProfile.value
        if (currentUser != null) {
            _userProfile.value = currentUser.copy(reservedStyle2 = style)
        }

        scope.launch {
            val uid = _userProfile.value?.userId ?: if (_isLoggedIn.value) PreferencesManager.getUserId()?.toLongOrNull() ?: 1L else GUEST_USER_ID
            dao?.updateReservedStyle2(uid, style)
            if (_isLoggedIn.value && uid != GUEST_USER_ID) {
                try {
                    MoodyApiProvider.apiService.updateUserProfile(
                        UpdateProfileRequest(reservedStyle2 = style)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync reservedStyle2 to server", e)
                }
            }
        }
    }

    /**
     * 从服务器拉取用户最新 Profile 与音信音乐资产（全量刷新）
     */
    fun syncFromServer() {
        if (!_isLoggedIn.value) return

        scope.launch {
            // 1. 拉取最新用户资料
            try {
                val profileRes = MoodyApiProvider.apiService.getUserProfile()
                if (profileRes.isSuccess() && profileRes.data != null) {
                    val remoteUser = profileRes.data
                    val safeMode = remoteUser.getEffectivePlayMode()
                    val token = PreferencesManager.getUserToken()
                    val refreshToken = PreferencesManager.getUserRefreshToken()

                    // 智能校准：避免服务端接口未同步导致本地已收藏单曲计数被清零
                    val localSongsCount = maxOf(_favoriteSongsList.value.size, _favoriteSongIds.value.size)
                    val effectiveSongsCount = if (remoteUser.favoriteSongsCount > 0) {
                        remoteUser.favoriteSongsCount
                    } else {
                        maxOf(remoteUser.favoriteSongsCount, localSongsCount)
                    }

                    val mergedUser = remoteUser.copy(
                        token = token,
                        refreshToken = refreshToken,
                        playMode = safeMode,
                        favoriteSongsCount = effectiveSongsCount,
                        cardClickDirectPlay = remoteUser.cardClickDirectPlay,
                        fontScale = if (remoteUser.fontScale > 0f) remoteUser.fontScale else _fontScale.value,
                        themeMode = remoteUser.themeMode,
                        cassetteStyle = if (remoteUser.cassetteStyle.isNotBlank()) remoteUser.cassetteStyle else _cassetteStyle.value,
                        reservedStyle1 = remoteUser.reservedStyle1 ?: _userProfile.value?.reservedStyle1,
                        reservedStyle2 = remoteUser.reservedStyle2 ?: _userProfile.value?.reservedStyle2,
                        reservedPrefInt = remoteUser.reservedPrefInt,
                        reservedPrefStr = remoteUser.reservedPrefStr ?: _userProfile.value?.reservedPrefStr
                    )

                    // 写入 Room
                    dao?.saveUserProfile(mergedUser.toEntity(token, refreshToken))

                    // 更新全局状态
                    _userProfile.value = mergedUser
                    _playMode.value = safeMode
                    _cardClickDirectPlay.value = mergedUser.cardClickDirectPlay
                    _fontScale.value = mergedUser.fontScale
                    _themeMode.value = mergedUser.themeMode
                    _cassetteStyle.value = mergedUser.cassetteStyle

                    PreferencesManager.savePlayMode(safeMode)
                    PreferencesManager.saveCardClickDirectPlay(mergedUser.cardClickDirectPlay)
                    PreferencesManager.saveFontScale(mergedUser.fontScale)
                    PreferencesManager.putString("cassette_style", mergedUser.cassetteStyle)

                    Log.d(TAG, "Profile synced: ${mergedUser.username}, songs=${mergedUser.favoriteSongsCount}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync profile from server failed", e)
            }

            // 2. 拉取音乐资产（收藏 ID 集合，用于心形图标状态判断）
            try {
                val libRes = MoodyApiProvider.apiService.getUserLibrary()
                if (libRes.isSuccess() && libRes.data != null) {
                    val libData = libRes.data
                    val serverSongIds = libData.favoriteSongIds.toSet()
                    val albumIds = libData.favoriteAlbums.map { it.albumId }.toSet()
                    val artistIds = libData.followedArtists.map { it.artistId }.toSet()

                    // 取并集，避免服务端尚未持久化单曲时冲掉本地乐观收藏
                    val finalSongIds = if (serverSongIds.isNotEmpty()) serverSongIds + _favoriteSongIds.value else _favoriteSongIds.value

                    _favoriteSongIds.value = finalSongIds
                    _favoriteAlbumIds.value = albumIds
                    _followedArtistIds.value = artistIds

                    Log.d(TAG, "Library synced: ${finalSongIds.size} songs, ${albumIds.size} albums, ${artistIds.size} artists")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Sync library from server failed", e)
            }
        }
    }

    // ==================== 歌曲收藏 ====================

    fun isSongFavorite(songId: Long): Boolean = _favoriteSongIds.value.contains(songId)

    fun toggleFavoriteSong(
        songId: Long,
        songTitle: String? = null,
        artistName: String? = null,
        coverUrl: String? = null,
        audioUrl: String? = null,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val currentSet = _favoriteSongIds.value
        val willFavorite = !currentSet.contains(songId)

        // 1. 立即乐观更新内存状态
        _favoriteSongIds.value = if (willFavorite) currentSet + songId else currentSet - songId
        onResult?.invoke(willFavorite)

        // 2. 立即乐观更新歌曲列表对象并写入本地缓存
        val currentList = _favoriteSongsList.value
        val updatedList = if (willFavorite) {
            val newSong = FavoriteSong(
                songId = songId,
                title = songTitle?.takeIf { it.isNotBlank() } ?: "未知单曲",
                artistName = artistName,
                coverUrl = coverUrl,
                filePath = audioUrl
            )
            listOf(newSong) + currentList.filterNot { it.songId == songId }
        } else {
            currentList.filterNot { it.songId == songId }
        }
        _favoriteSongsList.value = updatedList
        try {
            PreferencesManager.putString("KEY_CACHED_FAVORITE_SONGS", Gson().toJson(updatedList))
        } catch (_: Exception) {}

        // 3. 立即乐观更新 userProfile 中的 favoriteSongsCount 并写入 Room
        val currentUser = _userProfile.value
        if (currentUser != null) {
            val delta = if (willFavorite) 1 else -1
            val newCount = (currentUser.favoriteSongsCount + delta).coerceAtLeast(0)
            val updated = currentUser.copy(favoriteSongsCount = newCount)
            _userProfile.value = updated
            scope.launch {
                dao?.saveUserProfile(updated.toEntity())
            }
        }

        // 4. 同步至云端
        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    val res = MoodyApiProvider.apiService.toggleFavoriteSong(FavoriteSongToggleRequest(songId))
                    if (res.isSuccess() && res.data != null) {
                        val serverFavorited = res.data.isFavorited
                        if (serverFavorited != willFavorite) {
                            // 校正本地状态
                            val corrected = if (serverFavorited) _favoriteSongIds.value + songId else _favoriteSongIds.value - songId
                            _favoriteSongIds.value = corrected
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "toggleFavoriteSong API error (kept local state)", e)
                }
            }
        }
    }

    // ==================== 专辑收藏 ====================

    fun isAlbumFavorite(albumId: String): Boolean = _favoriteAlbumIds.value.contains(albumId)

    fun toggleFavoriteAlbum(
        albumId: String,
        title: String? = null,
        cover: String? = null,
        artistId: String? = null,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val currentSet = _favoriteAlbumIds.value
        val willFavorite = !currentSet.contains(albumId)

        _favoriteAlbumIds.value = if (willFavorite) currentSet + albumId else currentSet - albumId
        onResult?.invoke(willFavorite)

        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    MoodyApiProvider.apiService.toggleFavoriteAlbum(mapOf("album_id" to albumId))
                    // 更新 profile 计数
                    val currentUser = _userProfile.value
                    if (currentUser != null) {
                        val delta = if (willFavorite) 1 else -1
                        val newCount = (currentUser.favoriteAlbumsCount + delta).coerceAtLeast(0)
                        val updated = currentUser.copy(favoriteAlbumsCount = newCount)
                        _userProfile.value = updated
                        dao?.saveUserProfile(updated.toEntity())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "toggleFavoriteAlbum API error", e)
                }
            }
        }
    }

    // ==================== 歌手关注 ====================

    fun isArtistFollowed(artistId: String): Boolean = _followedArtistIds.value.contains(artistId)

    fun toggleFollowArtist(
        artistId: String,
        name: String? = null,
        avatar: String? = null,
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val currentSet = _followedArtistIds.value
        val willFollow = !currentSet.contains(artistId)

        _followedArtistIds.value = if (willFollow) currentSet + artistId else currentSet - artistId
        onResult?.invoke(willFollow)

        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    MoodyApiProvider.apiService.toggleFollowArtist(mapOf("artist_id" to artistId))
                    // 更新 profile 计数
                    val currentUser = _userProfile.value
                    if (currentUser != null) {
                        val delta = if (willFollow) 1 else -1
                        val newCount = (currentUser.followedArtistsCount + delta).coerceAtLeast(0)
                        val updated = currentUser.copy(followedArtistsCount = newCount)
                        _userProfile.value = updated
                        dao?.saveUserProfile(updated.toEntity())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "toggleFollowArtist API error", e)
                }
            }
        }
    }

    // ==================== 批量取消收藏 / 关注 ====================

    fun batchRemoveFavoriteSongs(songIds: Set<Long>) {
        if (songIds.isEmpty()) return
        val currentSet = _favoriteSongIds.value
        _favoriteSongIds.value = currentSet - songIds

        val currentList = _favoriteSongsList.value
        val updatedList = currentList.filterNot { it.songId in songIds }
        _favoriteSongsList.value = updatedList
        try {
            PreferencesManager.putString("KEY_CACHED_FAVORITE_SONGS", Gson().toJson(updatedList))
        } catch (_: Exception) {}

        val currentUser = _userProfile.value
        if (currentUser != null) {
            val newCount = (currentUser.favoriteSongsCount - songIds.size).coerceAtLeast(0)
            val updated = currentUser.copy(favoriteSongsCount = newCount)
            _userProfile.value = updated
            scope.launch {
                dao?.saveUserProfile(updated.toEntity())
            }
        }

        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    MoodyApiProvider.apiService.batchRemoveLibraryItems(
                        BatchRemoveRequest(type = "songs", ids = songIds.toList())
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "batchRemoveFavoriteSongs API error", e)
                }
            }
        }
    }

    fun batchRemoveFavoriteAlbums(albumIds: Set<String>) {
        if (albumIds.isEmpty()) return
        val currentSet = _favoriteAlbumIds.value
        _favoriteAlbumIds.value = currentSet - albumIds

        val currentUser = _userProfile.value
        if (currentUser != null) {
            val newCount = (currentUser.favoriteAlbumsCount - albumIds.size).coerceAtLeast(0)
            val updated = currentUser.copy(favoriteAlbumsCount = newCount)
            _userProfile.value = updated
            scope.launch {
                dao?.saveUserProfile(updated.toEntity())
            }
        }

        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    MoodyApiProvider.apiService.batchRemoveLibraryItems(
                        BatchRemoveRequest(type = "albums", ids = albumIds.toList())
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "batchRemoveFavoriteAlbums API error", e)
                }
            }
        }
    }

    fun batchRemoveFollowedArtists(artistIds: Set<String>) {
        if (artistIds.isEmpty()) return
        val currentSet = _followedArtistIds.value
        _followedArtistIds.value = currentSet - artistIds

        val currentUser = _userProfile.value
        if (currentUser != null) {
            val newCount = (currentUser.followedArtistsCount - artistIds.size).coerceAtLeast(0)
            val updated = currentUser.copy(followedArtistsCount = newCount)
            _userProfile.value = updated
            scope.launch {
                dao?.saveUserProfile(updated.toEntity())
            }
        }

        if (_isLoggedIn.value) {
            scope.launch {
                try {
                    MoodyApiProvider.apiService.batchRemoveLibraryItems(
                        BatchRemoveRequest(type = "artists", ids = artistIds.toList())
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "batchRemoveFollowedArtists API error", e)
                }
            }
        }
    }

    // ==================== 登录与注销 ====================

    /**
     * 登录或注册成功后的统一数据处理
     */
    fun onLoginSuccess(user: User, token: String, refreshToken: String) {
        PreferencesManager.saveUserToken(token)
        val displayName = user.nickname?.takeIf { it.isNotBlank() } ?: user.username
        PreferencesManager.saveUserInfo(user.userId.toString(), displayName)

        val effectivePlayMode = user.getEffectivePlayMode()
        val fullUser = user.copy(
            token = token,
            refreshToken = refreshToken,
            playMode = effectivePlayMode,
            cardClickDirectPlay = user.cardClickDirectPlay,
            fontScale = if (user.fontScale > 0f) user.fontScale else _fontScale.value,
            themeMode = user.themeMode,
            cassetteStyle = if (user.cassetteStyle.isNotBlank()) user.cassetteStyle else _cassetteStyle.value
        )

        _userProfile.value = fullUser
        _isLoggedIn.value = true
        _playMode.value = effectivePlayMode
        _cardClickDirectPlay.value = fullUser.cardClickDirectPlay
        _fontScale.value = fullUser.fontScale
        _themeMode.value = fullUser.themeMode
        _cassetteStyle.value = fullUser.cassetteStyle

        PreferencesManager.savePlayMode(effectivePlayMode)
        PreferencesManager.saveCardClickDirectPlay(fullUser.cardClickDirectPlay)
        PreferencesManager.saveFontScale(fullUser.fontScale)
        PreferencesManager.putString("cassette_style", fullUser.cassetteStyle)

        // 异步写入 Room（先删除游客记录，保存真实用户资料）
        scope.launch {
            dao?.deleteGuestProfile()
            dao?.saveUserProfile(fullUser.toEntity(token, refreshToken))
        }

        EventBusManager.post(EventType.USER_LOGIN, "登录成功", fullUser)
        EventBusManager.post(EventType.AUTH_LOGIN_SUCCESS, "登录成功", fullUser)

        // 立即触发云端全量数据同步
        syncFromServer()
    }

    /**
     * 退出登录清理
     */
    fun onLogout() {
        val currentCardClick = _cardClickDirectPlay.value
        val currentFont = _fontScale.value
        val currentTheme = _themeMode.value
        val currentCassette = _cassetteStyle.value
        val currentPlay = _playMode.value

        scope.launch {
            dao?.clearUserProfile()
            // 登出后创建新的游客 Profile 保留当前偏好设置
            val guestUser = createDefaultGuestUser(
                cardClickDirectPlay = currentCardClick,
                fontScale = currentFont,
                themeMode = currentTheme,
                playMode = currentPlay,
                cassetteStyle = currentCassette
            )
            dao?.saveUserProfile(guestUser.toEntity())
            _userProfile.value = guestUser
        }
        PreferencesManager.clearUserInfo()

        _isLoggedIn.value = false
        _favoriteSongIds.value = emptySet()
        _favoriteSongsList.value = emptyList()
        _favoriteAlbumIds.value = emptySet()
        _followedArtistIds.value = emptySet()
        try {
            PreferencesManager.putString("KEY_CACHED_FAVORITE_SONGS", null)
        } catch (_: Exception) {}

        EventBusManager.post(EventType.USER_LOGOUT, "退出登录")
    }
}
