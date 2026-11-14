package com.edgecos.mobileapp.data.auth

import com.edgecos.mobileapp.domain.model.User

/**
 * AuthRepository 全局引用持有器。
 *
 * 由于 [com.edgecos.mobileapp.data.remote.ResponseInterceptor] 在 OkHttp 拦截器链中
 * 无法直接依赖 AuthRepository（循环依赖），通过此 holder 反向引用。
 *
 * App 启动时由 ServiceLocator 调用 [bind] 注入实例。
 */
object AuthRepositoryHolder {
    @Volatile
    private var clearUserFn: (() -> Unit)? = null

    @Volatile
    private var currentUserFn: (() -> User?)? = null

    fun bind(clearUser: () -> Unit, currentUser: () -> User?) {
        clearUserFn = clearUser
        currentUserFn = currentUser
    }

    fun clearUser() {
        clearUserFn?.invoke()
    }

    fun currentUser(): User? = currentUserFn?.invoke()
}
