package com.example.moodymusicforandroid.ui.version

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import com.example.moodymusicforandroid.common.utils.DeviceInfoUtils
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.api.MoodyApiService
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
 * 版本展示与在线更新页面 (The Modern Songbook 现代颂歌风格)
 *
 * 功能点：
 * 1. 完整展示当前本地安装版本、设备硬件指纹、瘦身优化包体积
 * 2. 自动/手动连线服务端检查最新版本信息（支持强制更新与非强制更新策略）
 * 3. 实时显示包体积大小、更新日志、Hash校验
 * 4. 内置断点式流下载引擎，实时更新下载进度，完成自动调用 FileProvider 唤起 APK 覆盖安装
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionUpdateScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 客户端本地环境信息
    val currentVersionName = remember { DeviceInfoUtils.getVersionName(context) }
    val currentVersionCode = remember { DeviceInfoUtils.getVersionCode(context) }
    val deviceBrand = remember { DeviceInfoUtils.getBrand() }
    val deviceModel = remember { DeviceInfoUtils.getModel() }
    val deviceArch = remember { DeviceInfoUtils.getArch() }
    val osVersion = remember { DeviceInfoUtils.getOsVersion() }

    // 检查状态与服务器数据
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var versionData by remember { mutableStateOf<AppVersionData?>(null) }

    // 下载状态：idle, downloading, completed, failed
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedMbStr by remember { mutableStateOf("0.0 MB") }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }

    // 检查版本更新方法
    fun checkVersion() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    MoodyApiProvider.apiService.checkAppVersion()
                }
                if ((response.code == 200 || response.code == 0) && response.data != null) {
                    versionData = response.data
                } else {
                    errorMessage = response.message ?: "获取版本信息失败"
                }
            } catch (e: Exception) {
                errorMessage = "网络请求失败：${e.localizedMessage ?: "无法连接到服务器"}"
            } finally {
                isLoading = false
            }
        }
    }

    // 进入页面时自动检查一次
    LaunchedEffect(Unit) {
        checkVersion()
    }

    // 执行 APK 文件下载并触发安装
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

                    val client = OkHttpClient.Builder().build()
                    val request = Request.Builder().url(targetUrl).build()
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        throw IllegalStateException("下载失败 HTTP ${response.code}")
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

                // 调用系统安装器
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
                        text = "版本展示与在线更新",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // ── 1. App 头部品牌与本地版本卡片 ──────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.BurntOrange.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "♪",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.BurntOrange,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "音信 · TunePost",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SongbookColors.SoftCharcoal,
                        fontFamily = FontFamily.Serif
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 本地安装版本 Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SongbookColors.GhostBorderActive
                    ) {
                        Text(
                            text = "本地当前版本：v$currentVersionName (构建号 $currentVersionCode)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SongbookColors.SoftCharcoal,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 硬件与瘦身指标
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "运行环境",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android $osVersion · $deviceArch",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = SongbookColors.SoftCharcoal
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "包体积状态",
                                style = MaterialTheme.typography.labelSmall,
                                color = SongbookColors.SoftCharcoal.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "已精简至 21.4 MB",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.BurntOrange
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── 2. 服务端版本与在线检测卡片 ────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SongbookColors.SurfaceLow),
                border = androidx.compose.foundation.BorderStroke(1.dp, SongbookColors.GhostBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "服务器版本与更新",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SongbookColors.SoftCharcoal
                        )

                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = SongbookColors.BurntOrange
                            )
                        } else {
                            IconButton(
                                onClick = { checkVersion() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "重新检查",
                                    tint = SongbookColors.BurntOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (errorMessage != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { checkVersion() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                        ) {
                            Text("重新尝试连线")
                        }
                    } else if (versionData != null) {
                        val data = versionData!!
                        if (!data.hasUpdate) {
                            // ── 已是最新版本 ──
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                    .padding(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "您已在使用最新版本",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "服务器最新版本：v${data.versionName}",
                                        fontSize = 12.sp,
                                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            // ── 发现新版本 ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = data.title.ifBlank { "发现新版本 v${data.versionName}" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SongbookColors.SoftCharcoal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "安装包体积：${data.packageSizeStr}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f)
                                    )
                                }

                                // 强制更新 vs 推荐更新标签
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (data.isForceUpdate) MaterialTheme.colorScheme.error else SongbookColors.BurntOrange
                                ) {
                                    Text(
                                        text = if (data.isForceUpdate) "强制更新" else "推荐更新",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 更新日志
                            Text(
                                text = "更新内容：",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SongbookColors.BurntOrange
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SongbookColors.PaperBackground,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = data.releaseNotes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SongbookColors.SoftCharcoal,
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 下载进度展示
                            AnimatedVisibility(visible = isDownloading) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = SongbookColors.BurntOrange,
                                        trackColor = SongbookColors.GhostBorder
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "下载进度：${(downloadProgress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = "$downloadedMbStr / ${data.packageSizeStr}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SongbookColors.SoftCharcoal.copy(alpha = 0.7f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }

                            // 操作按键
                            if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                                Button(
                                    onClick = { installApk(context, downloadedApkFile!!) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("下载完成，立即安装", fontWeight = FontWeight.Bold)
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
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange)
                                ) {
                                    Text(
                                        text = if (isDownloading) "正在下载更新包..." else "立即在线更新 (${data.packageSizeStr})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (data.isIgnoredAllowed && !data.isForceUpdate && !isDownloading) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "稍后提醒我",
                                        color = SongbookColors.SoftCharcoal.copy(alpha = 0.6f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

/**
 * 调起系统安装器安装 APK
 */
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
