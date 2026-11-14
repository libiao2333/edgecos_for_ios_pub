package com.edgecos.mobileapp.upload

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 上传事件总线：广播上传完成/失败事件给 UI 层（FilesViewModel 监听后刷新列表）。
 *
 * 解耦 [UploadService] 与 UI：Service 完成上传后通过 [broadcast] 发送事件，
 * ViewModel 通过 [events] 收集并触发刷新。
 */
object UploadEventBus {

    sealed class Event {
        /** 上传成功，[path] 为目标目录（用于精准刷新），[filename] 为文件名 */
        data class Success(val path: String, val filename: String) : Event()
        /** 上传失败 */
        data class Failed(val filename: String, val message: String) : Event()
        /** 上传进度更新（0-100） */
        data class Progress(val filename: String, val percent: Int) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun broadcast(event: Event) {
        _events.tryEmit(event)
    }
}
