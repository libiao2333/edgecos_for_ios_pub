package com.edgecos.mobileapp.feature.transfer

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 传输事件总线：广播上传/下载进度、成功、失败事件。
 *
 * [UploadService] → 上传事件
 * [FilesViewModel.download] → 下载事件
 * [TransferViewModel] → 收集并更新 UI 列表
 */
object TransferEventBus {

    sealed class Event {
        /** 开始传输 */
        data class Started(
            val id: String,
            val filename: String,
            val type: TransferType,
        ) : Event()
        /** 进度更新（0-100） */
        data class Progress(
            val id: String,
            val filename: String,
            val percent: Int,
            val type: TransferType,
        ) : Event()
        /** 传输完成（上传：刷新目录；下载：提示成功） */
        data class Completed(
            val id: String,
            val filename: String,
            val type: TransferType,
            val path: String? = null,   // 上传时为目标目录
        ) : Event()
        /** 传输失败 */
        data class Failed(
            val id: String,
            val filename: String,
            val error: String,
        ) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun broadcast(event: Event) {
        _events.tryEmit(event)
    }
}
