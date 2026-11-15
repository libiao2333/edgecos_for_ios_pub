package com.edgecos.mobileapp.domain.model

/**
 * 直链信息（POST /generate-direct-link 返回）。
 *
 * [directUrl] 为付费直链域名拼接的完整 URL（如 https://edgecos.com/f/xyz9876543）。
 */
data class DirectLink(
    val linkCode: String,
    val directUrl: String,
)
