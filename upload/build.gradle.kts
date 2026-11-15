plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.edgecos.mobileapp.upload"
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
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    // 网络（直传 OSS）
    implementation(libs.okhttp)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // Okio 流式 IO
    implementation(libs.okio)

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
