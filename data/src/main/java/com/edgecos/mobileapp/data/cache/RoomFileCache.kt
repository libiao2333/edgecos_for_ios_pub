package com.edgecos.mobileapp.data.cache

import androidx.room.withTransaction
import com.edgecos.mobileapp.data.db.EdgeDatabase
import com.edgecos.mobileapp.data.db.FileNodeEntity
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [FileCache] 的 Room 实现：文件列表落在 `file_nodes` 表。
 * 用于离线展示与减少重复请求。
 */
class RoomFileCache(
    private val db: EdgeDatabase,
) : FileCache {

    private val dao get() = db.fileNodeDao()

    override fun observeFiles(path: String): Flow<List<FileNode>> =
        dao.observeByPath(path).map { entities -> entities.map { it.toDomain() } }

    /**
     * 删旧 + 插新包在同一事务中：即使中途协程被取消，
     * 也不会出现“旧缓存已删、新数据未写”的半完成状态导致目录短暂变空。
     */
    override suspend fun replaceFiles(path: String, files: List<FileNodeDto>) {
        val entities = files.map { it.toEntity(path) }
        db.withTransaction {
            dao.deleteByPath(path, "$path/%")
            dao.upsertAll(entities)
        }
    }

    override suspend fun clear() {
        dao.clear()
    }
}

/**
 * DTO → Entity：保留 `activeShare` 的 shareId / expiration / extractionCode，
 * size 保留后端原始字符串（避免 BigInt 精度丢失），cachedAt 使用实体默认值。
 */
private fun FileNodeDto.toEntity(path: String): FileNodeEntity = FileNodeEntity(
    id = id,
    path = path,
    filename = filename,
    basename = basename,
    lastmod = lastmod,
    size = size,
    type = type,
    thumbnail = thumbnail,
    hasThumbnail = hasThumbnail,
    shareId = activeShare?.shareId,
    shareExpiration = activeShare?.expiration,
    shareExtractionCode = activeShare?.extractionCode,
)

private fun FileNodeEntity.toDomain(): FileNode = FileNode(
    id = id,
    filename = filename,
    basename = basename,
    lastmod = lastmod,
    size = size.toLongOrNull() ?: 0L,
    type = if (type == "directory") FileType.DIRECTORY else FileType.FILE,
    thumbnail = thumbnail,
    hasThumbnail = hasThumbnail,
)
