package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.IOSLargeTitleNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.design.theme.IOSBlue
import com.edgecos.mobileapp.design.theme.IOSGreen
import com.edgecos.mobileapp.design.theme.IOSOrange
import com.edgecos.mobileapp.design.theme.IOSPink
import com.edgecos.mobileapp.design.theme.IOSPurple
import com.edgecos.mobileapp.design.theme.IOSTeal
import com.edgecos.mobileapp.domain.model.MyPlan
import com.edgecos.mobileapp.domain.model.TrafficStatus
import com.edgecos.mobileapp.domain.util.formatBytes

/**
 * 个人页：iOS HIG Inset Grouped List 风格。
 *
 * - 顶部大标题（Large Title 34pt Bold）
 * - 分组列表（圆角卡片，组标题大写小字）
 * - 列表项 44dp 最小高度，前导图标用圆角方块背景
 * - chevron 右箭头作为导航指示
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateAbout: () -> Unit,
    onNavigatePlan: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateWorkOrder: () -> Unit,
    onNavigateAnnouncements: () -> Unit,
    onNavigateShareManagement: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateOrders: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(),
) {
    val user by viewModel.user.collectAsState()
    val myPlan by viewModel.myPlan.collectAsState()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // iOS Large Title 导航栏（嵌套子页面：状态栏 inset 由外层 MainScreen 的 Scaffold 处理）
            IOSLargeTitleNavBar(title = "我的", applyStatusBarInset = false)
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xl),
            ) {
                // 账户：用户信息 + 套餐用量（存储 + 流量，整合到一个分组卡片中）
                IOSGroup(title = "账户") {
                    UserProfileContent(
                        username = user?.username ?: "未登录",
                        email = user?.email ?: "未绑定邮箱",
                        isAdmin = user?.role == "admin",
                    )
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    myPlan?.let { PlanUsageContent(it) }
                }

                // 资源管理（Apple HIG 彩色图标）
                IOSGroup(
                    title = "资源管理",
                    items = listOf(
                        GroupItem("分享管理", Icons.Outlined.Share, IOSBlue, onClick = onNavigateShareManagement),
                        GroupItem("工单中心", Icons.Outlined.Description, IOSOrange, onClick = onNavigateWorkOrder),
                        GroupItem("公告通知", Icons.Outlined.Notifications, IOSPink, onClick = onNavigateAnnouncements),
                    ),
                )

                // 套餐与账单（Apple HIG 彩色图标）
                IOSGroup(
                    title = "套餐与账单",
                    items = listOf(
                        GroupItem("我的套餐", Icons.Outlined.VerifiedUser, IOSPurple, onClick = onNavigatePlan),
                        GroupItem("我的订单", Icons.Outlined.ReceiptLong, IOSTeal, onClick = onNavigateOrders),
                        GroupItem("钱包", Icons.Outlined.Wallet, IOSGreen, onClick = onNavigateWallet),
                    ),
                )

                // 设置
                IOSGroup(
                    title = "设置",
                    items = listOf(
                        GroupItem("设置", Icons.Outlined.Settings, IOSBlue, onClick = onNavigateSettings),
                    ),
                )
            }

            // 底部留白：与浮动 TabBar 等高（TabBar 56dp + 底部边距 12dp + 导航栏安全区）
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp + EdgeSpacing.md),
            )
        }
    }
}

/**
 * 用户信息内容（账户分组内的上半部分）：头像 + 用户名 + 邮箱。
 *
 * 无外层卡片，由 IOSGroup 提供卡片容器。
 */
@Composable
private fun UserProfileContent(
    username: String,
    email: String,
    isAdmin: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EdgeSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
    ) {
        // 头像：圆形 + 系统色背景
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isAdmin) {
                Text(
                    text = "管理员",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 套餐用量内容（账户分组内的下半部分）：套餐名 + 存储进度条 + 流量进度条。
 *
 * 用 MyPlan 数据替换原来的纯存储空间卡片，同时展示存储与流量用量。
 * 无外层卡片，由 IOSGroup 提供卡片容器。
 */
@Composable
private fun PlanUsageContent(
    myPlan: MyPlan,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(EdgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
    ) {
        // 套餐名 + 到期时间
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
        UsageRow(
            label = "存储",
            usedText = formatBytes(myPlan.used),
            totalText = formatBytes(myPlan.quota),
            progress = if (myPlan.quota > 0L) {
                (myPlan.used.toFloat() / myPlan.quota.toFloat()).coerceIn(0f, 1f)
            } else 0f,
            color = MaterialTheme.colorScheme.primary,
        )

        // 本月流量用量
        UsageRow(
            label = "本月流量",
            usedText = formatBytes(myPlan.monthlyTrafficUsed),
            totalText = formatBytes(myPlan.trafficQuota),
            progress = (myPlan.trafficUsagePercent / 100.0).toFloat().coerceIn(0f, 1f),
            color = when (myPlan.trafficStatus) {
                TrafficStatus.HEALTHY -> MaterialTheme.colorScheme.primary
                TrafficStatus.WARNING -> MaterialTheme.colorScheme.tertiary
                TrafficStatus.EXHAUSTED -> MaterialTheme.colorScheme.error
            },
        )

        // 套餐外流量（若有）
        if (myPlan.extraTraffic > 0L) {
            Text(
                text = "套餐外流量：${formatBytes(myPlan.extraTraffic)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = EdgeSpacing.xs),
            )
        }
    }
}

/**
 * 用量行：标签 + 已用/总量 + 进度条。
 */
@Composable
private fun UsageRow(
    label: String,
    usedText: String,
    totalText: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = EdgeSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$usedText / $totalText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
        )
    }
}

/**
 * iOS Inset Grouped List 分组。
 */
private data class GroupItem(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit,
)

/**
 * 分组卡片容器：统一的标题 + 圆角卡片包裹。
 *
 * 提供两种用法：
 * 1. items 列表（标准导航行）
 * 2. content 自定义内容（用于账户区等非标准布局）
 *
 * 卡片视觉强化：增加 shadowElevation 与轻微 border，确保在浅色/深色背景下均清晰可辨。
 */
@Composable
private fun IOSGroup(
    title: String,
    modifier: Modifier = Modifier,
    items: List<GroupItem> = emptyList(),
    content: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EdgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 组标题（iOS 风格：大写小字）
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
        )
        // 卡片容器（Apple HIG：白色卡片放在浅灰分组背景上，靠色差建立层次）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (content != null) {
                    content()
                } else {
                    items.forEachIndexed { index, item ->
                        IOSGroupRow(
                            title = item.title,
                            icon = item.icon,
                            iconColor = item.iconColor,
                            onClick = item.onClick,
                            showDivider = index < items.size - 1,
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS 风格分组列表项。
 */
@Composable
private fun IOSGroupRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 前导图标：Apple HIG 彩色图标 + 对应色调 15% tinted 背景
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            // chevron 右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = EdgeSpacing.lg + 28.dp + EdgeSpacing.md),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
