package com.edgecos.mobileapp.feature.share

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeErrorState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.util.formatBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分享访问页（深度链接 /s/{shareId} 落地页，无需登录）。
 *
 * 流程：
 * 1. 通过 shareId 拉取分享信息（GET /share/:shareId）
 * 2. 若 needExtractionCode=true，先让用户输入提取码
 * 3. 文件：显示文件信息 + 下载按钮；文件夹：列出文件列表，可点击下载
 */
@Composable
fun ShareAccessScreen(
    shareId: String,
    onBack: () -> Unit,
    viewModel: ShareAccessViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            IOSNavBar(title = "分享详情", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.lg)
                    .padding(bottom = EdgeSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
            ) {
                // 启动时加载分享信息
                androidx.compose.runtime.LaunchedEffect(shareId) {
                    viewModel.loadShareInfo(shareId)
                }

                when {
                    state.isLoading -> EdgeLoadingState(modifier = Modifier.fillMaxWidth())
                    state.error != null -> EdgeErrorState(
                        title = "加载失败",
                        message = state.error,
                        onRetry = { viewModel.loadShareInfo(shareId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.shareInfo != null -> {
                        val info = state.shareInfo!!
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(EdgeSpacing.lg)) {
                                Text(
                                    text = info.fileName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(EdgeSpacing.sm))
                                Text(
                                    text = "分享者：${info.owner}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                info.expiration?.let { exp ->
                                    Text(
                                        text = "过期时间：$exp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // 需要提取码
                        if (info.needExtractionCode && !state.codeVerified) {
                            ExtractionCodeCard(
                                code = state.inputCode,
                                onCodeChange = viewModel::setInputCode,
                                error = state.codeError,
                                onSubmit = { viewModel.verifyCode(shareId) },
                            )
                        } else if (info.isFolder) {
                            // 文件夹：列出文件
                            Text(
                                text = "文件夹内容",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            when {
                                state.filesLoading -> EdgeLoadingState(modifier = Modifier.fillMaxWidth())
                                state.files.isEmpty() -> EdgeEmptyState(
                                    title = "文件夹为空",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                else -> LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
                                ) {
                                    items(state.files, key = { it.id }) { file ->
                                        ShareFileRow(
                                            file = file,
                                            onDownload = {
                                                openShareDownload(
                                                    context = context,
                                                    shareId = shareId,
                                                    code = state.inputCode.takeIf { it.isNotBlank() },
                                                    path = file.filename,
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        } else {
                            // 单文件：直接下载
                            IOSButton(
                                text = "下载文件",
                                onClick = {
                                    openShareDownload(
                                        context = context,
                                        shareId = shareId,
                                        code = state.inputCode.takeIf { it.isNotBlank() },
                                        path = "/",
                                    )
                                },
                                style = IOSButtonStyle.Filled,
                                leadingIcon = Icons.Outlined.Download,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtractionCodeCard(
    code: String,
    onCodeChange: (String) -> Unit,
    error: String?,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(EdgeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            Text(
                text = "请输入提取码",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = code,
                onValueChange = onCodeChange,
                label = { Text("提取码") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                ),
            )
            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            IOSButton(
                text = "确认",
                onClick = onSubmit,
                style = IOSButtonStyle.Filled,
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShareFileRow(
    file: FileNode,
    onDownload: () -> Unit,
) {
    Surface(
        onClick = onDownload,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            Icon(
                imageVector = if (file.type == FileType.DIRECTORY) Icons.Outlined.Folder
                else Icons.Outlined.Description,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.basename,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatBytes(file.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = "下载",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * 用系统浏览器打开分享下载链接（302 重定向到实际文件）。
 *
 * 不在 App 内下载：分享下载无需登录态，浏览器拉起更通用，
 * 用户也可用第三方下载工具接管。
 */
private fun openShareDownload(
    context: android.content.Context,
    shareId: String,
    code: String?,
    path: String,
) {
    val url = buildString {
        append("https://user.edgecos.com/api/share/")
        append(shareId)
        append("/download?path=")
        append(java.net.URLEncoder.encode(path, "UTF-8"))
        if (code != null) {
            append("&code=")
            append(java.net.URLEncoder.encode(code, "UTF-8"))
        }
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

// ==================== ViewModel ====================

data class ShareAccessUiState(
    val isLoading: Boolean = false,
    val shareInfo: com.edgecos.mobileapp.domain.model.ShareInfo? = null,
    val error: String? = null,
    val needExtractionCode: Boolean = false,
    val inputCode: String = "",
    val codeVerified: Boolean = false,
    val codeError: String? = null,
    val filesLoading: Boolean = false,
    val files: List<FileNode> = emptyList(),
)

class ShareAccessViewModel : ViewModel() {
    private val shareRepo = ServiceLocator.shareRepository

    private val _state = MutableStateFlow(ShareAccessUiState())
    val state: StateFlow<ShareAccessUiState> = _state.asStateFlow()

    fun loadShareInfo(shareId: String) {
        _state.value = ShareAccessUiState(isLoading = true)
        viewModelScope.launch {
            when (val r = shareRepo.getShareInfo(shareId)) {
                is EdgeResult.Success -> {
                    val info = r.data
                    _state.value = _state.value.copy(
                        isLoading = false,
                        shareInfo = info,
                        needExtractionCode = info.needExtractionCode,
                        codeVerified = !info.needExtractionCode,
                    )
                    // 文件夹且无需提取码：直接加载文件列表
                    if (info.isFolder && !info.needExtractionCode) {
                        loadFiles(shareId, null)
                    }
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun setInputCode(code: String) {
        _state.value = _state.value.copy(inputCode = code, codeError = null)
    }

    fun verifyCode(shareId: String) {
        val code = _state.value.inputCode.trim()
        if (code.isEmpty()) {
            _state.value = _state.value.copy(codeError = "请输入提取码")
            return
        }
        val info = _state.value.shareInfo ?: return
        viewModelScope.launch {
            if (info.isFolder) {
                // 文件夹：尝试用提取码加载文件列表，成功则视为验证通过
                _state.value = _state.value.copy(filesLoading = true)
                when (val r = shareRepo.getShareFiles(shareId, "/", code)) {
                    is EdgeResult.Success -> _state.value = _state.value.copy(
                        filesLoading = false,
                        codeVerified = true,
                        files = r.data,
                    )
                    is EdgeResult.Failure -> _state.value = _state.value.copy(
                        filesLoading = false,
                        codeError = r.error.message,
                    )
                }
            } else {
                // 单文件：无法预验证，直接标记为已验证，让用户点下载
                _state.value = _state.value.copy(codeVerified = true)
            }
        }
    }

    private fun loadFiles(shareId: String, code: String?) {
        _state.value = _state.value.copy(filesLoading = true)
        viewModelScope.launch {
            when (val r = shareRepo.getShareFiles(shareId, "/", code)) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    filesLoading = false,
                    files = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    filesLoading = false,
                    error = r.error.message,
                )
            }
        }
    }
}
