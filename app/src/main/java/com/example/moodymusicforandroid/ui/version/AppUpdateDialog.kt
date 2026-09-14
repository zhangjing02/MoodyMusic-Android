package com.example.moodymusicforandroid.ui.version

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moodymusicforandroid.data.model.AppVersionData
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 现代颂歌 (The Modern Songbook) 风格的紧凑型强制升级提醒卡片
 * 设计为精炼直接的小矩形，不遮挡大面积屏幕，专用于强制更新场景。
 */
@Composable
fun AppUpdateDialog(
    versionData: AppVersionData,
    onConfirmUpdate: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* 强制升级不可取消 */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLowest),
            border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "版本更新提示",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SongbookColors.SoftCharcoal,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "检测到新版本 v" + versionData.versionName + "，当前版本已停用，请更新后继续使用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.75f),
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onConfirmUpdate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                ) {
                    Text(
                        text = "立即升级",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
