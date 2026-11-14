package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== OAuth Login Session (QR Code) ====================

@Serializable
data class OAuthSessionRequest(
    val redirect: String = "/",
)

@Serializable
data class OAuthSessionResponse(
    val state: String,
    val url: String,
    val qrcode: String? = null,
    val expiresAt: String? = null,
)

// ==================== OAuth QR Status ====================

@Serializable
data class OAuthQrStatusResponse(
    val status: String, // pending / success / not_bound / failed / expired
    val mode: String? = null,
    val provider: String? = null,
    val message: String? = null,
    val expiresAt: String? = null,
)
