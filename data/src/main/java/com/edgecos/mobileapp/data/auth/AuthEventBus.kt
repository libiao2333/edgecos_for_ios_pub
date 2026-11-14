package com.edgecos.mobileapp.data.auth

import com.edgecos.mobileapp.domain.error.EdgeError
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 鉴权事件总线：用 SharedFlow 广播 [EdgeError.AuthExpired] 给 UI 层（RootNav），
 * UI 监听后清 user 并跳登录页。
 */
object AuthEventBus {
    private val _events = MutableSharedFlow<EdgeError>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<EdgeError> = _events.asSharedFlow()

    fun broadcast(error: EdgeError) {
        _events.tryEmit(error)
    }
}
