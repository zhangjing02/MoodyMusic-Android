package com.example.moodymusicforandroid.ui.home.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
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
     * null  = 尚未加载（初始态）或加载失败
     * empty = 后端真实返回空列表
     * non-empty = 正常数据
     *
     * DiscoverScreen 通过区分 null 与 emptyList 决定是否展示本地兜底数据：
     * - null → 展示兜底（网络未就绪 / 请求失败）
     * - emptyList / non-empty → 展示真实数据（哪怕是空）
     */
    private val _artists = MutableLiveData<List<Artist>?>()
    val artists: LiveData<List<Artist>?> = _artists

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchArtists()
    }

    fun fetchArtists() {
        viewModelScope.launch {
            _isRefreshing.value = true
            Log.d(TAG, "fetchArtists() → 开始请求 /api/skeleton ...")
            try {
                val response = MoodyApiProvider.apiService.getArtists()
                Log.d(TAG, "fetchArtists() → 服务端返回 code=${response.code}, message=${response.message}, artists数量=${response.data?.artists?.size}")
                if (response.code == 200) {
                    val list = response.data?.artists ?: emptyList()
                    _artists.value = list
                    Log.i(TAG, "fetchArtists() ✅ 成功，共 ${list.size} 位歌手")
                } else {
                    val errMsg = "接口返回非200: code=${response.code}, message=${response.message}"
                    Log.w(TAG, "fetchArtists() ⚠️ $errMsg")
                    // 保持 null，让 UI 继续展示兜底数据
                }
            } catch (e: Exception) {
                val errMsg = "${e.javaClass.simpleName}: ${e.message}"
                Log.e(TAG, "fetchArtists() ❌ 网络异常: $errMsg", e)
                // 保持 null，让 UI 继续展示兜底数据
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
