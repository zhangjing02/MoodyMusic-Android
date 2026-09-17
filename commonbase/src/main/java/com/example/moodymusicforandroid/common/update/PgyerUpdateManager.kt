package com.example.moodymusicforandroid.common.update

import android.content.Context
import com.example.moodymusicforandroid.common.utils.DeviceInfoUtils
import com.example.moodymusicforandroid.data.model.AppVersionData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 蒲公英 (Pgyer) 版本在线检测管理器
 * 直接与蒲公英开放平台通信，确保获取最真实、实时的版本数据。
 */
object PgyerUpdateManager {
    private const val PGYER_API_KEY = "48ceaf75791c09d36fdb364b8f1fd314"
    private const val PGYER_APP_KEY = "e127ba6898e9d160519e7fb0b0dc3bde"
    private const val PGYER_CHECK_URL = "https://www.pgyer.com/apiv2/app/check"

    private val _versionState = MutableStateFlow<AppVersionData?>(null)
    val versionState: StateFlow<AppVersionData?> = _versionState.asStateFlow()

    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 语义化版本名称比对算法 (Semantic Versioning)
     * 规则：
     * 1. 严格只比对版本名称 (Version Name，如 "1.0.1" vs "1.0.0")，完全忽略构建号 (Build)
     * 2. 自动清理 'v'/'V' 前缀
     * 3. 逐段对比，按自然数大小排序 (10 > 2)
     * 
     * 返回值：
     *   > 0 : v1 > v2 (例如蒲公英版本高于当前本地版本)
     *   < 0 : v1 < v2
     *   = 0 : v1 == v2 (两版本相同)
     */
    fun compareVersionNames(v1: String?, v2: String?): Int {
        if (v1.isNullOrBlank() && v2.isNullOrBlank()) return 0
        if (v1.isNullOrBlank()) return -1
        if (v2.isNullOrBlank()) return 1

        val clean = { s: String -> s.trim().removePrefix("v").removePrefix("V") }
        val s1 = clean(v1)
        val s2 = clean(v2)

        if (s1 == s2) return 0

        val parts1 = s1.split(Regex("[.\\-_+]")).filter { it.isNotEmpty() }
        val parts2 = s2.split(Regex("[.\\-_+]")).filter { it.isNotEmpty() }
        val maxLen = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLen) {
            val p1 = parts1.getOrNull(i)
            val p2 = parts2.getOrNull(i)

            if (p1 == null) {
                val n2 = p2?.toIntOrNull() ?: 0
                return if (n2 > 0) -1 else 0
            }
            if (p2 == null) {
                val n1 = p1.toIntOrNull() ?: 0
                return if (n1 > 0) 1 else 0
            }

            val num1 = p1.toIntOrNull()
            val num2 = p2.toIntOrNull()

            if (num1 != null && num2 != null) {
                if (num1 != num2) {
                    return if (num1 > num2) 1 else -1
                }
            } else {
                val cmp = p1.compareTo(p2, ignoreCase = true)
                if (cmp != 0) return if (cmp > 0) 1 else -1
            }
        }
        return 0
    }

    /**
     * 从蒲公英直接拉取真实版本元数据并严格按照版本名称比对
     */
    suspend fun checkUpdate(context: Context): AppVersionData = withContext(Dispatchers.IO) {
        val currentVersionName = DeviceInfoUtils.getVersionName(context)

        val formBody = FormBody.Builder()
            .add("_api_key", PGYER_API_KEY)
            .add("appKey", PGYER_APP_KEY)
            .build()

        val request = Request.Builder()
            .url(PGYER_CHECK_URL)
            .post(formBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("蒲公英通信异常 HTTP " + response.code)
        }

        val bodyString = response.body?.string() ?: throw IllegalStateException("空响应体")
        val json = JSONObject(bodyString)
        val code = json.optInt("code", -1)

        if (code == 0 && json.has("data")) {
            val data = json.getJSONObject("data")
            val pgyerVersion = data.optString("buildVersion", "1.0.0")
            val pgyerBuildNumber = data.optInt("buildBuildVersion", 1)
            val downloadUrl = data.optString("downloadURL", "")
            val mirrorUrl = data.optString("buildShortcutUrl", "https://www.pgyer.com/yinxin-android")
            val fileSize = data.optLong("buildFileSize", 0L)
            val fileSizeMb = if (fileSize > 0) {
                String.format("%.1f MB", fileSize / (1024f * 1024f))
            } else "22.6 MB"

            val updateNotes = data.optString("buildUpdateDescription", "").ifBlank {
                data.optString("buildDescription", "常规体验优化与性能提升")
            }
            val isForce = data.optBoolean("needForceUpdate", false)

            // 核心比对：只有当蒲公英上的版本名称严格大于当前本地版本名称时，才判定为有新版本
            val hasUpdate = compareVersionNames(pgyerVersion, currentVersionName) > 0

            val result = AppVersionData(
                hasUpdate = hasUpdate,
                isForceUpdate = hasUpdate && isForce,
                isIgnoredAllowed = !isForce,
                isSilentUpdate = false,
                versionCode = pgyerBuildNumber,
                versionName = pgyerVersion,
                downloadUrl = downloadUrl,
                downloadUrlMirror = mirrorUrl,
                packageSize = fileSize,
                packageSizeStr = fileSizeMb,
                releaseNotes = updateNotes,
                title = if (hasUpdate) "发现新版本 v" + pgyerVersion else "当前已是最新版本"
            )
            _versionState.value = result
            result
        } else {
            val msg = json.optString("message", "获取版本信息失败")
            throw IllegalStateException(msg)
        }
    }

    /**
     * 异步静默触发版本检测（供极光推送透传接收器、抽屉展开事件等在后台直接触发）
     */
    fun triggerSilentCheck(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                checkUpdate(context.applicationContext)
            } catch (_: Exception) {
                // 静默处理，网络或接口故障不打扰用户正常体验
            }
        }
    }
}
