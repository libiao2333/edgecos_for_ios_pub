package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.OAuthSessionRequest
import com.edgecos.mobileapp.shared.dto.OAuthSessionResponse
import com.edgecos.mobileapp.shared.dto.OAuthQrStatusResponse
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge

/**
 * OAuth Repository：第三方登录（七相聚合 OAuth，支持 qq/wx/alipay/dingtalk/douyin/gitee/github/microsoft）。
 *
 * 移动端优先用二维码扫码 + 轮询状态模式：
 * 1. 调用 [getWxSession] 获取 state + 二维码
 * 2. 展示二维码给用户扫码
 * 3. 轮询 [getQrStatus] 直到 status 变为 success
 * 4. 调用 [completeLogin] 获取 token cookie
 *
 * 对于支持 CustomTabs 跳转的 provider（github/gitee/microsoft）：
 * 直接打开 Web 端 OAuth URL，通过 deep link 回调。
 */
class OAuthRepository(
    private val api: EdgeApi,
) {

    suspend fun getWxSession(redirect: String = "/"): EdgeResult<OAuthSessionResponse> = runCatchingEdge {
        api.getOAuthWxSession(OAuthSessionRequest(redirect))
    }

    suspend fun getQrStatus(state: String): EdgeResult<OAuthQrStatusResponse> = runCatchingEdge {
        api.getOAuthQrStatus(state)
    }

    suspend fun completeLogin(state: String): EdgeResult<Unit> = runCatchingEdge {
        api.completeOAuthLogin(state)
    }
}
