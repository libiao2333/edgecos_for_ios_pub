package com.edgecos.mobileapp.data.remote

import com.edgecos.mobileapp.data.auth.AuthEventBus
import com.edgecos.mobileapp.data.auth.AuthRepositoryHolder
import com.edgecos.mobileapp.domain.error.EdgeError
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject

/**
 * 响应拦截器：解析后端业务错误码，映射为 [EdgeError] 异常。
 *
 * 后端响应格式：`{code: string, message: string, ...}`，HTTP 状态码与 code 联动：
 * - 401 + `AUTH_REQUIRED/AUTH_EXPIRED/AUTH_INVALID`（非 /login）→ [EdgeError.AuthExpired]，并广播 [AuthEventBus]
 * - 403 + `suspended_overquota` → [EdgeError.Suspended]
 * - 403 + `REAL_NAME_REQUIRED` → [EdgeError.RealNameRequired]
 * - 403 + `TRAFFIC_EXHAUSTED` → [EdgeError.TrafficExhausted]
 * - 429 → [EdgeError.RateLimited]
 * - 409 + `INSTANT_MISS` → [EdgeError.InstantMiss]
 * - 其他 code 非空且非 success → [EdgeError.Business]
 */
class ResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val requestPath = chain.request().url.encodedPath
        // 注意：ResponseBody 只能消费一次 —— 必须先把 body 文本读出来，
        // 再基于文本解析 code/message，否则二次 string() 会返回空串，
        // 业务错误提示（EdgeError.Business.message）会全部变成空白。
        val bodyText = runCatching { response.peekBody(1024 * 4).string() }.getOrDefault("")
        val json = runCatching { JSONObject(bodyText) }.getOrNull()
        val code: String? = json?.optString("code")?.takeIf { it.isNotEmpty() }
        val serverMessage: String? = json?.optString("message")?.takeIf { it.isNotEmpty() }

        val edgeError: EdgeError = when {
            response.code == 401 && code in setOf("AUTH_REQUIRED", "AUTH_EXPIRED", "AUTH_INVALID")
                    && !requestPath.endsWith("/login") -> {
                AuthEventBus.broadcast(EdgeError.AuthExpired(code ?: "AUTH_REQUIRED", "登录已失效"))
                AuthRepositoryHolder.clearUser()
                EdgeError.AuthExpired(code ?: "AUTH_REQUIRED", "登录已失效")
            }
            response.code == 403 && code == "suspended_overquota" ->
                EdgeError.Suspended("账号因套餐过期且容量超限已被封禁")
            response.code == 403 && code == "REAL_NAME_REQUIRED" ->
                EdgeError.RealNameRequired
            response.code == 403 && code == "TRAFFIC_EXHAUSTED" ->
                EdgeError.TrafficExhausted
            response.code == 429 -> {
                val retryAfter = response.header("Retry-After")?.toLongOrNull()
                EdgeError.RateLimited(retryAfter)
            }
            response.code == 409 && code == "INSTANT_MISS" ->
                EdgeError.InstantMiss
            code != null -> EdgeError.Business(code, serverMessage ?: "请求失败：$code")
            else -> EdgeError.Network(java.io.IOException("HTTP ${response.code}: ${response.message}"))
        }

        response.close()
        throw edgeError
    }
}
