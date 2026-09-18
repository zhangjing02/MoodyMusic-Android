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
            } catch (e: Throwable) {
                val userFriendlyMessage = when (e) {
                    is java.net.UnknownHostException, is java.net.SocketTimeoutException -> "网络连接异常，请检查网络后重试"
                    is retrofit2.HttpException -> when (e.code()) {
                        404 -> "该专栏深度导赏正在编排中，敬请期待"
                        500, 502, 503 -> "专栏服务暂时不可用，请稍后重试"
                        else -> "获取专栏内容失败 (${e.code()})"
                    }
                    else -> e.message?.takeIf { it.isNotBlank() } ?: "加载专栏内容失败，请稍后重试"
                }
                _uiState.value = ThemeDetailUiState.Error(userFriendlyMessage)
            }
        }
    }
}
