package com.edgecos.mobileapp.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.edgecos.mobileapp.data.remote.KtorEdgeApi
import com.edgecos.mobileapp.shared.dto.AnnouncementDto
import com.edgecos.mobileapp.shared.dto.CaptchaPrecheckRequest
import com.edgecos.mobileapp.shared.dto.DeleteRequest
import com.edgecos.mobileapp.shared.dto.DirectLinkCreateRequest
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import com.edgecos.mobileapp.shared.dto.FileSummaryDto
import com.edgecos.mobileapp.shared.dto.LoginRequest
import com.edgecos.mobileapp.shared.dto.MkdirRequest
import com.edgecos.mobileapp.shared.dto.MyPlanResponse
import com.edgecos.mobileapp.shared.dto.OrderDto
import com.edgecos.mobileapp.shared.dto.OrdersResponse
import com.edgecos.mobileapp.shared.dto.PlanDto
import com.edgecos.mobileapp.shared.dto.QuotaResponse
import com.edgecos.mobileapp.shared.dto.RegisterRequest
import com.edgecos.mobileapp.shared.dto.ShareCreateRequest
import com.edgecos.mobileapp.shared.dto.UploadCompleteRequest
import com.edgecos.mobileapp.shared.dto.UploadInstantCompleteRequest
import com.edgecos.mobileapp.shared.dto.UserDto
import com.edgecos.mobileapp.shared.dto.WorkOrderDto
import com.edgecos.mobileapp.shared.dto.WorkOrderSubmitRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 与 Android `RootNav` 的次级路由一一对应。 */
enum class Route {
    Main,
    Login,
    Register,
    Plan,
    Wallet,
    WorkOrder,
    Announcements,
    ShareManagement,
    Orders,
    Settings,
    About,
    Oss,
    LegalAgreement,
    LegalPrivacy,
    Preview,
}

/**
 * 全 App 共用一个 [KtorEdgeApi]：Ktor 的 HttpCookies 在内存中维护登录 Cookie，
 * 每次新建实例都会丢失登录态。
 */
internal object ApiHolder {
    val api: KtorEdgeApi by lazy { KtorEdgeApi() }
}

/** 传输方向。 */
enum class TransferDirection { Upload, Download }

/** 传输任务状态。 */
enum class TransferStatus { Running, Success, Failed }

/**
 * 一条传输任务（上传 / 下载）。
 *
 * 字段使用 Compose 的 [mutableStateOf]，上传协程直接更新即可驱动进度条重绘。
 */
class TransferTask(
    val id: String,
    val fileName: String,
    val direction: TransferDirection,
    val size: Long,
) {
    var progress by mutableStateOf(0f)
    var status by mutableStateOf(TransferStatus.Running)
    var message by mutableStateOf<String?>(null)
}

/**
 * Compose 版的「ServiceLocator + ViewModel」：集中持有登录态、导航栈与所有页面数据。
 *
 * 所有页面共用同一份状态，切 Tab / 进出次级页面都不需要重新登录。
 */
class AppStore(private val scope: CoroutineScope) {

    val api: KtorEdgeApi get() = ApiHolder.api

    // ---------- 登录态 ----------
    var user by mutableStateOf<UserDto?>(null)
        private set
    var isLoadingLogin by mutableStateOf(false)
        private set
    var loginError by mutableStateOf<String?>(null)
        private set

    // ---------- 导航 ----------
    var routeStack by mutableStateOf<List<Route>>(listOf(Route.Main))
        private set
    val route: Route get() = routeStack.last()

    /** 0=首页 1=文件 2=传输 3=我的（与 Android 四个 Tab 顺序一致）。 */
    var selectedTab by mutableStateOf(0)

    /** 文件预览页当前文件。 */
    var previewFile by mutableStateOf<FileNodeDto?>(null)

    fun open(route: Route) {
        if (routeStack.last() != route) routeStack = routeStack + route
    }

    fun back() {
        if (routeStack.size > 1) routeStack = routeStack.dropLast(1)
    }

    fun goMain(tab: Int = selectedTab) {
        routeStack = listOf(Route.Main)
        selectedTab = tab
    }

    // ---------- 首页 ----------
    var summary by mutableStateOf<FileSummaryDto?>(null)
        private set
    var quota by mutableStateOf<QuotaResponse?>(null)
        private set
    var recentFiles by mutableStateOf<List<FileNodeDto>>(emptyList())
        private set
    var isLoadingHome by mutableStateOf(false)
        private set
    var homeError by mutableStateOf<String?>(null)
        private set

    // ---------- 文件 ----------
    var files by mutableStateOf<List<FileNodeDto>>(emptyList())
        private set
    var currentPath by mutableStateOf("/")
        private set
    var pathHistory by mutableStateOf<List<String>>(emptyList())
        private set
    var isLoadingFiles by mutableStateOf(false)
        private set
    var filesError by mutableStateOf<String?>(null)
        private set

    // ---------- 套餐 / 订单 ----------
    var myPlan by mutableStateOf<MyPlanResponse?>(null)
        private set
    var plans by mutableStateOf<List<PlanDto>>(emptyList())
        private set
    var orders by mutableStateOf<OrdersResponse?>(null)
        private set
    var isLoadingBilling by mutableStateOf(false)
        private set
    var billingError by mutableStateOf<String?>(null)
        private set

    // ---------- 公告 / 工单 / 分享 ----------
    var announcements by mutableStateOf<List<AnnouncementDto>>(emptyList())
        private set
    var isLoadingAnnouncements by mutableStateOf(false)
        private set
    var announcementsError by mutableStateOf<String?>(null)
        private set

    var workOrders by mutableStateOf<List<WorkOrderDto>>(emptyList())
        private set
    var isLoadingWorkOrders by mutableStateOf(false)
        private set
    var workOrdersError by mutableStateOf<String?>(null)
        private set

    var sharedFiles by mutableStateOf<List<FileNodeDto>>(emptyList())
        private set
    var isLoadingShared by mutableStateOf(false)
        private set
    var sharedError by mutableStateOf<String?>(null)
        private set

    // ---------- 设置 ----------
    var darkTheme by mutableStateOf(false)
    var linePreference by mutableStateOf("每次询问")

    // ---------- 传输任务 ----------
    var transferTasks by mutableStateOf<List<TransferTask>>(emptyList())
        private set

    /** 通用一次性提示（弹窗展示）。 */
    var actionMessage by mutableStateOf<String?>(null)

    // ==================== 登录 / 登出 ====================

    suspend fun login(username: String, password: String): Boolean {
        if (username.isBlank() || password.isEmpty()) {
            loginError = "请输入用户名和密码"
            return false
        }
        isLoadingLogin = true
        loginError = null
        return try {
            user = api.login(LoginRequest(username.trim(), password)).user
            goMain(0)
            loadHome()
            loadFiles("/")
            loadMyPlan()
            true
        } catch (e: Throwable) {
            loginError = describeError(e)
            false
        } finally {
            isLoadingLogin = false
        }
    }

    fun loginAsync(username: String, password: String) {
        scope.launch { login(username, password) }
    }

    fun logout() {
        user = null
        routeStack = listOf(Route.Main)
        selectedTab = 0
        files = emptyList()
        currentPath = "/"
        pathHistory = emptyList()
        summary = null
        quota = null
        recentFiles = emptyList()
        myPlan = null
        plans = emptyList()
        orders = null
        announcements = emptyList()
        workOrders = emptyList()
        sharedFiles = emptyList()
        previewFile = null
        homeError = null
        filesError = null
        loginError = null
    }

    // ==================== 首页 ====================

    suspend fun loadHome() {
        if (user == null) return
        isLoadingHome = true
        homeError = null
        try {
            val rootFiles = api.listFiles("/")
            summary = api.getFilesSummary()
            quota = api.getUserQuota()
            recentFiles = rootFiles
                .filter { it.type == "file" }
                .sortedByDescending { it.lastmod }
                .take(3)
        } catch (e: Throwable) {
            homeError = describeError(e)
        } finally {
            isLoadingHome = false
        }
    }

    fun refreshHome() {
        scope.launch { loadHome() }
    }

    // ==================== 文件 ====================

    suspend fun loadFiles(path: String) {
        if (user == null) return
        isLoadingFiles = true
        filesError = null
        try {
            files = api.listFiles(path)
            currentPath = path
        } catch (e: Throwable) {
            filesError = describeError(e)
        } finally {
            isLoadingFiles = false
        }
    }

    fun refreshFiles() {
        scope.launch { loadFiles(currentPath) }
    }

    fun enterDirectory(item: FileNodeDto) {
        if (item.type != "directory") return
        pathHistory = pathHistory + currentPath
        scope.launch { loadFiles(joinPath(currentPath, item.basename)) }
    }

    fun goUp() {
        val previous = pathHistory.lastOrNull() ?: return
        pathHistory = pathHistory.dropLast(1)
        scope.launch { loadFiles(previous) }
    }

    fun mkdir(name: String) {
        if (name.isBlank()) return
        scope.launch {
            try {
                api.mkdir(MkdirRequest(joinPath(currentPath, name.trim())))
                actionMessage = "文件夹已创建"
                loadFiles(currentPath)
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    fun deleteFile(item: FileNodeDto) {
        scope.launch {
            try {
                api.delete(DeleteRequest(path = item.filename, id = item.id))
                actionMessage = "已删除「${item.basename}」"
                loadFiles(currentPath)
                loadHome()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    fun shareFile(item: FileNodeDto) {
        scope.launch {
            try {
                val response = api.createShare(
                    ShareCreateRequest(
                        fileId = item.id,
                        filePath = item.filename,
                        fileName = item.basename,
                        expirationType = "permanent",
                    ),
                )
                val code = response.activeShare?.extractionCode
                actionMessage = buildString {
                    append("分享链接：https://user.edgecos.com/s/")
                    append(response.shareId)
                    if (code != null) append("\n提取码：$code")
                }
                loadFiles(currentPath)
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    fun directLink(item: FileNodeDto) {
        scope.launch {
            try {
                val response = api.generateDirectLink(
                    DirectLinkCreateRequest(fileId = item.id, filePath = item.filename),
                )
                actionMessage = "直链：${response.directUrl}"
                openExternalUrl(response.directUrl)
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    // ==================== 上传 ====================

    /** 打开系统文件选择器并上传到当前目录。 */
    fun pickAndUpload() {
        if (user == null) {
            actionMessage = "请先登录"
            return
        }
        scope.launch {
            val picked = try {
                pickFileForUpload()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
                null
            } ?: return@launch
            upload(picked)
        }
    }

    /** 上传流程与 Android `Uploader` 一致：秒传判断 → OSS 直传 → 完成回调。 */
    private fun upload(picked: PickedFile) {
        val size = picked.bytes.size.toLong()
        if (size <= 0L) {
            actionMessage = "无法读取文件"
            return
        }
        val targetPath = currentPath
        val task = TransferTask(
            id = randomRequestId(),
            fileName = picked.name,
            direction = TransferDirection.Upload,
            size = size,
        )
        transferTasks = transferTasks + task

        scope.launch {
            try {
                val hash = "sha256:${sha256Hex(picked.bytes)}:$size"
                val token = api.getUploadToken(picked.name, size, targetPath, hash, overwrite = false)
                if (token.instant) {
                    api.uploadInstantComplete(
                        UploadInstantCompleteRequest(picked.name, targetPath, size, hash),
                    )
                    task.progress = 1f
                } else {
                    val putUrl = token.url ?: error("服务端未返回上传地址")
                    api.uploadRaw(putUrl, picked.bytes) { sent, total ->
                        if (total > 0L) {
                            task.progress = (sent.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                    val key = token.key ?: error("服务端未返回 OSS key")
                    api.uploadComplete(
                        UploadCompleteRequest(
                            key = key,
                            filename = picked.name,
                            path = targetPath,
                            size = size,
                            hash = hash,
                        ),
                    )
                    task.progress = 1f
                }
                task.status = TransferStatus.Success
                loadFiles(targetPath)
                loadHome()
            } catch (e: Throwable) {
                task.status = TransferStatus.Failed
                task.message = describeError(e)
            }
        }
    }

    fun clearFinishedTransfers() {
        transferTasks = transferTasks.filter { it.status == TransferStatus.Running }
    }

    // ==================== 套餐 / 订单 ====================

    suspend fun loadMyPlan() {
        if (user == null) return
        try {
            myPlan = api.getMyPlan()
        } catch (e: Throwable) {
            billingError = describeError(e)
        }
    }

    suspend fun loadPlans() {
        if (user == null) return
        isLoadingBilling = true
        billingError = null
        try {
            plans = api.getPlans()
        } catch (e: Throwable) {
            billingError = describeError(e)
        } finally {
            isLoadingBilling = false
        }
    }

    suspend fun loadOrders() {
        if (user == null) return
        isLoadingBilling = true
        billingError = null
        try {
            orders = api.getOrders()
        } catch (e: Throwable) {
            billingError = describeError(e)
        } finally {
            isLoadingBilling = false
        }
    }

    fun refreshOrders() {
        scope.launch { loadOrders() }
    }

    fun refreshPlans() {
        scope.launch {
            loadMyPlan()
            loadPlans()
        }
    }

    fun cancelOrder(order: OrderDto) {
        scope.launch {
            try {
                api.cancelOrder(com.edgecos.mobileapp.shared.dto.PayCancelRequest(order.tradeNo))
                actionMessage = "订单已取消"
                loadOrders()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    fun purchase(plan: PlanDto, method: String) {
        scope.launch {
            try {
                val response = api.createPayment(
                    com.edgecos.mobileapp.shared.dto.PayCreateRequest(
                        planId = plan.id,
                        paymentMethod = method,
                        clientRequestId = randomRequestId(),
                    ),
                )
                actionMessage = when {
                    response.payUrl != null -> "已创建订单，支付链接：${response.payUrl}"
                    response.notice != null -> response.notice
                    else -> "订单已创建"
                }
                response.payUrl?.let { openExternalUrl(it) }
                loadOrders()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    // ==================== 公告 / 工单 / 分享管理 ====================

    suspend fun loadAnnouncements() {
        if (user == null) return
        isLoadingAnnouncements = true
        announcementsError = null
        try {
            announcements = api.getAnnouncements().list
        } catch (e: Throwable) {
            announcementsError = describeError(e)
        } finally {
            isLoadingAnnouncements = false
        }
    }

    fun markAnnouncementRead(id: Long) {
        scope.launch {
            runCatching { api.markAnnouncementRead(id) }
            loadAnnouncements()
        }
    }

    fun refreshAnnouncements() {
        scope.launch { loadAnnouncements() }
    }

    suspend fun loadWorkOrders() {
        if (user == null) return
        isLoadingWorkOrders = true
        workOrdersError = null
        try {
            workOrders = api.listWorkOrders()
        } catch (e: Throwable) {
            workOrdersError = describeError(e)
        } finally {
            isLoadingWorkOrders = false
        }
    }

    fun refreshWorkOrders() {
        scope.launch { loadWorkOrders() }
    }

    fun submitWorkOrder(title: String, content: String, category: String) {
        scope.launch {
            try {
                api.submitWorkOrder(WorkOrderSubmitRequest(title = title, content = content, category = category))
                actionMessage = "工单已提交"
                loadWorkOrders()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    suspend fun loadSharedFiles() {
        if (user == null) return
        isLoadingShared = true
        sharedError = null
        try {
            sharedFiles = api.listFiles("/").filter { it.activeShare != null }
        } catch (e: Throwable) {
            sharedError = describeError(e)
        } finally {
            isLoadingShared = false
        }
    }

    fun refreshSharedFiles() {
        scope.launch { loadSharedFiles() }
    }

    fun closeShare(item: FileNodeDto) {
        scope.launch {
            try {
                api.closeShare(item.id)
                actionMessage = "已取消「${item.basename}」的分享"
                loadSharedFiles()
            } catch (e: Throwable) {
                actionMessage = describeError(e)
            }
        }
    }

    // ==================== 注册 ====================

    suspend fun register(username: String, email: String, code: String, password: String): String? {
        return try {
            val precheck = api.captchaPrecheck(CaptchaPrecheckRequest())
            api.register(
                RegisterRequest(
                    username = username.trim(),
                    email = email.trim(),
                    verifyCode = code.trim(),
                    password = password,
                    captchaData = com.edgecos.mobileapp.shared.dto.CaptchaData(ticket = "", randstr = ""),
                    captchaPrecheckToken = precheck.token,
                ),
            )
            goMain(0)
            loadHome()
            loadFiles("/")
            null
        } catch (e: Throwable) {
            describeError(e)
        }
    }

    private fun randomRequestId(): String {
        val chars = "0123456789abcdef"
        return buildString {
            repeat(32) { append(chars[kotlin.random.Random.nextInt(chars.length)]) }
        }
    }
}

/**
 * Compose 版根界面：与 Android 一致，启动即进入主界面（首页），
 * 未登录时首页展示空态，文件/传输/我的点击后引导登录；登录是独立页面，可返回。
 */
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val store = remember { AppStore(scope) }

    EdgeTheme(dark = store.darkTheme) {
        when (store.route) {
            Route.Main -> MainScreen(store)
            Route.Login -> LoginScreen(store)
            Route.Register -> RegisterScreen(store)
            Route.Plan -> PlanScreen(store)
            Route.Wallet -> WalletScreen(store)
            Route.WorkOrder -> WorkOrderScreen(store)
            Route.Announcements -> AnnouncementsScreen(store)
            Route.ShareManagement -> ShareManagementScreen(store)
            Route.Orders -> OrdersScreen(store)
            Route.Settings -> SettingsScreen(store)
            Route.About -> AboutScreen(store)
            Route.Oss -> OpenSourceScreen(store)
            Route.LegalAgreement -> LegalScreen(store, privacy = false)
            Route.LegalPrivacy -> LegalScreen(store, privacy = true)
            Route.Preview -> PreviewScreen(store)
        }

        store.actionMessage?.let { message ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { store.actionMessage = null },
                title = { androidx.compose.material3.Text("提示") },
                text = { androidx.compose.material3.Text(message) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { store.actionMessage = null },
                    ) { androidx.compose.material3.Text("确定") }
                },
            )
        }
    }
}
