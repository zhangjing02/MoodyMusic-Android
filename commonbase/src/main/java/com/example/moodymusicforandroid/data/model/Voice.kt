package com.example.moodymusicforandroid.data.model

import com.google.gson.annotations.SerializedName

/**
 * 语音调度请求实体（纯文本场景）
 */
data class VoiceTextRequest(
    @SerializedName("text") val text: String
)

/**
 * 云端一步式语音调度返回的曲目条目（均已由云端计算并装配公网绝对直链）
 */
data class VoicePlayQueueItem(
    @SerializedName("songTitle") val songTitle: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumTitle") val albumTitle: String,
    @SerializedName("coverUrl") val coverUrl: String,
    @SerializedName("audioUrl") val audioUrl: String,
    @SerializedName("lrcPath") val lrcPath: String? = null
)

/**
 * 云端一步式语音调度响应数据
 */
data class VoiceDispatchData(
    @SerializedName("recognizedText") val recognizedText: String,
    @SerializedName("intentSummary") val intentSummary: String,
    @SerializedName("playType") val playType: String,
    @SerializedName("playlist") val playlist: List<VoicePlayQueueItem>,
    @SerializedName("startIndex") val startIndex: Int = 0
)
