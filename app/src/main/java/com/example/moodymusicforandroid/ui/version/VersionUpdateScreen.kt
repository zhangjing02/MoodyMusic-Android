package com.example.moodymusicforandroid.ui.version

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.moodymusicforandroid.common.update.PgyerUpdateManager
import com.example.moodymusicforandroid.common.utils.DeviceInfoUtils
import com.example.moodymusicforandroid.data.model.AppVersionData
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 简约版 · 版本与在线更新页面 (The Modern Songbook 现代颂歌风格)
 * 剔除冗余硬件/开发指纹，呈现极简、纯粹、优雅的版本状态与更新交互。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionUpdateScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentVersionName = remember { DeviceInfoUtils.getVersionName(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var versionData by remember { mutableStateOf<AppVersionData?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMbStr by remember { mutableStateOf("0.0 MB") }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    fun checkVersion() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val data = withContext(Dispatchers.IO) {
                    PgyerUpdateManager.checkUpdate(context)
                }
                versionData = data
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "检查更新失败，请稍后重试"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkVersion()
    }

    fun startDownloadAndInstall(targetUrl: String, apkFileName: String) {
        if (isDownloading) return
        isDownloading = true
        downloadProgress = 0f
        downloadedMbStr = "0.0 MB"

        coroutineScope.launch {
            try {
                val destFile = withContext(Dispatchers.IO) {
                    val downloadDir = context.getExternalFilesDir("releases") ?: context.cacheDir
                    val file = File(downloadDir, apkFileName)
                    if (file.exists()) file.delete()

                    val client = OkHttpClient.Builder()
                        .followRedirects(true)
                        .followSslRedirects(true)
                        .build()
                    val request = Request.Builder().url(targetUrl).build()
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        throw IllegalStateException("下载失败 HTTP " + response.code)
                    }

                    val body = response.body ?: throw IllegalStateException("空响应体")
                    val contentLength = body.contentLength()
                    var bytesReadTotal = 0L

                    body.byteStream().use { input: InputStream ->
                        FileOutputStream(file).use { output: FileOutputStream ->
                            val buffer = ByteArray(8 * 1024)
                            var bytes: Int
                            while (input.read(buffer).also { bytes = it } != -1) {
                                output.write(buffer, 0, bytes)
                                bytesReadTotal += bytes
                                if (contentLength > 0) {
                                    val progress = bytesReadTotal.toFloat() / contentLength.toFloat()
                                    val currentMb = String.format("%.1f MB", bytesReadTotal / (1024f * 1024f))
                                    withContext(Dispatchers.Main) {
                                        downloadProgress = progress
                                        downloadedMbStr = currentMb
                                    }
                                }
                            }
                            output.flush()
                        }
                    }
                    file
                }

                downloadedApkFile = destFile
                isDownloading = false
                Toast.makeText(context, "下载完成，正在调起安装器...", Toast.LENGTH_SHORT).show()
                installApk(context, destFile)
            } catch (e: Exception) {
                isDownloading = false
                Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "版本与更新",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.SoftCharcoal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = SongbookColors.SoftCharcoal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SongbookColors.PaperBackground
                )
            )
        },
        containerColor = SongbookColors.PaperBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. App 极简品牌标识
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SongbookColors.BurntOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "♪",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = SongbookColors.BurntOrange,
                    fontFamily = FontFamily.Serif
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "音信 · TunePost",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SongbookColors.SoftCharcoal,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Version $currentVersionName",
                style = MaterialTheme.typography.bodySmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 2. 状态与更新卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        isLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = SongbookColors.BurntOrange
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "正在检查更新...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.7f)
                                )
                            }
                        }
                        errorMessage != null -> {
                            Text(
                                text = errorMessage ?: "网络连接异常",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { checkVersion() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("重新检查", fontSize = 13.sp)
                            }
                        }
                        versionData?.hasUpdate == true -> {
                            val data = versionData!!
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "发现新版本 v${data.versionName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SongbookColors.SoftCharcoal
                                    )
                                    if (data.packageSizeStr.isNotBlank()) {
                                        Text(
                                            text = data.packageSizeStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                                        )
                                    }
                                }

                                if (data.releaseNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = data.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.75f),
                                        lineHeight = 19.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // 下载进度
                                if (isDownloading) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = SongbookColors.BurntOrange,
                                        trackColor = SongbookColors.GhostBorder
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "下载中 ${(downloadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = downloadedMbStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                                    Button(
                                        onClick = { installApk(context, downloadedApkFile!!) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Text("下载完成，立即安装", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val url = data.downloadUrl.ifBlank { data.downloadUrlMirror ?: "" }
                                            if (url.isNotBlank()) {
                                                startDownloadAndInstall(url, "MoodyMusic-v${data.versionName}.apk")
                                            } else {
                                                Toast.makeText(context, "下载地址不可用", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isDownloading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                                    ) {
                                        Text(
                                            text = if (isDownloading) "正在下载更新包..." else "立即更新",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            // 已是最新版本
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "当前已是最新版本",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = SongbookColors.SoftCharcoal
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { checkVersion() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = SongbookColors.SoftCharcoal.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "检查新版本",
                                    fontSize = 12.5.sp,
                                    color = SongbookColors.SoftCharcoal.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "The Modern Songbook © 2026",
                style = MaterialTheme.typography.labelSmall,
                color = SongbookColors.SoftCharcoal.copy(alpha = 0.35f),
                fontSize = 11.sp
            )
        }
    }
}

private fun installApk(context: Context, apkFile: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(context, authority, apkFile)
        } else {
            Uri.fromFile(apkFile)
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "调起安装程序失败：${e.message}", Toast.LENGTH_LONG).show()
    }
}
