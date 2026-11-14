package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.Order
import com.edgecos.mobileapp.domain.model.OrderStatus

private val REPAY_METHODS = listOf(
    "balance" to "余额支付",
    "alipay" to "支付宝",
    "wxpay" to "微信支付",
)

/**
 * 订单详情底部弹窗：展示订单详情 + 状态相关操作（取消 / 重新支付 / 查询状态）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailSheet(
    order: Order,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    onRepay: (paymentMethod: String) -> Unit,
    onQuery: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRepayOptions by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("balance") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EdgeSpacing.lg)
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            Text(
                text = "订单详情",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            DetailRow("订单号", order.tradeNo)
            DetailRow("套餐", order.plan?.name ?: "套餐 #${order.planId}")
            DetailRow("金额", "¥${order.amount}")
            DetailRow("状态", order.status.label())
            DetailRow("计费月数", "${order.billingMonths} 个月")
            order.paymentMethod?.let { DetailRow("支付方式", it) }
            order.payTime?.let { DetailRow("支付时间", it) }
            order.expireAt?.let { DetailRow("过期时间", it) }

            // 操作按钮
            when (order.status) {
                OrderStatus.PENDING -> {
                    if (showRepayOptions) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                REPAY_METHODS.forEachIndexed { index, (value, label) ->
                                    val isSelected = selectedMethod == value
                                    val isLast = index == REPAY_METHODS.size - 1
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 44.dp)
                                            .clickable { selectedMethod = value }
                                            .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    if (!isLast) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = EdgeSpacing.lg),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant,
                                        )
                                    }
                                }
                            }
                        }
                        IOSButton(
                            text = "确认支付",
                            onClick = { onRepay(selectedMethod) },
                            modifier = Modifier.fillMaxWidth(),
                            style = IOSButtonStyle.Filled,
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                        ) {
                            IOSButton(
                                text = "取消订单",
                                onClick = onCancel,
                                modifier = Modifier.weight(1f),
                                style = IOSButtonStyle.Tinted,
                            )
                            IOSButton(
                                text = "立即支付",
                                onClick = { showRepayOptions = true },
                                modifier = Modifier.weight(1f),
                                style = IOSButtonStyle.Filled,
                            )
                        }
                    }
                }
                OrderStatus.PAID -> {
                    IOSButton(
                        text = "刷新支付状态",
                        onClick = onQuery,
                        modifier = Modifier.fillMaxWidth(),
                        style = IOSButtonStyle.Tinted,
                    )
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EdgeSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PAID -> "已支付"
    OrderStatus.PENDING -> "待支付"
    OrderStatus.CANCELLED -> "已取消"
    OrderStatus.EXPIRED -> "已过期"
    OrderStatus.UNKNOWN -> "未知"
}
