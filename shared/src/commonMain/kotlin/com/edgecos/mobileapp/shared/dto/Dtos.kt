package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== Auth ====================

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val user: UserDto,
)

@Serializable
data class UserDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val username: String,
    val role: String,
    val email: String? = null,
    val qqNumber: String? = null,
    val qqOpenId: String? = null,
    val qqNickname: String? = null,
    val qqAvatar: String? = null,
    val oauthAccounts: Map<String, OAuthAccountDto>? = null,
    val realNameStatus: String = "unverified",
    val realNameMaskedName: String? = null,
    val realNameMaskedIdNo: String? = null,
    val realNameVerifiedAt: String? = null,
)

/**
 * 第三方 OAuth 账号信息。
 * 后端 routes/szczkOAuth.js normalizeOauthProfile 返回结构：
 * {provider, uid, nickname, avatar, gender, location, ip, accessToken, updatedAt}
 */
@Serializable
data class OAuthAccountDto(
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
 * 腾讯 Captcha 验证数据：由前端 Captcha SDK 弹窗回调拿到，
 * 提交给后端由 requireCaptcha 中间件调腾讯云 API 校验。
 */
@Serializable
data class CaptchaData(
    val ticket: String,
    val randstr: String,
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val verifyCode: String,
    val password: String,
    val captchaData: CaptchaData,
    val captchaPrecheckToken: String,
)

@Serializable
data class SendEmailCodeRequest(
    val email: String,
    val captchaData: CaptchaData,
    val captchaPrecheckToken: String,
)

@Serializable
data class CaptchaPrecheckRequest(
    val scene: String = "default",
)

@Serializable
data class CaptchaPrecheckResponse(
    val token: String,
)

// ==================== User ====================

@Serializable
data class QuotaResponse(
    @Serializable(with = FlexibleStringSerializer::class)
    val quota: String,   // BigInt 字符串
    @Serializable(with = FlexibleStringSerializer::class)
    val used: String,
)

// ==================== Files ====================

@Serializable
data class FileNodeDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val id: String,
    val filename: String,
    val basename: String,
    val lastmod: String,
    // 注意：目录项的 size 可能是 number（如 "size": 1），文件项是大整数字符串，
    // 必须用兼容序列化器，否则 listFiles/最近文件会整体解析失败。
    @Serializable(with = FlexibleStringSerializer::class)
    val size: String,   // BigInt 字符串或 JSON number
    val type: String,   // "file" | "directory"
    val thumbnail: String? = null,
    val hasThumbnail: Boolean = false,
    val activeShare: ActiveShareDto? = null,
)

@Serializable
data class ActiveShareDto(
    @Serializable(with = FlexibleStringSerializer::class)
    val shareId: String,
    val expiration: String? = null,
    val extractionCode: String? = null,
)

@Serializable
data class FileSummaryDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val fileCount: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val folderCount: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val recentCount: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val sharedCount: Long,
)

@Serializable
data class MkdirRequest(val path: String)

@Serializable
data class RenameRequest(
    val oldPath: String,
    val id: String,
    val newName: String,
)

@Serializable
data class MoveRequest(
    val sourcePath: String,
    val sourceId: String,
    val targetDir: String,
)

@Serializable
data class CopyRequest(
    val sourcePath: String,
    val sourceId: String,
    val targetDir: String,
)

@Serializable
data class DeleteRequest(
    val path: String,
    val id: String,
)

@Serializable
data class BatchMoveRequest(
    val sourceIds: List<String>,
    val sourcePaths: List<String>,
    val targetDir: String,
)

@Serializable
data class BatchCopyRequest(
    val sourceIds: List<String>,
    val sourcePaths: List<String>,
    val targetDir: String,
)

@Serializable
data class BatchDeleteRequest(
    val ids: List<String>,
    val paths: List<String>,
)

@Serializable
data class BatchIdsRequest(
    val ids: List<String>,
)

// ==================== Upload ====================

@Serializable
data class UploadTokenResponse(
    val method: String,    // "INSTANT" | "PUT"
    val instant: Boolean = false,
    val key: String? = null,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    val size: Long? = null,
    val hash: String? = null,
    val url: String? = null,
)

@Serializable
data class UploadCompleteRequest(
    val key: String,
    val filename: String,
    val path: String,
    val size: Long,
    val hash: String,
    val overwrite: Boolean = false,
)

@Serializable
data class UploadInstantCompleteRequest(
    val filename: String,
    val path: String,
    val size: Long,
    val hash: String,
    val overwrite: Boolean = false,
)

@Serializable
data class UploadConflictResponse(
    val exists: Boolean,
    val isFolder: Boolean = false,
    val id: String? = null,
)

// ==================== Announcements ====================

@Serializable
data class AnnouncementsResponse(
    val list: List<AnnouncementDto>,
    @Serializable(with = FlexibleLongSerializer::class)
    val unread: Long,
)

@Serializable
data class AnnouncementDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: String,
    val isRead: Boolean = false,
)
