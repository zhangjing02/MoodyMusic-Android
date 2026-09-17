package com.example.moodymusicforandroid.common.utils

import android.app.Activity
import android.os.Build
import androidx.annotation.AnimRes
import com.example.moodymusicforandroid.R

/**
 * Activity 页面跳转与退出过渡动画工具类
 * 兼容 Android 14 (API 34) overrideActivityTransition 及更低版本 overridePendingTransition
 */
object ActivityTransitionUtils {

    /**
     * 进入 Activity 的平滑过渡动画（默认由下往上滑入，背景保持静止）
     */
    fun overrideOpenTransition(
        activity: Activity,
        @AnimRes enterAnim: Int = R.anim.anim_slide_up_in,
        @AnimRes exitAnim: Int = R.anim.anim_stay
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    /**
     * 关闭/退出 Activity 的平滑过渡动画（默认由上往下滑出，底层保持静止）
     */
    fun overrideCloseTransition(
        activity: Activity,
        @AnimRes enterAnim: Int = R.anim.anim_stay,
        @AnimRes exitAnim: Int = R.anim.anim_slide_down_out
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }

    /**
     * 渐隐淡出平滑过渡动画
     */
    fun overrideFadeTransition(
        activity: Activity,
        @AnimRes enterAnim: Int = R.anim.anim_fade_in,
        @AnimRes exitAnim: Int = R.anim.anim_fade_out
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, enterAnim, exitAnim)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(enterAnim, exitAnim)
        }
    }
}
