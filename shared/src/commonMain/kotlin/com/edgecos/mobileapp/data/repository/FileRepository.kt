package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.cache.FileCache
import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.BatchCopyRequest
import com.edgecos.mobileapp.shared.dto.BatchDeleteRequest
import com.edgecos.mobileapp.shared.dto.BatchIdsRequest
import com.edgecos.mobileapp.shared.dto.BatchMoveRequest
import com.edgecos.mobileapp.shared.dto.CopyRequest
import com.edgecos.mobileapp.shared.dto.DeleteRequest
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import com.edgecos.mobileapp.shared.dto.MkdirRequest
import com.edgecos.mobileapp.shared.dto.MoveRequest
import com.edgecos.mobileapp.shared.dto.RenameRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileSummary
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.model.Quota
import kotlinx.coroutines.flow.Flow

/**
 * 文件 Repository：列目录、文件操作、批量操作、回收站。
 *
 * 缓存策略：NetworkFirst——优先网络，成功后写缓存，失败回退本地缓存。
 * 平台无关：只依赖 [EdgeApi] 与 [FileCache]，具体缓存实现由各平台提供。
 */
class FileRepository(
    private val api: EdgeApi,
    private val cache: FileCache,
) {

    /** 观察指定路径下的文件列表（先返回缓存，后台刷新） */
    fun observeFiles(path: String): Flow<List<FileNode>> = cache.observeFiles(path)

    /**
     * 刷新指定路径下的文件列表（强制走网络）。
     * 缓存写入（删旧 + 插新）由 [FileCache] 实现在单事务内完成：即使中途协程被取消，
     * 也不会出现“旧缓存已删、新数据未写”的半完成状态导致目录短暂变空。
     */
    suspend fun refreshFiles(path: String): EdgeResult<List<FileNode>> = runCatchingEdge {
        val dtos = api.listFiles(path)
        cache.replaceFiles(path, dtos)
        dtos.map { it.toDomain() }
    }

    /** 清空本地文件缓存（登出时调用，避免跨账号数据残留） */
    suspend fun clearCache() {
        cache.clear()
    }

    /** 文件汇总 */
    suspend fun getSummary(): EdgeResult<FileSummary> = runCatchingEdge {
        val dto = api.getFilesSummary()
        FileSummary(dto.fileCount, dto.folderCount, dto.recentCount, dto.sharedCount)
    }

    /**
     * 用户存储配额：总量 / 已用 / 剩余 / 使用百分比。
     * 来源：GET /user/quota，后端返回 BIGINT 字符串以避免 JS 精度问题。
     */
    suspend fun getQuota(): EdgeResult<Quota> = runCatchingEdge {
        val dto = api.getUserQuota()
        Quota(
            quota = dto.quota.toLongOrNull() ?: 0L,
            used = dto.used.toLongOrNull() ?: 0L,
        )
    }

    /** 新建文件夹 */
    suspend fun mkdir(path: String): EdgeResult<Unit> = runCatchingEdge {
        api.mkdir(MkdirRequest(path))
        Unit
    }

    /** 重命名 */
    suspend fun rename(oldPath: String, id: String, newName: String): EdgeResult<Unit> = runCatchingEdge {
        api.rename(RenameRequest(oldPath, id, newName))
        Unit
    }

    /** 移动 */
    suspend fun move(sourcePath: String, sourceId: String, targetDir: String): EdgeResult<Unit> = runCatchingEdge {
        api.move(MoveRequest(sourcePath, sourceId, targetDir))
        Unit
    }

    /** 复制 */
    suspend fun copy(sourcePath: String, sourceId: String, targetDir: String): EdgeResult<Unit> = runCatchingEdge {
        api.copy(CopyRequest(sourcePath, sourceId, targetDir))
        Unit
    }

    /** 删除（移入回收站） */
    suspend fun delete(path: String, id: String): EdgeResult<Unit> = runCatchingEdge {
        api.delete(DeleteRequest(path, id))
        Unit
    }

    /** 批量移动 */
    suspend fun batchMove(sourceIds: List<String>, sourcePaths: List<String>, targetDir: String): EdgeResult<Unit> = runCatchingEdge {
        api.batchMove(BatchMoveRequest(sourceIds, sourcePaths, targetDir))
        Unit
    }

    /** 批量复制 */
    suspend fun batchCopy(sourceIds: List<String>, sourcePaths: List<String>, targetDir: String): EdgeResult<Unit> = runCatchingEdge {
        api.batchCopy(BatchCopyRequest(sourceIds, sourcePaths, targetDir))
        Unit
    }

    /** 批量删除 */
    suspend fun batchDelete(ids: List<String>, paths: List<String>): EdgeResult<Unit> = runCatchingEdge {
        api.batchDelete(BatchDeleteRequest(ids, paths))
        Unit
    }

    /** 回收站列表 */
    suspend fun listTrash(): EdgeResult<List<FileNode>> = runCatchingEdge {
        api.listTrash().map { it.toDomain() }
    }

    /** 恢复 */
    suspend fun restore(ids: List<String>): EdgeResult<Unit> = runCatchingEdge {
        api.batchRestoreTrash(BatchIdsRequest(ids))
        Unit
    }

    /** 彻底删除 */
    suspend fun deletePermanently(ids: List<String>): EdgeResult<Unit> = runCatchingEdge {
        api.batchDeleteTrash(BatchIdsRequest(ids))
        Unit
    }
}

private fun FileNodeDto.toDomain(): FileNode = FileNode(
    id = id,
    filename = filename,
    basename = basename,
    lastmod = lastmod,
    size = size.toLongOrNull() ?: 0L,
    type = if (type == "directory") FileType.DIRECTORY else FileType.FILE,
    thumbnail = thumbnail,
    hasThumbnail = hasThumbnail,
)
