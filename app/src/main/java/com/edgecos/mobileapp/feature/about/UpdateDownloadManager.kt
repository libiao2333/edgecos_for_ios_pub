package com.edgecos.mobileapp.feature.about

import android.content.Context
import android.widget.Toast
import com.edgecos.mobileapp.data.remote.UpdateResult
import com.edgecos.mobileapp.feature.transfer.DownloadService
import com.edgecos.mobileapp.feature.transfer.TransferEventBus
import com.edgecos.mobileapp.feature.transfer.TransferType
import com.edgecos.mobileapp.feature.transfer.TransferViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 更新包下载管理器（更新弹窗「去更新」触发）。
 *
 * 实际下载由 [DownloadService] 前台服务执行：
 * - 通知栏展示 Live 进度（Android 16 流体云），下载期间不可被系统回收；
 * - 全程广播 [TransferEventBus] 事件：任务出现在「传输」列表并持久化；
 * - 下载完成后服务自动拉起系统安装程序（覆盖模式下载，避免旧包续传损坏）。
 *
 * 用进程级协程作用域监听任务结束，弹窗关闭 / 页面退出后下载继续进行。
 */
object UpdateDownloadManager {

    /** 进程级作用域：任务状态与界面生命周期解耦 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** 进行中的更新包任务 id */
    @Volatile
    private var currentTaskId: String? = null

    /** 是否已有更新包在下载（防重入） */
    val inProgress: Boolean get() = currentTaskId != null

    init {
        // 监听任务结束：清空防重入标记，允许用户重新发起（例如下载失败后重试）
        scope.launch {
            TransferEventBus.events.collect { event ->
                val taskId = currentTaskId ?: return@collect
                val finished = when (event) {
                    is TransferEventBus.Event.Completed ->
                        event.id == taskId && event.type == TransferType.DOWNLOAD
                    // Failed 事件不带任务 id；更新包是唯一下载 apk 的场景，按后缀匹配
                    is TransferEventBus.Event.Failed -> event.filename.endsWith(".apk")
                    else -> false
                }
                if (finished) currentTaskId = null
            }
        }
    }

    /**
     * 开始下载更新包（非阻塞，立即返回）。
     *
     * @param context 任意 Context（内部取 applicationContext，避免持有页面引用）
     * @param update 更新检查结果（版本名 + 下载地址）
     */
    fun start(context: Context, update: UpdateResult.UpdateAvailable) {
        if (inProgress) {
            Toast.makeText(context, "更新包正在下载中，请稍候", Toast.LENGTH_SHORT).show()
            return
        }
        if (update.downloadUrl.isBlank()) {
            Toast.makeText(context, "更新包地址无效，请稍后重试", Toast.LENGTH_SHORT).show()
            return
        }

        val appContext = context.applicationContext
        val filename = "EdgeCOS-v${update.remoteVersion}.apk"
        val taskId = TransferViewModel.newId()
        currentTaskId = taskId

        DownloadService.start(
            context = appContext,
            url = update.downloadUrl,
            filename = filename,
            taskId = taskId,
            autoInstall = true,
        )
        Toast.makeText(appContext, "更新包开始下载，完成后将自动打开安装程序", Toast.LENGTH_SHORT).show()
    }
}
