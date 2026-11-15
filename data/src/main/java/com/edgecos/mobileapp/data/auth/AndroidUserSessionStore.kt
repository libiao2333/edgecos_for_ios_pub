package com.edgecos.mobileapp.data.auth

import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import com.edgecos.mobileapp.data.remote.UserStore
import com.edgecos.mobileapp.shared.dto.UserDto

/**
 * Android 端会话存储实现。
 *
 * - 用户信息：[UserStore]（EncryptedSharedPreferences，Keystore/ROM 异常时回退普通 SharedPreferences）
 * - 鉴权 Cookie：[EdgeHttpClient] 的 `EncryptedCookieJar`（加密落盘，退出登录时清空）
 * - 会话过期检查：[AuthWatcher] 解析 JWT payload 的 `exp`
 */
class AndroidUserSessionStore : UserSessionStore {

    override fun loadUser(): UserDto? = UserStore.get()

    override fun saveUser(user: UserDto) = UserStore.save(user)

    override fun clear() {
        UserStore.clear()
        EdgeHttpClient.clearAuth()
        AuthWatcher.reset()
    }

    override fun checkExpiry() = AuthWatcher.checkExpiry()
}
