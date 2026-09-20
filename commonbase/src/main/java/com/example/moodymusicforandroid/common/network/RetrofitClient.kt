package com.example.moodymusicforandroid.common.network

import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.model.RefreshTokenRequest
import com.example.moodymusicforandroid.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val BASE_URL_DEV = com.example.moodymusicforandroid.common.config.AppConfig.BASE_URL_DEV
    private val BASE_URL_PROD = com.example.moodymusicforandroid.common.config.AppConfig.apiBaseUrl
    private val BASE_URL = BASE_URL_PROD

    private const val CONNECT_TIMEOUT = 15L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    @Volatile
    private var last401KickTime = 0L

    private val headerInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "MoodyMusicAndroid/1.0")

        try {
            requestBuilder.header("X-Client-Type", "android")
            requestBuilder.header("X-App-Platform", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getPlatform())
            requestBuilder.header("X-App-Version-Code", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getVersionCode().toString())
            requestBuilder.header("X-App-Version-Name", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getVersionName())
            requestBuilder.header("X-App-Package-Name", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getPackageName())
            requestBuilder.header("X-App-Channel", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getChannel())
            requestBuilder.header("X-Device-Brand", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getBrand())
            requestBuilder.header("X-Device-Model", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getModel())
            requestBuilder.header("X-Device-OS-Version", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getOsVersion())
            requestBuilder.header("X-Device-SDK-Int", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getSdkInt().toString())
            requestBuilder.header("X-Device-Arch", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getArch())
            requestBuilder.header("X-Device-Id", PreferencesManager.getDeviceId() ?: "")
            requestBuilder.header("X-JPush-Registration-Id", PreferencesManager.getJPushRegistrationId() ?: "")
            requestBuilder.header("X-App-Locale", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getLocale())
            requestBuilder.header("X-Network-Type", com.example.moodymusicforandroid.common.utils.DeviceInfoUtils.getNetworkType())

            val token = PreferencesManager.getUserToken()
            if (!token.isNullOrEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        } catch (_: Exception) {
            // PreferencesManager or context may not be initialized yet.
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            try {
                val peek = response.peekBody(2048).string()
                if (peek.contains("TOKEN_EXPIRED_OR_INVALID") ||
                    peek.contains("SESSION_KICKED_OUT") ||
                    peek.contains("在其他设备登录") ||
                    peek.contains("已在其他设备")
                ) {
                    val now = System.currentTimeMillis()
                    if (now - last401KickTime > 6000L) {
                        last401KickTime = now
                        android.util.Log.e("RetrofitClient", "=== 401 KICK OUT DETECTED IN OKHTTP ===")
                        PreferencesManager.clearUserInfo()
                        try {
                            com.example.moodymusicforandroid.data.manager.UserManager.onLogout()
                        } catch (_: Exception) {}
                        PreferencesManager.getContext()?.let { ctx ->
                            com.example.moodymusicforandroid.common.utils.ToastUtils.showShort(
                                ctx,
                                "您的账号已在其他设备登录，当前已退出登录"
                            )
                        }
                        com.example.moodymusicforandroid.common.eventbus.EventBusManager.post(
                            com.example.moodymusicforandroid.common.eventbus.EventType.AUTH_TOKEN_EXPIRED,
                            "KICKED_OUT"
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        response
    }

    val defaultGson: Gson = Gson()

    private val refreshHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        synchronized(this) {
            val refreshToken = PreferencesManager.getUserRefreshToken()
            if (refreshToken.isNullOrEmpty()) return@Authenticator null

            val currentToken = PreferencesManager.getUserToken()
            val currentAuthHeader = if (currentToken.isNullOrEmpty()) null else "Bearer $currentToken"
            if (response.request.header("Authorization") != currentAuthHeader) {
                return@Authenticator response.request.newBuilder().apply {
                    if (!currentToken.isNullOrEmpty()) {
                        header("Authorization", "Bearer $currentToken")
                    }
                }.build()
            }

            try {
                val requestBody = defaultGson.toJson(RefreshTokenRequest(refreshToken))
                    .toRequestBody("application/json".toMediaTypeOrNull())
                val refreshRequest = Request.Builder()
                    .url("${BASE_URL}api/user/refresh")
                    .post(requestBody)
                    .build()

                val refreshResponse = refreshHttpClient.newCall(refreshRequest).execute()
                if (refreshResponse.isSuccessful) {
                    val bodyString = refreshResponse.body?.string()
                    val type = object : TypeToken<BaseResponse<User>>() {}.type
                    val result: BaseResponse<User> = defaultGson.fromJson(bodyString, type)
                    val userData = result.data

                    if (userData?.token != null) {
                        PreferencesManager.saveUserToken(userData.token)
                        userData.refreshToken?.let { PreferencesManager.saveUserRefreshToken(it) }

                        return@Authenticator response.request.newBuilder()
                            .header("Authorization", "Bearer ${userData.token}")
                            .build()
                    }
                }
            } catch (_: Exception) {
                // ignore and fallback to logout
            }

            PreferencesManager.clearUserInfo()
            null
        }
    }

    private val retryInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val canRetry = originalRequest.method.equals("GET", ignoreCase = true) ||
            originalRequest.method.equals("HEAD", ignoreCase = true)

        var response = try {
            chain.proceed(originalRequest)
        } catch (_: Exception) {
            null
        }

        var retryCount = 0
        val maxRetryCount = 2

        while (canRetry && (response == null || response.code in 500..599) && retryCount < maxRetryCount) {
            retryCount++
            response?.close()
            response = try {
                chain.proceed(originalRequest)
            } catch (_: Exception) {
                null
            }
        }

        response ?: throw java.io.IOException("Network request failed after retries")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .addInterceptor(retryInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val serviceCache = java.util.concurrent.ConcurrentHashMap<Class<*>, Any>()

    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(defaultGson))
            .build()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> create(service: Class<T>): T {
        return serviceCache.getOrPut(service) {
            retrofitInstance.create(service) as Any
        } as T
    }

    fun getBaseUrl(): String = BASE_URL
}