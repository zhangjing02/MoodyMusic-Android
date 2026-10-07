package com.example.moodymusicforandroid.common.network

import com.google.gson.annotations.SerializedName

/**
 * 基础响应实体
 * 所有API响应都应该使用此结构
 */
data class BaseResponse<T>(
    @SerializedName("code")
    override val code: Int,

    @SerializedName("message")
    override val message: String?,

    @SerializedName("data")
    override val data: T?,

    @SerializedName(value = "allow_download", alternate = ["allowDownload", "download_enabled", "downloadEnabled"])
    val allowDownload: Boolean? = null
) : ApiResponse<T> {

    companion object {
        const val SUCCESS = 0
        const val SUCCESS_200 = 200
    }
}
