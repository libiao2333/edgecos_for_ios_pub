package com.edgecos.mobileapp.data.cache

import com.edgecos.mobileapp.data.db.AnnouncementEntity
import com.edgecos.mobileapp.data.db.EdgeDatabase
import com.edgecos.mobileapp.domain.model.Announcement
import com.edgecos.mobileapp.shared.dto.AnnouncementDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [AnnouncementCache] 的 Room 实现：公告落在 `announcements` 表。
 */
class RoomAnnouncementCache(
    private val db: EdgeDatabase,
) : AnnouncementCache {

    private val dao get() = db.announcementDao()

    override fun observeAll(): Flow<List<Announcement>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeUnreadCount(): Flow<Int> = dao.observeUnreadCount()

    override suspend fun upsertAll(items: List<AnnouncementDto>) {
        dao.upsertAll(items.map { it.toEntity() })
    }

    override suspend fun markRead(id: Long) {
        dao.markRead(id)
    }
}

private fun AnnouncementEntity.toDomain(): Announcement = Announcement(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    isRead = isRead,
)

/** DTO → Entity：cachedAt 使用实体默认值。 */
private fun AnnouncementDto.toEntity(): AnnouncementEntity = AnnouncementEntity(
    id = id,
    title = title,
    content = content,
    createdAt = createdAt,
    isRead = isRead,
)
