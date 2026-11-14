package com.edgecos.mobileapp.domain.model

/**
 * 套餐（GET /plans 列表项）。
 */
data class Plan(
    val id: Long,
    val name: String,
    val quota: Long,
    val trafficQuota: Long,
    val price: String,
    val originalPrice: String?,
    val duration: Long,
    val level: Long,
    val isTrafficPackage: Boolean,
    val discount: String?,
    val contactOnly: Boolean,
    val contactEmail: String?,
)

/**
 * 当前用户套餐详情（GET /my-plan 返回）。
 */
data class MyPlan(
    val plan: Plan,
    val expireTime: String?,
    val quota: Long,
    val used: Long,
    val trafficQuota: Long,
    val trafficUsed: Long,
    val monthlyTrafficUsed: Long,
    val monthlyTrafficRemaining: Long,
    val extraTraffic: Long,
    val totalPurchasedTraffic: Long,
    val packageTrafficRemaining: Long,
    val packageTrafficUsedInCycle: Long,
    val totalTrafficRemaining: Long,
    val trafficUsagePercent: Double,
    val trafficStatus: TrafficStatus,
) {
    val storageRemaining: Long get() = (quota - used).coerceAtLeast(0L)
}

enum class OrderStatus { PAID, PENDING, CANCELLED, EXPIRED, UNKNOWN }

/**
 * 订单（GET /orders 列表项）。
 */
data class Order(
    val id: Long,
    val tradeNo: String,
    val planId: Long,
    val amount: String,
    val status: OrderStatus,
    val paymentMethod: String?,
    val expireAt: String?,
    val payTime: String?,
    val billingMonths: Long,
    val plan: Plan?,
)

data class OrdersSummary(
    val total: Long,
    val paid: Long,
    val pending: Long,
    val cancelled: Long,
    val expired: Long,
)

data class Orders(
    val list: List<Order>,
    val summary: OrdersSummary,
)

/**
 * 支付创建结果（POST /pay/create 或 /pay/repay 返回）。
 *
 * - 余额支付：[isPaid]=true，[balance] 为支付后余额，[order] 为已支付订单
 * - 网关支付：[payUrl] 非空，客户端打开 [payUrl] 完成支付后轮询查询状态
 */
data class PaymentResult(
    val tradeNo: String,
    val isPaid: Boolean,
    val payUrl: String?,
    val html: String?,
    val payType: String?,
    val expireAt: String?,
    val remainingSeconds: Long?,
    val balance: String?,
    val order: Order?,
    val notice: String?,
)

/**
 * 支付状态查询结果（POST /pay/query 返回）。
 */
data class PaymentStatus(
    val status: String,         // "paid" / "pending" / "expired"
    val order: Order?,
    val remainingSeconds: Long?,
)
