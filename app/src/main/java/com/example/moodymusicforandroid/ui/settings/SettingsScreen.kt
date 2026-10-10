package com.example.moodymusicforandroid.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.utils.SleepTimerManager
import com.example.moodymusicforandroid.data.manager.UserManager

/**
 * 应用设置页面 (SettingsScreen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // ── 休眠定时状态 ──
    val selectedSleepTimer by SleepTimerManager.selectedMinutes.collectAsState()
    val remainingSeconds by SleepTimerManager.remainingSeconds.collectAsState()

    // ── 首页卡片播放偏好 ──
    val cardClickDirectPlay by UserManager.cardClickDirectPlay.collectAsState()

    // ── 缓存大小 ──
    var cacheSizeText by remember { mutableStateOf("16.8 MB") }

    // ── 语义色快捷变量（跟随 MaterialTheme 自动切换深/浅）──
    val primary    = MaterialTheme.colorScheme.primary
    val onSurface  = MaterialTheme.colorScheme.onSurface
    val surface    = MaterialTheme.colorScheme.surface
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow

    androidx.activity.compose.BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surface
                )
            )
        },
        containerColor = surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            // ── 1. App 休眠时间 (睡眠定时器) ──────────────────────────
            SettingsSectionLabel(text = "播放与休眠", primary = primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App 休眠定时",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = if (remainingSeconds > 0) "已开启：将在 ${SleepTimerManager.getFormattedRemainingTime()} 后暂停播放"
                                       else if (selectedSleepTimer == SleepTimerManager.TIMER_END_OF_SONG) "将在当前单曲播放完毕后停止"
                                       else "睡前聆听，倒计时结束后自动暂停播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remainingSeconds > 0) primary else onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val sleepOptions = listOf(
                        "关闭" to SleepTimerManager.TIMER_OFF,
                        "15 分钟" to SleepTimerManager.TIMER_15_MIN,
                        "30 分钟" to SleepTimerManager.TIMER_30_MIN,
                        "45 分钟" to SleepTimerManager.TIMER_45_MIN,
                        "60 分钟" to SleepTimerManager.TIMER_60_MIN,
                        "播完当曲" to SleepTimerManager.TIMER_END_OF_SONG
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sleepOptions.chunked(3).forEach { rowOptions ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowOptions.forEach { (label, minutes) ->
                                    val isSelected = selectedSleepTimer == minutes
                                    Surface(
                                        onClick = {
                                            SleepTimerManager.setSleepTimer(context, minutes)
                                            val tip = if (minutes == 0) "已关闭休眠定时"
                                                      else if (minutes == -1) "已设置为播完当前歌曲后停止"
                                                      else "已设置 ${minutes} 分钟后停止播放"
                                            Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) primary else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. 首页卡片播放偏好 ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "卡片播放偏好",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = if (!cardClickDirectPlay) "当前：点击卡片进入图文详情，不直接播放" else "当前：点击卡片即播（若播放器有音频则保持连贯）",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cardClickDirectPlay) primary else onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 选项 1：不直接播放
                        Surface(
                            onClick = {
                                UserManager.updateCardClickDirectPlay(false)
                                Toast.makeText(context, "已设为点击卡片不直接播放", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!cardClickDirectPlay) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (!cardClickDirectPlay) primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "不直接播放",
                                    fontSize = 13.sp,
                                    fontWeight = if (!cardClickDirectPlay) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!cardClickDirectPlay) primary else onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "进入详情按需播放",
                                    fontSize = 11.sp,
                                    color = onSurface.copy(alpha = 0.55f),
                                    maxLines = 1
                                )
                            }
                        }

                        // 选项 2：直接播放（默认）
                        Surface(
                            onClick = {
                                UserManager.updateCardClickDirectPlay(true)
                                Toast.makeText(context, "已设为点击卡片直接播放", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (cardClickDirectPlay) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (cardClickDirectPlay) primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "直接播放",
                                        fontSize = 13.sp,
                                        fontWeight = if (cardClickDirectPlay) FontWeight.Bold else FontWeight.Medium,
                                        color = if (cardClickDirectPlay) primary else onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (cardClickDirectPlay) primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "默认",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cardClickDirectPlay) Color.White else onSurface.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "空闲时点击即播，有播放不打断",
                                    fontSize = 11.sp,
                                    color = onSurface.copy(alpha = 0.55f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 5. 存储与缓存管理 ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "存储与缓存清理",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )
                                Text(
                                    text = "当前占用空间：$cacheSizeText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = onSurface.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                Coil.imageLoader(context).memoryCache?.clear()
                                cacheSizeText = "0.0 MB"
                                Toast.makeText(context, "图片与媒体缓存已彻底清空", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "一键清理",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

}

// ─────────────────────────────────────────────────────────────────────────────
// 子组件
// ─────────────────────────────────────────────────────────────────────────────

/** Section 分组标题标签 */
@Composable
internal fun SettingsSectionLabel(text: String, primary: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        fontSize = 11.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}
