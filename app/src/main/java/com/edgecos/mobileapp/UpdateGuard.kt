package com.edgecos.mobileapp

import androidx.activity.ComponentActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.lifecycle.lifecycleScope
import com.edgecos.mobileapp.common.ComposeDialogHost
import com.edgecos.mobileapp.data.remote.UpdateChecker
import com.edgecos.mobileapp.data.remote.UpdateResult
import com.edgecos.mobileapp.design.components.EdgeAlertDialog
import com.edgecos.mobileapp.feature.about.UpdateDownloadManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 全局更新守卫。
 *
 * 【为什么存在】
 * 更新必须"全程可见"：如果只在主界面（RootNav）检测，用户可以借助
 * 非首页入口（OAuth 回调页、分享链接、崩溃兜底页等）绕过更新弹窗。
 * 因此【每个 Activity】的 onCreate 都必须调用 [check]。
 *
 * 【工作方式】
 * - 进程内只发起一次网络检查（GET 生产环境版本接口），结果缓存在本对象；
 * - 一旦确定"远程 versionCode > 本地 VERSION_CODE"：本进程内每次新
 *   Activity 启动都会弹出更新弹窗；
 * - 弹窗与 App 内其他弹窗完全统一（[EdgeAlertDialog]），提供「去更新」与
 *   「取消」两个按钮：「去更新」直接下载安装包（出现在传输列表，通知栏
 *   Live 进度，完成后自动拉起系统安装程序）；「取消」关闭弹窗，本次运行
 *   内不再主动提示，下次冷启动会再次提示；
 * - 用 [ComposeDialogHost] 承载 Compose 弹窗：[com.edgecos.mobileapp.feature.auth.OAuthLoginActivity]
 *   是纯 View 体系（无 Compose 环境），宿主保证所有 Activity 类型表现一致。
 *
 * 【放行条件】
 * - 已是最新（远程 versionCode <= 本地）：放行；
 * - 检查失败（网络错误等）：放行不阻塞，避免弱网锁死 App。
 */
object UpdateGuard {

    /** 需要强制更新时的版本信息；null 表示尚未确定需要更新 */
    @Volatile
    private var updateRequired: UpdateResult.UpdateAvailable? = null

    /** 是否已完成过一次有效检查。检查失败/协程被取消不算完成，后续 Activity 会重新检查 */
    @Volatile
    private var checkCompleted = false

    /** 用户已在本进程内点过「取消」：不再重复打扰，待下次冷启动再提示 */
    @Volatile
    private var dismissedInSession = false

    /** 进行中的检查任务（避免多个 Activity 并发发起重复请求） */
    private var checkJob: Job? = null

    /** 当前弹窗宿主（避免切换 Activity 时弹窗叠加） */
    private var dialogHost: ComposeDialogHost? = null

    /**
     * 检测更新。应在【每个 Activity】的 onCreate 中调用。
     *
     * @param activity 宿主 Activity（弹窗挂在其窗口上）
     */
    fun check(activity: ComponentActivity) {
        if (dismissedInSession) return
        // 已确定需要更新：直接在当前 Activity 弹出（覆盖所有入口）
        updateRequired?.let { result ->
            showDialog(activity, result)
            return
        }
        // 已完成检查且无需更新：放行
        if (checkCompleted) return
        // 已有检查在途：由发起它的协程负责出结果
        if (checkJob?.isActive == true) return

        checkJob = activity.lifecycleScope.launch {
            val result = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
            checkCompleted = true
            if (result is UpdateResult.UpdateAvailable) {
                updateRequired = result
                if (!activity.isFinishing && !activity.isDestroyed) {
                    showDialog(activity, result)
                }
            }
            // UpToDate / CheckFailed：不阻塞使用
        }
    }

    /** 弹出更新弹窗：「去更新」开始下载，「取消」关闭并停止本次运行内的提示 */
    private fun showDialog(
        activity: ComponentActivity,
        result: UpdateResult.UpdateAvailable,
        markDismissed: Boolean = true,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val host = ComposeDialogHost(activity)
        dialogHost = host
        host.show(
            cancelable = true,
            onDismiss = { if (dialogHost === host) dialogHost = null },
        ) { dismiss ->
            EdgeAlertDialog(
                onDismissRequest = {
                    if (markDismissed) dismissedInSession = true
                    dismiss()
                },
                title = "发现新版本",
                message = result.description.ifBlank { "请更新到最新版本后继续使用。" },
                icon = Icons.Outlined.SystemUpdate,
                confirmText = "去更新",
                onConfirm = {
                    UpdateDownloadManager.start(activity, result)
                    dismiss()
                },
                dismissText = "取消",
                onDismiss = {
                    if (markDismissed) dismissedInSession = true
                    dismiss()
                },
            )
        }
    }
}
