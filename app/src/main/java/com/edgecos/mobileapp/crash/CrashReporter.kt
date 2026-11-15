package com.edgecos.mobileapp.crash

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 崩溃日志收集与持久化。
 *
 * 职责：
 * 1. 收集设备信息（机型、Android 版本、App 版本、发生时间、发生组件）
 * 2. 把崩溃堆栈写入到 app 私有目录 `files/crash/` 下，文件名 `crash_<时间戳>.txt`
 * 3. 同时写一份 JSON 元数据，便于后续上传服务器分析
 *
 * 设计原则：
 * - 任何 IO / 反射异常自身都不能再抛出，否则会陷入"崩溃 → 写日志 → 再崩溃"的死循环
 * - 不依赖 Room / Retrofit / ServiceLocator，因为这些可能正是崩溃源头
 * - 只用 android.util.Log，因为 BuildConfig.DEBUG 在 release 被 ProGuard 移除
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val CRASH_DIR = "crash"
    private const val MAX_LOG_FILES = 20 // 最多保留 20 份崩溃日志，超出自动清理最旧的

    /** 公共下载目录下的崩溃日志子文件夹：Download/EdgeCOS/log/ */
    private const val PUBLIC_LOG_SUBDIR = "EdgeCOS/log"

    /**
     * 写入崩溃日志。
     *
     * @param context 任意 Context，内部使用 applicationContext 避免内存泄漏
     * @param thread  崩溃发生的线程
     * @param throwable 崩溃异常
     * @param component 发生崩溃的组件名（Activity / Service / ViewModel 等），可由调用方推断后传入
     * @return 生成的日志文件绝对路径，失败返回 null
     */
    fun report(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        component: String = "unknown",
    ): String? {
        return try {
            val app = context.applicationContext
            val timestamp = System.currentTimeMillis()
            val dateStr = formatDate(timestamp)

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val meta = buildMeta(app, thread, throwable, component, dateStr, timestamp)
            val humanReadable = buildHumanReadable(meta, stackTrace)

            val crashDir = File(app.filesDir, CRASH_DIR).apply { mkdirs() }
            trimOldLogs(crashDir)

            val logFile = File(crashDir, "crash_${timestamp}.txt")
            logFile.writeText(humanReadable)

            val metaFile = File(crashDir, "crash_${timestamp}.json")
            metaFile.writeText(meta.toString(2))

            // 额外复制一份到公共下载目录 Download/EdgeCOS/log/，
            // 方便用户用文件管理器查看，与 logcat 核对
            exportToPublicDir(humanReadable, timestamp)

            Log.e(TAG, "Crash log written to ${logFile.absolutePath}")
            logFile.absolutePath
        } catch (t: Throwable) {
            // 兜底中的兜底：写日志失败不能再抛
            Log.e(TAG, "Failed to write crash log", t)
            null
        }
    }

    /**
     * 列出所有崩溃日志文件（按时间倒序），供"崩溃日志查看页"使用。
     */
    fun listCrashFiles(context: Context): List<File> {
        val app = context.applicationContext
        val dir = File(app.filesDir, CRASH_DIR)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.isFile && (f.extension == "txt" || f.extension == "json") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * 读取崩溃日志文件内容。
     */
    fun readCrashFile(context: Context, fileName: String): String? {
        return try {
            val app = context.applicationContext
            val file = File(File(app.filesDir, CRASH_DIR), fileName)
            if (file.exists()) file.readText() else null
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to read crash log", t)
            null
        }
    }

    /**
     * 删除指定崩溃日志文件。
     */
    fun deleteCrashFile(context: Context, fileName: String): Boolean {
        return try {
            val app = context.applicationContext
            val file = File(File(app.filesDir, CRASH_DIR), fileName)
            file.delete()
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * 清空所有崩溃日志。
     */
    fun clearAllCrashFiles(context: Context) {
        try {
            val app = context.applicationContext
            val dir = File(app.filesDir, CRASH_DIR)
            dir.listFiles()?.forEach { it.delete() }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to clear crash logs", t)
        }
    }

    // ==================== 内部辅助 ====================

    /**
     * 把崩溃日志复制到公共下载目录 Download/EdgeCOS/log/。
     *
     * 这样用户可以直接用文件管理器打开查看，与 logcat 输出核对。
     * 失败不影响主流程（崩溃处理中不能因 IO 异常再次崩溃）。
     */
    private fun exportToPublicDir(content: String, timestamp: Long) {
        try {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                PUBLIC_LOG_SUBDIR,
            )
            if (!publicDir.exists()) publicDir.mkdirs()
            val publicFile = File(publicDir, "crash_${timestamp}.txt")
            publicFile.writeText(content)
            Log.i(TAG, "Crash log exported to ${publicFile.absolutePath}")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to export crash log to public dir", t)
        }
    }

    private fun buildMeta(
        context: Context,
        thread: Thread,
        throwable: Throwable,
        component: String,
        dateStr: String,
        timestamp: Long,
    ): JSONObject {
        val pm = context.packageManager
        val pkgInfo = runCatching {
            pm.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        return JSONObject().apply {
            put("timestamp", timestamp)
            put("time", dateStr)
            put("deviceManufacturer", Build.MANUFACTURER ?: "unknown")
            put("deviceModel", Build.MODEL ?: "unknown")
            put("deviceBrand", Build.BRAND ?: "unknown")
            put("androidVersion", Build.VERSION.RELEASE ?: "unknown")
            put("sdkLevel", Build.VERSION.SDK_INT)
            put("appVersionName", pkgInfo?.versionName ?: "unknown")
            put("appVersionCode", pkgInfo?.longVersionCode ?: -1L)
            put("packageName", context.packageName)
            put("threadName", thread.name ?: "unknown")
            put("threadId", thread.id)
            put("component", component)
            put("exceptionClass", throwable.javaClass.name)
            put("exceptionMessage", throwable.message ?: "")
            put("stackTrace", sw.toString())
        }
    }

    private fun buildHumanReadable(meta: JSONObject, stackTrace: String): String {
        return buildString {
            appendLine("================================================")
            appendLine("EdgeCOS Crash Report")
            appendLine("================================================")
            appendLine("时间         : ${meta.optString("time")}")
            appendLine("设备         : ${meta.optString("deviceBrand")} ${meta.optString("deviceManufacturer")} ${meta.optString("deviceModel")}")
            appendLine("Android 版本 : ${meta.optString("androidVersion")} (SDK ${meta.optInt("sdkLevel")})")
            appendLine("App 版本     : ${meta.optString("appVersionName")} (${meta.optLong("appVersionCode")})")
            appendLine("包名         : ${meta.optString("packageName")}")
            appendLine("线程         : ${meta.optString("threadName")} (id=${meta.optLong("threadId")})")
            appendLine("组件         : ${meta.optString("component")}")
            appendLine("异常         : ${meta.optString("exceptionClass")}: ${meta.optString("exceptionMessage")}")
            appendLine("================================================")
            appendLine("Stack Trace:")
            appendLine(stackTrace)
            appendLine("================================================")
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }

    private fun trimOldLogs(dir: File) {
        try {
            val files = dir.listFiles { f -> f.isFile }?.sortedBy { it.lastModified() } ?: return
            // 每次崩溃产生 2 个文件（txt + json），所以按 2 倍裁剪
            val maxFiles = MAX_LOG_FILES * 2
            if (files.size > maxFiles) {
                files.take(files.size - maxFiles).forEach { it.delete() }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to trim old crash logs", t)
        }
    }
}
