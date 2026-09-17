package com.example.moodymusicforandroid.data.manager

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.local.db.MoodyDatabase
import com.example.moodymusicforandroid.data.local.db.PlaylistDao
import com.example.moodymusicforandroid.data.local.db.PlaylistEntity
import com.example.moodymusicforandroid.data.local.db.PlaylistSongEntity
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 全局自定义播放列表管理中枢 (PlaylistManager)
 * 采用 Single Source of Truth (SSOT) 架构与 0ms 乐观更新机制：
 * - 本地 Room 数据库即时读写，保证离线可用、弱网秒开、体验无卡顿；
 * - 云端 Cloudflare Worker 协同静默同步，保证多端与多设备一致性。
 */
object PlaylistManager {

    private const val TAG = "PlaylistManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var db: MoodyDatabase? = null
    private val dao: PlaylistDao? get() = db?.playlistDao()

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    private fun currentUserId(): Long {
        if (UserManager.isLoggedIn.value) {
            val user = UserManager.userProfile.value
            if (user != null && user.userId > 0) {
                return user.userId
            }
            val spUid = PreferencesManager.getUserId()?.toLongOrNull()
            if (spUid != null && spUid > 0) {
                return spUid
            }
        }
        return UserManager.GUEST_USER_ID
    }

    /**
     * 初始化：在 Application.onCreate() 中注入 Context
     */
    fun init(context: Context) {
        db = MoodyDatabase.getInstance(context)

        // 监听登录态与用户资料切换，刷新本地和远端歌单
        scope.launch {
            combine(UserManager.isLoggedIn, UserManager.userProfile) { loggedIn, profile ->
                if (loggedIn && profile != null && profile.userId > 0) {
                    profile.userId
                } else if (loggedIn) {
                    PreferencesManager.getUserId()?.toLongOrNull() ?: UserManager.GUEST_USER_ID
                } else {
                    UserManager.GUEST_USER_ID
                }
            }.distinctUntilChanged().collect { uid ->
                if (uid != UserManager.GUEST_USER_ID) {
                    cleanOrMigrateGuestPlaylists(uid)
                }
                refreshPlaylists()
            }
        }
    }

    /**
     * 当用户登录后，自动将遗留的游客模式歌单迁移合并至当前用户，
     * 避免因游客残留数据导致清单列表混乱或歌单丢失
     */
    private suspend fun cleanOrMigrateGuestPlaylists(realUserId: Long) {
        val d = dao ?: return
        try {
            val guestPlaylists = d.getUserPlaylists(UserManager.GUEST_USER_ID)
            if (guestPlaylists.isEmpty()) return

            val userPlaylists = d.getUserPlaylists(realUserId)
            val userPlaylistByName = userPlaylists.associateBy { it.name }

            for (gp in guestPlaylists) {
                val matched = userPlaylistByName[gp.name]
                if (matched != null) {
                    // 已有同名歌单，将其中的歌曲迁移合并到已有歌单中
                    val guestSongs = d.getSongsByPlaylistId(gp.id)
                    for (gs in guestSongs) {
                        d.insertOrUpdatePlaylistSong(gs.copy(playlistId = matched.id))
                    }
                    val totalCount = d.getSongsByPlaylistId(matched.id).size
                    d.updatePlaylistCount(matched.id, totalCount)
                    if (matched.coverUrl.isNullOrBlank() && !gp.coverUrl.isNullOrBlank()) {
                        d.updatePlaylistCover(matched.id, gp.coverUrl)
                    }
                    // 删除该游客歌单及关联歌曲
                    d.deleteSongsByPlaylistId(gp.id)
                    d.deletePlaylist(gp.id)
                } else {
                    // 没有同名歌单，直接更新 user_id 为当前登录用户
                    d.updatePlaylistUserId(gp.id, realUserId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "cleanOrMigrateGuestPlaylists error", e)
        }
    }

    /**
     * 刷新播放列表：先秒级加载本地 Room，若已登录则异步拉取云端合并
     */
    fun refreshPlaylists() {
        val uid = currentUserId()
        val d = dao ?: return
        scope.launch {
            try {
                // 1. 本地 Room 秒开加载
                val localList = d.getUserPlaylists(uid)
                _playlists.value = localList

                // 2. 若已登录，静默拉取云端
                if (UserManager.isLoggedIn.value && uid != UserManager.GUEST_USER_ID) {
                    val response = MoodyApiProvider.apiService.getUserPlaylists()
                    if (response.isSuccess() && response.data != null) {
                        val serverPlaylists = response.data!!
                        val entities = serverPlaylists.map { p ->
                            PlaylistEntity(
                                id = p.id,
                                userId = uid,
                                name = p.name,
                                description = p.description,
                                themeColor = p.themeColor,
                                coverUrl = p.coverUrl,
                                songCount = p.songCount,
                                updatedAt = System.currentTimeMillis()
                            )
                        }
                        d.insertOrUpdatePlaylists(entities)
                        _playlists.value = d.getUserPlaylists(uid)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "refreshPlaylists failed", e)
            }
        }
    }

    /**
     * 查询指定单曲被收录进了当前用户的哪些歌单 ID
     */
    suspend fun getSongMembershipIds(songId: Long): Set<Long> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext emptySet()
        val uid = currentUserId()
        try {
            val localIds = d.getPlaylistIdsForSong(uid, songId).toSet()
            if (localIds.isNotEmpty() || !UserManager.isLoggedIn.value) {
                return@withContext localIds
            }

            // 若本地为空且已登录，尝试从服务器同步
            val res = MoodyApiProvider.apiService.getSongPlaylistMemberships(songId)
            if (res.isSuccess() && res.data != null) {
                res.data!!.playlistIds.toSet()
            } else {
                localIds
            }
        } catch (e: Exception) {
            Log.w(TAG, "getSongMembershipIds error", e)
            emptySet()
        }
    }

    /**
     * 创建歌单（0ms 乐观更新）
     */
    fun createPlaylist(
        name: String,
        description: String? = null,
        themeColor: String = "DEFAULT",
        onCreated: ((Long) -> Unit)? = null
    ) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        val uid = currentUserId()
        val d = dao ?: return

        scope.launch {
            // 临时生成一个本地 ID（负时间戳或毫秒戳）
            val tempId = System.currentTimeMillis()
            val localEntity = PlaylistEntity(
                id = tempId,
                userId = uid,
                name = trimmed,
                description = description?.trim(),
                themeColor = themeColor,
                coverUrl = null,
                songCount = 0,
                updatedAt = System.currentTimeMillis()
            )

            // 1. 立即入库 Room 并刷新 StateFlow
            d.insertOrUpdatePlaylist(localEntity)
            _playlists.value = d.getUserPlaylists(uid)
            withContext(Dispatchers.Main) {
                onCreated?.invoke(tempId)
            }

            // 2. 若已登录，同步云端
            if (UserManager.isLoggedIn.value && uid != UserManager.GUEST_USER_ID) {
                try {
                    val res = MoodyApiProvider.apiService.createPlaylist(
                        CreatePlaylistRequest(
                            name = trimmed,
                            description = description?.trim(),
                            themeColor = themeColor
                        )
                    )
                    if (res.isSuccess() && res.data != null) {
                        val serverPlaylist = res.data!!
                        // 替换临时 ID 为真实云端 ID
                        d.deletePlaylist(tempId)
                        val realEntity = localEntity.copy(id = serverPlaylist.id)
                        d.insertOrUpdatePlaylist(realEntity)
                        _playlists.value = d.getUserPlaylists(uid)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "createPlaylist cloud sync error", e)
                }
            }
        }
    }

    /**
     * 修改歌单基本信息（名称、寄语、色调）
     */
    fun updatePlaylist(
        playlistId: Long,
        name: String,
        description: String?,
        themeColor: String
    ) {
        val uid = currentUserId()
        val d = dao ?: return

        scope.launch {
            val existing = d.getPlaylistById(playlistId) ?: return@launch
            val updated = existing.copy(
                name = name.trim().ifEmpty { existing.name },
                description = description?.trim() ?: existing.description,
                themeColor = themeColor.ifEmpty { existing.themeColor },
                updatedAt = System.currentTimeMillis()
            )
            d.insertOrUpdatePlaylist(updated)
            _playlists.value = d.getUserPlaylists(uid)

            if (UserManager.isLoggedIn.value && uid != UserManager.GUEST_USER_ID && playlistId > 0) {
                try {
                    MoodyApiProvider.apiService.updatePlaylist(
                        id = playlistId,
                        request = UpdatePlaylistRequest(
                            name = updated.name,
                            description = updated.description,
                            themeColor = updated.themeColor
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "updatePlaylist cloud sync error", e)
                }
            }
        }
    }

    /**
     * 删除歌单
     */
    fun deletePlaylist(playlistId: Long) {
        val uid = currentUserId()
        val d = dao ?: return

        scope.launch {
            d.deleteSongsByPlaylistId(playlistId)
            d.deletePlaylist(playlistId)
            _playlists.value = d.getUserPlaylists(uid)

            if (UserManager.isLoggedIn.value && uid != UserManager.GUEST_USER_ID && playlistId > 0) {
                try {
                    MoodyApiProvider.apiService.deletePlaylist(playlistId)
                } catch (e: Exception) {
                    Log.w(TAG, "deletePlaylist cloud sync error", e)
                }
            }
        }
    }

    /**
     * 单曲与歌单的关联切换（添加或移出）
     */
    fun toggleSongInPlaylist(
        playlistId: Long,
        songId: Long,
        title: String,
        artistName: String? = null,
        albumTitle: String? = null,
        coverUrl: String? = null,
        filePath: String? = null,
        duration: Int = 0,
        isAdd: Boolean
    ) {
        val uid = currentUserId()
        val d = dao ?: return

        scope.launch {
            if (isAdd) {
                val songEntity = PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = songId,
                    title = title,
                    artistName = artistName,
                    albumTitle = albumTitle,
                    coverUrl = coverUrl,
                    filePath = filePath,
                    duration = duration,
                    addedAt = System.currentTimeMillis()
                )
                d.insertOrUpdatePlaylistSong(songEntity)
                val newCount = d.getSongsByPlaylistId(playlistId).size
                d.updatePlaylistCount(playlistId, newCount)
                if (!coverUrl.isNullOrBlank()) {
                    d.updatePlaylistCover(playlistId, coverUrl)
                }
            } else {
                d.deletePlaylistSong(playlistId, songId)
                val newCount = d.getSongsByPlaylistId(playlistId).size
                d.updatePlaylistCount(playlistId, newCount)
            }
            _playlists.value = d.getUserPlaylists(uid)

            // 云端静默同步
            if (UserManager.isLoggedIn.value && uid != UserManager.GUEST_USER_ID && playlistId > 0) {
                try {
                    if (isAdd) {
                        MoodyApiProvider.apiService.addSongToPlaylist(
                            id = playlistId,
                            request = AddSongToPlaylistRequest(
                                songId = songId,
                                title = title,
                                artistName = artistName,
                                albumTitle = albumTitle,
                                coverUrl = coverUrl,
                                filePath = filePath,
                                duration = duration
                            )
                        )
                    } else {
                        MoodyApiProvider.apiService.removeSongFromPlaylist(
                            id = playlistId,
                            songId = songId
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "toggleSongInPlaylist cloud sync error", e)
                }
            }
        }
    }

    /**
     * 获取歌单全部歌曲 Flow
     */
    fun getSongsFlow(playlistId: Long): Flow<List<PlaylistSongEntity>> {
        return dao?.getSongsByPlaylistIdFlow(playlistId) ?: kotlinx.coroutines.flow.emptyFlow()
    }

    /**
     * 获取歌单全部歌曲（挂起直接取列表）
     */
    suspend fun getSongs(playlistId: Long): List<PlaylistSongEntity> = withContext(Dispatchers.IO) {
        val d = dao ?: return@withContext emptyList()
        var list = d.getSongsByPlaylistId(playlistId)

        // 若本地没有，尝试向服务器拉取
        if (list.isEmpty() && UserManager.isLoggedIn.value && playlistId > 0) {
            try {
                val res = MoodyApiProvider.apiService.getPlaylistSongs(playlistId)
                if (res.isSuccess() && res.data != null) {
                    val serverSongs = res.data!!.songs.map { s ->
                        PlaylistSongEntity(
                            playlistId = playlistId,
                            songId = s.songId,
                            title = s.title,
                            artistName = s.artistName,
                            albumTitle = s.albumTitle,
                            coverUrl = s.coverUrl,
                            filePath = s.filePath,
                            duration = s.duration,
                            sortOrder = s.sortOrder,
                            addedAt = System.currentTimeMillis()
                        )
                    }
                    d.insertOrUpdatePlaylistSongs(serverSongs)
                    list = serverSongs
                }
            } catch (e: Exception) {
                Log.w(TAG, "getSongs cloud fetch error", e)
            }
        }
        list
    }
}
