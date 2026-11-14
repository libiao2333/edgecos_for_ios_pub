package com.edgecos.mobileapp.feature.transfer

import com.edgecos.mobileapp.data.db.TransferRecordEntity

/** 传输类型 */
enum class TransferType { UPLOAD, DOWNLOAD }

/** 传输状态 */
enum class TransferStatus { TRANSFERRING, COMPLETED, FAILED }

/** 传输任务项：上传/下载进度追踪 */
data class TransferItem(
    val id: String,
    val filename: String,
    val progress: Int,         // 0–100
    val status: TransferStatus,
    val type: TransferType,
    val error: String? = null,
    val localPath: String? = null,  // 下载完成后的本地文件绝对路径（供第三方应用打开）
)

/** 持久化实体 → UI 模型（未知枚举值兜底，避免脏数据崩溃）。 */
fun TransferRecordEntity.toItem(): TransferItem = TransferItem(
    id = id,
    filename = filename,
    progress = progress,
    status = runCatching { TransferStatus.valueOf(status) }.getOrDefault(TransferStatus.FAILED),
    type = runCatching { TransferType.valueOf(type) }.getOrDefault(TransferType.DOWNLOAD),
    error = error,
    localPath = localPath,
)

/** UI 模型 → 持久化实体。 */
fun TransferItem.toEntity(): TransferRecordEntity = TransferRecordEntity(
    id = id,
    filename = filename,
    progress = progress,
    status = status.name,
    type = type.name,
    error = error,
    localPath = localPath,
)
