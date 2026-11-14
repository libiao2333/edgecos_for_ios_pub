package com.edgecos.mobileapp.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TransferViewModel : ViewModel() {

    private val recordDao = ServiceLocator.database.transferRecordDao()

    private val _transfers = MutableStateFlow<List<TransferItem>>(emptyList())
    val transfers: StateFlow<List<TransferItem>> = _transfers.asStateFlow()

    init {
        // 恢复持久化记录（重启后保留；"传输中"的任务随进程终止已无法继续，修正为"传输已中断"）
        viewModelScope.launch {
            val restored = recordDao.getAll().map { entity ->
                val item = entity.toItem()
                if (item.status == TransferStatus.TRANSFERRING) {
                    item.copy(status = TransferStatus.FAILED, error = "传输已中断")
                        .also { recordDao.upsert(it.toEntity()) }
                } else {
                    item
                }
            }
            // 防御并发：加载期间若已有新事件落地，内存态保持在后
            val liveIds = _transfers.value.map { it.id }.toSet()
            _transfers.value = restored.filterNot { it.id in liveIds } + _transfers.value
        }
        // 监听下载事件（TransferEventBus）
        viewModelScope.launch {
            TransferEventBus.events.collect { event ->
                applyEvent(event)
            }
        }
        // 监听上传事件（UploadEventBus，来自 upload 模块）
        viewModelScope.launch {
            com.edgecos.mobileapp.upload.UploadEventBus.events.collect { event ->
                when (event) {
                    is com.edgecos.mobileapp.upload.UploadEventBus.Event.Progress -> {
                        TransferEventBus.broadcast(
                            TransferEventBus.Event.Progress(
                                event.filename, event.filename, event.percent, TransferType.UPLOAD,
                            ),
                        )
                    }
                    is com.edgecos.mobileapp.upload.UploadEventBus.Event.Success -> {
                        TransferEventBus.broadcast(
                            TransferEventBus.Event.Completed(
                                event.filename, event.filename, TransferType.UPLOAD, event.path,
                            ),
                        )
                    }
                    is com.edgecos.mobileapp.upload.UploadEventBus.Event.Failed -> {
                        TransferEventBus.broadcast(
                            TransferEventBus.Event.Failed(
                                event.filename, event.filename, event.message,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun applyEvent(event: TransferEventBus.Event) {
        val current = _transfers.value.toMutableList()
        when (event) {
            is TransferEventBus.Event.Started -> {
                // 去重
                if (current.none { it.id == event.id }) {
                    val item = TransferItem(
                        id = event.id,
                        filename = event.filename,
                        progress = 0,
                        status = TransferStatus.TRANSFERRING,
                        type = event.type,
                    )
                    current.add(item)
                    persist(item)
                }
            }
            is TransferEventBus.Event.Progress -> {
                val idx = current.indexOfFirst { it.id == event.id }
                if (idx >= 0) {
                    current[idx] = current[idx].copy(
                        progress = event.percent,
                        status = TransferStatus.TRANSFERRING,
                    )
                } else {
                    // 无 Started 事件时（如上传），直接用 Progress 的类型创建
                    val item = TransferItem(
                        id = event.id,
                        filename = event.filename,
                        progress = event.percent,
                        status = TransferStatus.TRANSFERRING,
                        type = event.type,
                    )
                    current.add(item)
                    persist(item)
                }
            }
            is TransferEventBus.Event.Completed -> {
                val idx = current.indexOfFirst { it.id == event.id }
                if (idx >= 0) {
                    // 下载完成：记录本地文件路径，供传输页"用其他应用打开"
                    // （上传的 path 是服务端目录，不记录）
                    val localPath = if (event.type == TransferType.DOWNLOAD) event.path else null
                    val item = current[idx].copy(
                        progress = 100,
                        status = TransferStatus.COMPLETED,
                        localPath = localPath ?: current[idx].localPath,
                    )
                    current[idx] = item
                    persist(item)
                }
            }
            is TransferEventBus.Event.Failed -> {
                val idx = current.indexOfFirst { it.id == event.id }
                if (idx >= 0) {
                    val item = current[idx].copy(
                        status = TransferStatus.FAILED,
                        error = event.error,
                    )
                    current[idx] = item
                    persist(item)
                }
            }
        }
        _transfers.value = current
    }

    /** 删除单条传输记录（长按删除；不影响文件本身）。 */
    fun remove(id: String) {
        _transfers.value = _transfers.value.filterNot { it.id == id }
        viewModelScope.launch { runCatching { recordDao.delete(id) } }
    }

    /**
     * 异步落库。
     *
     * 仅在"新建记录 / 完成 / 失败"节点调用；高频进度更新不落库——
     * 重启后"传输中"的任务统一修正为"传输已中断"，进度值无持久化意义。
     */
    private fun persist(item: TransferItem) {
        viewModelScope.launch { runCatching { recordDao.upsert(item.toEntity()) } }
    }

    companion object {
        fun newId(): String = UUID.randomUUID().toString().take(8)
    }
}
