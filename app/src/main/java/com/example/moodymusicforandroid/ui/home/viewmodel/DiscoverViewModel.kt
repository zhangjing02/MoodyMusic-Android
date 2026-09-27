package com.example.moodymusicforandroid.ui.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.manager.ArtistManager
import com.example.moodymusicforandroid.data.model.Artist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel : BaseViewModel() {

    companion object {
        private const val TAG = "DiscoverVM"
    }

    /**
     * 发现页艺人列表（由本地数据库提供响应式流）
     */
    private val _artists = MutableLiveData<List<Artist>?>()
    val artists: LiveData<List<Artist>?> = _artists

    /**
     * 下拉刷新动画状态（仅在用户手动下拉或首次无数据刷新时为 true）
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // 1. 响应式监听本地 Room 数据库的歌手列表
        viewModelScope.launch {
            ArtistManager.artists.collect { list ->
                if (list.isNotEmpty()) {
                    _artists.value = list
                }
            }
        }

        // 2. 检查本地数据库是否为空；仅在本地完全无数据时才触发初次静默同步，绝不打扰用户
        viewModelScope.launch {
            val currentList = ArtistManager.artists.value
            if (currentList.isEmpty()) {
                Log.d(TAG, "本地暂无歌手缓存，执行首次静默拉取...")
                ArtistManager.refresh(force = false)
            } else {
                _artists.value = currentList
                Log.d(TAG, "从本地 Room 数据库直接呈现 ${currentList.size} 位歌手，无需开启下拉刷新")
            }
        }
    }

    /**
     * 用户手动下拉刷新时调用：强制请求云端接口并批量更新本地 Room 数据库
     */
    fun fetchArtists(force: Boolean = true) {
        viewModelScope.launch {
            _isRefreshing.value = true
            Log.d(TAG, "用户触发下拉刷新，拉取最新 /api/skeleton ...")
            try {
                ArtistManager.refresh(force = force)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
