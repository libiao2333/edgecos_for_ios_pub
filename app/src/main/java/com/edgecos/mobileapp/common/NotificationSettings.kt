package com.edgecos.mobileapp.common

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * 通知权限与实时活动（Live Updates）状态查询 / 跳转工具。
 *
 * 【为什么需要】
 * - 上传/下载进度通过通知实时展示，Android 13+ 需要运行时权限
 *   `POST_NOTIFICATIONS`，未授予时通知不展示；
 * - Android 16（API 36）的 Live Updates（ColorOS 16 等机型的"流体云"）
 *   没有独立的运行时权限，只能由用户在系统通知设置中开启/关闭。
 *   App 的职责是如实展示状态并在用户点击时引导跳转，把开关权交给用户。
 */
object NotificationSettings {

    /** API 36 = Android 16，Live Updates 起始版本 */
    private const val API_LIVE_UPDATES = 36

    /**
     * Android 16 新增的「实时活动」专用设置页。
     * 与欢太工具箱等应用一致：直接定位到本应用的实时活动/流体云开关，
     * 比进入普通通知设置页再找开关的路径更短。
     */
    private const val ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS =
        "android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS"

    /** 通知总开关是否开启（含 Android 13+ 的 POST_NOTIFICATIONS 运行时权限） */
    fun areNotificationsEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * 是否允许发布"实时活动"（Live Updates / 流体云）。
     * 低版本系统不支持该能力，统一返回 false。
     */
    fun canPostPromotedNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < API_LIVE_UPDATES) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return manager.canPostPromotedNotifications()
    }

    /**
     * 跳转本应用的「实时活动」系统设置页。
     *
     * Android 16+ 使用专用 Action（可直接看到实时活动开关）；
     * 低版本或跳转失败时退回普通通知设置页。
     */
    fun openPromotionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < API_LIVE_UPDATES) {
            openNotificationSettings(context)
            return
        }
        val intent = Intent(ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openNotificationSettings(context)
        }
    }

    /**
     * 跳转本应用的系统通知设置页。
     * 逐级兜底：通知设置 → 应用详情 → 系统设置首页。
     */
    fun openNotificationSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            },
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            }
        }
    }
}
