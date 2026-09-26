plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.moodymusicforandroid.commonbase"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        consumerProguardFiles("consumer-rules.pro")

        val apiBaseUrl = (project.findProperty("MOODY_API_BASE_URL") as? String)
            ?: "https://m-api.changgepd.ccwu.cc/"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        val apiFallbackUrl = (project.findProperty("MOODY_API_FALLBACK_URL") as? String) ?: ""
        buildConfigField("String", "API_FALLBACK_URL", "\"$apiFallbackUrl\"")

        val apiLifelineUrl = (project.findProperty("MOODY_API_LIFELINE_URL") as? String) ?: ""
        buildConfigField("String", "API_LIFELINE_URL", "\"$apiLifelineUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
    }
}

dependencies {
    // ========== API 依赖（暴露给 app 模块） ==========

    // AndroidX Lifecycle - BaseViewModel 和 BaseActivity 需要
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.livedata.ktx)

    // Kotlin Coroutines - BaseViewModel 的 request() 方法需要
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    // 网络库 - API 层需要
    api(libs.retrofit)
    api(libs.retrofit.converter.gson)
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)
    api(libs.gson)

    // Room - 数据模型可能使用
    api(libs.room.runtime)
    api(libs.room.ktx)
    ksp(libs.room.compiler)

    // 图片加载 - Glide & Coil
    api(libs.glide)
    api(libs.coil.compose)
    api(libs.coil.gif)

    // EventBus - 事件总线
    api(libs.eventbus)

    // RecyclerView (BaseRecyclerViewAdapterHelper)
    api(libs.base.adapter.helper)

    // UI Widget - BlurView (真机实时动态背景毛玻璃效果)
    api(libs.blurview)




    // ========== Implementation 依赖（仅内部使用） ==========

    // AndroidX 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Material & UI - DataBinding 需要
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
