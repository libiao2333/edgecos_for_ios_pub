package com.edgecos.mobileapp.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.edgecos.mobileapp.data.remote.UpdateResult
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 手动检查更新结果弹窗（关于页"检查更新"使用）。
 *
 * 与全局强制更新弹窗（[com.edgecos.mobileapp.UpdateGuard]）共用同一套
 * [EdgeAlertDialog] 样式，按钮文案统一为「去更新 / 取消」。
 *
 * - [UpdateResult.UpdateAvailable]：提示有新版本，引导前往下载（可取消）
 * - [UpdateResult.UpToDate]：已是最新版本
 * - [UpdateResult.CheckFailed]：检查失败，提示重试
 */
@Composable
fun UpdateResultDialog(
    result: UpdateResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    when (result) {
        is UpdateResult.UpdateAvailable -> {
            EdgeAlertDialog(
                onDismissRequest = onDismiss,
                title = "发现新版本",
                message = "最新版本 v${result.remoteVersion}（构建 ${result.remoteCode}）已发布。" +
                    "安装包将在后台下载，完成后自动打开安装程序。",
                icon = Icons.Outlined.SystemUpdate,
                confirmText = "去更新",
                onConfirm = {
                    UpdateDownloadManager.start(context, result)
                    onDismiss()
                },
                dismissText = "取消",
                onDismiss = onDismiss,
            )
        }

        is UpdateResult.UpToDate -> {
            EdgeAlertDialog(
                onDismissRequest = onDismiss,
                title = "检查更新",
                message = "当前已是最新版本。",
                confirmText = "好",
                onConfirm = onDismiss,
            )
        }

        is UpdateResult.CheckFailed -> {
            EdgeAlertDialog(
                onDismissRequest = onDismiss,
                title = "检查更新",
                message = "检查更新失败，请检查网络后重试。",
                confirmText = "好",
                onConfirm = onDismiss,
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xxs),
                    ) {
                        Text(
                            text = "错误信息：${result.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
            )
        }
    }
}
