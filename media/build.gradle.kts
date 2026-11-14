plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.edgecos.mobileapp.media"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":design"))

    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 图片加载
    implementation(libs.coil.compose)

    // 网络
    implementation(libs.okhttp)

    // 视频播放
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource.okhttp)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // AndroidDocViewer — 办公文档预览（doc/docx/xls/xlsx/ppt/pptx/pdf）
    // 该模块位于仓库之外，仅在本地存在时引入；CI 不含该目录
    findProject(":docviewer")?.let { implementation(project(":docviewer")) }

    // Fragment / Activity（用于内嵌 DocViewerFragment）
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
}
