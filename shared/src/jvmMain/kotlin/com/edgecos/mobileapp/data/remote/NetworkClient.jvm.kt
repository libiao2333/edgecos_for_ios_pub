package com.edgecos.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * JVM（desktop 预览）侧 Ktor 客户端，使用 OkHttp 引擎，
 * 与 Android 实现保持一致的重试与重定向行为。
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
