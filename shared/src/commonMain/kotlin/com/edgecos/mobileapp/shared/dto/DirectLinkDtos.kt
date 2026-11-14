package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== 7.1 POST /generate-direct-link ====================

@Serializable
data class DirectLinkCreateRequest(
    val fileId: String? = null,
    val filePath: String? = null,
)

@Serializable
data class DirectLinkCreateResponse(
    @Serializable(with = FlexibleStringSerializer::class)
    val linkCode: String,
    val directUrl: String,
)

// ==================== 7.2 GET /f/:linkCode ====================
// 302 重定向，无响应体
