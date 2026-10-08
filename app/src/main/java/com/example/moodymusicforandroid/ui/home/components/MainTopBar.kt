package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.moodymusicforandroid.R

/**
 * 首页顶部导航栏组件
 *
 * 负责展示应用的主标题、侧滑菜单入口以及用户头像（点击可进入认证页面）。
 * 采用带有半透明效果的 Material3 TopAppBar 样式以配合 EdgeToEdge 的沉浸式体验。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    /** 当前页面标题 */
    title: String,
    /** 点击汉堡菜单时的回调，通常用于打开侧边栏 */
    onMenuClick: () -> Unit,
    /** 点击用户头像时的回调，通常用于跳转到认证页面 */
    onAuthClick: () -> Unit
) {
    TopAppBar(
        title = { 
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu_strokes),
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            IconButton(onClick = onAuthClick) {
                Image(
                    painter = painterResource(R.drawable.ic_home), // Placeholder for avatar
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    )
}
