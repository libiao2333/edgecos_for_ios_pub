package com.edgecos.mobileapp.feature.about

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.edgecos.mobileapp.design.components.EdgeErrorState
import com.edgecos.mobileapp.design.components.IOSNavBar

/**
 * 法律文档展示页：用 WebView 加载 EdgeCOS 网页端协议页。
 *
 * - 不共享主业务 Cookie（协议页是公开路由）
 * - 使用浏览器 UA，避免触发人机验证
 * - 关闭文件/内容访问，外链拦截到系统浏览器
 * - 支持加载进度条 + 错误重试
 *
 * 参考 `协议获取与排版TODO.md`。
 */
@Composable
fun LegalDocScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var loadingProgress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(url) }

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            IOSNavBar(title = title, onBack = onBack)

            Box(modifier = Modifier.fillMaxSize()) {
                if (hasError) {
                    EdgeErrorState(
                        title = "加载失败",
                        message = "请检查网络后重试",
                        onRetry = {
                            hasError = false
                            currentUrl = url
                            webViewRef?.loadUrl(url)
                        },
                    )
                    return@Box
                }

                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            configureForLegalDoc()
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    hasError = false
                                    loadingProgress = 0
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loadingProgress = 100
                                    // 网站 CSS 设了 body{overflow:hidden} 和 .app-layout{height:100vh;overflow:hidden}，
                                    // 导致 WebView 内整页无法滚动。注入覆盖样式恢复 body 滚动。
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            var s = document.createElement('style');
                                            s.id = 'edge-webview-scroll-fix';
                                            if (document.getElementById('edge-webview-scroll-fix')) return;
                                            s.textContent =
                                                'body{overflow:auto !important;height:auto !important;}' +
                                                '.app-layout{height:auto !important;overflow:visible !important;}' +
                                                'html{overflow-x:hidden;overflow-y:auto !important;}';
                                            document.head.appendChild(s);
                                        })();
                                        """.trimIndent(),
                                        null,
                                    )
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?,
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    val target = request?.url ?: return false
                                    if (target.host != "user.edgecos.com") {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, target),
                                            )
                                        }
                                        return true
                                    }
                                    return false
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                }
                            }
                            webViewRef = this
                            loadUrl(currentUrl)
                        }
                    },
                    update = { webView ->
                        if (webView.url != currentUrl) {
                            webView.loadUrl(currentUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                if (loadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { loadingProgress / 100f },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureForLegalDoc() {
    settings.apply {
        javaScriptEnabled = true              // 前端可能依赖 React 水合
        domStorageEnabled = true              // localStorage
        allowFileAccess = false
        allowContentAccess = false
        // 使用浏览器 UA，避免被识别为爬虫
        userAgentString = WebSettings.getDefaultUserAgent(context)
        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
    }
    isVerticalScrollBarEnabled = true
    isHorizontalScrollBarEnabled = false
}
