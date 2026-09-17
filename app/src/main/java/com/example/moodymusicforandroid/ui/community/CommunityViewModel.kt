package com.example.moodymusicforandroid.ui.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodymusicforandroid.data.api.MoodyApiProvider
import com.example.moodymusicforandroid.data.manager.UserManager
import com.example.moodymusicforandroid.data.model.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 社区留言板、待办任务与系统公告 ViewModel
 */
class CommunityViewModel : ViewModel() {

    private val _notices = MutableStateFlow<List<SystemNotice>>(emptyList())
    val notices: StateFlow<List<SystemNotice>> = _notices.asStateFlow()

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    private val _selectedCategory = MutableStateFlow("resource")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedResourceSubTab = MutableStateFlow("album")
    val selectedResourceSubTab: StateFlow<String> = _selectedResourceSubTab.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _commentsMap = MutableStateFlow<Map<Long, List<CommunityComment>>>(emptyMap())
    val commentsMap: StateFlow<Map<Long, List<CommunityComment>>> = _commentsMap.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    init {
        fetchNotices()
        fetchPosts("resource")
    }

    // ==================== 公告相关 ====================

    fun fetchNotices() {
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.getNotices()
                if (res.isSuccess()) {
                    _notices.value = res.data ?: emptyList()
                }
            } catch (e: Exception) {
                // 网络异常时不覆盖当前列表
            }
        }
    }

    fun createNotice(title: String, content: String, isPinned: Boolean, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.createNotice(
                    CreateNoticeRequest(title = title, content = content, isPinned = isPinned)
                )
                val noticeData = res.data
                if (res.isSuccess() && noticeData != null) {
                    _notices.value = listOf(noticeData) + _notices.value
                    _toastMessage.emit("公告发布成功")
                    onSuccess()
                } else {
                    _toastMessage.emit(res.message ?: "发布失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit("网络异常，请重试")
            }
        }
    }

    fun deleteNotice(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                _notices.value = _notices.value.filterNot { it.id == id }
                val res = MoodyApiProvider.apiService.deleteNotice(id)
                if (res.isSuccess()) {
                    _toastMessage.emit("公告已删除")
                    onSuccess()
                } else {
                    _toastMessage.emit(res.message ?: "删除失败")
                    fetchNotices()
                }
            } catch (e: Exception) {
                _toastMessage.emit("网络异常，删除失败")
                fetchNotices()
            }
        }
    }

    // ==================== 留言与待办相关 ====================

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        fetchPosts(category)
    }

    fun selectResourceSubTab(subTab: String) {
        _selectedResourceSubTab.value = subTab
    }

    /**
     * 过滤资源补齐帖子：若在“资源补齐”板块，按当前二级子分类精准匹配
     */
    fun filterPostsBySubTab(postsList: List<CommunityPost>, category: String, subTab: String): List<CommunityPost> {
        if (category != "resource") return postsList
        return postsList.filter { post ->
            val data = ResourceHelper.decode(post.content)
            val actualSub = data?.subType ?: when {
                post.title.contains("【歌曲补齐】") || post.title.contains("单曲补齐") -> "song"
                post.title.contains("【歌手补齐】") -> "artist"
                post.title.contains("【演唱会补齐】") || post.title.contains("现场补齐") -> "concert"
                post.title.contains("【音质升级】") || post.title.contains("无损补齐") -> "hires"
                else -> "album"
            }
            actualSub == subTab
        }
    }

    fun fetchPosts(category: String? = _selectedCategory.value) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val catParam = if (category == "all" || category.isNullOrBlank()) null else category
                val res = MoodyApiProvider.apiService.getCommunityPosts(category = catParam)
                if (res.isSuccess()) {
                    _posts.value = res.data ?: emptyList()
                }
            } catch (e: Exception) {
                // 网络异常时不覆盖当前列表
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Master / 管理员一键打钩（切换待办状态为 completed / pending）
     */
    fun toggleTaskStatus(post: CommunityPost) {
        val nextStatus = if (post.status == "completed") "pending" else "completed"
        val currentList = _posts.value

        // 1. 立即乐观更新本地状态
        _posts.value = currentList.map {
            if (it.id == post.id) it.copy(status = nextStatus) else it
        }

        // 2. 异步同步云端
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.updateCommunityPostStatus(
                    id = post.id,
                    request = UpdatePostStatusRequest(status = nextStatus)
                )
                val updated = res.data
                if (res.isSuccess() && updated != null) {
                    _posts.value = _posts.value.map {
                        if (it.id == post.id) updated else it
                    }
                    _toastMessage.emit(if (nextStatus == "completed") "已标记为完成 ✓" else "已标记为待处理")
                } else {
                    // 回滚
                    _posts.value = currentList
                    _toastMessage.emit(res.message ?: "更新失败")
                }
            } catch (e: Exception) {
                _posts.value = currentList
                _toastMessage.emit("网络异常，状态已恢复")
            }
        }
    }

    /**
     * 发布新贴
     */
    fun createPost(category: String, title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.createCommunityPost(
                    CreatePostRequest(category = category, title = title, content = content)
                )
                val postData = res.data
                if (res.isSuccess() && postData != null) {
                    _posts.value = listOf(postData) + _posts.value
                    _toastMessage.emit("发布成功")
                    onSuccess()
                } else {
                    _toastMessage.emit(res.message ?: "发布失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit("发布失败，请检查网络")
            }
        }
    }

    /**
     * 删除帖子 (Master/Admin可删任意，作者本人可删自己)
     */
    fun deletePost(postId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val original = _posts.value
            _posts.value = original.filterNot { it.id == postId }
            try {
                val res = MoodyApiProvider.apiService.deleteCommunityPost(postId)
                if (res.isSuccess()) {
                    _toastMessage.emit("已删除")
                    onSuccess()
                } else {
                    _posts.value = original
                    _toastMessage.emit(res.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _posts.value = original
                _toastMessage.emit("删除异常，已还原")
            }
        }
    }

    // ==================== 跟帖评论相关 ====================

    fun fetchComments(postId: Long) {
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.getCommunityComments(postId)
                if (res.isSuccess() && res.data != null) {
                    val map = _commentsMap.value.toMutableMap()
                    map[postId] = res.data ?: emptyList()
                    _commentsMap.value = map
                }
            } catch (_: Exception) {}
        }
    }

    fun createComment(postId: Long, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val res = MoodyApiProvider.apiService.createCommunityComment(
                    postId = postId,
                    request = CreateCommunityCommentRequest(content = content)
                )
                val newComment = res.data
                if (res.isSuccess() && newComment != null) {
                    val currentComments = _commentsMap.value[postId].orEmpty()
                    val map = _commentsMap.value.toMutableMap()
                    map[postId] = currentComments + newComment
                    _commentsMap.value = map

                    // 更新帖子的 commentCount
                    _posts.value = _posts.value.map {
                        if (it.id == postId) it.copy(commentCount = it.commentCount + 1) else it
                    }

                    _toastMessage.emit("评论成功")
                    onSuccess()
                } else {
                    _toastMessage.emit(res.message ?: "评论失败")
                }
            } catch (e: Exception) {
                _toastMessage.emit("网络异常，评论发送失败")
            }
        }
    }

    fun deleteComment(postId: Long, commentId: Long) {
        viewModelScope.launch {
            val currentComments = _commentsMap.value[postId].orEmpty()
            val filtered = currentComments.filterNot { it.id == commentId }
            val map = _commentsMap.value.toMutableMap()
            map[postId] = filtered
            _commentsMap.value = map

            _posts.value = _posts.value.map {
                if (it.id == postId) it.copy(commentCount = maxOf(0, it.commentCount - 1)) else it
            }

            try {
                val res = MoodyApiProvider.apiService.deleteCommunityComment(commentId)
                if (res.isSuccess()) {
                    _toastMessage.emit("评论已删除")
                } else {
                    map[postId] = currentComments
                    _commentsMap.value = map
                    _toastMessage.emit(res.message ?: "删除失败")
                }
            } catch (e: Exception) {
                map[postId] = currentComments
                _commentsMap.value = map
                _toastMessage.emit("网络异常，删除失败")
            }
        }
    }

}
