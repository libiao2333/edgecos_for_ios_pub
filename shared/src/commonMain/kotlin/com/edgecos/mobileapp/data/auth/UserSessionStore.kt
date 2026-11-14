package com.edgecos.mobileapp.data.auth

import com.edgecos.mobileapp.shared.dto.UserDto

/**
 * 当前用户会话持久化抽象（KMP commonMain）。
 *
 * 把「用户信息落盘 + 会话过期检查」从平台实现中抽离，让 [com.edgecos.mobileapp.data.repository.AuthRepository]
 * 可以下沉到 shared 层：
 * - Android 实现（`data` 模块的 `AndroidUserSessionStore`）：用户信息用 EncryptedSharedPreferences
 *   （`UserStore`，Keystore 异常时回退普通 SharedPreferences），鉴权 Cookie 由 OkHttp
 *   `EncryptedCookieJar` 持久化，`AuthWatcher` 解析 JWT exp 判断过期。
 * - iOS 实现（后续）：用户信息用 Keychain，鉴权 Cookie 由 Ktor/NSURLSession 的持久化存储承载。
 */
interface UserSessionStore {

    /** 读取本地缓存的当前用户，未登录返回 null。 */
    fun loadUser(): UserDto?

    /** 保存当前用户（登录/注册成功后调用）。 */
    fun saveUser(user: UserDto)

    /** 清除本地会话（用户信息 + 鉴权 Cookie 状态）。 */
    fun clear()

    /** 检查当前 token 是否即将过期（供 UI 展示「会话即将过期」提示）。 */
    fun checkExpiry()
}
