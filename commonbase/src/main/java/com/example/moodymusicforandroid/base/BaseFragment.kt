package com.example.moodymusicforandroid.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.moodymusicforandroid.common.eventbus.BaseEvent
import com.example.moodymusicforandroid.common.eventbus.EventBusManager
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Fragment基类
 * 支持泛型ViewModel和DataBinding
 * 自动处理EventBus的注册和注销
 *
 * 使用示例：
 * class MainFragment : BaseFragment<FragmentMainBinding, MainViewModel>() {
 *     override fun getViewModelClass() = MainViewModel::class.java
 *
 *     @Subscribe(threadMode = ThreadMode.MAIN)
 *     override fun onEventReceived(event: BaseEvent) {
 *         when(event.eventType) {
 *             EventType.MUSIC_PLAY -> handleMusicPlay(event)
 *         }
 *     }
 * }
 */
abstract class BaseFragment<VB : ViewDataBinding, VM : BaseViewModel> : Fragment() {

    protected lateinit var binding: VB
    protected lateinit var viewModel: VM

    /**
     * 是否启用 EventBus
     * 默认为 false，如果需要接收事件，重写此方法返回 true
     * 并添加 @Subscribe 注解的 onEventReceived 方法
     */
    protected open fun useEventBus(): Boolean = false

    /**
     * 子类实现此方法返回ViewModel的Class对象
     */
    protected abstract fun getViewModelClass(): Class<VM>

    /**
     * 子类实现此方法返回布局ID
     */
    protected abstract fun getLayoutId(): Int

    /**
     * 是否使用DataBinding
     * 默认为true，如果不需要使用DataBinding，重写此方法返回false
     */
    protected open fun useDataBinding(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initEventBus()  // 注册 EventBus
        initViewModel()
    }

    /**
     * 初始化 EventBus（自动注册）
     */
    private fun initEventBus() {
        if (useEventBus()) {
            EventBusManager.register(this)
        }
    }

    /**
     * 接收事件（子类重写此方法来处理事件）
     * 注意：子类重写时也必须添加 @Subscribe 注解
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onEventReceived(event: BaseEvent) {
        // 子类实现，例如：
        // when(event.eventType) {
        //     EventType.MUSIC_PLAY -> handleMusicPlay(event)
        //     EventType.USER_LOGIN -> handleUserLogin(event)
        // }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (useDataBinding()) {
            binding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
            binding.lifecycleOwner = viewLifecycleOwner
            setupBindingVariables()
            return binding.root
        } else {
            return inflater.inflate(getLayoutId(), container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        initView()
        initData()
    }

    /**
     * 初始化ViewModel
     */
    private fun initViewModel() {
        viewModel = ViewModelProvider(this)[getViewModelClass()]
    }

    /**
     * 设置DataBinding变量
     * 子类可以重写此方法来设置额外的变量
     */
    protected open fun setupBindingVariables() {
        // 默认设置viewModel变量
        // 注意：子类需要重写此方法来设置viewModel
        // binding.viewModel = viewModel
    }

    /**
     * 观察ViewModel的LiveData
     */
    private fun observeViewModel() {
        // 观察Toast消息
        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            if (message.isNotEmpty()) {
                showToast(message)
            }
        }

        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg.isNotEmpty()) {
                handleError(errorMsg)
            }
        }

        // 观察业务错误（可选处理）
        viewModel.businessError.observe(viewLifecycleOwner) { businessError ->
            if (businessError != null) {
                handleBusinessError(businessError)
            }
        }

        // 观察加载状态
        viewModel.loadingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LoadingState.ShowLoading -> showLoadingView()
                is LoadingState.HideLoading -> hideLoadingView()
            }
        }
    }

    /**
     * 处理业务错误
     * 子类可以重写此方法来处理特定的业务错误码
     * @param businessError 业务错误对象
     */
    protected open fun handleBusinessError(businessError: com.example.moodymusicforandroid.common.network.BusinessError) {
        // 默认处理：显示错误消息
        // 子类可以重写此方法来处理特定错误码，例如：
        // when(businessError.code) {
        //     ErrorCode.VIP_REQUIRED -> showVIPDialog()
        //     ErrorCode.MUSIC_NOT_AVAILABLE -> showMusicNotAvailableDialog()
        // }
    }

    /**
     * 初始化View
     * 子类重写此方法进行View初始化
     */
    protected open fun initView() {
        // 子类实现
    }

    /**
     * 初始化数据
     * 子类重写此方法进行数据初始化
     */
    protected open fun initData() {
        // 子类实现
    }

    /**
     * 显示Toast
     */
    protected fun showToast(message: String) {
        com.example.moodymusicforandroid.common.utils.ToastUtils.showShort(context, message)
    }

    /**
     * 处理错误
     */
    protected open fun handleError(errorMsg: String) {
        // 默认显示Toast，子类可以重写
        showToast(errorMsg)
    }

    /**
     * 显示加载框
     * 子类可以重写此方法自定义加载框
     */
    protected open fun showLoadingView() {
        // 子类实现
    }

    /**
     * 隐藏加载框
     * 子类可以重写此方法自定义加载框
     */
    protected open fun hideLoadingView() {
        // 子类实现
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 注销 EventBus
        if (useEventBus()) {
            EventBusManager.unregister(this)
        }
        // 解绑 DataBinding
        if (useDataBinding() && ::binding.isInitialized) {
            binding.unbind()
        }
    }
}
