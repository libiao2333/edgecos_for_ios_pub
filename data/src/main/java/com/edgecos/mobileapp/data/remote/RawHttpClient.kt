package com.edgecos.mobileapp.data.remote

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * OSS 直传专用 HTTP 客户端。
 *
 * - 无 CookieJar（与前端 `rawApi = axios.create({ withCredentials: false })` 对齐）
 * - 无 [AuthInterceptor]（避免大文件携带无用 Cookie）
 * - `followRedirects(true)`（OSS 端可能 302）
 * - 写超时 5min（大文件分片上传）
 */
object RawHttpClient {

    @Volatile
    private var client: OkHttpClient? = null

    fun get(): OkHttpClient {
        client?.let { return it }
        synchronized(this) {
            client?.let { return it }
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
                .also { client = it }
        }
        return client!!
    }
}
