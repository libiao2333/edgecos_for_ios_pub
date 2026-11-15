package com.edgecos.mobileapp

import android.app.Application
import com.edgecos.mobileapp.crash.CrashHandler
import com.edgecos.mobileapp.data.ServiceLocator

/**
 * EdgeCOS Application 入口。
 *
 * 启动顺序：
 * 1. 先安装全局崩溃处理器（保证后续初始化若崩溃也能被捕获 + 写日志 + 启动兜底页）
 * 2. 再初始化 ServiceLocator（注入 Context 与 BASE_URL）
 *
 * 这样即便 ServiceLocator 内部因 Room / EncryptedSharedPreferences / 网络配置等抛异常，
 * 也会被 CrashHandler 接住，给用户一个友好的崩溃页而非系统默认的"应用已停止"。
 */
class EdgeCOSApp : Application() {
    override fun onCreate() {
        // 第一步：安装崩溃兜底（必须在 ServiceLocator 之前，否则初始化期崩溃无人接）
        CrashHandler.install(this)

        super.onCreate()

        // 第二步：用 runCatching 包一层，避免初始化失败直接白屏
        // 即便 init 抛异常，App 也能进首页（ albeit 半功能状态），让用户有机会反馈
        runCatching {
            ServiceLocator.init(this, BuildConfig.BASE_URL, BuildConfig.DEBUG)
        }.onFailure { t ->
            android.util.Log.e("EdgeCOSApp", "ServiceLocator init failed", t)
            // 不再 rethrow，让 App 继续启动；后续访问 ServiceLocator 时再处理
        }
    }
}
