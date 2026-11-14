package com.edgecos.mobileapp.data.cache

import com.edgecos.mobileapp.domain.model.Announcement
import com.edgecos.mobileapp.shared.dto.AnnouncementDto
import kotlinx.coroutines.flow.Flow

/**
 * 公告缓存抽象。
 *
 * 平台无关的 [com.edgecos.mobileapp.data.repository.AnnouncementRepository]
 * 只依赖本接口，具体持久化实现由各平台提供。
 */
interface AnnouncementCache {

    /** 观察全部公告（按后端返回顺序持久化，缓存变化时推送） */
    fun observeAll(): Flow<List<Announcement>>

    /** 观察未读公告数量 */
    fun observeUnreadCount(): Flow<Int>

    /** 批量写入（存在则更新）公告 */
    suspend fun upsertAll(items: List<AnnouncementDto>)

    /** 标记单条公告为已读 */
    suspend fun markRead(id: Long)
}
