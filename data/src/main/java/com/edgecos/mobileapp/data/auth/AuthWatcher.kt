package com.edgecos.mobileapp.data.auth

import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Base64

/**
 * JWT 滑动续期观察器。
 *
 * 后端鉴权中间件会在 JWT 距过期 ≤ 24h 时自动重签 Cookie（`Set-Cookie: token=...`）。
 * OkHttp 的 CookieJar 会自动接收新 Cookie 并持久化，无需客户端主动调用续期接口。
 *
 * 本观察器的职责：
 * - 解析当前 token 的 `exp`，计算剩余有效期
 * - 当剩余 ≤ 24h 时标记 `needsRefresh=true`，下次任意带鉴权请求触发后端自动续期
 * - 提供 [expiringSoon] StateFlow 给 UI 层展示"会话即将过期"提示
 *
 * 实现说明：仅做 Base64URL 解码 + 正则提取 `exp`，不引入 JJWT 等 JWT 库
 * （SECRET_KEY 在服务端，客户端不验签，只读 payload）。
 */
object AuthWatcher {

    private val _expiringSoon = MutableStateFlow(false)
    val expiringSoon: StateFlow<Boolean> = _expiringSoon

    /** 阈值：距过期 ≤ 24h 标记为即将过期 */
    private val THRESHOLD_SECONDS = 86400L

    /** 匹配 JWT payload JSON 中的秒级 exp，如 `"exp":1735689600` */
    private val EXP_REGEX = Regex("\"exp\"\\s*:\\s*(\\d+)")

    /**
     * 检查当前 token 是否即将过期。
     * 应在每次成功响应后调用（由 Repository 层触发）。
     */
    fun checkExpiry() {
        val exp = parseExpSeconds()
        _expiringSoon.value = exp != null && (exp - nowSeconds()) <= THRESHOLD_SECONDS
    }

    /** 计算剩余有效期（秒），失败返回 null */
    fun remainingSeconds(): Long? {
        val exp = parseExpSeconds() ?: return null
        return exp - nowSeconds()
    }

    /** 退出登录时重置 */
    fun reset() {
        _expiringSoon.value = false
    }

    /**
     * 从当前 token 的 JWT payload 段解析秒级 exp。
     * 无 token / 结构不是三段 JWT / Base64URL 解码失败 / 无 exp 字段时返回 null。
     */
    private fun parseExpSeconds(): Long? {
        val token = EdgeHttpClient.cookieJar.getToken() ?: return null
        val parts = token.split('.')
        if (parts.size < 2) return null
        val payload = try {
            String(decodeBase64Url(parts[1]), Charsets.UTF_8)
        } catch (_: Throwable) {
            return null
        }
        return EXP_REGEX.find(payload)?.groupValues?.get(1)?.toLongOrNull()
    }

    /** Base64URL 解码：先按需补 '=' 到 4 的倍数 */
    private fun decodeBase64Url(input: String): ByteArray {
        val padded = when (input.length % 4) {
            2 -> "$input=="
            3 -> "$input="
            else -> input
        }
        return Base64.getUrlDecoder().decode(padded)
    }

    private fun nowSeconds(): Long = System.currentTimeMillis() / 1000
}
