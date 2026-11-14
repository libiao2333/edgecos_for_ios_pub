package com.edgecos.mobileapp.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.CastConnected
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.edgecos.mobileapp.data.ServiceLocator
import com.edgecos.mobileapp.data.remote.ClientConfig
import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import com.edgecos.mobileapp.design.components.IOSImmersivePreviewHeader
import com.edgecos.mobileapp.design.components.IOSPageIndicator
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.error.EdgeError
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.util.formatBytes
import com.edgecos.mobileapp.media.cast.CastDevice
import com.edgecos.mobileapp.media.cast.CastDeviceSheet
import com.edgecos.mobileapp.media.cast.DlnaController
import com.seapeak.docviewer.DocViewerFragment
import com.seapeak.docviewer.config.DocConfig
import com.seapeak.docviewer.config.DocType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// ============================================================================
// 文件类型枚举（用于预览路由分发）
// ============================================================================

/** 可预览的文件分类 */
enum class PreviewType {
    IMAGE, VIDEO, AUDIO, TEXT, DOCUMENT, OTHER
}

/** 根据文件名后缀判断预览类型 */
fun classifyPreview(filename: String): PreviewType {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        // 图片
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif" -> PreviewType.IMAGE
        // 视频
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "ts", "flv" -> PreviewType.VIDEO
        // 音频
        "mp3", "wav", "ogg", "flac", "aac", "m4a", "opus", "wma", "mid", "midi" -> PreviewType.AUDIO
        // 文本
        "txt", "json", "xml", "csv", "log", "yml", "yaml", "ini", "cfg",
        "conf", "properties", "sh", "bat", "py", "js", "ts", "kt", "java", "css",
        "html", "htm", "sql", "env", "gitignore", "gradle", "toml" -> PreviewType.TEXT
        // 办公文档 / PDF / markdown（与网页端 extOf 分组一致，markdownExts 含 mdown/mkd/mkdn）
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "pps", "ppsx",
        "pdf", "md", "markdown", "mdown", "mkd", "mkdn" -> PreviewType.DOCUMENT
        else -> PreviewType.OTHER
    }
}

/**
 * 根据文件名后缀获取 MIME 类型，用于系统意图打开。
 */
fun getMimeType(filename: String): String {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
}

/** 办公文档扩展名（与网页端 officeExts 一致） */
private val OFFICE_EXTS = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "pps", "ppsx")

/** Office 预览 WebView 诊断日志 TAG（adb logcat -s OfficePreview 可过滤） */
private const val OFFICE_WEBVIEW_TAG = "OfficePreview"

/** Office 预览加载浮层状态（避免白屏无提示的观感） */
private enum class OfficeOverlayState { LOADING, HIDDEN, FAILED }

/**
 * 微软 Office viewer 静态资源 CDN 容灾镜像映射。
 *
 * viewer 脚本/样式默认托管在 [OFFICE_CDN_HOST]（Cloudflare），该域名在部分网络下 TLS 证书
 * 校验失败（SSL_UNTRUSTED），导致 appresourceloader.min.js 等核心脚本加载不到 → 页面白屏。
 * 同路径资源在 [OFFICE_CDN_MIRROR_HOST]（Akamai）上可用，故 WebView 拦截后改从镜像拉取。
 */
private const val OFFICE_CDN_HOST = "res.public.onecdn.static.microsoft"
private const val OFFICE_CDN_MIRROR_HOST = "res-h1.cdn.office.net"

/** 镜像资源拉取专用客户端（精简配置：无应用鉴权拦截器，纯静态资源 GET） */
private val officeMirrorClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
}

/** 镜像响应的 Content-Type 缺失时按扩展名兜底（WebView 对 JS/CSS 的 MIME 校验严格） */
private fun guessResourceMime(path: String): String = when {
    path.endsWith(".js") -> "application/javascript"
    path.endsWith(".css") -> "text/css"
    path.endsWith(".json") -> "application/json"
    path.endsWith(".ico") -> "image/x-icon"
    path.endsWith(".png") -> "image/png"
    path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
    path.endsWith(".gif") -> "image/gif"
    path.endsWith(".svg") -> "image/svg+xml"
    path.endsWith(".woff2") -> "font/woff2"
    path.endsWith(".woff") -> "font/woff"
    path.endsWith(".ttf") -> "font/ttf"
    path.endsWith(".eot") -> "application/vnd.ms-fontobject"
    else -> "application/octet-stream"
}

/**
 * 构建下载 URL（下载行为，需携带 Cookie 鉴权；line 与网页端一致取 cdn/free）。
 *
 * 网页端 Drive.jsx：`buildUrl('/download', file, line)` → line=cdn（计入流量）
 * 或 line=free（不计流量，单连接限速）。
 */
fun buildDownloadUrl(file: FileNode, line: TransferLine): String {
    val encodedPath = URLEncoder.encode(file.filename, "UTF-8")
    return "${ClientConfig.baseUrl}download?path=$encodedPath&id=${file.id}&line=${line.requestParam}"
}

/**
 * fileParams 查询串 —— 与网页端 lib/format.js 的 fileParams(file) 完全一致：
 * 有 id 传 id，否则传 path（filename）。
 */
private fun fileParamsQuery(file: FileNode): String = if (file.id.isNotBlank()) {
    "id=${URLEncoder.encode(file.id, "UTF-8")}"
} else {
    "path=${URLEncoder.encode(file.filename, "UTF-8")}"
}

/**
 * 构建预览 URL —— 与网页端 Drive.jsx `buildUrl('/preview', file, line)` 一致：
 * fileParams + lineParams（line 由用户选择的线路决定：cdn/free）。
 * 服务端 302 重定向到 CDN，由 OkHttp 自动跟随。
 * 适用于：图片 / 视频 / 音频（以及下载后本地渲染的 markdown）。
 */
fun buildPreviewUrl(file: FileNode, line: TransferLine): String =
    "${ClientConfig.baseUrl}preview?${fileParamsQuery(file)}&line=${line.requestParam}"

/**
 * 构建文本内容 URL —— 与网页端 `api.get('/file/content', { params: fileParams })` 一致（无 line 参数）。
 * 响应：`{ "content": "..." }`
 */
private fun buildContentUrl(file: FileNode): String =
    "${ClientConfig.baseUrl}file/content?${fileParamsQuery(file)}"

/**
 * 构建 PDF 预览地址 API —— 与网页端 `api.get('/preview/pdf-url', { params })` 一致。
 * 响应：`{ "url": "https://cdn.../file.pdf?sign=..." }`（签名 CDN 地址，无需 Cookie）。
 */
private fun buildPdfUrlApi(file: FileNode, line: TransferLine): String =
    "${ClientConfig.baseUrl}preview/pdf-url?${fileParamsQuery(file)}&line=${line.requestParam}"

/**
 * 从 Compose 上下文中递归找出 FragmentActivity。
 *
 * LocalContext 返回的可能是 Activity、ContextWrapper 或其他包装类。
 * 对于 Fragment 管理（supportFragmentManager），必须持有 FragmentActivity。
 */
private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ============================================================================
// 预览主页面
// ============================================================================

/**
 * 文件预览页：根据文件类型自动选择预览方式。
 *
 * 线路选择（与网页端 Drive.jsx chooseLine 完全一致）：
 * - 线路偏好为「每次询问」（默认）→ 先弹线路选择框（可勾选记住）
 * - 已存偏好（cdn / free）→ 直接使用，不再询问（可在「设置」中修改）
 *
 * 所有预览接口调用与网页端完全一致（照抄前端 API）：
 * - 图片/视频/音频：GET /preview?id=...&line=cdn|free（302 → CDN），Coil/ExoPlayer 原生渲染
 * - 文本：GET /file/content?id=...（响应 {content}，文本接口不涉及下载线路）
 * - PDF：GET /preview/pdf-url?line=... → 签名 CDN 地址 → 本地原生渲染
 * - Office：POST /generate-direct-link → 微软在线预览（与网页端相同，需实名认证）
 * - Markdown：GET /file/content 获取文本后写入缓存，本地渲染（与网页端一致）
 * - 其他：先下载到本地缓存（/download?line=...），再用系统应用打开
 */
@UnstableApi
@Composable
fun FilePreviewScreen(
    file: FileNode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val type = remember(file) { classifyPreview(file.basename) }

    // 预览线路（与网页端一致）：偏好为 ASK 时为 null → 弹窗询问；否则直接使用偏好
    var chosenLine by remember(file.id) {
        mutableStateOf(
            LinePreferenceManager.previewLine(context).takeIf { it != TransferLine.ASK },
        )
    }

    val line = chosenLine
    if (line == null) {
        LineChoiceDialog(
            title = "选择预览线路",
            onChoose = { choice, remember ->
                if (remember) LinePreferenceManager.setPreviewLine(context, choice)
                chosenLine = choice
            },
            onDismiss = onBack,
        )
        return
    }

    val previewUrl = remember(file, line) { buildPreviewUrl(file, line) }

    when (type) {
        // 图片：Coil 加载 /preview（302 → CDN，Cookie 由 CoilLoader/EdgeHttpClient 携带）
        PreviewType.IMAGE -> ImagePreview(previewUrl, file, onBack)
        // 视频：ExoPlayer 流式播放（携带 Cookie），支持 DLNA 投屏
        PreviewType.VIDEO -> VideoPreview(previewUrl, file, onBack)
        // 音频：ExoPlayer 流式播放
        PreviewType.AUDIO -> AudioPreview(previewUrl, file, onBack)
        // 文本：GET /file/content 读取内容
        PreviewType.TEXT -> TextPreview(buildContentUrl(file), file, onBack)
        // 文档：PDF / Office / Markdown 分流
        PreviewType.DOCUMENT -> DocumentPreview(file, line, onBack)
        PreviewType.OTHER -> OtherPreview(file, line, onBack)
    }
}

// ============================================================================
// 图片预览（Coil 加载 /preview）
// ============================================================================

/**
 * 图片预览：Coil 加载 /preview URL（302 → CDN）。
 *
 * URL 与网页端 `<img src="/api/preview?..." />` 完全一致；
 * 图片请求由 [CoilLoader] 提供的 OkHttp（挂载 EncryptedCookieJar）发出，自动携带登录态。
 */
@Composable
private fun ImagePreview(
    url: String,
    file: FileNode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val imageLoader = remember { CoilLoader.get(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 图片内容：等比缩放填满屏幕宽度，居中显示（延伸至状态栏下方，沉浸式）
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = file.basename,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth,
        )

        // 浮动沉浸式顶部栏（图片不滚动，标题常驻显示）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = true,
        )
    }
}

// ============================================================================
// 音频预览（ExoPlayer 流式播放 /preview）
// ============================================================================

/**
 * 音频预览：ExoPlayer 流式播放 /preview URL（携带 Cookie），无画面。
 */
@UnstableApi
@Composable
private fun AudioPreview(
    url: String,
    file: FileNode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 播放器：共用构建函数（携带 Cookie 数据源，否则流请求 401）
    val player = remember(url) { buildCookiePlayer(context, url) }

    // 播放状态（驱动按钮文案；Player 回调触发重组）
    var isPlaying by remember { mutableStateOf(true) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 生命周期：切后台自动暂停，回前台恢复
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player.pause()
                Lifecycle.Event.ON_RESUME -> player.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 音频内容（铺满屏幕，标题栏浮在上层）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = EdgeSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.AudioFile,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                file.basename,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = EdgeSpacing.lg),
            )
            Text(
                formatBytes(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = EdgeSpacing.xs),
            )
            Button(
                onClick = {
                    if (player.isPlaying) player.pause() else player.play()
                },
                modifier = Modifier.padding(top = EdgeSpacing.xl),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.padding(start = EdgeSpacing.sm),
                )
            }
        }

        // 浮动沉浸式顶部栏（标题常驻，因音频页不可滚动）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = true,
        )
    }
}

// ============================================================================
// 视频预览（ExoPlayer + OkHttp 携带 Cookie）
// ============================================================================

/**
 * 构建携带 Cookie 的 ExoPlayer（视频/音频预览共用）。
 *
 * 关键：使用 EdgeHttpClient（已挂载 EncryptedCookieJar）作为数据源，
 * 否则媒体流请求不会带 Cookie，会被服务器 401 拒绝。
 */
@UnstableApi
private fun buildCookiePlayer(context: Context, url: String): ExoPlayer {
    val dataSourceFactory = OkHttpDataSource.Factory(EdgeHttpClient.get())
    val mediaSourceFactory = DefaultMediaSourceFactory(context)
        .setDataSourceFactory(dataSourceFactory)
    return ExoPlayer.Builder(context)
        .setMediaSourceFactory(mediaSourceFactory)
        .build()
        .apply {
            setMediaItem(MediaItem.Builder().setUri(url).build())
            prepare()
            playWhenReady = true
        }
}

@UnstableApi
@Composable
private fun VideoPreview(
    url: String,
    file: FileNode,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 视频页纯黑背景：强制状态栏图标为浅色，保证在黑底上可见（覆盖主题默认）
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val prev = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose {
            controller?.isAppearanceLightStatusBars = prev ?: true
        }
    }

    // 播放器：共用构建函数（携带 Cookie 数据源，否则流请求 401）
    val player = remember(url) { buildCookiePlayer(context, url) }

    // 播放状态与进度
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    // iOS 风格：控制栏可见性（点击切换，3 秒自动隐藏）
    var controlsVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    // 自动隐藏任务：新交互取消旧任务，避免旧计时器错杀控制栏（竞态修复）
    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    // 是否因切后台被自动暂停（回前台恢复用户暂停前的播放状态）
    var autoPaused by remember { mutableStateOf(false) }

    // 投屏状态
    var castDevice by remember { mutableStateOf<CastDevice?>(null) }
    var showCastSheet by remember { mutableStateOf(false) }
    var castPlaying by remember { mutableStateOf(true) }

    // 显示控制栏并重新计时自动隐藏
    fun showControlsTemporarily() {
        controlsVisible = true
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(3000)
            val playing = if (castDevice != null) castPlaying else isPlaying
            if (playing) controlsVisible = false
        }
    }

    // 监听 Player 状态
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                // 播放时 3 秒后自动隐藏控制栏，暂停时显示
                if (playing) showControlsTemporarily() else controlsVisible = true
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                currentPositionMs = player.currentPosition
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // 进度轮询：本地播放 500ms；投屏时 1000ms 从 TV 上报（SOAP GetPositionInfo）
    LaunchedEffect(player, castDevice) {
        val device = castDevice
        if (device == null) {
            while (true) {
                delay(500)
                if (durationMs == 0L) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
            }
        } else {
            while (true) {
                delay(1000)
                DlnaController.getPosition(device.controlUrl)?.let { pos ->
                    currentPositionMs = pos.positionMs
                    if (pos.durationMs > 0) durationMs = pos.durationMs
                }
            }
        }
    }

    // 生命周期：切后台自动暂停（记忆播放意图，回前台恢复）；
    // onDispose 释放播放器；若正在投屏则异步停止 TV 播放，避免留下无法控制的孤儿播放
    DisposableEffect(player, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    autoPaused = player.playWhenReady && castDevice == null
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (autoPaused && castDevice == null) player.play()
                    autoPaused = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            autoHideJob?.cancel()
            player.release()
            castDevice?.let { DlnaController.stopAsync(it.controlUrl) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // 点击切换控制栏（iOS AVPlayerViewController 行为）
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (controlsVisible) {
                            controlsVisible = false
                        } else {
                            showControlsTemporarily()
                        }
                    },
                )
            },
    ) {
        // 视频内容：等比缩放填满屏幕
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // iOS 风格控制栏（淡入淡出；投屏中时进度来自 TV，播放控制改为 SOAP）
        VideoControlsOverlay(
            visible = controlsVisible,
            isPlaying = if (castDevice != null) castPlaying else isPlaying,
            castingDeviceName = castDevice?.name,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            file = file,
            onBack = onBack,
            onCastClick = {
                // 打开投屏弹窗期间不自动隐藏控制栏
                autoHideJob?.cancel()
                controlsVisible = true
                showCastSheet = true
            },
            onStopCast = {
                val device = castDevice
                if (device != null) {
                    scope.launch { DlnaController.stop(device.controlUrl) }
                    castDevice = null
                    castPlaying = true
                    // 本地从 TV 当前进度接着播
                    runCatching { player.seekTo(currentPositionMs) }
                    player.play()
                }
                showControlsTemporarily()
            },
            onPlayPause = {
                val device = castDevice
                if (device != null) {
                    val target = !castPlaying
                    castPlaying = target  // 乐观更新，失败回滚
                    scope.launch {
                        val ok = if (target) {
                            DlnaController.play(device.controlUrl)
                        } else {
                            DlnaController.pause(device.controlUrl)
                        }
                        if (!ok) castPlaying = !target
                    }
                } else {
                    if (isPlaying) player.pause() else player.play()
                }
                showControlsTemporarily()
            },
            onSeekBack = {
                val device = castDevice
                val target = (currentPositionMs - 15_000).coerceAtLeast(0L)
                if (device != null) {
                    currentPositionMs = target
                    scope.launch { DlnaController.seek(device.controlUrl, target) }
                } else {
                    player.seekBack()
                }
                showControlsTemporarily()
            },
            onSeekForward = {
                val device = castDevice
                val target = if (durationMs > 0) {
                    (currentPositionMs + 15_000).coerceAtMost(durationMs)
                } else {
                    currentPositionMs + 15_000
                }
                if (device != null) {
                    currentPositionMs = target
                    scope.launch { DlnaController.seek(device.controlUrl, target) }
                } else {
                    player.seekForward()
                }
                showControlsTemporarily()
            },
            onSeekTo = { posMs ->
                val device = castDevice
                if (device != null) {
                    currentPositionMs = posMs
                    scope.launch { DlnaController.seek(device.controlUrl, posMs) }
                } else {
                    player.seekTo(posMs)
                }
                showControlsTemporarily()
            },
        )

        // 投屏设备选择弹窗
        if (showCastSheet) {
            CastDeviceSheet(
                file = file,
                onDismiss = {
                    showCastSheet = false
                    showControlsTemporarily()
                },
                onConnected = { device ->
                    showCastSheet = false
                    castDevice = device
                    castPlaying = true
                    player.pause()  // 本地暂停，避免手机与电视双重播放
                    showControlsTemporarily()
                },
            )
        }
    }
}

/**
 * iOS AVPlayerViewController 风格控制栏覆盖层。
 *
 * - 顶部：返回按钮 + 文件名 + 投屏按钮（半透明黑色背景）
 * - 投屏中：顶部栏下方显示投屏状态条（点击停止）
 * - 中央：播放/暂停大按钮（半透明黑色圆形，48dp）
 * - 底部：15秒后退 | 进度条 | 15秒前进 + 时间显示
 * - 控制栏整体淡入淡出
 */
@Composable
private fun VideoControlsOverlay(
    visible: Boolean,
    isPlaying: Boolean,
    castingDeviceName: String?,
    currentPositionMs: Long,
    durationMs: Long,
    file: FileNode,
    onBack: () -> Unit,
    onCastClick: () -> Unit,
    onStopCast: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
) {
    // 整体淡入淡出（提速：StiffnessMediumLow → StiffnessMedium，显隐更干脆）
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "controlsAlpha",
    )

    if (alpha < 0.01f) return

    Box(modifier = Modifier.fillMaxSize().alpha(alpha)) {
        // 顶部栏：返回按钮 + 文件名（半透明黑色渐变）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.6f),
                        1f to Color.Transparent,
                    ),
                )
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 返回按钮（半透明白色圆形）
                val backInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = backInteraction,
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                // 文件名
                Text(
                    text = file.basename,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = EdgeSpacing.md),
                )
                // 右侧投屏按钮（与返回按钮对称；投屏中变品牌色高亮）
                val castInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (castingDeviceName != null) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            } else {
                                Color.Black.copy(alpha = 0.4f)
                            },
                        )
                        .clickable(
                            interactionSource = castInteraction,
                            indication = null,
                            onClick = onCastClick,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (castingDeviceName != null) {
                            Icons.Outlined.CastConnected
                        } else {
                            Icons.Outlined.Cast
                        },
                        contentDescription = if (castingDeviceName != null) "投屏中" else "投屏",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // 投屏状态条：正在投屏到 XX（胶囊，点击停止）
        if (castingDeviceName != null) {
            val stopCastInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = stopCastInteraction,
                        indication = null,
                        onClick = onStopCast,
                    )
                    .padding(horizontal = EdgeSpacing.md, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CastConnected,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "正在投屏到 $castingDeviceName",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 200.dp),
                )
                Spacer(Modifier.width(EdgeSpacing.sm))
                Text(
                    text = "停止",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // 中央播放/暂停大按钮
        val centerInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = centerInteraction,
                    indication = null,
                    onClick = onPlayPause,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }

        // 底部控制栏：进度条 + 15秒跳转按钮 + 时间
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f),
                    ),
                )
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.md),
        ) {
            // 进度条 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTime(currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(modifier = Modifier.width(EdgeSpacing.sm))
                // 自定义进度条（细线，拖动时显示拇指）
                VideoSeekBar(
                    progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f,
                    onSeek = { fraction -> onSeekTo((fraction * durationMs).toLong()) },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(EdgeSpacing.sm))
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            // 15秒后退 | 播放/暂停 | 15秒前进
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = EdgeSpacing.sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 15秒后退（iOS 风格圆弧箭头 + "15"）
                val backInteraction = remember { MutableInteractionSource() }
                IOSSkipButton(
                    direction = SkipDirection.Backward,
                    onClick = onSeekBack,
                    interactionSource = backInteraction,
                )
                Spacer(modifier = Modifier.width(EdgeSpacing.xxl))
                // 15秒前进
                val forwardInteraction = remember { MutableInteractionSource() }
                IOSSkipButton(
                    direction = SkipDirection.Forward,
                    onClick = onSeekForward,
                    interactionSource = forwardInteraction,
                )
            }
        }
    }
}

/** iOS 风格 15 秒跳转按钮（Canvas 绘制圆弧箭头 + "15" 标签）。 */
@Composable
private fun IOSSkipButton(
    direction: SkipDirection,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val strokePx = 2.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2 - strokePx

            // 顶部留间隙（12 点钟 = 270° Compose 角度）
            val gapHalfAngle = 20f
            val arcStart = 270f + gapHalfAngle
            val arcSweep = 360f - gapHalfAngle * 2

            // 绘制圆弧（间隙在顶部）
            drawArc(
                color = Color.White,
                startAngle = arcStart,
                sweepAngle = arcSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )

            // 箭头位置与方向
            val (arrowAngle, arrowDirX) = when (direction) {
                SkipDirection.Backward -> Pair(270f - gapHalfAngle, -1f)  // 间隙左侧，指向左
                SkipDirection.Forward -> Pair(270f + gapHalfAngle, 1f)   // 间隙右侧，指向右
            }
            val arrowRad = arrowAngle * PI.toFloat() / 180f
            val arrowX = center.x + radius * cos(arrowRad)
            val arrowY = center.y + radius * sin(arrowRad)

            // 绘制箭头三角形（尖端在圆弧端点，底座朝向圆心方向）
            val arrowLen = 6.dp.toPx()
            val arrowHalfW = 4.dp.toPx()
            val baseX = arrowX - arrowDirX * arrowLen
            val baseY = arrowY
            val path = Path().apply {
                moveTo(arrowX, arrowY)
                lineTo(baseX, baseY + arrowHalfW)
                lineTo(baseX, baseY - arrowHalfW)
                close()
            }
            drawPath(path, color = Color.White)
        }

        // 中央 "15" 文字
        Text(
            text = "15",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** 跳转方向。 */
private enum class SkipDirection { Backward, Forward }

/**
 * 视频进度条（iOS 风格细线，拖动时显示拇指）。
 */
@Composable
private fun VideoSeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 拖动状态
    var isDragging by remember { mutableStateOf(false) }
    // 拖动时的临时进度（松手才 onSeek）
    var dragProgress by remember { mutableStateOf(progress) }
    val effectiveProgress = if (isDragging) dragProgress else progress

    // 拇指大小动画（拖动时放大；提速：StiffnessMediumLow → StiffnessMedium，跟手更灵）
    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 8.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "thumbSize",
    )

    var boxWidthPx by remember { mutableStateOf(0) }
    Box(
        modifier = modifier
            .height(30.dp)
            .onSizeChanged { boxWidthPx = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { _ ->
                        isDragging = true
                        dragProgress = progress
                    },
                    onDragEnd = {
                        onSeek(dragProgress)
                        isDragging = false
                    },
                    onDragCancel = { isDragging = false },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val width = size.width.toFloat()
                        if (width > 0) {
                            dragProgress = (dragProgress + dragAmount / width)
                                .coerceIn(0f, 1f)
                        }
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // 轨道
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.3f)),
        )
        // 已播放部分
        Box(
            modifier = Modifier
                .fillMaxWidth(effectiveProgress)
                .height(3.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        // 拇指
        Box(
            modifier = Modifier
                .offset(x = with(LocalDensity.current) { (boxWidthPx.toFloat() * effectiveProgress).toDp() - thumbSize / 2 })
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** 格式化时间为 mm:ss 或 h:mm:ss */
private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

// ============================================================================
// 文本预览（GET /file/content，与网页端一致）
// ============================================================================

/**
 * 文本预览：GET /file/content 读取文件内容（响应 {content}）。
 *
 * 与网页端文本预览流程完全一致（Drive.jsx openPreview → api.get('/file/content')）。
 * 展示限制 500KB，避免超大文本卡顿。
 */
@Composable
private fun TextPreview(
    url: String,
    file: FileNode,
    onBack: () -> Unit,
) {
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    // 视口高度：用于按屏幕高度分页估算页数（onSizeChanged 在布局完成时回调）
    var viewportHeight by remember { mutableStateOf(0) }

    LaunchedEffect(url) {
        try {
            val text = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                EdgeHttpClient.get().newCall(request).execute().use { response ->
                    // 必须检查状态码，否则 401 错误页面 HTML 会被当作内容解析
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    JSONObject(response.body?.string().orEmpty()).optString("content")
                }
            }
            // 与网页端一致：二进制/异常编码内容（含 \u0000）不支持文本预览
            if (text.contains('\u0000')) {
                error = "该文件类型不支持文本预览"
            } else {
                // 限制展示最大 500KB，避免大文件卡顿
                textContent = if (text.length > 500_000) {
                    text.take(500_000) + "\n\n...（文件过大，仅显示前 500KB）"
                } else text
            }
        } catch (e: Exception) {
            error = e.message ?: "未知错误"
        }
        isLoading = false
    }

    // 标题可见性：仅在文本滚动到顶部（scrollTop == 0）时显示，否则一律隐藏
    val titleVisible = scrollState.value == 0

    // 页数估算：
    // - scrollState.maxValue = 内容高度 - 视口高度
    // - 总页数 = ceil((maxValue + viewportHeight) / viewportHeight)
    // - 当前页 = floor(滚动位置 / 视口高度) + 1
    val totalPages by remember {
        derivedStateOf {
            val vh = viewportHeight
            if (vh <= 0) 0
            else {
                val contentHeight = scrollState.maxValue + vh
                (contentHeight + vh - 1) / vh
            }
        }
    }
    val currentPage by remember {
        derivedStateOf {
            val vh = viewportHeight
            if (vh <= 0 || totalPages <= 0) 0
            else (scrollState.value / vh + 1).coerceIn(1, totalPages)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(0.5f),
                    )
                    Text(
                        "加载中...",
                        modifier = Modifier.padding(top = EdgeSpacing.md),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "加载失败: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(EdgeSpacing.lg),
                )
            }
        } else {
            // 文本内容（铺满屏幕，返回按钮浮在上层）
            Text(
                text = textContent ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportHeight = it.height }
                    .verticalScroll(scrollState)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(EdgeSpacing.lg),
            )
        }

        // 浮动沉浸式顶部栏（不显示标题，仅保留返回按钮和页码指示器）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = false,
            trailingContent = {
                IOSPageIndicator(
                    currentPage = currentPage,
                    totalPages = totalPages,
                )
            },
        )
    }
}

// ============================================================================
// 文档预览分流（PDF / Office / Markdown —— API 均与网页端一致）
// ============================================================================

/** PDF 在线预览大小上限：50MB（与网页端拦截阈值一致） */
private const val PDF_SIZE_LIMIT = 50L * 1024 * 1024

/** Markdown 等文本类预览大小上限：5MB（与网页端「文件超过 5MB，不支持在线编辑」一致） */
private const val TEXT_SIZE_LIMIT = 5L * 1024 * 1024

/**
 * 文档预览分流（line 为用户选择的线路 cdn/free，与网页端一致）：
 * - PDF → GET /preview/pdf-url?line=... 获取签名地址后下载，本地渲染（对应网页端 pdf.js）
 * - Office（doc/docx/xls/xlsx/ppt/pptx/pps/ppsx）→ POST /generate-direct-link
 *   后加载微软在线预览（与网页端 view.officeapps.live.com 方案完全一致，直链无需线路参数）
 * - Markdown 等 → GET /file/content 获取文本后写入缓存，本地渲染（与网页端一致）
 */
@Composable
private fun DocumentPreview(
    file: FileNode,
    line: TransferLine,
    onBack: () -> Unit,
) {
    val ext = remember(file) { file.basename.substringAfterLast('.', "").lowercase() }
    when {
        ext == "pdf" -> DocPreview(
            file = file,
            onBack = onBack,
            fetcher = { context ->
                // 大小拦截（与网页端一致：PDF 超过 50MB 不支持在线预览）
                if (file.size > PDF_SIZE_LIMIT) {
                    null to "PDF 超过 50MB，不支持在线预览"
                } else {
                    fetchPdfToCache(context, file, line)
                }
            },
        )
        ext in OFFICE_EXTS -> OfficePreview(file, onBack)
        else -> DocPreview(
            file = file,
            onBack = onBack,
            // Markdown 等：与网页端一致 → GET /file/content 获取文本 → 写入缓存 → 本地渲染
            fetcher = { context ->
                // 网页端对文本类文件的大小拦截：「文件超过 5MB，不支持在线编辑」
                if (file.size > TEXT_SIZE_LIMIT) {
                    null to "文件超过 5MB，不支持在线预览"
                } else {
                    fetchTextToCache(context, file)
                }
            },
        )
    }
}

/**
 * PDF：GET /preview/pdf-url 拿签名 CDN 地址（与网页端一致），下载到缓存。
 * 返回 Pair(缓存文件, 错误信息)。
 */
private suspend fun fetchPdfToCache(context: Context, file: FileNode, line: TransferLine): Pair<File?, String?> {
    // 缓存命中
    PreviewCacheManager.getCachedFile(context, file)?.let { return it to null }

    return withContext(Dispatchers.IO) {
        try {
            // 1) 获取签名地址（与网页端 api.get('/preview/pdf-url') 一致）
            val request = Request.Builder().url(buildPdfUrlApi(file, line)).build()
            val signedUrl = EdgeHttpClient.get().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null to "获取预览地址失败（HTTP ${response.code}）"
                }
                JSONObject(response.body?.string().orEmpty()).optString("url")
            }
            if (signedUrl.isBlank()) return@withContext null to "获取预览地址失败"

            // 2) 下载签名地址（CDN 直链，无需 Cookie）
            val cached = PreviewCacheManager.downloadAndCache(context, file, signedUrl)
            cached to if (cached == null) "文件下载失败，请检查网络" else null
        } catch (e: Exception) {
            null to (e.message ?: "未知错误")
        }
    }
}

/**
 * Markdown 等文本类文档：GET /file/content 获取文本内容（与网页端 openPreview 一致），
 * 写入本地缓存文件后交给 DocViewer 渲染。
 * 返回 Pair(缓存文件, 错误信息)。
 */
private suspend fun fetchTextToCache(context: Context, file: FileNode): Pair<File?, String?> {
    // 缓存命中
    PreviewCacheManager.getCachedFile(context, file)?.let { return it to null }

    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(buildContentUrl(file)).build()
            val content = EdgeHttpClient.get().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext null to when (response.code) {
                        415 -> "该文件类型暂不支持在线预览"
                        else -> "文件读取失败（HTTP ${response.code}）"
                    }
                }
                JSONObject(response.body?.string().orEmpty()).optString("content")
            }
            // 与网页端一致：包含 \u0000 说明不是文本类文件
            if (content.contains('\u0000')) return@withContext null to "该文件类型不支持文本预览"
            val target = PreviewCacheManager.writeTextToCache(context, file, content)
            target to if (target == null) "文件缓存失败" else null
        } catch (e: Exception) {
            null to (e.message ?: "未知错误")
        }
    }
}

// ============================================================================
// 文档本地渲染（PDF / Markdown）：下载缓存 → Seapeak DocViewer 原生渲染
// ============================================================================

/**
 * 通用文档预览：通过 [fetcher] 获取本地缓存文件后，用 Seapeak DocViewer 本地渲染。
 *
 * - PDF：fetcher 走 /preview/pdf-url 签名地址下载（对应网页端 pdf.js 渲染）
 * - Markdown 等：fetcher 走 /file/content 获取文本后写入缓存（对应网页端 markdown 渲染）
 *
 * 页数/翻页/缩放由 jit-viewer 提供：getPageInfo() / prevPage() / nextPage() / setZoom()。
 *
 * @param fetcher 获取缓存文件的挂起函数，返回 Pair(缓存文件, 错误信息)：
 *                文件为 null 时展示错误信息。
 */
@Composable
private fun DocPreview(
    file: FileNode,
    onBack: () -> Unit,
    fetcher: suspend (Context) -> Pair<File?, String?>,
) {
    val context = LocalContext.current
    var cacheFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // 文档类型（决定是否有分页概念）
    val docType = remember(file) {
        val ext = file.basename.substringAfterLast('.', "").lowercase()
        DocType.fromExtension(ext)
    }
    // 是否有分页概念：PDF/Word/Excel/PPT 有明确分页；Markdown 走滚动估算（viewer.html 已兜底）
    val hasPageCount = docType in setOf(
        DocType.PDF, DocType.WORD, DocType.EXCEL, DocType.PPT, DocType.MARKDOWN,
    )

    // 页数状态（0 表示尚未获取，IOSPageIndicator 在总页数 > 0 时才显示）
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(0) }
    val fragmentRef = remember { mutableStateOf<DocViewerFragment?>(null) }

    // 获取缓存文件（各类型走各自的 API，见 DocumentPreview 分流）
    LaunchedEffect(file) {
        val (cached, err) = fetcher(context)
        if (cached != null) {
            cacheFile = cached
        } else {
            error = err ?: "文件下载失败，请检查网络或登录状态"
        }
    }

    // 轮询页数：jit-viewer 的 getPageInfo() 统一支持所有文档类型（页数准确）
    LaunchedEffect(fragmentRef.value, docType) {
        val fragment = fragmentRef.value ?: return@LaunchedEffect
        // 等待 WebView 渲染就绪（Fragment 事务提交后 View 创建稍晚）
        var ready = false
        var waitCount = 0
        while (!ready && waitCount < 25) {
            ready = fragment.view?.findWebView() != null
            if (!ready) {
                delay(200)
                waitCount++
            }
        }
        if (!ready) return@LaunchedEffect

        while (true) {
            delay(500)
            val currentWv = fragment.view?.findWebView() ?: break
            currentWv.evaluateJavascript(JIT_PAGE_QUERY_JS) { result ->
                parsePageInfo(result)?.let { (cur, total) ->
                    currentPage = cur
                    totalPages = total
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 文档渲染区域（铺满屏幕，返回按钮浮在上层）
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "预览失败: $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(EdgeSpacing.lg),
                        )
                    }
                }
                cacheFile == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.5f),
                            )
                            Text(
                                "正在打开文档...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = EdgeSpacing.sm),
                            )
                        }
                    }
                }
                else -> {
                    val docConfig = cacheFile?.let { DocConfig.fromFile(it.absolutePath) }
                    if (docConfig != null) {
                        EmbeddedDocViewer(
                            docConfig = docConfig,
                            modifier = Modifier.fillMaxSize(),
                            onFragmentReady = { frag -> fragmentRef.value = frag },
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "不支持的文档格式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 浮动沉浸式顶部栏（不显示标题，仅保留返回按钮和页码指示器）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = false,
            trailingContent = {
                // 有分页概念时显示页码指示器（jit-viewer 统一提供 getPageInfo）
                if (hasPageCount) {
                    IOSPageIndicator(
                        currentPage = currentPage,
                        totalPages = totalPages,
                    )
                }
            },
        )

        // 底部浮动控制栏（翻页、缩放），对所有文档统一显示，统一样式
        // 通过 jit-viewer 的 prevPage()/nextPage()/setZoom() API 控制
        if (hasPageCount) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                        .padding(horizontal = EdgeSpacing.sm, vertical = EdgeSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PdfControlButton(
                        icon = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "上一页",
                        onClick = {
                            fragmentRef.value?.view?.findWebView()?.evaluateJavascript(
                                JIT_PREV_PAGE_JS,
                                null,
                            )
                        },
                    )
                    PdfControlButton(
                        icon = Icons.Outlined.ZoomOut,
                        contentDescription = "缩小",
                        onClick = {
                            fragmentRef.value?.view?.findWebView()?.evaluateJavascript(
                                JIT_ZOOM_JS_TEMPLATE.format("0.8"),
                                null,
                            )
                        },
                    )
                    PdfControlButton(
                        icon = Icons.Outlined.ZoomIn,
                        contentDescription = "放大",
                        onClick = {
                            fragmentRef.value?.view?.findWebView()?.evaluateJavascript(
                                JIT_ZOOM_JS_TEMPLATE.format("1.25"),
                                null,
                            )
                        },
                    )
                    PdfControlButton(
                        icon = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "下一页",
                        onClick = {
                            fragmentRef.value?.view?.findWebView()?.evaluateJavascript(
                                JIT_NEXT_PAGE_JS,
                                null,
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * PDF 控制栏按钮（圆形，iOS 风格，无 ripple）。
 */
@Composable
private fun PdfControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 内嵌 DocViewerFragment 渲染文档。
 *
 * 在 Compose 中嵌入 Fragment 的关键点：
 * 1. 通过 findFragmentActivity 递归找到 FragmentActivity（LocalContext 可能是 ContextWrapper）
 * 2. 使用固定 tag + 容器 ID，避免重复添加 Fragment
 * 3. 在 onDispose 中移除 Fragment，避免内存泄漏
 * 4. 在 update 回调里使用 findFragmentByTag 判断是否已添加
 *
 * @param onFragmentReady Fragment 添加完成回调（用于外部查询页数等）
 */
@Composable
private fun EmbeddedDocViewer(
    docConfig: DocConfig,
    modifier: Modifier = Modifier,
    onFragmentReady: ((DocViewerFragment) -> Unit)? = null,
) {
    val context = LocalContext.current
    val containerId = remember { View.generateViewId() }
    val fragment = remember(docConfig) {
        DocViewerFragment.newInstance(docConfig)
    }

    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findFragmentActivity()
            activity?.supportFragmentManager?.let { fm ->
                fm.findFragmentByTag("doc_preview")?.let { frag ->
                    fm.beginTransaction()
                        .remove(frag)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
            }
        },
        modifier = modifier,
        update = { container ->
            val activity = context.findFragmentActivity() ?: return@AndroidView
            val fm = activity.supportFragmentManager
            // 仅在尚未添加时添加，避免每次重组都重复事务
            if (fm.findFragmentByTag("doc_preview") == null) {
                fm.beginTransaction()
                    .replace(container.id, fragment, "doc_preview")
                    .commitAllowingStateLoss()
                // Fragment 事务提交后回调外部（实际 View 创建在 onViewCreated，稍后轮询会拿到 WebView）
                onFragmentReady?.invoke(fragment)
            }
        },
    )
}

// ============================================================================
// Office 在线预览（直链 → 微软 view.officeapps.live.com，与网页端一致）
// ============================================================================

/**
 * Office 文档预览（doc/docx/xls/xlsx/ppt/pptx/pps/ppsx）。
 *
 * 流程与网页端完全一致（Drive.jsx openPreview → getDirect → officeapps 嵌入）：
 * 1. POST /generate-direct-link 生成直链（需实名认证 + 流量充足）
 * 2. 拼接 `https://view.officeapps.live.com/op/embed.aspx?src=<encodeURIComponent(directUrl)>`
 * 3. WebView 加载微软在线预览页
 */
@Composable
private fun OfficePreview(
    file: FileNode,
    onBack: () -> Unit,
) {
    var officeUrl by remember(file) { mutableStateOf<String?>(null) }
    var error by remember(file) { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // 加载浮层状态：避免"白屏即坏了"的观感；失败时提供重试
    var overlayState by remember(file) { mutableStateOf(OfficeOverlayState.LOADING) }
    var loadProgress by remember(file) { mutableIntStateOf(0) }
    var retryCount by remember(file) { mutableIntStateOf(0) }
    var retryNote by remember(file) { mutableStateOf<String?>(null) }
    var lastActivityAt by remember(file) { mutableLongStateOf(System.currentTimeMillis()) }

    // 1) 生成直链（与网页端 getDirect(file) 一致；未实名/流量耗尽会返回对应错误）
    LaunchedEffect(file) {
        when (val result = ServiceLocator.directLinkRepository.generateDirectLink(fileId = file.id)) {
            is EdgeResult.Success -> {
                val directUrl = result.data.directUrl
                if (directUrl.isBlank()) {
                    error = "获取文件直链失败"
                } else {
                    // 与网页端 encodeURIComponent 一致：URLEncoder 会把空格编码为 "+"，需还原为 %20
                    officeUrl = "https://view.officeapps.live.com/op/embed.aspx?src=" +
                        URLEncoder.encode(directUrl, "UTF-8").replace("+", "%20")
                }
            }
            is EdgeResult.Failure -> error = result.error.message ?: "获取文件直链失败"
        }
    }

    // WebView 释放（避免内存泄漏）
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    // 浮层看门狗：LOADING 期间每 3 秒检查一次，满足任一条件即隐藏（露出真实内容）：
    // 1) 连续 8 秒无任何页面活动（请求 / 进度 / 加载事件）→ 画面临近呈现
    // 2) 距开始已超过 35 秒（保底：viewer 遥测心跳可能持续刷新活动时间，避免浮层永久滞留）
    LaunchedEffect(file, overlayState) {
        if (overlayState != OfficeOverlayState.LOADING) return@LaunchedEffect
        val startedAt = System.currentTimeMillis()
        while (true) {
            delay(3_000L)
            val quiet = System.currentTimeMillis() - lastActivityAt
            val elapsed = System.currentTimeMillis() - startedAt
            if (quiet >= 8_000L || elapsed >= 35_000L) {
                Log.i(OFFICE_WEBVIEW_TAG, "overlay hidden (quiet=${quiet}ms elapsed=${elapsed}ms)")
                overlayState = OfficeOverlayState.HIDDEN
                break
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val url = officeUrl
        when {
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "预览失败: $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(EdgeSpacing.lg),
                    )
                }
            }
            url == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(0.5f),
                        )
                        Text(
                            "正在打开文档...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = EdgeSpacing.sm),
                        )
                    }
                }
            }
            else -> {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // WebView 默认拒绝第三方 cookie（Android 5+）。微软 viewer 的 iframe
                            // （sg3-word-view.officeapps.live.com）相对顶层页是第三方域，其会话与
                            // 初始化流程依赖 cookie；被拦会静默卡死 → 白屏。Chrome 浏览器默认允许，
                            // 这是“浏览器正常、App 白屏”的关键差异之一。
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            // 伪装为标准 Chrome UA：去除 "; wv" 与 "Version/4.0 " 标记。
                            // 微软预览服务对嵌入式 WebView UA 可能返回 503（风控/降级），浏览器 UA 更稳。
                            settings.userAgentString = settings.userAgentString
                                .replace("; wv", "")
                                .replace("Version/4.0 ", "")
                            // 资源诊断 + CDN 镜像容灾：
                            // - 诊断日志：白屏时定位失败资源（URL + 错误码 + SSL 错误），由 logcat 采集
                            // - 镜像：res.public.onecdn.static.microsoft（Cloudflare）在部分网络下 TLS 校验失败，
                            //   导致 appresourceloader.min.js 等核心脚本缺失 → 白屏；同路径资源改从
                            //   res-h1.cdn.office.net（Akamai）拉取（已验证资源同源存在且设备可达）。
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView,
                                    request: WebResourceRequest,
                                ): WebResourceResponse? {
                                    val uri = request.url
                                    Log.i(OFFICE_WEBVIEW_TAG, "req: $uri")
                                    // 任何子资源请求都视为页面活动（浮层隐藏判断用）
                                    lastActivityAt = System.currentTimeMillis()
                                    if (!OFFICE_CDN_HOST.equals(uri.host, ignoreCase = true)) return null
                                    val mirrorUrl = uri.toString()
                                        .replace(OFFICE_CDN_HOST, OFFICE_CDN_MIRROR_HOST)
                                    return try {
                                        val resp = officeMirrorClient
                                            .newCall(Request.Builder().url(mirrorUrl).build())
                                            .execute()
                                        val body = resp.body
                                        if (resp.isSuccessful && body != null) {
                                            val mime = resp.header("Content-Type")
                                                ?.substringBefore(';')?.trim()
                                                ?.takeIf { it.isNotBlank() }
                                                ?: guessResourceMime(uri.path.orEmpty())
                                            val encoding = if (
                                                mime.startsWith("text/") ||
                                                mime.contains("javascript") ||
                                                mime.contains("json")
                                            ) "utf-8" else null
                                            Log.i(OFFICE_WEBVIEW_TAG, "cdnMirror: ${uri.path} (${resp.code})")
                                            WebResourceResponse(
                                                mime,
                                                encoding,
                                                resp.code,
                                                "OK",
                                                mapOf(
                                                    "Access-Control-Allow-Origin" to "*",
                                                    "Cache-Control" to (resp.header("Cache-Control")
                                                        ?: "public, max-age=3600"),
                                                ),
                                                body.byteStream(),
                                            )
                                        } else {
                                            body?.close()
                                            Log.e(OFFICE_WEBVIEW_TAG, "cdnMirror fail: $mirrorUrl code=${resp.code}")
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Log.e(OFFICE_WEBVIEW_TAG, "cdnMirror error: $mirrorUrl ${e.message}")
                                        null
                                    }
                                }

                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    Log.i(OFFICE_WEBVIEW_TAG, "onPageStarted: $url")
                                    // 整页重载开始后清除重试提示（若再次 503 会重新设置）
                                    retryNote = null
                                    lastActivityAt = System.currentTimeMillis()
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    Log.i(OFFICE_WEBVIEW_TAG, "onPageFinished: $url")
                                    lastActivityAt = System.currentTimeMillis()
                                    // 修复 + 诊断：embed 页的 viewer iframe（#wacframe）使用 height:100%，
                                    // 在 WebView 上该值会塌陷为 0（诊断实测 360x0）→ 渲染区高度为零 → 白屏。
                                    // 注入脚本在 iframe 出现后强制写入像素尺寸（轮询重试 + resize 跟随）。
                                    view.postDelayed({
                                        view.evaluateJavascript(
                                            "(function(){var n=0;function fix(){var f=document.getElementById('wacframe');" +
                                                "if(f){var w=Math.max(window.innerWidth||0,document.documentElement.clientWidth||0)||360;" +
                                                "var h=Math.max(window.innerHeight||0,document.documentElement.clientHeight||0)||800;" +
                                                "f.style.width=w+'px';f.style.height=h+'px';" +
                                                "console.log('DIAG iframe fixed '+w+'x'+h+' ih='+window.innerHeight);}" +
                                                "if(++n<12)setTimeout(fix,500);}" +
                                                "fix();" +
                                                "window.addEventListener('resize',function(){var f=document.getElementById('wacframe');" +
                                                "if(f){var w=Math.max(window.innerWidth||0,document.documentElement.clientWidth||0)||360;" +
                                                "var h=Math.max(window.innerHeight||0,document.documentElement.clientHeight||0)||800;" +
                                                "f.style.width=w+'px';f.style.height=h+'px';}});})()",
                                            null,
                                        )
                                    }, 1_500L)
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError,
                                ) {
                                    Log.e(
                                        OFFICE_WEBVIEW_TAG,
                                        "onReceivedError: ${request.url} code=${error.errorCode} desc=${error.description}",
                                    )
                                    // 仅主框架失败判定为预览失败（子资源错误由页面自身容错）
                                    if (request.isForMainFrame) {
                                        retryNote = "网络错误：${error.description}"
                                        overlayState = OfficeOverlayState.FAILED
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView,
                                    handler: SslErrorHandler,
                                    error: SslError,
                                ) {
                                    Log.e(
                                        OFFICE_WEBVIEW_TAG,
                                        "onReceivedSslError: ${error.url} primary=${error.primaryError}",
                                    )
                                    // 保持系统默认行为（cancel），不绕过证书校验
                                    super.onReceivedSslError(view, handler, error)
                                }

                                override fun onReceivedHttpError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    errorResponse: WebResourceResponse,
                                ) {
                                    Log.e(
                                        OFFICE_WEBVIEW_TAG,
                                        "onReceivedHttpError: ${request.url} status=${errorResponse.statusCode}",
                                    )
                                    // 微软预览服务对嵌入式 iframe 偶发 503（风控/降级）：自动整页重试（最多 2 次）
                                    val failedUrl = request.url.toString()
                                    if (errorResponse.statusCode == 503 &&
                                        failedUrl.contains("officeapps.live.com")
                                    ) {
                                        if (retryCount < 2) {
                                            retryCount++
                                            retryNote = "微软在线服务繁忙，正在自动重试（$retryCount/2）…"
                                            lastActivityAt = System.currentTimeMillis()
                                            Log.i(OFFICE_WEBVIEW_TAG, "autoRetry($retryCount) after 503: $failedUrl")
                                            view.postDelayed({ if (webViewRef === view) view.reload() }, 2_500L)
                                        } else {
                                            retryNote = "微软在线服务暂时不可用（503），请稍后重试"
                                            overlayState = OfficeOverlayState.FAILED
                                        }
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    Log.i(OFFICE_WEBVIEW_TAG, "progress: $newProgress")
                                    loadProgress = newProgress
                                    lastActivityAt = System.currentTimeMillis()
                                }
                            }
                            Log.i(OFFICE_WEBVIEW_TAG, "loadUrl: $url")
                            lastActivityAt = System.currentTimeMillis()
                            loadUrl(url)
                        }.also { webViewRef = it }
                    },
                    // 避开系统导航栏/手势条区域：沉浸式布局下 WebView 延伸到屏幕最底，
                    // viewer 页面的底部工具栏会落入系统手势区导致“点不到”（与视频预览控制栏同策略）。
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars),
                )
            }
        }

        // 加载/失败浮层：避免白屏无提示的观感；轻触任意处即可跳过等待（用户可随时关掉）
        if (url != null && overlayState != OfficeOverlayState.HIDDEN) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { overlayState = OfficeOverlayState.HIDDEN },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = EdgeSpacing.xxl)
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(horizontal = 28.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (overlayState == OfficeOverlayState.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                        )
                        Text(
                            "正在加载文档…",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = EdgeSpacing.lg),
                        )
                        Text(
                            text = retryNote ?: when {
                                loadProgress >= 100 -> "正在渲染文档…"
                                loadProgress > 0 -> "页面加载 $loadProgress%"
                                else -> "正在连接微软在线预览服务…"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = EdgeSpacing.xs),
                        )
                        Text(
                            "由微软在线服务渲染",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = EdgeSpacing.sm),
                        )
                    } else {
                        Text(
                            "文档加载遇到问题",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                        )
                        retryNote?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = EdgeSpacing.xs),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                            modifier = Modifier.padding(top = EdgeSpacing.lg),
                        ) {
                            TextButton(
                                onClick = { overlayState = OfficeOverlayState.HIDDEN },
                            ) { Text("先看看") }
                            Button(
                                onClick = {
                                    retryCount = 0
                                    retryNote = null
                                    loadProgress = 0
                                    lastActivityAt = System.currentTimeMillis()
                                    overlayState = OfficeOverlayState.LOADING
                                    webViewRef?.reload()
                                },
                            ) { Text("重试") }
                        }
                    }
                }
            }
        }

        // 浮动沉浸式顶部栏（不显示标题，微软预览页顶部自带文件名与工具栏）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = false,
        )
    }
}

/**
 * 其他文件（不可预览类型）→ 先下载到本地，再用系统应用打开（类似 QQ/WPS）。
 */
@Composable
private fun OtherPreview(
    file: FileNode,
    line: TransferLine,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 其他文件内容（铺满屏幕，标题栏浮在上层）
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = EdgeSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                file.basename,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = EdgeSpacing.lg),
            )
            Text(
                text = formatBytes(file.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = EdgeSpacing.xs),
            )
            Text(
                text = "暂不支持该文件格式预览",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = EdgeSpacing.xxl),
            )

            downloadError?.let {
                Text(
                    text = "下载失败：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = EdgeSpacing.sm),
                )
            }

            if (isDownloading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(top = EdgeSpacing.lg),
                )
                Text(
                    "下载中...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = EdgeSpacing.sm),
                )
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isDownloading = true
                            downloadError = null
                            val localFile = PreviewCacheManager.downloadAndCache(context, file, buildDownloadUrl(file, line))
                            isDownloading = false
                            if (localFile != null) {
                                openWithExternalApp(context, localFile, file.basename)
                            } else {
                                downloadError = "请检查网络或登录状态"
                            }
                        }
                    },
                    modifier = Modifier.padding(top = EdgeSpacing.lg),
                ) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text("下载并用其他应用打开", modifier = Modifier.padding(start = EdgeSpacing.sm))
                }
            }
        }

        // 浮动沉浸式顶部栏（标题常驻，因页面不可滚动）
        IOSImmersivePreviewHeader(
            title = file.basename,
            onBack = onBack,
            titleVisible = true,
        )
    }
}

// ============================================================================
// 工具函数
// ============================================================================

/** 用系统应用打开本地文件。 */
private fun openWithExternalApp(context: Context, file: File, filename: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val mime = getMimeType(filename)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        // 没有 App 能处理时 startActivity 会抛 ActivityNotFoundException
        context.startActivity(Intent.createChooser(intent, "使用其他应用打开"))
    } catch (_: Exception) {
        // fallback：忽略
    }
}

// ============================================================================
// 页数查询工具（文档预览，基于 jit-viewer SDK）
// ============================================================================

/**
 * 递归查找 View 树中的 WebView。
 *
 * DocViewerFragment 的 webView 字段为 private，无法直接访问，
 * 通过遍历 Fragment 的 View 层级找到 WebView 实例。
 */
private fun View.findWebView(): WebView? {
    if (this is WebView) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val found = getChildAt(i).findWebView()
            if (found != null) return found
        }
    }
    return null
}

/**
 * 统一页数查询 JS（基于 jit-viewer SDK）。
 *
 * jit-viewer 提供统一的 getPageInfo() API，支持 PDF/Word/Excel/PPT/Markdown 等所有格式，
 * 页数由各渲染引擎准确提供，无需滚动估算。
 * 返回格式："当前页/总页数" 或 ""（未就绪）。
 */
private const val JIT_PAGE_QUERY_JS =
    """(function(){
  try{
    if(typeof window.getPageInfo === 'function'){
      var r = window.getPageInfo();
      if(r && r.indexOf('/') >= 0) return r;
    }
  }catch(e){}
  return '';
})()"""

/** jit-viewer 翻页 JS：上一页 */
private const val JIT_PREV_PAGE_JS = "if(typeof window.prevPage==='function'){window.prevPage();}"

/** jit-viewer 翻页 JS：下一页 */
private const val JIT_NEXT_PAGE_JS = "if(typeof window.nextPage==='function'){window.nextPage();}"

/** jit-viewer 缩放 JS：scale 为缩放比例（0.25 - 5） */
private const val JIT_ZOOM_JS_TEMPLATE = "if(typeof window.setZoom==='function'){window.setZoom(%s);}"

/**
 * 解析 evaluateJavascript 的页数返回值。
 *
 * 返回格式："当前页/总页数" 或 ""（未就绪）。
 * WebView 回调可能为：null、"\"3/15\""、"\"\""、或 "3/15"（部分版本）。
 * @return Pair(当前页, 总页数)，解析失败或未就绪返回 null
 */
private fun parsePageInfo(result: String?): Pair<Int, Int>? {
    if (result.isNullOrBlank() || result == "null") return null
    val raw = result.removeSurrounding("\"").trim()
    if (raw.isEmpty() || !raw.contains("/")) return null
    val parts = raw.split("/")
    val cur = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val total = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (cur < 1 || total < 1) return null
    return cur to total
}

