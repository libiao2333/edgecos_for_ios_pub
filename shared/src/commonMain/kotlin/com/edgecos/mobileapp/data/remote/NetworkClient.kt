package com.edgecos.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * 共享层统一的 JSON 配置。
 *
 * - [Json.ignoreUnknownKeys]：后端新增字段不会让旧客户端解析失败；
 * - [Json.coerceInputValues]：`null` 落到非空默认值字段时使用默认值（容错对齐 Android 侧）；
 * - [Json.encodeDefaults]：请求体显式携带默认值，避免后端依赖字段缺省语义时行为不一致。
 */
internal val EdgeJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

/**
 * 为 Ktor [HttpClient] 安装 EdgeCOS 共享默认插件：
 *
 * - [ContentNegotiation]：请求/响应体按 [EdgeJson] 做 JSON 编解码；
 * - [HttpCookies]：自动携带后端下发的 httpOnly JWT Cookie；
 * - [Logging]：仅 INFO 级别（方法/URL/状态码），不打印请求体与敏感头。
 *
 * TODO(WP-cookie)：Cookie 持久化尚未实现。当前使用 Ktor 默认的内存 CookieStorage，
 * 进程重启后登录态丢失。后续由独立工作包引入 multiplatform-settings + SecureStorage
 * 作为 [HttpCookies] 的 `CookiesStorage`；Android 侧继续复用现有 EncryptedCookieJar，
 * 这里不引入任何新的存储依赖。
 *
 * 注意：这里**不开启** `expectSuccess`。非 2xx 响应由 [KtorEdgeApi] 统一读取响应体并映射为
 * `com.edgecos.mobileapp.domain.error.EdgeError`，与 Android `ResponseInterceptor` 语义保持一致。
 */
internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installEdgeDefaults() {
    install(ContentNegotiation) {
        json(EdgeJson)
    }
    install(HttpCookies)
    install(Logging) {
        level = LogLevel.INFO
    }
}

/**
 * 创建平台 HTTP 客户端。
 *
 * - Android：OkHttp 引擎（`androidMain/NetworkClient.android.kt`）
 * - iOS：Darwin 引擎，基于 NSURLSession（`iosMain/NetworkClient.ios.kt`）
 */
expect fun createPlatformHttpClient(): HttpClient
