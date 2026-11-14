package com.edgecos.mobileapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * 鉴权与请求头拦截器。
 *
 * - Cookie 由 [EdgeHttpClient] 的 CookieJar 自动注入（无需手动加 Cookie 头）。
 * - 注入 `X-Trace-Id`：UUID（无连字符），便于后端日志联调。
 * - 注入 `User-Agent: EdgeCOS-Android/<version> (<model>; Android <api>)`。
 * - `X-Idempotency-Key`：支付/钱包请求由调用方通过 header `X-Idempotency-Key` 自行设置，
 *   本拦截器不覆盖。
 */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Trace-Id", UUID.randomUUID().toString().replace("-", ""))
            .header("User-Agent", ClientConfig.userAgent)
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
