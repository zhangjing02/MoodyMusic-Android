plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

import java.util.Properties
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.moodymusicforandroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moodyimusic.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 18
        versionName = "1.0.18"


        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        manifestPlaceholders["JPUSH_PKGNAME"] = "com.moodyimusic.app"
        manifestPlaceholders["JPUSH_APPKEY"] = "0c279e2a4de3471067c84370"
        manifestPlaceholders["JPUSH_CHANNEL"] = "developer-default"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String?
                keyPassword = keystoreProperties["keyPassword"] as String?
                storeFile = keystoreProperties["storeFile"]?.let { file(it) }
                storePassword = keystoreProperties["storePassword"] as String?
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        dataBinding = true
        viewBinding = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm").format(Date())
            val appName = "音信"
            val env = variant.buildType.name
            val vName = variant.versionName
            val vCode = variant.versionCode
            output?.outputFileName = "${appName}_${env}_v${vName}_build${vCode}_${timeStamp}.apk"
        }
    }
}

dependencies {
    // CommonBase module (provides base classes, network, API services, data models, and image loading)
    implementation(project(":commonbase"))

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Compose & Navigation 3
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation("androidx.compose.runtime:runtime-livedata")
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)

    // Material & UI
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)

    // UI Widget Helper - BlurView & Haze (Compose Frosted Glass)
    implementation(libs.blurview)
    implementation(libs.rwidget.helper)
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation("androidx.palette:palette-ktx:1.0.0")

    // JPush
    implementation(libs.jpush)

    // Media (MediaStyle notification + MediaSessionCompat)
    implementation(libs.androidx.media)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}