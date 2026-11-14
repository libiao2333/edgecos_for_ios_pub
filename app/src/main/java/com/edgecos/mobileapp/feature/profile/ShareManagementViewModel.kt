package com.edgecos.mobileapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 分享管理 ViewModel：加载根目录下已分享的文件（activeShare != null）。
 *
 * 当前实现：刷新根目录文件列表后过滤出已分享文件。
 * 后端未提供独立的 GET /shares 列表端点，文件分享状态嵌入在 FileNode.activeShare 中。
 */
class ShareManagementViewModel : ViewModel() {
    private val fileRepo = ServiceLocator.fileRepository
    private val shareRepo = ServiceLocator.shareRepository

    private val _state = MutableStateFlow(ShareManagementUiState())
    val state: StateFlow<ShareManagementUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            when (val r = fileRepo.refreshFiles("/")) {
                is EdgeResult.Success -> {
                    val shared = r.data.filter { it.activeShare != null }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        sharedFiles = shared,
                    )
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }

    /** 关闭文件的分享 */
    fun closeShare(fileId: String) {
        viewModelScope.launch {
            when (val r = shareRepo.closeShare(fileId)) {
                is EdgeResult.Success -> refresh()
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    error = r.error.message,
                )
            }
        }
    }
}

data class ShareManagementUiState(
    val isLoading: Boolean = false,
    val sharedFiles: List<FileNode> = emptyList(),
    val error: String? = null,
)
