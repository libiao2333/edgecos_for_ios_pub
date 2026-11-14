package com.edgecos.mobileapp.upload

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.domain.error.EdgeResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * 上传前台服务：负责后台执行上传任务，通知栏展示进度。
 *
 * - foregroundServiceType=dataSync（声明在 Manifest）
 * - 通知栏：单文件进度 + 总进度
 * - 实际调度可后续接入 WorkManager（断网恢复续传）
 *
 * 【并发上传守护】多个上传任务共享同一个 Service 实例与协程作用域，
 * 必须等最后一个任务结束后才能 stopSelf——否则先完成的任务会把
 * 仍在传输的任务一起杀死（scope 随 Service 销毁被 cancel）。
 */
class UploadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // 通知渠道只需创建一次（原先在每次通知构建时重复创建）
        ensureChannel()
        logPromotionState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 14+（API 34+）必须显式声明 foregroundServiceType
        // 否则系统抛 MissingForegroundServiceTypeException 并 5 秒内杀进程
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification("准备上传...", 0),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("准备上传...", 0))
        }

        val fileUriStr = intent?.getStringExtra(EXTRA_FILE_URI)
        val filename = intent?.getStringExtra(EXTRA_FILENAME)
        if (fileUriStr == null || filename == null) {
            // 非法启动：仅在没有其他任务在跑时才停服务，避免误杀并行上传
            stopIfIdle()
            return START_NOT_STICKY
        }
        val path = intent.getStringExtra(EXTRA_PATH) ?: "/"
        val fileUri = Uri.parse(fileUriStr)

        // 与 finally 中的递减互斥：保证「先递增」和后完成者的「递减并停服」不会交错出错
        synchronized(countLock) { activeUploads.incrementAndGet() }

        scope.launch {
            var result: Pair<String, String?>? = null
            try {
                val uploader = Uploader(this@UploadService, ServiceLocator.uploadRepository)
                when (val r = uploader.upload(fileUri, filename, path) { progress ->
                    notifyProgress(filename, progress)
                    UploadEventBus.broadcast(UploadEventBus.Event.Progress(filename, progress))
                }) {
                    is EdgeResult.Success -> {
                        UploadEventBus.broadcast(UploadEventBus.Event.Success(path, filename))
                        result = filename to null
                    }
                    is EdgeResult.Failure -> {
                        val message = r.error.message ?: "上传失败"
                        UploadEventBus.broadcast(UploadEventBus.Event.Failed(filename, message))
                        result = filename to message
                    }
                }
            } finally {
                // 当前任务结束：仅当没有其他任务在跑时才停服务
                finishOne(result)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * 记录一个任务结束；若无进行中的任务则收尾并停止服务。
     *
     * @param result 最后一个任务的结果（文件名 + 错误信息，null 表示成功）；
     *   用于把前台进度通知切换为可点击的结果通知，让未开启实时活动的设备
     *   也能从通知栏看到上传结果。
     */
    private fun finishOne(result: Pair<String, String?>? = null) {
        synchronized(countLock) {
            if (activeUploads.decrementAndGet() <= 0) {
                activeUploads.set(0)
                if (result != null) {
                    notifyResult(result.first, result.second)
                }
                // 保留下拉通知（不移除），让用户在通知栏看到最终结果
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    /** 无进行中任务时才停止服务 */
    private fun stopIfIdle() {
        synchronized(countLock) {
            if (activeUploads.get() <= 0) stopSelf()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // IMPORTANCE_DEFAULT：Android 16 的 Live Updates（实时活动/流体云）
        // 只对 DEFAULT 及以上的渠道生效，LOW 会被系统直接忽略提升申请
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "上传", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "文件上传进度（支持实时活动/流体云）"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
                // 锁屏可见：流体云/锁屏岛需要公开展示
                setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            },
        )
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EdgeCOS 上传")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            // 锁屏/流体云可见（本应用不展示敏感内容）
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // 前台服务通知立即展示：默认会延迟约 10 秒，短任务的进度通知会直接错过
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // 显式写入系统提升标志（ColorOS 流体云读取的是该 extra）
            .addExtras(promotionExtras())

        if (progress in 1..99) {
            builder
                // Android 16+：Live Updates 进度样式
                .setStyle(NotificationCompat.ProgressStyle().setProgress(progress))
                // 状态栏芯片短文本
                .setShortCriticalText("$progress%")
                // 申请提升为常驻实时活动（流体云等依赖此标志）
                .setRequestPromotedOngoing(true)
                // 低版本兼容：标准进度条
                .setProgress(100, progress, false)
        } else {
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
     * NotificationCompat 会写同一个键，这里补一次以兼容不同 Compat 版本 / 厂商 ROM。
     */
    private fun promotionExtras(): Bundle = Bundle().apply {
        putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
    }

    /** 打印实时活动相关系统状态，便于排查通知未展示的原因 */
    private fun logPromotionState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
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

    private fun notifyProgress(filename: String, progress: Int) {
        val active = activeUploads.get()
        val text = if (active > 1) "$filename $progress%（共 $active 个任务）" else "$filename $progress%"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress))
    }

    /** 上传结束后的结果通知（普通通知，所有系统版本可见；不再作为实时活动展示） */
    private fun notifyResult(filename: String, error: String?) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val success = error == null
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_upload_done
                else android.R.drawable.stat_notify_error,
            )
            .setContentTitle(if (success) "上传完成" else "上传失败")
            .setContentText(if (success) filename else "$filename：$error")
            .setOngoing(false)
            .setAutoCancel(true)
            .setRequestPromotedOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "UploadService"
        // v2：渠道重要性创建后无法修改，旧渠道（edgecos_upload，IMPORTANCE_LOW）
        // 无法升级为 Live Updates，故换新 id 让系统重新创建 DEFAULT 渠道
        private const val CHANNEL_ID = "edgecos_upload_v2"
        private const val NOTIFICATION_ID = 1001

        /** Android 16 实时活动提升标志（系统读取的 extra key） */
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

        const val EXTRA_FILE_URI = "extra_file_uri"
        const val EXTRA_FILENAME = "extra_filename"
        const val EXTRA_PATH = "extra_path"

        /** 进行中的上传任务数（并发上传守护） */
        private val activeUploads = AtomicInteger(0)
        private val countLock = Any()

        fun start(context: Context, fileUri: Uri, filename: String, path: String) {
            val intent = Intent(context, UploadService::class.java).apply {
                putExtra(EXTRA_FILE_URI, fileUri.toString())
                putExtra(EXTRA_FILENAME, filename)
                putExtra(EXTRA_PATH, path)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
