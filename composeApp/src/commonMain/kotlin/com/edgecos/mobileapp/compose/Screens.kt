package com.edgecos.mobileapp.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgecos.mobileapp.shared.dto.AnnouncementDto
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import com.edgecos.mobileapp.shared.dto.MyPlanResponse
import com.edgecos.mobileapp.shared.dto.OrderDto
import com.edgecos.mobileapp.shared.dto.PlanDto
import com.edgecos.mobileapp.shared.dto.WorkOrderDto
import kotlinx.coroutines.launch

private const val APP_VERSION = "1.1.3"

// ==================== 主界面（四个 Tab） ====================

private val mainTabs = listOf(
    GlassTabItem("首页", Icons.Outlined.Home, Icons.Filled.Home),
    GlassTabItem("文件", Icons.Outlined.Folder, Icons.Filled.Folder),
    GlassTabItem("传输", Icons.Outlined.SwapVert, Icons.Filled.SwapVert),
    GlassTabItem("我的", Icons.Outlined.Person, Icons.Filled.Person),
)

@Composable
fun MainScreen(store: AppStore) {
    var showLoginPrompt by remember { mutableStateOf(false) }

    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text("需要登录") },
            text = { Text("登录后即可使用该功能") },
            confirmButton = {
                TextButton(onClick = {
                    showLoginPrompt = false
                    store.open(Route.Login)
                }) { Text("登录") }
            },
            dismissButton = {
                TextButton(onClick = { showLoginPrompt = false }) { Text("取消") }
            },
        )
    }

    // 与 Android 一致：内容可延伸到 TabBar 下方，TabBar 以浮层形式覆盖在底部
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(bottom = 84.dp)) {
            when (store.selectedTab) {
                0 -> HomeTab(store)
                1 -> FilesTab(store)
                2 -> TransferTab(store)
                else -> ProfileTab(store)
            }
        }
        GlassTabBar(
            items = mainTabs,
            selectedIndex = store.selectedTab,
            onSelect = { index ->
                if (index != 0 && store.user == null) {
                    showLoginPrompt = true
                } else {
                    store.selectedTab = index
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ==================== 登录 / 注册 ====================

@Composable
fun LoginScreen(store: AppStore) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("登录", onBack = { store.back() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            CloudLogo()
            Spacer(Modifier.height(12.dp))
            Text("EdgeCOS", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "登录您的 EdgeCOS 账号",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("账号 / 手机号 / 邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            store.loginError?.let { error ->
                Spacer(Modifier.height(10.dp))
                Text(
                    error,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (store.isLoadingLogin) "登录中…" else "登录",
                enabled = !store.isLoadingLogin && username.isNotBlank() && password.isNotEmpty(),
                onClick = { store.loginAsync(username.trim(), password) },
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { store.open(Route.Register) }) { Text("还没有账号？新用户注册") }
        }
    }
}

@Composable
fun RegisterScreen(store: AppStore) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("注册", onBack = { store.back() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CloudLogo(size = 72)
            Spacer(Modifier.height(12.dp))
            Text("新用户注册", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "注册后即可在 iOS 端同步文件",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("邮箱验证码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "注册需通过图形验证码校验，iOS 端暂未接入验证码 SDK；若提交失败，请前往网页版注册。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (isSubmitting) "提交中…" else "注册",
                enabled = !isSubmitting && username.isNotBlank() && email.isNotBlank() &&
                    code.isNotBlank() && password.isNotBlank(),
                onClick = {
                    isSubmitting = true
                    error = null
                    scope.launch {
                        error = store.register(username, email, code, password)
                        isSubmitting = false
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { store.back() }) { Text("已有账号？返回登录") }
        }
    }
}

// ==================== 首页 ====================

@Composable
private fun HomeTab(store: AppStore) {
    val user = store.user
    if (user == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CloudLogo()
            Spacer(Modifier.height(8.dp))
            Text(
                "欢迎使用EdgeCOS",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            PrimaryButton("登录") { store.open(Route.Login) }
            Spacer(Modifier.height(10.dp))
            SecondaryButton("新用户注册") { store.open(Route.Register) }
        }
        return
    }

    LaunchedEffect(user.id) {
        if (store.recentFiles.isEmpty()) store.loadHome()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("首页")
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList(title = "最近文件") {
                if (store.isLoadingHome && store.recentFiles.isEmpty()) {
                    LoadingBlock()
                } else if (store.recentFiles.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        EmptyState(title = "最近无文件")
                    }
                } else {
                    store.recentFiles.forEachIndexed { index, file ->
                        NavRow(
                            label = file.basename,
                            icon = if (file.type == "directory") Icons.Filled.List else Icons.Filled.DateRange,
                            value = if (file.activeShare != null) "已分享" else null,
                            showDivider = index < store.recentFiles.lastIndex,
                            onClick = {
                                store.previewFile = file
                                store.open(Route.Preview)
                            },
                        )
                    }
                }
            }

            store.homeError?.let { err ->
                Spacer(Modifier.height(12.dp))
                Text(
                    err,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            GroupedList(title = "存储空间") {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val quota = store.quota
                    if (quota != null) {
                        val total = quota.quota.toLongOrNull() ?: 0L
                        val used = quota.used.toLongOrNull() ?: 0L
                        val ratio = if (total > 0) (used.toDouble() / total).coerceIn(0.0, 1.0).toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = if (ratio >= 0.9f) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("已用 ${formatBytes(quota.used)}", fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            Text("共 ${formatBytes(quota.quota)}", fontSize = 13.sp)
                        }
                    } else {
                        Text("暂无数据", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            GroupedList(title = "概览") {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SummaryCell("文件", store.summary?.fileCount, Modifier.weight(1f))
                    SummaryCell("文件夹", store.summary?.folderCount, Modifier.weight(1f))
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    SummaryCell("最近", store.summary?.recentCount, Modifier.weight(1f))
                    SummaryCell("分享", store.summary?.sharedCount, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCell(title: String, value: Long?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value?.toString() ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== 文件 ====================

@Composable
private fun FilesTab(store: AppStore) {
    var detailFile by remember { mutableStateOf<FileNodeDto?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    LaunchedEffect(store.currentPath, store.user?.id) {
        if (store.files.isEmpty()) store.loadFiles(store.currentPath)
    }

    if (detailFile != null) {
        val file = detailFile!!
        AlertDialog(
            onDismissRequest = { detailFile = null },
            title = { Text(file.basename, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow("大小", if (file.type == "directory") "文件夹" else formatBytes(file.size))
                    InfoRow("修改时间", file.lastmod.ifBlank { "—" })
                    InfoRow("路径", file.filename)
                    if (file.activeShare != null) InfoRow("分享", "已分享")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    detailFile = null
                    store.previewFile = file
                    store.open(Route.Preview)
                }) { Text("打开") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { detailFile = null; store.shareFile(file) }) { Text("分享") }
                    TextButton(onClick = { detailFile = null; store.directLink(file) }) { Text("直链") }
                    TextButton(onClick = { detailFile = null; store.deleteFile(file) }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { detailFile = null }) { Text("取消") }
                }
            },
        )
    }

    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("文件夹名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.mkdir(newFolderName)
                    newFolderName = ""
                    showNewFolder = false
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showNewFolder = false }) { Text("取消") } },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(
            "文件",
            actions = {
                TextButton(onClick = { store.pickAndUpload() }) { Text("上传") }
                TextButton(onClick = { showNewFolder = true }) { Text("新建") }
                TextButton(onClick = { store.refreshFiles() }) { Text("刷新") }
            },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (store.pathHistory.isNotEmpty()) {
                TextButton(onClick = { store.goUp() }) { Text("‹ 上级") }
            }
            Text(
                store.currentPath,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        when {
            store.isLoadingFiles && store.files.isEmpty() -> LoadingBlock()
            store.filesError != null && store.files.isEmpty() ->
                ErrorBlock(store.filesError!!, onRetry = { store.refreshFiles() })
            store.files.isEmpty() -> EmptyState("当前目录为空", "点击右上角「上传」选择文件，或「新建」创建文件夹")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(store.files, key = { it.id }) { item ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.type == "directory") store.enterDirectory(item) else detailFile = item
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FileBadge(isDirectory = item.type == "directory", fileName = item.basename)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.basename,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    formatApiTime(item.lastmod).ifBlank { "—" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (item.activeShare != null) {
                                Text("已分享", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(start = 64.dp))
                    }
                }
            }
        }
    }
}

// ==================== 传输 ====================

@Composable
private fun TransferTab(store: AppStore) {
    var selected by remember { mutableStateOf(0) }
    val direction = if (selected == 0) TransferDirection.Upload else TransferDirection.Download
    val tasks = store.transferTasks.filter { it.direction == direction }
    val hasFinished = store.transferTasks.any { it.status != TransferStatus.Running }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(
            "传输",
            actions = {
                if (hasFinished) {
                    TextButton(onClick = { store.clearFinishedTransfers() }) { Text("清除已完成") }
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        SegmentedControl(
            options = listOf("上传", "下载"),
            selectedIndex = selected,
            onSelect = { selected = it },
        )
        Spacer(Modifier.height(8.dp))

        when {
            tasks.isNotEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(tasks, key = { it.id }) { task -> TransferRow(task) }
            }
            selected == 0 -> EmptyState(title = "暂无上传任务")
            else -> EmptyState(title = "暂无下载任务")
        }
    }
}

@Composable
private fun TransferRow(task: TransferTask) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                task.fileName,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (task.status) {
                    TransferStatus.Running -> "${(task.progress * 100).toInt()}%"
                    TransferStatus.Success -> "已完成"
                    TransferStatus.Failed -> "失败"
                },
                fontSize = 12.sp,
                color = when (task.status) {
                    TransferStatus.Running -> MaterialTheme.colorScheme.onSurfaceVariant
                    TransferStatus.Success -> IOSColors.Green
                    TransferStatus.Failed -> MaterialTheme.colorScheme.error
                },
            )
        }
        if (task.status == TransferStatus.Running) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { task.progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            buildString {
                append(formatBytes(task.size))
                task.message?.let {
                    append(" · ")
                    append(it)
                }
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
    HorizontalDivider(thickness = 0.5.dp)
}

// ==================== 我的 ====================

@Composable
private fun ProfileTab(store: AppStore) {
    val user = store.user
    if (user == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("登录后管理账号与套餐", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            PrimaryButton("登录 / 注册") { store.open(Route.Login) }
        }
        return
    }

    LaunchedEffect(user.id) {
        if (store.myPlan == null) store.loadMyPlan()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("我的")
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList(title = "账户") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarBadge(user.username, size = 52)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(user.username, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            user.email ?: "未绑定邮箱",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (user.role == "admin") {
                            Text("管理员", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val plan = store.myPlan
                    if (plan == null) {
                        Text(
                            "套餐信息加载中…",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        PlanUsage(plan)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            GroupedList(title = "资源管理") {
                NavRow("分享管理", Icons.Filled.Share, IOSColors.Blue, onClick = { store.open(Route.ShareManagement) })
                NavRow("工单中心", Icons.Filled.Build, IOSColors.Orange, onClick = { store.open(Route.WorkOrder) })
                NavRow("公告通知", Icons.Filled.Notifications, IOSColors.Pink, showDivider = false, onClick = { store.open(Route.Announcements) })
            }

            Spacer(Modifier.height(16.dp))
            GroupedList(title = "套餐与账单") {
                NavRow("我的套餐", Icons.Filled.Star, IOSColors.Purple, onClick = { store.open(Route.Plan) })
                NavRow("我的订单", Icons.Filled.ShoppingCart, IOSColors.Teal, onClick = { store.open(Route.Orders) })
                NavRow("钱包", Icons.Filled.AccountCircle, IOSColors.Green, showDivider = false, onClick = { store.open(Route.Wallet) })
            }

            Spacer(Modifier.height(16.dp))
            GroupedList(title = "设置") {
                NavRow("设置", Icons.Filled.Settings, IOSColors.Blue, showDivider = false, onClick = { store.open(Route.Settings) })
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlanUsage(plan: MyPlanResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(plan.plan.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                plan.expireTime?.let { "到期 $it" } ?: "永久",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        UsageRow(
            label = "存储",
            used = formatBytes(plan.used),
            total = formatBytes(plan.quota),
            progress = if (plan.quota.toLongOrNull() != null && plan.quota.toLong() > 0) {
                (plan.used.toDouble() / plan.quota.toDouble()).coerceIn(0.0, 1.0).toFloat()
            } else 0f,
            color = MaterialTheme.colorScheme.primary,
        )
        UsageRow(
            label = "本月流量",
            used = formatBytes(plan.monthlyTrafficUsed),
            total = formatBytes(plan.trafficQuota),
            progress = (plan.trafficUsagePercent / 100.0).toFloat().coerceIn(0f, 1f),
            color = when (plan.trafficStatus) {
                "exhausted" -> MaterialTheme.colorScheme.error
                "warning" -> IOSColors.Orange
                else -> MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun UsageRow(label: String, used: String, total: String, progress: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, fontSize = 13.sp)
            Spacer(Modifier.weight(1f))
            Text("$used / $total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(5.dp),
            color = color,
        )
    }
}

// ==================== 套餐 ====================

@Composable
fun PlanScreen(store: AppStore) {
    var purchasePlan by remember { mutableStateOf<PlanDto?>(null) }

    LaunchedEffect(Unit) {
        if (store.myPlan == null) store.loadMyPlan()
        if (store.plans.isEmpty()) store.loadPlans()
    }

    if (purchasePlan != null) {
        val plan = purchasePlan!!
        AlertDialog(
            onDismissRequest = { purchasePlan = null },
            title = { Text("购买 ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoRow("存储空间", formatBytes(plan.quota))
                    InfoRow("流量", formatBytes(plan.trafficQuota))
                    InfoRow("价格", "¥ ${plan.price}")
                    InfoRow("时长", if (plan.duration == 0L) "永久" else "${plan.duration} 个月")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.purchase(plan, "alipay")
                    purchasePlan = null
                }) { Text("支付宝支付") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        store.purchase(plan, "wxpay")
                        purchasePlan = null
                    }) { Text("微信支付") }
                    TextButton(onClick = { purchasePlan = null }) { Text("取消") }
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("套餐", onBack = { store.back() })
        if (store.isLoadingBilling && store.myPlan == null && store.plans.isEmpty()) {
            LoadingBlock()
            return@PlanScreen
        }
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))

            if (store.billingError != null) {
                ErrorBlock(store.billingError!!, onRetry = { store.refreshPlans() })
            }

            store.myPlan?.let { plan ->
                GroupedList(title = "当前套餐") {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        PlanUsage(plan)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            if (store.plans.isEmpty()) {
                EmptyState("暂无可购买套餐")
            } else {
                store.plans.forEach { plan ->
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 1.dp,
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(plan.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "存储 ${formatBytes(plan.quota)} · 流量 ${formatBytes(plan.trafficQuota)}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("¥ ${plan.price}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { purchasePlan = plan }) { Text("购买") }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(
                onClick = { store.open(Route.Orders) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("查看我的订单") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 钱包 ====================

@Composable
fun WalletScreen(store: AppStore) {
    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("钱包", onBack = { store.back() })
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("钱包余额", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text("¥ 0.00", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    PrimaryButton("充值") { store.actionMessage = "钱包充值请在网页端完成" }
                }
                Box(Modifier.weight(1f)) {
                    SecondaryButton("提现") { store.actionMessage = "钱包提现在网页端完成" }
                }
            }
            Spacer(Modifier.height(16.dp))
            GroupedList(title = "交易记录") {
                EmptyState("暂无交易记录", "充值与消费记录将在后续版本接入")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 订单 ====================

@Composable
fun OrdersScreen(store: AppStore) {
    var filter by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (store.orders == null) store.loadOrders()
    }

    val all = store.orders?.list.orEmpty()
    val filtered = when (filter) {
        1 -> all.filter { it.status == "pending" }
        2 -> all.filter { it.status == "paid" }
        3 -> all.filter { it.status == "cancelled" }
        else -> all
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("订单", onBack = { store.back() })
        Spacer(Modifier.height(12.dp))
        SegmentedControl(
            options = listOf("全部", "待支付", "已支付", "已取消"),
            selectedIndex = filter,
            onSelect = { filter = it },
        )
        Spacer(Modifier.height(8.dp))

        when {
            store.isLoadingBilling && all.isEmpty() -> LoadingBlock()
            store.billingError != null -> ErrorBlock(store.billingError!!, onRetry = {
                store.refreshOrders()
            })
            all.isEmpty() -> EmptyState("暂无订单", "在套餐页购买套餐后订单会显示在此")
            filtered.isEmpty() -> EmptyState("该状态下暂无订单")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { order ->
                    OrderCard(order, onCancel = { store.cancelOrder(order) })
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderDto, onCancel: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.Plan?.name ?: "套餐订单", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        statusLabel(order.status),
                        fontSize = 12.sp,
                        color = when (order.status) {
                            "paid" -> IOSColors.Green
                            "pending" -> IOSColors.Orange
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text("¥ ${order.amount}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("订单号 ${order.tradeNo}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                order.payTime?.let {
                    Text("支付时间 $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (order.status == "pending") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onCancel) { Text("取消订单") }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "pending" -> "待支付"
    "paid" -> "已支付"
    "cancelled" -> "已取消"
    "expired" -> "已过期"
    else -> status
}

// ==================== 公告 ====================

@Composable
fun AnnouncementsScreen(store: AppStore) {
    LaunchedEffect(Unit) {
        if (store.announcements.isEmpty()) store.loadAnnouncements()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("公告通知", onBack = { store.back() })
        when {
            store.isLoadingAnnouncements && store.announcements.isEmpty() -> LoadingBlock()
            store.announcementsError != null -> ErrorBlock(store.announcementsError!!, onRetry = {
                store.refreshAnnouncements()
            })
            store.announcements.isEmpty() -> EmptyState("暂无公告")
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(store.announcements, key = { it.id }) { ann ->
                    AnnouncementCard(ann, onClick = { store.markAnnouncementRead(ann.id) })
                }
            }
        }
    }
}

@Composable
private fun AnnouncementCard(ann: AnnouncementDto, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!ann.isRead) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(ann.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                Text(
                    ann.createdAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    ann.content,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

// ==================== 工单 ====================

private val workOrderCategories = listOf(
    "file_download_cdn" to "CDN 下载",
    "upload" to "上传问题",
    "account" to "账号问题",
    "billing" to "计费问题",
    "other" to "其他",
)

@Composable
fun WorkOrderScreen(store: AppStore) {
    var showSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (store.workOrders.isEmpty()) store.loadWorkOrders()
    }

    if (showSubmit) {
        SubmitWorkOrderDialog(
            onDismiss = { showSubmit = false },
            onSubmit = { title, content, category ->
                store.submitWorkOrder(title, content, category)
                showSubmit = false
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(
            "工单中心",
            onBack = { store.back() },
            actions = { TextButton(onClick = { showSubmit = true }) { Text("新建") } },
        )
        when {
            store.isLoadingWorkOrders && store.workOrders.isEmpty() -> LoadingBlock()
            store.workOrdersError != null -> ErrorBlock(store.workOrdersError!!, onRetry = {
                store.refreshWorkOrders()
            })
            store.workOrders.isEmpty() -> EmptyState("暂无工单", "点击右上角「新建」提交问题")
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(store.workOrders, key = { it.id }) { order ->
                    WorkOrderCard(order)
                }
            }
        }
    }
}

@Composable
private fun WorkOrderCard(order: WorkOrderDto) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        when (order.status) {
                            "open" -> "处理中"
                            "closed" -> "已关闭"
                            "pending" -> "待处理"
                            else -> order.status
                        },
                        fontSize = 12.sp,
                        color = IOSColors.Orange,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    workOrderCategories.firstOrNull { it.first == order.category }?.second ?: order.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(order.content, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                Text(
                    order.createdAt,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                order.WorkOrderReplies?.takeIf { it.isNotEmpty() }?.let { replies ->
                    Text(
                        "共 ${replies.size} 条回复",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubmitWorkOrderDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var categoryIndex by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交工单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("问题描述") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                )
                Text("分类", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    workOrderCategories.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryIndex = index }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.second, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (index == categoryIndex) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && content.isNotBlank(),
                onClick = { onSubmit(title, content, workOrderCategories[categoryIndex].first) },
            ) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

// ==================== 分享管理 ====================

@Composable
fun ShareManagementScreen(store: AppStore) {
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        store.loadSharedFiles()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("分享管理", onBack = { store.back() })
        when {
            store.isLoadingShared && store.sharedFiles.isEmpty() -> LoadingBlock()
            store.sharedError != null -> ErrorBlock(store.sharedError!!, onRetry = { store.refreshSharedFiles() })
            store.sharedFiles.isEmpty() -> EmptyState("暂无分享", "在文件页点击文件可创建分享")
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(store.sharedFiles, key = { it.id }) { file ->
                    SharedFileCard(
                        file = file,
                        onCopy = {
                            val share = file.activeShare
                            if (share != null) {
                                val text = buildString {
                                    append("https://user.edgecos.com/s/")
                                    append(share.shareId)
                                    share.extractionCode?.let { append("\n提取码：$it") }
                                }
                                clipboard.setText(AnnotatedString(text))
                                store.actionMessage = "分享链接已复制"
                            }
                        },
                        onClose = { store.closeShare(file) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedFileCard(file: FileNodeDto, onCopy: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FileBadge(isDirectory = file.type == "directory", fileName = file.basename)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(file.basename, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        val share = file.activeShare
                        val detail = buildString {
                            share?.extractionCode?.let { append("提取码：$it  ") }
                            share?.expiration?.let { append("过期：$it") }
                            if (isEmpty()) append("永久有效")
                        }
                        Text(detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCopy) { Text("复制链接") }
                    TextButton(onClick = onClose) { Text("取消分享", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

// ==================== 设置 ====================

@Composable
fun SettingsScreen(store: AppStore) {
    val lineOptions = listOf("每次询问", "默认 CDN 线路", "默认免费线路")

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("设置", onBack = { store.back() })
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("外观".uppercase(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
                SegmentedControl(
                    options = listOf("跟随系统", "浅色", "深色"),
                    selectedIndex = if (store.darkTheme) 2 else 1,
                    onSelect = { store.darkTheme = it == 2 },
                )
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "文件传输") {
                NavRow(
                    "预览线路偏好",
                    Icons.Filled.Star,
                    IOSColors.Blue,
                    value = store.linePreference,
                    onClick = {
                        val next = (lineOptions.indexOf(store.linePreference) + 1) % lineOptions.size
                        store.linePreference = lineOptions[next]
                    },
                )
                NavRow(
                    "下载线路偏好",
                    Icons.Filled.Refresh,
                    IOSColors.Green,
                    value = store.linePreference,
                    showDivider = false,
                    onClick = {
                        val next = (lineOptions.indexOf(store.linePreference) + 1) % lineOptions.size
                        store.linePreference = lineOptions[next]
                    },
                )
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "通知") {
                NavRow(
                    "通知权限",
                    Icons.Filled.Notifications,
                    IOSColors.Red,
                    value = "跟随系统",
                    showDivider = false,
                    onClick = { store.actionMessage = "请在系统设置中管理 EdgeCOS 的通知权限" },
                )
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "账号") {
                NavRow("修改密码", Icons.Filled.Lock, IOSColors.Blue, onClick = {
                    store.actionMessage = "修改密码请在网页端完成"
                })
                NavRow("绑定邮箱", Icons.Filled.Email, IOSColors.Green, onClick = {
                    store.actionMessage = "绑定邮箱请在网页端完成"
                })
                NavRow("实名认证", Icons.Filled.Check, IOSColors.Purple, showDivider = false, onClick = {
                    store.actionMessage = "实名认证请在网页端完成"
                })
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "关于") {
                NavRow("关于", Icons.Filled.Info, IOSColors.Orange, showDivider = false, onClick = { store.open(Route.About) })
            }

            Spacer(Modifier.height(24.dp))
            SecondaryButton("退出登录") {
                store.logout()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 关于 / 开源 ====================

@Composable
fun AboutScreen(store: AppStore) {
    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("关于", onBack = { store.back() })
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CloudLogo(size = 64)
                    Spacer(Modifier.height(8.dp))
                    Text("EdgeCOS", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("版本 $APP_VERSION", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("边缘云存储客户端", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "更新") {
                NavRow("检查更新", Icons.Filled.Refresh, IOSColors.Green, value = "v$APP_VERSION", showDivider = false, onClick = {
                    store.actionMessage = "当前已是最新版本"
                })
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "法律与协议") {
                NavRow("用户服务协议", Icons.Filled.Info, IOSColors.Blue, onClick = { store.open(Route.LegalAgreement) })
                NavRow("隐私政策", Icons.Filled.Lock, IOSColors.Purple, showDivider = false, onClick = { store.open(Route.LegalPrivacy) })
            }

            Spacer(Modifier.height(20.dp))
            GroupedList(title = "开源") {
                NavRow("开源致谢", Icons.Filled.Star, IOSColors.Orange, value = "查看使用的开源项目", showDivider = false, onClick = { store.open(Route.Oss) })
            }

            Spacer(Modifier.height(20.dp))
            GroupedList {
                NavRow("访问官网", Icons.Filled.LocationOn, IOSColors.Teal, showDivider = false, onClick = {
                    openExternalUrl("https://user.edgecos.com")
                })
            }

            Spacer(Modifier.height(16.dp))
            val copyright = listOf(
                "Copyright © 2026 EdgeCOS. All Rights Reserved.",
                "版权所有，未经许可，不得对本软件进行逆向工程、修改或二次分发",
                "ICP备案号：蜀ICP备2025117706号-4",
                "公安网备案号：川公网安备51010602003341号",
            )
            copyright.forEach {
                Text(
                    it,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun OpenSourceScreen(store: AppStore) {
    val items = listOf(
        "Kotlin" to "JetBrains · Apache-2.0",
        "Compose Multiplatform" to "JetBrains · Apache-2.0",
        "Ktor" to "JetBrains · Apache-2.0",
        "kotlinx.serialization" to "JetBrains · Apache-2.0",
        "kotlinx.coroutines" to "JetBrains · Apache-2.0",
        "OkHttp" to "Square · Apache-2.0",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("开源致谢", onBack = { store.back() })
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList(title = "开源项目") {
                items.forEachIndexed { index, item ->
                    NavRow(
                        item.first,
                        Icons.Filled.Build,
                        IOSColors.Orange,
                        value = item.second,
                        showDivider = index < items.lastIndex,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "感谢以上开源项目为 EdgeCOS 提供基础能力。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ==================== 协议 / 隐私 ====================

@Composable
fun LegalScreen(store: AppStore, privacy: Boolean) {
    val title = if (privacy) "隐私政策" else "用户服务协议"
    val paragraphs = if (privacy) {
        listOf(
            "一、我们收集的信息",
            "为提供云存储服务，我们会收集账号信息（用户名、邮箱）、文件元数据（文件名、大小、修改时间）以及必要的设备与日志信息。",
            "二、信息的使用",
            "收集的信息仅用于文件同步、空间统计、故障排查与安全防护，不会用于与服务无关的用途。",
            "三、信息的存储与保护",
            "文件内容存储于 EdgeCOS 云端节点，传输过程使用 HTTPS 加密。我们采取访问控制与审计措施防止未授权访问。",
            "四、信息的共享",
            "除法律法规要求或你主动创建分享链接外，我们不会向第三方提供你的文件内容。",
            "五、你的权利",
            "你可以随时查看、修改、删除自己的文件，也可以注销账号。注销后相关数据将按法规要求处理。",
        )
    } else {
        listOf(
            "一、服务内容",
            "EdgeCOS 提供边缘云存储、文件上传下载、分享与预览等服务。部分功能需登录后使用。",
            "二、账号与安全",
            "你应妥善保管账号密码，不得将账号转借他人。因账号泄露造成的损失由使用者自行承担。",
            "三、使用规范",
            "不得上传违法、侵权或恶意内容。我们有权对违规内容采取限制措施并保留追究责任的权利。",
            "四、套餐与计费",
            "套餐、流量与存储配额以购买页面展示为准。虚拟商品一经使用，除服务故障外不支持退款。",
            "五、服务变更与终止",
            "我们可能因业务调整变更或终止部分服务，重大变更将提前公告。",
            "六、免责声明",
            "因不可抗力、网络故障或第三方原因导致的服务中断，我们将尽力恢复但不承担超出法定范围的赔偿责任。",
        )
    }
    val url = if (privacy) "https://user.edgecos.com/UserPrivacy" else "https://user.edgecos.com/UserAgreement"

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar(title, onBack = { store.back() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            paragraphs.forEachIndexed { index, text ->
                if (index % 2 == 0) {
                    Text(
                        text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 8.dp),
                    )
                } else {
                    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { openExternalUrl(url) },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    "查看完整条款：$url",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

// ==================== 文件预览 ====================

@Composable
fun PreviewScreen(store: AppStore) {
    val file = store.previewFile

    Column(modifier = Modifier.fillMaxSize()) {
        NavBar("预览", onBack = { store.back() })
        if (file == null) {
            EmptyState("未选择文件")
            return
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            GroupedList {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FileBadge(isDirectory = file.type == "directory", fileName = file.basename)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        file.basename,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        if (file.type == "directory") "文件夹" else formatBytes(file.size),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            GroupedList(title = "文件信息") {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    InfoRow("路径", file.filename)
                    InfoRow("修改时间", file.lastmod.ifBlank { "—" })
                    InfoRow("类型", if (file.type == "directory") "文件夹" else "文件")
                    InfoRow("分享状态", if (file.activeShare != null) "已分享" else "未分享")
                }
            }

            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("用直链打开") { store.directLink(file) }
                SecondaryButton("创建分享") { store.shareFile(file) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
