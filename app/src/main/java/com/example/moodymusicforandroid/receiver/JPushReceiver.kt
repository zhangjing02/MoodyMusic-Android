package com.example.moodymusicforandroid.receiver

import android.content.Context
import android.content.Intent
import android.util.Log
import cn.jpush.android.api.CmdMessage
import cn.jpush.android.api.CustomMessage
import cn.jpush.android.api.JPushMessage
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.network.RetrofitClient
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.common.update.PgyerUpdateManager
import com.example.moodymusicforandroid.common.utils.AppFlags
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.manager.UserManager
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * JPush 消息接收器
 *
 * ──────────────────────────────────────────────────────────
 * 透传消息处理（核心：被动刷新 — 设计文档 §5）
 * ──────────────────────────────────────────────────────────
 *
 * 后端触发推送的 payload 结构：
 * {
 *   "title": "refresh_comments",
 *   "extras": {
 *     "album_id": "db_42",
 *     "action": "FETCH_NEW"
 *   }
 * }
 *
 * 安卓端处理策略（参见设计文档 §5.1 ~ §5.3）：
 *
 * ┌──────────────────────┬──────────────────────────────────────────────┐
 * │ 场景                 │ 动作                                          │
 * ├──────────────────────┼──────────────────────────────────────────────┤
 * │ App 在后台（Service  │ 仅置脏标记 AppFlags.hasNewComments = true     │
 * │  存活，UI 不可见）   │ 禁止发起网络请求                              │
 * ├──────────────────────┼──────────────────────────────────────────────┤
 * │ 用户切回前台         │ AlbumDetailActivity.onResume() 检查标记，      │
 * │ (onResume)           │ 触发 ViewModel.fetchSocialContent()           │
 * ├──────────────────────┼──────────────────────────────────────────────┤
 * │ 用户当前在专辑页     │ LiveBus/LocalBroadcast 直接触发刷新           │
 * │ (isResumed == true)  │ ViewModel 更新 UI（DiffUtil 防止闪烁）        │
 * └──────────────────────┴──────────────────────────────────────────────┘
 */
class JPushReceiver : JPushMessageReceiver() {

    companion object {
        private const val TAG = "JPushReceiver"

        /** 透传消息 action 值 */
        const val ACTION_FETCH_NEW = "FETCH_NEW"
        const val ACTION_KICK_OUT = "KICK_OUT"
        const val ACTION_ROSTER_UPDATE = "ROSTER_UPDATE"
        const val ACTION_APP_VERSION_UPDATE = "APP_VERSION_UPDATE"

        /**
         * LocalBroadcast Action — 通知前台 UI 刷新评论
         * 在 AlbumDetailActivity 中注册此 Action 的接收器
         */
        const val BROADCAST_ACTION_REFRESH_COMMENTS =
            "com.example.moodymusicforandroid.ACTION_REFRESH_COMMENTS"

        /**
         * LocalBroadcast Action — 通知前台 UI 刷新座位表
         */
        const val BROADCAST_ACTION_REFRESH_ROSTER =
            "com.example.moodymusicforandroid.ACTION_REFRESH_ROSTER"

        /** Intent extra key：对应专辑 ID */
        const val EXTRA_ALBUM_ID = "album_id"

        /** Intent extra key：对应班级 ID */
        const val EXTRA_CLASS_ID = "class_id"
    }

    // ──────────────────────────────────────────
    // 透传消息（核心入口）
    // ──────────────────────────────────────────
    override fun onMessage(context: Context?, customMessage: CustomMessage?) {
        super.onMessage(context, customMessage)
        if (customMessage == null) return

        val msg = customMessage.message.orEmpty()
        val extra = customMessage.extra.orEmpty()
        Log.d(TAG, "[onMessage] 收到透传: msg=$msg, extra=$extra")

        var action: String? = null
        var albumId: String? = null
        var classId: String? = null

        try {
            // 优先从 extra 解析 (极光透传的标准 extras 字段)
            if (extra.isNotBlank()) {
                val json = RetrofitClient.defaultGson.fromJson(extra, JsonObject::class.java)
                action = json.get("action")?.asString
                albumId = json.get("album_id")?.asString
                classId = json.get("class_id")?.asString
            }
            // 兜底：尝试从 msg 解析 (若发送方整体将 JSON 放入 msg_content)
            if (action == null && msg.isNotBlank() && msg.startsWith("{")) {
                val json = RetrofitClient.defaultGson.fromJson(msg, JsonObject::class.java)
                val extras = json.getAsJsonObject("extras")
                if (extras != null) {
                    action = extras.get("action")?.asString
                    albumId = extras.get("album_id")?.asString
                    classId = extras.get("class_id")?.asString
                } else {
                    action = json.get("action")?.asString
                    albumId = json.get("album_id")?.asString
                    classId = json.get("class_id")?.asString
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[onMessage] 解析透传 JSON 失败: ${e.message}")
        }

        if (action == ACTION_KICK_OUT) {
            Log.d(TAG, "[onMessage] KICK_OUT signal received, triggering kickout")
            handleKickOut(context)
            return
        }

        if (action == ACTION_ROSTER_UPDATE) {
            Log.d(TAG, "[onMessage] ROSTER_UPDATE signal for class: $classId")
            handleRosterUpdate(context, classId.orEmpty())
            return
        }

        if (action == ACTION_APP_VERSION_UPDATE) {
            Log.d(TAG, "[onMessage] APP_VERSION_UPDATE signal received, triggering silent version check")
            handleVersionUpdateCheck(context)
            return
        }

        if (action == ACTION_FETCH_NEW && !albumId.isNullOrEmpty()) {
            Log.d(TAG, "[onMessage] FETCH_NEW signal for album: $albumId")
            handleFetchNew(context, albumId)
            return
        }

        Log.d(TAG, "[onMessage] 未匹配已知 action: $action，忽略")
    }

    /**
     * 处理 FETCH_NEW 信号
     *
     * 判断当前 App 是否在前台：
     * - 前台 → 发送 LocalBroadcast，Activity 直接刷新
     * - 后台 → 设置全局脏标记，等待 onResume 触发刷新
     */
    private fun handleFetchNew(context: Context?, albumId: String) {
        if (context == null) return

        if (AppFlags.isAlbumDetailVisible && AppFlags.visibleAlbumId == albumId) {
            // 场景 C：用户当前正在看这张专辑 — 通过 LocalBroadcast 直接通知 UI
            Log.d(TAG, "[handleFetchNew] 前台可见，发送 LocalBroadcast")
            val intent = android.content.Intent(BROADCAST_ACTION_REFRESH_COMMENTS).apply {
                putExtra(EXTRA_ALBUM_ID, albumId)
            }
            androidx.localbroadcastmanager.content.LocalBroadcastManager
                .getInstance(context)
                .sendBroadcast(intent)
        } else {
            // 场景 A/B：后台或切到其他页面 — 仅置脏标记，onResume 处理
            Log.d(TAG, "[handleFetchNew] 后台状态，置脏标记 hasNewComments=true (albumId=$albumId)")
            AppFlags.hasNewComments = true
            AppFlags.pendingRefreshAlbumId = albumId
        }
    }

    /**
     * 处理 ROSTER_UPDATE 信号
     */
    private fun handleRosterUpdate(context: Context?, classId: String) {
        if (context == null) return

        // 无论应用在后台还是前台，都可以尝试发送广播
        // ClassroomActivity 会在可见时监听此广播
        Log.d(TAG, "[handleRosterUpdate] 发送刷新广播")
        val intent = android.content.Intent(BROADCAST_ACTION_REFRESH_ROSTER).apply {
            putExtra(EXTRA_CLASS_ID, classId)
        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager
            .getInstance(context)
            .sendBroadcast(intent)
    }

    /**
     * 处理 APP_VERSION_UPDATE 信号
     * 收到云端新版本发布推送，立即在后台异步静默请求蒲公英检测更新，更新全局 StateFlow
     */
    private fun handleVersionUpdateCheck(context: Context?) {
        if (context == null) return
        Log.d(TAG, "[handleVersionUpdateCheck] 触发后台静默检测蒲公英新版本")
        PgyerUpdateManager.triggerSilentCheck(context)
    }

    /**
     * 处理 KICK_OUT 互踢信号
     *
     * 完整流程：
     * 1. 清除 PreferencesManager 中的登录凭据（token / userId 等）
     * 2. 通知 UserManager 清除 Room 数据库中的用户状态
     * 3. 弹出轻量 Toast 提示当前设备已退出登录
     * 4. 通过 EventBus 发送 AUTH_TOKEN_EXPIRED 事件，触发前台组件响应式更新
     */
    private fun handleKickOut(context: Context?) {
        Log.d(TAG, "[handleKickOut] 用户被互踢，清除本地登录状态并轻提示")

        // Step 1: 清除 SharedPreferences 中的登录凭据
        try {
            PreferencesManager.clearUserInfo()
        } catch (e: Exception) {
            Log.e(TAG, "[handleKickOut] clearUserInfo 失败: ${e.message}")
        }

        // Step 2: 清除 UserManager / Room DB 中的用户状态（重置为未登录状态）
        try {
            UserManager.onLogout()
        } catch (e: Exception) {
            Log.e(TAG, "[handleKickOut] UserManager.onLogout 失败: ${e.message}")
        }

        // Step 3: 弹出轻量 Toast 提示（无论在哪个页面）
        try {
            com.example.moodymusicforandroid.common.utils.ToastUtils.showShort(
                context,
                "您的账号已在其他设备登录，当前已退出登录"
            )
        } catch (e: Exception) {
            Log.e(TAG, "[handleKickOut] Toast 提示失败: ${e.message}")
        }

        // Step 4: 发送事件，通知前台组件（如音信页面、抽屉等）响应式更新 UI
        EventBusManager.post(EventType.AUTH_TOKEN_EXPIRED, "KICKED_OUT")
    }

    // ──────────────────────────────────────────
    // 通知栏消息（普通推送，非透传）
    // ──────────────────────────────────────────
    override fun onNotifyMessageOpened(context: Context?, message: NotificationMessage?) {
        Log.d(TAG, "[onNotifyMessageOpened] 用户点击了通知")
        super.onNotifyMessageOpened(context, message)
    }

    override fun onNotifyMessageArrived(context: Context?, message: NotificationMessage?) {
        Log.d(TAG, "[onNotifyMessageArrived] 通知已抵达")
        super.onNotifyMessageArrived(context, message)
    }

    override fun onNotifyMessageDismiss(context: Context?, message: NotificationMessage?) {
        Log.d(TAG, "[onNotifyMessageDismiss] 通知被清除")
        super.onNotifyMessageDismiss(context, message)
    }

    /**
     * 极光 SDK 完成注册后的回调。
     *
     * 触发时机：App 首次安装后 JPush 分配 RegistrationId，或重新注册后 ID 变更。
     * 策略：如果当前用户已登录，立即将新的 RegistrationId 上报到服务器，
     *       确保服务端的互踢推送目标始终是最新的设备。
     */
    override fun onRegister(context: Context?, registrationId: String?) {
        Log.d(TAG, "[onRegister] Registration Id: $registrationId")
        super.onRegister(context, registrationId)

        if (registrationId.isNullOrEmpty()) return
        PreferencesManager.saveJPushRegistrationId(registrationId)
        if (!PreferencesManager.isLoggedIn()) return

        // 异步上报新 RegistrationId 到服务器
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MoodyApiProvider.apiService.updateJPushRegistrationId(
                    mapOf("jpush_registration_id" to registrationId)
                )
                Log.d(TAG, "[onRegister] RegistrationId 上报成功: $registrationId")
            } catch (e: Exception) {
                Log.e(TAG, "[onRegister] RegistrationId 上报失败: ${e.message}")
            }
        }
    }

    override fun onConnected(context: Context?, isConnected: Boolean) {
        Log.d(TAG, "[onConnected] $isConnected")
        super.onConnected(context, isConnected)
    }

    override fun onCommandResult(context: Context?, cmdMessage: CmdMessage?) {
        Log.d(TAG, "[onCommandResult] $cmdMessage")
        super.onCommandResult(context, cmdMessage)
    }

    override fun onMultiActionClicked(context: Context?, intent: android.content.Intent?) {
        Log.d(TAG, "[onMultiActionClicked] 用户点击了通知栏按钮")
        super.onMultiActionClicked(context, intent)
    }

    override fun onNotificationSettingsCheck(context: Context?, isOn: Boolean, source: Int) {
        Log.d(TAG, "[onNotificationSettingsCheck] isOn=$isOn, source=$source")
        super.onNotificationSettingsCheck(context, isOn, source)
    }
}
