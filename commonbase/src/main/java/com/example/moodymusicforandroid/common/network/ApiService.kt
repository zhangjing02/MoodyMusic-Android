package com.example.moodymusicforandroid.common.network

import com.example.moodymusicforandroid.data.model.HomeFeedResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API接口
 * 所有网络请求接口都定义在这里
 */
interface ApiService {

    /**
     * 获取首页切片流数据 (SDUI)
     */
    @GET("api/home/feed")
    suspend fun getHomeFeed(): Response<BaseResponse<HomeFeedResponse>>

    /**
     * 示例：根据心情获取音乐列表
     */
    @GET("music/byMood")
    suspend fun getMusicByMood(@Query("mood") mood: String): BaseResponse<List<MusicItem>>

    /**
     * 示例：获取推荐音乐
     */
    @GET("music/recommend")
    suspend fun getRecommendMusic(): BaseResponse<List<MusicItem>>
}

/**
 * 音乐数据项
 */
data class MusicItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Int,
    val coverUrl: String?,
    val audioUrl: String,
    val mood: String
)

/**
 * API服务实例
 */
object ApiServiceProvider {
    val apiService: ApiService by lazy {
        RetrofitClient.create(ApiService::class.java)
    }
}
