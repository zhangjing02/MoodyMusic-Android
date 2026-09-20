package com.example.moodymusicforandroid.common.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Toast 工具类（支持防重复抖动、防无限叠加与主线程安全调度）
 */
object ToastUtils {

    @Volatile
    private var lastToastTime = 0L

    @Volatile
    private var lastToastMsg: String? = null

    @Volatile
    private var currentToast: Toast? = null

    /**
     * 错误信息脱敏（安全净化）：
     * 拦截所有 D1 限制、数据库内部错误、SQL 错误、HTTP 5xx 堆栈等底层基础设施崩溃，
     * 严禁将长串技术报错透传到用户界面，统一转换为友好的中文提示。
     */
    fun sanitizeErrorMessage(message: String?): String {
        if (message.isNullOrBlank()) return "服务器异常，请稍后重试"
        val lower = message.lowercase()
        if (
            lower.contains("d1_error") ||
            lower.contains("row read limit") ||
            lower.contains("exceeded d1") ||
            lower.contains("sqlite") ||
            lower.contains("sql error") ||
            lower.contains("syntax error") ||
            lower.contains("database is locked") ||
            lower.contains("no such table") ||
            lower.contains("no such column") ||
            lower.contains("constraint failed") ||
            lower.contains("internal server error") ||
            lower.contains("cloudflare") ||
            lower.contains("worker") ||
            lower.contains("failed to connect") ||
            lower.contains("connection refused") ||
            lower.contains("unexpected end of stream") ||
            lower.contains("http 500") ||
            lower.contains("http 502") ||
            lower.contains("http 503") ||
            lower.contains("http 504") ||
            lower.contains("server error") ||
            lower.contains("nullpointerexception")
        ) {
            return "服务器异常，请稍后重试"
        }
        return message
    }

    /**
     * 显示短 Toast，提供智能去重与防无限叠加保护
     * - 相同文案 3.5 秒内自动去重
     * - 下线/互踢提示（含“退出登录”/“其他设备”）6 秒内严格防抖去重
     * - 每次新弹窗前主动 cancel 上一个弹窗，杜绝 Android 系统队列疯狂排队叠加
     * - 自动对底层基础设施错误进行脱敏净化，提示“服务器异常，请稍后重试”
     */
    fun showShort(context: Context?, message: String?) {
        if (context == null || message.isNullOrBlank()) return

        val safeMsg = sanitizeErrorMessage(message)
        val now = System.currentTimeMillis()
        val isKickOutRelated = safeMsg.contains("退出登录") || safeMsg.contains("其他设备")
        val debounceInterval = if (isKickOutRelated) 6000L else 3000L

        if (isKickOutRelated && lastToastMsg?.let { it.contains("退出登录") || it.contains("其他设备") } == true) {
            if (now - lastToastTime < debounceInterval) {
                return
            }
        } else if (safeMsg == lastToastMsg && (now - lastToastTime) < debounceInterval) {
            return
        }

        lastToastTime = now
        lastToastMsg = safeMsg

        val appContext = context.applicationContext
        val showRunnable = Runnable {
            try {
                // 取消排队中的上一个 Toast，杜绝无限叠加
                currentToast?.cancel()
                val t = Toast.makeText(appContext, safeMsg, Toast.LENGTH_SHORT)
                currentToast = t
                t.show()
            } catch (_: Exception) {}
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            showRunnable.run()
        } else {
            Handler(Looper.getMainLooper()).post(showRunnable)
        }
    }
}
