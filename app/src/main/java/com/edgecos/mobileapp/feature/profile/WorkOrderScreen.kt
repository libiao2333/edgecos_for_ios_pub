package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.components.EdgeCard
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.components.pressClickable
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.WorkOrder
import com.edgecos.mobileapp.domain.model.WorkOrderReply
import com.edgecos.mobileapp.domain.util.formatApiTime

private val WORK_ORDER_CATEGORIES = listOf(
    "file_download_cdn" to "CDN 下载",
    "upload" to "上传问题",
    "account" to "账号问题",
    "billing" to "计费问题",
    "other" to "其他",
)

private fun statusLabel(status: String): String = when (status) {
    "open" -> "处理中"
    "closed" -> "已关闭"
    "pending" -> "待处理"
    else -> status
}

private fun categoryLabel(category: String): String =
    WORK_ORDER_CATEGORIES.firstOrNull { it.first == category }?.second ?: category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderScreen(
    onBack: () -> Unit,
    viewModel: WorkOrderViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSubmitDialog by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }

    // Error snackbar
    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    // Detail sheet: open when selectedWorkOrder changes
    LaunchedEffect(state.selectedWorkOrder) {
        if (state.selectedWorkOrder != null) {
            showDetailSheet = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitDialog = true },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("提交工单") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            IOSNavBar(title = "工单详情", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = EdgeSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
            ) {
                when {
                    state.isLoading -> {
                        EdgeLoadingState(modifier = Modifier.weight(1f))
                    }
                    state.workOrders.isEmpty() -> {
                        EdgeEmptyState(
                            title = "暂无工单",
                            subtitle = "如有问题请点击右下角提交工单",
                            icon = Icons.Outlined.HelpOutline,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
                        ) {
                            state.workOrders.forEach { wo ->
                                WorkOrderCard(
                                    workOrder = wo,
                                    onTap = {
                                        viewModel.loadDetail(wo.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Submit dialog
    if (showSubmitDialog) {
        SubmitWorkOrderDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = { title, content, category ->
                viewModel.submitWorkOrder(title, content, category)
                showSubmitDialog = false
            },
            isSubmitting = state.isSubmitting,
        )
    }

    // Detail bottom sheet
    if (showDetailSheet && state.selectedWorkOrder != null) {
        val detail = state.selectedWorkOrder!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = {
                showDetailSheet = false
                viewModel.clearSelectedWorkOrder()
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            WorkOrderDetailSheet(
                workOrder = detail,
                isLoadingDetail = state.isLoadingDetail,
                isSubmittingReply = state.isSubmittingReply,
                isClosing = state.isClosing,
                onReply = { content ->
                    viewModel.replyToWorkOrder(detail.id, content)
                },
                onClose = {
                    viewModel.closeWorkOrder(detail.id)
                    showDetailSheet = false
                    viewModel.clearSelectedWorkOrder()
                },
                modifier = Modifier.padding(bottom = EdgeSpacing.xxl),
            )
        }
    }
}

@Composable
private fun WorkOrderCard(
    workOrder: WorkOrder,
    onTap: () -> Unit,
) {
    IOSGroupedList {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressClickable(onClick = onTap)
                .padding(EdgeSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workOrder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(EdgeSpacing.xs))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                    ) {
                        StatusBadge(status = workOrder.status)
                        CategoryBadge(category = workOrder.category)
                    }
                }
            }
            Spacer(modifier = Modifier.height(EdgeSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = workOrder.username,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatApiTime(workOrder.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (workOrder.replies.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(EdgeSpacing.md))
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = workOrder.replies.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val color = when (status) {
        "open" -> MaterialTheme.colorScheme.primary
        "closed" -> MaterialTheme.colorScheme.outline
        "pending" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = statusLabel(status),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun CategoryBadge(category: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = categoryLabel(category),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitWorkOrderDialog(
    onDismiss: () -> Unit,
    onSubmit: (title: String, content: String, category: String) -> Unit,
    isSubmitting: Boolean,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(WORK_ORDER_CATEGORIES.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    EdgeAlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = "提交工单",
        confirmText = if (isSubmitting) "提交中…" else "提交",
        onConfirm = { onSubmit(title.trim(), content.trim(), selectedCategory.first) },
        confirmEnabled = title.isNotBlank() && content.isNotBlank() && !isSubmitting,
        dismissText = "取消",
        onDismiss = onDismiss,
        dismissOnBackPress = !isSubmitting,
        dismissOnClickOutside = !isSubmitting,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    enabled = !isSubmitting,
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                ) {
                    OutlinedTextField(
                        value = categoryLabel(selectedCategory.first),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("分类") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = MaterialTheme.shapes.small,
                        enabled = !isSubmitting,
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        WORK_ORDER_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.second) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("内容") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = MaterialTheme.shapes.small,
                    maxLines = 8,
                    enabled = !isSubmitting,
                )
            }
        },
    )
}

@Composable
private fun WorkOrderDetailSheet(
    workOrder: WorkOrder,
    isLoadingDetail: Boolean,
    isSubmittingReply: Boolean,
    isClosing: Boolean,
    onReply: (content: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var replyText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = EdgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
    ) {
        // Header
        Text(
            text = workOrder.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
        ) {
            StatusBadge(status = workOrder.status)
            CategoryBadge(category = workOrder.category)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatApiTime(workOrder.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "提交者：${workOrder.username}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Content
        EdgeCard {
            Column {
                Text(
                    text = workOrder.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Close button
        if (workOrder.status != "closed") {
            IOSButton(
                text = if (isClosing) "关闭中…" else "关闭工单",
                onClick = onClose,
                style = IOSButtonStyle.Tinted,
                enabled = !isClosing,
                leadingIcon = Icons.Outlined.Close,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Replies section
        Text(
            text = "回复 (${workOrder.replies.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // 回复列表：占据剩余空间并可滚动（LazyColumn），图片可按原始比例显示
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoadingDetail -> EdgeLoadingState(
                    modifier = Modifier.height(100.dp),
                    label = "加载中…",
                )
                workOrder.replies.isEmpty() -> Text(
                    text = "暂无回复",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = EdgeSpacing.lg),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                ) {
                    items(workOrder.replies) { reply ->
                        ReplyItem(reply = reply)
                    }
                }
            }
        }

        // Reply input：固定底部
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
        ) {
            OutlinedTextField(
                value = replyText,
                onValueChange = { replyText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入回复…") },
                maxLines = 3,
                shape = MaterialTheme.shapes.small,
                enabled = !isSubmittingReply && workOrder.status != "closed",
            )
            IOSButton(
                text = "发送",
                onClick = {
                    if (replyText.isNotBlank()) {
                        onReply(replyText.trim())
                        replyText = ""
                    }
                },
                style = IOSButtonStyle.Filled,
                leadingIcon = Icons.Outlined.Send,
                enabled = replyText.isNotBlank() && !isSubmittingReply && workOrder.status != "closed",
                isLoading = isSubmittingReply,
            )
        }
    }
}

@Composable
private fun ReplyItem(reply: WorkOrderReply) {
    val isAdmin = reply.isAdminReply
    // 用 Box + align 强制对齐：客服居左，客户居右
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(EdgeSpacing.sm)
                .width(300.dp)
                .align(if (isAdmin) Alignment.TopStart else Alignment.TopEnd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isAdmin) Arrangement.Start else Arrangement.End,
            ) {
                if (isAdmin) {
                    Text(
                        text = "客服",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(EdgeSpacing.sm))
                }
                Text(
                    text = formatApiTime(reply.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isAdmin) {
                    Spacer(modifier = Modifier.width(EdgeSpacing.sm))
                    Text(
                        text = "我",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Spacer(modifier = Modifier.height(EdgeSpacing.xs))
            Box(
                modifier = Modifier
                    .background(
                        color = if (isAdmin)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(
                            topStart = if (isAdmin) 4.dp else 12.dp,
                            topEnd = if (isAdmin) 12.dp else 4.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp,
                        ),
                    )
                    .padding(EdgeSpacing.md),
            ) {
                ReplyContent(content = reply.content)
            }
        }
    }
}

/**
 * 渲染工单回复内容：解析 <img> 标签并使用 AsyncImage 加载图片，其余文本用 Text 渲染。
 *
 * 后端回复内容可能包含 HTML img 标签（如客服粘贴的截图），不解析会导致图片无法显示。
 * Column 固定 width(300.dp)，图片宽度固定 300dp，可按比例自适应高度。
 */
@Composable
private fun ReplyContent(content: String) {
    val imgPattern = remember {
        Regex("""<img[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
    }
    val matches = imgPattern.findAll(content).toList()

    if (matches.isEmpty()) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm)) {
        var lastEnd = 0
        matches.forEach { match ->
            // img 之前的文本
            val textBefore = content.substring(lastEnd, match.range.first)
            if (textBefore.isNotBlank()) {
                Text(
                    text = textBefore.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // 图片：宽度固定 300dp，高度按比例自适应，对话框可滚动无需缩小
            AsyncImage(
                model = match.groupValues[1],
                contentDescription = "回复图片",
                modifier = Modifier
                    .width(300.dp)
                    .heightIn(max = 400.dp),
                contentScale = ContentScale.Fit,
            )
            lastEnd = match.range.last + 1
        }
        // 末尾剩余文本
        val textAfter = content.substring(lastEnd)
        if (textAfter.isNotBlank()) {
            Text(
                text = textAfter.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
