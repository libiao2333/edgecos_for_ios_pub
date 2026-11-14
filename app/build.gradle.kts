plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.edgecos.mobileapp"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.edgecos.mobileapp"
        minSdk = 26
        targetSdk = 37
        versionCode = 12
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 网络配置（对齐前端 vite proxy target）
        // 注意：Retrofit baseUrl 必须以 / 结尾
        buildConfigField("String", "BASE_URL", "\"https://user.edgecos.com/api/\"")
        buildConfigField("String", "WEB_BASE_URL", "\"https://user.edgecos.com\"")

        // 腾讯云 Captcha AppId（与前端 VITE_TENCENT_CAPTCHA_APP_ID 一致）
        buildConfigField("String", "TENCENT_CAPTCHA_APP_ID", "\"197733334\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // 项目内模块
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":design"))
    implementation(project(":upload"))
    implementation(project(":media"))

    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    // Fragment（用于内嵌 DocViewerFragment 预览文档）
    implementation(libs.androidx.fragment.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // Coil（图片加载，由 :media 也使用）
    implementation(libs.coil.compose)

    // AndroidDocViewer — 办公文档预览（doc/docx/xls/xlsx/ppt/pptx/pdf）
    // 该模块位于仓库之外，仅在本地存在时引入；CI（只构建 domain 的 iOS framework）不含该目录
    findProject(":docviewer")?.let { implementation(project(":docviewer")) }

    // Passkey — Credential Manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)

    // OAuth — CustomTabs
    implementation(libs.androidx.browser)

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}