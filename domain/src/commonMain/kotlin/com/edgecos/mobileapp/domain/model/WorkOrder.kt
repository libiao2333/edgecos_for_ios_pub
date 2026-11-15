package com.edgecos.mobileapp.domain.model

/**
 * 工单。
 * 来源：GET /workorder 列表项。
 */
data class WorkOrder(
    val id: Long,
    val title: String,
    val content: String,
    val category: String,
    val status: String,
    val createdAt: String,
    val username: String,
    val replies: List<WorkOrderReply>,
)

/**
 * 工单回复。
 * 来源：GET /workorder/{id} 详情中的 WorkOrderReplies。
 */
data class WorkOrderReply(
    val id: Long,
    val content: String,
    val isAdminReply: Boolean,
    val createdAt: String,
)
