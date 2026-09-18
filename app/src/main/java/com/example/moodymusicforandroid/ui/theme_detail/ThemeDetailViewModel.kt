package com.example.moodymusicforandroid.ui.theme_detail

import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.model.ThemeStoryDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ThemeDetailUiState {
    object Loading : ThemeDetailUiState
    data class Success(val story: ThemeStoryDto) : ThemeDetailUiState
    data class Error(val message: String) : ThemeDetailUiState
}

class ThemeDetailViewModel : BaseViewModel() {

    private val _uiState = MutableStateFlow<ThemeDetailUiState>(ThemeDetailUiState.Loading)
    val uiState: StateFlow<ThemeDetailUiState> = _uiState.asStateFlow()

    private var currentThemeId: String? = null

    fun loadThemeStory(themeId: String, storyUrl: String? = null) {
        if (currentThemeId == themeId && _uiState.value is ThemeDetailUiState.Success) {
            return
        }
        currentThemeId = themeId
        viewModelScope.launch {
            _uiState.value = ThemeDetailUiState.Loading
            try {
                val story = if (!storyUrl.isNullOrBlank()) {
                    MoodyApiProvider.apiService.getThemeStoryByUrl(storyUrl)
                } else {
                    MoodyApiProvider.apiService.getThemeStoryById(themeId)
                }
                _uiState.value = ThemeDetailUiState.Success(story)
            } catch (e: Exception) {
                try {
                    // 若特定 themeId 偶发网络异常，优雅回退到云端内置默认雪天咖啡馆
                    val fallback = MoodyApiProvider.apiService.getThemeStoryById("snow_cafe_theme")
                    _uiState.value = ThemeDetailUiState.Success(fallback)
                } catch (fallbackEx: Exception) {
                    _uiState.value = ThemeDetailUiState.Error(e.message ?: "加载专栏内容失败")
                }
            }
        }
    }
}
