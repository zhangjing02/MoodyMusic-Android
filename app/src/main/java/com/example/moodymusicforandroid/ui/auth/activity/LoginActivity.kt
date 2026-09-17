package com.example.moodymusicforandroid.ui.auth.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.moodymusicforandroid.R
import com.example.moodymusicforandroid.MoodyMusicApplication
import com.example.moodymusicforandroid.base.LoadingState
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.utils.ActivityTransitionUtils
import com.example.moodymusicforandroid.common.utils.ThemeManager
import com.example.moodymusicforandroid.ui.auth.viewmodel.AuthViewModel
import com.example.moodymusicforandroid.ui.home.activity.MainActivity
import com.example.moodymusicforandroid.ui.theme.SongbookColors
import com.example.moodymusicforandroid.ui.theme.SongbookTheme
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * 音信 (TunePost) — 现代颂歌 (The Modern Songbook) 风格认证界面
 * 支持邮箱验证码极速登录/免密注册、账号密码登录以及重置密码
 */
class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(MoodyMusicApplication.currentThemeResId)
        super.onCreate(savedInstanceState)

        EventBusManager.register(this)
        ThemeManager.initTheme(this)

        if (intent.getBooleanExtra("KICKED_OUT", false)) {
            Toast.makeText(this, "您的账号已在其他设备登录，请重新登录", Toast.LENGTH_SHORT).show()
        }

        // 绑定 Toast 提示
        viewModel.toastMessage.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // 登录成功跳转
        viewModel.loginUser.observe(this) { user ->
            if (user != null) navigateToMain()
        }
        viewModel.registerUser.observe(this) { user ->
            if (user != null) navigateToMain()
        }

        setContent {
            SongbookTheme {
                LoginScreenContent(
                    viewModel = viewModel,
                    onDismiss = { finish() }
                )
            }
        }
    }

    private var isNavigating = false

    private fun navigateToMain() {
        if (isNavigating || isFinishing || isDestroyed) return
        isNavigating = true

        if (!isTaskRoot) {
            // MainActivity 已经在下层，直接平滑退出当前登录页，即时呈现主页（0毫秒白屏）
            finish()
        } else {
            // 独立启动场景兜底：以单例模式拉起 MainActivity，平滑过渡
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    override fun finish() {
        super.finish()
        ActivityTransitionUtils.overrideCloseTransition(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventReceived(event: BaseEvent) {
        when (event.eventType) {
            EventType.AUTH_TOKEN_EXPIRED -> {
                if (event.eventData == "KICKED_OUT") {
                    Toast.makeText(this, "您的账号已在其他设备登录，请重新登录", Toast.LENGTH_SHORT).show()
                }
            }
            EventType.USER_LOGIN -> navigateToMain()
            else -> Unit
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBusManager.unregister(this)
    }
}

/**
 * 登录界面主体
 */
@Composable
private fun LoginScreenContent(
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: 免密登录, 1: 密码登录, 2: 注册账号
    var email by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // 注册模式专用状态
    var registerUsername by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var registerConfirmPassword by remember { mutableStateOf("") }
    var registerPasswordVisible by remember { mutableStateOf(false) }
    var registerConfirmPasswordVisible by remember { mutableStateOf(false) }

    var showResetPasswordDialog by remember { mutableStateOf(false) }

    val countdown by viewModel.countdown.observeAsState(0)
    val isSendingCode by viewModel.isSendingCode.observeAsState(false)
    val loadingState by viewModel.loadingState.observeAsState()
    val isLoading = loadingState is LoadingState.ShowLoading
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 背景微弱光影与留声机同心圆纹理
            LoginBackgroundDecorative()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部关闭/退出按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SongbookColors.SurfaceLow)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SongbookColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 品牌艺术黑胶徽章
                VinylBrandBadge()

                Spacer(modifier = Modifier.height(14.dp))

                // 极简 Slogan
                Text(
                    text = "音信 · TunePost",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 模式切换分段控件 (免密登录 / 密码登录 / 注册账号)
                TabSegmentedControl(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (selectedTab) {
                    0 -> {
                        // ================= 模式 0: 邮箱免密极速登录 =================
                        CustomInputField(
                            value = email,
                            onValueChange = { email = it },
                            label = "电子邮箱",
                            placeholder = "name@example.com",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomInputField(
                                value = code,
                                onValueChange = { if (it.length <= 6) code = it },
                                label = "验证码",
                                placeholder = "6 位数字",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )

                            // 验证码获取按钮
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.sendVerificationCode(email, "login")
                                },
                                enabled = countdown == 0 && !isSendingCode && viewModel.isEmailValid(email),
                                modifier = Modifier
                                    .height(56.dp)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (countdown == 0 && viewModel.isEmailValid(email)) SongbookColors.BurntOrange else SongbookColors.OutlineVariant
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SongbookColors.BurntOrange,
                                    disabledContentColor = SongbookColors.Outline
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                if (isSendingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = SongbookColors.BurntOrange,
                                        strokeWidth = 2.dp
                                    )
                                } else if (countdown > 0) {
                                    Text(
                                        text = "${countdown}s",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "获取验证码",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // ================= 模式 1: 密码登录 (用户名 / 邮箱) =================
                        val displayAccount = if (account.isNotBlank()) account else email
                        CustomInputField(
                            value = displayAccount,
                            onValueChange = {
                                account = it
                                if (email.isBlank() && it.contains("@")) email = it
                            },
                            label = "用户名 / 邮箱",
                            placeholder = "输入用户名或邮箱",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Account",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CustomInputField(
                            value = password,
                            onValueChange = { password = it },
                            label = "登录密码",
                            placeholder = "输入账户密码",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (passwordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible
                                        ),
                                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                                        tint = if (passwordVisible) SongbookColors.BurntOrange else SongbookColors.Outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // 忘记密码
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "忘记密码？",
                                style = MaterialTheme.typography.labelMedium,
                                color = SongbookColors.BurntOrange,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { showResetPasswordDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }
                    }

                    2 -> {
                        // ================= 模式 2: 注册账号 (用户名选填 + 邮箱验证码 + 设置密码) =================
                        CustomInputField(
                            value = registerUsername,
                            onValueChange = { registerUsername = it },
                            label = "用户名",
                            placeholder = "选填，默认使用邮箱前缀",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Username",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        CustomInputField(
                            value = email,
                            onValueChange = { email = it },
                            label = "电子邮箱",
                            placeholder = "name@example.com",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomInputField(
                                value = code,
                                onValueChange = { if (it.length <= 6) code = it },
                                label = "验证码",
                                placeholder = "6 位数字",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                )
                            )

                            // 验证码获取按钮
                            OutlinedButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.sendVerificationCode(email, "login")
                                },
                                enabled = countdown == 0 && !isSendingCode && viewModel.isEmailValid(email),
                                modifier = Modifier
                                    .height(56.dp)
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (countdown == 0 && viewModel.isEmailValid(email)) SongbookColors.BurntOrange else SongbookColors.OutlineVariant
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SongbookColors.BurntOrange,
                                    disabledContentColor = SongbookColors.Outline
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                if (isSendingCode) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = SongbookColors.BurntOrange,
                                        strokeWidth = 2.dp
                                    )
                                } else if (countdown > 0) {
                                    Text(
                                        text = "${countdown}s",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "获取验证码",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 设置初始密码
                        CustomInputField(
                            value = registerPassword,
                            onValueChange = { registerPassword = it },
                            label = "设置登录密码",
                            placeholder = "不少于 6 位字符",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Password",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { registerPasswordVisible = !registerPasswordVisible },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (registerPasswordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible
                                        ),
                                        contentDescription = if (registerPasswordVisible) "隐藏密码" else "显示密码",
                                        tint = if (registerPasswordVisible) SongbookColors.BurntOrange else SongbookColors.Outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (registerPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 确认密码
                        CustomInputField(
                            value = registerConfirmPassword,
                            onValueChange = { registerConfirmPassword = it },
                            label = "确认登录密码",
                            placeholder = "再次输入相同密码",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Confirm Password",
                                    tint = SongbookColors.Outline,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { registerConfirmPasswordVisible = !registerConfirmPasswordVisible },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (registerConfirmPasswordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible
                                        ),
                                        contentDescription = if (registerConfirmPasswordVisible) "隐藏密码" else "显示密码",
                                        tint = if (registerConfirmPasswordVisible) SongbookColors.BurntOrange else SongbookColors.Outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            visualTransformation = if (registerConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                val activeLoginAccount = if (account.isNotBlank()) account.trim() else email.trim()
                val isActionEnabled = when (selectedTab) {
                    0 -> !isLoading && email.isNotBlank() && code.isNotBlank()
                    1 -> !isLoading && activeLoginAccount.isNotBlank() && password.isNotBlank()
                    2 -> !isLoading && email.isNotBlank() && code.isNotBlank() && registerPassword.length >= 6 && registerConfirmPassword.length >= 6
                    else -> false
                }

                // 核心主按钮
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        when (selectedTab) {
                            0 -> viewModel.loginWithCode(email, code)
                            1 -> viewModel.loginWithPassword(activeLoginAccount, password)
                            2 -> viewModel.registerWithCode(email, code, registerPassword, registerConfirmPassword, registerUsername)
                        }
                    },
                    enabled = isActionEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SongbookColors.BurntOrange,
                        disabledContainerColor = SongbookColors.BurntOrange.copy(alpha = 0.4f),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when (selectedTab) {
                                0 -> "验证并进入音信"
                                1 -> "立即登录"
                                2 -> "完成注册并进入音信"
                                else -> "确认"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))
                Spacer(modifier = Modifier.height(28.dp))

                // 底部极简小标
                Text(
                    text = "音信 · 原声手札与情绪共鸣",
                    style = MaterialTheme.typography.labelSmall,
                    color = SongbookColors.Outline.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 忘记密码重置弹窗
            if (showResetPasswordDialog) {
                ResetPasswordDialog(
                    initialEmail = email,
                    viewModel = viewModel,
                    onDismiss = { showResetPasswordDialog = false },
                    onSuccess = {
                        showResetPasswordDialog = false
                        selectedTab = 1 // 切换至密码模式
                    }
                )
            }
        }
    }
}

/**
 * 留声机同心圆徽章
 */
@Composable
private fun VinylBrandBadge() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(SongbookColors.SurfaceLow),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(62.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val strokeColor = SongbookColors.OutlineVariant.copy(alpha = 0.4f)
            val accentColor = SongbookColors.BurntOrange

            // 黑胶同心环
            drawCircle(color = strokeColor, radius = size.width * 0.45f, center = center, style = Stroke(width = 1f))
            drawCircle(color = strokeColor, radius = size.width * 0.35f, center = center, style = Stroke(width = 1f))
            drawCircle(color = strokeColor, radius = size.width * 0.25f, center = center, style = Stroke(width = 1f))

            // 中心唱片标芯
            drawCircle(color = accentColor, radius = size.width * 0.16f, center = center)
            drawCircle(color = Color.White, radius = size.width * 0.05f, center = center)
        }
    }
}

/**
 * 分段切换组件 (TabSegmentedControl)
 */
@Composable
private fun TabSegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(SongbookColors.SurfaceLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabTitles = listOf("免密登录", "密码登录", "注册账号")

        tabTitles.forEachIndexed { index, title ->
            val isSelected = selectedTab == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onTabSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) SongbookColors.BurntOrange else SongbookColors.Outline
                )
            }
        }
    }
}

/**
 * 自定义轻量杂志风输入框
 */
@Composable
private fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, fontSize = 12.sp) },
        placeholder = { Text(text = placeholder, fontSize = 13.sp, color = SongbookColors.OutlineVariant) },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SongbookColors.SurfaceLowest,
            unfocusedContainerColor = SongbookColors.SurfaceLow,
            focusedBorderColor = SongbookColors.BurntOrange,
            unfocusedBorderColor = SongbookColors.GhostBorder,
            focusedLabelColor = SongbookColors.BurntOrange,
            unfocusedLabelColor = SongbookColors.Outline
        )
    )
}

/**
 * 忘记密码重置弹窗
 */
@Composable
private fun ResetPasswordDialog(
    initialEmail: String,
    viewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val countdown by viewModel.countdown.observeAsState(0)
    val isSendingCode by viewModel.isSendingCode.observeAsState(false)
    val resetSuccess by viewModel.resetPasswordSuccess.observeAsState(false)

    LaunchedEffect(resetSuccess) {
        if (resetSuccess) {
            viewModel.resetPasswordSuccess.value = false
            onSuccess()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, SongbookColors.GhostBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "重置密码",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SongbookColors.Outline)
                    }
                }

                Text(
                    text = "输入注册邮箱接收验证码，以重新设置您的安全密码",
                    style = MaterialTheme.typography.bodySmall,
                    color = SongbookColors.Outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 20.dp)
                )

                CustomInputField(
                    value = email,
                    onValueChange = { email = it },
                    label = "注册邮箱",
                    placeholder = "name@example.com",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = "Email",
                            tint = SongbookColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomInputField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = "验证码",
                        placeholder = "6位数字",
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                    )

                    OutlinedButton(
                        onClick = { viewModel.sendVerificationCode(email, "reset_password") },
                        enabled = countdown == 0 && !isSendingCode && viewModel.isEmailValid(email),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(54.dp).padding(top = 4.dp),
                        border = BorderStroke(1.dp, if (countdown == 0 && viewModel.isEmailValid(email)) SongbookColors.BurntOrange else SongbookColors.OutlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SongbookColors.BurntOrange)
                    ) {
                        if (isSendingCode) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SongbookColors.BurntOrange, strokeWidth = 2.dp)
                        } else if (countdown > 0) {
                            Text("${countdown}s", fontSize = 12.sp)
                        } else {
                            Text("获取验证码", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                CustomInputField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = "新密码",
                    placeholder = "不少于6位字符",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Password",
                            tint = SongbookColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { newPasswordVisible = !newPasswordVisible },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (newPasswordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible
                                ),
                                contentDescription = if (newPasswordVisible) "隐藏密码" else "显示密码",
                                tint = if (newPasswordVisible) SongbookColors.BurntOrange else SongbookColors.Outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(12.dp))

                CustomInputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "确认新密码",
                    placeholder = "再次输入新密码",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Confirm Password",
                            tint = SongbookColors.Outline,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (confirmPasswordVisible) R.drawable.ic_eye_visible else R.drawable.ic_eye_invisible
                                ),
                                contentDescription = if (confirmPasswordVisible) "隐藏密码" else "显示密码",
                                tint = if (confirmPasswordVisible) SongbookColors.BurntOrange else SongbookColors.Outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.resetPassword(email, code, newPassword, confirmPassword) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SongbookColors.BurntOrange, contentColor = Color.White)
                ) {
                    Text("确认重置密码", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/**
 * 极简留声机微光影背景修饰
 */
@Composable
private fun LoginBackgroundDecorative() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val topRadius = size.width * 0.7f
        drawCircle(
            color = SongbookColors.BurntOrange.copy(alpha = 0.03f),
            radius = topRadius,
            center = Offset(size.width * 0.85f, -size.width * 0.1f)
        )
    }
}
