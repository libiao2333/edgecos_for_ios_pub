package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.MyPlanResponse
import com.edgecos.mobileapp.shared.dto.OrderDto
import com.edgecos.mobileapp.shared.dto.PayCancelRequest
import com.edgecos.mobileapp.shared.dto.PayCreateRequest
import com.edgecos.mobileapp.shared.dto.PayCreateResponse
import com.edgecos.mobileapp.shared.dto.PayQueryRequest
import com.edgecos.mobileapp.shared.dto.PayRepayRequest
import com.edgecos.mobileapp.shared.dto.PlanDto
import com.edgecos.mobileapp.shared.platform.randomUuidString
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.MyPlan
import com.edgecos.mobileapp.domain.model.Order
import com.edgecos.mobileapp.domain.model.OrderStatus
import com.edgecos.mobileapp.domain.model.Orders
import com.edgecos.mobileapp.domain.model.OrdersSummary
import com.edgecos.mobileapp.domain.model.PaymentResult
import com.edgecos.mobileapp.domain.model.PaymentStatus
import com.edgecos.mobileapp.domain.model.Plan
import com.edgecos.mobileapp.domain.model.TrafficStatus

/**
 * 计费 Repository：套餐查询、订单管理、支付流程。
 *
 * 覆盖 API §8 全部端点：
 * - 8.1-8.2 套餐查询（GET /plans、GET /my-plan）
 * - 8.3-8.4 订单查询（GET /orders、GET /orders/:tradeNo）
 * - 8.5-8.8 支付流程（POST /pay/create、/pay/repay、/pay/query、/pay/cancel）
 */
class BillingRepository(private val api: EdgeApi) {

    // ==================== 套餐 ====================

    /** 8.1 获取所有套餐列表（无需登录） */
    suspend fun getPlans(): EdgeResult<List<Plan>> = runCatchingEdge {
        api.getPlans().map { it.toDomain() }
    }

    /** 8.2 获取当前用户套餐信息 */
    suspend fun getMyPlan(): EdgeResult<MyPlan> = runCatchingEdge {
        api.getMyPlan().toDomain()
    }

    // ==================== 订单 ====================

    /** 8.3 获取订单列表 */
    suspend fun getOrders(): EdgeResult<Orders> = runCatchingEdge {
        val resp = api.getOrders()
        Orders(
            list = resp.list.map { it.toDomain() },
            summary = OrdersSummary(
                total = resp.summary.total,
                paid = resp.summary.paid,
                pending = resp.summary.pending,
                cancelled = resp.summary.cancelled,
                expired = resp.summary.expired,
            ),
        )
    }

    /** 8.4 获取单个订单详情 */
    suspend fun getOrderDetail(tradeNo: String): EdgeResult<Order> = runCatchingEdge {
        api.getOrderDetail(tradeNo).order.toDomain()
    }

    // ==================== 支付 ====================

    /**
     * 8.5 创建支付订单。
     *
     * @param planId        套餐 ID
     * @param paymentMethod 支付方式："alipay" / "wxpay" / "balance"
     * @param billingMonths 计费月数：1 / 3 / 12
     * @return 余额支付返回 [PaymentResult.isPaid]=true；网关支付返回 [PaymentResult.payUrl]
     */
    suspend fun createPayment(
        planId: Long,
        paymentMethod: String,
        billingMonths: Long = 1,
    ): EdgeResult<PaymentResult> = runCatchingEdge {
        val clientRequestId = randomUuidString()
        api.createPayment(PayCreateRequest(planId, paymentMethod, clientRequestId, billingMonths)).toDomain()
    }

    /** 8.6 重新支付已取消/未完成的订单 */
    suspend fun repayOrder(
        tradeNo: String,
        paymentMethod: String,
    ): EdgeResult<PaymentResult> = runCatchingEdge {
        val clientRequestId = randomUuidString()
        api.repayOrder(PayRepayRequest(tradeNo, paymentMethod, clientRequestId)).toDomain()
    }

    /** 8.7 查询支付状态 */
    suspend fun queryPayment(tradeNo: String): EdgeResult<PaymentStatus> = runCatchingEdge {
        val resp = api.queryPayment(PayQueryRequest(tradeNo))
        PaymentStatus(
            status = resp.status,
            order = resp.order?.toDomain(),
            remainingSeconds = resp.remainingSeconds,
        )
    }

    /** 8.8 取消订单 */
    suspend fun cancelOrder(tradeNo: String): EdgeResult<Unit> = runCatchingEdge {
        api.cancelOrder(PayCancelRequest(tradeNo))
        Unit
    }
}

// ==================== DTO → Domain 转换 ====================

private fun PlanDto.toDomain(): Plan = Plan(
    id = id,
    name = name,
    quota = quota.toLongOrNull() ?: 0L,
    trafficQuota = trafficQuota.toLongOrNull() ?: 0L,
    price = price,
    originalPrice = originalPrice,
    duration = duration,
    level = level,
    isTrafficPackage = isTrafficPackage,
    discount = discount,
    contactOnly = contactOnly,
    contactEmail = contactEmail,
)

private fun MyPlanResponse.toDomain(): MyPlan = MyPlan(
    plan = plan.toDomain(),
    expireTime = expireTime,
    quota = quota.toLongOrNull() ?: 0L,
    used = used.toLongOrNull() ?: 0L,
    trafficQuota = trafficQuota.toLongOrNull() ?: 0L,
    trafficUsed = trafficUsed.toLongOrNull() ?: 0L,
    monthlyTrafficUsed = monthlyTrafficUsed.toLongOrNull() ?: 0L,
    monthlyTrafficRemaining = monthlyTrafficRemaining.toLongOrNull() ?: 0L,
    extraTraffic = extraTraffic.toLongOrNull() ?: 0L,
    totalPurchasedTraffic = totalPurchasedTraffic.toLongOrNull() ?: 0L,
    packageTrafficRemaining = packageTrafficRemaining.toLongOrNull() ?: 0L,
    packageTrafficUsedInCycle = packageTrafficUsedInCycle.toLongOrNull() ?: 0L,
    totalTrafficRemaining = totalTrafficRemaining.toLongOrNull() ?: 0L,
    trafficUsagePercent = trafficUsagePercent,
    trafficStatus = when (trafficStatus) {
        "healthy" -> TrafficStatus.HEALTHY
        "warning" -> TrafficStatus.WARNING
        "exhausted" -> TrafficStatus.EXHAUSTED
        else -> TrafficStatus.HEALTHY
    },
)

private fun OrderDto.toDomain(): Order = Order(
    id = id,
    tradeNo = tradeNo,
    planId = planId,
    amount = amount,
    status = when (status) {
        "paid" -> OrderStatus.PAID
        "pending" -> OrderStatus.PENDING
        "cancelled" -> OrderStatus.CANCELLED
        "expired" -> OrderStatus.EXPIRED
        else -> OrderStatus.UNKNOWN
    },
    paymentMethod = paymentMethod,
    expireAt = expireAt,
    payTime = payTime,
    billingMonths = billingMonths,
    plan = Plan?.toDomain(),
)

private fun PayCreateResponse.toDomain(): PaymentResult = PaymentResult(
    tradeNo = tradeNo ?: "",
    isPaid = status == "paid",
    payUrl = payUrl,
    html = html,
    payType = payType,
    expireAt = expireAt,
    remainingSeconds = remainingSeconds,
    balance = balance,
    order = order?.toDomain(),
    notice = notice,
)
