package com.example.moodymusicforandroid.common.network

import com.example.moodymusicforandroid.common.preferences.PreferencesManager
import com.example.moodymusicforandroid.data.model.RefreshTokenRequest
import com.example.moodymusicforandroid.data.model.User
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Authenticator
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
import java.net.Inet4Address
import java.net.InetAddress
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

    /**
     * IPv4 优先 DNS 策略
     *
     * 国内三大运营商 4G/5G 网络在双栈环境下，OkHttp 默认通过 Happy Eyeballs 算法
     * 优先尝试 IPv6 连接。然而 Cloudflare Anycast IPv6 在国内骨干网（电信/联通/移动）
     * 存在严重丢包与路由黑洞，导致 API 请求（/api/skeleton、/api/home/feed 等）
     * 连接超时（15秒）后才回退到 IPv4，进而引发歌手列表、首页数据全部加载失败。
     *
     * 此 DNS 实现与 LocalMediaProxy 的 IPv4 强制策略保持完全一致：
     * 解析结果中优先筛选 Inet4Address，仅当无 IPv4 地址时才降级使用原始结果。
     *
     * 注：okhttp3.Dns 是 Java interface，Kotlin 不支持 SAM lambda 简写，
     * 必须使用 object : Dns { override fun lookup(...) } 显式匿名对象写法。
     */
    private val ipv4PreferredDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                val addresses = Dns.SYSTEM.lookup(hostname)
                val ipv4List = addresses.filterIsInstance<Inet4Address>()
                if (ipv4List.isNotEmpty()) ipv4List else addresses
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
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
            .dns(ipv4PreferredDns)
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

    /**
     * 将请求的目标基准地址改写为指定网关（保持原始 path、query 和 headers 不变）
     */
    private fun rewriteRequestGateway(request: Request, targetBaseUrl: String): Request? {
        if (targetBaseUrl.isBlank()) return null
        val targetHttpUrl = targetBaseUrl.toHttpUrlOrNull() ?: return null
        val originalUrl = request.url
        val newUrl = originalUrl.newBuilder()
            .scheme(targetHttpUrl.scheme)
            .host(targetHttpUrl.host)
            .port(targetHttpUrl.port)
            .build()
        return request.newBuilder().url(newUrl).build()
    }

    /**
     * 三级高可用重试与容灾拦截器 (Multi-Gateway Failover Interceptor)
     *
     * 设计哲学：
     * 1. 【零冗余开销】：主网关正常时 100% 独占流量，绝不向备用网关发起任何多余网络探测；
     * 2. 【备用网关秒级平移】：主网关遇网络中断 (SocketException, 5xx等)，自动使用备用网关 (MOODY_API_FALLBACK_URL) 透明重试；
     * 3. 【生命线最后防线】：版本更新检测接口 (/api/app/version/check) 在前两者均不可达时，
     *    启用独立第三账号网关 (MOODY_API_LIFELINE_URL) 强力托底，确保升级弹窗与救命广播永远可达。
     */
    private val retryInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val canRetry = originalRequest.method.equals("GET", ignoreCase = true) ||
            originalRequest.method.equals("HEAD", ignoreCase = true)

        // 1. 尝试主网关
        var response = try {
            chain.proceed(originalRequest)
        } catch (_: Exception) {
            null
        }

        // 2. 主网关失败且允许重试，先在主网关重试 1 次
        if (canRetry && (response == null || response.code in 500..599)) {
            response?.close()
            response = try {
                chain.proceed(originalRequest)
            } catch (_: Exception) {
                null
            }
        }

        // 3. 主网关仍失败：自动平移至第二备用网关 (MOODY_API_FALLBACK_URL)
        val fallbackBase = com.example.moodymusicforandroid.common.config.AppConfig.apiFallbackUrl
        if (canRetry && (response == null || response.code in 500..599) && fallbackBase.isNotBlank()) {
            val fallbackRequest = rewriteRequestGateway(originalRequest, fallbackBase)
            if (fallbackRequest != null) {
                response?.close()
                response = try {
                    chain.proceed(fallbackRequest)
                } catch (_: Exception) {
                    null
                }
            }
        }

        // 4. 若为版本检测关键接口，且备用网关仍失败：启动第三生命线网关 (MOODY_API_LIFELINE_URL) 强力兜底
        val isVersionCheck = originalRequest.url.encodedPath.contains("app/version/check")
        val lifelineBase = com.example.moodymusicforandroid.common.config.AppConfig.apiLifelineUrl
        if (canRetry && isVersionCheck && (response == null || response.code in 500..599) && lifelineBase.isNotBlank()) {
            val lifelineRequest = rewriteRequestGateway(originalRequest, lifelineBase)
            if (lifelineRequest != null) {
                response?.close()
                response = try {
                    chain.proceed(lifelineRequest)
                } catch (_: Exception) {
                    null
                }
            }
        }

        response ?: throw java.io.IOException("Network request failed after multi-gateway retries")
    }

    private val okHttpClient = OkHttpClient.Builder()
        .dns(ipv4PreferredDns)
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