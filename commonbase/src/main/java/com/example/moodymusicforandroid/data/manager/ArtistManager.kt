package com.example.moodymusicforandroid.data.manager

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.local.db.ArtistDao
import com.example.moodymusicforandroid.data.local.db.ArtistEntity
import com.example.moodymusicforandroid.data.local.db.MoodyDatabase
import com.example.moodymusicforandroid.data.local.db.toArtist
import com.example.moodymusicforandroid.data.local.db.toEntity
import com.example.moodymusicforandroid.data.model.Artist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 全局艺人名录与发现页离线中枢 (ArtistManager)
 * 采用 Single Source of Truth (SSOT) 架构与离线优先策略：
 * 1. 数据保存在本地 Room 数据库中，第二次进入发现页不主动触发网络刷新，直接以本地数据秒开展示；
 * 2. 用户下拉刷新时才触发网络请求并批量更新本地数据库；
 * 3. 随心漫游无需在堆内存中常驻庞大对象，直接通过本地数据库 SQLite 进行极速随机抽选。
 */
object ArtistManager {

    private const val TAG = "ArtistManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var db: MoodyDatabase? = null
    private val dao: ArtistDao? get() = db?.artistDao()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * 初始化：在 Application.onCreate() 中注入 Context
     */
    fun init(context: Context) {
        if (db != null) return
        db = MoodyDatabase.getInstance(context)

        scope.launch {
            // 1. 优先从本地数据库加载，实现 0ms 零白屏展示
            val localList = dao?.getAllArtists()?.map { it.toArtist() } ?: emptyList()
            if (localList.isNotEmpty()) {
                _artists.value = localList
                _isInitialized.value = true
                Log.i(TAG, "从本地 Room 数据库秒开加载 ${localList.size} 位歌手")
            }

            // 2. 如果本地从未拉取过数据（初次安装），在后台静默拉取一次填充数据库
            if (localList.isEmpty()) {
                Log.i(TAG, "本地无歌手数据，触发初次静默同步...")
                refresh(force = true)
            } else {
                _isInitialized.value = true
            }

            // 3. 持续监听数据库变更，响应式同步到 StateFlow
            dao?.getAllArtistsFlow()?.collect { entities ->
                val domainList = entities.map { it.toArtist() }
                if (domainList.isNotEmpty()) {
                    _artists.value = domainList
                    _isInitialized.value = true
                }
            }
        }
    }

    /**
     * 刷新歌手名录数据
     * @param force 为 true 时强制发起网络请求（例如用户下拉刷新）；
     *              为 false 时若本地已有数据则不发起网络请求，避免浪费流量与 D1 读配额。
     * @return 刷新是否成功
     */
    suspend fun refresh(force: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val currentCount = dao?.getArtistCount() ?: 0
        if (!force && currentCount > 0) {
            Log.d(TAG, "本地已有 $currentCount 位歌手，跳过自动网络拉取")
            return@withContext true
        }

        try {
            Log.d(TAG, "发起网络请求: GET /api/skeleton ...")
            val resp = MoodyApiProvider.apiService.getArtists()
            if (resp.code == 200 && resp.data != null) {
                val remoteArtists = resp.data!!.artists
                if (remoteArtists.isNotEmpty()) {
                    val entities = remoteArtists.map { it.toEntity() }
                    dao?.insertOrUpdateAll(entities)
                    _artists.value = remoteArtists
                    _isInitialized.value = true
                    Log.i(TAG, "成功更新本地数据库，共 ${entities.size} 位歌手")
                    return@withContext true
                }
            }
            Log.w(TAG, "拉取歌手名录未获得有效数据: code=${resp.code}")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "拉取歌手名录网络异常: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * 随心漫游专用：直接从本地 Room 数据库中极速随机挑选若干艺人
     * 避免在堆内存中长期驻留庞大的列表对象，毫秒级响应。
     */
    suspend fun getRandomArtists(count: Int = 15): List<Artist> = withContext(Dispatchers.IO) {
        val randomEntities = dao?.getRandomArtists(count) ?: emptyList()
        if (randomEntities.isNotEmpty()) {
            return@withContext randomEntities.map { it.toArtist() }
        }
        // 如果本地数据库为空，尝试拉取后再抽选
        refresh(force = true)
        val fallback = dao?.getRandomArtists(count) ?: emptyList()
        return@withContext fallback.map { it.toArtist() }
    }
}
