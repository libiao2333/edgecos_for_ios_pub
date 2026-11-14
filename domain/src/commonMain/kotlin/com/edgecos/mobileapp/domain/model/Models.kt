package com.edgecos.mobileapp.domain.model

/**
 * 已登录用户。
 * 来源：POST /login 响应中的 user 字段。
 */
data class User(
    val id: Long,
    val username: String,
    val role: String,
    val email: String?,
    val qqNumber: String? = null,
    val qqOpenId: String? = null,
    val qqNickname: String? = null,
    val qqAvatar: String? = null,
    val oauthAccounts: Map<String, OAuthAccount> = emptyMap(),
    val realNameStatus: String = "unverified",
    val realNameMaskedName: String? = null,
    val realNameMaskedIdNo: String? = null,
    val realNameVerifiedAt: String? = null,
)

/**
 * 第三方 OAuth 账号信息。
 * 后端 routes/szczkOAuth.js normalizeOauthProfile 返回结构。
 */
data class OAuthAccount(
    val provider: String? = null,
    val uid: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val gender: String? = null,
    val location: String? = null,
    val ip: String? = null,
    val accessToken: String? = null,
    val updatedAt: String? = null,
)

/**
 * 文件/目录节点。
 * 来源：GET /files 列表项。
 */
data class FileNode(
    val id: String,
    val filename: String,
    val basename: String,
    val lastmod: String,
    /** 字节数。Long 上限约 8EB，足以覆盖文件与配额场景（commonMain 无 BigInteger） */
    val size: Long,
    val type: FileType,
    val thumbnail: String? = null,
    val hasThumbnail: Boolean = false,
    val activeShare: ActiveShare? = null,
)

enum class FileType { FILE, DIRECTORY }

data class ActiveShare(
    val shareId: String,
    val expiration: String?,
    val extractionCode: String?,
)

/**
 * 文件汇总。
 * 来源：GET /files/summary。
 */
data class FileSummary(
    val fileCount: Long,
    val folderCount: Long,
    val recentCount: Long,
    val sharedCount: Long,
)

/**
 * 配额。
 * 来源：GET /user/quota。
 *
 * 注意：后端返回字符串以避免 JS Number 精度问题，Kotlin 侧用 Long（字节级精度足够）。
 */
data class Quota(
    val quota: Long,
    val used: Long,
) {
    val remaining: Long get() = (quota - used).coerceAtLeast(0L)
    val usagePercent: Float get() = if (quota > 0L) {
        used.toFloat() / quota.toFloat() * 100f
    } else 0f
}

/**
 * 公告。
 * 来源：GET /announcements。
 */
data class Announcement(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val isRead: Boolean = false,
)

/**
 * 套餐与流量汇总。
 * 来源：GET /my-plan。
 */
data class PlanSummary(
    val planId: String?,
    val planName: String,
    val monthlyTrafficQuota: Long,
    val monthlyTrafficUsed: Long,
    val extraTraffic: Long,
    val trafficUsagePercent: Float,
    val trafficStatus: TrafficStatus,
    val expiredAt: String?,
)

enum class TrafficStatus { HEALTHY, WARNING, EXHAUSTED }
