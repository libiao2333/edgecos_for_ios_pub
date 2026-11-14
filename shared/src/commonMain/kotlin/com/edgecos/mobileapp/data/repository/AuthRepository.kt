package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.auth.UserSessionStore
import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.CaptchaData
import com.edgecos.mobileapp.shared.dto.CaptchaPrecheckRequest
import com.edgecos.mobileapp.shared.dto.LoginRequest
import com.edgecos.mobileapp.shared.dto.LoginResponse
import com.edgecos.mobileapp.shared.dto.OAuthAccountDto
import com.edgecos.mobileapp.shared.dto.RegisterRequest
import com.edgecos.mobileapp.shared.dto.SendEmailCodeRequest
import com.edgecos.mobileapp.shared.dto.UserDto
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.OAuthAccount
import com.edgecos.mobileapp.domain.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 鉴权 Repository：负责登录、注册、用户状态管理。
 *
 * 平台差异（用户信息持久化、Cookie、JWT 过期检查）通过 [UserSessionStore] 注入，
 * 本类只依赖 commonMain 的 [EdgeApi]，不再直接依赖 Android 实现。
 *
 * Captcha 流程（与前端 tencentCaptcha.js + 后端 middleware/captcha.js 对齐）：
 * 1. 调 [captchaPrecheck] 拿 precheckToken（5min TTL）
 * 2. 客户端弹腾讯 Captcha SDK，拿到 ticket / randstr
 * 3. 组装 [CaptchaData]，连同 precheckToken 一起调 [sendEmailCode] / [register]
 */
class AuthRepository(
    private val api: EdgeApi,
    private val sessionStore: UserSessionStore,
) {

    private val _currentUser = MutableStateFlow<User?>(sessionStore.loadUser()?.toDomain())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** 登录（无需 Captcha） */
    suspend fun login(username: String, password: String): EdgeResult<User> = runCatchingEdge {
        val resp = api.login(LoginRequest(username, password))
        handleLoginSuccess(resp)
    }

    /**
     * Captcha 预检：拿 precheckToken。
     * @param scene 业务场景，如 "register-send-code" / "register" / "realname-start"
     */
    suspend fun captchaPrecheck(scene: String): EdgeResult<String> = runCatchingEdge {
        api.captchaPrecheck(CaptchaPrecheckRequest(scene)).token
    }

    /** 发送邮箱验证码（需 Captcha） */
    suspend fun sendEmailCode(
        email: String,
        captchaData: CaptchaData,
        captchaPrecheckToken: String,
    ): EdgeResult<Unit> = runCatchingEdge {
        api.sendEmailCode(SendEmailCodeRequest(email, captchaData, captchaPrecheckToken))
        Unit
    }

    /** 注册（需 Captcha） */
    suspend fun register(
        username: String,
        email: String,
        verifyCode: String,
        password: String,
        captchaData: CaptchaData,
        captchaPrecheckToken: String,
    ): EdgeResult<User> = runCatchingEdge {
        val resp = api.register(
            RegisterRequest(
                username = username,
                email = email,
                verifyCode = verifyCode,
                password = password,
                captchaData = captchaData,
                captchaPrecheckToken = captchaPrecheckToken,
            )
        )
        handleLoginSuccess(resp)
    }

    /** 启动时从本地恢复用户 */
    fun restoreUser(): User? {
        val user = sessionStore.loadUser()?.toDomain()
        _currentUser.value = user
        if (user != null) sessionStore.checkExpiry()
        return user
    }

    /** 退出登录 */
    fun logout() {
        sessionStore.clear()
        _currentUser.value = null
    }

    private fun handleLoginSuccess(resp: LoginResponse): User {
        val user = resp.user.toDomain()
        sessionStore.saveUser(resp.user)
        _currentUser.value = user
        sessionStore.checkExpiry()
        return user
    }
}

/** DTO → Domain 转换 */
private fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    role = role,
    email = email,
    qqNumber = qqNumber,
    qqOpenId = qqOpenId,
    qqNickname = qqNickname,
    qqAvatar = qqAvatar,
    oauthAccounts = oauthAccounts?.mapValues { it.value.toDomain() } ?: emptyMap(),
    realNameStatus = realNameStatus,
    realNameMaskedName = realNameMaskedName,
    realNameMaskedIdNo = realNameMaskedIdNo,
    realNameVerifiedAt = realNameVerifiedAt,
)

private fun OAuthAccountDto.toDomain(): OAuthAccount = OAuthAccount(
    provider = provider,
    uid = uid,
    nickname = nickname,
    avatar = avatar,
    gender = gender,
    location = location,
    ip = ip,
    accessToken = accessToken,
    updatedAt = updatedAt,
)
