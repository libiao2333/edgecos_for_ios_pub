package com.edgecos.mobileapp

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.edgecos.mobileapp.common.RecentlyOpenedStore
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.auth.AuthEventBus
import com.edgecos.mobileapp.data.auth.DeepLinkBus
import com.edgecos.mobileapp.design.animation.EdgeEasing
import com.edgecos.mobileapp.feature.about.AboutScreen
import com.edgecos.mobileapp.feature.about.LegalDocScreen
import com.edgecos.mobileapp.feature.about.OpenSourceAcknowledgementsScreen
import com.edgecos.mobileapp.feature.auth.LoginScreen
import com.edgecos.mobileapp.feature.auth.RegisterScreen
import com.edgecos.mobileapp.feature.profile.AnnouncementListScreen
import com.edgecos.mobileapp.feature.profile.OrderListScreen
import com.edgecos.mobileapp.feature.profile.PlanScreen
import com.edgecos.mobileapp.feature.profile.SettingsScreen
import com.edgecos.mobileapp.feature.profile.ShareManagementScreen
import com.edgecos.mobileapp.feature.profile.WalletScreen
import com.edgecos.mobileapp.feature.profile.WorkOrderScreen
import com.edgecos.mobileapp.feature.share.ShareAccessScreen
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.media.FilePreviewScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.net.URLDecoder

object Routes {
    const val MAIN = "main"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val ABOUT = "about"
    const val OSS = "oss"
    const val LEGAL = "legal/{type}"
    const val PLAN = "plan"
    const val WALLET = "wallet"
    const val WORK_ORDER = "work_order"
    const val ANNOUNCEMENTS = "announcements"
    const val SHARE_MGMT = "share_management"
    const val ORDERS = "orders"
    const val SHARE_ACCESS = "share_access/{shareId}"
    const val SETTINGS = "settings"
    const val PREVIEW = "preview/{id}/{filename}/{basename}/{size}/{type}"

    fun shareAccess(shareId: String) = "share_access/$shareId"

    object LegalType {
        const val AGREEMENT = "agreement"
        const val PRIVACY = "privacy"
    }

    fun legal(type: String) = "legal/$type"
}

/**
 * 法律文档 URL（公开路由，无需登录）。
 */
private object LegalUrls {
    const val AGREEMENT = "https://user.edgecos.com/UserAgreement"
    const val PRIVACY = "https://user.edgecos.com/UserPrivacy"
    fun title(type: String): String = when (type) {
        Routes.LegalType.AGREEMENT -> "用户服务协议"
        Routes.LegalType.PRIVACY -> "隐私政策"
        else -> "协议"
    }
}

/**
 * 页面转场（快速淡入淡出）。
 *
 * Navigation 库默认转场为 700ms 淡入淡出，观感拖沓；
 * 保持"淡入淡出"形态不变，提速到 enter 220ms / exit 160ms + 强 ease-out 曲线：
 * - enter：强 ease-out 前段快速上屏，迅速到位
 * - exit：稍快于 enter，及时让位，避免页面叠加拖慢感知
 */
private val ShortEnterFade: EnterTransition =
    fadeIn(animationSpec = tween(durationMillis = 220, easing = EdgeEasing.EaseOut))
private val ShortExitFade: ExitTransition =
    fadeOut(animationSpec = tween(durationMillis = 160, easing = EdgeEasing.EaseOut))

/**
 * 根导航：始终以 MainScreen 为起点，未登录时主界面显示空状态并引导登录。
 * 监听 AuthExpired 事件自动登出（仍停留在 MainScreen，由其展示未登录态）。
 * 次级页面：login / register / about / oss / legal / plan / wallet / work_order / announcements / share_management / settings。
 */
@Composable
fun RootNav(
    navController: NavHostController = rememberNavController(),
) {
    val authRepo = ServiceLocator.authRepository
    val user by authRepo.currentUser.collectAsState()
    val context = LocalContext.current

    // 强制更新检查已迁移至 UpdateGuard（由各 Activity 的 onCreate 调用）：
    // 覆盖所有入口（含 OAuth 回链、深链、崩溃兜底页），
    // 弹窗为原生实现，Compose / View 两种 Activity 通用。

    // 监听鉴权失效事件：登出后回到 MainScreen（不强制跳登录页）
    LaunchedEffect(Unit) {
        AuthEventBus.events.collectLatest { error ->
            authRepo.logout()
            navController.popBackStack(Routes.MAIN, inclusive = false)
        }
    }

    // 处理 App Link 深链：MainActivity 接收 ACTION_VIEW Intent 后通过 DeepLinkBus 推送 Uri，
    // 此处收集事件并导航到对应页面（分享链接 → ShareAccessScreen，直链 → 系统浏览器下载）
    LaunchedEffect(Unit) {
        DeepLinkBus.events.collect { uri ->
            val path = uri.path ?: ""
            // 分享链接：/s/{shareId}
            if (path.startsWith("/s/")) {
                val shareId = path.removePrefix("/s/").trimEnd('/')
                if (shareId.isNotEmpty()) {
                    navController.navigate(Routes.shareAccess(shareId))
                }
            }
            // 直链 /f/{linkCode}：转发给系统浏览器下载（302 重定向到实际文件）
            if (path.startsWith("/f/")) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.MAIN,
        enterTransition = { ShortEnterFade },
        exitTransition = { ShortExitFade },
        popEnterTransition = { ShortEnterFade },
        popExitTransition = { ShortExitFade },
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                isLoggedIn = user != null,
                onNavigateToLogin = { navController.navigate(Routes.LOGIN) },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onNavigateAbout = { navController.navigate(Routes.ABOUT) },
                onNavigatePlan = { navController.navigate(Routes.PLAN) },
                onNavigateWallet = { navController.navigate(Routes.WALLET) },
                onNavigateWorkOrder = { navController.navigate(Routes.WORK_ORDER) },
                onNavigateAnnouncements = { navController.navigate(Routes.ANNOUNCEMENTS) },
                onNavigateShareManagement = { navController.navigate(Routes.SHARE_MGMT) },
                onNavigateSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigatePreview = { route -> navController.navigate(route) },
                onNavigateOrders = { navController.navigate(Routes.ORDERS) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.popBackStack(Routes.MAIN, inclusive = false)
                },
                onNavigateToRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.popBackStack(Routes.MAIN, inclusive = false)
                },
                onBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.popBackStack(Routes.LOGIN, inclusive = false)
                },
            )
        }
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onOpenUserAgreement = {
                    navController.navigate(Routes.legal(Routes.LegalType.AGREEMENT))
                },
                onOpenPrivacyPolicy = {
                    navController.navigate(Routes.legal(Routes.LegalType.PRIVACY))
                },
                onOpenOpenSourceAcknowledgements = {
                    navController.navigate(Routes.OSS)
                },
            )
        }
        composable(Routes.OSS) {
            OpenSourceAcknowledgementsScreen(
                onBack = { navController.popBackStack() },
                onOpenLink = { url ->
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
            )
        }
        composable(
            route = Routes.LEGAL,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: Routes.LegalType.AGREEMENT
            LegalDocScreen(
                url = when (type) {
                    Routes.LegalType.AGREEMENT -> LegalUrls.AGREEMENT
                    Routes.LegalType.PRIVACY -> LegalUrls.PRIVACY
                    else -> LegalUrls.AGREEMENT
                },
                title = LegalUrls.title(type),
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.PLAN) {
            PlanScreen(
                onBack = { navController.popBackStack() },
                onNavigateOrders = { navController.navigate(Routes.ORDERS) },
            )
        }
        composable(Routes.WALLET) {
            WalletScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WORK_ORDER) {
            WorkOrderScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ANNOUNCEMENTS) {
            AnnouncementListScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHARE_MGMT) {
            ShareManagementScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ORDERS) {
            OrderListScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.SHARE_ACCESS,
            arguments = listOf(
                navArgument("shareId") { type = NavType.StringType },
            ),
        ) { entry ->
            val shareId = entry.arguments?.getString("shareId").orEmpty()
            ShareAccessScreen(
                shareId = shareId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("filename") { type = NavType.StringType },
                navArgument("basename") { type = NavType.StringType },
                navArgument("size") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType },
            ),
        ) { entry ->
            val file = FileNode(
                id = entry.arguments?.getString("id") ?: "",
                filename = URLDecoder.decode(entry.arguments?.getString("filename") ?: "", "UTF-8"),
                basename = URLDecoder.decode(entry.arguments?.getString("basename") ?: "", "UTF-8"),
                size = (entry.arguments?.getString("size") ?: "0").toLongOrNull() ?: 0L,
                lastmod = "",
                type = if (entry.arguments?.getString("type") == "directory") FileType.DIRECTORY else FileType.FILE,
            )
            // 记录「app 内打开」事件：首页「最近文件」第一优先级数据源
            LaunchedEffect(file.id) {
                withContext(Dispatchers.IO) { RecentlyOpenedStore.record(context, file) }
            }
            FilePreviewScreen(
                file = file,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
