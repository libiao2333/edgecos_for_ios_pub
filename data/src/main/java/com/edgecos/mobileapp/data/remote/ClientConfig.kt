package com.edgecos.mobileapp.data.remote

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * 客户端配置：持有应用版本信息与 BASE_URL。
 * 由 App 启动时调用 [ClientConfig.init] 注入。
 */
object ClientConfig {
    lateinit var baseUrl: String
        private set

    /** Web 站点基地址（由 [baseUrl] 派生，如 https://user.edgecos.com），供 WebView 预览页使用 */
    lateinit var webBaseUrl: String
        private set
    var versionName: String = "1.0"
        private set
    var versionCode: Long = 1
        private set
    var userAgent: String = "EdgeCOS-Android/1.0"
        private set

    fun init(context: Context, baseUrl: String) {
        this.baseUrl = baseUrl
        // "https://user.edgecos.com/api/" → "https://user.edgecos.com"
        this.webBaseUrl = baseUrl.trimEnd('/').removeSuffix("/api")
        try {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = pi.versionName ?: "1.0"
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pi.longVersionCode else pi.versionCode.toLong()
        } catch (_: PackageManager.NameNotFoundException) {
            // 保持默认值
        }
        userAgent = "EdgeCOS-Android/$versionName (${Build.MODEL}; Android ${Build.VERSION.SDK_INT})"
    }
}
