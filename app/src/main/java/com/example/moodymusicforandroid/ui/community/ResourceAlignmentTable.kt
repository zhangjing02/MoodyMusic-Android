package com.example.moodymusicforandroid.ui.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.data.model.ResourceAlignmentData
import com.example.moodymusicforandroid.data.model.ResourceAlbumData
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 现代颂歌风格：资源补齐格式化对齐表格组件 (The Modern Songbook Resource Alignment Table)
 *
 * @param data 结构化专辑补齐数据 (歌手、专辑名、代表歌曲、年份、介绍)
 * @param isCompleted 任务是否已完成打钩
 * @param isCompact 是否为列表紧凑卡片模式（详情页为 false，列表卡片为 true）
 */
@Composable
fun ResourceAlignmentTable(
    data: ResourceAlignmentData,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val tableBorderColor = if (isCompleted) Color(0xFFC8E6C9) else SongbookColors.GhostBorder
    val containerBg = if (isCompleted) Color(0xFFF1F8E9) else SongbookColors.SurfaceLow

    val subType = data.subType
    val subTypeIcon = data.getSubTypeIcon()
    val subTypeTitle = when (subType) {
        "song" -> "单曲资源对齐表"
        "artist" -> "歌手收录倡议表"
        "concert" -> "演唱会资源对齐表"
        "hires" -> "音质升级对齐表"
        else -> "专辑资源对齐表"
    }
    val subTypeSpecTag = when (subType) {
        "song" -> "TRACK ALIGNMENT SPEC"
        "artist" -> "ARTIST ALIGNMENT SPEC"
        "concert" -> "LIVE CONCERT SPEC"
        "hires" -> "HI-RES AUDIO SPEC"
        else -> "ALBUM ALIGNMENT SPEC"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, tableBorderColor, RoundedCornerShape(12.dp)),
        color = containerBg,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(if (isCompact) 12.dp else 16.dp)
        ) {
            // ── 表格标题头 ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = subTypeIcon,
                        fontSize = if (isCompact) 13.sp else 15.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Column {
                        if (!isCompact) {
                            Text(
                                text = subTypeSpecTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.BurntOrange,
                                letterSpacing = 1.5.sp,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = subTypeTitle,
                            style = if (isCompact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )
                    }
                }

                // 状态指示徽章
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCompleted) Color(0xFFE8F5E9) else SongbookColors.BurntOrange.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "已完成对齐",
                                color = Color(0xFF2E7D32),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "待补齐核验",
                                color = SongbookColors.BurntOrange,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SongbookColors.GhostBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // ── 表格主体（根据子分类呈现针对性字段） ───────────────────────
            when (subType) {
                "song" -> {
                    // 单曲补齐
                    val targetSong = data.songName?.takeIf { it.isNotBlank() } ?: data.songs
                    ResourceTableRow(
                        label = "歌曲名",
                        value = if (targetSong.startsWith("《") && targetSong.endsWith("》")) targetSong else "《$targetSong》",
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    ResourceTableRow(
                        label = "演唱歌手",
                        value = data.artist,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    if (data.album.isNotBlank()) {
                        ResourceTableRow(
                            label = "所属专辑",
                            value = if (data.album.startsWith("《") && data.album.endsWith("》")) data.album else "《${data.album}》",
                            isCompact = isCompact
                        )
                    }
                    data.year?.takeIf { it.isNotBlank() }?.let { y ->
                        val yearDisplay = if (y.endsWith("年")) y else "$y 年"
                        ResourceTableRow(label = "发行年份", value = yearDisplay, isCompact = isCompact)
                    }
                    data.link?.takeIf { it.isNotBlank() }?.let { l ->
                        ResourceTableRow(label = "试听来源", value = l, isCompact = isCompact, maxLines = if (isCompact) 1 else 2)
                    }
                    data.desc?.takeIf { it.isNotBlank() }?.let { d ->
                        ResourceTableRow(label = "补充说明", value = d, isCompact = isCompact, maxLines = if (isCompact) 2 else Int.MAX_VALUE)
                    }
                }
                "artist" -> {
                    // 歌手补齐
                    ResourceTableRow(
                        label = "歌手/乐团",
                        value = data.artist,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    data.genre?.takeIf { it.isNotBlank() }?.let { g ->
                        ResourceTableRow(label = "流派风格", value = g, isCompact = isCompact)
                    }
                    if (data.songs.isNotBlank()) {
                        ResourceTableRow(label = "代表作", value = data.songs, isCompact = isCompact)
                    }
                    if (data.album.isNotBlank()) {
                        ResourceTableRow(
                            label = "代表专辑",
                            value = if (data.album.startsWith("《") && data.album.endsWith("》")) data.album else "《${data.album}》",
                            isCompact = isCompact
                        )
                    }
                    data.desc?.takeIf { it.isNotBlank() }?.let { d ->
                        ResourceTableRow(label = "生平推荐", value = d, isCompact = isCompact, maxLines = if (isCompact) 2 else Int.MAX_VALUE)
                    }
                }
                "concert" -> {
                    // 演唱会补齐
                    val cName = data.concertName?.takeIf { it.isNotBlank() } ?: data.album
                    ResourceTableRow(
                        label = "巡演现场",
                        value = cName,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    ResourceTableRow(
                        label = "演出歌手",
                        value = data.artist,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    data.year?.takeIf { it.isNotBlank() }?.let { y ->
                        val yearDisplay = if (y.endsWith("年")) y else "$y 年"
                        ResourceTableRow(label = "举办年份", value = yearDisplay, isCompact = isCompact)
                    }
                    if (data.songs.isNotBlank()) {
                        ResourceTableRow(label = "现场曲目", value = data.songs, isCompact = isCompact)
                    }
                    data.desc?.takeIf { it.isNotBlank() }?.let { d ->
                        ResourceTableRow(label = "现场说明", value = d, isCompact = isCompact, maxLines = if (isCompact) 2 else Int.MAX_VALUE)
                    }
                }
                "hires" -> {
                    // 音质升级
                    val targetAudio = data.songName?.takeIf { it.isNotBlank() } ?: (if (data.album.isNotBlank()) "《${data.album}》" else data.songs)
                    ResourceTableRow(
                        label = "目标音源",
                        value = targetAudio,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    ResourceTableRow(
                        label = "所属歌手",
                        value = data.artist,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    data.qualityIssue?.takeIf { it.isNotBlank() }?.let { q ->
                        ResourceTableRow(label = "音质问题", value = q, isCompact = isCompact)
                    }
                    val specVal = data.qualitySpec?.takeIf { it.isNotBlank() } ?: "FLAC / 24bit Hi-Res"
                    ResourceTableRow(label = "期望规格", value = specVal, isCompact = isCompact)
                    data.desc?.takeIf { it.isNotBlank() }?.let { d ->
                        ResourceTableRow(label = "补充说明", value = d, isCompact = isCompact, maxLines = if (isCompact) 2 else Int.MAX_VALUE)
                    }
                }
                else -> {
                    // 默认：专辑补齐
                    ResourceTableRow(
                        label = "歌手",
                        value = data.artist,
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    ResourceTableRow(
                        label = "专辑名",
                        value = if (data.album.startsWith("《") && data.album.endsWith("》")) data.album else "《${data.album}》",
                        isHighlight = true,
                        isCompact = isCompact
                    )
                    if (data.songs.isNotBlank()) {
                        ResourceTableRow(
                            label = "代表歌曲",
                            value = data.songs,
                            isCompact = isCompact
                        )
                    }
                    data.year?.takeIf { it.isNotBlank() }?.let { y ->
                        val yearDisplay = if (y.endsWith("年")) y else "$y 年"
                        ResourceTableRow(label = "发行年份", value = yearDisplay, isCompact = isCompact)
                    }
                    data.desc?.takeIf { it.isNotBlank() }?.let { d ->
                        ResourceTableRow(
                            label = "专辑介绍",
                            value = d,
                            isCompact = isCompact,
                            maxLines = if (isCompact) 2 else Int.MAX_VALUE
                        )
                    }
                }
            }

            if (!isCompact && isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFC8E6C9), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))
                val completionText = when (subType) {
                    "song" -> "✨ 该单曲已完成高品质音源入库与线上点亮！"
                    "artist" -> "✨ 该音乐人全集已列入收录规划并完成建档！"
                    "concert" -> "✨ 该现场巡演高保真音频已收录对齐！"
                    "hires" -> "✨ 该音源已完成无损母带/高规格音轨升级替换！"
                    else -> "✨ 该专辑已由开发者完成资产对齐与云端收录入库！"
                }
                Text(
                    text = completionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ResourceTableRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isCompact: Boolean = false,
    maxLines: Int = Int.MAX_VALUE
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isCompact) 3.5.dp else 4.5.dp),
        verticalAlignment = if (maxLines > 1 && value.length > 30) Alignment.Top else Alignment.CenterVertically
    ) {
        // 标签列（固定宽度规范化排版）
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = SongbookColors.SurfaceHighest.copy(alpha = 0.5f),
            modifier = Modifier.width(if (isCompact) 68.dp else 76.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.65f),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 数值列
        Text(
            text = value,
            style = if (isHighlight) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = SongbookColors.SoftCharcoal,
            fontSize = if (isCompact) 12.sp else 13.sp,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
