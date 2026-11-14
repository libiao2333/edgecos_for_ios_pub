package com.edgecos.mobileapp.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * EdgeCOS 主 HTTP 客户端单例。
 *
 * - 持久化 CookieJar：[EncryptedCookieJar]（token Cookie 加密落盘）
 * - 超时：connect 15s、read 30s、write 60s
 * - 启用 `retryOnConnectionFailure`、`followRedirects`
 * - 拦截器链：[AuthInterceptor] → [ResponseInterceptor]
 *
 * 由 ServiceLocator 在 App 启动时调用 [init] 注入 Context。
 */
object EdgeHttpClient {

    @Volatile
    private var client: OkHttpClient? = null

    @Volatile
    lateinit var cookieJar: EncryptedCookieJar
        private set

    fun init(context: Context, debug: Boolean = false) {
        if (client != null) return
        synchronized(this) {
            if (client != null) return
            val appCtx = context.applicationContext
            cookieJar = EncryptedCookieJar(appCtx)

            // release 包关闭 HTTP 日志，避免 URL/查询参数泄露到 Logcat
            // debug 包用 BASIC 级别（仅方法+URL+状态码，不打 headers/body）
            val logging = HttpLoggingInterceptor().apply {
                level = if (debug) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            }

            client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor(AuthInterceptor())
                .addInterceptor(ResponseInterceptor())
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    fun get(): OkHttpClient = client ?: error("EdgeHttpClient 未初始化，请先调用 init(context)")

    /** 退出登录时调用 */
    fun clearAuth() {
        cookieJar.clear()
    }
}
