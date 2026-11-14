package com.edgecos.mobileapp.domain.model

/**
 * 分享信息（GET /share/:shareId 返回）。
 */
data class ShareInfo(
    val fileName: String,
    val owner: String,
    val createdAt: String,
    val expiration: String?,
    val needExtractionCode: Boolean,
    val isFolder: Boolean,
)

/**
 * 创建分享结果（POST /share 返回）。
 *
 * [shareUrl] 为拼接好的完整访问链接（https://user.edgecos.com/s/{shareId}），
 * 可直接用于复制到剪贴板或分享给他人。
 */
data class ShareCreateResult(
    val shareId: String,
    val reused: Boolean,
    val shareUrl: String,
    val extractionCode: String?,
    val expiration: String?,
)
