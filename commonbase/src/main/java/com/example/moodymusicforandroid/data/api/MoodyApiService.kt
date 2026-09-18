package com.example.moodymusicforandroid.data.api

import com.example.moodymusicforandroid.common.network.BaseResponse
import com.example.moodymusicforandroid.data.model.*
import retrofit2.http.*

/**
 * Moody 音乐库 API 接口
 */
interface MoodyApiService {

    /**
     * 获取首页切片流 (SDUI)
     * GET /api/home/feed
     */
    @GET("api/home/feed")
    suspend fun getHomeFeed(): BaseResponse<HomeFeedResponse>

    /**
     * 获取专栏故事详情 (通过完整 URL，如 R2 静态直链)
     */
    @GET
    suspend fun getThemeStoryByUrl(@Url url: String): ThemeStoryDto

    /**
     * 获取专栏故事详情 (通过 themeId 请求 R2 静态直链)
     * GET storage/themes/{themeId}.json
     */
    @GET("storage/themes/{themeId}.json")
    suspend fun getThemeStoryById(@Path("themeId") themeId: String): ThemeStoryDto

    /**
     * 获取艺人列表（骨架数据）
     * GET /api/skeleton?group=A
     */
    @GET("api/skeleton")
    suspend fun getArtists(
        @Query("group") group: String? = null
    ): BaseResponse<ArtistsData>

    /**
     * 获取完整歌曲数据
     * GET /api/songs?artist=周杰伦&album=Jay
     */
    @GET("api/songs")
    suspend fun getSongs(
        @Query("artistId") artistId: String? = null,
        @Query("artist") artist: String? = null,
        @Query("album") album: String? = null
    ): BaseResponse<List<Artist>>

    /**
     * 获取艺人详情（含专辑+歌曲嵌套树）
     * GET /api/songs?artistId=db_xxx
     * 返回 List<ArtistWithAlbums>，通常只含一个元素（对应该艺人）
     */
    @GET("api/songs")
    suspend fun getArtistDetail(
        @Query("artistId") artistId: String
    ): BaseResponse<List<ArtistWithAlbums>>

    /**
     * 全局搜索
     * GET /api/search?q=周杰伦
     */
    @GET("api/search")
    suspend fun search(
        @Query("q") keyword: String
    ): BaseResponse<SearchResult>

    /**
     * 获取欢迎页背景图
     * GET /api/welcome-images
     */
    @GET("api/welcome-images")
    suspend fun getWelcomeImages(): BaseResponse<List<String>>

    /**
     * 获取系统统计
     * GET /api/admin/stats
     */
    @GET("api/admin/stats")
    suspend fun getSystemStats(): BaseResponse<SystemStats>

    /**
     * 获取媒体文件（音乐、封面等）
     * GET /storage/{path}
     */
    @GET
    suspend fun getMediaFile(@Url url: String): retrofit2.Response<okhttp3.ResponseBody>

    // ==================== 认证相关 ====================

    // ==================== 邮箱 OTP 认证与音信资料 ====================

    /**
     * 发送邮箱验证码
     * POST /api/auth/send-code
     */
    @POST("api/auth/send-code")
    suspend fun sendVerificationCode(@Body request: SendCodeRequest): BaseResponse<SendCodeResponse>

    /**
     * 邮箱验证码登录 / 极速注册
     * POST /api/auth/verify-code
     */
    @POST("api/auth/verify-code")
    suspend fun loginWithCode(@Body request: VerifyCodeRequest): BaseResponse<LoginData>

    /**
     * 用户名查重
     * GET /api/auth/check-username?username=xxx
     */
    @GET("api/auth/check-username")
    suspend fun checkUsername(@Query("username") username: String): BaseResponse<CheckUsernameResponse>

    /**
     * 邮箱验证码注册并设置初始密码
     * POST /api/auth/register-with-code
     */
    @POST("api/auth/register-with-code")
    suspend fun registerWithCode(@Body request: RegisterWithCodeRequest): BaseResponse<LoginData>

    /**
     * 邮箱+密码登录
     * POST /api/auth/login-with-password
     */
    @POST("api/auth/login-with-password")
    suspend fun loginWithPassword(@Body request: PasswordLoginRequest): BaseResponse<LoginData>

    /**
     * 邮箱验证码重置密码
     * POST /api/auth/reset-password
     */
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): BaseResponse<Any>

    /**
     * 更新极光推送 RegistrationId（App 启动 / 极光重新注册后调用）
     * POST /api/auth/update-jpush-id
     */
    @POST("api/auth/update-jpush-id")
    suspend fun updateJPushRegistrationId(@Body request: Map<String, @JvmSuppressWildcards Any>): BaseResponse<Any>

    /**
     * 获取用户完整资料及统计
     * GET /api/user/profile
     */
    @GET("api/user/profile")
    suspend fun getUserProfile(): BaseResponse<User>

    /**
     * 更新用户资料
     * PUT /api/user/profile
     */
    @PUT("api/user/profile")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): BaseResponse<User>

    /**
     * 获取音信页面音乐资产（收藏专辑与关注歌手）
     * GET /api/user/library
     */
    @GET("api/user/library")
    suspend fun getUserLibrary(): BaseResponse<UserLibraryResponse>

    /**
     * 收藏/取消收藏专辑
     * POST /api/user/library/favorite-album
     */
    @POST("api/user/library/favorite-album")
    suspend fun toggleFavoriteAlbum(@Body request: Map<String, String>): BaseResponse<Any>

    /**
     * 关注/取消关注歌手
     * POST /api/user/library/follow-artist
     */
    @POST("api/user/library/follow-artist")
    suspend fun toggleFollowArtist(@Body request: Map<String, String>): BaseResponse<Any>

    /**
     * 收藏/取消收藏歌曲
     * POST /api/user/library/favorite-song
     */
    @POST("api/user/library/favorite-song")
    suspend fun toggleFavoriteSong(@Body request: FavoriteSongToggleRequest): BaseResponse<FavoriteSongToggleResponse>

    /**
     * 批量移除资产（取消收藏歌曲/专辑/关注歌手）
     * POST /api/user/library/batch-remove
     */
    @POST("api/user/library/batch-remove")
    suspend fun batchRemoveLibraryItems(@Body request: BatchRemoveRequest): BaseResponse<Map<String, Any>>

    /**
     * 用户注册（兼容旧接口）
     * POST /api/auth/register
     */
    @POST("api/user/register")
    suspend fun register(@Body request: RegisterRequest): BaseResponse<User>

    /**
     * 登录（兼容旧接口）
     */
    @POST("api/user/login")
    suspend fun login(@Body request: LoginRequest): BaseResponse<LoginData>

    /**
     * 刷新 Token
     */
    @POST("api/user/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): BaseResponse<User>

    /**
     * 退出登录
     * POST /api/auth/logout
     */
    @POST("api/auth/logout")
    suspend fun logout(): BaseResponse<Any>

    /**
     * 获取用户信息
     * GET /api/auth/profile
     */
    @GET("api/user/me")
    suspend fun getProfile(): BaseResponse<User>

    // ==================== 收藏相关 ====================

    /**
     * 收藏/取消收藏歌曲（toggle模式）
     * POST /api/user/favorites/toggle
     */
    @POST("api/user/favorites/toggle")
    suspend fun toggleFavorite(@Body body: Map<String, @JvmSuppressWildcards Any>): BaseResponse<ToggleResult>

    /**
     * 获取收藏列表（分页）
     * GET /api/user/favorites?page=1&limit=20
     */
    @GET("api/user/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): BaseResponse<FavoritesData>

    /**
     * 检查歌曲是否已收藏
     * GET /api/user/favorites/check?songId=123
     */
    @GET("api/user/favorites/check")
    suspend fun checkFavorite(@Query("songId") songId: Long): BaseResponse<FavoriteCheckResult>

    // ==================== 关注相关 ====================

    /**
     * 关注/取消关注歌手（toggle模式）
     * POST /api/user/follows/toggle
     */
    @POST("api/user/follows/toggle")
    suspend fun toggleFollow(@Body body: Map<String, @JvmSuppressWildcards Any>): BaseResponse<ToggleFollowResult>

    /**
     * 获取关注歌手列表
     * GET /api/user/follows
     */
    @GET("api/user/follows")
    suspend fun getFollows(): BaseResponse<FollowsData>

    // ==================== 评论相关 ====================

    /**
     * 发表评论
     * POST /api/comments
     */
    @POST("api/comments")
    suspend fun createComment(@Body request: CreateCommentRequest): BaseResponse<Comment>

    /**
     * 获取评论列表（公开）
     * GET /api/comments?targetType=song&targetId=123&page=1&limit=20
     */
    @GET("api/comments")
    suspend fun getComments(
        @Query("targetType") targetType: String,
        @Query("targetId") targetId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): BaseResponse<CommentsData>

    /**
     * 删除评论
     * DELETE /api/comments/:id
     */
    @DELETE("api/comments/{id}")
    suspend fun deleteComment(@Path("id") commentId: Long): BaseResponse<Any>

    // ==================== 教室座位表相关 ====================
    
    /**
     * 获取座位表
     * GET /api/roster
     */
    @GET("api/roster")
    suspend fun getRoster(): RosterResponse

    /**
     * 验证认领答案 (第二步)
     */
    @POST("api/user/claim/verify")
    suspend fun verifyClaim(@Body request: VerifyClaimRequest): VerifyClaimResponse

    /**
     * 完成认领 (第三步) - 不带邮箱
     */
    @POST("api/user/claim/finalize")
    suspend fun finalizeClaim(@Body request: FinalizeClaimRequest): FinalizeClaimResponse

    /**
     * 完成认领 (第三步) - 带邮箱
     */
    @POST("api/user/claim/finalize")
    suspend fun finalizeClaimWithEmail(@Body request: FinalizeClaimWithEmailRequest): FinalizeClaimResponse

    // ══════════════════════════════════════════
    // 专辑社交功能
    // 所有接口均需要 Authorization: Bearer {token}
    // ══════════════════════════════════════════

    /**
     * 获取专辑社交内容（主贴 + 全部回复）
     * GET /api/albums/{albumId}/social_content
     */
    @GET("api/albums/{albumId}/social_content")
    suspend fun getAlbumSocialContent(
        @Path("albumId") albumId: String
    ): BaseResponse<AlbumSocialContent>

    /**
     * 在专辑下发主贴（每班级唯一）
     * POST /api/albums/{albumId}/posts
     */
    @POST("api/albums/{albumId}/posts")
    suspend fun postAlbumPost(
        @Path("albumId") albumId: String,
        @Body body: PostContentRequest
    ): BaseResponse<Any>

    /**
     * 在主贴下发评论
     * POST /api/albums/posts/{postId}/comments
     */
    @POST("api/albums/posts/{postId}/comments")
    suspend fun postAlbumComment(
        @Path("postId") postId: String,
        @Body body: PostContentRequest
    ): BaseResponse<Any>

    /**
     * 检查 App 版本更新
     * GET /api/app/version/check
     */
    @GET("api/app/version/check")
    suspend fun checkAppVersion(): BaseResponse<AppVersionData>

    // ══════════════════════════════════════════
    // 系统公告与多板块留言互动接口
    // ══════════════════════════════════════════

    /**
     * 获取系统公告列表
     * GET /api/notices
     */
    @GET("api/notices")
    suspend fun getNotices(): BaseResponse<List<SystemNotice>>

    /**
     * 发布系统公告 (管理员/Master)
     * POST /api/notices
     */
    @POST("api/notices")
    suspend fun createNotice(@Body request: CreateNoticeRequest): BaseResponse<SystemNotice>

    /**
     * 删除系统公告 (管理员/Master)
     * DELETE /api/notices/{id}
     */
    @DELETE("api/notices/{id}")
    suspend fun deleteNotice(@Path("id") id: Long): BaseResponse<Any>

    /**
     * 获取社区留言与待办列表
     * GET /api/community/posts?category=xxx&status=xxx
     */
    @GET("api/community/posts")
    suspend fun getCommunityPosts(
        @Query("category") category: String? = null,
        @Query("status") status: String? = null
    ): BaseResponse<List<CommunityPost>>

    /**
     * 发布留言 / 反馈 / 待办任务
     * POST /api/community/posts
     */
    @POST("api/community/posts")
    suspend fun createCommunityPost(@Body request: CreatePostRequest): BaseResponse<CommunityPost>

    /**
     * 获取帖子详情
     * GET /api/community/posts/{id}
     */
    @GET("api/community/posts/{id}")
    suspend fun getCommunityPostDetail(@Path("id") id: Long): BaseResponse<CommunityPost>

    /**
     * 更新待办任务状态 (打钩标记完成/待处理，管理员/Master)
     * PATCH /api/community/posts/{id}/status
     */
    @PATCH("api/community/posts/{id}/status")
    suspend fun updateCommunityPostStatus(
        @Path("id") id: Long,
        @Body request: UpdatePostStatusRequest
    ): BaseResponse<CommunityPost>

    /**
     * 删除帖子 (管理员/Master可删任意，作者可删自己)
     * DELETE /api/community/posts/{id}
     */
    @DELETE("api/community/posts/{id}")
    suspend fun deleteCommunityPost(@Path("id") id: Long): BaseResponse<Any>

    /**
     * 获取帖子下的评论列表
     * GET /api/community/posts/{id}/comments
     */
    @GET("api/community/posts/{id}/comments")
    suspend fun getCommunityComments(@Path("id") postId: Long): BaseResponse<List<CommunityComment>>

    /**
     * 发表跟帖评论
     * POST /api/community/posts/{id}/comments
     */
    @POST("api/community/posts/{id}/comments")
    suspend fun createCommunityComment(
        @Path("id") postId: Long,
        @Body request: CreateCommunityCommentRequest
    ): BaseResponse<CommunityComment>

    /**
     * 删除评论 (管理员/Master可删任意，作者可删自己)
     * DELETE /api/community/comments/{id}
     */
    @DELETE("api/community/comments/{id}")
    suspend fun deleteCommunityComment(@Path("id") commentId: Long): BaseResponse<Any>

    // ==================== 自定义播放列表 (Playlists) ====================

    @GET("api/user/playlists")
    suspend fun getUserPlaylists(): BaseResponse<List<com.example.moodymusicforandroid.data.model.Playlist>>

    @POST("api/user/playlists")
    suspend fun createPlaylist(@Body request: com.example.moodymusicforandroid.data.model.CreatePlaylistRequest): BaseResponse<com.example.moodymusicforandroid.data.model.Playlist>

    @PUT("api/user/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") id: Long,
        @Body request: com.example.moodymusicforandroid.data.model.UpdatePlaylistRequest
    ): BaseResponse<com.example.moodymusicforandroid.data.model.Playlist>

    @DELETE("api/user/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: Long): BaseResponse<Any>

    @GET("api/user/playlists/{id}/songs")
    suspend fun getPlaylistSongs(@Path("id") id: Long): BaseResponse<com.example.moodymusicforandroid.data.model.PlaylistDetailData>

    @POST("api/user/playlists/{id}/songs")
    suspend fun addSongToPlaylist(
        @Path("id") id: Long,
        @Body request: com.example.moodymusicforandroid.data.model.AddSongToPlaylistRequest
    ): BaseResponse<Any>

    @DELETE("api/user/playlists/{id}/songs/{songId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") id: Long,
        @Path("songId") songId: Long
    ): BaseResponse<Any>

    @GET("api/user/playlists/memberships/{songId}")
    suspend fun getSongPlaylistMemberships(@Path("songId") songId: Long): BaseResponse<com.example.moodymusicforandroid.data.model.SongMembershipsData>
}


/**
 * 艺人数据包装兼容别名
 */
typealias ArtistsData = com.example.moodymusicforandroid.data.model.ArtistsData
