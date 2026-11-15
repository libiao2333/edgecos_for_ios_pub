package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== 6.1 POST /share ====================

@Serializable
data class ShareCreateRequest(
    val fileId: String? = null,
    val filePath: String? = null,
    val fileName: String,
    val expirationType: String,           // "1d" / "7d" / "30d" / "permanent" / "custom"
    val customExpiration: String? = null, // ISO 8601，仅 expirationType=custom 时必填
    val extractionCode: String? = null,   // 4-8 位字母数字，可选
)

@Serializable
data class ShareCreateResponse(
    @Serializable(with = FlexibleStringSerializer::class)
    val shareId: String,
    val reused: Boolean = false,
    val activeShare: ActiveShareDto? = null,
)

// 6.2 DELETE /share/file/:fileId — 仅 Path 参数，无 Body

// ==================== 6.3 GET /share/:shareId ====================

@Serializable
data class ShareInfoResponse(
    val fileName: String,
    val owner: String,
    val createdAt: String,
    val expiration: String? = null,
    val needExtractionCode: Boolean = false,
    val isFolder: Boolean = false,
)

// ==================== 6.4 POST /share/:shareId/files ====================

@Serializable
data class ShareFilesRequest(
    val path: String = "/",
    val code: String? = null,
)

// 响应复用 List<FileNodeDto>

// ==================== 6.5 GET /share/:shareId/download ====================
// Query: code / path，302 重定向，无响应体
