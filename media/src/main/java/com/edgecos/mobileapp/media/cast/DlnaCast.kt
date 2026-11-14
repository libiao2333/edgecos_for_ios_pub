package com.edgecos.mobileapp.media.cast

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import android.util.Xml
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.DatagramPacket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URI
import java.util.concurrent.TimeUnit

// ============================================================================
// DLNA/UPnP 投屏（SSDP 发现 + AVTransport SOAP 控制）
//
// 投屏要求 TV 能直接拉流，因此播放地址必须是免登录的直链
// （由 DirectLinkRepository.generateDirectLink 生成，而非带 Cookie 的下载地址）。
// 本文件所有网络操作均在 IO 线程执行，超时/异常一律安全降级，绝不抛出。
// ============================================================================

/**
 * 投屏设备（DLNA MediaRenderer）。
 *
 * @param name       设备友好名称（如"客厅的小米电视"）
 * @param host       设备 IP
 * @param controlUrl AVTransport 服务控制地址（SOAP 端点）
 */
data class CastDevice(
    val name: String,
    val host: String,
    val controlUrl: String,
)

/** 投屏播放位置（由 TV 上报）。 */
data class CastPosition(
    val positionMs: Long,
    val durationMs: Long,
)

/** 投屏共享 HTTP 客户端（局域网直连，短超时，与业务网络请求隔离）。 */
private val castHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
}

/**
 * DLNA/UPnP 设备发现（SSDP M-SEARCH）。
 *
 * 向组播地址 239.255.255.250:1900 发送 M-SEARCH 搜索 MediaRenderer，
 * 收集响应中的 LOCATION 描述地址，解析出支持 AVTransport 服务的设备。
 * 全程 IO 线程执行，超时/异常均降级为空列表，不会抛出。
 */
object DlnaDiscovery {

    private const val TAG = "DlnaDiscovery"
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    private const val MEDIA_RENDERER_ST = "urn:schemas-upnp-org:device:MediaRenderer:1"

    /**
     * 搜索局域网中的投屏设备。
     *
     * @param timeoutMs 等待设备响应的总时长（默认 4 秒）
     * @return 支持 AVTransport 的设备列表（可能为空，绝不抛异常）
     */
    suspend fun discover(context: Context, timeoutMs: Long = 4000): List<CastDevice> =
        withContext(Dispatchers.IO) {
            // 部分机型需要 MulticastLock 才能收发组播；获取失败不影响主流程
            val multicastLock = runCatching {
                val wifi = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifi?.createMulticastLock("edgecos-dlna")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }.getOrNull()

            try {
                MulticastSocket().use { socket ->
                    socket.soTimeout = 800
                    // 指定活动网卡（优先 Wi-Fi），多网卡机型避免组播走错接口
                    findActiveInterface()?.let { nif ->
                        runCatching { socket.networkInterface = nif }
                    }

                    val searchMessage = buildString {
                        append("M-SEARCH * HTTP/1.1\r\n")
                        append("HOST: $SSDP_ADDRESS:$SSDP_PORT\r\n")
                        append("MAN: \"ssdp:discover\"\r\n")
                        append("MX: 2\r\n")
                        append("ST: $MEDIA_RENDERER_ST\r\n")
                        append("USER-AGENT: EdgeCOS/1.0 UPnP/1.1\r\n")
                        append("\r\n")
                    }.toByteArray(Charsets.UTF_8)
                    val group = InetAddress.getByName(SSDP_ADDRESS)

                    // 连发两次提高抵达率（UDP 不可靠）
                    repeat(2) {
                        runCatching {
                            socket.send(DatagramPacket(searchMessage, searchMessage.size, group, SSDP_PORT))
                        }
                        delay(150)
                    }

                    // 收集响应中的 LOCATION（去重）
                    val locations = LinkedHashSet<String>()
                    val deadline = System.currentTimeMillis() + timeoutMs
                    val buffer = ByteArray(4096)
                    while (System.currentTimeMillis() < deadline) {
                        val packet = DatagramPacket(buffer, buffer.size)
                        try {
                            socket.receive(packet)
                        } catch (_: SocketTimeoutException) {
                            continue
                        }
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        parseHeader(text, "LOCATION")?.let(locations::add)
                    }

                    // 并行解析设备描述，筛选支持 AVTransport 的设备
                    locations.map { location -> async { fetchDevice(location) } }
                        .awaitAll()
                        .filterNotNull()
                        .distinctBy { it.controlUrl }
                }
            } catch (e: Exception) {
                Log.w(TAG, "SSDP 搜索失败", e)
                emptyList()
            } finally {
                runCatching { multicastLock?.takeIf { it.isHeld }?.release() }
            }
        }

    /** 选择可用于组播的活动网卡（优先 Wi-Fi，其次任意带 IPv4 的非回环接口）。 */
    private fun findActiveInterface(): NetworkInterface? = runCatching {
        val candidates = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .filter { nif ->
                nif.isUp && !nif.isLoopback &&
                    nif.inetAddresses.toList().any { it is Inet4Address && !it.isLoopbackAddress }
            }
        candidates.firstOrNull { nif ->
            val name = nif.name.lowercase()
            name.startsWith("wlan") || name.startsWith("ap")
        } ?: candidates.firstOrNull()
    }.getOrNull()

    /** 从 SSDP 响应头中提取字段值（大小写不敏感）。 */
    private fun parseHeader(response: String, name: String): String? =
        response.lineSequence()
            .firstOrNull { it.startsWith("$name:", ignoreCase = true) }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    /** 拉取并解析设备描述 XML，返回投屏设备；不支持 AVTransport 或失败返回 null。 */
    private fun fetchDevice(location: String): CastDevice? = runCatching {
        val request = Request.Builder().url(location).get().build()
        castHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val description = response.body?.string() ?: return null
            val parsed = parseDescription(description)
            val controlPath = parsed.controlUrl ?: return null
            val resolvedControlUrl = URI(location).resolve(controlPath).toString()
            val name = parsed.friendlyName ?: URI(location).host
            CastDevice(name = name, host = URI(location).host, controlUrl = resolvedControlUrl)
        }
    }.getOrElse { e ->
        Log.d(TAG, "设备描述获取失败：$location", e)
        null
    }

    /** 设备描述解析结果。 */
    private data class Description(val friendlyName: String?, val controlUrl: String?)

    /**
     * 解析 UPnP 设备描述 XML（命名空间前缀统一剥离，兼容各厂商）：
     * - friendlyName：文档中首个出现值（根设备名称）
     * - controlUrl：首个 serviceType 包含 AVTransport 的服务的相对 controlURL
     */
    private fun parseDescription(xml: String): Description {
        var friendlyName: String? = null
        var controlUrl: String? = null
        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xml))
            var inService = false
            var serviceType: String? = null
            var serviceControlUrl: String? = null

            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val tag = parser.name?.substringAfterLast(':')?.lowercase()
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "friendlyname" -> if (friendlyName == null) {
                            friendlyName = readTextSafe(parser)
                        }
                        "service" -> {
                            inService = true
                            serviceType = null
                            serviceControlUrl = null
                        }
                        "servicetype" -> if (inService) serviceType = readTextSafe(parser)
                        "controlurl" -> if (inService) serviceControlUrl = readTextSafe(parser)
                    }
                    XmlPullParser.END_TAG -> if (tag == "service") {
                        if (controlUrl == null &&
                            serviceType?.contains("AVTransport", ignoreCase = true) == true &&
                            !serviceControlUrl.isNullOrBlank()
                        ) {
                            controlUrl = serviceControlUrl
                        }
                        inService = false
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            Log.d(TAG, "设备描述 XML 解析失败", e)
        }
        return Description(friendlyName, controlUrl)
    }

    /** 读取当前元素的文本（空元素返回 null）。 */
    private fun readTextSafe(parser: XmlPullParser): String? =
        runCatching { parser.nextText() }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

/**
 * DLNA AVTransport 控制器（SOAP over HTTP）。
 *
 * 通过 UPnP AVTransport 服务控制 TV：
 * - [playUrl]      让 TV 打开指定 URL 并开始播放（投屏入口）
 * - [play]/[pause]/[stop] 播放控制
 * - [seek]         跳转进度（REL_TIME）
 * - [getPosition]  查询 TV 播放进度（驱动进度条）
 *
 * 所有方法内部捕获全部异常并以 false/null 降级，调用方无需处理异常。
 */
object DlnaController {

    private const val TAG = "DlnaController"
    private const val AVT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1"

    /** 进程级作用域：用于离开页面等 fire-and-forget 场景（不随组合取消）。 */
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 让 TV 打开 [url] 并开始播放。成功返回 true。 */
    suspend fun playUrl(controlUrl: String, url: String, title: String): Boolean =
        setAvTransportUri(controlUrl, url, buildDidlMetadata(url, title)) && play(controlUrl)

    /** 设置播放源（不自动播放）。 */
    suspend fun setAvTransportUri(controlUrl: String, url: String, metadata: String): Boolean {
        val innerRequest = buildString {
            append("<InstanceID>0</InstanceID>")
            append("<CurrentURI>").append(xmlEscape(url)).append("</CurrentURI>")
            append("<CurrentURIMetaData>").append(xmlEscape(metadata)).append("</CurrentURIMetaData>")
        }
        return invokeAction(controlUrl, "SetAVTransportURI", innerRequest)
    }

    suspend fun play(controlUrl: String): Boolean =
        invokeAction(controlUrl, "Play", "<InstanceID>0</InstanceID><Speed>1</Speed>")

    suspend fun pause(controlUrl: String): Boolean =
        invokeAction(controlUrl, "Pause", "<InstanceID>0</InstanceID>")

    suspend fun stop(controlUrl: String): Boolean =
        invokeAction(controlUrl, "Stop", "<InstanceID>0</InstanceID>")

    /** 跳转到指定进度（REL_TIME）。 */
    suspend fun seek(controlUrl: String, positionMs: Long): Boolean =
        invokeAction(
            controlUrl,
            "Seek",
            "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>${formatDuration(positionMs)}</Target>",
        )

    /** 查询 TV 播放进度。失败返回 null。 */
    suspend fun getPosition(controlUrl: String): CastPosition? {
        val response = invokeActionRaw(controlUrl, "GetPositionInfo", "<InstanceID>0</InstanceID>")
            ?: return null
        val position = parseDuration(extractTag(response, "RelTime")) ?: 0L
        val duration = parseDuration(extractTag(response, "TrackDuration")) ?: 0L
        return CastPosition(position, duration)
    }

    /** 异步停止投屏（页面销毁等 fire-and-forget 场景，不阻塞调用方）。 */
    fun stopAsync(controlUrl: String) {
        backgroundScope.launch { runCatching { stop(controlUrl) } }
    }

    // ==================== 内部实现 ====================

    private suspend fun invokeAction(controlUrl: String, action: String, innerXml: String): Boolean =
        invokeActionRaw(controlUrl, action, innerXml) != null

    private suspend fun invokeActionRaw(
        controlUrl: String,
        action: String,
        innerXml: String,
    ): String? = withContext(Dispatchers.IO) {
        val envelope = """<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body>
<u:$action xmlns:u="$AVT_SERVICE">$innerXml</u:$action>
</s:Body>
</s:Envelope>"""
        runCatching {
            val request = Request.Builder()
                .url(controlUrl)
                .header("SOAPACTION", "\"$AVT_SERVICE#$action\"")
                .post(envelope.toRequestBody("text/xml; charset=utf-8".toMediaType()))
                .build()
            castHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() ?: "" else null
            }
        }.getOrElse { e ->
            Log.d(TAG, "SOAP $action 失败", e)
            null
        }
    }

    /** 构建 DLNA 投屏元数据（DIDL-Lite，video 类型，TV 端用于识别媒体）。 */
    private fun buildDidlMetadata(url: String, title: String): String =
        """<DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
<item id="0" parentID="-1" restricted="1">
<dc:title>${xmlEscape(title)}</dc:title>
<upnp:class>object.item.videoItem</upnp:class>
<res protocolInfo="http-get:*:video/*:*">${xmlEscape(url)}</res>
</item>
</DIDL-Lite>"""
}

// ==================== 工具函数 ====================

/** SOAP/XML 特殊字符转义。 */
private fun xmlEscape(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

/** 提取 SOAP 响应中指定标签的文本（容忍命名空间前缀）。 */
private fun extractTag(xml: String, tag: String): String? =
    Regex("<(?:[A-Za-z0-9]+:)?$tag>(.*?)</(?:[A-Za-z0-9]+:)?$tag>", RegexOption.DOT_MATCHES_ALL)
        .find(xml)?.groupValues?.getOrNull(1)?.trim()

/**
 * 解析 UPnP 时长字符串（"H:MM:SS" 或 "H:MM:SS.mmm"）为毫秒。
 * TV 上报 "NOT_IMPLEMENTED" 等非法值时返回 null。
 */
private fun parseDuration(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    val parts = text.split(":")
    if (parts.size != 3) return null
    val hours = parts[0].toLongOrNull() ?: return null
    val minutes = parts[1].toLongOrNull() ?: return null
    val seconds = parts[2].toDoubleOrNull() ?: return null
    val ms = ((hours * 3600 + minutes * 60 + seconds) * 1000).toLong()
    return ms.takeIf { it >= 0 }
}

/** 毫秒 → "H:MM:SS"（Seek REL_TIME 格式）。 */
private fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d:%02d".format(totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60)
}
