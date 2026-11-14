package com.edgecos.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android 侧 Ktor 客户端（OkHttp 引擎）。
 *
 * 仅共享层 Ktor 客户端使用本实现；Android App 生产链路仍走 Retrofit +
 * `RetrofitEdgeApi`（见 `:data` 模块），这里不替换现有网络栈。
 *
 * OkHttp 默认即开启连接失败重试与 302 跟随，这里显式声明以对齐 Android
 * `EdgeHttpClient` 的行为（下载接口依赖 followRedirects）。
 */
actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    installEdgeDefaults()
    engine {
        config {
            retryOnConnectionFailure(true)
            followRedirects(true)
        }
    }
}
