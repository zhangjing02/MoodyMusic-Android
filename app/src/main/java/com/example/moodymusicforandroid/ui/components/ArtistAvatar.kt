package com.example.moodymusicforandroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.moodymusicforandroid.common.config.AppConfig
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlin.math.abs

/**
 * 艺术调色盘（用于生成歌手独特质感的文字头像背景色）
 * 采用复古胶片与现代颂歌低饱和温暖色系
 */
private val AVATAR_PALETTES = listOf(
    listOf(Color(0xFFC86D51), Color(0xFFA34D35)), // 焦橙 / 陶土
    listOf(Color(0xFF6B7A60), Color(0xFF4D5B44)), // 鼠尾草绿 / 橄榄
    listOf(Color(0xFFB58450), Color(0xFF8F6233)), // 复古琥珀 / 黄铜
    listOf(Color(0xFF5E6D82), Color(0xFF414E61)), // 靛蓝 / 灰蓝
    listOf(Color(0xFF94685A), Color(0xFF754D41)), // 暖褐 / 赭石
    listOf(Color(0xFF7D6B7D), Color(0xFF5C4C5C)), // 枯紫 / 暮色
    listOf(Color(0xFF5C7873), Color(0xFF3F5854)), // 墨绿 / 青瓷
    listOf(Color(0xFF8C7355), Color(0xFF6E5539))  // 亚麻 / 浅褐
)

/**
 * 艺术家专用圆形头像组件
 * 
 * 逻辑：
 * 1. 若拥有后端真实图片（/storage/... 或 http...），优先异步加载真实照片
 * 2. 若无图片、图片为空或为网页占位路径（如 /src/assets/...），或网络加载失败，
 *    自动退化为各大音乐 App（Apple Music/Spotify）主流的【专属艺术首字徽标头像】，
 *    根据歌手姓名哈希分配专属复古色谱，居中显示首个汉字或首字母，彻底杜绝所有歌手千篇一律错乱头像问题。
 */
@Composable
fun ArtistAvatar(
    name: String,
    avatarUrl: String?,
    artistId: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp
) {
    val context = LocalContext.current

    // 计算该歌手专属渐变配色
    val palette = remember(name) {
        val index = abs(name.hashCode()) % AVATAR_PALETTES.size
        AVATAR_PALETTES[index]
    }

    // 提取展示文字（取首个有效字符，若英文取大写，中文直接取汉字）
    val displayChar = remember(name) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) "M"
        else {
            val first = trimmed.first()
            if (first in 'a'..'z') first.uppercaseChar().toString()
            else first.toString()
        }
    }

    // 智能解析头像优先级：
    // 1. 优先通过网络加载 Cloudflare R2 / 后端下发的高清图片 (https://... 或 /storage/artists/...)
    // 2. 若加载失败或图片为空，自动优雅降级为专属艺术首字徽标
    val resolvedModel = remember(avatarUrl) {
        val hasCustomNetworkAvatar = !avatarUrl.isNullOrBlank() &&
            !avatarUrl.contains("default.png") &&
            !avatarUrl.contains("landing_cover.png") &&
            !avatarUrl.startsWith("/src/")

        if (hasCustomNetworkAvatar) {
            AppConfig.resolveUrl(avatarUrl)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, SongbookColors.OutlineVariant.copy(alpha = 0.35f), CircleShape)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        ) {
            if (resolvedModel != null) {
                val imageRequest = remember(context, resolvedModel) {
                    ImageRequest.Builder(context)
                        .data(resolvedModel)
                        .crossfade(true)
                        .allowHardware(false) // 杜绝软绘崩溃
                        .build()
                }

                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = {
                        LetterAvatarFallback(
                            displayChar = displayChar,
                            palette = palette,
                            size = size
                        )
                    },
                    error = {
                        LetterAvatarFallback(
                            displayChar = displayChar,
                            palette = palette,
                            size = size
                        )
                    }
                )
            } else {
                LetterAvatarFallback(
                    displayChar = displayChar,
                    palette = palette,
                    size = size
                )
            }
        }
    }
}

@Composable
private fun LetterAvatarFallback(
    displayChar: String,
    palette: List<Color>,
    size: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = palette
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val fontSize = (size.value * 0.44f).sp
        Text(
            text = displayChar,
            color = Color.White.copy(alpha = 0.95f),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif
        )
    }
}
