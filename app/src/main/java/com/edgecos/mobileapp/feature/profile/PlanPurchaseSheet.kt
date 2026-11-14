package com.edgecos.mobileapp.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSSegmentedControl
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.Plan
import com.edgecos.mobileapp.domain.util.formatBytes

private val BILLING_MONTHS_OPTIONS = listOf(1L, 3L, 12L)
private val PAYMENT_METHODS = listOf(
    "balance" to "余额支付",
    "alipay" to "支付宝",
    "wxpay" to "微信支付",
)

/**
 * 套餐购买底部弹窗：选择计费月数 + 支付方式 → 创建支付订单。
 *
 * - 余额支付：成功后 [PaymentResult.isPaid]=true，调用方刷新套餐状态
 * - 网关支付：[PaymentResult.payUrl] 非空，调用方打开浏览器完成支付
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanPurchaseSheet(
    plan: Plan,
    onDismiss: () -> Unit,
    onPaymentCreated: (com.edgecos.mobileapp.domain.model.PaymentResult) -> Unit,
    viewModel: PlanViewModel = viewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsState()
    val paymentResult by viewModel.paymentResult.collectAsState()
    val paymentError by viewModel.paymentError.collectAsState()
    val context = LocalContext.current

    var billingMonths by remember { mutableStateOf(1L) }
    var paymentMethod by remember { mutableStateOf("balance") }

    // 支付结果回调
    androidx.compose.runtime.LaunchedEffect(paymentResult) {
        paymentResult?.let {
            onPaymentCreated(it)
            viewModel.consumePaymentResult()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.consumePaymentResult()
            onDismiss()
        },
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
                text = "购买 ${plan.name}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // 套餐概要
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(EdgeSpacing.lg)) {
                    Text(
                        text = "存储 ${formatBytes(plan.quota)} / 流量 ${formatBytes(plan.trafficQuota)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "单价 ¥${plan.price}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 计费周期
            Text(
                text = "计费周期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IOSSegmentedControl(
                options = BILLING_MONTHS_OPTIONS.map { if (it == 1L) "1 个月" else "$it 个月" },
                selectedIndex = BILLING_MONTHS_OPTIONS.indexOf(billingMonths),
                onSelect = { index -> billingMonths = BILLING_MONTHS_OPTIONS[index] },
            )

            // 支付方式（iOS Grouped List 风格）
            Text(
                text = "支付方式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                shadowElevation = 1.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PAYMENT_METHODS.forEachIndexed { index, (value, label) ->
                        val isSelected = paymentMethod == value
                        val isLast = index == PAYMENT_METHODS.size - 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clickable { paymentMethod = value }
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

            // 错误提示
            paymentError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // 支付按钮：余额支付 Filled，网关支付 Tinted
            val payButtonStyle = if (paymentMethod == "balance") IOSButtonStyle.Filled else IOSButtonStyle.Tinted
            IOSButton(
                text = "立即支付",
                onClick = { viewModel.createPayment(plan.id, paymentMethod, billingMonths) },
                enabled = !state.paying,
                isLoading = state.paying,
                modifier = Modifier.fillMaxWidth(),
                style = payButtonStyle,
            )

            // 联系方式套餐（contactOnly）
            if (plan.contactOnly) {
                IOSButton(
                    text = "联系客服开通",
                    onClick = {
                        plan.contactEmail?.let { email ->
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = IOSButtonStyle.Tinted,
                    leadingIcon = Icons.Outlined.FlashOn,
                )
            }
        }
    }
}
