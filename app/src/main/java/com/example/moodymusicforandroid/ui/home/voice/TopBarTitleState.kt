package com.example.moodymusicforandroid.ui.home.voice

/**
 * 杂志刊头居中标题状态模型
 * 支持默认刊名、倾听状态、搜索状态、播放跑马灯与未收录提示
 */
sealed interface TopBarTitleState {
    object Default : TopBarTitleState
    object Listening : TopBarTitleState
    data class Searching(val queryText: String? = null) : TopBarTitleState
    data class Success(val message: String) : TopBarTitleState
    data class Error(val message: String) : TopBarTitleState
}
