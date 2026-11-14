package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.Serializable

// ==================== 8.1 GET /plans ====================

@Serializable
data class PlanDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val name: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val quota: String,              // BigInt 字符串（存储配额）
    @Serializable(with = FlexibleStringSerializer::class)
    val trafficQuota: String,       // BigInt 字符串（流量配额）
    @Serializable(with = FlexibleStringSerializer::class)
    val price: String,              // DECIMAL 字符串
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val originalPrice: String? = null,
    @Serializable(with = FlexibleLongSerializer::class)
    val duration: Long = 0,         // 0=永久，1=1 月
    @Serializable(with = FlexibleLongSerializer::class)
    val level: Long = 0,
    val isTrafficPackage: Boolean = false,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val discount: String? = null,
    val contactOnly: Boolean = false,
    val contactEmail: String? = null,
)

// ==================== 8.2 GET /my-plan ====================

@Serializable
data class MyPlanResponse(
    val plan: PlanDto,
    val expireTime: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    val quota: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val used: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val trafficQuota: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val trafficUsed: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val monthlyTrafficUsed: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val monthlyTrafficRemaining: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val extraTraffic: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalPurchasedTraffic: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val packageTrafficRemaining: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val packageTrafficUsedInCycle: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val totalTrafficRemaining: String,
    @Serializable(with = FlexibleDoubleSerializer::class)
    val trafficUsagePercent: Double,
    val trafficStatus: String,      // "healthy" / "warning" / "exhausted"
    // packagePolicy 后端返回对象而非字符串，且 UI 未使用，忽略以避免解析失败
)

// ==================== 8.3 GET /orders ====================

@Serializable
data class OrderDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val tradeNo: String,
    @Serializable(with = FlexibleLongSerializer::class)
    val planId: Long,
    @Serializable(with = FlexibleStringSerializer::class)
    val amount: String,
    val status: String,             // "paid" / "pending" / "cancelled" / "expired"
    val paymentMethod: String? = null,
    val expireAt: String? = null,
    val payTime: String? = null,
    @Serializable(with = FlexibleLongSerializer::class)
    val billingMonths: Long = 1,
    val Plan: PlanDto? = null,
)

@Serializable
data class OrdersSummaryDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val total: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val paid: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val pending: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val cancelled: Long,
    @Serializable(with = FlexibleLongSerializer::class)
    val expired: Long,
)

@Serializable
data class OrdersResponse(
    val list: List<OrderDto>,
    val summary: OrdersSummaryDto,
)

// ==================== 8.4 GET /orders/:tradeNo ====================

@Serializable
data class OrderDetailResponse(
    val order: OrderDto,
)

// ==================== 8.5 POST /pay/create ====================

@Serializable
data class PayCreateRequest(
    val planId: Long,
    val paymentMethod: String,      // "alipay" / "wxpay" / "balance"
    val clientRequestId: String,    // UUID 幂等键
    val billingMonths: Long = 1,    // 1 / 3 / 12
)

/**
 * 支付创建响应（余额支付 / 网关支付两种形态共存，未用字段为 null）。
 */
@Serializable
data class PayCreateResponse(
    // 通用
    val tradeNo: String? = null,
    val traceId: String? = null,
    val reused: Boolean = false,
    val notice: String? = null,
    // 余额支付成功
    val status: String? = null,         // "paid"
    val order: OrderDto? = null,
    val bill: BillDto? = null,
    @Serializable(with = FlexibleNullableStringSerializer::class)
    val balance: String? = null,
    val paymentMethod: String? = null,
    // 网关支付
    val payUrl: String? = null,
    val params: Map<String, String>? = null,
    val html: String? = null,
    val payType: String? = null,
    val expireAt: String? = null,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    val remainingSeconds: Long? = null,
)

@Serializable
data class BillDto(
    @Serializable(with = FlexibleLongSerializer::class)
    val id: Long,
    val tradeNo: String,
    @Serializable(with = FlexibleStringSerializer::class)
    val amount: String,
    val type: String? = null,
    val category: String? = null,
    val title: String? = null,
    val status: String? = null,
    val paymentMethod: String? = null,
    val payTime: String? = null,
    val expireAt: String? = null,
)

// ==================== 8.6 POST /pay/repay ====================

@Serializable
data class PayRepayRequest(
    val tradeNo: String,
    val paymentMethod: String,
    val clientRequestId: String,
)

// 响应同 PayCreateResponse

// ==================== 8.7 POST /pay/query ====================

@Serializable
data class PayQueryRequest(val tradeNo: String)

@Serializable
data class PayQueryResponse(
    val status: String,             // "paid" / "pending" / "expired"
    val order: OrderDto? = null,
    val traceId: String? = null,
    @Serializable(with = FlexibleNullableLongSerializer::class)
    val remainingSeconds: Long? = null,
)

// ==================== 8.8 POST /pay/cancel ====================

@Serializable
data class PayCancelRequest(val tradeNo: String)
