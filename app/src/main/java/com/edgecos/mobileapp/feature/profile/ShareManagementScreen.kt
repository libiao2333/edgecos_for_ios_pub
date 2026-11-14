package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.data.remote.ClientConfig
import com.edgecos.mobileapp.design.components.EdgeCard
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeErrorState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType

/**
 * 分享管理页：列出根目录下已分享的文件，可复制分享链接或取消分享。
 *
 * 当前仅展示根目录下有 activeShare 的文件。后端未提供独立的分享列表端点，
 * 文件分享状态嵌入在 FileNode.activeShare 中。
 */
@Composable
fun ShareManagementScreen(
    onBack: () -> Unit,
    viewModel: ShareManagementViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
        ) {
            IOSNavBar(title = "分享管理", onBack = onBack)

            when {
                state.isLoading -> EdgeLoadingState(modifier = Modifier.fillMaxWidth())
                state.error != null -> EdgeErrorState(
                    title = "加载失败",
                    message = state.error,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.fillMaxWidth(),
                )
                state.sharedFiles.isEmpty() -> EdgeEmptyState(
                    title = "暂无分享",
                    subtitle = "在文件页长按文件可创建分享",
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                    contentPadding = PaddingValues(
                        horizontal = EdgeSpacing.lg,
                        vertical = EdgeSpacing.sm,
                    ),
                ) {
                    items(state.sharedFiles, key = { it.id }) { file ->
                        SharedFileCard(
                            file = file,
                            onCopyLink = {
                                val share = file.activeShare
                                if (share != null) {
                                    val webBaseUrl = ClientConfig.baseUrl.removeSuffix("api/")
                                    val url = "$webBaseUrl/s/${share.shareId}"
                                    val text = buildString {
                                        append(url)
                                        share.extractionCode?.let { code ->
                                            append("\n提取码：$code")
                                        }
                                    }
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                            },
                            onClose = { viewModel.closeShare(file.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedFileCard(
    file: FileNode,
    onCopyLink: () -> Unit,
    onClose: () -> Unit,
) {
    EdgeCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
            ) {
                Icon(
                    imageVector = if (file.type == FileType.DIRECTORY) Icons.Outlined.Folder
                    else Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.basename,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    file.activeShare?.let { share ->
                        val detail = buildString {
                            share.extractionCode?.let { append("提取码：$it  ") }
                            share.expiration?.let { append("过期：$it") }
                            if (isEmpty()) append("永久有效")
                        }
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.md),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IOSButton(
                    text = "复制链接",
                    onClick = onCopyLink,
                    style = IOSButtonStyle.Tinted,
                    leadingIcon = Icons.Outlined.ContentCopy,
                )
                IOSButton(
                    text = "取消",
                    onClick = onClose,
                    style = IOSButtonStyle.Plain,
                    modifier = Modifier.padding(start = EdgeSpacing.sm),
                )
            }
        }
    }
}
