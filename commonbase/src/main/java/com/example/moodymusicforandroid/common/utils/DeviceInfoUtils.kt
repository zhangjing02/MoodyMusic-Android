package com.example.moodymusicforandroid.common.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import java.util.Locale

/**
 * 设备与客户端环境信息采集工具类
 * 用于在上报、检查更新以及全局请求头中提供设备指纹与版本信息
 */
object DeviceInfoUtils {

    private var cachedVersionName: String? = null
    private var cachedVersionCode: Long? = null
    private var cachedPackageName: String? = null
    private val cachedLocale: String by lazy { Locale.getDefault().toLanguageTag() }

    fun getPlatform(): String = "android"

    fun getVersionName(context: Context? = PreferencesManager.getContext()): String {
        cachedVersionName?.let { return it }
        return try {
            val ctx = context ?: return PreferencesManager.getAppVersion() ?: "1.0"
            val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val name = packageInfo.versionName ?: "1.0"
            cachedVersionName = name
            name
        } catch (_: Exception) {
            PreferencesManager.getAppVersion() ?: "1.0"
        }
    }

    fun getVersionCode(context: Context? = PreferencesManager.getContext()): Long {
        cachedVersionCode?.let { return it }
        return try {
            val ctx = context ?: return 1L
            val packageInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val code = PackageInfoCompat.getLongVersionCode(packageInfo)
            cachedVersionCode = code
            code
        } catch (_: Exception) {
            1L
        }
    }

    fun getPackageName(context: Context? = PreferencesManager.getContext()): String {
        cachedPackageName?.let { return it }
        return try {
            val name = context?.packageName ?: "com.example.moodymusicforandroid"
            cachedPackageName = name
            name
        } catch (_: Exception) {
            "com.example.moodymusicforandroid"
        }
    }

    fun getChannel(): String = "official"

    fun getBrand(): String = Build.BRAND ?: "Unknown"

    fun getModel(): String = Build.MODEL ?: "Unknown"

    fun getOsVersion(): String = Build.VERSION.RELEASE ?: ""

    fun getSdkInt(): Int = Build.VERSION.SDK_INT

    fun getArch(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: ""

    fun getLocale(): String = cachedLocale

    fun getNetworkType(context: Context? = PreferencesManager.getContext()): String {
        return try {
            val ctx = context ?: return "UNKNOWN"
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "UNKNOWN"
            val activeNetwork = cm.activeNetwork ?: return "NONE"
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return "NONE"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "OTHER"
            }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }
}
