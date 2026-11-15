package com.edgecos.mobileapp.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeErrorState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.MyPlan
import com.edgecos.mobileapp.domain.model.PaymentResult
import com.edgecos.mobileapp.domain.model.Plan
import com.edgecos.mobileapp.domain.model.TrafficStatus
import com.edgecos.mobileapp.domain.util.formatBytes

/**
 * 我的套餐页：iOS HIG 风格。
 *
 * - 顶部 Large Title 导航栏（含返回按钮）
 * - 当前套餐：iOS 圆角卡片（存储/流量进度条）
 * - 套餐列表：iOS 圆角卡片 + Filled 按钮
 */
@Composable
fun PlanScreen(
    onBack: () -> Unit,
    onNavigateOrders: () -> Unit = {},
    viewModel: PlanViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var purchasePlan by remember { mutableStateOf<Plan?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            IOSNavBar(
                title = "套餐",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
        ) {
            // 当前套餐卡片
            Column(
                modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
            ) {
                when {
                    state.myPlanLoading -> EdgeLoadingState(modifier = Modifier.fillMaxWidth())
                    state.myPlanError != null -> EdgeErrorState(
                        title = "加载失败",
                        message = state.myPlanError,
                        onRetry = { viewModel.loadMyPlan() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.myPlan != null -> CurrentPlanCard(
                        myPlan = state.myPlan!!,
                        onNavigateOrders = onNavigateOrders,
                    )
                }
            }

            // 套餐列表：每个套餐独立成卡，卡片之间有间距（优雅分离）
            when {
                state.plansLoading -> EdgeLoadingState(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = EdgeSpacing.lg),
                )
                state.plansError != null -> EdgeErrorState(
                    title = "加载失败",
                    message = state.plansError,
                    onRetry = { viewModel.loadPlans() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = EdgeSpacing.lg),
                )
                state.plans.isEmpty() -> EdgeEmptyState(
                    title = "暂无可购买套餐",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = EdgeSpacing.lg),
                )
                else -> {
                    // 小标题
                    Text(
                        text = "可购买套餐",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = EdgeSpacing.lg)
                            .padding(start = 4.dp, bottom = 2.dp),
                    )
                    // 每个套餐独立卡片
                    state.plans.forEach { plan ->
                        IOSGroupedList {
                            PlanCardContent(
                                plan = plan,
                                onPurchase = { purchasePlan = plan },
                            )
                        }
                    }
                }
            }
        }
    }

    // 购买弹窗
    purchasePlan?.let { plan ->
        PlanPurchaseSheet(
            plan = plan,
            onDismiss = { purchasePlan = null },
            onPaymentCreated = { result ->
                purchasePlan = null
                handlePaymentResult(context, result)
            },
        )
    }
}

/**
 * 统一处理支付结果：
 * - 余额支付成功：弹 toast（由调用方处理）
 * - 网关支付：调起浏览器打开 payUrl
 */
private fun handlePaymentResult(
    context: android.content.Context,
    result: PaymentResult,
) {
    if (result.isPaid) {
        // 余额支付成功，无需额外动作；UI 已刷新套餐状态
        return
    }
    result.payUrl?.let { url ->
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
}

/**
 * 当前套餐卡片：iOS 圆角卡片风格。
 */
@Composable
private fun CurrentPlanCard(
    myPlan: MyPlan,
    onNavigateOrders: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EdgeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = myPlan.plan.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                myPlan.expireTime?.let { exp ->
                    Text(
                        text = "到期 $exp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } ?: Text(
                    text = "永久",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 存储用量
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "存储",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${formatBytes(myPlan.used)} / ${formatBytes(myPlan.quota)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = {
                    val raw = if (myPlan.quota > 0L) {
                        myPlan.used.toFloat() / myPlan.quota.toFloat()
                    } else 0f
                    raw.coerceIn(0f, 1f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )

            // 流量用量
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "本月流量",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${formatBytes(myPlan.monthlyTrafficUsed)} / ${formatBytes(myPlan.trafficQuota)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { (myPlan.trafficUsagePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (myPlan.trafficStatus) {
                    TrafficStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                    TrafficStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                    TrafficStatus.EXHAUSTED -> MaterialTheme.colorScheme.error
                },
            )

            // 套餐外流量
            if (myPlan.extraTraffic > 0L) {
                Text(
                    text = "套餐外流量：${formatBytes(myPlan.extraTraffic)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = EdgeSpacing.xs),
                )
            }

            // 查看订单（iOS 风格 Tinted 按钮）
            IOSButton(
                text = "查看订单",
                onClick = onNavigateOrders,
                style = IOSButtonStyle.Tinted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.md),
            )
        }
    }
}

/**
 * 可购买套餐内容项：IOSGroupedList 内部使用（无独立 Surface，由 IOSGroupedList 提供卡片背景）。
 */
@Composable
private fun PlanCardContent(
    plan: Plan,
    onPurchase: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EdgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "¥${plan.price}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = "存储 ${formatBytes(plan.quota)} / 流量 ${formatBytes(plan.trafficQuota)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        plan.originalPrice?.let { orig ->
            Text(
                text = "原价 ¥$orig",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (plan.isTrafficPackage) {
            Text(
                text = "流量包",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        IOSButton(
            text = if (plan.contactOnly) "联系客服开通" else "立即购买",
            onClick = onPurchase,
            style = IOSButtonStyle.Filled,
            enabled = !plan.contactOnly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EdgeSpacing.sm),
        )
    }
}
