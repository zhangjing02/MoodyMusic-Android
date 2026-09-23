package com.example.moodymusicforandroid.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.moodymusicforandroid.ui.components.SongbookPullToRefreshLayout
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.common.utils.PinyinUtils
import com.example.moodymusicforandroid.ui.home.components.ArtistDirectoryItem
import com.example.moodymusicforandroid.ui.home.viewmodel.DiscoverViewModel
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel(),
    onMenuClick: () -> Unit = {},
    onArtistClick: (artistId: String, artistName: String, avatarUrl: String?) -> Unit = { _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("全部") }
    val genres = listOf("全部", "民谣", "爵士乐", "摇滚", "古典", "流行")

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 监听 ViewModel 中的数据与刷新状态
    val artistsFromVm by viewModel.artists.observeAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullToRefreshState = rememberPullToRefreshState()

    // 本地默认兜底名录
    val defaultArtists = listOf(
        DirectoryArtist(
            id = "abigail_chen",
            name = "Abigail Chen",
            initial = "A",
            genre = "流行",
            albumCount = 4,
            avatarUrl = null,
            fallbackRes = R.drawable.artist_abigail_chen
        ),
        DirectoryArtist(
            id = "charlie_puth",
            name = "查理·普斯",
            initial = "C",
            genre = "流行",
            albumCount = 15,
            avatarUrl = null,
            fallbackRes = R.drawable.artist_charlie
        ),
        DirectoryArtist(
            id = "hero_acoustic",
            name = "林间碎影",
            initial = "L",
            genre = "民谣",
            albumCount = 9,
            avatarUrl = null,
            fallbackRes = R.drawable.hero_acoustic_guitar
        ),
        DirectoryArtist(
            id = "sunset_rollercoaster",
            name = "落日飞车",
            initial = "L",
            genre = "爵士乐",
            albumCount = 6,
            avatarUrl = null,
            fallbackRes = R.drawable.album_modern_jazz
        ),
        DirectoryArtist(
            id = "omnipresent_youth",
            name = "万能青年旅店",
            initial = "W",
            genre = "摇滚",
            albumCount = 2,
            avatarUrl = null,
            fallbackRes = R.drawable.album_hebei_kirin
        ),
        DirectoryArtist(
            id = "ryuichi_sakamoto",
            name = "坂本龍一",
            initial = "S",
            genre = "古典",
            albumCount = 24,
            avatarUrl = null,
            fallbackRes = R.drawable.album_classical_piano
        )
    )

    // 合并后端数据与本地数据，使用 PinyinUtils 将汉字准确映射为标准拼音首字母 ('A'..'Z', '#')
    val allArtists = remember(artistsFromVm) {
        if (artistsFromVm.isNotEmpty()) {
            artistsFromVm.map { vmArtist ->
                val pinyinInitial = PinyinUtils.getPinyinInitial(vmArtist.name)
                DirectoryArtist(
                    id = vmArtist.id,
                    name = vmArtist.name,
                    initial = pinyinInitial,
                    genre = vmArtist.category ?: "华语",
                    albumCount = vmArtist.albumCount,
                    avatarUrl = vmArtist.avatar,
                    fallbackRes = R.drawable.artist_abigail_chen
                )
            }
        } else {
            defaultArtists
        }
    }

    // 过滤与按拼音字母分组 (A-Z 升序，# 归在最后)
    val filteredArtists = allArtists.filter { artist ->
        val matchesSearch = searchQuery.isBlank() ||
            artist.name.contains(searchQuery, ignoreCase = true) ||
            artist.genre.contains(searchQuery, ignoreCase = true)
        val matchesGenre = selectedGenre == "全部" || artist.genre == selectedGenre
        matchesSearch && matchesGenre
    }

    val groupedArtists = remember(filteredArtists) {
        filteredArtists.groupBy { it.initial }
            .toSortedMap(Comparator { a, b ->
                if (a == "#") 1
                else if (b == "#") -1
                else a.compareTo(b)
            })
    }

    // 完整的字母导航表（A-Z + #）
    val alphabetList = remember {
        ('A'..'Z').map { it.toString() } + "#"
    }

    // 预先计算每个字母在 LazyColumn 中的起始条目绝对索引，支持精准跳转
    val groupIndexMap = remember(groupedArtists) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 3 // 前置 3 个 item: Search(0), Genres(1), ArchiveTitle(2)
        groupedArtists.forEach { (initial, artistsInGroup) ->
            map[initial] = currentIndex
            currentIndex += 1 + artistsInGroup.size // 1个分组大标题 + N个艺术家条目
        }
        map
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarContentHeight = 44.dp
    val topBarTotalHeight = statusBarTop + topBarContentHeight

    // 滚动驱动的 TopBar 背景 Alpha (0f -> 1f)：
    // 在顶部未滚动时：alpha = 0f，完全透明，融入背景，零色差！
    // 向上滑动前 60dp 过程中：alpha 平滑过渡到 1f，变身为实色吸顶栏，完美遮挡滑过的卡片
    val topBarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 120f).coerceIn(0f, 1f)
            }
        }
    }

    // 跟踪手势拖拽选中的字母与快速滚动协程
    var draggingLetter by remember { mutableStateOf<String?>(null) }
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    val availableInitials = remember(groupedArtists) { groupedArtists.keys.toList() }

    // 智能最近分区索引检索：即便某个字母暂无歌手，也能智能定位到最近分区，彻底杜绝手势滑动卡死
    fun getTargetScrollIndex(letter: String): Int? {
        if (groupedArtists.isEmpty()) return null
        if (groupIndexMap.containsKey(letter)) return groupIndexMap[letter]
        val letterIdx = alphabetList.indexOf(letter)
        if (letterIdx < 0) return null

        var closestInitial: String? = null
        var minDiff = Int.MAX_VALUE
        for (init in availableInitials) {
            val idx = alphabetList.indexOf(init)
            if (idx >= 0) {
                val diff = kotlin.math.abs(idx - letterIdx)
                if (diff < minDiff) {
                    minDiff = diff
                    closestInitial = init
                }
            }
        }
        return closestInitial?.let { groupIndexMap[it] }
    }

    // 当手动滚动列表时，动态识别当前顶部正显示哪个字母分区
    val visibleSectionLetter by remember(groupIndexMap) {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            var currentLetter: String? = null
            for ((letter, targetIdx) in groupIndexMap) {
                if (firstIdx >= targetIdx) {
                    currentLetter = letter
                } else {
                    break
                }
            }
            currentLetter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SongbookPullToRefreshLayout(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchArtists() },
            state = pullToRefreshState,
            headerTopPadding = statusBarTop,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 20.dp, end = 28.dp),
                    contentPadding = PaddingValues(
                        top = topBarTotalHeight + 8.dp,
                        bottom = 140.dp
                    )
                ) {
                    // 1. 极简低饱和搜索框 (首个列表项)
                    item(key = "header_search") {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        placeholder = {
                            Text(
                                text = "搜索歌手、流派或乐器",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = SongbookColors.Outline
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 3. 流派筛选胶囊
                item(key = "header_genres") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(genres) { genre ->
                            val isSelected = selectedGenre == genre
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) SongbookColors.BurntOrange else MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.clickable { selectedGenre = genre }
                            ) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }

                // 4. 档案名录大标题
                item(key = "header_archive_title") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "档案名录",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "A-Z ARCHIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SongbookColors.Outline,
                            letterSpacing = 2.sp
                        )
                    }
                    HorizontalDivider(
                        color = SongbookColors.OutlineVariant.copy(alpha = 0.25f),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 5. 按拼音字母分组的艺术家列表
                groupedArtists.forEach { (initial, artistsInGroup) ->
                    item(key = "group_header_$initial") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 18.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                color = SongbookColors.BurntOrange,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            HorizontalDivider(
                                color = SongbookColors.OutlineVariant.copy(alpha = 0.25f),
                                thickness = 0.5.dp
                            )
                        }
                    }

                    items(artistsInGroup, key = { it.id }) { artist ->
                        ArtistDirectoryItem(
                            name = artist.name,
                            genre = artist.genre,
                            albumCount = artist.albumCount,
                            avatarUrl = artist.avatarUrl,
                            artistId = artist.id,
                            fallbackRes = artist.fallbackRes,
                            onClick = { onArtistClick(artist.id, artist.name, artist.avatarUrl) }
                        )
                    }
                }

                if (groupedArtists.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "未找到相关艺术家档案",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 顶部吸顶 TopBar（位于列表之上，最高 Z-Order）
            // - 在最顶部未滚动时：topBarAlpha 为 0f，完全透明，融入页面背景色，0 色差！
            // - 向上滚动时：topBarAlpha 渐变到 1f，平滑变身为实体背景 + 细分割线，无缝吸顶；
            // - 下拉刷新时：随整个内容整体下移，完全不遮挡顶部的刷新文字与旋转指示器。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = topBarAlpha))
                    .padding(top = statusBarTop)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 28.dp, top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = SongbookColors.BurntOrange
                        )
                    }

                    Text(
                        text = "歌手",
                        style = MaterialTheme.typography.headlineMedium,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.size(36.dp))
                }
                // 底部细分割线，同样跟随 topBarAlpha 淡入淡出
                HorizontalDivider(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f * topBarAlpha),
                    thickness = 0.5.dp
                )
            }
        }
    }

        // 7. 右侧快捷字母导航索引栏（侧边水滴跟随气泡 + 波浪放大动效 + 智能就近吸附 + 磨砂导轨）
        AlphabetIndexBar(
            alphabetList = alphabetList,
            groupedArtists = groupedArtists,
            activeLetter = draggingLetter ?: visibleSectionLetter,
            onLetterSelected = { letter ->
                draggingLetter = letter
                val targetIndex = getTargetScrollIndex(letter)
                if (targetIndex != null) {
                    scrollJob?.cancel()
                    scrollJob = coroutineScope.launch {
                        listState.scrollToItem(targetIndex)
                    }
                }
            },
            onDragStateChange = { isDragging ->
                if (!isDragging) {
                    draggingLetter = null
                    scrollJob?.cancel()
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp, top = statusBarTop + 64.dp, bottom = 120.dp)
                .fillMaxHeight(0.72f)
        )
    }
}

/**
 * 侧边气泡指示器几何形状 (SpeechBubbleShape)
 * 采用圆角矩形主体 + 右侧三角形指示气嘴，直指用户手指触碰字母
 */
class SpeechBubbleShape(
    private val cornerRadiusPx: Float,
    private val arrowWidthPx: Float,
    private val arrowHeightPx: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val r = cornerRadiusPx.coerceAtMost(h / 2f).coerceAtMost((w - arrowWidthPx) / 2f)
        val bodyWidth = w - arrowWidthPx
        val centerY = h / 2f
        val halfArrowH = arrowHeightPx / 2f

        val path = Path().apply {
            moveTo(r, 0f)
            lineTo(bodyWidth - r, 0f)
            arcTo(
                rect = Rect(bodyWidth - 2 * r, 0f, bodyWidth, 2 * r),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(bodyWidth, centerY - halfArrowH)
            lineTo(w, centerY)
            lineTo(bodyWidth, centerY + halfArrowH)
            lineTo(bodyWidth, h - r)
            arcTo(
                rect = Rect(bodyWidth - 2 * r, h - 2 * r, bodyWidth, h),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(r, h)
            arcTo(
                rect = Rect(0f, h - 2 * r, 2 * r, h),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            lineTo(0f, r)
            arcTo(
                rect = Rect(0f, 0f, 2 * r, 2 * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * 现代原生字母快捷导航索引条 (AlphabetIndexBar)
 *
 * 对标微信通讯录、网易云音乐、QQ音乐主流做法：
 * 1. 【侧边水滴气泡】：跟随手势平滑垂直位移，带三角形指示指针直指触碰字母；
 * 2. 【波浪放大动效】：当前字母以 1.35x 放大凸出，邻近字母联动放大 1.15x；
 * 3. 【半透明磨砂导轨】：触控激活柔和胶囊导轨，宽触控区（28dp）不脱轨；
 * 4. 【自适应全展现】：weight(1f) 保证 A-Z + # 共 27 个字符永远 100% 完整显示；
 * 5. 【双向高亮联动】：列表翻阅与手势拖拽双向高亮；
 * 6. 【触觉振动】：滑过每个字母触发微振动。
 */
@Composable
private fun AlphabetIndexBar(
    alphabetList: List<String>,
    groupedArtists: Map<String, List<DirectoryArtist>>,
    activeLetter: String?,
    onLetterSelected: (String) -> Unit,
    onDragStateChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var barHeightPx by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current
    var lastSelectedIndex by remember { mutableIntStateOf(-1) }
    val density = LocalDensity.current

    val touchStripWidth = 28.dp
    val bubbleWidth = 56.dp
    val bubbleHeight = 44.dp
    val bubbleHeightPx = with(density) { bubbleHeight.toPx() }

    fun selectLetter(y: Float) {
        if (barHeightPx <= 0f || alphabetList.isEmpty()) return
        val ratio = (y / barHeightPx).coerceIn(0f, 0.999f)
        val index = (ratio * alphabetList.size).toInt().coerceIn(0, alphabetList.size - 1)
        if (index != lastSelectedIndex) {
            lastSelectedIndex = index
            val letter = alphabetList[index]
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onLetterSelected(letter)
        }
    }

    // 计算水滴气泡的目标 Y 偏移量（垂直居中对齐当前触控的字母）
    val targetBubbleTop = remember(lastSelectedIndex, barHeightPx, bubbleHeightPx) {
        if (lastSelectedIndex >= 0 && barHeightPx > 0 && alphabetList.isNotEmpty()) {
            val itemH = barHeightPx / alphabetList.size
            (lastSelectedIndex + 0.5f) * itemH - bubbleHeightPx / 2f
        } else {
            0f
        }
    }

    val animatedBubbleTop by animateFloatAsState(
        targetValue = targetBubbleTop,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bubbleVerticalAnim"
    )

    Box(
        modifier = modifier
    ) {
        // 1. 侧边水滴气泡 (跟随手指垂直滑动)
        AnimatedVisibility(
            visible = isDragging && lastSelectedIndex in alphabetList.indices,
            enter = fadeIn(tween(80)) + scaleIn(tween(80), initialScale = 0.7f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        x = -(touchStripWidth + 4.dp).roundToPx(),
                        y = animatedBubbleTop.toInt()
                    )
                }
        ) {
            val currentLetter = alphabetList.getOrNull(lastSelectedIndex) ?: ""
            Box(
                modifier = Modifier
                    .size(width = bubbleWidth, height = bubbleHeight)
                    .shadow(
                        elevation = 8.dp,
                        shape = SpeechBubbleShape(
                            cornerRadiusPx = with(density) { 12.dp.toPx() },
                            arrowWidthPx = with(density) { 8.dp.toPx() },
                            arrowHeightPx = with(density) { 14.dp.toPx() }
                        ),
                        ambientColor = SongbookColors.BurntOrange.copy(alpha = 0.35f),
                        spotColor = SongbookColors.BurntOrange.copy(alpha = 0.45f)
                    )
                    .background(
                        color = SongbookColors.BurntOrange,
                        shape = SpeechBubbleShape(
                            cornerRadiusPx = with(density) { 12.dp.toPx() },
                            arrowWidthPx = with(density) { 8.dp.toPx() },
                            arrowHeightPx = with(density) { 14.dp.toPx() }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 偏左 4dp 避开右侧小三角，使文字在气泡主体圆角矩形内完美居中
                Box(
                    modifier = Modifier.padding(end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentLetter,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. 字母触摸感应导轨与波浪字母列表
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(touchStripWidth)
                .fillMaxHeight()
                .background(
                    color = if (isDragging) SongbookColors.BurntOrange.copy(alpha = 0.10f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .border(
                    width = if (isDragging) 1.dp else 0.dp,
                    color = if (isDragging) SongbookColors.BurntOrange.copy(alpha = 0.20f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .onGloballyPositioned { coordinates ->
                    barHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(alphabetList, groupedArtists) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isDragging = true
                        onDragStateChange(true)
                        selectLetter(down.position.y)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                selectLetter(change.position.y)
                                change.consume()
                            } else {
                                break
                            }
                        }
                        isDragging = false
                        onDragStateChange(false)
                        lastSelectedIndex = -1
                    }
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                alphabetList.forEachIndexed { index, letter ->
                    val hasArtists = groupedArtists.containsKey(letter)
                    val isSelectedInDrag = isDragging && (index == lastSelectedIndex)
                    val isCurrentActive = !isDragging && (letter == activeLetter)

                    // 波浪放大物理效果：选中的字母放大 1.35x 并向左偏移，邻近字母放大 1.15x
                    val distance = if (isDragging && lastSelectedIndex >= 0) {
                        kotlin.math.abs(index - lastSelectedIndex)
                    } else {
                        Int.MAX_VALUE
                    }

                    val scale = when (distance) {
                        0 -> 1.35f
                        1 -> 1.15f
                        else -> 1.0f
                    }
                    val translationX = when (distance) {
                        0 -> -3.dp
                        1 -> -1.5.dp
                        else -> 0.dp
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.translationX = translationX.toPx()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentActive && hasArtists) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(SongbookColors.BurntOrange)
                            )
                        }

                        Text(
                            text = letter,
                            fontSize = if (isSelectedInDrag) 12.sp
                                      else if (isCurrentActive && hasArtists) 10.sp
                                      else 9.sp,
                            fontWeight = if (isSelectedInDrag || (isCurrentActive && hasArtists)) FontWeight.Bold
                                         else if (hasArtists) FontWeight.Medium
                                         else FontWeight.Normal,
                            color = if (isSelectedInDrag) SongbookColors.BurntOrange
                                    else if (isCurrentActive && hasArtists) Color.White
                                    else if (hasArtists) SongbookColors.BurntOrange
                                    else SongbookColors.Outline.copy(alpha = 0.28f),
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * 档案名录模型
 */
data class DirectoryArtist(
    val id: String,
    val name: String,
    val initial: String,
    val genre: String,
    val albumCount: Int,
    val avatarUrl: String?,
    val fallbackRes: Int
)
