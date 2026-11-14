package com.edgecos.mobileapp.feature.transfer

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSLargeTitleNavBar
import com.edgecos.mobileapp.design.components.IOSSegmentedControl
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.media.getMimeType
import java.io.File

@Composable
fun TransferScreen(
    modifier: Modifier = Modifier,
    viewModel: TransferViewModel = viewModel(),
) {
    val transfers by viewModel.transfers.collectAsState()
    // 长按待删除的记录（弹确认框）
    var pendingDelete by remember { mutableStateOf<TransferItem?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // iOS Large Title 导航栏（嵌套子页面：状态栏 inset 由外层 MainScreen 的 Scaffold 处理）
            IOSLargeTitleNavBar(title = "传输", applyStatusBarInset = false)

            // 上传 / 下载 分页切换（与订单页同一个 iOS 分段控件，保证 UI 统一）
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.lg)
                    .padding(bottom = EdgeSpacing.lg),
            ) {
                IOSSegmentedControl(
                    options = listOf("上传", "下载"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            }

            val isUploadTab = selectedTab == 0
            val currentType = if (isUploadTab) TransferType.UPLOAD else TransferType.DOWNLOAD
            // 进行中（含失败待处理）与已完成分开分组展示
            val active = transfers.filter {
                it.type == currentType && it.status != TransferStatus.COMPLETED
            }
            val completed = transfers.filter {
                it.type == currentType && it.status == TransferStatus.COMPLETED
            }

            if (active.isEmpty() && completed.isEmpty()) {
                EdgeEmptyState(
                    title = if (isUploadTab) "暂无上传任务" else "暂无下载任务",
                    subtitle = if (isUploadTab) "上传文件后这里会显示进度" else "下载文件后这里会显示进度",
                    icon = Icons.Outlined.CloudDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = EdgeSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xl),
                ) {
                    if (active.isNotEmpty()) {
                        item(key = "__active__") {
                            TransferGroup(
                                title = if (isUploadTab) "上传中" else "下载中",
                                items = active,
                                onLongClick = { pendingDelete = it },
                            )
                        }
                    }
                    if (completed.isNotEmpty()) {
                        item(key = "__completed__") {
                            TransferGroup(
                                title = "已完成",
                                items = completed,
                                onLongClick = { pendingDelete = it },
                            )
                        }
                    }
                    // 底部留白：与浮动 TabBar 等高（TabBar 56dp + 底部边距 12dp + 导航栏安全区）
                    item {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(56.dp + EdgeSpacing.md),
                        )
                    }
                }
            }
        }
    }

    // 长按删除确认（仅删记录，不动文件）
    pendingDelete?.let { item ->
        EdgeAlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = "删除传输记录",
            message = "将删除「${item.filename}」的传输记录，不会删除文件本身。",
            confirmText = "删除",
            onConfirm = {
                viewModel.remove(item.id)
                pendingDelete = null
            },
            destructive = true,
            dismissText = "取消",
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun TransferGroup(
    title: String,
    items: List<TransferItem>,
    onLongClick: (TransferItem) -> Unit,
) {
    IOSGroupedList(title = title) {
        items.forEachIndexed { index, item ->
            TransferRow(item = item, showDivider = index != items.lastIndex, onLongClick = onLongClick)
        }
    }
}

/**
 * 传输项：iOS 列表项风格（文件名 + 进度条 + 状态），项间 0.5dp 分隔线。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransferRow(
    item: TransferItem,
    showDivider: Boolean,
    onLongClick: (TransferItem) -> Unit,
) {
    val context = LocalContext.current
    // 已完成的下载项可点击：拉起系统"打开方式"（文件在公共 Download/EdgeCOS/ 目录）
    val canOpen = item.status == TransferStatus.COMPLETED &&
        item.type == TransferType.DOWNLOAD &&
        item.localPath != null
    val (icon, tint) = when {
        item.status == TransferStatus.FAILED ->
            Icons.Outlined.Error to MaterialTheme.colorScheme.error
        item.status == TransferStatus.COMPLETED ->
            Icons.Outlined.CloudDone to MaterialTheme.colorScheme.primary
        item.type == TransferType.UPLOAD ->
            Icons.Outlined.CloudUpload to MaterialTheme.colorScheme.primary
        else ->
            Icons.Outlined.Download to MaterialTheme.colorScheme.primary
    }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .combinedClickable(
                    onClick = {
                        // 仅已完成的下载项可打开；其余项点击无操作（可长按删除）
                        if (canOpen) {
                            item.localPath?.let { openDownloadedFile(context, File(it), item.filename) }
                        }
                    },
                    onLongClick = {
                        // 进行中的传输不可删（无取消机制，删除后会被后续进度事件重建）
                        if (item.status != TransferStatus.TRANSFERRING) onLongClick(item)
                    },
                )
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = tint,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.filename,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(EdgeSpacing.xs))
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (item.status == TransferStatus.FAILED)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(EdgeSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = when (item.status) {
                            TransferStatus.TRANSFERRING -> if (item.progress in 1..99) "${item.progress}%" else "准备中"
                            TransferStatus.COMPLETED -> "完成"
                            TransferStatus.FAILED -> "失败"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = when (item.type) {
                            TransferType.UPLOAD -> "上传"
                            TransferType.DOWNLOAD -> "下载"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                if (item.error != null) {
                    Text(
                        text = item.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            // 可打开项尾部指示（iOS 惯例：可交互行右侧显示 chevron）
            if (canOpen) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = EdgeSpacing.lg),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/**
 * 用系统"打开方式"选择器打开已下载的本地文件。
 *
 * 文件位于公共下载目录 Download/EdgeCOS/（下载器落盘位置），
 * 经 FileProvider 授权读取后交给第三方应用；无应用可处理或文件已不存在时给出提示。
 */
private fun openDownloadedFile(context: Context, file: File, filename: String) {
    if (!file.exists()) {
        Toast.makeText(context, "文件不存在，可能已被移动或删除", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, getMimeType(filename))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "打开方式"))
    } catch (_: Exception) {
        Toast.makeText(context, "没有可打开此文件的应用", Toast.LENGTH_SHORT).show()
    }
}
