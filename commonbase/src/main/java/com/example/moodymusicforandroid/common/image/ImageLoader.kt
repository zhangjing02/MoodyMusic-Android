package com.example.moodymusicforandroid.common.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.File

/**
 * 图片加载工具类
 * 封装 Glide，提供多种图片加载方法
 *
 * 使用示例：
 * ImageLoader.with(context)
 *     .load(url)
 *     .circle()
 *     .into(imageView)
 */
object ImageLoader {

    /**
     * 开始图片加载
     * @param context 上下文
     * @return LoadBuilder 构建器
     */
    fun with(context: Context): LoadBuilder {
        return LoadBuilder(context)
    }

    /**
     * 图片加载构建器
     */
    class LoadBuilder(private val context: Context) {
        private var requestBuilder: RequestBuilder<*> = Glide.with(context).asDrawable()

        // 占位图配置
        private var placeholderRes: Int? = null
        private var errorRes: Int? = null
        private var fallbackRes: Int? = null

        // 变换配置
        private var transformations: List<Transformation<Bitmap>> = emptyList()
        private var isCircle = false
        private var roundedCornerRadius: Int? = null

        // 缓存配置
        private var diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        private var skipMemoryCache = false

        // 尺寸配置
        private var overrideWidth: Int = 0
        private var overrideHeight: Int = 0

        // 淡入动画
        private var crossFade = true

        /**
         * 加载网络图片 URL
         */
        fun load(url: String?): LoadBuilder {
            if (url != null) {
                requestBuilder = Glide.with(context).load(url)
            }
            return this
        }

        /**
         * 加载 Uri
         */
        fun load(uri: Uri?): LoadBuilder {
            if (uri != null) {
                requestBuilder = Glide.with(context).load(uri)
            }
            return this
        }

        /**
         * 加载 File
         */
        fun load(file: File?): LoadBuilder {
            if (file != null) {
                requestBuilder = Glide.with(context).load(file)
            }
            return this
        }

        /**
         * 加载资源 ID
         */
        fun load(@DrawableRes resId: Int): LoadBuilder {
            requestBuilder = Glide.with(context).load(resId)
            return this
        }

        /**
         * 加载 Bitmap
         */
        fun load(bitmap: Bitmap?): LoadBuilder {
            if (bitmap != null) {
                requestBuilder = Glide.with(context).load(bitmap)
            }
            return this
        }

        /**
         * 加载 ByteArray
         */
        fun load(bytes: ByteArray?): LoadBuilder {
            if (bytes != null) {
                requestBuilder = Glide.with(context).load(bytes)
            }
            return this
        }

        /**
         * 设置占位图（加载中显示）
         */
        fun placeholder(@DrawableRes resId: Int): LoadBuilder {
            this.placeholderRes = resId
            return this
        }

        /**
         * 设置错误图（加载失败显示）
         */
        fun error(@DrawableRes resId: Int): LoadBuilder {
            this.errorRes = resId
            return this
        }

        /**
         * 设置回退图（null 时显示）
         */
        fun fallback(@DrawableRes resId: Int): LoadBuilder {
            this.fallbackRes = resId
            return this
        }

        /**
         * 圆形裁剪
         */
        fun circle(): LoadBuilder {
            this.isCircle = true
            return this
        }

        /**
         * 圆角矩形
         * @param radiusDp 圆角半径（单位：dp）
         */
        fun rounded(radiusDp: Int): LoadBuilder {
            this.roundedCornerRadius = radiusDp
            return this
        }

        /**
         * 添加自定义变换
         */
        fun transform(vararg transformation: Transformation<Bitmap>): LoadBuilder {
            this.transformations = transformation.toList()
            return this
        }

        /**
         * 设置磁盘缓存策略
         */
        fun diskCacheStrategy(strategy: DiskCacheStrategy): LoadBuilder {
            this.diskCacheStrategy = strategy
            return this
        }

        /**
         * 跳过内存缓存
         */
        fun skipMemoryCache(skip: Boolean): LoadBuilder {
            this.skipMemoryCache = skip
            return this
        }

        /**
         * 覆盖图片尺寸
         */
        fun override(width: Int, height: Int): LoadBuilder {
            this.overrideWidth = width
            this.overrideHeight = height
            return this
        }

        /**
         * 禁用淡入动画
         */
        fun dontAnimate(): LoadBuilder {
            this.crossFade = false
            return this
        }

        /**
         * 加载到 ImageView
         */
        fun into(imageView: ImageView) {
            // 应用占位图
            if (placeholderRes != null) {
                requestBuilder = requestBuilder.placeholder(placeholderRes!!)
            }

            // 应用错误图
            if (errorRes != null) {
                requestBuilder = requestBuilder.error(errorRes!!)
            }

            // 应用回退图
            if (fallbackRes != null) {
                requestBuilder = requestBuilder.fallback(fallbackRes!!)
            }

            // 应用缓存策略
            requestBuilder = requestBuilder.diskCacheStrategy(diskCacheStrategy)
            requestBuilder = requestBuilder.skipMemoryCache(skipMemoryCache)

            // 应用尺寸覆盖
            if (overrideWidth > 0 && overrideHeight > 0) {
                requestBuilder = requestBuilder.override(overrideWidth, overrideHeight)
            }

            // 应用淡入动画
            if (crossFade) {
                // 注意：Glide 的淡入动画是默认的，这里不做额外处理
            }

            // 应用变换
            if (isCircle) {
                requestBuilder = requestBuilder.transform(GlideCircleTransform.INSTANCE)
            } else if (roundedCornerRadius != null) {
                val radiusPx = (roundedCornerRadius!! * context.resources.displayMetrics.density).toInt()
                requestBuilder = requestBuilder.transform(CenterCrop(), RoundedCorners(radiusPx))
            } else if (transformations.isNotEmpty()) {
                requestBuilder = requestBuilder.transform(*transformations.toTypedArray())
            }

            requestBuilder.into(imageView)
        }

        /**
         * 预加载图片到缓存
         */
        fun preload() {
            requestBuilder
                .diskCacheStrategy(diskCacheStrategy)
                .skipMemoryCache(skipMemoryCache)
                .preload()
        }

        /**
         * 清除内存缓存
         */
        fun clearMemoryCache() {
            Glide.get(context).clearMemory()
        }

        /**
         * 清除磁盘缓存（需要在后台线程执行）
         */
        fun clearDiskCache(onComplete: (() -> Unit)? = null) {
            Thread {
                Glide.get(context).clearDiskCache()
                onComplete?.invoke()
            }.start()
        }
    }

    /**
     * 简单方法：加载 URL
     */
    fun loadUrl(context: Context, url: String?, imageView: ImageView) {
        with(context).load(url).into(imageView)
    }

    /**
     * 简单方法：加载 URL 并显示为圆形
     */
    fun loadUrlCircle(context: Context, url: String?, imageView: ImageView) {
        with(context).load(url).circle().into(imageView)
    }

    /**
     * 简单方法：加载 URL 并显示为圆角矩形
     */
    fun loadUrlRounded(context: Context, url: String?, imageView: ImageView, radiusDp: Int = 10) {
        with(context).load(url).rounded(radiusDp).into(imageView)
    }

    /**
     * 简单方法：加载资源
     */
    fun loadResource(context: Context, resId: Int, imageView: ImageView) {
        with(context).load(resId).into(imageView)
    }

    /**
     * 简单方法：加载本地文件
     */
    fun loadFile(context: Context, file: File?, imageView: ImageView) {
        with(context).load(file).into(imageView)
    }

    /**
     * 简单方法：加载 Uri
     */
    fun loadUri(context: Context, uri: Uri?, imageView: ImageView) {
        with(context).load(uri).into(imageView)
    }

    /**
     * 清除所有缓存（内存 + 磁盘）
     */
    fun clearAllCache(context: Context, onComplete: (() -> Unit)? = null) {
        Glide.get(context).clearMemory()
        Thread {
            Glide.get(context).clearDiskCache()
            onComplete?.invoke()
        }.start()
    }
}
