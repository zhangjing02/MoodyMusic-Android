package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 侧滑抽屉内容组件 (The Modern Songbook 现代颂歌风格)
 *
 * 重新规划布局：
 * 1. 顶部账户资料卡片与登录/退出快捷入口
 * 2. 手札互动（留言板、风格喜好 - 预留功能）
 * 3. 系统与设置（设置、版本展示与在线更新、关于音信）
 */
@Composable
fun AppDrawerContent(
    isLoggedIn: Boolean,
    userName: String,
    currentVersionName: String = "1.0",
    hasUpdate: Boolean = false,
    onCloseClick: () -> Unit,
    onAuthClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onMessageBoardClick: () -> Unit = {},
    onStylePreferenceClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onVersionClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SongbookColors.PaperBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── 1. 抽屉顶栏 (标题 + 关闭按钮) ──────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THE MODERN SONGBOOK",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.BurntOrange,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp
                )
                Text(
                    text = "音信 · TunePost",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = SongbookColors.SoftCharcoal
                )
            }
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭抽屉",
                    tint = SongbookColors.SoftCharcoal.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── 2. 账号区域 (放置在顶部，最醒目位置) ────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { if (!isLoggedIn) onAuthClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.GhostBorderActive),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoggedIn) {
                            SongbookImage(
                                model = "/storage/avatars/user_avatar_default.jpg",
                                contentDescription = "用户头像",
                                fallbackRes = R.drawable.user_avatar_default,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "访客",
                                tint = SongbookColors.BurntOrange,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoggedIn) userName else "访客未登录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLoggedIn) "原声手札已开启" else "登录同步收藏、关注与动态",
                            style = MaterialTheme.typography.bodySmall,
                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!isLoggedIn) {
                    Button(
                        onClick = onAuthClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SongbookColors.BurntOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "登录 / 注册",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onLogoutClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "退出登录",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // ── 3. 手札互动 Section ─────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 22.dp, top = 14.dp, bottom = 8.dp)
        ) {
            Text(
                text = "手札互动",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.BurntOrange,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "· INTERACTION",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.BurntOrange.copy(alpha = 0.45f),
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawerMenuItem(
                    title = "留言板",
                    badge = "待开放",
                    onClick = onMessageBoardClick
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = SongbookColors.GhostBorder.copy(alpha = 0.6f)
                )

                DrawerMenuItem(
                    title = "风格喜好",
                    badge = "待开放",
                    onClick = onStylePreferenceClick
                )
            }
        }

        // ── 4. 系统与设置 Section ───────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 8.dp)
        ) {
            Text(
                text = "系统与设置",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.BurntOrange,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 11.5.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "· PREFERENCES",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.BurntOrange.copy(alpha = 0.45f),
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                fontSize = 9.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
            border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawerMenuItem(
                    title = "设置",
                    onClick = onSettingsClick
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = SongbookColors.GhostBorder.copy(alpha = 0.6f)
                )

                DrawerMenuItem(
                    title = "版本更新",
                    trailingText = "v$currentVersionName",
                    badge = if (hasUpdate) "NEW" else null,
                    onClick = onVersionClick
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = SongbookColors.GhostBorder.copy(alpha = 0.6f)
                )

                DrawerMenuItem(
                    title = "关于音信",
                    onClick = onAboutClick
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(16.dp))

        // 底部微版权与理念
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "音信 · TunePost v$currentVersionName",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.4f),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "The Modern Songbook © 2026",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.3f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    title: String,
    badge: String? = null,
    trailingText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = SongbookColors.SoftCharcoal,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (badge == "NEW") SongbookColors.BurntOrange else SongbookColors.BurntOrange.copy(alpha = 0.08f)
            ) {
                Text(
                    text = badge,
                    color = if (badge == "NEW") Color.White else SongbookColors.BurntOrange,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = SongbookColors.SoftCharcoal.copy(alpha = 0.25f),
            modifier = Modifier.size(16.dp)
        )
    }
}

