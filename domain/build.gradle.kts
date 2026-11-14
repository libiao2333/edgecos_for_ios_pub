import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.edgecos.mobileapp.domain"
        compileSdk = 37
        minSdk = 26
    }

    // JVM 目标：供 :composeApp 的 desktop 目标（Windows 本地预览）复用同一份领域模型。
    jvm()

    // iOS 目标：真机（iosArm64）与 Apple Silicon 模拟器（iosSimulatorArm64），
    // 产物统一打包为 DomainKit.framework。Apple 目标只能在 macOS 上编译，
    // 因此由 GitHub Actions 的 macOS 任务执行 linkDebugFrameworkIosSimulatorArm64。
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DomainKit"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// 与 :data/:app 的 compileOptions（Java 11）保持一致，
// 否则 commonMain 的 inline 函数会因 JVM target 21 > 11 而无法被消费方内联。
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}
