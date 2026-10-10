package com.example.moodymusicforandroid.common.utils

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * 睡眠休眠定时器与夜间防沉睡自动守护中枢 (SleepTimerManager)
 *
 * 核心功能：
 * 1. 【主动休眠定时器 (Explicit Sleep Timer)】
 *    - 支持设定 15/30/45/60 分钟倒计时或“播完当前歌曲后停止”；
 *    - 倒计时结束时触发 EventBus 停播与平滑淡出。
 *
 * 2. 【智能夜间闲置守护 (Smart Night-time Idle Sleep Guard)】
 *    - 处于夜间时段 (23:00 ~ 06:00)；
 *    - 连续 N 分钟 (默认 45 分钟，可选 30/45/60 分钟) 无任何交互操作；
 *    - 自动触发渐弱平滑淡出，停止播放并释放前台通知与后台服务，保护听力并彻底杜绝整夜耗电。
 */
object SleepTimerManager {

    private const val TAG = "SleepTimerManager"

    // ── 主动休眠定时选项常量 ──
    const val TIMER_OFF = 0
    const val TIMER_15_MIN = 15
    const val TIMER_30_MIN = 30
    const val TIMER_45_MIN = 45
    const val TIMER_60_MIN = 60
    const val TIMER_END_OF_SONG = -1

    // ── 主动定时器响应式状态 ──
    private val _selectedMinutes = MutableStateFlow(TIMER_OFF)
    val selectedMinutes: StateFlow<Int> = _selectedMinutes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // ── 夜间闲置守护响应式状态 ──
    private val _nightIdleGuardEnabled = MutableStateFlow(true)
    val nightIdleGuardEnabled: StateFlow<Boolean> = _nightIdleGuardEnabled.asStateFlow()

    private val _nightIdleGuardMinutes = MutableStateFlow(45)
    val nightIdleGuardMinutes: StateFlow<Int> = _nightIdleGuardMinutes.asStateFlow()

    // 用户最近一次交互时间戳（支持系统启动、触控、切歌、按键）
    @Volatile
    private var lastInteractionTimeMs: Long = System.currentTimeMillis()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // 从本地持久化加载夜间守护配置
        try {
            if (PreferencesManager.getContext() != null) {
                _nightIdleGuardEnabled.value = PreferencesManager.isNightIdleGuardEnabled()
                _nightIdleGuardMinutes.value = PreferencesManager.getNightIdleGuardMinutes()
            }
        } catch (_: Exception) {}
    }

    /**
     * 刷新从 SP 加载的配置（如 App 启动或 context 就绪后）
     */
    fun reloadPreferences() {
        try {
            _nightIdleGuardEnabled.value = PreferencesManager.isNightIdleGuardEnabled()
            _nightIdleGuardMinutes.value = PreferencesManager.getNightIdleGuardMinutes()
        } catch (_: Exception) {}
    }

    /**
     * 记录用户活跃交互（触摸屏、切歌、调节音量、语音点歌、耳机线控）
     */
    fun recordUserInteraction() {
        lastInteractionTimeMs = System.currentTimeMillis()
    }

    fun getLastInteractionTimeMs(): Long = lastInteractionTimeMs

    /**
     * 判断当前时间是否处于夜间时段 (23:00 ~ 06:00)
     */
    fun isNightTime(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour >= 23 || hour < 6
    }

    /**
     * 检查夜间闲置是否超时（供播放服务进度心跳每秒轮询调用）
     * 满足条件：夜间守护已开启 + 当前处于夜间时段 + 距离最近交互超过设定分钟数
     */
    fun checkNightIdleTimeout(): Boolean {
        if (!_nightIdleGuardEnabled.value) return false
        if (!isNightTime()) return false

        val timeoutMs = _nightIdleGuardMinutes.value * 60 * 1000L
        val idleMs = System.currentTimeMillis() - lastInteractionTimeMs
        return idleMs >= timeoutMs
    }

    /**
     * 更新夜间守护开关
     */
    fun setNightIdleGuardEnabled(enabled: Boolean) {
        _nightIdleGuardEnabled.value = enabled
        PreferencesManager.saveNightIdleGuardEnabled(enabled)
        recordUserInteraction()
    }

    /**
     * 更新夜间守护闲置时长（分钟）
     */
    fun setNightIdleGuardMinutes(minutes: Int) {
        _nightIdleGuardMinutes.value = minutes
        PreferencesManager.saveNightIdleGuardMinutes(minutes)
        recordUserInteraction()
    }

    /**
     * 设置主动休眠时间（分钟）
     * 0: 关闭
     * -1: 播完当前歌曲
     * > 0: 分钟数
     */
    fun setSleepTimer(context: Context, minutes: Int) {
        recordUserInteraction()
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
            // 倒计时结束，触发停止/平滑淡出
            Log.i(TAG, "Explicit sleep timer expired ($minutes min), triggering sleep stop")
            triggerSleepStop(context, reason = "倒计时已结束")
            _selectedMinutes.value = TIMER_OFF
            _remainingSeconds.value = 0
        }
    }

    /**
     * 当单曲播放自然结束时由 Service 调用
     * @return true 表示由于“播完当曲”策略已命中并触发停播；false 表示正常放行播下一首
     */
    fun onSongCompletion(context: Context): Boolean {
        if (_selectedMinutes.value == TIMER_END_OF_SONG) {
            Log.i(TAG, "Song completed under TIMER_END_OF_SONG, triggering sleep stop")
            triggerSleepStop(context, reason = "当前单曲已播放完毕")
            _selectedMinutes.value = TIMER_OFF
            _remainingSeconds.value = 0
            return true
        }
        return false
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

    /**
     * 触发休眠停播（优先通过 EventBus 发送解耦事件）
     */
    fun triggerSleepStop(context: Context, reason: String = "休眠定时") {
        try {
            EventBusManager.post(BaseEvent(EventType.SLEEP_TIMER_PAUSE, reason))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post SLEEP_TIMER_PAUSE event: ${e.message}")
        }
    }
}
