package com.example.moodymusicforandroid.common.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * SharedPreferences 封装管理类
 * 提供类型安全的存取方法和委托属性支持
 *
 * 使用示例：
 * ```
 * // 方式1：直接调用
 * PreferencesManager.saveToken("your_token")
 * val token = PreferencesManager.getToken()
 *
 * // 方式2：使用委托属性（在 ViewModel 或其他类中）
 * class MyViewModel {
 *     var userToken by PreferencesDelegate.string("user_token")
 * }
 * ```
 */
object PreferencesManager {

    private const val PREFS_NAME = "moody_music_prefs"
    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null

    fun getContext(): Context? = appContext

    // 缓存相关的键名
    private const val KEY_CACHE_ARTISTS = "cache_artists"
    private const val KEY_CACHE_ARTISTS_TIME = "cache_artists_time"
    private const val KEY_CACHE_STATS = "cache_stats"
    private const val KEY_CACHE_STATS_TIME = "cache_stats_time"

    // 用户相关的键名
    private const val KEY_USER_TOKEN = "user_token"
    private const val KEY_USER_REFRESH_TOKEN = "user_refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_CLASS_ID = "class_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_JPUSH_REG_ID = "jpush_registration_id"
    private const val KEY_APP_VERSION = "app_version"
    private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"

    fun saveJPushRegistrationId(regId: String) {
        putString(KEY_JPUSH_REG_ID, regId)
    }

    fun getJPushRegistrationId(): String? {
        return getString(KEY_JPUSH_REG_ID)
    }

    fun saveLoginTimestamp(timestamp: Long) {
        putLong(KEY_LOGIN_TIMESTAMP, timestamp)
    }

    fun getLoginTimestamp(): Long {
        return getLong(KEY_LOGIN_TIMESTAMP, 0L)
    }

    // 设置相关的键名
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_FIRST_LAUNCH = "first_launch"
    private const val KEY_FONT_SCALE = "font_scale"
    private const val KEY_PLAY_MODE = "play_mode"
    private const val KEY_CARD_CLICK_DIRECT_PLAY = "card_click_direct_play"

    fun getFontScale(): Float = getFloat(KEY_FONT_SCALE, 1.0f)
    fun saveFontScale(scale: Float) = putFloat(KEY_FONT_SCALE, scale)

    fun getPlayMode(): String = getString(KEY_PLAY_MODE, "SEQUENTIAL") ?: "SEQUENTIAL"
    fun savePlayMode(mode: String) = putString(KEY_PLAY_MODE, mode)

    fun isCardClickDirectPlay(): Boolean = getBoolean(KEY_CARD_CLICK_DIRECT_PLAY, true)
    fun saveCardClickDirectPlay(enabled: Boolean) = putBoolean(KEY_CARD_CLICK_DIRECT_PLAY, enabled)


    /**
     * 初始化，建议在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 保存当前版本号
        try {
            putString(KEY_APP_VERSION, com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getVersionName(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 确保已初始化
     */
    internal fun checkInitialized() {
        if (!::prefs.isInitialized) {
            throw IllegalStateException("PreferencesManager not initialized. Call init(context) in Application.onCreate()")
        }
    }

    // ==================== 通用方法 ====================

    /**
     * 保存字符串
     */
    fun putString(key: String, value: String?) {
        checkInitialized()
        prefs.edit().putString(key, value).apply()
    }

    /**
     * 获取字符串
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        checkInitialized()
        return prefs.getString(key, defaultValue)
    }

    /**
     * 保存整型
     */
    fun putInt(key: String, value: Int) {
        checkInitialized()
        prefs.edit().putInt(key, value).apply()
    }

    /**
     * 获取整型
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        checkInitialized()
        return prefs.getInt(key, defaultValue)
    }

    /**
     * 保存长整型
     */
    fun putLong(key: String, value: Long) {
        checkInitialized()
        prefs.edit().putLong(key, value).apply()
    }

    /**
     * 获取长整型
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        checkInitialized()
        return prefs.getLong(key, defaultValue)
    }

    /**
     * 保存浮点型
     */
    fun putFloat(key: String, value: Float) {
        checkInitialized()
        prefs.edit().putFloat(key, value).apply()
    }

    /**
     * 获取浮点型
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        checkInitialized()
        return prefs.getFloat(key, defaultValue)
    }

    /**
     * 保存布尔型
     */
    fun putBoolean(key: String, value: Boolean) {
        checkInitialized()
        prefs.edit().putBoolean(key, value).apply()
    }

    /**
     * 获取布尔型
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        checkInitialized()
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * 移除指定键
     */
    fun remove(key: String) {
        checkInitialized()
        prefs.edit().remove(key).apply()
    }

    /**
     * 清空所有数据
     */
    fun clear() {
        checkInitialized()
        prefs.edit().clear().apply()
    }

    /**
     * 检查是否包含某个键
     */
    fun contains(key: String): Boolean {
        checkInitialized()
        return prefs.contains(key)
    }

    /**
     * 获取所有键值对
     */
    fun getAll(): Map<String, *> {
        checkInitialized()
        return prefs.all
    }

    // ==================== 缓存相关方法 ====================

    /**
     * 保存艺人列表缓存（JSON 字符串）
     */
    fun saveArtistsCache(json: String) {
        putString(KEY_CACHE_ARTISTS, json)
        putLong(KEY_CACHE_ARTISTS_TIME, System.currentTimeMillis())
    }

    /**
     * 获取艺人列表缓存
     */
    fun getArtistsCache(): String? {
        return getString(KEY_CACHE_ARTISTS)
    }

    /**
     * 获取艺人列表缓存时间戳
     */
    fun getArtistsCacheTime(): Long {
        return getLong(KEY_CACHE_ARTISTS_TIME, 0L)
    }

    /**
     * 检查艺人列表缓存是否过期
     * @param cacheDuration 缓存有效期（毫秒），默认1小时
     */
    fun isArtistsCacheExpired(cacheDuration: Long = 3600_000L): Boolean {
        val cacheTime = getArtistsCacheTime()
        return cacheTime == 0L || System.currentTimeMillis() - cacheTime > cacheDuration
    }

    /**
     * 保存系统统计缓存（JSON 字符串）
     */
    fun saveStatsCache(json: String) {
        putString(KEY_CACHE_STATS, json)
        putLong(KEY_CACHE_STATS_TIME, System.currentTimeMillis())
    }

    /**
     * 获取系统统计缓存
     */
    fun getStatsCache(): String? {
        return getString(KEY_CACHE_STATS)
    }

    /**
     * 获取系统统计缓存时间戳
     */
    fun getStatsCacheTime(): Long {
        return getLong(KEY_CACHE_STATS_TIME, 0L)
    }

    /**
     * 检查系统统计缓存是否过期
     * @param cacheDuration 缓存有效期（毫秒），默认1小时
     */
    fun isStatsCacheExpired(cacheDuration: Long = 3600_000L): Boolean {
        val cacheTime = getStatsCacheTime()
        return cacheTime == 0L || System.currentTimeMillis() - cacheTime > cacheDuration
    }

    // ==================== 用户相关方法 ====================

    /**
     * 保存用户 Token
     */
    fun saveUserToken(token: String) {
        putString(KEY_USER_TOKEN, token)
    }

    /**
     * 获取用户 Token
     */
    fun getUserToken(): String? {
        return getString(KEY_USER_TOKEN)
    }

    /**
     * 保存用户 Refresh Token
     */
    fun saveUserRefreshToken(token: String) {
        putString(KEY_USER_REFRESH_TOKEN, token)
    }

    /**
     * 获取用户 Refresh Token
     */
    fun getUserRefreshToken(): String? {
        return getString(KEY_USER_REFRESH_TOKEN)
    }

    /**
     * 保存用户信息
     */
    fun saveUserInfo(userId: String, userName: String, classId: String? = null) {
        putString(KEY_USER_ID, userId)
        putString(KEY_USER_NAME, userName)
        if (classId != null) {
            putString(KEY_CLASS_ID, classId)
        }
        putBoolean(KEY_IS_LOGGED_IN, true)
    }

    /**
     * 保存班级 ID
     */
    fun saveClassId(classId: String) {
        putString(KEY_CLASS_ID, classId)
    }

    /**
     * 获取班级 ID
     */
    fun getClassId(): String? {
        return getString(KEY_CLASS_ID)
    }

    /**
     * 获取用户 ID
     */
    fun getUserId(): String? {
        return getString(KEY_USER_ID)
    }

    /**
     * 获取用户名
     */
    fun getUserName(): String? {
        return getString(KEY_USER_NAME)
    }

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean {
        return getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 获取或生成设备唯一 ID
     */
    fun getDeviceId(): String {
        checkInitialized()
        var deviceId = getString(KEY_DEVICE_ID)
        if (deviceId.isNullOrEmpty()) {
            deviceId = java.util.UUID.randomUUID().toString().replace("-", "")
            putString(KEY_DEVICE_ID, deviceId)
        }
        return deviceId
    }

    /**
     * 获取 App 版本号
     */
    fun getAppVersion(): String {
        return getString(KEY_APP_VERSION, "1.0.0") ?: "1.0.0"
    }

    /**
     * 清除用户信息（退出登录）
     */
    fun clearUserInfo() {
        remove(KEY_USER_TOKEN)
        remove(KEY_USER_REFRESH_TOKEN)
        remove(KEY_USER_ID)
        remove(KEY_USER_NAME)
        putBoolean(KEY_IS_LOGGED_IN, false)
        remove(KEY_LOGIN_TIMESTAMP)
    }

    // ==================== 设置相关方法 ====================

    /**
     * 保存主题模式
     * @param mode 0: 跟随系统, 1: 浅色, 2: 深色
     */
    fun saveThemeMode(mode: Int) {
        putInt(KEY_THEME_MODE, mode)
    }

    /**
     * 获取主题模式
     */
    fun getThemeMode(): Int {
        return getInt(KEY_THEME_MODE, 0) // 默认跟随系统
    }

    /**
     * 保存语言设置
     */
    fun saveLanguage(language: String) {
        putString(KEY_LANGUAGE, language)
    }

    /**
     * 获取语言设置
     */
    fun getLanguage(): String? {
        return getString(KEY_LANGUAGE)
    }

    /**
     * 检查是否首次启动
     */
    fun isFirstLaunch(): Boolean {
        return getBoolean(KEY_FIRST_LAUNCH, true)
    }

    /**
     * 设置首次启动标志
     */
    fun setFirstLaunch(isFirst: Boolean) {
        putBoolean(KEY_FIRST_LAUNCH, isFirst)
    }
}

/**
 * SharedPreferences 委托属性
 * 简化使用方式
 *
 * 使用示例：
 * ```
 * class MyViewModel {
 *     var userToken by PreferencesDelegate.string("user_token", "")
 *     var isLogin by PreferencesDelegate.boolean("is_login", false)
 * }
 * ```
 */
object PreferencesDelegate {

    /**
     * 字符串委托
     */
    fun string(
        key: String,
        defaultValue: String? = null
    ): ReadWriteProperty<Any, String?> =
        object : ReadWriteProperty<Any, String?> {
            override fun getValue(thisRef: Any, property: KProperty<*>): String? {
                PreferencesManager.checkInitialized()
                return PreferencesManager.getString(key, defaultValue)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
                PreferencesManager.putString(key, value)
            }
        }

    /**
     * 整型委托
     */
    fun int(
        key: String,
        defaultValue: Int = 0
    ): ReadWriteProperty<Any, Int> =
        object : ReadWriteProperty<Any, Int> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Int {
                PreferencesManager.checkInitialized()
                return PreferencesManager.getInt(key, defaultValue)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) {
                PreferencesManager.putInt(key, value)
            }
        }

    /**
     * 长整型委托
     */
    fun long(
        key: String,
        defaultValue: Long = 0L
    ): ReadWriteProperty<Any, Long> =
        object : ReadWriteProperty<Any, Long> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Long {
                PreferencesManager.checkInitialized()
                return PreferencesManager.getLong(key, defaultValue)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) {
                PreferencesManager.putLong(key, value)
            }
        }

    /**
     * 浮点型委托
     */
    fun float(
        key: String,
        defaultValue: Float = 0f
    ): ReadWriteProperty<Any, Float> =
        object : ReadWriteProperty<Any, Float> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Float {
                PreferencesManager.checkInitialized()
                return PreferencesManager.getFloat(key, defaultValue)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Float) {
                PreferencesManager.putFloat(key, value)
            }
        }

    /**
     * 布尔型委托
     */
    fun boolean(
        key: String,
        defaultValue: Boolean = false
    ): ReadWriteProperty<Any, Boolean> =
        object : ReadWriteProperty<Any, Boolean> {
            override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
                PreferencesManager.checkInitialized()
                return PreferencesManager.getBoolean(key, defaultValue)
            }

            override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
                PreferencesManager.putBoolean(key, value)
            }
        }
}
