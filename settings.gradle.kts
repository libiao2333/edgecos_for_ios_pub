pluginManagement {
    repositories {
        // 阿里云镜像（优先）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 官方源（兜底，镜像未同步时回落）
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像（优先）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        // 官方源（兜底）
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "EdgeCOS"
include(":app")
include(":data")
include(":domain")
include(":shared")
// Compose Multiplatform 共享 UI：desktop 目标用于 Windows 本地预览，
// iOS 目标产出 ComposeApp.framework 供 SwiftUI 壳嵌入。
include(":composeApp")
include(":design")
include(":upload")
include(":media")
// AndroidDocViewer 作为本地模块引入（便于修改 viewer.html 和 DocViewerFragment）。
// 该目录位于本仓库之外，CI 上没有，因此仅在目录存在时才 include，
// 否则 Gradle 9 会在配置阶段直接报 "projectDirectory does not exist"。
val docViewerDir = file("../AndroidDocViewer-main/docviewer")
if (docViewerDir.isDirectory) {
    include(":docviewer")
    project(":docviewer").projectDir = docViewerDir
}
