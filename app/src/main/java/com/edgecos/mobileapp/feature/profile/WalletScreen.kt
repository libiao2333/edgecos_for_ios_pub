package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 钱包页：MVP 占位。
 * TODO: 接入 GET /wallet 显示余额、交易记录、充值入口。
 */
@Composable
fun WalletScreen(
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "钱包", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .padding(bottom = EdgeSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xl),
            ) {
                // 余额卡片：iOS 圆角卡片（Surface surface shapes.medium shadowElevation=1.dp）
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EdgeSpacing.lg),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EdgeSpacing.lg),
                    ) {
                        Text(
                            text = "钱包余额",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "¥ 0.00",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = EdgeSpacing.sm),
                        )
                    }
                }

                // 充值（Filled 主操作）/ 提现（Tinted 次操作）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EdgeSpacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
                ) {
                    IOSButton(
                        text = "充值",
                        onClick = { /* TODO: 跳转充值 */ },
                        style = IOSButtonStyle.Filled,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    )
                    IOSButton(
                        text = "提现",
                        onClick = { /* TODO: 跳转提现 */ },
                        style = IOSButtonStyle.Tinted,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                    )
                }

                // 交易记录分组列表（iOS Grouped List 风格）
                IOSGroupedList(title = "交易记录") {
                    EdgeEmptyState(
                        title = "暂无交易记录",
                        subtitle = "充值与消费记录将在后续版本接入",
                    )
                }
            }
        }
    }
}
