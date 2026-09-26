package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage

private val THEME_BANNER_URL =
    com.example.moodymusicforandroid.common.config.AppConfig.resolveStorageUrl("covers/home/snow_cafe_static.jpg")

/**
 * 首页主题音乐专栏卡片 (ThemeMusicBanner)
 *
 * 纯粹视觉卡片：以高质量动态窗外雪景与热咖啡图像本身呈现，去除多余冗杂文字与装饰，
 * 图片即代表含义；轻触即可进入深度图文详情页。
 */
@Composable
fun ThemeMusicBanner(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        // 大图背景（飘雪咖啡馆动图，带咖啡蒸气与窗外白雪，画面即代表全部含义）
        SongbookImage(
            model = THEME_BANNER_URL,
            contentDescription = "雪天咖啡馆阅读钢琴",
            fallbackRes = R.drawable.home_vinyl_banner,
            modifier = Modifier.fillMaxSize()
        )
    }
}
