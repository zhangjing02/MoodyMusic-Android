package com.example.moodymusicforandroid.ui.home.voice

import android.content.Context
import android.util.Log
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.ui.player.PlayQueueItem
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress
import java.io.File
import java.util.concurrent.TimeUnit
import com.example.moodymusicforandroid.data.local.db.MoodyDatabase
import com.example.moodymusicforandroid.data.local.db.ArtistEntity
import com.example.moodymusicforandroid.data.manager.ArtistManager

/**
 * 繁体转简体汉字映射器
 * 解决 Groq Whisper / LLM 在语音输入时默认输出繁体字（例如「周杰倫」），
 * 而 D1 曲库中歌手与歌曲采用简体字（「周杰伦」）导致的 SQL 匹配失败问题。
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
        '態' to '态', '響' to '响', '應' to '应', '調' to '调', '轉' to '转',
        '遙' to '遥', '願' to '愿', '義' to '义', '務' to '务', '標' to '标',
        '遠' to '远', '選' to '选', '邊' to '边', '處' to '处', '風' to '风',
        '頭' to '头', '門' to '门', '間' to '间', '題' to '题', '讓' to '让',
        '識' to '识', '設' to '设', '緊' to '紧', '現' to '现', '規' to '规',
        '視' to '视', '藝' to '艺', '價' to '价', '證' to '证', '獨' to '独',
        '劇' to '剧', '歲' to '岁', '備' to '备', '齊' to '齐', '秦' to '秦',
        '蘇' to '苏', '芮' to '芮', '姜' to '姜', '恒' to '恒', '恆' to '恒',
        '趙' to '赵', '王' to '王', '黃' to '黄', '鄭' to '郑', '凱' to '凯',
        '邰' to '邰', '啟' to '启', '賢' to '贤', '鴻' to '鸿', '許' to '许',
        '靜' to '静', '曉' to '晓', '萬' to '万', '芳' to '芳', '樺' to '桦',
        '楊' to '杨', '千' to '千', '嬅' to '嬅', '兒' to '儿', '謝' to '谢',
        '霆' to '霆', '鋒' to '锋', '冠' to '冠', '希' to '希', '樂' to '乐',
        '麥' to '麦', '浚' to '浚', '龍' to '龙', '鄧' to '邓', '欣' to '欣',
        '衛' to '卫', '蘭' to '兰', '吳' to '吴', '寶' to '宝', '儀' to '仪',
        '廣' to '广', '羅' to '罗', '達' to '达', '濤' to '涛', '雲' to '云',
        '輝' to '辉', '銘' to '铭', '溫' to '温', '鐘' to '钟', '鎮' to '镇',
        '請' to '请', '播' to '播', '放' to '放', '歌' to '歌', '曲' to '曲',
        '專' to '专', '輯' to '辑', '孫' to '孙', '燕' to '燕', '姿' to '姿',
        '蕭' to '萧', '騰' to '腾', '謙' to '谦', '榮' to '荣', '浩' to '浩',
        '駒' to '驹', '強' to '强', '健' to '健', '陰' to '阴', '陽' to '阳',
        '單' to '单', '雙' to '双', '紅' to '红', '綠' to '绿', '藍' to '蓝',
        '夢' to '梦', '話' to '话', '說' to '说', '傷' to '伤', '淚' to '泪',
        '離' to '离', '歸' to '归', '別' to '别', '約' to '约', '驚' to '惊',
        '嘆' to '叹', '號' to '号', '錯' to '错', '戀' to '恋', '團' to '团',
        '隊' to '队', '熱' to '热', '飛' to '飞', '鳥' to '鸟', '歡' to '欢',
        '見' to '见', '節' to '节', '跡' to '迹', '簡' to '简', '楓' to '枫',
        '親' to '亲', '東' to '东', '西' to '西', '南' to '南', '北' to '北',
        '發' to '发', '財' to '财', '買' to '买', '賣' to '卖', '車' to '车',
        '錢' to '钱', '銀' to '银', '鐵' to '铁', '銅' to '铜', '鋼' to '钢',
        '聲' to '声', '詩' to '诗', '詞' to '词', '書' to '书', '畫' to '画',
        '筆' to '笔', '紙' to '纸', '線' to '线', '網' to '网', '絡' to '络',
        '聯' to '联', '輕' to '轻', '體' to '体', '員' to '员', '際' to '际',
        '條' to '条', '級' to '级', '數' to '数', '據' to '据', '庫' to '库',
        '訊' to '讯', '碼' to '码', '驟' to '骤', '結' to '结', '終' to '终',
        '於' to '于', '總' to '总', '當' to '当', '側' to '侧', '臺' to '台',
        '灣' to '湾', '搖' to '摇', '滾' to '滚', '謠' to '谣', '純' to '纯',
        '協' to '协', '況' to '况', '帶' to '带', '歷' to '历', '憶' to '忆',
        '懷' to '怀', '念' to '念', '隨' to '随', '緒' to '绪'
    )

    private val s2tMap = t2sMap.entries.associate { (k, v) -> v to k }

    fun toSimplified(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(t2sMap[ch] ?: ch)
        }
        return sb.toString()
    }

    fun toTraditional(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(s2tMap[ch] ?: ch)
        }
        return sb.toString()
    }
}

data class MusicIntent(
    @SerializedName("intent") val intent: String = "song", // "song", "album", "artist", "mood"
    @SerializedName("artist") val artist: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("album") val album: String? = null,
    @SerializedName("reply") val reply: String? = null
)

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
    private val gson = Gson()

    // 语音输入高频同音/谐音曲目先验映射表
    private val commonSongAliases = mapOf(
        "白话铃" to "白桦林", "白话林" to "白桦林", "白华林" to "白桦林", "百花林" to "白桦林",
        "那些花" to "那些花儿", "那些花二" to "那些花儿",
        "气里香" to "七里香", "七里箱" to "七里香",
        "东风坡" to "东风破",
        "平凡直路" to "平凡之路",
        "生如雪花" to "生如夏花",
        "蓝脸花" to "蓝莲花"
    )

    // 语音输入高频同音/谐音歌手先验映射表
    private val commonArtistAliases = mapOf(
        "朴素" to "朴树", "朴书" to "朴树", "璞树" to "朴树",
        "周花键" to "周华健", "周华建" to "周华健", "周化健" to "周华健",
        "李宗胜" to "李宗盛", "李中盛" to "李宗盛",
        "陈易迅" to "陈奕迅", "陈异讯" to "陈奕迅", "陈一迅" to "陈奕迅",
        "张雪友" to "张学友", "张学有" to "张学友",
        "汪蜂" to "汪峰", "汪风" to "汪峰",
        "许微" to "许巍", "许伟" to "许巍",
        "孙燕子" to "孙燕姿",
        "邓丽均" to "邓丽君",
        "王妃" to "王菲",
        "莫问蔚" to "莫文蔚", "莫文微" to "莫文蔚",
        "田福珍" to "田馥甄", "田馥珍" to "田馥甄",
        "陶吉吉" to "陶喆", "陶哲" to "陶喆",
        "五百" to "伍佰", "伍百" to "伍佰",
        "崔建" to "崔健",
        "梁静儒" to "梁静茹",
        "林俊捷" to "林俊杰",
        "周洁伦" to "周杰伦",
        "刘得华" to "刘德华",
        "童安哥" to "童安格",
        "童安歌" to "童安格"
    )

    // 华语经典名曲与歌手先验强关联（解决用户只说歌名时的无缝专辑串联与精准定位）
    private val famousSongToArtist = mapOf(
        "白桦林" to "朴树",
        "生如夏花" to "朴树",
        "那些花儿" to "朴树",
        "平凡之路" to "朴树",
        "NEW BOY" to "朴树",
        "其实你不懂我的心" to "童安格",
        "明天你是否依然爱我" to "童安格",
        "让生命等候" to "童安格",
        "忘不了" to "童安格",
        "把根留住" to "童安格",
        "七里香" to "周杰伦",
        "晴天" to "周杰伦",
        "青花瓷" to "周杰伦",
        "东风破" to "周杰伦",
        "告白气球" to "周杰伦",
        "十年" to "陈奕迅",
        "红豆" to "王菲",
        "吻别" to "张学友",
        "冰雨" to "刘德华",
        "忘情水" to "刘德华",
        "朋友" to "周华健",
        "山丘" to "李宗盛",
        "蓝莲花" to "许巍"
    )

    private val ipv4PreferredDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4List = addresses.filterIsInstance<Inet4Address>()
                if (ipv4List.isNotEmpty()) ipv4List else addresses
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dns(ipv4PreferredDns)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * 完整端到端执行：录音文件 -> Groq Whisper 听音识别 -> Groq LLM 意图解析 -> D1 曲库检索 -> 组装播放队列
     */
    suspend fun processVoiceAudio(
        audioFile: File,
        onTranscribed: (suspend (String) -> Unit)? = null
    ): Result<VoiceDispatchResult> = withContext(Dispatchers.IO) {
        var recognizedText: String? = null
        try {
            val apiKey = AppConfig.groqApiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(VoiceProcessException(null, "未配置语音服务密钥"))
            }

            // 1. 调用 Groq Whisper 语音转录 (ASR)
            recognizedText = transcribeAudioWithGroq(audioFile, apiKey)
            if (recognizedText.isBlank()) {
                return@withContext Result.failure(VoiceProcessException(null, "未能识别到清晰的语音，请长按说话"))
            }
            Log.d(TAG, "Groq Whisper 识别文本: $recognizedText")

            // 立即回调转录文本，用于 UI 标题实时刷新回显原话
            onTranscribed?.invoke(recognizedText)

            // 2. 调用 Groq LLM 进行音乐意图解析
            val intent = parseMusicIntentWithGroq(recognizedText, apiKey)
            Log.d(TAG, "Groq LLM 解析意图: $intent")

            // 3. 检索曲库并组装播放队列
            val result = dispatchIntentToPlaylist(intent, recognizedText)
            if (result == null || result.playlist.isEmpty()) {
                val notFoundMsg = if (!intent.title.isNullOrBlank()) {
                    "曲库中暂未收录歌曲《${intent.title}》"
                } else if (!intent.artist.isNullOrBlank()) {
                    "曲库中暂未收录歌手「${intent.artist}」的相关曲目"
                } else if (!intent.album.isNullOrBlank()) {
                    "曲库中暂未收录专辑《${intent.album}》"
                } else {
                    "未在曲库中找到匹配的内容"
                }
                return@withContext Result.failure(VoiceProcessException(recognizedText, notFoundMsg))
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "处理语音指令失败: ${e.message}", e)
            if (e is VoiceProcessException) {
                Result.failure(e)
            } else {
                val friendlyMsg = when {
                    e is java.net.UnknownHostException -> "网络连接异常，请检查网络设置"
                    e is java.net.SocketTimeoutException -> "连接服务器超时，请稍后重试"
                    e is java.io.IOException && e.message?.contains("Failed to connect") == true -> "网络连接失败，请检查网络"
                    !e.message.isNullOrBlank() && !e.message!!.contains("Groq") && !e.message!!.contains("HTTP") -> e.message!!
                    else -> "语音处理出现异常，请稍后重试"
                }
                Result.failure(VoiceProcessException(recognizedText, friendlyMsg, e))
            }
        }
    }

    /**
     * 直接通过纯文本进行意图分析与曲库匹配（支持实时语音转录完成后的秒级直查）
     */
    suspend fun processTextQuery(recognizedText: String): Result<VoiceDispatchResult> = withContext(Dispatchers.IO) {
        try {
            val apiKey = AppConfig.groqApiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(VoiceProcessException(recognizedText, "未配置语音服务密钥"))
            }

            // 1. 调用 Groq LLM 进行音乐意图解析
            val intent = parseMusicIntentWithGroq(recognizedText, apiKey)
            Log.d(TAG, "Groq LLM 解析文本意图: $intent")

            // 2. 检索曲库并组装播放队列
            val result = dispatchIntentToPlaylist(intent, recognizedText)
            if (result == null || result.playlist.isEmpty()) {
                val notFoundMsg = if (!intent.title.isNullOrBlank()) {
                    "曲库中暂未收录歌曲《${intent.title}》"
                } else if (!intent.artist.isNullOrBlank()) {
                    "曲库中暂未收录歌手「${intent.artist}」的相关曲目"
                } else if (!intent.album.isNullOrBlank()) {
                    "曲库中暂未收录专辑《${intent.album}》"
                } else {
                    "未在曲库中找到匹配的内容"
                }
                return@withContext Result.failure(VoiceProcessException(recognizedText, notFoundMsg))
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "处理文本意图失败: ${e.message}", e)
            if (e is VoiceProcessException) {
                Result.failure(e)
            } else {
                val friendlyMsg = when {
                    e is java.net.UnknownHostException -> "网络连接异常，请检查网络设置"
                    e is java.net.SocketTimeoutException -> "连接服务器超时，请稍后重试"
                    e is java.io.IOException && e.message?.contains("Failed to connect") == true -> "网络连接失败，请检查网络"
                    !e.message.isNullOrBlank() && !e.message!!.contains("Groq") && !e.message!!.contains("HTTP") -> e.message!!
                    else -> "语音检索出现异常，请稍后重试"
                }
                Result.failure(VoiceProcessException(recognizedText, friendlyMsg, e))
            }
        }
    }

    /**
     * 步骤一：Groq Whisper-large-v3 极速转录
     */
    private fun transcribeAudioWithGroq(audioFile: File, apiKey: String): String {
        val whisperPrompt = "华语流行音乐点歌系统。常见歌手与经典名曲：朴树、白桦林、生如夏花、那些花儿、平凡之路、周杰伦、晴天、七里香、青花瓷、花海、陈奕迅、十年、富士山下、张学友、吻别、刘德华、冰雨、王菲、红豆、李宗盛、山丘、莫文蔚、孙燕姿、遇见、林俊杰、江南、五月天、伍佰、挪威的森林、罗大佑、童年、崔健、许巍、蓝莲花、汪峰、民谣、摇滚、流行金曲点播。"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
            )
            .addFormDataPart("model", "whisper-large-v3")
            .addFormDataPart("language", "zh")
            .addFormDataPart("prompt", whisperPrompt)
            .build()

        val request = Request.Builder()
            .url("${AppConfig.groqApiBaseUrl}audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Groq Whisper 请求失败 HTTP ${response.code}: $errBody")
                throw IllegalStateException("Groq 语音识别服务异常 (${response.code})")
            }
            val jsonStr = response.body?.string() ?: ""
            val jsonObj = gson.fromJson(jsonStr, JsonObject::class.java)
            return jsonObj.get("text")?.asString?.trim() ?: ""
        }
    }

    /**
     * 步骤二：Groq LLM (qwen/qwen3.8-27b) 音乐意图解析与语音识别智能纠错
     */
    private fun parseMusicIntentWithGroq(recognizedText: String, apiKey: String): MusicIntent {
        val systemPrompt = """
            你是一个专业的华语流行音乐意图分析与语音纠错助手。用户会通过语音点歌，说出想听的歌曲、歌手、专辑或情绪。
            请将用户的输入精准解析为 JSON 格式，字段如下：
            {
              "intent": "song" | "album" | "artist" | "mood",
              "artist": "歌手名字或null",
              "title": "歌曲名字或null",
              "album": "专辑名字或null",
              "reply": "给用户的一句话友好回复"
            }
            【核心规则】：
            1. 音乐库统一采用中国大陆【简体中文】存储，输出的 artist、title、album 必须全部转换为标准简体中文（例如：周杰伦、陈奕迅、刘德华、张学友、朋友、告白气球、朴树、白桦林），严禁输出繁体字。
            2. 【音乐语音同音字/近音字智能纠错（极重要）】：
               用户的输入来自实时语音听写，极易产生同音字、谐音字、错别字或转录偏误。你拥有深厚的华语音乐知识库，必须主动结合常识纠正：
               - “白话铃”、“白话林”、“白华林”、“百花林” -> 纠错为歌曲《白桦林》（歌手：朴树）
               - “朴素”、“朴书” -> 纠错为歌手“朴树”（例如“朴素的白话铃” -> artist="朴树", title="白桦林"）
               - “周花键”、“周华建” -> 纠错为“周华健”
               - “那些花”、“那些花二” -> 纠错为歌曲《那些花儿》（歌手：朴树）
               - “气里香”、“七里箱” -> 纠错为《七里香》（歌手：周杰伦）
               - “东风坡” -> 纠错为《东风破》（歌手：周杰伦）
               - “平凡直路” -> 纠错为《平凡之路》（歌手：朴树）
               - “陈异讯”、“陈易迅” -> 纠错为“陈奕迅”
               - “生如雪花” -> 纠错为《生如夏花》（歌手：朴树）
               - “蓝脸花” -> 纠错为《蓝莲花》（歌手：许巍）
               - 若用户只说了知名歌曲名（如“白桦林”、“晴天”、“十年”、“吻别”），如果该曲目有公认的原唱知名歌手，请将 artist 自动补全填充（例如“白桦林”自动填 artist="朴树"），方便系统直接拉取歌手专辑与曲库！
            3. 严格只输出标准 JSON 字符串，绝不要附加任何解释或 Markdown 代码块标签。
        """.trimIndent()

        val payload = mapOf(
            "model" to "qwen/qwen3.8-27b",
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to recognizedText)
            ),
            "temperature" to 0.1
        )

        val request = Request.Builder()
            .url("${AppConfig.groqApiBaseUrl}chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "Groq LLM 请求失败 HTTP ${response.code}: $errBody")
                throw IllegalStateException("Groq 意图理解服务异常 (${response.code})")
            }
            val jsonStr = response.body?.string() ?: ""
            val jsonObj = gson.fromJson(jsonStr, JsonObject::class.java)
            val content = jsonObj.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString ?: ""

            // 清理可能包裹的 markdown 代码块
            val cleanJson = content
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val rawIntent = gson.fromJson(cleanJson, MusicIntent::class.java)
            return rawIntent.copy(
                artist = rawIntent.artist?.let { HanziConverter.toSimplified(it).trim() },
                title = rawIntent.title?.let { HanziConverter.toSimplified(it).trim() },
                album = rawIntent.album?.let { HanziConverter.toSimplified(it).trim() }
            )
        }
    }

    /**
     * 智能解析并对齐歌手：
     * 1. 结合 commonArtistAliases 纠正语音同音近音（如 朴素 -> 朴树）
     * 2. 先经 HanziConverter 转为简体中文（如 周杰倫 -> 周杰伦）
     * 3. 在本地 Room 169 位歌手名录中精准匹配或模糊匹配，获得标准简体名与 artistId
     */
    private suspend fun resolveArtist(rawArtist: String?): Pair<String, String?>? {
        if (rawArtist.isNullOrBlank()) return null
        val simplified = HanziConverter.toSimplified(rawArtist).trim()
        val correctedArtist = commonArtistAliases[simplified] ?: simplified

        val cached = ArtistManager.artists.value
        val matchedFromCache = cached.firstOrNull { artist ->
            val artistSimp = HanziConverter.toSimplified(artist.name)
            artistSimp.equals(correctedArtist, ignoreCase = true) ||
            artist.name.equals(rawArtist, ignoreCase = true) ||
            correctedArtist.contains(artistSimp) ||
            artistSimp.contains(correctedArtist)
        }
        if (matchedFromCache != null) {
            return Pair(matchedFromCache.name, matchedFromCache.id)
        }

        val allEntities = try {
            MoodyDatabase.getInstance(context).artistDao().getAllArtists()
        } catch (e: Exception) {
            Log.w(TAG, "从本地 Room 查询歌手失败", e)
            emptyList()
        }

        val matchedEntity = allEntities.firstOrNull { entity ->
            val entitySimp = HanziConverter.toSimplified(entity.name)
            entitySimp.equals(correctedArtist, ignoreCase = true) ||
            entity.name.equals(rawArtist, ignoreCase = true) ||
            correctedArtist.contains(entitySimp) ||
            entitySimp.contains(correctedArtist)
        }

        if (matchedEntity != null) {
            return Pair(matchedEntity.name, matchedEntity.id)
        }

        // 容错：首字相同且长度为 2 的模糊候选（例如 "朴素" vs "朴树"）
        if (correctedArtist.length == 2) {
            val fuzzyArt = cached.firstOrNull { it.name.length == 2 && it.name.first() == correctedArtist.first() }
            if (fuzzyArt != null) {
                return Pair(fuzzyArt.name, fuzzyArt.id)
            }
            val fuzzyEntity = allEntities.firstOrNull { it.name.length == 2 && it.name.first() == correctedArtist.first() }
            if (fuzzyEntity != null) {
                return Pair(fuzzyEntity.name, fuzzyEntity.id)
            }
        }

        return Pair(correctedArtist, null)
    }

    /**
     * 步骤三：分流调度与曲库组装
     */
    private suspend fun dispatchIntentToPlaylist(intent: MusicIntent, recognizedText: String): VoiceDispatchResult? {
        return when (intent.intent.lowercase()) {
            "album" -> handleAlbumIntent(intent, recognizedText)
            "artist" -> handleArtistIntent(intent, recognizedText)
            "song" -> handleSongIntent(intent, recognizedText)
            else -> handleMoodOrFallbackIntent(intent, recognizedText)
        }
    }

    /**
     * 场景一：指定歌曲（精准点歌）
     */
    private suspend fun handleSongIntent(intent: MusicIntent, recognizedText: String): VoiceDispatchResult? {
        var rawArtist = intent.artist?.trim()
        var rawTitle = intent.title?.trim() ?: ""

        // 1. 同音词/错别字先验纠偏 (如 "白话铃" -> "白桦林")
        val simpTitle = HanziConverter.toSimplified(rawTitle)
        val correctedTitle = commonSongAliases[simpTitle] ?: simpTitle

        // 2. 检查歌手纠偏 (如 "朴素" -> "朴树")
        if (!rawArtist.isNullOrBlank()) {
            val simpArtist = HanziConverter.toSimplified(rawArtist)
            rawArtist = commonArtistAliases[simpArtist] ?: simpArtist
        }

        // 3. 若歌曲包含歌手名（例如 title="朴树白桦林" 或 title="朴树的白桦林"），剥离并提取歌手
        if (rawArtist.isNullOrBlank()) {
            val cachedArtists = ArtistManager.artists.value
            for (artist in cachedArtists) {
                val artName = HanziConverter.toSimplified(artist.name)
                if (correctedTitle.startsWith(artName)) {
                    rawArtist = artist.name
                    rawTitle = correctedTitle.removePrefix(artName).trimStart('的', ' ', '《', '—', '-')
                    break
                }
            }
        }

        // 4. 判断用户在原声语音中是否真实提及了该歌手
        val userMentionedArtist = !rawArtist.isNullOrBlank() && (
            recognizedText.contains(rawArtist, ignoreCase = true) ||
            (rawArtist.length >= 2 && recognizedText.contains(rawArtist.takeLast(2), ignoreCase = true))
        )

        // 若大模型给出的歌手用户并未提及（大模型自作主张的幻觉），且歌曲属于知名华语经典（如《其实你不懂我的心》），纠正为原唱！
        val famousArtist = famousSongToArtist[correctedTitle] ?: famousSongToArtist[rawTitle]
        if (!famousArtist.isNullOrBlank() && !userMentionedArtist) {
            Log.d(TAG, "用户未明示歌手，纠正大模型幻觉: 《$correctedTitle》 -> 真实歌手: $famousArtist (原模型推断: $rawArtist)")
            rawArtist = famousArtist
        } else if (rawArtist.isNullOrBlank() && !famousArtist.isNullOrBlank()) {
            rawArtist = famousArtist
            Log.d(TAG, "从华语名曲库自动关联歌手: 《$correctedTitle》 -> 歌手: $rawArtist")
        }

        val (targetArtist, artistId) = if (!rawArtist.isNullOrBlank()) {
            resolveArtist(rawArtist) ?: Pair(HanziConverter.toSimplified(rawArtist), null)
        } else {
            Pair(null, null)
        }
        val targetTitle = HanziConverter.toSimplified(rawTitle.ifBlank { correctedTitle })

        val api = MoodyApiProvider.apiService
        Log.d(TAG, "正在精准点歌: artist=$targetArtist, artistId=$artistId, title=$targetTitle")

        // 优先按歌手检索完整专辑
        val resp = if (!artistId.isNullOrBlank()) {
            api.getSongsByArtist(artistId = artistId)
        } else if (!targetArtist.isNullOrBlank()) {
            api.getSongsByArtist(artist = targetArtist)
        } else {
            null
        }
        val artistsList = resp?.data ?: emptyList()

        var matchedSong: PlayQueueItem? = null

        for (artistObj in artistsList) {
            val artName = artistObj.name
            for (album in artistObj.albums) {
                val albTitle = album.title
                val coverUrl = resolveUrl(album.cover)
                for (s in album.songs) {
                    val audioUrl = resolveAudioUrl(s.path)
                    if (audioUrl.isBlank()) continue

                    val simpSongTitle = HanziConverter.toSimplified(s.title)
                    val tradTargetTitle = HanziConverter.toTraditional(targetTitle)
                    if (simpSongTitle.equals(targetTitle, ignoreCase = true) ||
                        simpSongTitle.contains(targetTitle, ignoreCase = true) ||
                        targetTitle.contains(simpSongTitle, ignoreCase = true) ||
                        s.title.equals(tradTargetTitle, ignoreCase = true) ||
                        s.title.contains(tradTargetTitle, ignoreCase = true)
                    ) {
                        matchedSong = PlayQueueItem(
                            songTitle = s.title,
                            artistName = artName,
                            albumTitle = albTitle,
                            coverUrl = coverUrl,
                            audioUrl = audioUrl,
                            lrcPath = s.lrcPath
                        )
                        break
                    }
                }
                if (matchedSong != null) break
            }
            if (matchedSong != null) break
        }

        // 如果在指定歌手中没找到，或者未指定歌手，尝试全局搜索
        if (matchedSong == null && targetTitle.isNotBlank()) {
            // 尝试 1：简体搜索
            var searchResp = try { api.search(targetTitle) } catch (_: Exception) { null }
            var searchSongs = searchResp?.data?.songs ?: emptyList()

            // 尝试 2：繁体搜索兜底 (因为 D1 曲库中不少歌曲为繁体，如「白樺林」)
            val tradTitle = HanziConverter.toTraditional(targetTitle)
            if (searchSongs.isEmpty() && tradTitle != targetTitle) {
                Log.d(TAG, "简体搜索未命中，尝试繁体曲库兜底搜索: $tradTitle")
                searchResp = try { api.search(tradTitle) } catch (_: Exception) { null }
                searchSongs = searchResp?.data?.songs ?: emptyList()
            }

            // 尝试 3：去除后缀修饰词（如“的歌”、“歌曲”、“完整版”）兜底搜索
            if (searchSongs.isEmpty()) {
                val stripped = targetTitle.replace("歌曲", "").replace("完整版", "").replace("原唱", "").trim()
                if (stripped.length >= 2 && stripped != targetTitle) {
                    searchResp = try { api.search(stripped) } catch (_: Exception) { null }
                    searchSongs = searchResp?.data?.songs ?: emptyList()
                    if (searchSongs.isEmpty()) {
                        val tradStripped = HanziConverter.toTraditional(stripped)
                        if (tradStripped != stripped) {
                            searchResp = try { api.search(tradStripped) } catch (_: Exception) { null }
                            searchSongs = searchResp?.data?.songs ?: emptyList()
                        }
                    }
                }
            }

            if (searchSongs.isNotEmpty()) {
                val first = searchSongs.firstOrNull { !it.filePath.isNullOrBlank() }
                if (first != null) {
                    val rawFilePath = first.filePath ?: ""
                    // 优先从 artistId 查真实歌手
                    var resolvedArtName: String? = first.artistId?.let { aId ->
                        ArtistManager.artists.value.firstOrNull { it.id == aId.toString() }?.name
                    }

                    // 若 artistId 缺失，从 filePath 智能解析真实歌手 (例如: .../music/童安格/... 或 .../music/蔡琴/...)
                    if (resolvedArtName.isNullOrBlank() && rawFilePath.isNotBlank()) {
                        try {
                            val pathSegments = rawFilePath.split('/')
                            val musicIdx = pathSegments.indexOf("music")
                            if (musicIdx != -1 && musicIdx + 1 < pathSegments.size) {
                                val folderArtist = java.net.URLDecoder.decode(pathSegments[musicIdx + 1], "UTF-8")
                                if (folderArtist.isNotBlank() && !folderArtist.contains('.')) {
                                    resolvedArtName = folderArtist
                                }
                            }
                        } catch (_: Exception) {}
                    }

                    // 兜底：若仍未解析出，且用户亲口说了歌手才用 targetArtist，否则决不使用大模型幻觉脑补的名字
                    val finalArtistName = resolvedArtName ?: (if (userMentionedArtist) targetArtist else null) ?: "华语精选"

                    matchedSong = PlayQueueItem(
                        songTitle = HanziConverter.toSimplified(first.title),
                        artistName = finalArtistName,
                        albumTitle = "热门单曲",
                        coverUrl = "",
                        audioUrl = resolveAudioUrl(rawFilePath),
                        lrcPath = first.lrcPath
                    )
                }
            }
        }

        if (matchedSong == null) return null

        // 点播单曲场景：播放列表仅保留此单首歌曲（与歌手专属电台的 20 首区分开）
        val finalQueue = listOf(matchedSong)
        val summary = "已为您播放：${matchedSong.artistName} -《${matchedSong.songTitle}》"

        return VoiceDispatchResult(
            recognizedText = recognizedText,
            intentSummary = summary,
            playType = "song",
            playlist = finalQueue,
            startIndex = 0
        )
    }

    /**
     * 场景二：只报歌手（专属电台）
     */
    private suspend fun handleArtistIntent(intent: MusicIntent, recognizedText: String): VoiceDispatchResult? {
        val rawArtist = intent.artist?.trim() ?: return null
        val (targetArtist, artistId) = resolveArtist(rawArtist) ?: Pair(HanziConverter.toSimplified(rawArtist), null)
        Log.d(TAG, "正在查询歌手专属电台: raw=$rawArtist, resolved=$targetArtist, artistId=$artistId")

        val api = MoodyApiProvider.apiService
        val resp = if (!artistId.isNullOrBlank()) {
            api.getSongsByArtist(artistId = artistId)
        } else {
            api.getSongsByArtist(artist = targetArtist)
        }
        val artistsList = resp.data ?: emptyList()

        val allSongs = mutableListOf<PlayQueueItem>()
        for (artistObj in artistsList) {
            val artName = artistObj.name
            for (album in artistObj.albums) {
                val albTitle = album.title
                val coverUrl = resolveUrl(album.cover)
                for (s in album.songs) {
                    val audioUrl = resolveAudioUrl(s.path)
                    if (audioUrl.isNotBlank()) {
                        allSongs.add(
                            PlayQueueItem(
                                songTitle = s.title,
                                artistName = artName,
                                albumTitle = albTitle,
                                coverUrl = coverUrl,
                                audioUrl = audioUrl,
                                lrcPath = s.lrcPath
                            )
                        )
                    }
                }
            }
        }

        if (allSongs.isEmpty()) {
            Log.w(TAG, "未在曲库中找到歌手 $targetArtist 的任何曲目")
            return null
        }

        // 随机打乱并挑选前 20 首经典曲目
        val radioQueue = allSongs.shuffled().take(20)
        val summary = "为您开启：$targetArtist 专属精选电台（共 ${radioQueue.size} 首）"

        return VoiceDispatchResult(
            recognizedText = recognizedText,
            intentSummary = summary,
            playType = "artist",
            playlist = radioQueue,
            startIndex = 0
        )
    }

    /**
     * 场景三：指定唱片专辑（整专鉴赏）
     */
    private suspend fun handleAlbumIntent(intent: MusicIntent, recognizedText: String): VoiceDispatchResult? {
        val rawArtist = intent.artist?.trim()
        val (targetArtist, artistId) = if (!rawArtist.isNullOrBlank()) {
            resolveArtist(rawArtist) ?: Pair(HanziConverter.toSimplified(rawArtist), null)
        } else {
            Pair(null, null)
        }
        val targetAlbum = HanziConverter.toSimplified(intent.album?.trim() ?: "")
        Log.d(TAG, "正在查询整张专辑: artist=$targetArtist, artistId=$artistId, album=$targetAlbum")

        val api = MoodyApiProvider.apiService
        val resp = if (!artistId.isNullOrBlank()) {
            api.getSongsByArtist(artistId = artistId, album = targetAlbum.ifBlank { null })
        } else {
            api.getSongsByArtist(artist = targetArtist, album = targetAlbum.ifBlank { null })
        }
        val artistsList = resp.data ?: emptyList()

        var matchedAlbumTitle = ""
        val albumQueue = mutableListOf<PlayQueueItem>()

        for (artistObj in artistsList) {
            val artName = artistObj.name
            for (album in artistObj.albums) {
                val simpAlbumTitle = HanziConverter.toSimplified(album.title)
                if (simpAlbumTitle.contains(targetAlbum, ignoreCase = true) || targetAlbum.contains(simpAlbumTitle, ignoreCase = true)) {
                    matchedAlbumTitle = album.title
                    val coverUrl = resolveUrl(album.cover)
                    for (s in album.songs) {
                        val audioUrl = resolveAudioUrl(s.path)
                        if (audioUrl.isNotBlank()) {
                            albumQueue.add(
                                PlayQueueItem(
                                    songTitle = s.title,
                                    artistName = artName,
                                    albumTitle = album.title,
                                    coverUrl = coverUrl,
                                    audioUrl = audioUrl,
                                    lrcPath = s.lrcPath
                                )
                            )
                        }
                    }
                    if (albumQueue.isNotEmpty()) break
                }
            }
            if (albumQueue.isNotEmpty()) break
        }

        if (albumQueue.isEmpty()) {
            // 退化当作单曲或普通搜索处理
            return handleSongIntent(intent, recognizedText)
        }

        val summary = "正在播放专辑：《$matchedAlbumTitle》（全 ${albumQueue.size} 首）"

        return VoiceDispatchResult(
            recognizedText = recognizedText,
            intentSummary = summary,
            playType = "album",
            playlist = albumQueue,
            startIndex = 0
        )
    }

    /**
     * 场景四：情绪/场景漫游泛听
     */
    private suspend fun handleMoodOrFallbackIntent(intent: MusicIntent, recognizedText: String): VoiceDispatchResult? {
        val api = MoodyApiProvider.apiService
        val randomResp = api.getRandomSongs(limit = 10)
        val songs = randomResp.data ?: emptyList()

        val playlist = songs.mapNotNull { song ->
            val audioUrl = resolveAudioUrl(song.audioUrl ?: song.filePath)
            if (audioUrl.isBlank()) return@mapNotNull null
            PlayQueueItem(
                songTitle = song.title,
                artistName = song.artistName ?: "精选歌手",
                albumTitle = song.albumTitle ?: "心绪漫游",
                coverUrl = song.coverUrl ?: "",
                audioUrl = audioUrl,
                lrcPath = song.lrcPath
            )
        }

        if (playlist.isEmpty()) return null

        val summary = intent.reply?.takeIf { it.isNotBlank() } ?: "为您挑选了 10 首随心漫游曲目"

        return VoiceDispatchResult(
            recognizedText = recognizedText,
            intentSummary = summary,
            playType = "mood",
            playlist = playlist,
            startIndex = 0
        )
    }

    private fun resolveAudioUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            MoodyApiProvider.getMediaUrl(path)
        }
    }

    private fun resolveUrl(path: String?): String {
        if (path.isNullOrBlank()) return ""
        return if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            MoodyApiProvider.getMediaUrl(path)
        }
    }
}
