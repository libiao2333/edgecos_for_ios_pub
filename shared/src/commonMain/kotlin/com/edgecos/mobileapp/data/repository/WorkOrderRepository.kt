package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.WorkOrderDto
import com.edgecos.mobileapp.shared.dto.WorkOrderReplyDto
import com.edgecos.mobileapp.shared.dto.WorkOrderReplyRequest
import com.edgecos.mobileapp.shared.dto.WorkOrderStatusRequest
import com.edgecos.mobileapp.shared.dto.WorkOrderSubmitRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.WorkOrder
import com.edgecos.mobileapp.domain.model.WorkOrderReply

/**
 * 工单 Repository：提交、列表、详情、回复、状态变更。
 */
class WorkOrderRepository(
    private val api: EdgeApi,
) {

    suspend fun submit(
        title: String,
        content: String,
        category: String = "file_download_cdn",
    ): EdgeResult<WorkOrder> = runCatchingEdge {
        val resp = api.submitWorkOrder(
            WorkOrderSubmitRequest(title = title, content = content, category = category),
        )
        resp.toDomain()
    }

    suspend fun list(): EdgeResult<List<WorkOrder>> = runCatchingEdge {
        api.listWorkOrders().map { it.toDomain() }
    }

    suspend fun detail(id: Long): EdgeResult<WorkOrder> = runCatchingEdge {
        api.getWorkOrderDetail(id).toDomain()
    }

    suspend fun reply(id: Long, content: String): EdgeResult<WorkOrderReply> = runCatchingEdge {
        api.replyWorkOrder(id, WorkOrderReplyRequest(content)).toDomain()
    }

    suspend fun updateStatus(id: Long, status: String): EdgeResult<WorkOrder> = runCatchingEdge {
        api.updateWorkOrderStatus(id, WorkOrderStatusRequest(status)).toDomain()
    }
}

private fun WorkOrderDto.toDomain(): WorkOrder = WorkOrder(
    id = id,
    title = title,
    content = content,
    category = category,
    status = status,
    createdAt = createdAt,
    username = User?.username ?: "未知用户",
    replies = WorkOrderReplies?.map { it.toDomain() } ?: emptyList(),
)

private fun WorkOrderReplyDto.toDomain(): WorkOrderReply = WorkOrderReply(
    id = id,
    content = content,
    isAdminReply = isAdminReply,
    createdAt = createdAt,
)
