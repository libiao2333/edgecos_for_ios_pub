package com.edgecos.mobileapp.domain.error

/**
 * EdgeCOS 统一错误模型。
 *
 * 放在 commonMain，因此不再继承 java.io.IOException（JVM 专属）。
 * 网络层抛出的异常统一由 [com.edgecos.mobileapp.domain.error.runCatchingEdge]
 * 收敛为 EdgeError，调用方无需依赖 IOException 语义。
 *
 * 对应后端响应：
 * - 401 + `code ∈ {AUTH_REQUIRED, AUTH_EXPIRED, AUTH_INVALID}` → [AuthExpired]
 * - 403 + `suspended_overquota` → [Suspended]
 * - 403 + `REAL_NAME_REQUIRED` → [RealNameRequired]
 * - 403 + `TRAFFIC_EXHAUSTED` → [TrafficExhausted]
 * - 429 → [RateLimited]
 * - 409 + `INSTANT_MISS` → [InstantMiss]
 * - 其他业务错误 → [Business]
 */
sealed class EdgeError : Exception() {
    // 说明：commonMain 的 Throwable 没有 JVM 的 initCause，
    // 原始异常统一通过各子类的 origin 字段暴露，调用方按需读取即可。
    /**
     * 网络层错误（断网、DNS 失败、超时等）。
     *
     * [message] 取 [origin] 的 message（origin 为空或无 message 时兜底"网络错误"）——
     * UI 层普遍直接展示 error.message，若为 null 会显示 "null"，丢失真实失败原因
     * （如 "HTTP 404: Not Found"、超时详情等）。
     */
    data class Network(val origin: Throwable? = null) : EdgeError() {
        override val message: String = origin?.message ?: "网络错误"
    }

    /** 鉴权失效：JWT 过期/无效，需跳登录页 */
    data class AuthExpired(val code: String, override val message: String) : EdgeError()

    /** 账号封禁：套餐过期且容量超限 */
    data class Suspended(override val message: String) : EdgeError()

    /** 需实名认证：分享/直链操作前置 */
    object RealNameRequired : EdgeError() {
        private fun readResolve(): Any = RealNameRequired
        override val message: String = "需要实名认证后才能使用此功能"
    }

    /** 流量耗尽：下载/预览/直链生成被禁 */
    object TrafficExhausted : EdgeError() {
        private fun readResolve(): Any = TrafficExhausted
        override val message: String = "流量已耗尽，请购买流量包或升级套餐"
    }

    /** 限流：操作过于频繁 */
    data class RateLimited(val retryAfter: Long? = null) : EdgeError() {
        override val message: String = "操作过于频繁，请稍后重试"
    }

    /** 上传秒传未命中：hash 命中但对象已被删，需 fallback 重传 */
    object InstantMiss : EdgeError() {
        private fun readResolve(): Any = InstantMiss
        override val message: String = "秒传未命中，正在重试"
    }

    /** 业务错误：带后端 code 与 message */
    data class Business(val code: String, override val message: String) : EdgeError()

    /** 未知错误 */
    data class Unknown(override val message: String = "未知错误", val origin: Throwable? = null) : EdgeError()
}
