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
    val allowDownload: Boolean = true,
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
        if (albumTitle.isNotBlank() || artistId.isNotBlank()) {
            loadAlbumDetail()
        }
    }

    fun loadAlbumDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = if (artistId.isNotBlank()) {
                    MoodyApiProvider.apiService.getArtistDetail(artistId)
                } else {
                    MoodyApiProvider.apiService.getSongsByArtist(album = albumTitle)
                }
                if (response.code == 200) {
                    val artistDataList = response.data ?: emptyList()
                    var isDownloadAllowed = response.allowDownload ?: true

                    // 找到匹配专辑（忽略大小写）
                    var album: com.example.moodymusicforandroid.data.model.AlbumWithSongs? = null
                    for (artist in artistDataList) {
                        val matched = artist.albums.firstOrNull { a ->
                            a.title.trim().equals(albumTitle.trim(), ignoreCase = true)
                        }
                        if (matched != null) {
                            album = matched
                            break
                        }
                    }
                    if (album == null) {
                        album = artistDataList.firstOrNull()?.albums?.firstOrNull()
                    }
                    if (album == null && albumTitle.isNotBlank()) {
                        // 兜底：若全集名录未包含，发起单专辑精准查询
                        try {
                            val albumResp = MoodyApiProvider.apiService.getSongsByArtist(
                                artistId = artistId.takeIf { it.isNotBlank() },
                                album = albumTitle
                            )
                            if (albumResp.code == 200) {
                                album = albumResp.data?.firstOrNull()?.albums?.firstOrNull()
                                if (albumResp.allowDownload != null) {
                                    isDownloadAllowed = albumResp.allowDownload == true
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        songs = album?.songs ?: emptyList(),
                        coverUrl = album?.cover ?: "",
                        releaseYear = album?.year ?: "",
                        allowDownload = isDownloadAllowed
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
