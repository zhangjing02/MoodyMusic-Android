package com.example.moodymusicforandroid.ui.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.ui.components.SwipeToRevealDelete
import com.example.moodymusicforandroid.ui.playlist.AddToPlaylistSheet
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.cos
import kotlin.math.sin

/**
 * 全屏沉浸式专属播放页面 (NowPlayingScreen) - 艺术级复古磁带与极简排版重构
 *
 * 核心优化：
 * 1. 磁带方向纠正（对齐参考图 media_1789132874035.png）：
 *    - 经典磁头梯形接触区朝上（Top），底边保持平正规整，彻底纠正“头朝下”问题；
 *    - 歌曲名、歌手信息直接印制于磁带标贴纸（Label Sticker）下方，宛若真实专辑磁带；
 *    - 收藏心动红心作为复古钢印徽章，精致嵌于磁带右上角，点击点亮红心。
 * 2. 播放模式归位底部左侧：
 *    - 控制栏最左侧恢复为「循环/播放模式 🔁」键；
 *    - 顶栏精简为纯粹的「收起 ⌄」与「关闭 ✕」。
 * 3. 移除“极高音质”冗余文案：
 *    - 进度条仅保留 2dp 极细发丝轨道与两端纯净时间文字。
 * 4. 宽幅自适应歌词沉浸区：
 *    - 节省出原标题栏空间，歌词视口更加宏大、透气，双向平滑羽化。
 */
@Composable
fun NowPlayingScreen(
    playState: MusicPlayState,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onTogglePlayMode: () -> Unit = {},
    onSelectQueueItem: (Int) -> Unit = {},
    onRemoveQueueItem: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onAddToCurrentQueue: (() -> AddToQueueResult)? = null,
    modifier: Modifier = Modifier
) {
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }

    BackHandler {
        if (showAddToPlaylistSheet) {
            showAddToPlaylistSheet = false
        } else if (showQueueSheet) {
            showQueueSheet = false
        } else {
            onCollapse()
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var dominantColor by remember { mutableStateOf(Color(0xFF1E1715)) }
    val currentSongId = remember(playState.songTitle, playState.artistName) {
        val key = "${playState.songTitle.trim()}_${playState.artistName.trim()}"
        key.hashCode().toLong() and 0x7FFFFFFF
    }
    val favoriteSongIds by UserManager.favoriteSongIds.collectAsState()
    val isFavorite = currentSongId in favoriteSongIds

    // 从封面提取沉浸式主色
    LaunchedEffect(playState.coverUrl) {
        if (playState.coverUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context)
                        .data(playState.coverUrl)
                        .allowHardware(false)
                        .build()
                    val result = (loader.execute(request) as? SuccessResult)?.drawable
                    val bitmap = (result as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val swatch = palette.darkVibrantSwatch
                            ?: palette.dominantSwatch
                            ?: palette.mutedSwatch
                        swatch?.rgb?.let { rgb -> dominantColor = Color(rgb) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "DynamicBgColor"
    )

    // 磁带走带轮无限平滑匀速旋转系统（基于 VSync 帧时钟连续累加，彻底告别周期重启与卡顿）
    var continuousReelAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playState.isPlaying) {
        if (playState.isPlaying) {
            var lastTimeNanos = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { frameTimeNanos ->
                    val dt = (frameTimeNanos - lastTimeNanos) / 1_000_000_000f
                    lastTimeNanos = frameTimeNanos
                    // 每秒匀速平稳旋转 90 度（4 秒一圈），无限持续递增，暂停时保持原位，播放时丝滑续转
                    continuousReelAngle = (continuousReelAngle + dt * 90f) % 360000f
                }
            }
        }
    }
    val currentReelAngle = continuousReelAngle

    // 歌词加载逻辑
    var lyricsLines by remember { mutableStateOf<List<LrcLine>>(emptyList()) }
    var isPureMusic by remember { mutableStateOf(false) }
    var isLyricsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(playState.songTitle, playState.artistName, playState.lrcPath) {
        val title = playState.songTitle.trim()
        val artist = playState.artistName.trim()

        if (title.isBlank()) {
            lyricsLines = emptyList()
            isPureMusic = false
            return@LaunchedEffect
        }

        // 判断是否为纯音乐（支持简繁体）
        val lowerTitle = title.lowercase()
        val lowerArtist = artist.lowercase()
        val isInstrumental = lowerTitle.contains("钢琴") || lowerTitle.contains("鋼琴") ||
                lowerTitle.contains("piano") || lowerTitle.contains("纯音乐") ||
                lowerTitle.contains("純音樂") || lowerTitle.contains("伴奏") ||
                lowerTitle.contains("reading") || lowerTitle.contains("阅读") ||
                lowerTitle.contains("閱讀") || lowerTitle.contains("instrumental") ||
                lowerArtist.contains("piano") || lowerArtist.contains("鋼琴") ||
                lowerArtist.contains("钢琴")

        if (isInstrumental && playState.lrcPath.isNullOrBlank()) {
            isPureMusic = true
            lyricsLines = emptyList()
            return@LaunchedEffect
        } else {
            isPureMusic = false
        }

        isLyricsLoading = true
        scope.launch(Dispatchers.IO) {
            val loaded = fetchLyricsChain(playState.lrcPath, title, artist)
            withContext(Dispatchers.Main) {
                lyricsLines = loaded
                isLyricsLoading = false
            }
        }
    }

    // 计算当前歌词播放行
    val currentLyricIndex by remember(playState.position, lyricsLines) {
        derivedStateOf {
            if (lyricsLines.isEmpty()) return@derivedStateOf -1
            val posSec = playState.position / 1000.0
            val idx = lyricsLines.indexOfLast { it.timeSec <= posSec }
            if (idx < 0) 0 else idx
        }
    }

    // 播放进度百分比 (0.0 ~ 1.0)
    val playProgressRatio = remember(playState.position, playState.duration) {
        if (playState.duration > 0) {
            (playState.position.toFloat() / playState.duration).coerceIn(0f, 1f)
        } else {
            0.15f
        }
    }

    // 深色动态渐变底衬
    val backgroundBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0.0f to animatedBgColor.copy(alpha = 0.95f),
            0.45f to Color(0xFF141318),
            1.0f to Color(0xFF0A0A0C)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0C))
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 1. 顶栏：紧贴两边极窄间距（4.dp），使收起与关闭图标更靠近屏幕左右边缘 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = "收起播放页",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = playState.albumTitle.ifBlank { "Moody Archive" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "关闭音乐并关闭悬浮窗",
                        tint = Color.White.copy(alpha = 0.80f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 2. 经典正向复古磁带（内置歌名/歌手与右上角红心微章） ─────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .aspectRatio(1.56f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(12.dp),
                        ambientColor = Color(0x70000000),
                        spotColor = Color(0x90000000)
                    )
            ) {
                CassetteTapeIntegratedView(
                    reelAngle = currentReelAngle,
                    progressRatio = playProgressRatio,
                    isPlaying = playState.isPlaying,
                    albumTitle = playState.albumTitle.ifBlank { "CLASSIC TAPE" },
                    songTitle = playState.songTitle.ifBlank { "未播放音乐" },
                    artistName = playState.artistName.ifBlank { "Moody Music" },
                    isFavorite = isFavorite,
                    onFavoriteToggle = {
                        if (playState.songTitle.isNotBlank()) {
                            UserManager.toggleFavoriteSong(
                                songId = currentSongId,
                                songTitle = playState.songTitle,
                                artistName = playState.artistName,
                                coverUrl = playState.coverUrl,
                                audioUrl = playState.audioUrl
                            )
                        }
                    },
                    onAddToPlaylist = {
                        showAddToPlaylistSheet = true
                    },
                    onTapeClick = onPlayPauseToggle,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 3. 宽幅沉浸式歌词区（双向平滑羽化渐变遮罩） ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp)
            ) {
                FadingLyricsList(
                    lyrics = lyricsLines,
                    currentIndex = currentLyricIndex,
                    isPureMusic = isPureMusic,
                    isLoading = isLyricsLoading,
                    onLineClick = { timeSec -> onSeekTo((timeSec * 1000).toInt()) },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── 4. 2dp 极细发丝级纯净进度条（移除“极高音质”文案） ─────────────
            DelicateHairlineProgressBar(
                positionMs = playState.position,
                durationMs = playState.duration,
                onSeekTo = onSeekTo,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── 5. 5 键黄金比例控制栏（播放模式放左下角） ────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. 播放/循环模式键（置于左下角，符合标准手势直觉）
                val modeIcon = when (playState.playMode) {
                    PlayMode.LIST_LOOP -> R.drawable.ic_repeat
                    PlayMode.SINGLE_LOOP -> R.drawable.ic_repeat_one
                    PlayMode.SHUFFLE -> R.drawable.ic_shuffle
                    PlayMode.SEQUENTIAL -> R.drawable.ic_order
                }

                IconButton(
                    onClick = onTogglePlayMode,
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        painter = painterResource(modeIcon),
                        contentDescription = playState.playMode.label,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. 上一曲
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "上一首",
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 3. 播放/暂停（轻量化透光圆环，48dp 黄金尺寸）
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPlayPauseToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (playState.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = if (playState.isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 4. 下一曲
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "下一首",
                        tint = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // 5. 播放列表键
                IconButton(
                    onClick = { showQueueSheet = true },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_playlist),
                        contentDescription = "播放列表",
                        tint = if (showQueueSheet) SongbookColors.BurntOrangeLight else Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── 6. 播放队列底部弹窗 (PlayQueueBottomSheet) ──
        PlayQueueBottomSheet(
            visible = showQueueSheet,
            queue = playState.queue,
            currentIndex = playState.playlistIndex,
            isPlaying = playState.isPlaying,
            playMode = playState.playMode,
            onDismiss = { showQueueSheet = false },
            onTogglePlayMode = onTogglePlayMode,
            onSelectSong = { index ->
                onSelectQueueItem(index)
            },
            onRemoveSong = { index ->
                onRemoveQueueItem(index)
            },
            onClearQueue = {
                onClearQueue()
                showQueueSheet = false
            }
        )

        // ── 7. 听歌归档抽屉 (AddToPlaylistSheet) ──
        AddToPlaylistSheet(
            visible = showAddToPlaylistSheet,
            songId = currentSongId,
            songTitle = playState.songTitle,
            artistName = playState.artistName,
            albumTitle = playState.albumTitle,
            coverUrl = playState.coverUrl,
            filePath = playState.audioUrl,
            duration = (playState.duration / 1000).toInt(),
            currentQueue = playState.queue,
            onAddToCurrentQueue = onAddToCurrentQueue,
            onDismiss = { showAddToPlaylistSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// 正向复古磁带组件 (带磁带贴纸内置歌名/歌手 + 右上角收藏红心与收录磁带徽章)
// ─────────────────────────────────────────────────────────────────

@Composable
private fun CassetteTapeIntegratedView(
    reelAngle: Float,
    progressRatio: Float,
    isPlaying: Boolean,
    albumTitle: String,
    songTitle: String,
    artistName: String,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onTapeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTapeClick
            )
    ) {
        // 1. 磁带外壳、顶部梯形磁头区与动态走带视窗绘制 (Canvas)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAccurateVintageCassette(
                reelAngle = reelAngle,
                progressRatio = progressRatio,
                albumTitle = albumTitle
            )
        }

        // 2. 左上角「收录到私藏磁带」图标（书签＋，与右侧红心对称）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 1.dp, start = 12.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAddToPlaylist
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark_add),
                contentDescription = "收录到音乐手札",
                tint = Color(0xFF7A6A56),
                modifier = Modifier.size(20.dp)
            )
        }

        // 3. 右上角「收藏红心」操作区
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 1.dp, end = 12.dp)
                .size(36.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFavoriteToggle
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                ),
                contentDescription = "收藏歌曲",
                tint = if (isFavorite) Color(0xFFE53935) else Color(0xFF7A6A56),
                modifier = Modifier.size(19.dp)
            )
        }

        // 3. 磁带贴纸下方直接印制【歌曲名】与【歌手】（宛如真实黑胶/卡带标题）
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = songTitle,
                color = Color(0xFF221C16),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.5.dp))
            Text(
                text = artistName,
                color = Color(0xFF6E5C4B),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 依据参考图 media_1789132874035.png 精准绘制正向复古磁带
 * - 磁头梯形区位于顶部 (Top)；
 * - 4 个导带孔与中心微孔位于顶部；
 * - 双走带轮视窗居中偏上；
 * - 磁带贴纸下方提供充足的曲目文字印刷区。
 */
private fun DrawScope.drawAccurateVintageCassette(
    reelAngle: Float,
    progressRatio: Float,
    albumTitle: String
) {
    val w = size.width
    val h = size.height

    // ── 1. 象牙复古外壳底色 ──
    val shellColor = Color(0xFFECE5D8)
    val shellHighlight = Color(0xFFF9F6EE)
    val shellShadow = Color(0xFFD4C8B5)
    val shellCorner = CornerRadius(10.dp.toPx())

    // 绘制整体圆角矩形
    drawRoundRect(
        color = shellColor,
        size = size,
        cornerRadius = shellCorner
    )
    drawRoundRect(
        color = shellHighlight,
        size = Size(w, h * 0.5f),
        cornerRadius = shellCorner,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawRoundRect(
        color = shellShadow,
        size = size,
        cornerRadius = shellCorner,
        style = Stroke(width = 1.dp.toPx())
    )

    // ── 2. 顶部磁头梯形接触区 (Head Opening at TOP) ──
    val trapW = w * 0.50f
    val trapH = h * 0.17f
    val trapX = (w - trapW) / 2f
    val trapY = 0f

    // 顶部梯形凹槽底色
    drawRoundRect(
        color = Color(0xFFDDD3C0),
        topLeft = Offset(trapX, trapY),
        size = Size(trapW, trapH),
        cornerRadius = CornerRadius(0f, 0f)
    )
    drawRoundRect(
        color = Color(0xFFC7BBA5),
        topLeft = Offset(trapX, trapY),
        size = Size(trapW, trapH),
        cornerRadius = CornerRadius(0f, 0f),
        style = Stroke(width = 0.8.dp.toPx())
    )

    // 梯形区内部真实导带孔 (4 个孔 + 1 个中心微孔，与参考图完全一致)
    val holeR = 3.5.dp.toPx()
    val slotW = 5.dp.toPx()
    val slotH = 7.dp.toPx()
    val holeY = trapH * 0.52f

    // 左右各两个方形孔与圆形孔
    drawRoundRect(
        color = Color(0xFF7A6A56),
        topLeft = Offset(trapX + trapW * 0.12f, holeY - slotH / 2f),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(1.5.dp.toPx())
    )
    drawCircle(
        color = Color(0xFF7A6A56),
        radius = holeR,
        center = Offset(trapX + trapW * 0.28f, holeY)
    )
    // 中心孔
    drawCircle(
        color = Color(0xFF7A6A56),
        radius = 2.5.dp.toPx(),
        center = Offset(trapX + trapW * 0.50f, holeY)
    )
    // 右侧导带孔
    drawCircle(
        color = Color(0xFF7A6A56),
        radius = holeR,
        center = Offset(trapX + trapW * 0.72f, holeY)
    )
    drawRoundRect(
        color = Color(0xFF7A6A56),
        topLeft = Offset(trapX + trapW * 0.88f - slotW, holeY - slotH / 2f),
        size = Size(slotW, slotH),
        cornerRadius = CornerRadius(1.5.dp.toPx())
    )

    // ── 3. 磁带贴纸标贴区 (Label Sticker) ──
    val stickerMarginH = w * 0.05f
    val stickerTop = trapH + h * 0.015f
    val stickerW = w - stickerMarginH * 2f
    val stickerH = h - stickerTop - h * 0.045f
    val stickerCorner = CornerRadius(6.dp.toPx())

    // 贴纸米黄底纸
    drawRoundRect(
        color = Color(0xFFFAF7F0),
        topLeft = Offset(stickerMarginH, stickerTop),
        size = Size(stickerW, stickerH),
        cornerRadius = stickerCorner
    )
    drawRoundRect(
        color = Color(0xFFD6CCA8),
        topLeft = Offset(stickerMarginH, stickerTop),
        size = Size(stickerW, stickerH),
        cornerRadius = stickerCorner,
        style = Stroke(width = 0.8.dp.toPx())
    )

    // 贴纸上方复古赛车条纹 (焦橙色 + 深棕双条)
    val stripeY = stickerTop + h * 0.02f
    val stripeH1 = h * 0.028f
    val stripeH2 = h * 0.010f
    drawRect(
        color = SongbookColors.BurntOrange.copy(alpha = 0.90f),
        topLeft = Offset(stickerMarginH, stripeY),
        size = Size(stickerW, stripeH1)
    )
    drawRect(
        color = Color(0xFF4A3427),
        topLeft = Offset(stickerMarginH, stripeY + stripeH1),
        size = Size(stickerW, stripeH2)
    )

    // ── 4. 中央透明走带视窗 (Clear Tape Window) ──
    val winW = stickerW * 0.72f
    val winH = stickerH * 0.44f
    val winX = (w - winW) / 2f
    val winY = stripeY + stripeH1 + stripeH2 + h * 0.035f
    val winCorner = CornerRadius(5.dp.toPx())

    // 视窗深色底衬
    drawRoundRect(
        color = Color(0xFF1E1A16),
        topLeft = Offset(winX, winY),
        size = Size(winW, winH),
        cornerRadius = winCorner
    )
    drawRoundRect(
        color = Color(0xFF4A3E32),
        topLeft = Offset(winX, winY),
        size = Size(winW, winH),
        cornerRadius = winCorner,
        style = Stroke(width = 1.dp.toPx())
    )

    // ── 5. 走带轮轴心与动态磁带卷 ──
    val winCenterY = winY + winH / 2f
    val leftCX = winX + winW * 0.28f
    val rightCX = winX + winW * 0.72f

    val maxTapeR = winH * 0.44f
    val minTapeR = winH * 0.25f
    val leftTapeR = minTapeR + (maxTapeR - minTapeR) * (1f - progressRatio)
    val rightTapeR = minTapeR + (maxTapeR - minTapeR) * progressRatio

    // 左磁带卷
    drawCircle(
        color = Color(0xFF2E1F16),
        radius = leftTapeR,
        center = Offset(leftCX, winCenterY)
    )
    // 右磁带卷
    drawCircle(
        color = Color(0xFF2E1F16),
        radius = rightTapeR,
        center = Offset(rightCX, winCenterY)
    )

    // 磁带横向连接带
    val bridgeY = winCenterY + minTapeR * 0.65f
    drawLine(
        color = Color(0xFF2E1F16),
        start = Offset(leftCX, bridgeY),
        end = Offset(rightCX, bridgeY),
        strokeWidth = 2.8.dp.toPx()
    )

    // 刻度尺微刻度
    val rulerY = winCenterY - winH * 0.05f
    for (i in 0..10) {
        val rx = winX + winW * 0.42f + (winW * 0.16f) * (i / 10f)
        val tickH = if (i % 5 == 0) 4.5.dp.toPx() else 2.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.40f),
            start = Offset(rx, rulerY - tickH / 2f),
            end = Offset(rx, rulerY + tickH / 2f),
            strokeWidth = 1.dp.toPx()
        )
    }

    // ── 6. 双 6 齿齿轮走带轮 (6-Tooth Cog Hubs) ──
    val hubRadius = winH * 0.24f
    val hubHoleRadius = hubRadius * 0.48f

    listOf(
        Pair(leftCX, reelAngle),
        Pair(rightCX, reelAngle)
    ).forEach { (cx, angle) ->
        drawCircle(
            color = Color(0xFFF4EDE2),
            radius = hubRadius,
            center = Offset(cx, winCenterY)
        )
        drawCircle(
            color = Color(0xFFC7BBA8),
            radius = hubRadius,
            center = Offset(cx, winCenterY),
            style = Stroke(width = 0.8.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF191613),
            radius = hubHoleRadius,
            center = Offset(cx, winCenterY)
        )

        val teethCount = 6
        for (t in 0 until teethCount) {
            val toothAngleRad = Math.toRadians((angle + t * (360f / teethCount)).toDouble())
            val toothInner = Offset(
                cx + (hubHoleRadius * 0.65f * cos(toothAngleRad)).toFloat(),
                winCenterY + (hubHoleRadius * 0.65f * sin(toothAngleRad)).toFloat()
            )
            val toothOuter = Offset(
                cx + (hubHoleRadius * 1.15f * cos(toothAngleRad)).toFloat(),
                winCenterY + (hubHoleRadius * 1.15f * sin(toothAngleRad)).toFloat()
            )
            drawLine(
                color = Color(0xFFFAF6EE),
                start = toothInner,
                end = toothOuter,
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Square
            )
        }
    }

    // ── 7. 底边两侧微防写孔 (Bottom Corner Tabs) ──
    val notchW = w * 0.08f
    val notchH = h * 0.04f
    drawRoundRect(
        color = Color(0xFFDDD3C0),
        topLeft = Offset(w * 0.08f, h - notchH),
        size = Size(notchW, notchH),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
    drawRoundRect(
        color = Color(0xFFDDD3C0),
        topLeft = Offset(w - w * 0.08f - notchW, h - notchH),
        size = Size(notchW, notchH),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

// ─────────────────────────────────────────────────────────────────
// 网易云同款 2dp 极细发丝级精致进度条（纯净版，无冗余音质文案）
// ─────────────────────────────────────────────────────────────────

@Composable
private fun DelicateHairlineProgressBar(
    positionMs: Int,
    durationMs: Int,
    onSeekTo: (Int) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = maxOf(1, durationMs)
    val currentRatio = if (isDragging) dragProgress else (positionMs.toFloat() / safeDuration).coerceIn(0f, 1f)
    val displayPositionMs = (currentRatio * safeDuration).toInt()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(safeDuration) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekTo((ratio * safeDuration).toInt())
                    }
                }
                .pointerInput(safeDuration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekTo((dragProgress * safeDuration).toInt())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                val cy = size.height / 2f
                val strokeW = 2.dp.toPx()
                val activeWidth = size.width * currentRatio

                // 底轨 (2dp 半透明白)
                drawLine(
                    color = Color.White.copy(alpha = 0.20f),
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // 已播轨道 (2dp 高亮)
                if (activeWidth > 0) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, cy),
                        end = Offset(activeWidth, cy),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }

                // 极简圆形小滑块 (Thumb)
                val thumbR = if (isDragging) 5.5.dp.toPx() else 4.dp.toPx()
                val thumbX = activeWidth.coerceIn(thumbR, size.width - thumbR)

                drawCircle(
                    color = Color(0x33000000),
                    radius = thumbR + 1.dp.toPx(),
                    center = Offset(thumbX, cy + 0.5.dp.toPx())
                )
                drawCircle(
                    color = Color.White,
                    radius = thumbR,
                    center = Offset(thumbX, cy)
                )
            }
        }

        // 仅保留清晰优雅的纯净时间刻度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMs(displayPositionMs),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Normal
            )

            Text(
                text = if (durationMs > 0) formatMs(durationMs) else "--:--",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 沉浸式宽幅歌词区（双向平滑羽化渐变遮罩 + 秒级居中滚动）
// ─────────────────────────────────────────────────────────────────

@Composable
private fun FadingLyricsList(
    lyrics: List<LrcLine>,
    currentIndex: Int,
    isPureMusic: Boolean,
    isLoading: Boolean,
    onLineClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0 && lyrics.isNotEmpty()) {
            listState.animateScrollToItem(
                index = currentIndex.coerceIn(0, lyrics.lastIndex),
                scrollOffset = -with(density) { 50.dp.roundToPx() }
            )
        }
    }

    Box(
        modifier = modifier
            .drawWithContent {
                drawContent()
                // 顶部淡出遮罩
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C0B0E), Color.Transparent),
                        startY = 0f,
                        endY = 48.dp.toPx()
                    )
                )
                // 底部淡出遮罩
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF0C0B0E)),
                        startY = size.height - 48.dp.toPx(),
                        endY = size.height
                    )
                )
            }
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "正在获取歌词...",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.40f)
                    )
                }
            }
            isPureMusic -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "“ 纯音乐，请静心欣赏 ”",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.70f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pure Instrumental Melody",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }
            lyrics.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无歌词",
                        fontSize = 13.5.sp,
                        color = Color.White.copy(alpha = 0.35f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(vertical = 36.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isHighlighted = index == currentIndex
                        val isNear = kotlin.math.abs(index - currentIndex) == 1

                        val alpha = when {
                            isHighlighted -> 1.0f
                            isNear -> 0.55f
                            else -> 0.22f
                        }
                        val fontSize = if (isHighlighted) 16.sp else if (isNear) 14.sp else 13.sp
                        val fontWeight = FontWeight.Normal

                        Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = alpha),
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onLineClick(line.timeSec) }
                                )
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 多级歌词获取策略与 LRC 解析模块
// ─────────────────────────────────────────────────────────────────

data class LrcLine(val timeSec: Double, val text: String)

private suspend fun fetchLyricsChain(
    lrcPath: String?,
    songTitle: String,
    artistName: String
): List<LrcLine> {
    // 1. 优先尝试从云存储直连加载
    if (!lrcPath.isNullOrBlank()) {
        val rawUrl = AppConfig.resolveStorageUrl(lrcPath)
        val fullUrl = AppConfig.canonicalizeUrl(rawUrl)
        val timestampUrl = if (fullUrl.contains("?")) "$fullUrl&t=${System.currentTimeMillis()}" else "$fullUrl?t=${System.currentTimeMillis()}"
        try {
            val url = URL(timestampUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }
            if (connection.responseCode == 200) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val parsed = parseLrcText(text)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (_: Exception) {}
    }

    // 2. 本地经典内置歌词库匹配
    val cleanTitle = songTitle.substringBefore("—").substringBefore("(").substringBefore("（").trim()
    val localLrc = BuiltinLyricsDB.find(cleanTitle, artistName)
    if (localLrc != null) {
        val parsed = parseLrcText(localLrc)
        if (parsed.isNotEmpty()) return parsed
    }

    // 3. 线上开放歌词 API 查询 (api.lyrics.ovh)
    try {
        val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
        val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
        val apiUrl = "https://api.lyrics.ovh/v1/$encodedArtist/$encodedTitle"
        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4000
            readTimeout = 4000
        }
        if (connection.responseCode == 200) {
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            val lyricsContent = json.substringAfter("\"lyrics\":\"", "").substringBeforeLast("\"")
                .replace("\\n", "\n").replace("\\r", "").replace("\\\"", "\"")
            if (lyricsContent.isNotBlank()) {
                val lines = lyricsContent.lines().filter { it.isNotBlank() }
                return lines.mapIndexed { idx, line ->
                    LrcLine(timeSec = idx * 5.0, text = line.trim())
                }
            }
        }
    } catch (_: Exception) {}

    return emptyList()
}

private val LRC_TIME_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]""")

private fun parseLrcText(rawText: String): List<LrcLine> {
    val lines = mutableListOf<LrcLine>()

    for (line in rawText.lines()) {
        val matches = LRC_TIME_REGEX.findAll(line).toList()
        if (matches.isEmpty()) continue
        val content = line.replace(LRC_TIME_REGEX, "").trim()
        if (content.isBlank()) continue

        for (m in matches) {
            val min = m.groupValues[1].toDoubleOrNull() ?: continue
            val sec = m.groupValues[2].toDoubleOrNull() ?: continue
            val msStr = m.groupValues[3].padEnd(3, '0').substring(0, 3)
            val ms = msStr.toDoubleOrNull() ?: 0.0
            val totalSec = min * 60.0 + sec + (ms / 1000.0)
            lines.add(LrcLine(timeSec = totalSec, text = content))
        }
    }
    return lines.sortedBy { it.timeSec }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private object BuiltinLyricsDB {
    fun find(title: String, artist: String): String? {
        return when {
            title.contains("星晴") -> """
[00:09.54]作曲 : 周杰伦
[00:11.86]一步两步三步四步
[00:13.49]望著天 手牵手
[00:16.64]一颗两颗三颗四颗
[00:18.81]连成线看星星
[00:21.59]一步两步三步四步
[00:24.16]望著天 手牵手
[00:26.89]一颗两颗三颗四颗
[00:29.61]连成线 看星星
[00:33.00]乘著风 游荡在蓝天边
[00:38.50]一片云掉落在我面前
[00:43.86]捏成你的形状 随风跟著我
[00:48.46]一口一口吃掉忧愁
[00:54.57]载着你 彷彿载著阳光
[00:59.97]不管到哪里都是晴天
[01:05.44]蝴蝶自在飞 花也布满天
[01:10.05]一朵一朵因你而香
[01:14.74]试图让夕阳飞翔 带领你我环绕大自然
[01:27.62]迎著风 开始共渡每一天
[01:36.09]手牵手 一步两步三步四步
[01:39.78]望著天 看星星
[01:42.62]一颗两颗三颗四颗 连成线
[01:46.94]背对背 默默许下心愿
[01:51.64]看远方的星 是否听的见
[01:58.73]一步两步三步四步
[02:01.43]望著天 看星星
[02:04.14]一颗两颗三颗四颗 连成线
[02:08.51]背对背 默默许下心愿
[02:13.19]看远方的星 是否听得见
[02:18.65]它一定实现
            """.trimIndent()

            title.contains("山丘") || (artist.contains("李宗盛") && title.contains("理性")) -> """
[00:30.00]想说却还没说的 还很多
[00:37.50]攒着是因为想写成歌
[00:45.00]让人轻轻地唱着 淡淡地记着
[00:52.50]就算终于忘了 也值了
[01:03.00]说不定我一生涓滴意念
[01:09.50]侥幸汇成河
[01:16.00]然后我俩各自一端
[01:22.50]望着大河弯弯 终于敢放胆
[01:29.00]嬉皮笑脸 面对 人生的难
[01:36.00]也许我们从未成熟
[01:42.00]还没能晓得 就快要老了
[01:48.00]尽管心里活着的还是那个年轻人
[01:55.00]因为越过山丘 才发现无人等候
[02:02.00]喋喋不休 再也唤不回温柔
[02:09.00]为何记不得 上一次是谁给的拥抱
[02:16.00]在什么时候
[02:22.00]越过山丘 虽然已白了头
[02:29.00]喋喋不休 时不我予的哀愁
[02:36.00]还舍不得 曾经的奢望
[02:43.00]只顾着跟往事瞎扯
            """.trimIndent()

            title.contains("崇拜") -> """
[00:23.20]就算失敗了一百遍
[00:28.69]不管剩下多少時間
[00:34.26]最遙遠的距離 就是當我說放棄
[00:40.18]即使幸福已經很近
[00:45.14]不需要安慰的字句
[00:50.48]就算需要很多運氣
[00:55.72]我永遠不放棄 擁有幸福的權利
[01:02.15]等你願意我就願意
[01:05.86]就在第一百零一遍 當我慢慢睜開眼
[01:13.01]喔 也許你就會出現
[01:16.65]失敗了一百零一遍 也要不斷的許願
[01:23.55]神啊 再給我些時間
[01:34.13]每一遍都是新的體驗
[01:39.58]一轉眼又跨了一年
[01:44.84]昨天的每一天 都是為了要讓自己
[01:51.30]到明天更勇敢一點
            """.trimIndent()

            else -> null
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 播放队列底部弹窗 (PlayQueueBottomSheet) 与列表项组件
// ─────────────────────────────────────────────────────────────────

@Composable
private fun PlayQueueBottomSheet(
    visible: Boolean,
    queue: List<PlayQueueItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    playMode: PlayMode,
    onDismiss: () -> Unit,
    onTogglePlayMode: () -> Unit,
    onSelectSong: (Int) -> Unit,
    onRemoveSong: (Int) -> Unit,
    onClearQueue: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(180))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.60f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.62f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF19171C))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // 拦截点击穿透
                    )
                    .padding(top = 10.dp)
            ) {
                // 顶部拖拽装饰条
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 顶栏：播放模式切换 + 歌曲数量统计 + 清空列表
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val modeIcon = when (playMode) {
                        PlayMode.LIST_LOOP -> R.drawable.ic_repeat
                        PlayMode.SINGLE_LOOP -> R.drawable.ic_repeat_one
                        PlayMode.SHUFFLE -> R.drawable.ic_shuffle
                        PlayMode.SEQUENTIAL -> R.drawable.ic_order
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onTogglePlayMode)
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(modeIcon),
                            contentDescription = playMode.label,
                            tint = SongbookColors.BurntOrangeLight,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "${playMode.label} (${queue.size})",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (queue.isNotEmpty()) {
                        IconButton(
                            onClick = onClearQueue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_trash),
                                contentDescription = "清空队列",
                                tint = Color.White.copy(alpha = 0.45f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 细分隔线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                // 队列歌曲列表
                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "播放队列为空",
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(queue, key = { _, item -> item.queueId }) { index, item ->
                            val isCurrent = index == currentIndex
                            PlayQueueRowItem(
                                modifier = Modifier.animateItem(),
                                item = item,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying,
                                onClick = { onSelectSong(index) },
                                onRemove = { onRemoveSong(index) }
                            )
                        }
                    }
                }

                // 底部分隔线与“关闭”按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "关闭",
                        color = Color.White.copy(alpha = 0.70f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayQueueRowItem(
    item: PlayQueueItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleColor = if (isCurrent) SongbookColors.BurntOrangeLight else Color.White.copy(alpha = 0.88f)

    SwipeToRevealDelete(
        onDelete = onRemove,
        modifier = modifier,
        deleteLabel = "移除",
        deleteColor = Color(0xFFFF3B30),
        contentBackgroundColor = Color(0xFF19171C)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 当前播放指示器（音波或占位）
            if (isCurrent) {
                PlayingEqualizerBars(
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            // 歌名
            Text(
                text = item.songTitle,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 仿网易云/Apple Music 动态三柱跳动音波
 */
@Composable
private fun PlayingEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barWidth = 2.5.dp
        val maxH = 13.dp
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(if (isPlaying) (maxH * h1).coerceAtLeast(3.dp) else 4.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SongbookColors.BurntOrangeLight)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(if (isPlaying) (maxH * h2).coerceAtLeast(3.dp) else 10.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SongbookColors.BurntOrangeLight)
        )
        Box(
            modifier = Modifier
                .width(barWidth)
                .height(if (isPlaying) (maxH * h3).coerceAtLeast(3.dp) else 7.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SongbookColors.BurntOrangeLight)
        )
    }
}

