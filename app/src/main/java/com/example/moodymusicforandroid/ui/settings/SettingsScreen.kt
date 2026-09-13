package com.example.moodymusicforandroid.ui.settings

import android.content.Context
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import coil.Coil
import com.example.moodymusicforandroid.MoodyMusicApplication
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.common.utils.SleepTimerManager
import com.example.moodymusicforandroid.common.utils.ThemeManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 应用设置页面 (SettingsScreen)
 *
 * 核心功能：
 * 1. App 休眠时间（睡眠定时器：关闭、15m、30m、45m、60m、播完当曲）
 * 2. 首页卡片播放偏好（点击卡片不直接播放、点击卡片直接播放[默认]）
 * 3. 字体显示大小（小号 85%、标准 100%、大号 115%、超大号 130%）附带实时手札预览
 * 4. 主题色彩切换（默认绿、海洋蓝、日落橙、暗夜紫）
 * 5. 存储与缓存清理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // 休眠定时状态
    val selectedSleepTimer by SleepTimerManager.selectedMinutes.collectAsState()
    val remainingSeconds by SleepTimerManager.remainingSeconds.collectAsState()

    // 首页卡片播放偏好（由 UserManager 全局统一管理，持久化在 Room 用户表）
    val cardClickDirectPlay by UserManager.cardClickDirectPlay.collectAsState()

    // 字体大小比例（由 UserManager 全局统一管理，持久化在 Room 用户表）
    val currentFontScale by UserManager.fontScale.collectAsState()

    // 主题模式（由 UserManager 全局统一管理，持久化在 Room 用户表）
    val currentThemeModeInt by UserManager.themeMode.collectAsState()
    val currentThemeMode = ThemeManager.ThemeMode.fromValue(currentThemeModeInt)

    // 缓存大小
    var cacheSizeText by remember { mutableStateOf("16.8 MB") }

    androidx.activity.compose.BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "应用设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.SoftCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = SongbookColors.SoftCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SongbookColors.PaperBackground
                )
            )
        },
        containerColor = SongbookColors.PaperBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── 1. App 休眠时间 (睡眠定时器) ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App 休眠定时",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.SoftCharcoal
                            )
                            Text(
                                text = if (remainingSeconds > 0) "已开启：将在 ${SleepTimerManager.getFormattedRemainingTime()} 后暂停播放"
                                       else if (selectedSleepTimer == SleepTimerManager.TIMER_END_OF_SONG) "将在当前单曲播放完毕后停止"
                                       else "睡前聆听，倒计时结束后自动暂停播放",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (remainingSeconds > 0) SongbookColors.BurntOrange else SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
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

                    // 选项网格
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
                                        color = if (isSelected) SongbookColors.BurntOrange else SongbookColors.PaperBackground,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorder
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
                                                color = if (isSelected) Color.White else SongbookColors.SoftCharcoal
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

            // ── 2. 首页卡片播放偏好 ──────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_play_arrow),
                                contentDescription = null,
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "卡片播放偏好",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.SoftCharcoal
                            )
                            Text(
                                text = if (!cardClickDirectPlay) "当前：点击卡片进入图文详情，不直接播放" else "当前：点击卡片即刻开播音乐并进入详情",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
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
                            color = if (!cardClickDirectPlay) SongbookColors.BurntOrange.copy(alpha = 0.12f) else SongbookColors.PaperBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (!cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.GhostBorder
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
                                    color = if (!cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.SoftCharcoal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "进入详情按需播放",
                                    fontSize = 11.sp,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.55f),
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
                            color = if (cardClickDirectPlay) SongbookColors.BurntOrange.copy(alpha = 0.12f) else SongbookColors.PaperBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.GhostBorder
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
                                        color = if (cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.SoftCharcoal
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (cardClickDirectPlay) SongbookColors.BurntOrange else SongbookColors.GhostBorder.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "默认",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cardClickDirectPlay) Color.White else SongbookColors.SoftCharcoal.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "轻触卡片即刻开播",
                                    fontSize = 11.sp,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.55f),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. 字体显示大小 (含实时手札预览) ───────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "字体显示大小",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.SoftCharcoal
                            )
                            Text(
                                text = "调节全应用手札文字与歌词的显示字号",
                                style = MaterialTheme.typography.bodySmall,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val fontScales = listOf(
                        "小号" to 0.85f,
                        "标准" to 1.00f,
                        "大号" to 1.15f,
                        "超大号" to 1.30f
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontScales.forEach { (label, scale) ->
                            val isSelected = kotlin.math.abs(currentFontScale - scale) < 0.05f
                            Surface(
                                onClick = {
                                    UserManager.updateFontScale(scale)
                                    Toast.makeText(context, "已设为${label}字号", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) SongbookColors.BurntOrange else SongbookColors.PaperBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) SongbookColors.BurntOrange else SongbookColors.GhostBorder
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
                                        color = if (isSelected) Color.White else SongbookColors.SoftCharcoal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 实时手札文字预览卡片
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SongbookColors.PaperBackground,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, SongbookColors.GhostBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "实时效果预览",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.BurntOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "「岁月如歌，黑胶流转。听一曲老歌，品半生从容。」",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SongbookColors.SoftCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = (14 * currentFontScale).sp,
                                lineHeight = (22 * currentFontScale).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "—— 音信 · The Modern Songbook",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                                fontSize = (11 * currentFontScale).sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 3. 主题色彩 ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "主题色彩",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.SoftCharcoal
                            )
                            Text(
                                text = "挑选契合当下心境的色彩氛围",
                                style = MaterialTheme.typography.bodySmall,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val themes = listOf(
                        Triple("默认绿", ThemeManager.ThemeMode.DEFAULT, Color(0xFF2D4A3E)),
                        Triple("海洋蓝", ThemeManager.ThemeMode.OCEAN, Color(0xFF1E3A5F)),
                        Triple("日落橙", ThemeManager.ThemeMode.SUNSET, Color(0xFFB3541E)),
                        Triple("暗夜紫", ThemeManager.ThemeMode.NIGHT, Color(0xFF3A2350))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEach { (title, mode, primaryColor) ->
                            val isSelected = currentThemeMode == mode
                            Surface(
                                onClick = {
                                    UserManager.updateThemeMode(mode.value, context)
                                    Toast.makeText(context, "已切换为${title}主题", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) primaryColor.copy(alpha = 0.12f) else SongbookColors.PaperBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) primaryColor else SongbookColors.GhostBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) primaryColor else SongbookColors.SoftCharcoal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 4. 存储与缓存管理 ────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
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
                                    .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = SongbookColors.BurntOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "存储与缓存清理",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SongbookColors.SoftCharcoal
                                )
                                Text(
                                    text = "当前占用空间：$cacheSizeText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
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
                            colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange),
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
