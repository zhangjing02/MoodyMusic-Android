package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

/**
 * 客户端版本检测与在线更新实体
 */
data class AppVersionData(
    @SerializedName("has_update")
    val hasUpdate: Boolean = false,

    @SerializedName("is_force_update")
    val isForceUpdate: Boolean = false,

    @SerializedName("is_ignored_allowed")
    val isIgnoredAllowed: Boolean = true,

    @SerializedName("is_silent_update")
    val isSilentUpdate: Boolean = false,

    @SerializedName("version_code")
    val versionCode: Int = 1,

    @SerializedName("version_name")
    val versionName: String = "1.0",

    @SerializedName("min_version_code")
    val minVersionCode: Int = 1,

    @SerializedName("min_version_name")
    val minVersionName: String = "1.0",

    @SerializedName("download_url")
    val downloadUrl: String = "",

    @SerializedName("download_url_mirror")
    val downloadUrlMirror: String? = null,

    @SerializedName("package_size")
    val packageSize: Long = 0L,

    @SerializedName("package_size_str")
    val packageSizeStr: String = "0.0 MB",

    @SerializedName("package_md5")
    val packageMd5: String? = null,

    @SerializedName("title")
    val title: String = "",

    @SerializedName("release_notes")
    val releaseNotes: String = "",

    @SerializedName("publish_time")
    val publishTime: String = ""
) : Serializable
