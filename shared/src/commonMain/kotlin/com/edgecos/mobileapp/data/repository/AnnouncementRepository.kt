package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.cache.AnnouncementCache
import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

/**
 * 公告 Repository：列表、已读标记。
 *
 * 平台无关：只依赖 [EdgeApi] 与 [AnnouncementCache]，具体缓存实现由各平台提供。
 */
class AnnouncementRepository(
    private val api: EdgeApi,
    private val cache: AnnouncementCache,
) {

    fun observeAll(): Flow<List<Announcement>> = cache.observeAll()

    fun observeUnreadCount(): Flow<Int> = cache.observeUnreadCount()

    suspend fun refresh(): EdgeResult<Unit> = runCatchingEdge {
        val resp = api.getAnnouncements()
        cache.upsertAll(resp.list)
        Unit
    }

    suspend fun markRead(id: Long): EdgeResult<Unit> = runCatchingEdge {
        api.markAnnouncementRead(id)
        cache.markRead(id)
        Unit
    }
}
