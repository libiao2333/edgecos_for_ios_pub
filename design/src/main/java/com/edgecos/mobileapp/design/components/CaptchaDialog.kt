package com.edgecos.mobileapp.design.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONObject

private const val TAG = "CaptchaDialog"

/**
 * 人机验证结果。
 * - [success] 为 true 时，[ticket] 与 [randstr] 有效
 * - [success] 为 false 时，用户取消或验证失败
 */
data class CaptchaResult(
    val success: Boolean,
    val ticket: String? = null,
    val randstr: String? = null,
)

/**
 * 腾讯云 Captcha 弹窗：用 WebView 加载 TCaptcha.js 实现人机验证。
 *
 * 与前端 tencentCaptcha.js 流程一致：
 * 1. WebView 以 https://user.edgecos.com/ 为 baseURL 加载 captcha.html（避免 file:// 协议导致 SDK referer 校验失败）
 * 2. 调用 `initWhenReady(appId)` 初始化腾讯 Captcha SDK
 * 3. SDK 就绪后调 `show()` 弹出验证窗
 * 4. 用户完成验证，JS 回调 `onResult(data)`
 * 5. 解析 `data.ticket` / `data.randstr`，通过 [onResult] 回传给调用方
 *
 * @param appId 腾讯 Captcha AppId（AppId=197733334）
 * @param onResult 验证结果回调（成功或失败/取消）
 * @param onDismiss 用户主动关闭弹窗
 */
@Composable
fun CaptchaDialog(
    appId: String,
    onResult: (CaptchaResult) -> Unit,
    onDismiss: () -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }
    var initCalled by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // JavascriptInterface：接收 JS 回调
    val bridge = remember {
        object {
            @JavascriptInterface
            fun onReady() {
                Log.d(TAG, "JS onReady: SDK 就绪，调用 show()")
                // SDK 就绪后调 show() 弹出验证窗
                webView?.post {
                    webView?.evaluateJavascript("show();", null)
                }
            }

            @JavascriptInterface
            fun onResult(json: String) {
                // 安全：不打印完整 json，其中包含 ticket/randstr 等人机验证票据，泄露后可被重放。
                if (finished) {
                    Log.d(TAG, "onResult 已 finished，忽略")
                    return
                }
                finished = true
                try {
                    val data = JSONObject(json)
                    val ret = data.optInt("ret", -1)
                    val ticket = data.optString("ticket").takeIf { it.isNotEmpty() }
                    val randstr = data.optString("randstr").takeIf { it.isNotEmpty() }
                    // 仅打印前 8 字符，足够调试又不足以重放
                    Log.d(TAG, "解析: ret=$ret, ticket=${ticket?.take(8)}, randstr=${randstr?.take(8)}")
                    if (ret == 0 && ticket != null && randstr != null) {
                        onResult(CaptchaResult(success = true, ticket = ticket, randstr = randstr))
                    } else {
                        onResult(CaptchaResult(success = false))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "onResult 解析异常", e)
                    onResult(CaptchaResult(success = false))
                }
            }

            @JavascriptInterface
            fun onError(message: String) {
                Log.e(TAG, "JS onError: $message")
                if (finished) return
                finished = true
                onResult(CaptchaResult(success = false))
            }
        }
    }

    // 页面加载完成后注入 appId 初始化 SDK（只调一次）
    LaunchedEffect(pageLoaded) {
        if (pageLoaded && !initCalled) {
            initCalled = true
            Log.d(TAG, "页面加载完成，注入 initWhenReady('$appId')")
            webView?.evaluateJavascript("initWhenReady('$appId');", null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "DisposableEffect onDispose: 销毁 WebView")
            webView?.apply {
                stopLoading()
                removeJavascriptInterface("AndroidCaptcha")
                destroy()
            }
            webView = null
        }
    }

    Dialog(
        onDismissRequest = {
            if (!finished) {
                Log.d(TAG, "Dialog onDismissRequest: 用户关闭弹窗")
                finished = true
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    createCaptchaWebView(ctx, bridge) { pageLoaded = true }.also { wv ->
                        webView = wv
                        // 用 loadDataWithBaseURL 加载，baseURL 设为 https://user.edgecos.com/
                        // 避免 file:// 协议导致腾讯 Captcha SDK referer 校验失败
                        val html = ctx.assets.open("captcha.html").bufferedReader().use { it.readText() }
                        wv.loadDataWithBaseURL(
                            "https://user.edgecos.com/",
                            html,
                            "text/html",
                            "utf-8",
                            null,
                        )
                    }
                },
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createCaptchaWebView(
    context: Context,
    bridge: Any,
    onPageLoaded: () -> Unit,
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }
        setBackgroundColor(Color.TRANSPARENT)
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: url=$url")
                onPageLoaded()
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "onReceivedError: ${error?.description} (request=${request?.url})")
            }
        }
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                Log.d(TAG, "JS console: ${consoleMessage?.message()} (${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                return true
            }
        }
        addJavascriptInterface(bridge, "AndroidCaptcha")
        // 禁止长按、选择等无关交互
        setOnLongClickListener { true }
        isLongClickable = false
    }
}
