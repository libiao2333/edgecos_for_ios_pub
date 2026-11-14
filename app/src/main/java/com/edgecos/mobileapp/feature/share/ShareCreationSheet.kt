package com.edgecos.mobileapp.feature.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSSegmentedControl
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.ShareCreateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val EXPIRATION_OPTIONS = listOf(
    "1d" to "1 天",
    "7d" to "7 天",
    "30d" to "30 天",
    "permanent" to "永久",
)

/**
 * 创建分享底部弹窗：选择过期时间 + 可选提取码 → 调用 [ShareRepository.createShare]。
 *
 * 自带 ViewModel 逻辑（[ShareCreationViewModel]），由 Composable 内部 remember 持有，
 * 避免污染 FilesViewModel。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareCreationSheet(
    file: FileNode,
    onDismiss: () -> Unit,
    viewModel: ShareCreationViewModel = remember { ShareCreationViewModel() },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
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
            Text(
                text = "创建分享",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = file.basename,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 过期时间选择
            Text(
                text = "有效期",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = EdgeSpacing.sm),
            )
            val labels = EXPIRATION_OPTIONS.map { it.second }
            val selectedIndex = EXPIRATION_OPTIONS
                .indexOfFirst { it.first == state.expirationType }
                .coerceAtLeast(0)
            IOSSegmentedControl(
                options = labels,
                selectedIndex = selectedIndex,
                onSelect = { index ->
                    viewModel.setExpiration(EXPIRATION_OPTIONS[index].first)
                },
            )

            // 提取码（可选）
            OutlinedTextField(
                value = state.extractionCode,
                onValueChange = {
                    // 4-8 位字母数字
                    val filtered = it.filter { ch -> ch.isLetterOrDigit() }.take(8)
                    viewModel.setExtractionCode(filtered)
                },
                label = { Text("提取码（可选，4-8 位字母数字）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                ),
            )

            // 错误提示
            state.error?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // 创建结果
            state.result?.let { result ->
                ShareResultCard(
                    result = result,
                    onCopy = {
                        val text = buildString {
                            append(result.shareUrl)
                            result.extractionCode?.let { code ->
                                append("\n提取码：$code")
                            }
                        }
                        clipboardManager.setText(AnnotatedString(text))
                    },
                )
            }

            // 创建按钮
            IOSButton(
                text = "创建分享",
                onClick = {
                    scope.launch {
                        viewModel.createShare(
                            fileId = file.id,
                            filePath = file.filename,
                            fileName = file.basename,
                        )
                    }
                },
                style = IOSButtonStyle.Filled,
                enabled = !state.isLoading && state.result == null,
                isLoading = state.isLoading,
                leadingIcon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ShareResultCard(
    result: ShareCreateResult,
    onCopy: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EdgeSpacing.md),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "分享链接",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IOSButton(
                    text = "复制",
                    onClick = onCopy,
                    style = IOSButtonStyle.Plain,
                    leadingIcon = Icons.Outlined.ContentCopy,
                )
            }
            Text(
                text = result.shareUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            result.extractionCode?.let { code ->
                Text(
                    text = "提取码：$code",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (result.reused) {
                Text(
                    text = "已复用现有分享",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ==================== ViewModel ====================

data class ShareCreationUiState(
    val expirationType: String = "7d",
    val extractionCode: String = "",
    val isLoading: Boolean = false,
    val result: ShareCreateResult? = null,
    val error: String? = null,
)

class ShareCreationViewModel : ViewModel() {
    private val shareRepo = ServiceLocator.shareRepository

    private val _state = MutableStateFlow(ShareCreationUiState())
    val state: StateFlow<ShareCreationUiState> = _state.asStateFlow()

    fun setExpiration(type: String) {
        _state.value = _state.value.copy(expirationType = type)
    }

    fun setExtractionCode(code: String) {
        _state.value = _state.value.copy(extractionCode = code)
    }

    fun createShare(
        fileId: String,
        filePath: String,
        fileName: String,
    ) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val current = _state.value
            val code = current.extractionCode.takeIf { it.length >= 4 }
            when (val r = shareRepo.createShare(
                fileId = fileId,
                filePath = filePath,
                fileName = fileName,
                expirationType = current.expirationType,
                extractionCode = code,
            )) {
                is EdgeResult.Success -> _state.value = _state.value.copy(
                    isLoading = false,
                    result = r.data,
                )
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }

    fun reset() {
        _state.value = ShareCreationUiState()
    }
}
