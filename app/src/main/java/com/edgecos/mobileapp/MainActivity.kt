package com.edgecos.mobileapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.CookieManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.auth.DeepLinkBus
import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import com.edgecos.mobileapp.data.theme.ThemeMode
import com.edgecos.mobileapp.design.theme.EdgeCOSTheme
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 切换回正常主题（启动页主题只在冷启动窗口背景上显示）
        setTheme(R.style.Theme_EdgeCOS)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 强制更新守卫：每个 Activity 入口都要检测，防止深链/次级入口绕过更新
        UpdateGuard.check(this)

        requestManageExternalStorage()
        requestLegacyStoragePermissionIfNeeded()

        // 处理 OAuth 回调 deep link
        handleOAuthCallback(intent)

        // 处理分享/直链 deep link（/s/{shareId}、/f/{linkCode}）
        handleDeepLink(intent)

        val themeStore = ServiceLocator.themeStore

        setContent {
            val mode by themeStore.mode.collectAsState()

            val darkTheme = when (mode) {
                ThemeMode.SYSTEM, ThemeMode.MONET -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val dynamicColor = mode == ThemeMode.MONET

            EdgeCOSTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
            ) {
                // Surface 兜底铺主题背景色：保证所有页面（包括未用 Scaffold 的页面）
                // 都跟随主题切换背景，不再透出 Activity 窗口的白底
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    RootNav()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
        handleDeepLink(intent)
    }

    /**
     * 处理 OAuth 回调 deep link。
     *
     * OAuth 流程完成后，服务器会重定向到 edgecos://oauth/callback
     * 或 https://user.edgecos.com/oauth/callback。
     * 
     * 此时 token cookie 已由 CustomTabs/WebView 写入
     * 系统的 CookieManager，我们需要提取它并存入
     * EncryptedCookieJar 中，然后恢复登录态。
     */
    private fun handleOAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        val path = uri.path ?: return

        if (path.startsWith("/oauth/callback")) {
            // 用 lifecycleScope 绑定 Activity 生命周期，避免 Activity 销毁后协程继续运行
            // 用 SupervisorJob 确保子协程异常不会逃逸到全局异常处理器
            lifecycleScope.launch {
                runCatching {
                    // 从系统 CookieManager 提取 token cookie
                    val cookies = CookieManager.getInstance()
                        .getCookie("https://user.edgecos.com")
                        ?: ""

                    // 提取 token cookie 值
                    val tokenCookie = cookies.split("; ")
                        .firstOrNull { it.startsWith("token=") }
                        ?.removePrefix("token=")
                        ?: ""

                    if (tokenCookie.isNotEmpty()) {
                        // 将 token 注入到 EncryptedCookieJar 中
                        val cookieJar = EdgeHttpClient.cookieJar
                        try {
                            cookieJar.putCookie(
                                "user.edgecos.com",
                                okhttp3.Cookie.Builder()
                                    .name("token")
                                    .value(tokenCookie)
                                    .domain("user.edgecos.com")
                                    .path("/")
                                    .httpOnly()
                                    .build(),
                            )
                        } catch (_: Exception) { }

                        // 恢复用户信息
                        ServiceLocator.authRepository.restoreUser()
                    }
                }.onFailure { t ->
                    android.util.Log.e("MainActivity", "OAuth callback handling failed", t)
                }
            }
        }
    }

    /**
     * Android 11+ 请求"管理所有文件"权限。
     *
     * 网盘应用需要将文件下载到公共 Downloads/EdgeCOS/ 目录，
     * Android 11+ 引入分区存储后，写入公共目录需要 MANAGE_EXTERNAL_STORAGE。
     *
     * 这不是普通运行时权限——必须通过系统设置页面让用户手动开启。
     * Android 10 不需要此权限（requestLegacyExternalStorage 已处理）。
     */
    private fun requestManageExternalStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (Environment.isExternalStorageManager()) return
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    /**
     * Android 6.0–10（API 23–29）写公共下载目录（Download/EdgeCOS/）需要
     * WRITE_EXTERNAL_STORAGE 运行时权限；Android 11+ 改用 MANAGE_EXTERNAL_STORAGE
     * （见 [requestManageExternalStorage]）。仅在对应系统版本上申请，
     * 否则这些设备上的“下载”会因 EACCES 直接失败。
     */
    private fun requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT !in Build.VERSION_CODES.M..Build.VERSION_CODES.Q) return
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_CODE_LEGACY_STORAGE,
        )
    }

    /**
     * 处理分享/直链 deep link。
     *
     * App Links 已在 AndroidManifest 中配置 autoVerify="true"：
     * - https://user.edgecos.com/s/{shareId} → 分享访问页
     * - https://user.edgecos.com/f/{linkCode} → 直链下载（转发浏览器）
     *
     * 此方法将 Uri 推送到 [DeepLinkBus]，由 RootNav 收集并导航。
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        val path = uri.path ?: return
        if (path.startsWith("/s/") || path.startsWith("/f/")) {
            lifecycleScope.launch {
                runCatching { DeepLinkBus.emit(uri) }
                    .onFailure { android.util.Log.e("MainActivity", "DeepLink emit failed", it) }
            }
        }
    }

    private companion object {
        const val REQUEST_CODE_LEGACY_STORAGE = 1001
    }
}
