package com.example.moodymusicforandroid.ui.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.FavoriteSong
import com.example.moodymusicforandroid.data.model.User
import com.example.moodymusicforandroid.data.model.UserLibraryResponse
import com.example.moodymusicforandroid.data.model.isInvalidName
import com.example.moodymusicforandroid.data.model.enrichedWith
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * 音信 (Library) 页面 ViewModel
 * 管理用户资料展示、收藏歌曲/专辑及关注歌手等音乐资产
 */
class LibraryViewModel : BaseViewModel() {

    private val _userProfile = MutableLiveData<User?>(
        if (UserManager.isLoggedIn.value && UserManager.userProfile.value?.userId != UserManager.GUEST_USER_ID) {
            UserManager.userProfile.value
        } else null
    )
    val userProfile: LiveData<User?> = _userProfile

    private val _userLibrary = MutableLiveData<UserLibraryResponse?>()
    val userLibrary: LiveData<UserLibraryResponse?> = _userLibrary

    private val _favoriteSongs = MutableLiveData<List<FavoriteSong>>(emptyList())
    val favoriteSongs: LiveData<List<FavoriteSong>> = _favoriteSongs

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    init {
        // 订阅 UserManager 登录状态变化
        viewModelScope.launch {
            UserManager.isLoggedIn.collect { loggedIn ->
                if (!loggedIn) {
                    _userProfile.postValue(null)
                    _userLibrary.postValue(null)
                    _favoriteSongs.postValue(emptyList())
                } else {
                    loadData()
                }
            }
        }
        // 订阅 UserManager 全局用户状态
        viewModelScope.launch {
            UserManager.userProfile.collect { user ->
                if (UserManager.isLoggedIn.value && user != null && user.userId != UserManager.GUEST_USER_ID) {
                    _userProfile.postValue(user)
                } else {
                    _userProfile.postValue(null)
                    _userLibrary.postValue(null)
                    _favoriteSongs.postValue(emptyList())
                }
            }
        }
        // 订阅本地已收藏歌曲列表（确保本地收藏操作即时同步至音信页）
        viewModelScope.launch {
            UserManager.favoriteSongsList.collect { list ->
                if (!UserManager.isLoggedIn.value) {
                    _favoriteSongs.postValue(emptyList())
                } else if (list.isNotEmpty() || _favoriteSongs.value?.isNotEmpty() == true) {
                    _favoriteSongs.postValue(list)
                }
            }
        }
    }

    /**
     * 加载音信页面所有数据：library 资产 + 收藏歌曲列表
     * 两个请求并发执行，互不阻塞
     */
    fun loadData() {
        if (!UserManager.isLoggedIn.value) {
            _isRefreshing.value = false
            _userProfile.value = null
            _userLibrary.value = null
            _favoriteSongs.value = emptyList()
            return
        }

        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // 并发请求 library 和 favorites
                val libraryDeferred = async {
                    runCatching { MoodyApiProvider.apiService.getUserLibrary() }.getOrNull()
                }
                val songsDeferred = async {
                    runCatching { MoodyApiProvider.apiService.getFavorites(page = 1, limit = 50) }.getOrNull()
                }

                val libRes = libraryDeferred.await()
                val songsRes = songsDeferred.await()

                val rawLibData = libRes?.data
                if (libRes?.isSuccess() == true && rawLibData != null) {
                    var finalLibData = rawLibData
                    // 容错：如果歌手名无效（后端 JOIN 异常导致返回 db_xx / 占位 ID）或头像为空，从全量歌手列表/本地缓存中补全
                    val hasMissingArtistInfo = finalLibData.followedArtists.any { it.isInvalidName() || it.avatar.isNullOrBlank() }
                    if (hasMissingArtistInfo) {
                        try {
                            val skeleton = MoodyApiProvider.apiService.getArtists()
                            val artistsList = skeleton.data?.artists ?: emptyList()
                            if (artistsList.isNotEmpty()) {
                                val enrichedArtists = finalLibData.followedArtists.map { item ->
                                    item.enrichedWith(artistsList)
                                }
                                finalLibData = finalLibData.copy(followedArtists = enrichedArtists)
                            }
                        } catch (_: Exception) {}
                    }
                    _userLibrary.postValue(finalLibData)
                }

                val localFavorites = UserManager.favoriteSongsList.value
                val serverSongs = if (songsRes?.isSuccess() == true) songsRes.data?.favorites else null

                if (!serverSongs.isNullOrEmpty()) {
                    _favoriteSongs.postValue(serverSongs)
                } else if (localFavorites.isNotEmpty()) {
                    _favoriteSongs.postValue(localFavorites)
                }

            } catch (e: Exception) {
                // 静默失败，保留上次数据
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }
}
