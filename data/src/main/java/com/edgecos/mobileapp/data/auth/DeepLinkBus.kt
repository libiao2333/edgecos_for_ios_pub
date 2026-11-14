package com.edgecos.mobileapp.data.auth

import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 深度链接事件总线：MainActivity 在 onCreate / onNewIntent 中解析 ACTION_VIEW Intent，
 * 将 Uri 推送到此总线；RootNav 收集事件并导航到对应页面。
 *
 * 用 SharedFlow（replay=0）：避免配置变更后重复消费旧事件。
 */
object DeepLinkBus {
    private val _events = MutableSharedFlow<Uri>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<Uri> = _events.asSharedFlow()

    suspend fun emit(uri: Uri) {
        _events.emit(uri)
    }
}
