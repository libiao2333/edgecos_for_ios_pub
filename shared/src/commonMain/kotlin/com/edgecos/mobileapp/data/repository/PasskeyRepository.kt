package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.PasskeyLoginOptionsRequest
import com.edgecos.mobileapp.shared.dto.PasskeyLoginOptionsResponse
import com.edgecos.mobileapp.shared.dto.PasskeyLoginVerifyRequest
import com.edgecos.mobileapp.shared.dto.PasskeyRegisterOptionsResponse
import com.edgecos.mobileapp.shared.dto.PasskeyRegisterVerifyRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge

/**
 * Passkey Repository：WebAuthn 注册 / 登录 / 管理。
 */
class PasskeyRepository(
    private val api: EdgeApi,
) {

    suspend fun getRegisterOptions(): EdgeResult<PasskeyRegisterOptionsResponse> = runCatchingEdge {
        api.getPasskeyRegisterOptions()
    }

    suspend fun verifyRegister(request: PasskeyRegisterVerifyRequest): EdgeResult<Unit> = runCatchingEdge {
        api.verifyPasskeyRegister(request)
        // 注册成功无有意义响应体
    }

    suspend fun getLoginOptions(username: String = ""): EdgeResult<PasskeyLoginOptionsResponse> = runCatchingEdge {
        api.getPasskeyLoginOptions(PasskeyLoginOptionsRequest(username))
    }

    suspend fun verifyLogin(request: PasskeyLoginVerifyRequest): EdgeResult<Unit> = runCatchingEdge {
        api.verifyPasskeyLogin(request)
        // 登录成功后 Cookie 由 EncryptedCookieJar 自动持久化
    }
}
