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
    private val artistAvatarArg: String? = savedStateHandle["artistAvatar"]

    private val _uiState = MutableStateFlow(
        ArtistDetailUiState(
            artistName = artistNameArg,
            artistAvatar = artistAvatarArg?.takeIf { it.isNotBlank() }
        )
    )
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        if (artistId.isNotBlank()) {
            loadArtistDetail()
        }
    }

    fun updateAvatar(avatar: String?) {
        if (!avatar.isNullOrBlank() && _uiState.value.artistAvatar != avatar) {
            _uiState.value = _uiState.value.copy(artistAvatar = avatar)
        }
    }

    fun loadArtistDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = MoodyApiProvider.apiService.getArtistDetail(artistId)
                if (response.code == 200) {
                    val artistData = response.data?.firstOrNull()
                    val rawAlbums = artistData?.albums ?: emptyList()
                    val sortedAlbums = rawAlbums.sortedWith(
                        compareByDescending<AlbumWithSongs> { album ->
                            album.songs.any { !it.path.isNullOrBlank() }
                        }.thenBy { album ->
                            val yr = album.year.takeIf { it != "未知" && it.isNotBlank() } ?: "9999"
                            yr
                        }
                    )
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        albums = sortedAlbums,
                        artistAvatar = artistData?.avatar ?: _uiState.value.artistAvatar
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
