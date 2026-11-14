package com.edgecos.mobileapp.data.cache

import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import kotlinx.coroutines.flow.Flow

/**
 * 文件列表缓存抽象。
 *
 * 平台无关的 [com.edgecos.mobileapp.data.repository.FileRepository] 只依赖本接口，
 * 具体持久化实现（Android 侧 Room、iOS 侧后续实现）由各平台提供。
 */
interface FileCache {

    /** 观察指定路径下的文件列表（缓存变化时推送） */
    fun observeFiles(path: String): Flow<List<FileNode>>

    /**
     * 用最新网络数据整体替换指定路径的缓存。
     *
     * 入参使用 DTO 而非领域模型：实现侧需要保留 `activeShare` 等
     * 领域模型未覆盖的持久化字段。实现需在单事务内完成「删旧 + 插新」。
     */
    suspend fun replaceFiles(path: String, files: List<FileNodeDto>)

    /** 清空全部文件缓存（登出时调用，避免跨账号数据残留） */
    suspend fun clear()
}
