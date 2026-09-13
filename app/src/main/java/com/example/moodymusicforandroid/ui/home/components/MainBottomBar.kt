package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.navigation.RouteDiscover
import com.example.moodymusicforandroid.ui.navigation.RouteHome
import com.example.moodymusicforandroid.ui.navigation.RouteLibrary
import com.example.moodymusicforandroid.ui.components.SongbookBlurContainer
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 稳定阴影颜色常量—避免 MainBottomBar 每次重组时创建新 Color 对象
private val ShadowAmbientColor = Color(0x221F1C18)
private val ShadowSpotColor    = Color(0x331F1C18)
// 高光折射层稳定颜色常量
private val SpecularHighlight1 = Color.White.copy(alpha = 0.35f)
private val SpecularHighlight2 = Color.White.copy(alpha = 0.10f)
// 毛玻璃边框稳定颜色常量
private val BorderHighlight    = Color.White.copy(alpha = 0.85f)

/**
 * 现代颂歌 硬件级实时动态毛玻璃胶囊底栏 (MainBottomBar)
 */
@Composable
fun MainBottomBar(
    currentRoute: Any,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    SongbookBlurContainer(
        modifier = modifier.fillMaxWidth(),
        hazeState = hazeState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        cornerRadius = 20.dp,
        elevation = 12.dp,
        overlayColor = Color(0x6EF8F9FA), // 苹果级液态磨砂玻璃：清透珠光淡灰，高通透度（约43%不透明度，能清晰透显底层图像轮廓与色彩）
        borderColor = Color.Transparent,  // 彻底移除边缘杂线
        borderWidth = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            MainBottomBarContent(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    }
}

/**
 * 导航栏纯内容布局（供独立显示或在一体化 Dock 容器中紧贴复用）
 */
@Composable
fun MainBottomBarContent(
    currentRoute: Any,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HomeNavIcon(
            isSelected = currentRoute is RouteHome,
            onClick = { onNavigate(RouteHome) }
        )
        DiscoverNavIcon(
            isSelected = currentRoute is RouteDiscover,
            onClick = { onNavigate(RouteDiscover) }
        )
        LibraryNavIcon(
            isSelected = currentRoute is RouteLibrary,
            onClick = { onNavigate(RouteLibrary) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 首页：优雅三角大钢琴 + 音符轻扬动效（琴声流淌，旋律跃出）
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HomeNavIcon(isSelected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        animationSpec = tween(220), label = "homeTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "homeScale"
    )
    val floatY by animateFloatAsState(
        targetValue = if (isSelected) -4f else 0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "homeFloat"
    )
    
    // 钢琴弹奏时跳跃出的微小音符粒子
    val noteBurst = remember { Animatable(0f) }
    LaunchedEffect(isSelected) {
        if (isSelected) {
            noteBurst.snapTo(0f)
            noteBurst.animateTo(1f, tween(500, easing = LinearOutSlowInEasing))
        } else {
            noteBurst.snapTo(0f)
        }
    }

    NavBox(onClick) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            // 1. 优雅三角大钢琴矢量图标
            Icon(
                painter = painterResource(R.drawable.ic_nav_home_vec),
                contentDescription = "首页",
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationY = floatY
                    },
                tint = tint
            )

            // 2. 启封时自信封上方飘扬而出的轻灵音符微粒子
            val burstColor = tint
            Canvas(modifier = Modifier.size(34.dp)) {
                val p = noteBurst.value
                if (p > 0.02f && p < 0.98f) {
                    val alpha = (1f - p) * 0.85f
                    val u = size.width / 24f

                    // 粒子 1：右上方小音符 ♪ 飘向高处
                    val p1X = size.width * 0.75f + (p * 3.5f * u)
                    val p1Y = size.height * 0.20f - (p * 8.0f * u)
                    val r1 = 1.2f * u
                    drawCircle(color = burstColor.copy(alpha = alpha), radius = r1, center = Offset(p1X, p1Y), style = Fill)
                    drawLine(
                        color = burstColor.copy(alpha = alpha),
                        start = Offset(p1X + r1 * 0.8f, p1Y),
                        end   = Offset(p1X + r1 * 0.8f, p1Y - 2.8f * u),
                        strokeWidth = 0.9f * u,
                        cap = StrokeCap.Round
                    )

                    // 粒子 2：左上方微光点 ✦
                    if (p > 0.1f) {
                        val alpha2 = (1f - p) * 0.7f
                        val p2X = size.width * 0.35f - (p * 2.5f * u)
                        val p2Y = size.height * 0.22f - (p * 6.5f * u)
                        drawCircle(color = burstColor.copy(alpha = alpha2), radius = 0.9f * u, center = Offset(p2X, p2Y), style = Fill)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 发现：矢量行星 + 卫星 360° 椭圆轨道绕行
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DiscoverNavIcon(isSelected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        animationSpec = tween(220), label = "discoverTint"
    )
    val planetScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "planetScale"
    )
    val orbitProgress = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            orbitProgress.snapTo(0f)
            orbitProgress.animateTo(1f, tween(780, easing = FastOutSlowInEasing))
        } else {
            orbitProgress.snapTo(0f)
        }
    }

    NavBox(onClick) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_discover_vec),
                contentDescription = "发现",
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer { scaleX = planetScale; scaleY = planetScale },
                tint = tint
            )

            val satColor = tint
            Canvas(modifier = Modifier.size(34.dp)) {
                val p = orbitProgress.value
                if (p < 0.01f || p > 0.99f) return@Canvas

                val cx    = size.width  * 0.47f
                val cy    = size.height * 0.50f
                val rx    = size.width  * 0.41f
                val ry    = size.height * 0.15f
                val tiltR = (-28f * PI / 180f).toFloat()
                val cosT  = cos(tiltR)
                val sinT  = sin(tiltR)

                val startAngle = (-68f * PI / 180f).toFloat()
                val θ = startAngle + p * 2f * PI.toFloat()

                val ex = rx * cos(θ)
                val ey = ry * sin(θ)
                val satX = cx + ex * cosT - ey * sinT
                val satY = cy + ex * sinT + ey * cosT

                drawCircle(
                    color  = satColor,
                    radius = size.width * 0.07f,
                    center = Offset(satX, satY),
                    style  = Fill
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 音信：极简经典曲别针 + 金属回弹微振动反馈
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LibraryNavIcon(isSelected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        animationSpec = tween(220), label = "libTint"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "libScale"
    )

    val pluckAngle = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            pluckAngle.snapTo(0f)
            pluckAngle.animateTo(8.5f, tween(90, easing = FastOutSlowInEasing))
            pluckAngle.animateTo(-6.0f, tween(110, easing = FastOutSlowInEasing))
            pluckAngle.animateTo(3.5f, tween(120, easing = FastOutSlowInEasing))
            pluckAngle.animateTo(-1.5f, tween(120, easing = FastOutSlowInEasing))
            pluckAngle.animateTo(0f, tween(130, easing = LinearOutSlowInEasing))
        } else {
            pluckAngle.snapTo(0f)
        }
    }

    NavBox(onClick) {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_library_vec),
                contentDescription = "音信",
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.35f, 0.76f)
                        rotationZ = pluckAngle.value
                    },
                tint = tint
            )
        }
    }
}

@Composable
private fun NavBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
