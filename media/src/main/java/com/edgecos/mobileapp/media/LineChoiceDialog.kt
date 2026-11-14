package com.edgecos.mobileapp.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 线路选择弹窗（对齐网页端 Drive.jsx 的「选择预览线路 / 选择下载线路」弹窗）。
 *
 * 网页端行为：
 * - 偏好为「每次询问」时，点击预览/下载先弹出本弹窗
 * - 「CDN 极速线路」速度更快，计入流量；「免费线路」不计流量，单连接限速
 * - 勾选「记住选择」后写入偏好，后续不再询问（可在偏好设置中修改）
 *
 * @param title 弹窗标题（「选择预览线路」/「选择下载线路」）
 * @param onChoose 选择回调：选中的线路 + 是否记住了选择
 * @param onDismiss 取消回调（不预览/不下载）
 */
@Composable
fun LineChoiceDialog(
    title: String,
    onChoose: (line: TransferLine, remember: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var remember by remember { mutableStateOf(false) }

    EdgeAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = null,
        onConfirm = null,
        dismissText = "取消",
        onDismiss = onDismiss,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm)) {
                LineOptionRow(
                    title = "CDN 极速线路",
                    subtitle = "速度更快，计入流量",
                    icon = Icons.Outlined.Bolt,
                    onClick = { onChoose(TransferLine.CDN, remember) },
                )
                LineOptionRow(
                    title = "免费线路",
                    subtitle = "不计流量，单连接限速",
                    icon = Icons.Outlined.Eco,
                    onClick = { onChoose(TransferLine.FREE, remember) },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { remember = !remember },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = remember,
                        onCheckedChange = { remember = it },
                    )
                    Text(
                        text = "记住选择，可在「设置」中修改",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

/** 线路选项行：图标 + 标题 + 说明，整行可点。 */
@Composable
private fun LineOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = EdgeSpacing.md, vertical = EdgeSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
