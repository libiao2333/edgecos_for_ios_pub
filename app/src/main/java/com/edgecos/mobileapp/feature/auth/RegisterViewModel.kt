package com.edgecos.mobileapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.shared.dto.CaptchaData
import com.edgecos.mobileapp.domain.error.EdgeError
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val username: String = "",
    val email: String = "",
    val verifyCode: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val codeSent: Boolean = false,
    val resendCountdown: Int = 0,
    val error: String? = null,
    val registerSuccess: Boolean = false,
    /** 是否正在等待用户完成人机验证 */
    val captchaPending: Boolean = false,
)

/**
 * Captcha 请求类型：标识当前 Captcha 弹窗是为哪个业务触发。
 * @property scene 提交给 /captcha/precheck 的场景标识
 */
sealed class CaptchaRequest {
    abstract val scene: String
    data class SendCode(val email: String) : CaptchaRequest() {
        override val scene: String = "register-send-code"
    }
    data class Register(
        val username: String,
        val email: String,
        val verifyCode: String,
        val password: String,
    ) : CaptchaRequest() {
        override val scene: String = "register"
    }
}

class RegisterViewModel : ViewModel() {
    private val authRepo = ServiceLocator.authRepository

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    /** 当前等待 Captcha 的业务请求；非 null 时 UI 应显示 CaptchaDialog */
    private val _captchaRequest = MutableStateFlow<CaptchaRequest?>(null)
    val captchaRequest: StateFlow<CaptchaRequest?> = _captchaRequest.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, error = null) }
    }

    fun onVerifyCodeChange(value: String) {
        _state.update { it.copy(verifyCode = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _state.update { it.copy(confirmPassword = value, error = null) }
    }

    fun togglePasswordVisible() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    /**
     * 发送邮箱验证码：先做本地校验，再触发 Captcha 弹窗。
     * Captcha 完成后由 [onCaptchaResult] 继续执行。
     */
    fun sendEmailCode() {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.update { it.copy(error = "请输入邮箱") }
            return
        }
        if (!current.email.contains("@")) {
            _state.update { it.copy(error = "邮箱格式不正确") }
            return
        }
        if (current.isSendingCode || current.captchaPending) return

        // 先调预检拿 token，再弹 Captcha
        requestCaptcha(CaptchaRequest.SendCode(current.email.trim().lowercase()))
    }

    /**
     * 注册：先做本地校验，再触发 Captcha 弹窗。
     * Captcha 完成后由 [onCaptchaResult] 继续执行。
     */
    fun register() {
        val current = _state.value
        if (current.username.isBlank()) {
            _state.update { it.copy(error = "请输入用户名") }
            return
        }
        if (current.email.isBlank() || !current.email.contains("@")) {
            _state.update { it.copy(error = "邮箱格式不正确") }
            return
        }
        if (current.verifyCode.isBlank()) {
            _state.update { it.copy(error = "请输入验证码") }
            return
        }
        if (current.password.length < 8) {
            _state.update { it.copy(error = "密码至少 8 位") }
            return
        }
        if (current.password != current.confirmPassword) {
            _state.update { it.copy(error = "两次密码输入不一致") }
            return
        }
        if (current.isLoading || current.captchaPending) return

        requestCaptcha(
            CaptchaRequest.Register(
                username = current.username.trim(),
                email = current.email.trim().lowercase(),
                verifyCode = current.verifyCode.trim(),
                password = current.password,
            )
        )
    }

    /**
     * 触发 Captcha：先调预检拿 token，成功后设置 captchaRequest 让 UI 显示弹窗。
     */
    private fun requestCaptcha(request: CaptchaRequest) {
        _state.update { it.copy(captchaPending = true, error = null) }
        viewModelScope.launch {
            when (val r = authRepo.captchaPrecheck(request.scene)) {
                is EdgeResult.Success -> {
                    pendingPrecheckToken = r.data
                    _captchaRequest.value = request
                }
                is EdgeResult.Failure -> {
                    _state.update {
                        it.copy(
                            captchaPending = false,
                            error = (r.error as? EdgeError)?.message ?: "人机验证预检失败，请稍后重试",
                        )
                    }
                }
            }
        }
    }

    /** 当前 Captcha 流程的预检 token（内存态，弹窗期间有效） */
    private var pendingPrecheckToken: String? = null

    /**
     * Captcha 弹窗结果回调：由 UI 调用。
     * - 成功：组装 CaptchaData，继续执行原请求
     * - 失败/取消：清空状态，提示用户
     */
    fun onCaptchaResult(success: Boolean, ticket: String?, randstr: String?) {
        val request = _captchaRequest.value
        val token = pendingPrecheckToken
        _captchaRequest.value = null
        pendingPrecheckToken = null

        if (!success || ticket == null || randstr == null || token == null || request == null) {
            _state.update {
                it.copy(captchaPending = false, error = if (!success) "人机验证未完成" else it.error)
            }
            return
        }

        val captchaData = CaptchaData(ticket = ticket, randstr = randstr)
        when (request) {
            is CaptchaRequest.SendCode -> executeSendCode(request.email, captchaData, token)
            is CaptchaRequest.Register -> executeRegister(request, captchaData, token)
        }
    }

    /** 用户主动取消 Captcha 弹窗 */
    fun onCaptchaDismiss() {
        _captchaRequest.value = null
        pendingPrecheckToken = null
        _state.update { it.copy(captchaPending = false) }
    }

    private fun executeSendCode(email: String, captchaData: CaptchaData, token: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSendingCode = true, error = null) }
            when (val r = authRepo.sendEmailCode(email, captchaData, token)) {
                is EdgeResult.Success -> {
                    _state.update {
                        it.copy(
                            isSendingCode = false,
                            captchaPending = false,
                            codeSent = true,
                            resendCountdown = 60,
                        )
                    }
                    startCountdown()
                }
                is EdgeResult.Failure -> _state.update {
                    it.copy(
                        isSendingCode = false,
                        captchaPending = false,
                        error = (r.error as? EdgeError)?.message ?: "验证码发送失败",
                    )
                }
            }
        }
    }

    private fun executeRegister(
        request: CaptchaRequest.Register,
        captchaData: CaptchaData,
        token: String,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val r = authRepo.register(
                username = request.username,
                email = request.email,
                verifyCode = request.verifyCode,
                password = request.password,
                captchaData = captchaData,
                captchaPrecheckToken = token,
            )) {
                is EdgeResult.Success -> _state.update {
                    it.copy(isLoading = false, captchaPending = false, registerSuccess = true)
                }
                is EdgeResult.Failure -> _state.update {
                    it.copy(
                        isLoading = false,
                        captchaPending = false,
                        error = (r.error as? EdgeError)?.message ?: "注册失败",
                    )
                }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (_state.value.resendCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                _state.update { it.copy(resendCountdown = it.resendCountdown - 1) }
            }
        }
    }
}
