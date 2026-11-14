package com.edgecos.mobileapp.crash

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 全局未捕获异常处理器。
 *
 * 工作流程：
 * 1. 注册为 [Thread.setDefaultUncaughtExceptionHandler]
 * 2. 捕获到未处理异常时：
 *    a. 通过 [CrashReporter] 把异常信息写入文件
 *    b. 启动 [CrashActivity] 显示安抚用户的错误页面
 *    c. 退出当前进程
 *
 * 设计要点：
 * - 使用 [AtomicBoolean] 防止递归崩溃（在写日志时再次崩溃进入死循环）
 * - 保留原默认 Handler，让系统正常走 ANR / native crash 流程
 * - 启动 CrashActivity 使用 FLAG_NEW_TASK（因为当前 Activity 已经销毁，没有任务栈）
 *
 * 不处理的情况（由系统兜底）：
 * - Native 崩溃（SIGSEGV / SIGABRT）：JVM 已经死亡，Java 层 Handler 不会被调用
 * - ANR：由系统弹"应用无响应"对话框
 * - OOM：在内存极度紧张时，Intent 启动 Activity 可能会失败
 */
class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    private val handling = AtomicBoolean(false)
    private val TAG = "CrashHandler"

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // 防止递归：如果在处理崩溃的过程中又抛了异常，直接交给系统默认处理
        if (!handling.compareAndSet(false, true)) {
            Log.e(TAG, "Recursive crash detected, delegating to default handler")
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)

        // 从异常堆栈推断组件名
        val component = inferComponent(throwable)

        // 写入崩溃日志
        CrashReporter.report(context, thread, throwable, component)

        // 启动崩溃兜底页
        try {
            val intent = Intent(context, CrashActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
                putExtra(CrashActivity.EXTRA_EXCEPTION_CLASS, throwable.javaClass.name)
                putExtra(CrashActivity.EXTRA_EXCEPTION_MESSAGE, throwable.message ?: "")
                putExtra(CrashActivity.EXTRA_COMPONENT, component)
                putExtra(CrashActivity.EXTRA_THREAD_NAME, thread.name)
            }
            context.startActivity(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to launch CrashActivity", t)
        }

        // 让系统默认 Handler 处理（会 kill 进程）
        // 必须交给默认 Handler，否则进程不会真正退出，会卡在半死状态
        defaultHandler?.uncaughtException(thread, throwable)

        // 兜底自杀：如果默认 Handler 没有杀进程（某些 ROM 会重写）
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    /**
     * 从异常堆栈推断发生崩溃的组件名。
     *
     * 规则：扫描 stackTrace elements，识别 Activity / Service / ViewModel 类名。
     */
    private fun inferComponent(throwable: Throwable): String {
        return try {
            val stack = throwable.stackTrace
            for (e in stack) {
                val cn = e.className
                when {
                    cn.contains("Activity") -> return cn.substringAfterLast('.')
                    cn.contains("Service") -> return cn.substringAfterLast('.')
                    cn.contains("ViewModel") -> return cn.substringAfterLast('.')
                    cn.contains("Composable") -> return "Compose:${e.methodName}"
                }
            }
            stack.firstOrNull()?.className?.substringAfterLast('.') ?: "unknown"
        } catch (t: Throwable) {
            "unknown"
        }
    }

    private fun exitProcess(code: Int) {
        kotlin.system.exitProcess(code)
    }

    companion object {
        @Volatile
        private var installed = false

        /**
         * 安装全局崩溃处理器。
         * 幂等，重复调用安全。
         */
        fun install(context: Context) {
            if (installed) return
            synchronized(this) {
                if (installed) return
                val default = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext, default))
                installed = true
                Log.i("CrashHandler", "Global crash handler installed")
            }
        }
    }
}
