package com.example.moodymusicforandroid.common.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

/**
 * Toast 工具类（支持防重复抖动与主线程安全调度）
 */
object ToastUtils {

    @Volatile
    private var lastToastTime = 0L

    @Volatile
    private var lastToastMsg: String? = null

    /**
     * 显示短 Toast，3 秒内相同文案自动防抖
     */
    fun showShort(context: Context?, message: String?) {
        if (context == null || message.isNullOrBlank()) return

        val now = System.currentTimeMillis()
        if (message == lastToastMsg && (now - lastToastTime) < 3000L) {
            return
        }
        lastToastTime = now
        lastToastMsg = message

        val appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
