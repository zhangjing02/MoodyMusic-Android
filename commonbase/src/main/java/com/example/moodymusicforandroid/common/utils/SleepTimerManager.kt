package com.example.moodymusicforandroid.common.utils

import android.content.Context
import android.content.Intent
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 睡眠休眠定时器管理器
 * 支持设定 15/30/45/60 分钟倒计时及“播完当前歌曲后停止”
 * 倒计时结束时向播放服务发送暂停指令
 */
object SleepTimerManager {

    const val TIMER_OFF = 0
    const val TIMER_15_MIN = 15
    const val TIMER_30_MIN = 30
    const val TIMER_45_MIN = 45
    const val TIMER_60_MIN = 60
    const val TIMER_END_OF_SONG = -1

    private val _selectedMinutes = MutableStateFlow(TIMER_OFF)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 设置休眠时间（分钟）
     * 0: 关闭
     * -1: 播完当前歌曲
     * > 0: 分钟数
     */
    fun setSleepTimer(context: Context, minutes: Int) {
        timerJob?.cancel()
        _selectedMinutes.value = minutes

        if (minutes <= 0) {
            _remainingSeconds.value = 0
            return
        }

        val totalSeconds = minutes * 60
        _remainingSeconds.value = totalSeconds

        timerJob = scope.launch {
            var current = totalSeconds
            while (current > 0) {
                delay(1000L)
                current--
                _remainingSeconds.value = current
            }
            // 倒计时结束，触发停止/暂停
            triggerPausePlayback(context)
            _selectedMinutes.value = TIMER_OFF
            _remainingSeconds.value = 0
        }
    }

    /**
     * 当歌曲播放结束时调用（若当前设置为 TIMER_END_OF_SONG，则停止播放）
     */
    fun onSongCompletion(context: Context) {
        if (_selectedMinutes.value == TIMER_END_OF_SONG) {
            triggerPausePlayback(context)
            _selectedMinutes.value = TIMER_OFF
            _remainingSeconds.value = 0
        }
    }

    /**
     * 格式化剩余时间展示 (mm:ss)
     */
    fun getFormattedRemainingTime(): String {
        val sec = _remainingSeconds.value
        if (sec <= 0) return ""
        val m = sec / 60
        val s = sec % 60
        return String.format("%02d:%02d", m, s)
    }

    private fun triggerPausePlayback(context: Context) {
        try {
            val intent = Intent("com.example.moodymusicforandroid.ACTION_PAUSE").apply {
                setPackage(context.packageName)
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }
}
