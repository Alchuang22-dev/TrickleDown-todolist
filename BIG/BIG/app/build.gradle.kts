plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.big"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.big"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        // 修复：移除重复配置，添加反糖支持
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.espresso.core)
    implementation(libs.generativeai)
    // 添加反糖支持库
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Kotlin BOM
    implementation(platform(libs.kotlin.bom))

    /* ---------- AndroidX 基础 ---------- */
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    /* ---------- 协程 & Lifecycle ---------- */
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.runtime.ktx)          // ← 新增

    /* ---------- UI / 组件 ---------- */
    implementation(libs.circleimageview)
    implementation(libs.glide)                          // ← 新增
    implementation(libs.mpandroidchart)

    /* ---------- Jetpack Compose ---------- */
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)   // ✅ 新增
    implementation(libs.activity.compose)
    implementation(libs.play.services.basement)
    implementation(libs.material.icons.extended.v160)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    /* ---------- 网络请求 ---------- */
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)                         // ← 新增
    implementation(libs.okhttp.logging)

    /* ---------- ViewModel / LiveData ---------- */
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.runtime.livedata)

    /* ---------- 测试 ---------- */
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.coil.compose)
}

// 添加全局配置以强制使用Kotlin 2.1.0
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.0")
        force("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
    }
}