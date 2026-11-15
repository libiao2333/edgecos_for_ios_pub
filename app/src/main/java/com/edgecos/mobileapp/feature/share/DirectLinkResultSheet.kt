package com.edgecos.mobileapp.feature.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.DirectLink
import com.edgecos.mobileapp.domain.model.FileNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 生成直链底部弹窗：调用 [DirectLinkRepository.generateDirectLink]，展示直链 URL。
 *
 * 直链用于第三方下载工具（如 IDM、迅雷）拉取文件，访问无需登录，
 * 但生成需实名认证 + 流量未耗尽（后端中间件校验）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectLinkResultSheet(
    file: FileNode,
    onDismiss: () -> Unit,
    viewModel: DirectLinkViewModel = remember { DirectLinkViewModel() },
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by viewModel.state.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // 进入时自动生成
    androidx.compose.runtime.LaunchedEffect(file.id) {
        viewModel.generate(file.id, file.filename)
    }

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
                text = "直链",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = file.basename,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.result?.let { link ->
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
                                text = "直链地址",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            IOSButton(
                                text = "复制",
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(link.directUrl))
                                },
                                style = IOSButtonStyle.Filled,
                                leadingIcon = Icons.Outlined.ContentCopy,
                            )
                        }
                        Text(
                            text = link.directUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "提示：直链可用于第三方下载工具，访问无需登录",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            IOSButton(
                text = "重新生成",
                onClick = {
                    scope.launch { viewModel.generate(file.id, file.filename) }
                },
                style = IOSButtonStyle.Filled,
                enabled = !state.isLoading,
                isLoading = state.isLoading,
                leadingIcon = Icons.Outlined.Link,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ==================== ViewModel ====================

data class DirectLinkUiState(
    val isLoading: Boolean = false,
    val result: DirectLink? = null,
    val error: String? = null,
)

class DirectLinkViewModel : ViewModel() {
    private val directLinkRepo = ServiceLocator.directLinkRepository

    private val _state = MutableStateFlow(DirectLinkUiState())
    val state: StateFlow<DirectLinkUiState> = _state.asStateFlow()

    fun generate(fileId: String, filePath: String) {
        _state.value = DirectLinkUiState(isLoading = true)
        viewModelScope.launch {
            when (val r = directLinkRepo.generateDirectLink(fileId = fileId, filePath = filePath)) {
                is EdgeResult.Success -> _state.value = DirectLinkUiState(result = r.data)
                is EdgeResult.Failure -> _state.value = DirectLinkUiState(error = r.error.message)
            }
        }
    }

    fun reset() {
        _state.value = DirectLinkUiState()
    }
}
