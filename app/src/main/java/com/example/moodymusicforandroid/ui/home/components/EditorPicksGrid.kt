package com.example.moodymusicforandroid.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.ui.components.SongbookImage
import com.example.moodymusicforandroid.ui.theme.SongbookColors

/**
 * 唱片精选数据模型
 */
data class EditorPickItemData(
    val id: String,
    val title: String,
    val artist: String,
    val genre: String,
    val imageUrl: String,
    val fallbackRes: Int
)

/**
 * 首页 2×2 错落排版唱片精选网格 (Editor's Picks)
 * 采用微错位节奏（Rhythmic Staggered Layout），呈现高端生活方式杂志的拼贴美感。
 */
@Composable
fun EditorPicksGrid(
    modifier: Modifier = Modifier,
    onAlbumClick: (String, String) -> Unit = { _, _ -> },
    onBrowseAllClick: () -> Unit = {}
) {
    val items = listOf(
        EditorPickItemData(
            id = "101",
            title = "旷野之息",
            artist = "晨曦乐团",
            genre = "民谣复兴",
            imageUrl = "/storage/covers/albums/album_forest_track.jpg",
            fallbackRes = R.drawable.album_forest_track
        ),
        EditorPickItemData(
            id = "102",
            title = "午夜萨克斯",
            artist = "Blue Note Collective",
            genre = "暗色爵士",
            imageUrl = "/storage/covers/albums/album_modern_jazz.jpg",
            fallbackRes = R.drawable.album_modern_jazz
        ),
        EditorPickItemData(
            id = "103",
            title = "十二平均律",
            artist = "巴赫作品集",
            genre = "古典乐",
            imageUrl = "/storage/covers/albums/album_classical_piano.jpg",
            fallbackRes = R.drawable.album_classical_piano
        ),
        EditorPickItemData(
            id = "104",
            title = "流动的静谧",
            artist = "静水深流",
            genre = "环境音",
            imageUrl = "/storage/covers/albums/album_electronic_vibes.jpg",
            fallbackRes = R.drawable.album_electronic_vibes
        )
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // 栏目标题与“浏览全部”链接
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "编辑精选",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "浏览全部",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onBrowseAllClick() }
            )
        }

        // 双列错落网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左列
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                EditorPickCard(
                    item = items[0],
                    onClick = { onAlbumClick(items[0].id, items[0].title) }
                )
                EditorPickCard(
                    item = items[2],
                    modifier = Modifier.offset(y = (-12).dp),
                    onClick = { onAlbumClick(items[2].id, items[2].title) }
                )
            }

            // 右列 (整体带有向下的视觉错位)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .offset(y = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                EditorPickCard(
                    item = items[1],
                    onClick = { onAlbumClick(items[1].id, items[1].title) }
                )
                EditorPickCard(
                    item = items[3],
                    onClick = { onAlbumClick(items[3].id, items[3].title) }
                )
            }
        }
    }
}

@Composable
private fun EditorPickCard(
    item: EditorPickItemData,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        // 唱片正方形封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            SongbookImage(
                model = item.imageUrl,
                contentDescription = item.title,
                fallbackRes = item.fallbackRes,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 流派标签
        Text(
            text = item.genre,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 唱片名 (Serif)
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 艺术家名称
        Text(
            text = item.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
