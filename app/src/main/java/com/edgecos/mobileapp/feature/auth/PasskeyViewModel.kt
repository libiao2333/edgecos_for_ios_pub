package com.edgecos.mobileapp.feature.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.shared.dto.PasskeyAssertionData
import com.edgecos.mobileapp.shared.dto.PasskeyAssertionResponse
import com.edgecos.mobileapp.shared.dto.PasskeyCredential
import com.edgecos.mobileapp.shared.dto.PasskeyLoginVerifyRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasskeyUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val loginSuccess: Boolean = false,
)

class PasskeyViewModel : ViewModel() {
    private val passkeyRepo = ServiceLocator.passkeyRepository
    private val authRepo = ServiceLocator.authRepository

    private val _state = MutableStateFlow(PasskeyUiState())
    val state: StateFlow<PasskeyUiState> = _state.asStateFlow()

    /**
     * 发起 Passkey 登录。
     *
     * 流程：
     * 1. 从服务器获取登录 options（challenge、rpId、allowCredentials）
     * 2. 调用 Android Credential Manager 弹出系统 Passkey 选择器
     * 3. 用户完成指纹/面容验证
     * 4. 将签名结果发送给服务器验证
     * 5. 服务器通过 Set-Cookie 下发 token
     */
    fun startLogin(context: android.content.Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Step 1: 获取登录 options
                val optionsResult = passkeyRepo.getLoginOptions()
                if (optionsResult is EdgeResult.Failure) {
                    _state.update { it.copy(isLoading = false, error = optionsResult.error.message) }
                    return@launch
                }
                val options = (optionsResult as EdgeResult.Success).data

                // Step 2: 构建 Credential Manager 请求
                val credentialManager = CredentialManager.create(context)

                val credOption = GetPublicKeyCredentialOption(
                    requestJson = buildGetCredentialRequestJson(
                        challenge = options.options.challenge,
                        rpId = options.options.rpId,
                        allowCredentials = options.options.allowCredentials,
                        timeout = options.options.timeout,
                    ),
                    clientDataHash = null,
                )

                val request = GetCredentialRequest(listOf(credOption))

                // Step 3: 弹出系统 Passkey UI
                val result = credentialManager.getCredential(
                    context = context,
                    request = request,
                )

                val credential = result.credential as? PublicKeyCredential
                if (credential == null) {
                    _state.update { it.copy(isLoading = false, error = "未能获取凭据") }
                    return@launch
                }

                // Step 4: 提取认证响应数据
                val authResponse = credential.authenticationResponseJson
                    ?: run {
                        _state.update { it.copy(isLoading = false, error = "认证响应为空") }
                        return@launch
                    }

                // 解析 JSON 响应（提取 credential id 和 assertion 数据）
                val credentialId = extractJsonStringField(authResponse, "id")
                    ?: run {
                        _state.update { it.copy(isLoading = false, error = "无法提取凭据 ID") }
                        return@launch
                    }

                val assertionData = parseAssertionResponse(authResponse)
                    ?: run {
                        _state.update { it.copy(isLoading = false, error = "解析认证响应失败") }
                        return@launch
                    }

                // Step 5: 发送到服务器验证
                val verifyResult = passkeyRepo.verifyLogin(
                    PasskeyLoginVerifyRequest(
                        transactionId = options.transactionId,
                        response = PasskeyAssertionResponse(
                            id = credentialId,
                            rawId = credentialId,
                            response = assertionData,
                        ),
                    ),
                )

                when (verifyResult) {
                    is EdgeResult.Success -> {
                        // 登录成功，Cookie 已由 EncryptedCookieJar 自动持久化
                        // 恢复用户信息
                        authRepo.restoreUser()
                        _state.update { it.copy(isLoading = false, loginSuccess = true) }
                    }
                    is EdgeResult.Failure -> {
                        _state.update { it.copy(isLoading = false, error = verifyResult.error.message) }
                    }
                }
            } catch (e: GetCredentialException) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = when {
                            e.message?.contains("cancel") == true -> null
                            else -> "Passkey 登录取消或失败: ${e.message}"
                        },
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "登录失败: ${e.message}") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        /**
         * 构建 GetPublicKeyCredentialOption 的 requestJson。
         * 格式：WebAuthn PublicKeyCredentialRequestOptions。
         */
        private fun buildGetCredentialRequestJson(
            challenge: String,
            rpId: String,
            allowCredentials: List<PasskeyCredential>,
            timeout: Long,
        ): String {
            val allowCredentialsJson = if (allowCredentials.isEmpty()) "" else {
                val items = allowCredentials.joinToString(",") { cred ->
                    """{"id":"${cred.id}","type":"${cred.type}"}"""
                }
                ""","allowCredentials":[$items]"""
            }

            return """
                {
                    "challenge":"$challenge",
                    "rpId":"$rpId",
                    "timeout":$timeout,
                    "userVerification":"required"
                    $allowCredentialsJson
                }
            """.trimIndent()
        }

        /**
         * 简易 JSON 解析：提取顶层字符串字段。
         */
        private fun extractJsonStringField(json: String, key: String): String? {
            val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
            return regex.find(json)?.groupValues?.getOrNull(1)
        }

        /**
         * 简易 JSON 解析：从 Credential Manager 返回的 authenticationResponseJson 中提取字段。
         * 
         * 注意：Credential Manager 返回的是 JSONObject 的 toString()，
         * 此处用简易正则提取，避免引入 Gson/Moshi 重复依赖。
         */
        private fun parseAssertionResponse(json: String): PasskeyAssertionData? {
            return try {
                fun extract(key: String): String? {
                    val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
                    return regex.find(json)?.groupValues?.getOrNull(1)
                }
                val authenticatorData = extract("authenticatorData") ?: return null
                val clientDataJSON = extract("clientDataJSON") ?: return null
                val signature = extract("signature") ?: return null
                val userHandle = extract("userHandle")

                PasskeyAssertionData(
                    authenticatorData = authenticatorData,
                    clientDataJSON = clientDataJSON,
                    signature = signature,
                    userHandle = userHandle,
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
