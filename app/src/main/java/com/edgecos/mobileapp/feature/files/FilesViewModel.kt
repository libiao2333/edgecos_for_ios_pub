package com.edgecos.mobileapp.feature.files

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.remote.ClientConfig
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.model.Quota
import com.edgecos.mobileapp.feature.transfer.DownloadService
import com.edgecos.mobileapp.feature.transfer.TransferEventBus
import com.edgecos.mobileapp.feature.transfer.TransferType
import com.edgecos.mobileapp.media.TransferLine
import com.edgecos.mobileapp.upload.UploadEventBus
import com.edgecos.mobileapp.upload.UploadService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.UUID

data class FilesUiState(
    val currentPath: String = "/",
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val toast: String? = null,
    val quota: Quota? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModel : ViewModel() {
    private val fileRepo = ServiceLocator.fileRepository
    private val authRepo = ServiceLocator.authRepository

    private val _state = MutableStateFlow(FilesUiState())
    val state: StateFlow<FilesUiState> = _state.asStateFlow()

    /** 当前进行中的刷新任务：新刷新开始时取消旧的，避免跨目录竞态写状态 */
    private var refreshJob: Job? = null

    // 当 currentPath 变化时自动重新订阅对应目录的文件列表。
    // flatMapLatest 内部先 emit 空列表清空旧数据、再 emit Room 缓存、最后等 refresh 写库后更新。
    // 排序：文件夹置顶 → 同类型按名称字母序（忽略大小写）。
    //
    // 使用 Eagerly（而非 WhileSubscribed）：切换底部 Tab 离开文件页 5 秒后再回来时，
    // WhileSubscribed 会让上游重启并重新 emit 空列表，造成"文件夹为空"空状态闪现；
    // Eagerly 保持 Room 订阅常驻，切回时直接沿用已有列表，无闪变。
    val files: StateFlow<List<FileNode>> = _state
        .map { it.currentPath }
        .distinctUntilChanged()
        .flatMapLatest { path ->
            flow {
                emit(emptyList())
                emitAll(fileRepo.observeFiles(path))
            }
        }
        .map { list ->
            list.sortedWith(
                compareBy({ it.type != FileType.DIRECTORY }, { it.basename.lowercase() }),
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // 监听登录状态：
        // - 登录/切换账号：刷新列表与配额，避免停留在上一个账号的缓存视图
        // - 登出：重置文件页状态并清空本地文件缓存（跨账号数据隔离）
        viewModelScope.launch {
            authRepo.currentUser.collect { user ->
                if (user == null) {
                    refreshJob?.cancel()
                    _state.value = FilesUiState()
                    runCatching { fileRepo.clearCache() }
                } else {
                    refresh()
                    loadQuota()
                }
            }
        }
        // 监听上传事件：成功/失败 → 刷新当前目录 + toast 提示；进度由通知栏展示
        viewModelScope.launch {
            UploadEventBus.events.collect { event ->
                when (event) {
                    is UploadEventBus.Event.Success -> {
                        if (event.path == _state.value.currentPath) refresh()
                        loadQuota()  // 上传成功后刷新配额
                        _state.value = _state.value.copy(toast = "上传成功: ${event.filename}")
                    }
                    is UploadEventBus.Event.Failed ->
                        _state.value = _state.value.copy(toast = "上传失败: ${event.message}")
                    is UploadEventBus.Event.Progress -> Unit
                }
            }
        }
    }

    /** 加载存储配额：失败时静默，不影响文件列表使用 */
    fun loadQuota() {
        viewModelScope.launch {
            when (val r = fileRepo.getQuota()) {
                is EdgeResult.Success -> _state.value = _state.value.copy(quota = r.data)
                is EdgeResult.Failure -> Unit  // 配额加载失败不影响文件操作
            }
        }
    }

    /**
     * 进入子目录。[path] 为完整路径（如 `/docs`），由 [FileNode.filename] 提供。
     */
    fun navigateTo(path: String) {
        if (path == _state.value.currentPath) return
        _state.value = _state.value.copy(currentPath = path)
        refresh()
    }

    fun navigateUp() {
        val current = _state.value.currentPath
        if (current == "/") return
        val parent = current.substringBeforeLast("/").ifEmpty { "/" }
        _state.value = _state.value.copy(currentPath = parent)
        refresh()
    }

    fun refresh() {
        // 捕获发起时的路径：网络返回时若用户已切到其他目录，结果不得写回状态
        val path = _state.value.currentPath
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.value = _state.value.copy(isRefreshing = true, error = null)
            when (val r = fileRepo.refreshFiles(path)) {
                is EdgeResult.Success ->
                    if (_state.value.currentPath == path) {
                        _state.value = _state.value.copy(isRefreshing = false)
                    }
                is EdgeResult.Failure ->
                    if (_state.value.currentPath == path) {
                        _state.value = _state.value.copy(
                            isRefreshing = false,
                            error = r.error.message,
                        )
                    }
            }
        }
    }

    fun mkdir(name: String) {
        viewModelScope.launch {
            val newPath = joinPath(_state.value.currentPath, name)
            fileRepo.mkdir(newPath)
            refresh()
        }
    }

    fun delete(id: String, path: String) {
        viewModelScope.launch {
            fileRepo.delete(path, id)
            refresh()
            loadQuota()  // 删除后刷新配额
        }
    }

    /**
     * 触发单文件上传：启动 [UploadService] 前台服务在后台执行。
     * 文件 URI 需通过 [android.content.ContentResolver.takePersistableUriPermission] 持久化读权限。
     */
    fun uploadFile(context: Context, uri: Uri, filename: String) {
        runCatching {
            UploadService.start(context, uri, filename, _state.value.currentPath)
        }.onSuccess {
            _state.value = _state.value.copy(toast = "开始上传: $filename")
        }.onFailure { t ->
            // 前台服务启动受限（如后台启动限制）时给出明确反馈，而不是静默失败
            _state.value = _state.value.copy(
                toast = "上传启动失败: ${t.message ?: "未知错误"}",
            )
        }
    }

    /**
     * 下载文件（交给 [DownloadService] 前台服务执行）。
     *
     * 服务负责：断点续传下载、通知栏 Live 进度（Android 16 流体云）、
     * 广播 [TransferEventBus] 事件（Started / Progress / Completed / Failed）。
     *
     * @param line 用户选择的下载线路（cdn/free，与网页端一致），由设置偏好或线路弹窗决定。
     */
    fun download(context: Context, file: FileNode, line: TransferLine) {
        // file.filename 是完整路径（如 /docs/foo.txt），后端 /download 接收完整 path
        val encodedPath = URLEncoder.encode(file.filename, "UTF-8")
        val url = "${ClientConfig.baseUrl}download?path=$encodedPath&id=${file.id}&line=${line.requestParam}"
        val taskId = UUID.randomUUID().toString().take(8)

        DownloadService.start(context, url, file.basename, taskId)
        _state.value = _state.value.copy(toast = "开始下载: ${file.basename}")
    }

    fun consumeToast() {
        _state.value = _state.value.copy(toast = null)
    }

    private fun joinPath(base: String, name: String): String =
        if (base.endsWith("/")) "$base$name" else "$base/$name"
}
