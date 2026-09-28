package com.example.moodymusicforandroid.ui.home.voice

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.model.VoicePlayQueueItem
import com.example.moodymusicforandroid.data.model.VoiceTextRequest
import com.example.moodymusicforandroid.ui.player.PlayQueueItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

data class VoiceDispatchResult(
    val recognizedText: String,
    val intentSummary: String,
    val playType: String,
    val playlist: List<PlayQueueItem>,
    val startIndex: Int = 0
)

/**
 * 语音识别与意图处理异常，附带用户已转录的原话文本，方便 UI 实时显示用户说错了什么
 */
class VoiceProcessException(
    val spokenText: String? = null,
    val friendlyMessage: String,
    cause: Throwable? = null
) : Exception(friendlyMessage, cause)

class VoiceAiManager(private val context: Context) {

    private val TAG = "VoiceAiManager"

    /**
     * 云端一步式执行：录音文件 -> 云端网关 (Whisper ASR + LLM NLU + D1 查库) -> 直接返回带绝对 CDN 直链的播放列表
     */
    suspend fun processVoiceAudio(
        audioFile: File,
        onTranscribed: (suspend (String) -> Unit)? = null
    ): Result<VoiceDispatchResult> = withContext(Dispatchers.IO) {
        var recognizedText: String? = null
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext Result.failure(VoiceProcessException(null, "录音文件为空，请长按重新说话"))
            }

            // 构建 Multipart 表单文件直传
            val mediaType = "audio/mp4".toMediaTypeOrNull()
            val requestFile = audioFile.asRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)

            Log.d(TAG, "正在向云端一步式语音网关发送音频 (${audioFile.length()} bytes)...")
            val resp = MoodyApiProvider.apiService.dispatchVoiceAudio(filePart)

            val data = resp.data
            if (resp.code != 200 || data == null) {
                val errMessage = resp.message?.takeIf { it.isNotBlank() } ?: "未能识别到清晰的语音指令，请长按重试"
                return@withContext Result.failure(VoiceProcessException(null, errMessage))
            }

            recognizedText = data.recognizedText
            Log.d(TAG, "云端一步式语音调度成功: text=$recognizedText, intent=${data.intentSummary}, count=${data.playlist.size}")

            // 立即回调转录文本，用于 UI 标题实时刷新回显原话
            if (!recognizedText.isNullOrBlank()) {
                onTranscribed?.invoke(recognizedText)
            }

            if (data.playlist.isEmpty()) {
                return@withContext Result.failure(VoiceProcessException(recognizedText, "未在曲库中找到匹配的内容"))
            }

            // 转换为客户端播放器统一识别的 PlayQueueItem 并确保规范化 URL
            val playQueue = data.playlist.map { item ->
                PlayQueueItem(
                    songTitle = item.songTitle,
                    artistName = item.artistName,
                    albumTitle = item.albumTitle,
                    coverUrl = AppConfig.canonicalizeUrl(item.coverUrl),
                    audioUrl = AppConfig.canonicalizeUrl(item.audioUrl),
                    lrcPath = item.lrcPath?.let { AppConfig.canonicalizeUrl(it) }
                )
            }

            Result.success(
                VoiceDispatchResult(
                    recognizedText = data.recognizedText,
                    intentSummary = data.intentSummary,
                    playType = data.playType,
                    playlist = playQueue,
                    startIndex = data.startIndex
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "处理语音指令失败: ${e.message}", e)
            if (e is VoiceProcessException) {
                Result.failure(e)
            } else {
                val friendlyMsg = when {
                    e is java.net.UnknownHostException -> "网络连接异常，请检查网络设置"
                    e is java.net.SocketTimeoutException -> "语音解析超时，请稍后重试"
                    e is java.io.IOException && e.message?.contains("Failed to connect") == true -> "网络连接失败，请检查网络"
                    !e.message.isNullOrBlank() && !e.message!!.contains("HTTP") -> e.message!!
                    else -> "语音调度异常，请稍后重试"
                }
                Result.failure(VoiceProcessException(recognizedText, friendlyMsg, e))
            }
        }
    }

    /**
     * 云端一步式纯文本调度（支持实时语音转录完成后的秒级直查或文本点歌）
     */
    suspend fun processTextQuery(recognizedText: String): Result<VoiceDispatchResult> = withContext(Dispatchers.IO) {
        try {
            if (recognizedText.isBlank()) {
                return@withContext Result.failure(VoiceProcessException(recognizedText, "请输入有效的音乐指令"))
            }

            Log.d(TAG, "正在向云端一步式语音网关发送文本指令: $recognizedText")
            val resp = MoodyApiProvider.apiService.dispatchVoiceText(VoiceTextRequest(recognizedText))

            val data = resp.data
            if (resp.code != 200 || data == null) {
                val errMessage = resp.message?.takeIf { it.isNotBlank() } ?: "未能识别匹配的音乐，请换个说法"
                return@withContext Result.failure(VoiceProcessException(recognizedText, errMessage))
            }

            if (data.playlist.isEmpty()) {
                return@withContext Result.failure(VoiceProcessException(recognizedText, "未在曲库中找到匹配的内容"))
            }

            val playQueue = data.playlist.map { item ->
                PlayQueueItem(
                    songTitle = item.songTitle,
                    artistName = item.artistName,
                    albumTitle = item.albumTitle,
                    coverUrl = AppConfig.canonicalizeUrl(item.coverUrl),
                    audioUrl = AppConfig.canonicalizeUrl(item.audioUrl),
                    lrcPath = item.lrcPath?.let { AppConfig.canonicalizeUrl(it) }
                )
            }

            Result.success(
                VoiceDispatchResult(
                    recognizedText = data.recognizedText,
                    intentSummary = data.intentSummary,
                    playType = data.playType,
                    playlist = playQueue,
                    startIndex = data.startIndex
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "处理文本意图失败: ${e.message}", e)
            if (e is VoiceProcessException) {
                Result.failure(e)
            } else {
                val friendlyMsg = when {
                    e is java.net.UnknownHostException -> "网络连接异常，请检查网络设置"
                    e is java.net.SocketTimeoutException -> "连接服务器超时，请稍后重试"
                    e is java.io.IOException && e.message?.contains("Failed to connect") == true -> "网络连接失败，请检查网络"
                    !e.message.isNullOrBlank() && !e.message!!.contains("HTTP") -> e.message!!
                    else -> "语音检索异常，请稍后重试"
                }
                Result.failure(VoiceProcessException(recognizedText, friendlyMsg, e))
            }
        }
    }
}

/**
 * 繁体转简体汉字映射器（客户端辅助工具）
 */
object HanziConverter {
    private val t2sMap = mapOf(
        '倫' to '伦', '傑' to '杰', '華' to '华', '劉' to '刘', '德' to '德',
        '學' to '学', '友' to '友', '張' to '张', '麗' to '丽', '君' to '君',
        '詠' to '咏', '琪' to '琪', '葉' to '叶', '蒨' to '倩', '譚' to '谭',
        '麟' to '麟', '陳' to '陈', '奕' to '奕', '迅' to '迅', '愛' to '爱',
        '來' to '来', '後' to '后', '為' to '为', '與' to '与', '時' to '时',
        '開' to '开', '無' to '无', '國' to '国', '語' to '语', '產' to '产',
        '長' to '长', '點' to '点', '變' to '变', '電' to '电', '動' to '动',
        '聽' to '听', '這' to '这', '過' to '过', '寫' to '写', '會' to '会',
        '經' to '经', '關' to '关', '們' to '们', '傳' to '传', '錄' to '录',
        '機' to '机', '觀' to '观', '場' to '场', '實' to '实', '驗' to '验',
        '斷' to '断', '種' to '种', '類' to '类', '難' to '难', '優' to '优',
        '請' to '请', '播' to '播', '放' to '放', '歌' to '歌', '曲' to '曲',
        '專' to '专', '輯' to '辑', '孫' to '孙', '燕' to '燕', '姿' to '姿',
        '想' to '想', '隨' to '随', '緒' to '绪'
    )

    fun toSimplified(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(t2sMap[ch] ?: ch)
        }
        return sb.toString()
    }
}
