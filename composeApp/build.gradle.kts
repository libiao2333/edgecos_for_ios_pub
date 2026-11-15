import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // Windows 本地预览目标：./gradlew :composeApp:run 直接启动桌面窗口
    jvm("desktop")

    // iOS 目标：产出 ComposeApp.framework，由 iosApp 的 SwiftUI 壳嵌入手
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            // 静态 framework：其依赖的 SharedKit/DomainKit 符号在最终 App 链接时解析，
            // 由 iosApp 的 OTHER_LDFLAGS 一起链接，避免在 framework 内重复打包共享代码。
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // 复用现有 KMP 数据层（KtorEdgeApi / DTO / EdgeError）
            implementation(project(":shared"))
            implementation(project(":domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.edgecos.mobileapp.compose.MainKt"
    }
}

// 与 :shared/:domain 的 Java 11 保持一致
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
}
