import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.edgecos.mobileapp.shared"
        compileSdk = 37
        minSdk = 26
    }

    // JVM 目标：供 :composeApp 的 desktop 目标（Windows 本地预览）复用共享网络层。
    jvm()

    // iOS 目标：与 :domain 保持一致，产物打包为 SharedKit.framework
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedKit"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            // Repository/缓存接口的公开签名使用 Flow，需 api 传递给消费方
            api(libs.kotlinx.coroutines.core)
            // Repository 的公开签名暴露 EdgeResult / 领域模型，需 api 传递给 :data / :app
            api(project(":domain"))
            // KtorEdgeApi 公开构造参数/类型使用 HttpClient，需 api 暴露给消费方
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.serialization.json)
        }
        // Android 端仍以 Retrofit 为主链路，Ktor OkHttp 引擎供共享层/测试使用
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        // iOS 端使用 Darwin 引擎（NSURLSession）
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        // JVM 端（desktop 预览）使用 OkHttp 引擎
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
    }
}

// 与 :data/:app 的 Java 11 保持一致，避免消费方无法内联
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}
