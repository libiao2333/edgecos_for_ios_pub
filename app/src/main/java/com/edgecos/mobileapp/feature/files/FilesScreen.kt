package com.edgecos.mobileapp.feature.files

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSLargeTitleNavBar
import com.edgecos.mobileapp.design.components.IOSPathBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.util.formatApiTime
import com.edgecos.mobileapp.feature.share.DirectLinkResultSheet
import com.edgecos.mobileapp.feature.share.ShareCreationSheet
import com.edgecos.mobileapp.media.LineChoiceDialog
import com.edgecos.mobileapp.media.LinePreferenceManager
import com.edgecos.mobileapp.media.TransferLine

/**
 * 文件页：iOS HIG 风格。
 *
 * - 顶部 Large Title 导航栏
 * - 路径栏用 iOS 风格圆角胶囊按钮
 * - 文件列表用 iOS Grouped List 风格（圆角卡片）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    modifier: Modifier = Modifier,
    onNavigatePreview: (FileNode) -> Unit = {},
    fabClickTrigger: Int = 0,
    viewModel: FilesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val files by viewModel.files.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var showFabMenu by remember { mutableStateOf(false) }
    var detailFile by remember { mutableStateOf<FileNode?>(null) }
    var shareFile by remember { mutableStateOf<FileNode?>(null) }
    var directLinkFile by remember { mutableStateOf<FileNode?>(null) }
    var downloadLineFile by remember { mutableStateOf<FileNode?>(null) }

    val pickFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val filename = queryFilename(context, uri) ?: "未知文件"
            viewModel.uploadFile(context, uri, filename)
        }
    }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    // 监听 FAB 点击 trigger（FAB 在 MainScreen 的 bottomBar 中，通过 trigger 通信）
    // 注意：LaunchedEffect 在每次进入组合时都会以当前 trigger 值执行一次，
    // 若只判断 > 0，切到其他 Tab 再切回文件页时，会因残留的旧 trigger 再次弹出菜单。
    // 用 lastHandledTrigger 记录已消费的值，仅在 trigger 递增（即发生新点击）时才弹菜单。
    var lastHandledTrigger by remember { mutableIntStateOf(fabClickTrigger) }
    LaunchedEffect(fabClickTrigger) {
        if (fabClickTrigger > lastHandledTrigger) {
            lastHandledTrigger = fabClickTrigger
            showFabMenu = true
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            // iOS Large Title 导航栏（嵌套子页面：状态栏 inset 由外层 MainScreen 的 Scaffold 处理）
            IOSLargeTitleNavBar(title = "文件", applyStatusBarInset = false)

            // 路径栏（macOS Finder 风格面包屑，与导航栏对齐的独立 bar）
            // currentPath 形如 "/" 或 "/Documents/Work"，拆分为路径段
            val pathSegments = remember(state.currentPath) {
                buildList {
                    add("根目录")
                    if (state.currentPath != "/") {
                        state.currentPath.removePrefix("/")
                            .split("/")
                            .filter { it.isNotEmpty() }
                            .forEach { add(it) }
                    }
                }
            }
            IOSPathBar(
                segments = pathSegments,
                onSegmentClick = { index ->
                    // 0 = 根目录；否则拼接前 index 段路径
                    if (index == 0) {
                        viewModel.navigateTo("/")
                    } else {
                        val target = "/" + pathSegments.subList(1, index + 1).joinToString("/")
                        viewModel.navigateTo(target)
                    }
                },
            )

            // 内容区域
            if (state.isRefreshing && files.isEmpty()) {
                EdgeLoadingState(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else if (files.isEmpty()) {
                EdgeEmptyState(
                    title = "文件夹为空",
                    subtitle = "点击右下角 + 上传文件或新建文件夹",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            } else {
                // 文件列表（iOS 26 时钟风格：无卡片背景，直接铺在页面背景上）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(files, key = { it.id }) { file ->
                        FileRow(
                            file = file,
                            onClick = {
                                if (file.type == FileType.DIRECTORY) {
                                    viewModel.navigateTo(file.filename)
                                } else {
                                    onNavigatePreview(file)
                                }
                            },
                            onLongClick = { detailFile = file },
                            showDivider = file != files.last(),
                        )
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

    // 文件操作菜单（由 MainScreen 的 FAB 触发，通过 fabClickTrigger 通信）
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.xl, vertical = EdgeSpacing.lg)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
            ) {
                // 上传文件
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showFabMenu = false
                            pickFileLauncher.launch(arrayOf("*/*"))
                        }
                        .padding(horizontal = EdgeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "上传文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                // 新建文件夹
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showFabMenu = false
                            newFolderName = ""
                            showNewFolderDialog = true
                        }
                        .padding(horizontal = EdgeSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = "新建文件夹",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }

    if (showNewFolderDialog) {
        EdgeAlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = "新建文件夹",
            confirmText = "创建",
            onConfirm = {
                val name = newFolderName.trim()
                if (name.isNotEmpty()) {
                    viewModel.mkdir(name)
                    showNewFolderDialog = false
                }
            },
            confirmEnabled = newFolderName.isNotBlank(),
            dismissText = "取消",
            onDismiss = { showNewFolderDialog = false },
            content = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            },
        )
    }

    detailFile?.let { file ->
        FileDetailSheet(
            file = file,
            onDismiss = { detailFile = null },
            onPreview = {
                detailFile = null
                if (file.type == FileType.DIRECTORY) {
                    viewModel.navigateTo(file.filename)
                } else {
                    onNavigatePreview(file)
                }
            },
            onDownload = {
                detailFile = null
                // 下载线路选择（与网页端 chooseLine 一致）：偏好为「每次询问」时先弹窗
                val pref = LinePreferenceManager.downloadLine(context)
                if (pref == TransferLine.ASK) {
                    downloadLineFile = file
                } else {
                    viewModel.download(context, file, pref)
                }
            },
            onShare = {
                detailFile = null
                shareFile = file
            },
            onDirectLink = {
                detailFile = null
                directLinkFile = file
            },
            onDelete = {
                detailFile = null
                viewModel.delete(file.id, file.filename)
            },
        )
    }

    shareFile?.let { file ->
        ShareCreationSheet(
            file = file,
            onDismiss = { shareFile = null },
        )
    }

    directLinkFile?.let { file ->
        DirectLinkResultSheet(
            file = file,
            onDismiss = { directLinkFile = null },
        )
    }

    // 下载线路选择弹窗（偏好为「每次询问」时展示，与网页端一致）
    downloadLineFile?.let { file ->
        LineChoiceDialog(
            title = "选择下载线路",
            onChoose = { line, remember ->
                if (remember) LinePreferenceManager.setDownloadLine(context, line)
                downloadLineFile = null
                viewModel.download(context, file, line)
            },
            onDismiss = { downloadLineFile = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    file: FileNode,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 文件图标：圆角方块背景
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (file.type == FileType.DIRECTORY) Icons.Outlined.Folder
                    else Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.basename,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = formatApiTime(file.lastmod),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = EdgeSpacing.lg + 32.dp + EdgeSpacing.md),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

private fun queryFilename(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
    }.getOrNull()
}
