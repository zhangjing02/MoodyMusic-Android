package com.example.moodymusicforandroid.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.utils.AppThemeManager
import com.example.moodymusicforandroid.common.utils.SleepTimerManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.theme.SongbookAccentPalettes

/**
 * 应用设置页面 (SettingsScreen)
 *
 * 改造：
 * - 全部 SongbookColors 语义化对接 MaterialTheme
 * - 显示模式（白色/黑色）切换卡片
 * - 主题色调（官方雅致推荐色 + 自由调色盘）切换卡片
 * - 移除旧版生硬“强调色”术语与固定怪异色彩
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // ── 主题配置（订阅 AppThemeManager StateFlow）──
    val themeConfig by AppThemeManager.config.collectAsState()
    val isDark = themeConfig.isDark
    val currentAccent = themeConfig.accentColor

    // ── 自由调色盘弹窗开关 ──
    var showColorPicker by remember { mutableStateOf(false) }

    // ── 休眠定时状态 ──
    val selectedSleepTimer by SleepTimerManager.selectedMinutes.collectAsState()
    val remainingSeconds by SleepTimerManager.remainingSeconds.collectAsState()

    // ── 首页卡片播放偏好 ──
    val cardClickDirectPlay by UserManager.cardClickDirectPlay.collectAsState()

    // ── 字体大小比例 ──
    val currentFontScale by UserManager.fontScale.collectAsState()

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

            // ── 0. 显示模式（白色/黑色主题） ──────────────────────────
            SettingsSectionLabel(text = "显示模式", primary = primary)

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
                                painter = painterResource(R.drawable.ic_palette),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "显示模式",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = if (isDark) "当前：黑色主题（夜间模式）" else "当前：白色主题（日间模式）",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) primary else onSurface.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 白色主题
                        ThemeModeCard(
                            label = "白色主题",
                            subLabel = "日间 · 纸质暖白",
                            isSelected = !isDark,
                            indicatorColor = Color(0xFFF5F3EF),
                            indicatorBorder = Color(0xFFDAC2B6),
                            primary = primary,
                            onSurface = onSurface,
                            surfaceLow = surfaceLow,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                AppThemeManager.setDarkMode(false, context)
                                Toast.makeText(context, "已切换为白色主题", Toast.LENGTH_SHORT).show()
                            }
                        )

                        // 黑色主题
                        ThemeModeCard(
                            label = "黑色主题",
                            subLabel = "夜间 · 深暗柔和",
                            isSelected = isDark,
                            indicatorColor = Color(0xFF222320),
                            indicatorBorder = Color(0xFF54433A),
                            primary = primary,
                            onSurface = onSurface,
                            surfaceLow = surfaceLow,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                AppThemeManager.setDarkMode(true, context)
                                Toast.makeText(context, "已切换为黑色主题", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 1. 主题色调选择 ─────────────────────────────────────────
            SettingsSectionLabel(text = "主题色调", primary = primary)

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
                                painter = painterResource(R.drawable.ic_palette),
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "主题色调",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            val accentDesc = if (currentAccent == AppThemeManager.AccentColor.CUSTOM && themeConfig.customColorArgb != null) {
                                val hex = String.format("#%06X", 0xFFFFFF and themeConfig.customColorArgb!!)
                                "当前：自由调色盘 ($hex)"
                            } else {
                                "当前：${currentAccent.displayName}"
                            }
                            Text(
                                text = accentDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = primary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val presetOptions = listOf(
                        Triple(AppThemeManager.AccentColor.MOCHA, "经典咖棕",
                            if (isDark) SongbookAccentPalettes.Mocha.darkPrimary else SongbookAccentPalettes.Mocha.lightPrimary),
                        Triple(AppThemeManager.AccentColor.FOREST, "复古墨绿",
                            if (isDark) SongbookAccentPalettes.Forest.darkPrimary else SongbookAccentPalettes.Forest.lightPrimary),
                        Triple(AppThemeManager.AccentColor.OCEAN, "静谧黛蓝",
                            if (isDark) SongbookAccentPalettes.Ocean.darkPrimary else SongbookAccentPalettes.Ocean.lightPrimary),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1~3. 官方精选推荐色
                        presetOptions.forEach { (accent, label, previewColor) ->
                            val isSelected = currentAccent == accent
                            Surface(
                                onClick = {
                                    AppThemeManager.setAccentColor(accent, context)
                                    Toast.makeText(context, "已切换为 $label", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) previewColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) previewColor else MaterialTheme.colorScheme.outlineVariant
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
                                            .background(previewColor),
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
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) previewColor else onSurface
                                    )
                                }
                            }
                        }

                        // 4. 自由调色（纯净彩虹圆盘，点击直接弹窗选色）
                        val isCustomSelected = currentAccent == AppThemeManager.AccentColor.CUSTOM
                        Surface(
                            onClick = { showColorPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCustomSelected) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isCustomSelected) primary else MaterialTheme.colorScheme.outlineVariant
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
                                // 纯粹彩虹渐变色相盘：无打勾、不覆盖颜色，点击即可打开调色盘查看和调试
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                listOf(
                                                    Color(0xFFFF5252),
                                                    Color(0xFFFFB74D),
                                                    Color(0xFF81C784),
                                                    Color(0xFF4DD0E1),
                                                    Color(0xFF7E57C2),
                                                    Color(0xFFFF5252)
                                                )
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "自由调色",
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCustomSelected) primary else onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. App 休眠时间 (睡眠定时器) ──────────────────────────
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

            // ── 4. 字体显示大小 (含实时手札预览) ───────────────────────
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
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "字体显示大小",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onSurface
                            )
                            Text(
                                text = "调节全应用手札文字与歌词的显示字号",
                                style = MaterialTheme.typography.bodySmall,
                                color = onSurface.copy(alpha = 0.6f),
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // 实时手札文字预览卡片
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "实时效果预览",
                                style = MaterialTheme.typography.labelSmall,
                                color = primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "「岁月如歌，黑胶流转。听一曲老歌，品半生从容。」",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onSurface,
                                fontFamily = FontFamily.Serif,
                                fontSize = (14 * currentFontScale).sp,
                                lineHeight = (22 * currentFontScale).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "—— 音信 · The Modern Songbook",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurface.copy(alpha = 0.5f),
                                fontSize = (11 * currentFontScale).sp
                            )
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

    // ── 自由调色盘弹窗 ─────────────────────────────────────────────────────────
    if (showColorPicker) {
        val initialPickerColor = if (currentAccent == AppThemeManager.AccentColor.CUSTOM && themeConfig.customColorArgb != null) {
            Color(themeConfig.customColorArgb!!)
        } else {
            SongbookAccentPalettes.Mocha.lightPrimary
        }
        ThemeColorPickerDialog(
            initialColor = initialPickerColor,
            onDismiss = { showColorPicker = false },
            onColorApplied = { selectedColor ->
                AppThemeManager.setCustomColor(selectedColor.toArgb(), context)
                showColorPicker = false
                Toast.makeText(context, "已应用自选主题色", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 子组件
// ─────────────────────────────────────────────────────────────────────────────

/** Section 分组标题标签 */
@Composable
private fun SettingsSectionLabel(text: String, primary: Color) {
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

/** 显示模式选择卡片（白色/黑色）*/
@Composable
private fun ThemeModeCard(
    label: String,
    subLabel: String,
    isSelected: Boolean,
    indicatorColor: Color,
    indicatorBorder: Color,
    primary: Color,
    onSurface: Color,
    surfaceLow: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) primary.copy(alpha = 0.10f) else surfaceLow,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(88.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 颜色预览圆点 + 勾选标记
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
                    .then(
                        if (!isSelected) Modifier.border(1.dp, indicatorBorder, CircleShape) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primary else onSurface
            )
            Text(
                text = subLabel,
                fontSize = 10.5.sp,
                color = onSurface.copy(alpha = 0.5f),
                maxLines = 1
            )
        }
    }
}

/**
 * 现代颂歌 自由调色盘弹窗组件 (ThemeColorPickerDialog)
 * 支持色相全光谱滑块 (0°~360°)、纯度/饱和度滑块、明度/亮度滑块，以及经典灵感速选色
 */
@Composable
private fun ThemeColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorApplied: (Color) -> Unit
) {
    val initialArgb = initialColor.toArgb()
    val initialHsv = remember(initialArgb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb, it) }
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1].coerceIn(0.1f, 1f)) }
    var value by remember { mutableFloatStateOf(initialHsv[2].coerceIn(0.2f, 1f)) }

    val currentColor = remember(hue, sat, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    }
    val currentHex = remember(currentColor) {
        val argb = currentColor.toArgb()
        String.format("#%06X", 0xFFFFFF and argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_palette),
                    contentDescription = null,
                    tint = currentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "自由调色盘",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部：实时预览大色球与 Hex 颜色代码
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "实时预览",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = currentHex,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(initialColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "初始色",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // 1. 色相 Hue 滑块 (0°..360°)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "色相 (Hue)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${hue.toInt()}°", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFF0000),
                                        Color(0xFFFFFF00),
                                        Color(0xFF00FF00),
                                        Color(0xFF00FFFF),
                                        Color(0xFF0000FF),
                                        Color(0xFFFF00FF),
                                        Color(0xFFFF0000)
                                    )
                                )
                            )
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                // 2. 鲜艳度 / 饱和度 Saturation 滑块 (0.05..1)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "纯度 / 饱和度", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${(sat * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.05f, value))),
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))
                                    )
                                )
                            )
                    )
                    Slider(
                        value = sat,
                        onValueChange = { sat = it },
                        valueRange = 0.05f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                // 3. 明亮度 Value 滑块 (0.2..1)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "明度 / 亮度", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${(value * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 0.2f))),
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f)))
                                    )
                                )
                            )
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0.2f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = currentColor,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )
                }

                // 4. 灵感速选色点
                Column {
                    Text(
                        text = "灵感色标",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val quickSwatches = listOf(
                        Color(0xFF6C2F00), // 经典咖棕
                        Color(0xFF2E563E), // 复古墨绿
                        Color(0xFF204D74), // 静谧黛蓝
                        Color(0xFF9C3636), // 晚霞暮绯
                        Color(0xFFB8781B), // 琥珀原金
                        Color(0xFF754591), // 优雅紫藤
                        Color(0xFF137A7F)  // 碧湖松石
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        quickSwatches.forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(swatch.toArgb(), hsv)
                                        hue = hsv[0]
                                        sat = hsv[1]
                                        value = hsv[2]
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorApplied(currentColor) },
                colors = ButtonDefaults.buttonColors(containerColor = currentColor)
            ) {
                Text(
                    text = "应用此色彩",
                    color = if ((currentColor.red * 0.299f + currentColor.green * 0.587f + currentColor.blue * 0.114f) > 0.62f) Color(0xFF1B1C1A) else Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
