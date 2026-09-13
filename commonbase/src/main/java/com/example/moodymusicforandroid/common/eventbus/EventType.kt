package com.example.moodymusicforandroid.common.eventbus

/**
 * 事件类型常量定义
 * 所有事件类型都应该在这里定义，避免硬编码
 *
 * 使用说明：
 * 1. 在这里定义新的事件类型
 * 2. 使用时：EventType.MUSIC_PLAY
 * 3. 发送事件：EventBusManager.post(EventType.MUSIC_PLAY, "开始播放", musicData)
 */
object EventType {
    // ========== 通用事件 ==========
    const val UNKNOWN = 0                    // 未知事件

    // ========== 音乐播放事件 ==========
    const val MUSIC_PLAY = 1001              // 开始播放
    const val MUSIC_PAUSE = 1002             // 暂停播放
    const val MUSIC_RESUME = 1003            // 恢复播放
    const val MUSIC_STOP = 1004              // 停止播放
    const val MUSIC_NEXT = 1005              // 下一首
    const val MUSIC_PREVIOUS = 1006          // 上一首
    const val MUSIC_SEEK = 1007              // 跳转进度
    const val MUSIC_COMPLETE = 1008          // 播放完成
    const val MUSIC_ERROR = 1009             // 播放错误

    // ========== 音乐数据变更事件 ==========
    const val MUSIC_LIST_CHANGED = 2001      // 播放列表变化
    const val MUSIC_CURRENT_CHANGED = 2002   // 当前播放音乐变化
    const val MUSIC_PROGRESS_UPDATE = 2003   // 播放进度更新
    const val MUSIC_FAVORITE_CHANGED = 2004  // 收藏状态变化
    const val MUSIC_DOWNLOAD_UPDATE = 2005   // 下载进度更新
    const val MUSIC_DOWNLOAD_COMPLETE = 2006 // 下载完成

    // ========== 用户相关事件 ==========
    const val USER_LOGIN = 3001              // 用户登录
    const val USER_LOGOUT = 3002             // 用户登出
    const val USER_INFO_UPDATE = 3003        // 用户信息更新
    const val USER_PROFILE_CHANGE = 3004     // 用户资料变化

    // ========== UI 相关事件 ==========
    const val UI_REFRESH = 4001              // 刷新界面
    const val UI_RELOAD = 4002               // 重新加载
    const val UI_SHOW_LOADING = 4003         // 显示加载
    const val UI_HIDE_LOADING = 4004         // 隐藏加载
    const val UI_SHOW_TOAST = 4005           // 显示提示
    const val UI_SHOW_DIALOG = 4006          // 显示对话框
    const val UI_CLOSE_PAGE = 4007           // 关闭页面
    const val UI_FINISH_ACTIVITY = 4008      // 结束 Activity

    // ========== 网络相关事件 ==========
    const val Network_CONNECTED = 5001       // 网络连接
    const val Network_DISCONNECTED = 5002    // 网络断开
    const val Network_CHANGE = 5003          // 网络类型变化

    // ========== 数据同步事件 ==========
    const val DATA_SYNC_START = 6001         // 开始同步
    const val DATA_SYNC_COMPLETE = 6002      // 同步完成
    const val DATA_SYNC_ERROR = 6003         // 同步错误
    const val DATA_UPDATE = 6004             // 数据更新

    // ========== 系统事件 ==========
    const val SYSTEM_THEME_CHANGE = 7001     // 主题变化
    const val SYSTEM_LANGUAGE_CHANGE = 7002  // 语言变化
    const val SYSTEM_TIME_CHANGE = 7003      // 时间变化

    // ========== 登录/注册事件 ==========
    const val AUTH_LOGIN_SUCCESS = 8001      // 登录成功
    const val AUTH_LOGIN_FAIL = 8002         // 登录失败
    const val AUTH_REGISTER_SUCCESS = 8003   // 注册成功
    const val AUTH_REGISTER_FAIL = 8004      // 注册失败
    const val AUTH_TOKEN_EXPIRED = 8005      // Token 过期

    // ========== 播放器事件 ==========
    const val PLAYER_BUFFERING_START = 9001  // 开始缓冲
    const val PLAYER_BUFFERING_END = 9002    // 缓冲结束
    const val PLAYER_MODE_CHANGE = 9003      // 播放模式变化（循环/随机/单曲）
    const val PLAYER_PLAYLIST_CLEARED = 9004 // 播放列表清空
    const val MUSIC_PLAY_STATE_CHANGED = 9005 // 播放状态变化（供 PlayerViewModel 监听）

    // ========== 通知事件 ==========
    const val NOTIFICATION_RECEIVED = 10001  // 收到通知
    const val NOTIFICATION_CLICK = 10002     // 点击通知
    const val NOTIFICATION_CLEAR = 10003     // 清除通知

    // ========== 自定义事件起始值 ==========
    /**
     * 自定义事件建议从 20000 开始
     * 例如：const val MY_CUSTOM_EVENT = 20001
     */
    const val CUSTOM_EVENT_START = 20000

    /**
     * 获取事件类型名称（用于调试）
     */
    fun getEventTypeName(type: Int): String {
        return when (type) {
            UNKNOWN -> "UNKNOWN"
            MUSIC_PLAY -> "MUSIC_PLAY"
            MUSIC_PAUSE -> "MUSIC_PAUSE"
            MUSIC_RESUME -> "MUSIC_RESUME"
            MUSIC_STOP -> "MUSIC_STOP"
            MUSIC_NEXT -> "MUSIC_NEXT"
            MUSIC_PREVIOUS -> "MUSIC_PREVIOUS"
            MUSIC_SEEK -> "MUSIC_SEEK"
            MUSIC_COMPLETE -> "MUSIC_COMPLETE"
            MUSIC_ERROR -> "MUSIC_ERROR"
            MUSIC_LIST_CHANGED -> "MUSIC_LIST_CHANGED"
            MUSIC_CURRENT_CHANGED -> "MUSIC_CURRENT_CHANGED"
            MUSIC_PROGRESS_UPDATE -> "MUSIC_PROGRESS_UPDATE"
            MUSIC_FAVORITE_CHANGED -> "MUSIC_FAVORITE_CHANGED"
            MUSIC_DOWNLOAD_UPDATE -> "MUSIC_DOWNLOAD_UPDATE"
            MUSIC_DOWNLOAD_COMPLETE -> "MUSIC_DOWNLOAD_COMPLETE"
            USER_LOGIN -> "USER_LOGIN"
            USER_LOGOUT -> "USER_LOGOUT"
            USER_INFO_UPDATE -> "USER_INFO_UPDATE"
            USER_PROFILE_CHANGE -> "USER_PROFILE_CHANGE"
            UI_REFRESH -> "UI_REFRESH"
            UI_RELOAD -> "UI_RELOAD"
            UI_SHOW_LOADING -> "UI_SHOW_LOADING"
            UI_HIDE_LOADING -> "UI_HIDE_LOADING"
            UI_SHOW_TOAST -> "UI_SHOW_TOAST"
            UI_SHOW_DIALOG -> "UI_SHOW_DIALOG"
            UI_CLOSE_PAGE -> "UI_CLOSE_PAGE"
            UI_FINISH_ACTIVITY -> "UI_FINISH_ACTIVITY"
            Network_CONNECTED -> "Network_CONNECTED"
            Network_DISCONNECTED -> "Network_DISCONNECTED"
            Network_CHANGE -> "Network_CHANGE"
            DATA_SYNC_START -> "DATA_SYNC_START"
            DATA_SYNC_COMPLETE -> "DATA_SYNC_COMPLETE"
            DATA_SYNC_ERROR -> "DATA_SYNC_ERROR"
            DATA_UPDATE -> "DATA_UPDATE"
            SYSTEM_THEME_CHANGE -> "SYSTEM_THEME_CHANGE"
            SYSTEM_LANGUAGE_CHANGE -> "SYSTEM_LANGUAGE_CHANGE"
            SYSTEM_TIME_CHANGE -> "SYSTEM_TIME_CHANGE"
            AUTH_LOGIN_SUCCESS -> "AUTH_LOGIN_SUCCESS"
            AUTH_LOGIN_FAIL -> "AUTH_LOGIN_FAIL"
            AUTH_REGISTER_SUCCESS -> "AUTH_REGISTER_SUCCESS"
            AUTH_REGISTER_FAIL -> "AUTH_REGISTER_FAIL"
            AUTH_TOKEN_EXPIRED -> "AUTH_TOKEN_EXPIRED"
            PLAYER_BUFFERING_START -> "PLAYER_BUFFERING_START"
            PLAYER_BUFFERING_END -> "PLAYER_BUFFERING_END"
            PLAYER_MODE_CHANGE -> "PLAYER_MODE_CHANGE"
            PLAYER_PLAYLIST_CLEARED -> "PLAYER_PLAYLIST_CLEARED"
            NOTIFICATION_RECEIVED -> "NOTIFICATION_RECEIVED"
            NOTIFICATION_CLICK -> "NOTIFICATION_CLICK"
            NOTIFICATION_CLEAR -> "NOTIFICATION_CLEAR"
            else -> "UNKNOWN_EVENT_$type"
        }
    }
}
