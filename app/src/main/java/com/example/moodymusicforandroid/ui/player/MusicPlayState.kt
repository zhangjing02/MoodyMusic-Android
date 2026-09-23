package com.example.moodymusicforandroid.ui.player

import java.io.Serializable

/**
 * 播放模式枚举
 */
enum class PlayMode {
    SEQUENTIAL,   // 顺序播放 (默认)
    LIST_LOOP,    // 列表循环
    SINGLE_LOOP,  // 单曲循环
    SHUFFLE;      // 随机播放

    fun next(): PlayMode {
        val modes = entries
        return modes[(ordinal + 1) % modes.size]
    }

    val label: String
        get() = when (this) {
            SEQUENTIAL -> "顺序播放"
            LIST_LOOP -> "列表循环"
            SINGLE_LOOP -> "单曲循环"
            SHUFFLE -> "随机播放"
        }

    companion object {
        fun fromString(name: String?): PlayMode {
            return entries.find { it.name.equals(name, ignoreCase = true) } ?: SEQUENTIAL
        }
    }
}

/**
 * 全局播放状态数据类，通过 EventBus 广播，供 PlayerViewModel 和 UI 层消费
 */
data class MusicPlayState(
    val songTitle: String = "",
    val artistName: String = "",
    val albumTitle: String = "",
    val coverUrl: String = "",
    val audioUrl: String = "",
    val lrcPath: String? = null,
    val isPlaying: Boolean = false,
    val duration: Int = 0,
    val position: Int = 0,
    val playlistIndex: Int = 0,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val queue: List<PlayQueueItem> = emptyList()
)

/** 播放队列条目（实现 Serializable 以便通过 Intent extra 传递）*/
data class PlayQueueItem(
    val songTitle: String,
    val artistName: String,
    val albumTitle: String,
    val coverUrl: String,
    val audioUrl: String,
    val lrcPath: String? = null,
    val queueId: String = java.util.UUID.randomUUID().toString()
) : Serializable

/**
 * 「加入当前播放列表」操作结果
 * - ADDED：成功追加到队列末尾
 * - DUPLICATE：队列中已存在相同音频，未重复添加
 * - STARTED_NEW：原队列为空，已新建单曲播放会话
 */
enum class AddToQueueResult { ADDED, DUPLICATE, STARTED_NEW }
