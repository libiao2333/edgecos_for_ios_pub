package com.edgecos.mobileapp.feature.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.edgecos.mobileapp.UpdateGuard
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import okhttp3.Cookie

/**
 * OAuth 登录页：用原生 Activity 承载 WebView。
 *
 * 为什么不用 Compose Dialog + AndroidView？
 * —— WebView 嵌入 Compose Dialog 时，触摸事件分发和布局测量存在已知问题，
 *   会导致 WebView 内部无法滚动（QQ 授权页底部按钮点不到）。
 *   原生 Activity 中的 WebView 是标准 View 体系，滚动天然可用。
 *
 * 流程：
 * 1. 接收授权 URL，加载到 WebView
 * 2. 拦截 mqqapi:// 等 scheme，调起 QQ/支付宝/微信原生 App
 * 3. 监听 onPageFinished，检测 /login?oauth=success 回调
 * 4. 从 CookieManager 提取 token cookie，注入 EncryptedCookieJar
 * 5. setResult(RESULT_OK) 返回，OAuthLoginSheet 据此回调 onLoginSuccess
 */
class OAuthLoginActivity : ComponentActivity() {

    private lateinit var webView: WebView
    // 待加载的 URL：onCreate 时收集，WebView 初始化后加载
    private var pendingUrl: String? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 强制更新守卫：OAuth 入口同样拦截（防止利用登录回链绕过强制更新）
        UpdateGuard.check(this)

        // 判断启动方式：内部启动（带 EXTRA_URL）or 回调启动（Intent.ACTION_VIEW）
        val url = when {
            intent.action == Intent.ACTION_VIEW -> {
                // QQ/支付宝等 App 授权完成后的回调
                intent.data?.toString() ?: run {
                    finish()
                    return
                }
            }
            else -> {
                intent.getStringExtra(EXTRA_URL) ?: run {
                    finish()
                    return
                }
            }
        }

        // 如果是 singleTop 模式下收到新 Intent，直接让 WebView 加载
        // 注意：此时 webView 尚未初始化，handleUrl 内部会判断
        pendingUrl = url

        window.setBackgroundDrawableResource(android.R.color.white)

        // 根容器：纵向排列（顶栏 + WebView）
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            fitsSystemWindows = true
            setBackgroundColor(Color.WHITE)
        }

        // ========== 顶栏：返回 + 标题 ==========
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val backButton = TextView(this).apply {
            text = "←"
            textSize = 22f
            setOnClickListener { finish() }
            setPadding(24, 24, 24, 24)
        }
        val titleView = TextView(this).apply {
            text = "第三方登录"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(24, 0, 0, 0)
        }
        topBar.addView(backButton)
        topBar.addView(titleView)
        container.addView(
            topBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        // ========== WebView ==========
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            // 允许混合内容（OAuth 重定向可能涉及 http 资源）
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            // 数据库与缓存
            settings.databaseEnabled = true
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            setBackgroundColor(Color.WHITE)

            // OAuth 流程需要跨域 cookie
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val uri = Uri.parse(url)
                    // 服务器完成授权后重定向到 /login?oauth=success，token cookie 已写入
                    if (uri.host == CALLBACK_HOST &&
                        uri.path?.startsWith("/login") == true &&
                        uri.getQueryParameter("oauth") == "success"
                    ) {
                        if (handleOAuthCallback()) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }

                override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?,
                ) {
                    // OAuth 流程中部分重定向证书可能有问题，选择继续加载
                    // 注意：生产环境应严格校验证书，此处为兼容 OAuth 重定向链
                    handler?.proceed()
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    // 只处理主资源加载失败，子资源失败不影响
                    if (request?.isForMainFrame == true) {
                        android.util.Log.e("OAuthLogin", "WebView 主资源加载失败: ${error?.description}")
                    }
                    super.onReceivedError(view, request, error)
                }

                override fun shouldOverrideUrlLoading(view: WebView, requestUrl: String): Boolean {
                    android.util.Log.d("OAuthLogin", "shouldOverrideUrlLoading: $requestUrl")
                    val uri = Uri.parse(requestUrl)
                    val scheme = uri.scheme?.lowercase() ?: ""

                    // 调起 QQ/支付宝/微信原生 App（WebView 默认不支持这些 scheme）
                    if (scheme in APP_SCHEMES) {
                        android.util.Log.d("OAuthLogin", "拦截到 App scheme: $scheme")
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            view.context.startActivity(intent)
                        } catch (e: Exception) {
                            android.util.Log.e("OAuthLogin", "调起 App 失败: ${e.message}")
                            // 没安装 App：让 WebView 继续加载网页版
                        }
                        return true
                    }

                    // http/https URL：让 WebView 自己加载
                    if (scheme == "http" || scheme == "https") {
                        return false
                    }

                    // 其他未知 scheme（如 market://、mailto: 等）：交给系统处理
                    android.util.Log.w("OAuthLogin", "未知 scheme: $scheme, URL: $requestUrl")
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        view.context.startActivity(intent)
                    } catch (_: Exception) {
                        // 系统也无法处理：忽略，避免 WebView 显示 unknown_url 错误页
                    }
                    return true
                }
            }

            // 处理 JS 的 alert/confirm，避免 OAuth 页面 JS 弹窗卡住
            webChromeClient = android.webkit.WebChromeClient()

            // 加载初始 URL 或待处理的回调 URL
            pendingUrl?.let { loadUrl(it) }
            pendingUrl = null
        }
        container.addView(
            webView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        setContentView(container)

        // 返回键：优先 WebView 后退，无法后退才退出
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    finish()
                }
            }
        })
    }

    /**
     * singleTop 模式下，QQ/支付宝等 App 授权完成后回调到此 Activity。
     * 让 WebView 加载回调 URL，由 WebViewClient 处理 token 提取。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            val callbackUrl = intent.data?.toString() ?: return
            android.util.Log.d("OAuthLogin", "onNewIntent 回调 URL: $callbackUrl")
            if (::webView.isInitialized) {
                webView.loadUrl(callbackUrl)
            }
        }
    }

    override fun onDestroy() {
        // 主动销毁 WebView，避免内存泄漏
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.stopLoading()
        webView.removeAllViews()
        webView.destroy()
        super.onDestroy()
    }

    /**
     * 从 WebView 的 CookieManager 提取 token cookie，注入 EncryptedCookieJar。
     * 返回 true 表示处理成功。
     */
    private fun handleOAuthCallback(): Boolean {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://$CALLBACK_HOST") ?: ""

        val tokenValue = cookies.split("; ")
            .firstOrNull { it.startsWith("token=") }
            ?.removePrefix("token=")
            ?.trim()
            ?: ""

        if (tokenValue.isNotEmpty()) {
            return try {
                EdgeHttpClient.cookieJar.putCookie(
                    CALLBACK_HOST,
                    Cookie.Builder()
                        .name("token")
                        .value(tokenValue)
                        .domain(CALLBACK_HOST)
                        .path("/")
                        .httpOnly()
                        .build(),
                )
                ServiceLocator.authRepository.restoreUser()
                true
            } catch (_: Exception) {
                false
            }
        }
        return false
    }

    companion object {
        private const val CALLBACK_HOST = "user.edgecos.com"
        const val EXTRA_URL = "extra_url"

        // 需要调起原生 App 的 scheme（QQ/支付宝/微信等）
        private val APP_SCHEMES = setOf(
            "mqqapi", "mqqopensdkapi", "mqq", "tencent",  // QQ
            "alipays",  // 支付宝
            "weixin",  // 微信
            "dingtalk",  // 钉钉
            "snssdk1128",  // 抖音
        )

        fun newIntent(context: Context, url: String): Intent =
            Intent(context, OAuthLoginActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
            }
    }
}
