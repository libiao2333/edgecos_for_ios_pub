package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== Passkey Register ====================

@Serializable
data class PasskeyRegisterOptionsResponse(
    val options: PasskeyCreationOptions,
    val transactionId: String,
)

@Serializable
data class PasskeyCreationOptions(
    val rp: PasskeyRp,
    val user: PasskeyUserInfo,
    val challenge: String,
    val pubKeyCredParams: List<PasskeyCredParam>,
    @Serializable(with = FlexibleLongSerializer::class)
    val timeout: Long,
    val excludeCredentials: List<PasskeyCredential> = emptyList(),
    val authenticatorSelection: PasskeyAuthSelection,
    val attestation: String = "none",
)

@Serializable
data class PasskeyRp(
    val name: String,
    val id: String,
)

@Serializable
data class PasskeyUserInfo(
    val id: String,
    val name: String,
    val displayName: String,
)

@Serializable
data class PasskeyCredParam(
    val type: String,
    @Serializable(with = FlexibleIntSerializer::class)
    val alg: Int,
)

@Serializable
data class PasskeyCredential(
    val id: String,
    val type: String,
)

@Serializable
data class PasskeyAuthSelection(
    val authenticatorAttachment: String = "platform",
    val userVerification: String = "preferred",
)

@Serializable
data class PasskeyRegisterVerifyRequest(
    val transactionId: String,
    val response: PasskeyAuthenticatorResponse,
    val name: String = "EdgeCOS Android",
)

@Serializable
data class PasskeyAuthenticatorResponse(
    val id: String,
    val rawId: String,
    val response: PasskeyAttestationResponse,
    val type: String = "public-key",
)

@Serializable
data class PasskeyAttestationResponse(
    val attestationObject: String,
    val clientDataJSON: String,
)

// ==================== Passkey Login ====================

@Serializable
data class PasskeyLoginOptionsRequest(
    val username: String = "",
)

@Serializable
data class PasskeyLoginOptionsResponse(
    val options: PasskeyLoginOptions,
    val transactionId: String,
)

@Serializable
data class PasskeyLoginOptions(
    val challenge: String,
    val rpId: String = "user.edgecos.com",
    val allowCredentials: List<PasskeyCredential> = emptyList(),
    @Serializable(with = FlexibleLongSerializer::class)
    val timeout: Long = 60000,
    val userVerification: String = "required",
)

@Serializable
data class PasskeyLoginVerifyRequest(
    val transactionId: String,
    val response: PasskeyAssertionResponse,
)

@Serializable
data class PasskeyAssertionResponse(
    val id: String,
    val rawId: String,
    val response: PasskeyAssertionData,
    val type: String = "public-key",
)

@Serializable
data class PasskeyAssertionData(
    val authenticatorData: String,
    val clientDataJSON: String,
    val signature: String,
    val userHandle: String? = null,
)

// ==================== Passkey Management ====================

@Serializable
data class PasskeyCredentialsResponse(
    val credentials: List<PasskeyCredentialInfo>,
)

@Serializable
data class PasskeyCredentialInfo(
    val id: String,
    val name: String,
    val deviceType: String,
    val backedUp: Boolean,
    val createdAt: String,
    val lastUsedAt: String,
)
