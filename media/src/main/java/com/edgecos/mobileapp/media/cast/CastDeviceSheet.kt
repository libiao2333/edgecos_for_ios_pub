package com.edgecos.mobileapp.media.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import kotlinx.coroutines.launch

/**
 * 投屏设备选择底部弹窗（DLNA）。
 *
 * 流程：
 * 1. 打开时自动搜索局域网中的 MediaRenderer 设备（SSDP），可手动刷新
 * 2. 点选设备 → 生成免登录直链（TV 端无法携带 App 登录态）
 * 3. 通过 AVTransport SetAVTransportURI + Play 让 TV 开始播放
 * 4. 成功后回调 [onConnected]，由播放页接管后续远端控制
 *
 * @param file        待投屏的视频文件
 * @param onDismiss   关闭回调
 * @param onConnected 投屏成功回调（返回已连接的设备）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastDeviceSheet(
    file: FileNode,
    onDismiss: () -> Unit,
    onConnected: (CastDevice) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var devices by remember { mutableStateOf<List<CastDevice>>(emptyList()) }
    var isDiscovering by remember { mutableStateOf(true) }
    var connectingDevice by remember { mutableStateOf<CastDevice?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun runDiscovery() {
        isDiscovering = true
        error = null
        devices = DlnaDiscovery.discover(context)
        isDiscovering = false
    }

    fun connect(device: CastDevice) {
        if (connectingDevice != null) return
        scope.launch {
            connectingDevice = device
            error = null
            // TV 无法携带 App 登录态，必须使用免登录直链拉流
            val linkResult = ServiceLocator.directLinkRepository.generateDirectLink(fileId = file.id)
            when (linkResult) {
                is EdgeResult.Failure -> {
                    error = "直链生成失败：${linkResult.error.message}"
                    connectingDevice = null
                }
                is EdgeResult.Success -> {
                    val ok = DlnaController.playUrl(
                        controlUrl = device.controlUrl,
                        url = linkResult.data.directUrl,
                        title = file.basename,
                    )
                    connectingDevice = null
                    if (ok) {
                        onConnected(device)
                    } else {
                        error = "无法控制「${device.name}」，请确认设备支持 DLNA 投屏"
                    }
                }
            }
        }
    }

    // 打开时自动搜索一次
    LaunchedEffect(Unit) { runDiscovery() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = EdgeSpacing.lg)
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 标题行：标题 + 刷新
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cast,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(EdgeSpacing.sm))
                Text(
                    text = "投屏到电视",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                IOSButton(
                    text = "刷新",
                    onClick = { scope.launch { runDiscovery() } },
                    style = IOSButtonStyle.Plain,
                    enabled = !isDiscovering && connectingDevice == null,
                )
            }

            // 说明
            Text(
                text = "确保手机与电视连接同一 Wi-Fi。投屏将使用免登录直链拉流，会消耗账户流量。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 错误提示
            error?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when {
                // 搜索中
                isDiscovering -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EdgeSpacing.xl),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(EdgeSpacing.md))
                    Text(
                        text = "正在搜索局域网中的设备…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 未找到设备
                devices.isEmpty() -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = EdgeSpacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
                ) {
                    Text(
                        text = "未找到投屏设备",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "请确认电视已开机、与手机处于同一 Wi-Fi，且已开启投屏 / 无线显示功能",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                // 设备列表
                else -> {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    devices.forEach { device ->
                        CastDeviceRow(
                            device = device,
                            isConnecting = connectingDevice == device,
                            enabled = connectingDevice == null,
                            onClick = { connect(device) },
                        )
                    }
                }
            }
        }
    }
}

/** 单个投屏设备行：TV 图标 + 名称 + 地址 + 连接状态。 */
@Composable
private fun CastDeviceRow(
    device: CastDevice,
    isConnecting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = EdgeSpacing.md, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // TV 图标（品牌色 12% 底）
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tv,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = device.host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    text = "投屏",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
