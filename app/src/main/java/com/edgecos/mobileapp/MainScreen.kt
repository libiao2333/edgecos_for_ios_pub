package com.edgecos.mobileapp

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.IOSTabItem
import com.edgecos.mobileapp.design.components.IOSGlassTabBar
import com.edgecos.mobileapp.design.components.LoginPromptSheet
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.feature.files.FilesScreen
import com.edgecos.mobileapp.feature.home.HomeScreen
import com.edgecos.mobileapp.feature.profile.ProfileScreen
import com.edgecos.mobileapp.feature.transfer.TransferScreen
import com.edgecos.mobileapp.feature.transfer.TransferViewModel
import java.net.URLEncoder

private val tabs = listOf(
    IOSTabItem(
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
        label = "首页",
    ),
    IOSTabItem(
        icon = Icons.Outlined.Folder,
        selectedIcon = Icons.Filled.Folder,
        label = "文件",
    ),
    IOSTabItem(
        icon = Icons.Outlined.SwapVert,
        selectedIcon = Icons.Filled.SwapVert,
        label = "传输",
    ),
    IOSTabItem(
        icon = Icons.Outlined.Person,
        selectedIcon = Icons.Filled.Person,
        label = "我的",
    ),
)

/**
 * 主界面：底部浮动胶囊 TabBar 导航（首页 / 文件 / 传输 / 我的）。
 *
 * 【iOS 26 设计】
 * - TabBar 浮动在内容之上（不贴边），胶囊形状，毛玻璃材质
 * - 内容区域延伸到屏幕底部（可滚动到 TabBar 下方）
 * - 给内容底部留出 TabBar 高度的额外空间（避免最后一条被完全遮挡）
 *
 * - 已登录：四个 Tab 各自展示对应 Screen。
 * - 未登录：首页展示空状态 + 登录按钮；点击"文件"/"传输"/"我的" Tab 弹出登录引导底部弹窗。
 *
 * 极简风格——只用图标 + 短文字，选中态用品牌色。
 */
@Composable
fun MainScreen(
    isLoggedIn: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigatePlan: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateWorkOrder: () -> Unit,
    onNavigateAnnouncements: () -> Unit,
    onNavigateShareManagement: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigatePreview: (String) -> Unit = {},
    onNavigateOrders: () -> Unit = {},
) {
    // 使用 rememberSaveable：跳转到次级页面（如设置）后返回时，Tab 选择状态需保留，
    // 实现"从哪来，回哪去"——从"我的"打开设置，返回时仍停留在"我的"。
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showLoginSheet by remember { mutableStateOf(false) }

    // 传输记录器常驻：在 MainScreen 组合期即创建 TransferViewModel（而非等用户打开"传输"Tab），
    // 使传输/上传事件总线始终有订阅者——否则未打开传输页时发生的事件会被直接丢弃，
    // 导致记录不落库、重启后丢失，也看不到进行中的任务。
    val transferViewModel: TransferViewModel = viewModel()

    // 登出（主动退出或 token 过期）后回到首页：未登录状态下"文件"/"传输"/"我的" Tab 为空白，
    // 停留无意义；重置为首页让用户看到登录引导。
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) selectedTab = 0
    }

    // 未登录时切换 Tab 的拦截：首页可访问，其他 Tab 弹登录引导
    fun onTabClick(index: Int) {
        if (!isLoggedIn && index != 0) {
            showLoginSheet = true
        } else {
            selectedTab = index
        }
    }

    // 【iOS 26 浮动 TabBar + 文件页 FAB 适配】
    // - 内容延伸到屏幕底部（可滚动到 TabBar 下方）
    // - TabBar 浮动在内容之上（叠放）
    // - 文件页 FAB 紧邻 TabBar 右侧，切到文件页时从左侧（TabBar 方向）水平弹出
    // - FAB 点击通过 trigger 通知 FilesScreen 展开菜单
    var filesFabClick by remember { mutableStateOf(0) }

    // 各 Tab 的 rememberSaveable 状态（如列表滚动位置）按 Tab 分开存档：
    // 切换 Tab 时内容树虽被移除（避免后台渲染开销），切回时从存档恢复，
    // 而非回到列表顶部——配合底部 Tab 的高频切换，保持“从哪离开、回哪继续”
    val saveableStateHolder = rememberSaveableStateHolder()

    Scaffold(
        bottomBar = {
            // 底部容器：TabBar + FAB（条件显示），同一水平线
            // 不使用 spacedBy：FAB 的占位宽度已包含与 TabBar 的间距，
            // 占位宽度随 visible 平滑动画，避免 TabBar 宽度突变造成割裂感
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = EdgeSpacing.lg)
                    .padding(bottom = EdgeSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // TabBar（不应用外部 padding，由父 Row 统一处理）
                IOSGlassTabBar(
                    modifier = Modifier.weight(1f),
                    items = tabs,
                    selectedIndex = selectedTab,
                    onSelect = { onTabClick(it) },
                    applyOuterPadding = false,
                )
                // 文件页 FAB（切到文件页时从左侧水平弹出）
                FilesTabFab(
                    visible = selectedTab == 1 && isLoggedIn,
                    onClick = { filesFabClick++ },
                )
            }
        },
    ) { innerPadding ->
        // 只应用 top padding（状态栏），不应用 bottom padding（让内容延伸到 TabBar 下方）
        val topPadding = innerPadding.calculateTopPadding()
        saveableStateHolder.SaveableStateProvider(selectedTab) {
            when (selectedTab) {
                0 -> HomeScreen(
                    modifier = Modifier.padding(top = topPadding),
                    isLoggedIn = isLoggedIn,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToRegister = onNavigateToRegister,
                    onNavigateAnnouncements = onNavigateAnnouncements,
                    onNavigateShareManagement = onNavigateShareManagement,
                    onNavigateWorkOrder = onNavigateWorkOrder,
                )
                1 -> if (isLoggedIn) {
                    FilesScreen(
                        modifier = Modifier.padding(top = topPadding),
                        fabClickTrigger = filesFabClick,
                        onNavigatePreview = { file ->
                            onNavigatePreview(
                                "preview/${file.id}/${URLEncoder.encode(file.filename, "UTF-8")}/${URLEncoder.encode(file.basename, "UTF-8")}/${file.size}/${file.type.name.lowercase()}"
                            )
                        },
                    )
                } else {
                    // 理论上不会到达，因为 onTabClick 拦截了
                    Box(modifier = Modifier.fillMaxSize())
                }
                2 -> if (isLoggedIn) {
                    TransferScreen(
                        modifier = Modifier.padding(top = topPadding),
                        viewModel = transferViewModel,
                    )
                } else {
                    // 理论上不会到达，因为 onTabClick 拦截了
                    Box(modifier = Modifier.fillMaxSize())
                }
                3 -> if (isLoggedIn) {
                    ProfileScreen(
                        modifier = Modifier.padding(top = topPadding),
                        onNavigateAbout = onNavigateAbout,
                        onNavigatePlan = onNavigatePlan,
                        onNavigateWallet = onNavigateWallet,
                        onNavigateWorkOrder = onNavigateWorkOrder,
                        onNavigateAnnouncements = onNavigateAnnouncements,
                        onNavigateShareManagement = onNavigateShareManagement,
                        onNavigateSettings = onNavigateSettings,
                        onNavigateOrders = onNavigateOrders,
                    )
                } else {
                    // 理论上不会到达，因为 onTabClick 拦截了
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    if (showLoginSheet) {
        LoginPromptSheet(
            onDismiss = { showLoginSheet = false },
            onLogin = {
                showLoginSheet = false
                onNavigateToLogin()
            },
            onRegister = {
                showLoginSheet = false
                onNavigateToRegister()
            },
        )
    }
}

/**
 * 文件页浮动按钮（紧邻 TabBar 右侧）。
 *
 * 【iOS 26 风格】切到文件页时从左侧（TabBar 方向）水平滑入，
 * 视觉上像是 TabBar 的一部分。
 *
 * 【实现原理】不使用 AnimatedVisibility（其进入时会立即分配空间，
 * 导致 TabBar 的 weight 宽度瞬间收缩产生割裂感），而是用动画化的
 * 占位宽度（slotWidth）让 TabBar 宽度随 FAB 占位平滑变化：
 * - slotWidth：0 → 56dp + spacing，平滑动画
 * - 内容 alpha + offset：实现滑入视觉
 *
 * @param visible 是否显示（切到文件页时为 true）
 * @param onClick 点击回调
 */
@Composable
private fun FilesTabFab(
    visible: Boolean,
    onClick: () -> Unit,
) {
    // 占位宽度：visible 时为 56dp + 间距，不可见时为 0dp
    // 与 TabBar 的 weight(1f) 平滑过渡，避免宽度突变
    // 提速：StiffnessLow(200) → StiffnessMedium(1500)，保留 LowBouncy 弹性形态，仅缩短响应
    val slotWidth by animateDpAsState(
        targetValue = if (visible) 56.dp + EdgeSpacing.sm else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fabSlotWidth",
    )
    // 透明度：配合淡入淡出（提速：StiffnessMediumLow → StiffnessMedium）
    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabAlpha",
    )
    // 缩放：从 TabBar 方向弹出（避免 offset 导致按钮被容器边界切割）
    // 提速：StiffnessLow(200) → StiffnessMedium(1500)，保留 LowBouncy 弹性形态
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fabScale",
    )

    // 占位容器：宽度平滑变化，内容居中对齐
    // 不使用 clipToBounds —— 允许缩放动画溢出容器边界，避免按钮被切割
    Box(
        modifier = Modifier
            .width(slotWidth)
            .height(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 圆形按钮，固定 56dp，与 TabBar 等高
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
                .alpha(contentAlpha)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .clickable(enabled = visible, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "添加",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
