plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.edgecos.mobileapp.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    // DTO 已下沉到 KMP 的 :shared，用 api 暴露给 :upload/:app 等消费方
    api(project(":shared"))

    // 网络
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.security.crypto)

    // 数据库
    // room-runtime 用 api 暴露：app 模块引用 TransferRecordEntity / ServiceLocator.database（EdgeDatabase
    // 继承 RoomDatabase）时需要解析 Room 父类，implementation 不传递会导致 app 编译失败
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 协程
    implementation(libs.kotlinx.coroutines.android)

    // 生命周期
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // 测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
}
