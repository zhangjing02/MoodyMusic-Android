package com.example.moodymusicforandroid.ui.theme_detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.model.ThemeStoryDto
import com.example.moodymusicforandroid.data.model.safeBodyParagraphs
import com.example.moodymusicforandroid.data.model.safeTimelineSections
import com.example.moodymusicforandroid.data.model.safeScenarios
import com.example.moodymusicforandroid.data.model.safeBenefits
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 现代颂歌 主题音乐详情页 (ThemeDetailScreen)
 *
 * 100% 后端数据驱动 (Server-Driven UI)。
 * 所有专栏文案、时序乐章解析、封面大图及引语均由服务端下发，前端零硬编码。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(
    themeId: String,
    title: String,
    audioUrl: String,
    coverUrl: String,
    artistName: String,
    isPlaying: Boolean,
    isThisThemeActive: Boolean,
    isMiniPlayerVisible: Boolean,
    onBackClick: () -> Unit,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier,
    storyUrl: String? = null,
    viewModel: ThemeDetailViewModel = viewModel()
) {
    BackHandler(onBack = onBackClick)

    LaunchedEffect(themeId, storyUrl) {
        viewModel.loadThemeStory(themeId, storyUrl)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "THE MODERN SONGBOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.BurntOrange,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val issueTagText = when (val state = uiState) {
                            is ThemeDetailUiState.Success -> state.story.issueTag.ifBlank { "专栏深度导赏" }
                            else -> "专栏深度导赏"
                        }
                        Text(
                            text = issueTagText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (val state = uiState) {
            is ThemeDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SongbookColors.BurntOrange,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            is ThemeDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.loadThemeStory(themeId, storyUrl) },
                            colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                        ) {
                            Text("重新加载", color = Color.White)
                        }
                    }
                }
            }
            is ThemeDetailUiState.Success -> {
                ThemeDetailContent(
                    story = state.story,
                    title = title,
                    coverUrl = coverUrl,
                    audioUrl = audioUrl,
                    isPlaying = isPlaying,
                    isThisThemeActive = isThisThemeActive,
                    onPlayToggle = onPlayToggle,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ThemeDetailContent(
    story: ThemeStoryDto,
    title: String,
    coverUrl: String,
    audioUrl: String,
    isPlaying: Boolean,
    isThisThemeActive: Boolean,
    onPlayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 160.dp
        )
    ) {
        // 1. 专题主视觉海报图 (支持轻触播放伴读原声)
        item(key = "theme_hero_image") {
            val heroUrl = story.heroUrl?.takeIf { it.isNotBlank() } ?: coverUrl
            val aspect = if (story.isSquareCover) 1f else story.posterAspectRatio
            val isCurrentlyPlayingThis = isThisThemeActive && isPlaying

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onPlayToggle() }
            ) {
                SongbookImage(
                    model = heroUrl,
                    contentDescription = title,
                    fallbackRes = R.drawable.home_vinyl_banner,
                    modifier = Modifier.fillMaxSize()
                )

                // 封面右下角：若正在播放伴读原声，展示跳动 EQ 柱状动画
                if (isCurrentlyPlayingThis) {
                    CoverEqIndicator(
                        isAnimating = true,
                        barColor = SongbookColors.BurntOrangeLight,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(width = 22.dp, height = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // 2. 刊头标题与元信息
        item(key = "theme_header_text") {
            val showPlayButton = !isThisThemeActive && audioUrl.isNotBlank()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (story.categoryTag.isNotBlank()) {
                    Text(
                        text = story.categoryTag,
                        style = MaterialTheme.typography.labelMedium,
                        color = SongbookColors.TerracottaBrown,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (showPlayButton) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.BurntOrange.copy(alpha = 0.08f))
                            .border(1.dp, SongbookColors.BurntOrange.copy(alpha = 0.75f), CircleShape)
                            .clickable { onPlayToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_rounded),
                            contentDescription = "播放伴读原声",
                            tint = SongbookColors.BurntOrange,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayHeadline = story.headline.ifBlank { title }
            Text(
                text = displayHeadline,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )

            if (story.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = story.subtitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = SongbookColors.BurntOrange,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            }

            if (story.authorDate.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = story.authorDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. 杂志长文正文
        item(key = "theme_article_body") {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                story.safeBodyParagraphs.forEachIndexed { idx, p ->
                    Text(
                        text = p,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (idx == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 28.sp
                    )
                }

                // 杂志拉页金句引言（双语对照）
                if (story.quoteEn.isNotBlank() || story.quoteZh.isNotBlank()) {
                    Surface(
                        color = Color(0x33EDD9C0),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            0.8.dp,
                            SongbookColors.TerracottaBrown.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            if (story.quoteEn.isNotBlank()) {
                                Text(
                                    text = story.quoteEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = SongbookColors.TerracottaBrown,
                                    lineHeight = 24.sp
                                )
                            }
                            if (story.quoteEn.isNotBlank() && story.quoteZh.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            if (story.quoteZh.isNotBlank()) {
                                Text(
                                    text = story.quoteZh,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SongbookColors.TerracottaBrown,
                                    lineHeight = 26.sp
                                )
                            }
                        }
                    }
                }

                // 4. 乐章全景时序解析 (剧情 · 情绪 · 演奏技巧剖析)
                if (story.safeTimelineSections.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val timelineTitle = story.timelineTitle.ifBlank { "🎼 乐章时序全景图解 · 剧情 / 情绪 / 演奏技巧剖析" }
                    Text(
                        text = timelineTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )

                    story.safeTimelineSections.forEachIndexed { idx, section ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                SongbookColors.TerracottaBrown.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = SongbookColors.TerracottaBrown,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = section.timeLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = "SECTION 0${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SongbookColors.Outline,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.5.dp
                                )

                                if (section.sceneStory.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "📖 剧情：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.BurntOrange
                                        )
                                        Text(
                                            text = section.sceneStory,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (section.emotion.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "🎭 情绪：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.TerracottaBrown
                                        )
                                        Text(
                                            text = section.emotion,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (section.technique.isNotBlank()) {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "🎻 技巧：",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SongbookColors.BurntOrange
                                        )
                                        Text(
                                            text = section.technique,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (section.performerNote.isNotBlank()) {
                                    Surface(
                                        color = SongbookColors.TerracottaBrown.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp)
                                        ) {
                                            Text(
                                                text = "💡 导赏：",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = SongbookColors.TerracottaBrown
                                            )
                                            Text(
                                                text = section.performerNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontStyle = FontStyle.Italic,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 19.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 适用场景
                if (story.safeScenarios.isNotEmpty()) {
                    Text(
                        text = story.scenariosTitle.ifBlank { "🎧 适用场景" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            story.safeScenarios.forEach { s ->
                                Text(
                                    text = s,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // 好处 / 专题亮点
                if (story.safeBenefits.isNotEmpty()) {
                    Text(
                        text = story.benefitsTitle.ifBlank { "✨ 专题亮点" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.BurntOrange
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            story.safeBenefits.forEach { b ->
                                Text(
                                    text = b,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 频道结语与作者关于
                if (story.aboutDesc.isNotBlank() || story.aboutMotto.isNotBlank()) {
                    Surface(
                        color = Color(0x247A4A28),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (story.aboutTitle.isNotBlank()) {
                                Text(
                                    text = story.aboutTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SongbookColors.TerracottaBrown
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (story.aboutDesc.isNotBlank()) {
                                Text(
                                    text = story.aboutDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                            }
                            if (story.aboutMotto.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = story.aboutMotto,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Medium,
                                    color = SongbookColors.TerracottaBrown,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                if (story.footerSign.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = story.footerSign,
                        style = MaterialTheme.typography.labelMedium,
                        color = SongbookColors.Outline,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

/**
 * 低调三角播放按钮（保留备用）
 */
@Composable
private fun CoverPlayButton(
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(R.drawable.ic_play_arrow),
        contentDescription = "播放",
        tint = SongbookColors.BurntOrangeLight,
        modifier = modifier.size(26.dp)
    )
}

/**
 * EQ 柱状图播放指示器（状态2/3：此曲在播放器中时显示）
 *
 * - isAnimating = true（播放中）：三根柱子高度无限循环动画
 * - isAnimating = false（暂停）：三根柱子静止在中间高度
 */
@Composable
fun CoverEqIndicator(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = SongbookColors.BurntOrangeLight
) {
    val barCount = 3
    val infiniteTransition = rememberInfiniteTransition(label = "EqBars")

    val heights = (0 until barCount).map { index ->
        val animatedHeight by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 400 + index * 120,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EqBar$index"
        )
        if (isAnimating) animatedHeight else 0.45f
    }

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 2 - 1)
        val barGap = barWidth

        for (i in 0 until barCount) {
            val barHeight = totalHeight * heights[i]
            val left = i * (barWidth + barGap)
            val top = totalHeight - barHeight
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
