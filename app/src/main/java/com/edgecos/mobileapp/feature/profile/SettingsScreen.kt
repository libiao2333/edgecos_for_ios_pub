package com.edgecos.mobileapp.feature.profile

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.edgecos.mobileapp.common.NotificationSettings
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.theme.ThemeMode
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.components.IOSListRow
import com.edgecos.mobileapp.design.components.IOSSegmentedControl
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.design.theme.IOSBlue
import com.edgecos.mobileapp.design.theme.IOSGreen
import com.edgecos.mobileapp.design.theme.IOSOrange
import com.edgecos.mobileapp.design.theme.IOSPurple
import com.edgecos.mobileapp.design.theme.IOSRed
import com.edgecos.mobileapp.media.LinePreferenceManager
import com.edgecos.mobileapp.media.TransferLine

/** Android 16（API 36）起支持 Live Updates（实时活动 / 流体云） */
private const val LIVE_UPDATES_API = 36

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateAbout: () -> Unit,
) {
    val themeStore = ServiceLocator.themeStore
    val currentMode by themeStore.mode.collectAsState()

    val themeOptions = ThemeMode.entries
    val themeLabels = themeOptions.map { it.displayName }
    val selectedIndex = themeOptions.indexOf(currentMode).coerceAtLeast(0)

    val context = LocalContext.current

    // ---- 通知权限 / 实时活动（Live Updates）状态 ----
    var notificationsEnabled by remember {
        mutableStateOf(NotificationSettings.areNotificationsEnabled(context))
    }
    var promotedEnabled by remember {
        mutableStateOf(NotificationSettings.canPostPromotedNotifications(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted
        // 被拒绝（含"不再询问"）时引导到系统设置，让用户自己决定是否开启
        if (!granted) NotificationSettings.openNotificationSettings(context)
    }
    // 从系统设置返回后刷新状态（用户可能在设置里改了通知/实时活动开关）
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationSettings.areNotificationsEnabled(context)
                promotedEnabled = NotificationSettings.canPostPromotedNotifications(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 线路偏好（与网页端 Preferences.jsx 一致）：每次询问 / 默认 CDN / 默认免费
    var previewLine by remember { mutableStateOf(LinePreferenceManager.previewLine(context)) }
    var downloadLine by remember { mutableStateOf(LinePreferenceManager.downloadLine(context)) }
    var editingLineTarget by remember { mutableStateOf<LineSettingTarget?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "设置", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xl),
        ) {
            // 外观：iOS 分段控件
            Column(
                modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
            ) {
                Text(
                    text = "外观".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
                IOSSegmentedControl(
                    options = themeLabels,
                    selectedIndex = selectedIndex,
                    onSelect = { index -> themeStore.setMode(themeOptions[index]) },
                )
            }

            // 文件传输（线路偏好，与网页端 Preferences.jsx 一致）
            IOSGroupedList(
                title = "文件传输"
            ) {
                IOSListRow(
                    title = "预览线路偏好",
                    leadingIcon = Icons.Outlined.Visibility,
                    iconColor = IOSBlue,
                    trailingText = previewLine.displayLabel,
                    showDivider = true,
                    onClick = { editingLineTarget = LineSettingTarget.PREVIEW },
                )
                IOSListRow(
                    title = "下载线路偏好",
                    leadingIcon = Icons.Outlined.Download,
                    iconColor = IOSGreen,
                    trailingText = downloadLine.displayLabel,
                    showDivider = false,
                    onClick = { editingLineTarget = LineSettingTarget.DOWNLOAD },
                )
            }

            // 通知（上传/下载进度实时展示 + Android 16 实时活动）
            IOSGroupedList(
                title = "通知"
            ) {
                IOSListRow(
                    title = "通知权限",
                    leadingIcon = Icons.Outlined.Notifications,
                    iconColor = IOSRed,
                    trailingText = if (notificationsEnabled) "已开启" else "未开启",
                    showDivider = Build.VERSION.SDK_INT >= LIVE_UPDATES_API,
                    onClick = {
                        if (!notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // 先走运行时权限弹窗；被拒绝时在回调里引导到系统设置
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        } else {
                            NotificationSettings.openNotificationSettings(context)
                        }
                    },
                )
                // Android 16+：实时活动（Live Updates / 流体云）开关状态
                if (Build.VERSION.SDK_INT >= LIVE_UPDATES_API) {
                    IOSListRow(
                        title = "实时活动",
                        leadingIcon = Icons.Outlined.Bolt,
                        iconColor = IOSOrange,
                        trailingText = if (promotedEnabled) "已允许" else "未允许",
                        showDivider = false,
                        onClick = {
                            // 与欢太工具箱一致：未允许时先提示，再直达系统的「实时活动」设置页
                            if (!promotedEnabled) {
                                Toast.makeText(
                                    context,
                                    "请先开启「实时活动」（流体云）",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            NotificationSettings.openPromotionSettings(context)
                        },
                    )
                }
            }

            // 账号安全（Apple HIG 彩色图标）
            IOSGroupedList(title = "账号") {
                IOSListRow(
                    title = "修改密码",
                    leadingIcon = Icons.Outlined.Lock,
                    iconColor = IOSBlue,
                    showDivider = true,
                    onClick = { /* TODO */ },
                )
                IOSListRow(
                    title = "绑定邮箱",
                    leadingIcon = Icons.Outlined.Mail,
                    iconColor = IOSGreen,
                    showDivider = true,
                    onClick = { /* TODO */ },
                )
                IOSListRow(
                    title = "实名认证",
                    leadingIcon = Icons.Outlined.VerifiedUser,
                    iconColor = IOSPurple,
                    showDivider = false,
                    onClick = { /* TODO */ },
                )
            }

            // 关于
            IOSGroupedList(title = "关于") {
                IOSListRow(
                    title = "关于",
                    leadingIcon = Icons.Outlined.Description,
                    iconColor = IOSOrange,
                    showDivider = false,
                    onClick = onNavigateAbout,
                )
            }

            // 退出登录（tinted error 风格按钮）
            Column(
                modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LogoutButton(onClick = {
                    ServiceLocator.authRepository.logout()
                    onBack()
                })
            }

            // 线路偏好选择弹窗（对应网页端 Preferences.jsx 的下拉选择）
            editingLineTarget?.let { target ->
                LinePreferenceDialog(
                    title = if (target == LineSettingTarget.PREVIEW) "预览线路偏好" else "下载线路偏好",
                    current = if (target == LineSettingTarget.PREVIEW) previewLine else downloadLine,
                    onSelect = { line ->
                        if (target == LineSettingTarget.PREVIEW) {
                            previewLine = line
                            LinePreferenceManager.setPreviewLine(context, line)
                        } else {
                            downloadLine = line
                            LinePreferenceManager.setDownloadLine(context, line)
                        }
                        editingLineTarget = null
                    },
                    onDismiss = { editingLineTarget = null },
                )
            }
        }
    }
}

/**
 * 退出登录按钮：复用 IOSButton Tinted 视觉结构，但使用 error 色（containerColor=error.copy(0.15f)）。
 */
@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
            .padding(horizontal = EdgeSpacing.lg, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "退出登录",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 线路偏好设置目标（对应网页端 Preferences 的两条线路设置项） */
private enum class LineSettingTarget { PREVIEW, DOWNLOAD }

/** 线路偏好显示名（与网页端 Preferences.jsx 选项一致） */
private val TransferLine.displayLabel: String
    get() = when (this) {
        TransferLine.ASK -> "每次询问"
        TransferLine.CDN -> "默认 CDN 线路"
        TransferLine.FREE -> "默认免费线路"
    }

/**
 * 线路偏好选择弹窗（对应网页端 Preferences.jsx 的 Select 下拉器）。
 */
@Composable
private fun LinePreferenceDialog(
    title: String,
    current: TransferLine,
    onSelect: (TransferLine) -> Unit,
    onDismiss: () -> Unit,
) {
    EdgeAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = null,
        onConfirm = null,
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column {
                TransferLine.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(option) }
                            .padding(horizontal = EdgeSpacing.sm, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.displayLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        if (option == current) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}
