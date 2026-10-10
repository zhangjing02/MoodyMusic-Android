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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.data.manager.DownloadStatus
import com.example.moodymusicforandroid.data.manager.OfflineDownloadManager
import com.example.moodymusicforandroid.data.model.SongItem
import com.example.moodymusicforandroid.data.model.ThemeStoryDto
import com.example.moodymusicforandroid.data.model.safeBodyParagraphs
import com.example.moodymusicforandroid.data.model.safeTimelineSections
import com.example.moodymusicforandroid.data.model.safeScenarios
import com.example.moodymusicforandroid.data.model.safeBenefits
import com.example.moodymusicforandroid.ui.album.AlbumDownloadIcon
import com.example.moodymusicforandroid.ui.components.DownloadedTrackBadge
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
    onSeekTo: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    storyUrl: String? = null,
    viewModel: ThemeDetailViewModel = viewModel()
) {
    BackHandler(onBack = onBackClick)

    LaunchedEffect(themeId, storyUrl) {
        viewModel.loadThemeStory(themeId, storyUrl)
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val downloadTasks by OfflineDownloadManager.tasksFlow.collectAsState()
    val downloadedSongs by OfflineDownloadManager.downloadedSongsFlow.collectAsState()

    val cleanTitle = remember(title) {
        title.substringBefore("—").replace("《", "").replace("》", "").trim()
    }
    val cleanAlbumTitle = remember(title) {
        if (title.contains("—")) title.substringAfter("—").trim() else "今日胶片精选 · 慢调专栏"
    }
    val currentTask = downloadTasks[audioUrl]
    val isDownloaded = remember(audioUrl, downloadedSongs) {
        OfflineDownloadManager.isDownloaded(
            filePath = audioUrl,
            title = cleanTitle,
            albumTitle = cleanAlbumTitle
        )
    }
    val isHashOutdated = remember(audioUrl, downloadedSongs) {
        OfflineDownloadManager.isHashOutdated(
            filePath = audioUrl,
            title = cleanTitle,
            albumTitle = cleanAlbumTitle
        )
    }

    val onDownloadAction: () -> Unit = {
        if (audioUrl.isBlank()) {
            Toast.makeText(context, "暂无有效音频直链", Toast.LENGTH_SHORT).show()
        } else if (isDownloaded && !isHashOutdated) {
            Toast.makeText(context, "《$cleanTitle》已下载至本地（高品质离线）", Toast.LENGTH_SHORT).show()
        } else {
            val songItem = SongItem(
                title = cleanTitle,
                path = audioUrl,
                lrcPath = null,
                id = null,
                fileHash = null
            )
            val heroUrl = (uiState as? ThemeDetailUiState.Success)?.story?.heroUrl?.takeIf { it.isNotBlank() } ?: coverUrl
            OfflineDownloadManager.enqueueSong(
                song = songItem,
                albumTitle = cleanAlbumTitle,
                artistName = artistName.ifBlank { "Moody Archive" },
                coverUrl = heroUrl
            )
            Toast.makeText(context, "已加入离线下载队列：《$cleanTitle》", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "THE MODERN SONGBOOK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
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
                actions = {
                    if (audioUrl.isNotBlank()) {
                        ThemeDownloadActionItem(
                            isDownloaded = isDownloaded,
                            isHashOutdated = isHashOutdated,
                            downloadStatus = currentTask?.status,
                            onClick = onDownloadAction
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
                        color = MaterialTheme.colorScheme.primary,
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                    isDownloaded = isDownloaded,
                    isHashOutdated = isHashOutdated,
                    downloadStatus = currentTask?.status,
                    onDownloadClick = onDownloadAction,
                    onPlayToggle = onPlayToggle,
                    onSeekTo = onSeekTo,
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
    isDownloaded: Boolean,
    isHashOutdated: Boolean,
    downloadStatus: DownloadStatus?,
    onDownloadClick: () -> Unit,
    onPlayToggle: () -> Unit,
    onSeekTo: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCurrentlyPlayingThis = isThisThemeActive && isPlaying

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

                // 封面右下角：若此卡片在播放状态，展示无背景纯净跳动 EQ 柱状动画
                if (isCurrentlyPlayingThis) {
                    CoverEqIndicator(
                        isAnimating = true,
                        barColor = MaterialTheme.colorScheme.primary,
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
            // 若此卡片正在播放，则隐藏上方播放按钮，仅在非播放中展示
            val showPlayButton = !isCurrentlyPlayingThis && audioUrl.isNotBlank()

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
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (showPlayButton) {
                    Box(
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), CircleShape)
                            .clickable { onPlayToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_play_arrow_rounded),
                            contentDescription = "播放伴读原声",
                            tint = MaterialTheme.colorScheme.primary,
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
                    color = MaterialTheme.colorScheme.primary,
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
                    Spacer(modifier = Modifier.height(10.dp))
                    val timelineTitle = story.timelineTitle.ifBlank { "⏱️ 乐章时序全景 · 曲目时间轴" }
                    Text(
                        text = timelineTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    story.safeTimelineSections.forEachIndexed { idx, section ->
                        val timeLabel = section.timeLabel
                        val targetMs = remember(timeLabel) { parseTimelineTimestampMs(timeLabel) }
                        val hasDetails = section.sceneStory.isNotBlank() ||
                                         section.emotion.isNotBlank() ||
                                         section.technique.isNotBlank() ||
                                         section.performerNote.isNotBlank()

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                SongbookColors.TerracottaBrown.copy(alpha = 0.20f)
                            ),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSeekTo(targetMs)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 顶栏：经典杂志/黑胶排版 —— 左侧章节标识 (SECTION 01)；右侧时间戳与播放提示 (00:01:27 ▶)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 左侧：经典章节大写标号
                                    Text(
                                        text = if (idx < 9) "SECTION 0${idx + 1}" else "SECTION ${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SongbookColors.TerracottaBrown,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    )

                                    // 右侧：时间节点与轻触播放三角 (00:01:27 ▶)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (timeLabel.isNotBlank()) {
                                            Text(
                                                text = timeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SongbookColors.Outline,
                                                fontWeight = FontWeight.SemiBold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(R.drawable.ic_play_arrow_rounded),
                                            contentDescription = "定位播放",
                                            tint = SongbookColors.BurntOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // 主标题：歌名 / 乐章名称（自动去除冗余开头的 01. / 02. 等数字前缀，保持纯粹经典）
                                val cleanTitle = remember(section.title) {
                                    section.title.replaceFirst(Regex("""^\d+[\.\、\s\-]+"""), "").trim()
                                }
                                Text(
                                    text = cleanTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )

                                // 下方导赏与解析：经典极简排版，绝不使用出戏的生硬 emoji 前缀
                                if (hasDetails) {
                                    val isStandardSingleDesc = section.sceneStory.isNotBlank() &&
                                            section.emotion.isBlank() &&
                                            section.technique.isBlank() &&
                                            section.performerNote.isBlank()
                                    if (isStandardSingleDesc) {
                                        // 经典内页副文本导赏（如演唱会解说、背景故事）
                                        Text(
                                            text = section.sceneStory,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    } else {
                                        // 结构化古典乐章多维解析（如梁祝、巴赫等）
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )

                                        if (section.sceneStory.isNotBlank()) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "剧情：",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = section.sceneStory,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 20.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        if (section.emotion.isNotBlank()) {
                                            Row(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "情绪：",
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
                                                    text = "技巧：",
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
                                                color = SongbookColors.TerracottaBrown.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp)
                                                ) {
                                                    Text(
                                                        text = "导赏：",
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
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 适用场景
                if (story.safeScenarios.isNotEmpty()) {
                    Text(
                        text = story.scenariosTitle.ifBlank { "🎧 适用场景" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
                        color = MaterialTheme.colorScheme.primary
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
        tint = MaterialTheme.colorScheme.primary,
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
    barColor: Color = MaterialTheme.colorScheme.primary
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

/**
 * 解析时间戳字符串为毫秒 (如 "04:19" -> 259000, "01:13:01" -> 4381000)
 */
private fun parseTimelineTimestampMs(timeStr: String): Int {
    if (timeStr.isBlank()) return 0
    val clean = timeStr.substringBefore("-").trim()
    val parts = clean.split(":").mapNotNull { it.trim().toIntOrNull() }
    return when (parts.size) {
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
        2 -> (parts[0] * 60 + parts[1]) * 1000
        1 -> parts[0] * 1000
        else -> 0
    }
}

/**
 * 专栏详情顶栏离线下载操作图标
 */
@Composable
private fun ThemeDownloadActionItem(
    isDownloaded: Boolean,
    isHashOutdated: Boolean,
    downloadStatus: DownloadStatus?,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        when {
            downloadStatus is DownloadStatus.DOWNLOADING -> {
                val progress = downloadStatus.progress
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(18.dp),
                        color = primary,
                        trackColor = primary.copy(alpha = 0.2f),
                        strokeWidth = 2.dp
                    )
                }
            }
            downloadStatus is DownloadStatus.QUEUED -> {
                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = primary.copy(alpha = 0.6f),
                        strokeWidth = 1.5.dp
                    )
                }
            }
            isDownloaded -> {
                DownloadedTrackBadge(
                    isHashOutdated = isHashOutdated,
                    size = 18.dp,
                    onClick = onClick
                )
            }
            else -> {
                AlbumDownloadIcon(
                    tint = primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

