package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.example.moodymusicforandroid.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 首页：信封 + 音符 —— 纯矢量 Canvas 绘制
 *
 * 结构（均为独立可动 path 元素）：
 *   ① envelope_body   信封外框矩形
 *   ② envelope_bottom 信封底部内折 V 线
 *   ③ envelope_flap   信封上盖（可展开的三角形盖子）← 动效：flapOpen 控制盖子展开角度
 *   ④ note_head       音符符头（填充椭圆）← 动效：noteFloat 控制垂直上浮偏移
 *   ⑤ note_stem       音符符杆
 *   ⑥ note_flag       音符符尾
 *
 * 动效：
 *   - 选中时 flapOpen 从 0→1，信封盖子从闭合位置向上翻开约 35% 信封高度
 *   - 同时 noteFloat 从 0→1，音符上浮约 10% 图标高度，带弹性回弹
 */
@Composable
fun HomeIconCanvas(
    modifier: Modifier,
    tint: Color,
    isSelected: Boolean
) {
    val flapOpen by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "flapOpen"
    )
    val noteFloat by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "noteFloat"
    )

    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        val sw = W * 0.088f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // ─── 信封坐标 ───
        val el   = W * 0.03f
        val er   = W * 0.75f
        val et   = H * 0.30f
        val eb   = H * 0.88f
        val emx  = (el + er) / 2f
        val envH = eb - et

        // ① 信封外框
        val bodyPath = Path().apply {
            moveTo(el, et);  lineTo(er, et)
            lineTo(er, eb);  lineTo(el, eb); close()
        }
        drawPath(bodyPath, color = tint, style = stroke)

        // ② 信封底部内折 V（从两个底角折向中心）
        val bottomV = Path().apply {
            moveTo(el, eb);  lineTo(emx, et + envH * 0.5f);  lineTo(er, eb)
        }
        drawPath(bottomV, color = tint, style = stroke)

        // ③ 信封盖子（动态 peak 高度）
        //    flapOpen=0 → peak 在信封中部偏上（闭合状态，与 V 交叉）
        //    flapOpen=1 → peak 提升到 et 上方（完全展开）
        val closedPeakY = et + envH * 0.40f
        val openPeakY   = et - envH * 0.35f
        val peakY       = closedPeakY + (openPeakY - closedPeakY) * flapOpen
        val flapPath = Path().apply {
            moveTo(el, et);  lineTo(emx, peakY);  lineTo(er, et)
        }
        drawPath(flapPath, color = tint, style = stroke)

        // ─── 音符坐标 ───
        val nx   = W * 0.84f
        val nR   = W * 0.086f
        // ④ 符头向上漂浮：flapOpen=0 → baseline，flapOpen=1 → 上移 10%
        val nBaseY = H * 0.33f - H * 0.10f * noteFloat

        rotate(-15f, pivot = Offset(nx, nBaseY)) {
            drawOval(
                color = tint,
                topLeft = Offset(nx - nR * 1.35f, nBaseY - nR * 0.82f),
                size    = Size(nR * 2.4f, nR * 1.5f),
                style   = Fill
            )
        }

        // ⑤ 符杆
        val stemX = nx + nR * 0.85f
        drawLine(
            color = tint,
            start = Offset(stemX, nBaseY - nR * 0.3f),
            end   = Offset(stemX, nBaseY - nR * 3.6f),
            strokeWidth = sw * 0.82f,
            cap = StrokeCap.Round
        )

        // ⑥ 符尾（贝塞尔曲线）
        val flagPath = Path().apply {
            val sy = nBaseY - nR * 3.6f
            moveTo(stemX, sy)
            cubicTo(
                stemX + nR * 1.9f, sy + nR * 0.3f,
                stemX + nR * 2.2f, sy + nR * 1.6f,
                stemX + nR * 0.5f, sy + nR * 2.2f
            )
        }
        drawPath(flagPath, color = tint,
            style = Stroke(width = sw * 0.82f, cap = StrokeCap.Round))
    }
}

/**
 * 发现：行星 + 倾斜星轨 + 卫星 —— 纯矢量 Canvas 绘制
 *
 * 结构：
 *   ① orbit_back     星轨后半（在行星后方，半透明）
 *   ② planet         行星圆圈
 *   ③ orbit_front    星轨前半（在行星前方，全不透明）
 *   ④ satellite_star 固定卫星星标（4芒星，位于轨道右上方）
 *   ⑤ satellite_dot  动画卫星亮点 ← 动效：沿参数方程椭圆轨迹运动 360°
 *
 * 动效：
 *   - 选中时 orbitProgress 0→1，卫星亮点按参数方程 (rx·cos θ, ry·sin θ) 旋转坐标
 *     绕椭圆轨道精确运行整整一圈（FastOutSlowIn，750ms 先快后缓）
 *   - 行星本体同步微弹性呼吸放大 ×1.08
 */
@Composable
fun DiscoverIconCanvas(
    modifier: Modifier,
    tint: Color,
    isSelected: Boolean
) {
    val orbitProgress = remember { Animatable(0f) }
    val planetScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "planetScale"
    )

    LaunchedEffect(isSelected) {
        if (isSelected) {
            orbitProgress.snapTo(0f)
            orbitProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
            )
        } else {
            orbitProgress.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        val sw   = W * 0.088f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // 行星中心及椭圆轨道参数
        val cx    = W * 0.47f
        val cy    = H * 0.52f
        val pR    = W * 0.22f * planetScale  // 行星半径

        val orx   = W * 0.46f               // 轨道椭圆长半轴
        val ory   = H * 0.17f               // 轨道椭圆短半轴
        val tiltR = (-28f * PI / 180f).toFloat()
        val cosT  = cos(tiltR)
        val sinT  = sin(tiltR)

        // 椭圆上任意角度 θ 对应的屏幕坐标
        fun orbitPt(theta: Float): Offset {
            val ex = orx * cos(theta)
            val ey = ory * sin(theta)
            return Offset(cx + ex * cosT - ey * sinT,
                          cy + ex * sinT + ey * cosT)
        }

        val steps = 80

        // ① 轨道后半（π→2π），半透明，在行星后方
        val backPath = Path()
        for (i in 0..steps) {
            val t = PI.toFloat() + i.toFloat() / steps * PI.toFloat()
            val pt = orbitPt(t)
            if (i == 0) backPath.moveTo(pt.x, pt.y) else backPath.lineTo(pt.x, pt.y)
        }
        drawPath(backPath, color = tint.copy(alpha = 0.38f), style = stroke)

        // ② 行星
        drawCircle(color = tint, radius = pR, center = Offset(cx, cy), style = stroke)

        // ③ 轨道前半（0→π），全不透明，在行星前方
        val frontPath = Path()
        for (i in 0..steps) {
            val t = i.toFloat() / steps * PI.toFloat()
            val pt = orbitPt(t)
            if (i == 0) frontPath.moveTo(pt.x, pt.y) else frontPath.lineTo(pt.x, pt.y)
        }
        drawPath(frontPath, color = tint, style = stroke)

        // ④ 固定卫星星标（位于轨道右上 -68° 处）
        val starAngle  = (-68f * PI / 180f).toFloat()
        val starCenter = orbitPt(starAngle)
        val starR      = W * 0.05f
        for (k in 0 until 4) {
            val a = k * PI.toFloat() / 2f
            drawLine(
                color  = tint,
                start  = Offset(starCenter.x + cos(a - PI.toFloat() / 4f) * starR * 0.3f,
                                starCenter.y + sin(a - PI.toFloat() / 4f) * starR * 0.3f),
                end    = Offset(starCenter.x + cos(a) * starR,
                                starCenter.y + sin(a) * starR),
                strokeWidth = sw * 0.65f,
                cap    = StrokeCap.Round
            )
        }

        // ⑤ 动画卫星亮点（参数椭圆方程精确运行，orbitProgress 0→1 = 完整一圈）
        val p = orbitProgress.value
        if (p > 0.01f && p < 0.99f) {
            val movingAngle = starAngle + p * 2f * PI.toFloat()
            val satPos = orbitPt(movingAngle)
            drawCircle(color = tint, radius = W * 0.068f, center = satPos, style = Fill)
        }
    }
}

/**
 * 收藏：木吉他轮廓 + 回形针 —— 纯矢量 Canvas 绘制
 *
 * 结构：
 *   ① guitar_lower_bout  吉他下箱体（大圆弧）
 *   ② guitar_upper_bout  吉他上箱体（小圆弧）
 *   ③ guitar_neck        吉他琴颈
 *   ④ guitar_soundhole   音孔
 *   ⑤ clip_outer         回形针外圈大弧
 *   ⑥ clip_inner         回形针内圈小弧
 *   ⑦ clip_tip           回形针弯折尖端 ← 动效：tipDx 仅移动这一个端点 ±2.5px
 *
 * 动效：
 *   - 选中时 clipTipDx 快速振荡 0→+2.5→-2→+1→0（约 230ms）
 *   - 仅 clip_tip 端点发生偏移，其余所有路径保持静止
 *   - 效果：好像用指甲弹了一下回形针弯头尖端，其余部分纹丝不动
 */
@Composable
fun LibraryIconCanvas(
    modifier: Modifier,
    tint: Color,
    isSelected: Boolean
) {
    val clipTipDx = remember { Animatable(0f) }
    val bodyScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bodyScale"
    )

    LaunchedEffect(isSelected) {
        if (isSelected) {
            clipTipDx.snapTo(0f)
            clipTipDx.animateTo( 2.5f, tween(55))
            clipTipDx.animateTo(-2.0f, tween(70))
            clipTipDx.animateTo( 1.0f, tween(55))
            clipTipDx.animateTo( 0.0f, tween(45))
        } else {
            clipTipDx.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        val W  = size.width
        val H  = size.height
        val px = W / 34f   // 1dp in canvas pixels
        val sw = W * 0.088f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)

        // ── 吉他（左侧约 60%）──
        val gx  = W * 0.31f
        val gy  = H * 0.55f

        // ① 下箱体（大弧，3/4 圆）
        val lbR = W * 0.195f
        val lbCy = gy + H * 0.04f
        drawArc(
            color         = tint,
            startAngle    = -230f,
            sweepAngle    = 280f,
            useCenter     = false,
            topLeft       = Offset(gx - lbR, lbCy - lbR),
            size          = Size(lbR * 2f, lbR * 2f),
            style         = stroke
        )

        // ② 上箱体（小弧）
        val ubR  = W * 0.145f
        val ubCy = gy - H * 0.25f
        drawArc(
            color      = tint,
            startAngle = -320f,
            sweepAngle = 280f,
            useCenter  = false,
            topLeft    = Offset(gx - ubR, ubCy - ubR),
            size       = Size(ubR * 2f, ubR * 2f),
            style      = stroke
        )

        // 上下箱体两侧腰部连线
        val waistTopL = Offset(gx - ubR * 0.80f, ubCy + ubR * 0.70f)
        val waistBotL = Offset(gx - lbR * 0.82f, lbCy - lbR * 0.72f)
        val waistTopR = Offset(gx + ubR * 0.80f, ubCy + ubR * 0.70f)
        val waistBotR = Offset(gx + lbR * 0.82f, lbCy - lbR * 0.72f)
        drawLine(tint, waistTopL, waistBotL, sw * 0.9f, StrokeCap.Round)
        drawLine(tint, waistTopR, waistBotR, sw * 0.9f, StrokeCap.Round)

        // ③ 琴颈
        val neckW = W * 0.065f
        val neckTop = H * 0.06f
        val neckBot = ubCy - ubR + sw / 2f
        drawLine(tint, Offset(gx - neckW, neckTop), Offset(gx - neckW, neckBot), sw * 0.7f, StrokeCap.Round)
        drawLine(tint, Offset(gx + neckW, neckTop), Offset(gx + neckW, neckBot), sw * 0.7f, StrokeCap.Round)
        drawLine(tint, Offset(gx - neckW * 1.4f, neckTop), Offset(gx + neckW * 1.4f, neckTop), sw * 0.9f, StrokeCap.Round)

        // ④ 音孔
        drawCircle(color = tint, radius = lbR * 0.30f,
            center = Offset(gx, lbCy - lbR * 0.05f), style = stroke)

        // ── 回形针（右侧约 40%）──
        val cpx = W * 0.73f
        val cpy = H * 0.52f
        val cpr = H * 0.24f   // 外圈半径

        // ⑤ 外圈大弧（约 300°，开口朝左下）
        drawArc(
            color      = tint,
            startAngle = -40f,
            sweepAngle = -300f,
            useCenter  = false,
            topLeft    = Offset(cpx - cpr, cpy - cpr),
            size       = Size(cpr * 2f, cpr * 2f),
            style      = stroke
        )

        // ⑥ 内圈小弧（约 250°，开口朝右上）
        val ir = cpr * 0.50f
        val iy = cpy + cpr * 0.12f
        drawArc(
            color      = tint,
            startAngle = 215f,
            sweepAngle = 260f,
            useCenter  = false,
            topLeft    = Offset(cpx - ir, iy - ir),
            size       = Size(ir * 2f, ir * 2f),
            style      = stroke
        )

        // ⑦ 回形针尖端（外圈大弧在 -40° 处的端点，仅此端点施加 clipTipDx 偏移）
        val tipAngleRad = (-40f * PI / 180f).toFloat()
        val tipBaseX = cpx + cpr * cos(tipAngleRad)
        val tipBaseY = cpy + cpr * sin(tipAngleRad)
        // 以单独小圆点 + 短横线画出弯折尖端，尖端施加动效偏移
        val tipX = tipBaseX + clipTipDx.value * px
        val tipY = tipBaseY
        drawLine(
            color       = tint,
            start       = Offset(tipBaseX - W * 0.04f, tipBaseY + H * 0.02f),
            end         = Offset(tipX, tipY),
            strokeWidth = sw * 0.85f,
            cap         = StrokeCap.Round
        )
        drawCircle(color = tint, radius = sw * 0.55f, center = Offset(tipX, tipY))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 耳机系列图标：HeadphonesHeartCanvas / HeadphonesVinylCanvas / HeadphonesArtistCanvas
// 均与漫游图标（ic_roam_dice）共享完全一致的耳机轮廓与动态设计语言。
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 收藏歌曲图标：耳机轮廓 + 内嵌爱心（与漫游图标完全同宗同源）
 */
@Composable
fun HeadphonesHeartCanvas(
    modifier: Modifier = Modifier,
    tint: Color,
    isSelected: Boolean = false
) {
    val heartScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "heart_scale"
    )

    Icon(
        painter = painterResource(R.drawable.ic_headset_fav_song),
        contentDescription = "收藏歌曲",
        tint = tint,
        modifier = modifier.graphicsLayer {
            scaleX = heartScale
            scaleY = heartScale
        }
    )
}

/**
 * 收藏专辑图标：耳机轮廓 + 内嵌黑胶唱片（与漫游图标完全同宗同源）
 */
@Composable
fun HeadphonesVinylCanvas(
    modifier: Modifier = Modifier,
    tint: Color,
    isSelected: Boolean = false
) {
    val vinylRotation = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            vinylRotation.snapTo(0f)
            vinylRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        } else {
            vinylRotation.snapTo(0f)
        }
    }

    Icon(
        painter = painterResource(R.drawable.ic_headset_fav_album),
        contentDescription = "收藏专辑",
        tint = tint,
        modifier = modifier.graphicsLayer {
            rotationZ = vinylRotation.value
        }
    )
}

/**
 * 关注歌手图标：耳机轮廓 + 内嵌歌手人像（与漫游图标完全同宗同源）
 */
@Composable
fun HeadphonesArtistCanvas(
    modifier: Modifier = Modifier,
    tint: Color,
    isSelected: Boolean = false
) {
    val floatOffset by animateFloatAsState(
        targetValue = if (isSelected) -4f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "artist_float"
    )

    Icon(
        painter = painterResource(R.drawable.ic_headset_fav_artist),
        contentDescription = "关注歌手",
        tint = tint,
        modifier = modifier.graphicsLayer {
            translationY = floatOffset
        }
    )
}

