package com.example.moodymusicforandroid.ui.auth.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.base.BaseViewModel
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import com.example.moodymusicforandroid.common.eventbus.EventType
import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * 认证 ViewModel
 * 支持邮箱验证码免密登录/注册、密码登录、重置密码与验证码倒计时
 */
class AuthViewModel : BaseViewModel() {

    val loginUser = MutableLiveData<User?>()
    val registerUser = MutableLiveData<User?>()
    val logoutSuccess = MutableLiveData<Boolean>()
    val resetPasswordSuccess = MutableLiveData<Boolean>()

    // 验证码倒计时 (秒)
    private val _countdown = MutableLiveData(0)
    val countdown: LiveData<Int> = _countdown

    // 正在发送验证码
    private val _isSendingCode = MutableLiveData(false)
    val isSendingCode: LiveData<Boolean> = _isSendingCode

    private var countdownJob: Job? = null

    /**
     * 校验邮箱格式
     */
    fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /**
     * 发送邮箱验证码
     * @param email 接收邮箱
     * @param type "login" 或 "reset_password"
     */
    fun sendVerificationCode(email: String, type: String = "login") {
        if (!isEmailValid(email)) {
            showToast("请输入正确的邮箱格式")
            return
        }

        if ((_countdown.value ?: 0) > 0) {
            showToast("请等待倒计时结束后再重新获取")
            return
        }

        _isSendingCode.value = true
        request(isShowLoading = false) {
            val response = MoodyApiProvider.apiService.sendVerificationCode(
                SendCodeRequest(email = email.trim(), type = type)
            )
            _isSendingCode.postValue(false)
            if (response.isSuccess()) {
                showToast("验证码已发送，请检查收件箱")
                startCountdown(60)
            }
            response
        }
    }

    /**
     * 启动倒计时
     */
    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        _countdown.value = seconds
        countdownJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                delay(1000)
                current--
                _countdown.value = current
            }
        }
    }

    /**
     * 邮箱验证码登录 / 极速注册 (主推方式)
     */
    fun loginWithCode(email: String, code: String) {
        if (!isEmailValid(email)) {
            showToast("请输入正确的邮箱")
            return
        }
        if (code.isBlank() || code.trim().length < 4) {
            showToast("请输入有效的验证码")
            return
        }

        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.loginWithCode(
                VerifyCodeRequest(email.trim(), code.trim())
            )
            handleLoginSuccess(response.data)
            response
        }
    }

    /**
     * 邮箱验证码注册账号并设置初始密码
     */
    fun registerWithCode(
        email: String,
        code: String,
        password: String,
        confirmPassword: String,
        username: String? = null,
        nickname: String? = null
    ) {
        val trimmedEmail = email.trim()
        val trimmedCode = code.trim()
        val trimmedUsername = username?.trim()?.ifBlank { null }

        if (trimmedUsername != null) {
            if (trimmedUsername.length < 2 || trimmedUsername.length > 20) {
                showToast("用户名长度需在 2-20 位字符之间")
                return
            }
        }

        if (!isEmailValid(trimmedEmail)) {
            showToast("请输入正确的邮箱格式")
            return
        }
        if (trimmedCode.length < 4) {
            showToast("请输入有效的验证码")
            return
        }
        if (password.length < 6) {
            showToast("密码长度不能少于6位")
            return
        }
        if (password != confirmPassword) {
            showToast("两次输入的密码不一致")
            return
        }

        val passwordHash = hashPassword(password)
        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.registerWithCode(
                RegisterWithCodeRequest(
                    email = trimmedEmail,
                    code = trimmedCode,
                    passwordHash = passwordHash,
                    username = trimmedUsername,
                    nickname = nickname?.trim()?.ifBlank { null }
                )
            )
            handleLoginSuccess(response.data)
            response
        }
    }

    /**
     * 用户名/邮箱 + 密码登录
     */
    fun loginWithPassword(account: String, password: String) {
        val trimmedAccount = account.trim()
        if (trimmedAccount.isBlank()) {
            showToast("请输入用户名或邮箱")
            return
        }
        if (password.isBlank()) {
            showToast("请输入密码")
            return
        }

        val passwordHash = hashPassword(password)
        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.loginWithPassword(
                PasswordLoginRequest(
                    account = trimmedAccount,
                    passwordHash = passwordHash
                )
            )
            handleLoginSuccess(response.data)
            response
        }
    }

    /**
     * 邮箱验证码重置密码
     */
    fun resetPassword(email: String, code: String, newPassword: String, confirmPassword: String) {
        if (!isEmailValid(email)) {
            showToast("请输入正确的邮箱")
            return
        }
        if (code.isBlank()) {
            showToast("请输入验证码")
            return
        }
        if (newPassword.length < 6) {
            showToast("新密码长度不能少于6位")
            return
        }
        if (newPassword != confirmPassword) {
            showToast("两次输入的密码不一致")
            return
        }

        val newPasswordHash = hashPassword(newPassword)
        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.resetPassword(
                ResetPasswordRequest(email.trim(), code.trim(), newPasswordHash)
            )
            if (response.isSuccess()) {
                showToast("密码重置成功，请使用新密码登录")
                resetPasswordSuccess.postValue(true)
            }
            response
        }
    }

    /**
     * 统一处理登录成功数据
     */
    private fun handleLoginSuccess(loginData: LoginData?) {
        val backendUser = loginData?.user
        if (backendUser != null) {
            val token = loginData.token ?: backendUser.token ?: ""
            val refreshToken = loginData.refreshToken ?: backendUser.refreshToken ?: ""
            UserManager.onLoginSuccess(backendUser, token, refreshToken)
            loginUser.postValue(UserManager.userProfile.value ?: backendUser)
        }
    }

    /**
     * 兼容旧用户名登录
     */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast("用户名和密码不能为空")
            return
        }

        val passwordHash = hashPassword(password)
        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.login(LoginRequest(username, passwordHash))
            handleLoginSuccess(response.data)
            response
        }
    }

    /**
     * 注册
     */
    fun register(username: String, password: String, nickname: String? = null) {
        if (username.isBlank() || password.isBlank()) {
            showToast("用户名和密码不能为空")
            return
        }
        if (username.length < 3) {
            showToast("用户名至少3个字符")
            return
        }
        if (password.length < 6) {
            showToast("密码至少6位")
            return
        }

        request(isShowLoading = true) {
            val response = MoodyApiProvider.apiService.register(
                RegisterRequest(username, password, nickname)
            )
            val user = response.data
            if (response.isSuccess() && user != null) {
                user.token?.let { PreferencesManager.saveUserToken(it) }
                PreferencesManager.saveUserInfo(user.userId.toString(), user.username)
                registerUser.postValue(user)
                EventBusManager.post(EventType.USER_LOGIN, "注册并登录成功", user)
                EventBusManager.post(EventType.AUTH_REGISTER_SUCCESS, "注册成功", user)
            }
            response
        }
    }

    /**
     * 密码 SHA-256 哈希
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 退出登录
     */
    fun logout() {
        request {
            val response = MoodyApiProvider.apiService.logout()
            if (response.isSuccess()) {
                PreferencesManager.clearUserInfo()
                logoutSuccess.postValue(true)
                EventBusManager.post(EventType.USER_LOGOUT, "已退出登录")
            }
            response
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
