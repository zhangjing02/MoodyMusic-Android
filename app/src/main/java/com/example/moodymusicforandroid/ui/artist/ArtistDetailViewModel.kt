package com.example.moodymusicforandroid.ui.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.model.AlbumWithSongs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ArtistDetailUiState(
    val isLoading: Boolean = false,
    val albums: List<AlbumWithSongs> = emptyList(),
    val artistName: String = "",
    val artistAvatar: String? = null,
    val error: String? = null
)

class ArtistDetailViewModel(
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val artistId: String = savedStateHandle["artistId"] ?: ""
    private val artistNameArg: String = savedStateHandle["artistName"] ?: ""

    private val _uiState = MutableStateFlow(ArtistDetailUiState(artistName = artistNameArg))
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        if (artistId.isNotBlank()) {
            loadArtistDetail()
        }
    }

    fun loadArtistDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = MoodyApiProvider.apiService.getArtistDetail(artistId)
                if (response.code == 200) {
                    val artistData = response.data?.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        albums = artistData?.albums ?: emptyList(),
                        artistAvatar = artistData?.avatar
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
