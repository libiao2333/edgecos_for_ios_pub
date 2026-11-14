package com.edgecos.mobileapp.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeErrorState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.components.IOSSegmentedControl
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.Order
import com.edgecos.mobileapp.domain.model.OrderStatus
import kotlinx.coroutines.launch

/**
 * 订单列表页：展示所有订单，可查看详情、取消、重新支付、查询状态。
 */
@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    viewModel: OrderListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var detailOrder by remember { mutableStateOf<Order?>(null) }
    var selectedFilter by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
        ) {
            IOSNavBar(title = "订单", onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.lg)
                    .padding(bottom = EdgeSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
            ) {
                // 订单状态筛选
                IOSSegmentedControl(
                    options = listOf("全部", "待支付", "已支付", "已取消"),
                    selectedIndex = selectedFilter,
                    onSelect = { selectedFilter = it },
                )

                // 错误提示
                actionError?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                val filteredOrders = when (selectedFilter) {
                    1 -> state.orders.list.filter { it.status == OrderStatus.PENDING }
                    2 -> state.orders.list.filter { it.status == OrderStatus.PAID }
                    3 -> state.orders.list.filter { it.status == OrderStatus.CANCELLED }
                    else -> state.orders.list
                }

                when {
                    state.isLoading -> EdgeLoadingState(modifier = Modifier.fillMaxWidth())
                    state.error != null -> EdgeErrorState(
                        title = "加载失败",
                        message = state.error,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.orders.list.isEmpty() -> EdgeEmptyState(
                        title = "暂无订单",
                        subtitle = "在套餐页购买套餐后订单会显示在此",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    filteredOrders.isEmpty() -> EdgeEmptyState(
                        title = "该状态下暂无订单",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            OrderCard(
                                order = order,
                                onClick = { detailOrder = order },
                                onCancel = { viewModel.cancelOrder(order.tradeNo) },
                                onRepay = {
                                    scope.launch {
                                        val result = viewModel.repayOrder(order.tradeNo, "alipay")
                                        result?.payUrl?.let { url ->
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        }
                                    }
                                },
                                onQuery = {
                                    scope.launch {
                                        viewModel.queryPayment(order.tradeNo)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // 订单详情弹窗
    detailOrder?.let { order ->
        OrderDetailSheet(
            order = order,
            onDismiss = { detailOrder = null },
            onCancel = {
                viewModel.cancelOrder(order.tradeNo)
                detailOrder = null
            },
            onRepay = { paymentMethod ->
                scope.launch {
                    val result = viewModel.repayOrder(order.tradeNo, paymentMethod)
                    result?.payUrl?.let { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                    if (result?.isPaid == true) {
                        detailOrder = null
                    }
                }
            },
            onQuery = {
                scope.launch {
                    viewModel.queryPayment(order.tradeNo)
                }
            },
        )
    }
}

@Composable
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onRepay: () -> Unit,
    onQuery: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EdgeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.plan?.name ?: "套餐 #${order.planId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = order.status.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = order.status.color(),
                )
            }
            Text(
                text = "订单号 ${order.tradeNo}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "¥${order.amount}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            order.payTime?.let { payTime ->
                Text(
                    text = "支付时间 $payTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            order.expireAt?.let { exp ->
                Text(
                    text = "过期时间 $exp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 操作按钮（根据状态显示）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
            ) {
                IOSButton(
                    text = "详情",
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    style = IOSButtonStyle.Tinted,
                )
                when (order.status) {
                    OrderStatus.PENDING -> {
                        IOSButton(
                            text = "去支付",
                            onClick = onRepay,
                            modifier = Modifier.weight(1f),
                            style = IOSButtonStyle.Filled,
                        )
                        IOSButton(
                            text = "取消",
                            onClick = onCancel,
                            modifier = Modifier.weight(1f),
                            style = IOSButtonStyle.Tinted,
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}

private fun OrderStatus.label(): String = when (this) {
    OrderStatus.PAID -> "已支付"
    OrderStatus.PENDING -> "待支付"
    OrderStatus.CANCELLED -> "已取消"
    OrderStatus.EXPIRED -> "已过期"
    OrderStatus.UNKNOWN -> "未知"
}

@Composable
private fun OrderStatus.color(): androidx.compose.ui.graphics.Color = when (this) {
    OrderStatus.PAID -> MaterialTheme.colorScheme.primary
    OrderStatus.PENDING -> MaterialTheme.colorScheme.tertiary
    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    OrderStatus.EXPIRED -> MaterialTheme.colorScheme.error
    OrderStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}
