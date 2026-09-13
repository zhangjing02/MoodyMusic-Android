package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.ArtistAvatar
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 艺术家档案名录列表项组件 (ArtistDirectoryItem)
 * 采用智能专属圆形头像（优先真实图片，无图自动生成艺术首字徽标）、精致衬线名称与流派信息。
 */
@Composable
fun ArtistDirectoryItem(
    name: String,
    genre: String,
    albumCount: Int,
    avatarUrl: String?,
    artistId: String? = null,
    modifier: Modifier = Modifier,
    fallbackRes: Int = R.drawable.artist_abigail_chen,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 歌手专属头像（优先真实图片与本地 Assets 头像，无图/异常自动退化为艺术首字徽标）
        ArtistAvatar(
            name = name,
            avatarUrl = avatarUrl,
            artistId = artistId,
            size = 56.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 歌手名称与专辑流派信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "$albumCount 张专辑 • $genre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                letterSpacing = 0.3.sp
            )
        }

        // 向右导航指示箭头
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View Artist",
            tint = SongbookColors.Outline.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
