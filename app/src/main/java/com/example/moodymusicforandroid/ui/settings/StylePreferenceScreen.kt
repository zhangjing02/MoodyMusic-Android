package com.example.moodymusicforandroid.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.utils.AppThemeManager
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.theme.SongbookAccentPalettes

/**
 * 风格喜好页面 (StylePreferenceScreen)
 *
 * 承载：
 * 1. 主题色调（昼夜拟态开关 + 官方精选色盘 + 自由调色盘拾色器）
 * 2. 字体显示大小（字号调节按钮组 + 实时手札文本排版预览）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylePreferenceScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // ── 主题配置（订阅 AppThemeManager StateFlow）──
    val themeConfig by AppThemeManager.config.collectAsState()
    val isDark = themeConfig.isDark
    val currentAccent = themeConfig.accentColor

    // ── 自由调色盘弹窗开关 ──
    var showColorPicker by remember { mutableStateOf(false) }

    // ── 字体大小比例 ──
    val currentFontScale by UserManager.fontScale.collectAsState()

    // ── 语义色快捷变量 ──
    val primary    = MaterialTheme.colorScheme.primary
    val onSurface  = MaterialTheme.colorScheme.onSurface
    val surface    = MaterialTheme.colorScheme.surface
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow

    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "风格喜好",
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

            // ── 1. 主题色调 ──────────────────────────
            SettingsSectionLabel(text = "主题色调", primary = primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // 主题色调：左侧标题与当前色调说明，右侧直接集成昼夜拟态 Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
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

                        // 右侧个性拟态昼夜 Switch 开关
                        DayNightNeumorphicSwitch(
                            isDark = isDark,
                            onToggle = { newIsDark ->
                                AppThemeManager.setDarkMode(newIsDark, context)
                                Toast.makeText(context, if (newIsDark) "已切换为黑色主题" else "已切换为白色主题", Toast.LENGTH_SHORT).show()
                            }
                        )
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
                                border = BorderStroke(
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
                            border = BorderStroke(
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

            Spacer(modifier = Modifier.height(20.dp))

            // ── 2. 字体显示大小 (含实时手札预览) ───────────────────────
            SettingsSectionLabel(text = "字体显示大小", primary = primary)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceLow),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 38.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
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
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                fontSize = 14.sp,
                                lineHeight = 22.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "—— 音信 · The Modern Songbook",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp
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

/**
 * 现代颂歌 拟态阴阳双相微开关 (DayNightNeumorphicSwitch)
 */
@Composable
fun DayNightNeumorphicSwitch(
    isDark: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // 0f 表示白天 (滑块居左)，1f 表示黑夜 (滑块居右)
    val progress by animateFloatAsState(
        targetValue = if (isDark) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "day_night_switch_progress"
    )

    val trackWidth = 70.dp
    val trackHeight = 32.dp
    val thumbSize = 26.dp
    val trackPadding = 3.dp
    val maxSlideDistance = trackWidth - thumbSize - (trackPadding * 2)

    val trackBgStart = lerp(Color(0xFFEBE7DF), Color(0xFF141512), progress)
    val trackBgEnd = lerp(Color(0xFFDFDAD0), Color(0xFF0C0D0B), progress)

    val innerShadowColor = lerp(Color(0x30352B20), Color(0x75000000), progress)
    val innerHighlightColor = lerp(Color(0xF5FFFFFF), Color(0x18FFFFFF), progress)

    val trackBorderTop = lerp(Color(0x30877369), Color(0x55000000), progress)
    val trackBorderBottom = lerp(Color(0xF0FFFFFF), Color(0x22FFFFFF), progress)

    val thumbGradTopLeft = lerp(Color(0xFFFFFFFF), Color(0xFF383A35), progress)
    val thumbGradCenter = lerp(Color(0xFFFAF9F6), Color(0xFF282A25), progress)
    val thumbGradBottomRight = lerp(Color(0xFFEBE6DC), Color(0xFF1C1D1A), progress)

    val thumbBorderTopLeft = lerp(Color(0xFFFFFFFF), Color(0xFF4C4E48), progress)
    val thumbBorderBottomRight = lerp(Color(0xFFDED8CD), Color(0xFF161714), progress)

    val thumbShadowColor = lerp(Color(0x2E3B2F23), Color(0x70000000), progress)

    val textIconColor = lerp(Color(0xFF7A7367), Color(0xFF94978F), progress)

    val lightTextAlpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
    val darkTextAlpha = ((progress - 0.55f) * 2.2f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .drawBehind {
                val corner = CornerRadius(size.height / 2f, size.height / 2f)

                // A. 基础内凹底槽渐变
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(trackBgStart, trackBgEnd)),
                    cornerRadius = corner
                )

                // B. 左上内阴影 (Inset Debossed Shadow)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(innerShadowColor, Color.Transparent),
                        startY = 0f,
                        endY = 5.5.dp.toPx()
                    ),
                    cornerRadius = corner
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(innerShadowColor, Color.Transparent),
                        startX = 0f,
                        endX = 6.5.dp.toPx()
                    ),
                    cornerRadius = corner
                )

                // C. 右下内高光 (Inset Highlight)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, innerHighlightColor),
                        startY = size.height - 4.5.dp.toPx(),
                        endY = size.height
                    ),
                    cornerRadius = corner
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, innerHighlightColor),
                        startX = size.width - 5.5.dp.toPx(),
                        endX = size.width
                    ),
                    cornerRadius = corner
                )

                // D. 外切面倒角细描边
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(trackBorderTop, trackBorderBottom)),
                    cornerRadius = corner,
                    style = Stroke(width = 0.85.dp.toPx())
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle(!isDark)
            }
            .padding(trackPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        // 白天文字
        Box(
            modifier = Modifier
                .width(38.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "LIGHT",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = textIconColor.copy(alpha = lightTextAlpha),
                modifier = Modifier.graphicsLayer(alpha = lightTextAlpha)
            )
        }

        // 黑夜文字
        Box(
            modifier = Modifier
                .width(38.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "DARK",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = textIconColor.copy(alpha = darkTextAlpha),
                modifier = Modifier.graphicsLayer(alpha = darkTextAlpha)
            )
        }

        // 浮雕滑块
        val thumbOffset = maxSlideDistance * progress

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .drawBehind {
                    drawCircle(
                        color = thumbShadowColor,
                        radius = size.width / 2f + 0.6.dp.toPx(),
                        center = Offset(
                            x = size.width / 2f + 1.5.dp.toPx(),
                            y = size.height / 2f + 2.0.dp.toPx()
                        )
                    )
                }
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(thumbGradTopLeft, thumbGradCenter, thumbGradBottomRight),
                        start = Offset.Zero,
                        end = Offset(thumbSize.value * 2.2f, thumbSize.value * 2.2f)
                    )
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(thumbBorderTopLeft, thumbBorderBottomRight),
                        start = Offset.Zero,
                        end = Offset(thumbSize.value * 2.2f, thumbSize.value * 2.2f)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconRotation = progress * 180f

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer(rotationZ = iconRotation),
                contentAlignment = Alignment.Center
            ) {
                // 太阳图标
                if (progress < 0.85f) {
                    val sunAlpha = (1f - progress * 1.3f).coerceIn(0f, 1f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = sunAlpha)
                    ) {
                        drawSunIcon(color = textIconColor)
                    }
                }

                // 月亮与双星芒图标
                if (progress > 0.15f) {
                    val moonAlpha = ((progress - 0.15f) * 1.3f).coerceIn(0f, 1f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = moonAlpha)
                    ) {
                        drawMoonAndStarIcon(color = textIconColor)
                    }
                }
            }
        }
    }
}

/** 太阳图标绘制 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSunIcon(color: Color) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val coreRadius = size.minDimension * 0.22f
    val strokeWidth = 1.35.dp.toPx()

    drawCircle(
        color = color,
        radius = coreRadius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    val rayStart = coreRadius + 1.8.dp.toPx()
    val rayEnd = coreRadius + 4.2.dp.toPx()

    for (i in 0 until 8) {
        val angleRad = (i * 45f) * (Math.PI / 180f).toFloat()
        val startOffset = Offset(
            x = center.x + kotlin.math.cos(angleRad) * rayStart,
            y = center.y + kotlin.math.sin(angleRad) * rayStart
        )
        val endOffset = Offset(
            x = center.x + kotlin.math.cos(angleRad) * rayEnd,
            y = center.y + kotlin.math.sin(angleRad) * rayEnd
        )
        drawLine(
            color = color,
            start = startOffset,
            end = endOffset,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** 月牙与双星芒图标绘制 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMoonAndStarIcon(color: Color) {
    val center = Offset(size.width * 0.42f, size.height * 0.52f)
    val moonRadius = size.minDimension * 0.35f

    val moonPath = Path().apply {
        val mainCircle = Path().apply {
            addOval(Rect(center = center, radius = moonRadius))
        }
        val cutCircle = Path().apply {
            addOval(
                Rect(
                    center = Offset(center.x + moonRadius * 0.50f, center.y - moonRadius * 0.30f),
                    radius = moonRadius * 0.88f
                )
            )
        }
        op(mainCircle, cutCircle, PathOperation.Difference)
    }

    drawPath(path = moonPath, color = color)

    val star1Center = Offset(size.width * 0.74f, size.height * 0.28f)
    val star1Ray = 2.1.dp.toPx()
    val star1Stroke = 1.0.dp.toPx()

    drawLine(
        color = color,
        start = Offset(star1Center.x - star1Ray, star1Center.y),
        end = Offset(star1Center.x + star1Ray, star1Center.y),
        strokeWidth = star1Stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(star1Center.x, star1Center.y - star1Ray),
        end = Offset(star1Center.x, star1Center.y + star1Ray),
        strokeWidth = star1Stroke,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = color,
        radius = 0.6.dp.toPx(),
        center = star1Center
    )

    val star2Center = Offset(size.width * 0.88f, size.height * 0.46f)
    val star2Ray = 1.2.dp.toPx()
    val star2Stroke = 0.85.dp.toPx()

    drawLine(
        color = color,
        start = Offset(star2Center.x - star2Ray, star2Center.y),
        end = Offset(star2Center.x + star2Ray, star2Center.y),
        strokeWidth = star2Stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(star2Center.x, star2Center.y - star2Ray),
        end = Offset(star2Center.x, star2Center.y + star2Ray),
        strokeWidth = star2Stroke,
        cap = StrokeCap.Round
    )
}

/**
 * 现代颂歌 自由调色盘弹窗组件 (ThemeColorPickerDialog)
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
