package com.edgecos.mobileapp.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.common.RecentlyOpenedStore
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val recentFiles: List<FileNode> = emptyList(),
    val error: String? = null,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val fileRepo = ServiceLocator.fileRepository
    private val authRepo = ServiceLocator.authRepository

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // 监听登录状态：登出时清空首页数据（加载由 HomeScreen 在进入页面时触发）
        viewModelScope.launch {
            authRepo.currentUser.collect { user ->
                if (user == null) {
                    _state.value = HomeUiState()
                }
            }
        }
    }

    /**
     * 加载首页「最近文件」，按以下顺序查找：
     * ① app 内最近 3 天打开的文件（本地打开记录，按打开时间倒序）
     * ② 都没有时：根目录按修改时间（lastmod）最新的 3 个文件
     * ③ 仍为空：UI 显示「最近无文件」
     */
    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            // ① 本地打开记录（SharedPreferences 磁盘 IO，放到 IO 线程）
            val opened = withContext(Dispatchers.IO) {
                RecentlyOpenedStore.recentWithin3Days(getApplication())
            }
            if (opened.isNotEmpty()) {
                _state.value = _state.value.copy(isLoading = false, recentFiles = opened)
                return@launch
            }

            // ② 修改时间最新的 3 个文件
            when (val r = fileRepo.refreshFiles("/")) {
                is EdgeResult.Success -> {
                    val recent = r.data
                        .filter { it.type == FileType.FILE }
                        .sortedByDescending { it.lastmod }
                        .take(3)
                    _state.value = _state.value.copy(isLoading = false, recentFiles = recent)
                }
                is EdgeResult.Failure -> _state.value = _state.value.copy(
                    isLoading = false,
                    error = r.error.message,
                )
            }
        }
    }
}
