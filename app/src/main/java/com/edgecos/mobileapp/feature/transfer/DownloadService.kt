package com.edgecos.mobileapp.feature.transfer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.FileProvider
import com.edgecos.mobileapp.MainActivity
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.upload.Downloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 下载前台服务：承载文件下载任务，并在通知栏展示进度。
 *
 * 【Android 16 Live Updates（流体云）】
 * 采用 Google 标准的 Live Updates 通知：
 * - `NotificationCompat.ProgressStyle`：进度以系统原生样式呈现（API 36+）；
 * - `requestPromotedOngoing(true)`：申请提升为"常驻实时活动"，支持的机型
 *   （如 ColorOS 16 的流体云）会把下载进度浮动到状态栏/息屏/锁屏；
 * - `setShortCriticalText`：状态栏芯片上的极简文本（百分比）。
 * 在 API < 36 的设备上以上能力自动降级为普通进度通知，无需分支判断。
 *
 * 【通知渠道】必须使用 IMPORTANCE_DEFAULT 及以上，系统才会评估
 * Live Updates 提升申请（IMPORTANCE_LOW 会被忽略）。
 *
 * 【并发守护】多个下载共享同一个 Service 与协程作用域，必须等最后一个
 * 任务结束后才能 stopSelf，否则先完成的任务会把仍在传的任务一起杀掉。
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 进行中的任务：taskId -> 展示信息（仅用于汇总通知文案） */
    private val tasks = ConcurrentHashMap<String, String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须先进入前台，否则 Android 12+ 会抛 ForegroundServiceDidNotStartInTimeException
        startForegroundSafely(buildProgressNotification("准备下载…", 0))
        logPromotionState()

        val url = intent?.getStringExtra(EXTRA_URL)
        val filename = intent?.getStringExtra(EXTRA_FILENAME)
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (url == null || filename == null || taskId == null) {
            return START_NOT_STICKY
        }
        val autoInstall = intent.getBooleanExtra(EXTRA_AUTO_INSTALL, false)

        tasks[taskId] = filename
        activeDownloads.incrementAndGet()

        TransferEventBus.broadcast(
            TransferEventBus.Event.Started(taskId, filename, TransferType.DOWNLOAD),
        )

        scope.launch {
            val result = Downloader(this@DownloadService).download(
                downloadUrl = url,
                filename = filename,
                overwrite = autoInstall,
            ) { percent ->
                notifyProgress(filename, percent)
                TransferEventBus.broadcast(
                    TransferEventBus.Event.Progress(taskId, filename, percent, TransferType.DOWNLOAD),
                )
            }

            when (result) {
                is EdgeResult.Success -> {
                    TransferEventBus.broadcast(
                        TransferEventBus.Event.Completed(
                            taskId, filename, TransferType.DOWNLOAD, result.data.absolutePath,
                        ),
                    )
                    finishTask(taskId, filename, result.data, null, autoInstall)
                }
                is EdgeResult.Failure -> {
                    val message = result.error.message ?: "下载失败"
                    TransferEventBus.broadcast(
                        TransferEventBus.Event.Failed(taskId, filename, message),
                    )
                    finishTask(taskId, filename, null, message, autoInstall)
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // 任务生命周期
    // ------------------------------------------------------------------

    /** 单个任务结束：还有任务则更新汇总通知，没有则收尾并停服 */
    private fun finishTask(
        taskId: String,
        filename: String,
        file: File?,
        error: String?,
        autoInstall: Boolean,
    ) {
        tasks.remove(taskId)
        val remaining = activeDownloads.decrementAndGet()

        if (remaining > 0) {
            // 仍有任务在跑：通知交还给进行中的任务
            val next = tasks.values.firstOrNull() ?: "继续下载…"
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIFICATION_ID, buildProgressNotification("$next 下载中…", 0))
            return
        }

        activeDownloads.set(0)
        tasks.clear()

        // 最后一个任务收尾：把通知改为可点击的结果态并脱离前台（保留下拉可见）
        val manager = getSystemService(NotificationManager::class.java)
        if (manager != null) {
            val finished = when {
                error != null -> buildResultNotification(
                    title = "下载失败",
                    text = "$filename：$error",
                    file = null,
                    success = false,
                )
                file != null -> buildResultNotification(
                    title = if (autoInstall) "更新包已下载" else "下载完成",
                    text = filename,
                    file = file,
                    success = true,
                )
                else -> null
            }
            if (finished != null) manager.notify(NOTIFICATION_ID, finished)
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()

        // 更新包：下载完成后直接拉起系统安装程序
        if (autoInstall && file != null) openInstaller(file)
    }

    // ------------------------------------------------------------------
    // 通知
    // ------------------------------------------------------------------

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "下载",
            // Live Updates 必须是 DEFAULT 及以上，LOW 会被系统忽略提升申请
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "文件下载进度（支持实时活动/流体云）"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            // 锁屏可见：流体云/锁屏岛需要公开展示
            setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC)
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * 构建进行中的下载通知。
     *
     * [NotificationCompat.ProgressStyle] + [NotificationCompat.Builder.setRequestPromotedOngoing]
     * 是 Android 16 Live Updates 的标准写法；低版本由 setProgress 兜底。
     */
    private fun buildProgressNotification(text: String, progress: Int): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (tasks.size > 1) "正在下载（${tasks.size} 个任务）" else "正在下载")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            // 锁屏/流体云可见（本应用不展示敏感内容）
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // 前台服务通知立即展示：默认会延迟约 10 秒，短任务的进度通知会直接错过
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // 显式写入系统提升标志（ColorOS 流体云读取的是该 extra）
            .addExtras(promotionExtras())

        if (progress in 1..99) {
            builder
                // Android 16+：Live Updates 进度样式
                .setStyle(NotificationCompat.ProgressStyle().setProgress(progress))
                // 状态栏芯片短文本（Android 16+）
                .setShortCriticalText("$progress%")
                // 申请提升为常驻实时活动（Android 16+，流体云等依赖此标志）
                .setRequestPromotedOngoing(true)
                // 低版本兼容：标准进度条
                .setProgress(100, progress, false)
        } else {
            // 0%：不确定进度；100%：满进度（不能标成不确定，否则会一直转圈）
            builder
                .setProgress(100, progress, progress == 0)
                // 与进行中保持一致：Android 16+ 始终申请实时活动
                .setShortCriticalText(if (progress >= 100) "100%" else "0%")
                .setRequestPromotedOngoing(true)
        }
        return builder.build()
    }

    /**
     * 显式写入系统的「申请提升为常驻实时活动」标志。
     *
     * NotificationCompat 的 setRequestPromotedOngoing 会写同一个键，
     * 这里再补一次，避免不同 Compat 版本 / 厂商 ROM 读取路径差异导致不上流体云。
     */
    private fun promotionExtras(): Bundle = Bundle().apply {
        putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
    }

    /** 打印实时活动相关系统状态，便于在 ColorOS 等机型上排查通知未展示的原因 */
    private fun logPromotionState() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val importance = manager.getNotificationChannel(CHANNEL_ID)?.importance
        val promoted = if (Build.VERSION.SDK_INT >= 36) {
            manager.canPostPromotedNotifications()
        } else {
            false
        }
        Log.i(
            TAG,
            "channel=$CHANNEL_ID importance=$importance enabled=${manager.areNotificationsEnabled()} " +
                "promoted=$promoted sdk=${Build.VERSION.SDK_INT}",
        )
    }

    /** 下载结束后的结果通知：可点击打开文件，不再作为实时活动展示 */
    private fun buildResultNotification(
        title: String,
        text: String,
        file: File?,
        success: Boolean,
    ): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error,
            )
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setRequestPromotedOngoing(false)

        builder.setContentIntent(
            if (file != null) openFileIntent(file) ?: openAppIntent() else openAppIntent(),
        )
        return builder.build()
    }

    private fun notifyProgress(filename: String, progress: Int) {
        val active = activeDownloads.get()
        val text = if (active > 1) "$filename $progress%（共 $active 个任务）" else "$filename $progress%"
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildProgressNotification(text, progress))
    }

    private fun startForegroundSafely(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ------------------------------------------------------------------
    // 意图
    // ------------------------------------------------------------------

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openFileIntent(file: File): PendingIntent? = try {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        PendingIntent.getActivity(
            this,
            file.absolutePath.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    } catch (_: Exception) {
        null
    }

    /** 拉起系统安装程序（更新包场景） */
    private fun openInstaller(file: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            // 后台启动安装页被系统拦截时忽略，用户可从通知或传输列表再次打开
        }
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "edgecos_download"
        private const val NOTIFICATION_ID = 1002

        /** Android 16 实时活动提升标志（系统读取的 extra key） */
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_FILENAME = "extra_filename"
        private const val EXTRA_TASK_ID = "extra_task_id"
        private const val EXTRA_AUTO_INSTALL = "extra_auto_install"

        /** 进行中的下载任务数（并发守护） */
        private val activeDownloads = AtomicInteger(0)

        /**
         * 启动下载。
         *
         * @param url 下载地址（后端 /download，会 302 到 CDN）
         * @param filename 落盘文件名
         * @param taskId 传输列表中的任务 id（与 [TransferEventBus] 事件对应）
         * @param autoInstall 是否为更新包（覆盖模式下载，完成后自动拉起安装器）
         */
        fun start(
            context: Context,
            url: String,
            filename: String,
            taskId: String,
            autoInstall: Boolean = false,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_AUTO_INSTALL, autoInstall)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
