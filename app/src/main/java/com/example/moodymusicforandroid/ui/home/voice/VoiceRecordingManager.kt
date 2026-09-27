package com.example.moodymusicforandroid.ui.home.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 麦克风音频录制管理类
 * 负责短音频的高效录制（AAC 格式，采样率 16kHz，码率 64kbps），
 * 生成的文件轻量（每秒约 8KB），与 Groq Whisper-large-v3 完美兼容。
 */
class VoiceRecordingManager(private val context: Context) {

    private val TAG = "VoiceRecordingManager"
    private var recorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var isRecording = false

    fun startRecording(): Boolean {
        try {
            stopRecording() // 确保之前的已被释放

            val cacheDir = context.cacheDir
            val audioFile = File(cacheDir, "voice_command_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(16000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            recorder = newRecorder
            isRecording = true
            Log.d(TAG, "Audio recording started: ${audioFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            cancelRecording()
            return false
        }
    }

    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            val file = currentOutputFile
            if (file != null && file.exists() && file.length() > 500) {
                Log.d(TAG, "Audio recording finished: ${file.absolutePath}, size: ${file.length()} bytes")
                file
            } else {
                Log.w(TAG, "Audio recording file too small or missing")
                file?.delete()
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
            cancelRecording()
            null
        }
    }

    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
        } finally {
            recorder = null
            isRecording = false
            currentOutputFile?.delete()
            currentOutputFile = null
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    fun getMaxAmplitude(): Int {
        if (!isRecording) return 0
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }
}
