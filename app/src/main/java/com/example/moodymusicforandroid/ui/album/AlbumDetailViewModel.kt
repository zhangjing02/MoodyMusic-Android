package com.example.moodymusicforandroid.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.model.SongItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AlbumDetailUiState(
    val isLoading: Boolean = false,
    val songs: List<SongItem> = emptyList(),
    val coverUrl: String = "",
    val releaseYear: String = "",
    val error: String? = null
)

class AlbumDetailViewModel(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val artistId: String = savedStateHandle["artistId"] ?: ""
    private val albumTitle: String = savedStateHandle["albumTitle"] ?: ""

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    init {
        if (artistId.isNotBlank() && albumTitle.isNotBlank()) {
            loadAlbumDetail()
        }
    }

    fun loadAlbumDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = MoodyApiProvider.apiService.getArtistDetail(artistId)
                if (response.code == 200) {
                    val artistData = response.data?.firstOrNull()
                    // 找到匹配专辑（忽略大小写）
                    val album = artistData?.albums?.firstOrNull { album ->
                        album.title.trim().equals(albumTitle.trim(), ignoreCase = true)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        songs = album?.songs ?: emptyList(),
                        coverUrl = album?.cover ?: "",
                        releaseYear = album?.year ?: ""
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "加载失败: ${response.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "网络异常：${e.message}"
                )
            }
        }
    }
}
