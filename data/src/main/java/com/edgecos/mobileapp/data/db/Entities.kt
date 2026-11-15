package com.edgecos.mobileapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 文件节点缓存实体。
 * 用于离线展示与减少重复请求。
 */
@Entity(tableName = "file_nodes")
data class FileNodeEntity(
    @PrimaryKey val id: String,
    val path: String,
    val filename: String,
    val basename: String,
    val lastmod: String,
    val size: String,   // BigInt 字符串
    val type: String,   // "file" | "directory"
    val thumbnail: String? = null,
    val hasThumbnail: Boolean = false,
    val shareId: String? = null,
    val shareExpiration: String? = null,
    val shareExtractionCode: String? = null,
    val cachedAt: Long = System.currentTimeMillis(),
)

/**
 * 上传任务实体。
 * 用于断点续传与上传队列管理。
 */
@Entity(tableName = "upload_tasks")
data class UploadTaskEntity(
    @PrimaryKey val taskId: String,
    val fileUri: String,
    val filename: String,
    val path: String,
    val size: Long,
    val hash: String? = null,
    val uploadId: String? = null,        // 分片上传 ID
    val key: String? = null,             // OSS 对象 key
    val progress: Int = 0,               // 0-100
    val status: String = "pending",      // pending | uploading | completed | failed | aborted
    val completedParts: String = "",     // JSON: [{partNumber, etag}]
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * 公告实体。
 */
@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val isRead: Boolean = false,
    val cachedAt: Long = System.currentTimeMillis(),
)

/**
 * 传输记录实体。
 * 持久化上传/下载历史，重启后保留（除非用户在传输页长按删除）。
 */
@Entity(tableName = "transfer_records")
data class TransferRecordEntity(
    @PrimaryKey val id: String,
    val filename: String,
    val progress: Int,          // 0-100
    val status: String,         // TRANSFERRING | COMPLETED | FAILED
    val type: String,           // UPLOAD | DOWNLOAD
    val error: String? = null,
    val localPath: String? = null,  // 下载完成后的本地文件绝对路径（供第三方应用打开）
    val updatedAt: Long = System.currentTimeMillis(),
)
