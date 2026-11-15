package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== Work Order Requests ====================

@Serializable
data class WorkOrderSubmitRequest(
    val title: String,
    val content: String,
    val category: String = "file_download_cdn",
)

@Serializable
data class WorkOrderReplyRequest(
    val content: String,
)

@Serializable
data class WorkOrderStatusRequest(
    val status: String,
)

// ==================== Work Order Responses ====================

@Serializable
data class WorkOrderDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val userId: Long,
    val title: String,
    val content: String,
    val category: String,
    val status: String,
    val createdAt: String,
    @SerialName("User") val User: WorkOrderUserDto? = null,
    @SerialName("WorkOrderReplies") val WorkOrderReplies: List<WorkOrderReplyDto>? = null,
)

@Serializable
data class WorkOrderUserDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val username: String,
)

@Serializable
data class WorkOrderReplyDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val workOrderId: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val userId: Long,
    val content: String,
    val isAdminReply: Boolean,
    val createdAt: String,
)
