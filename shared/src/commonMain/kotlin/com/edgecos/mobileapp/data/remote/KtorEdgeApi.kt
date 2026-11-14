package com.edgecos.mobileapp.data.remote

import com.edgecos.mobileapp.domain.error.EdgeError
import com.edgecos.mobileapp.shared.dto.AnnouncementsResponse
import com.edgecos.mobileapp.shared.dto.BatchCopyRequest
import com.edgecos.mobileapp.shared.dto.BatchDeleteRequest
import com.edgecos.mobileapp.shared.dto.BatchIdsRequest
import com.edgecos.mobileapp.shared.dto.BatchMoveRequest
import com.edgecos.mobileapp.shared.dto.CaptchaPrecheckRequest
import com.edgecos.mobileapp.shared.dto.CaptchaPrecheckResponse
import com.edgecos.mobileapp.shared.dto.CopyRequest
import com.edgecos.mobileapp.shared.dto.DeleteRequest
import com.edgecos.mobileapp.shared.dto.DirectLinkCreateRequest
import com.edgecos.mobileapp.shared.dto.DirectLinkCreateResponse
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import com.edgecos.mobileapp.shared.dto.FileSummaryDto
import com.edgecos.mobileapp.shared.dto.LoginRequest
import com.edgecos.mobileapp.shared.dto.LoginResponse
import com.edgecos.mobileapp.shared.dto.MkdirRequest
import com.edgecos.mobileapp.shared.dto.MoveRequest
import com.edgecos.mobileapp.shared.dto.MyPlanResponse
import com.edgecos.mobileapp.shared.dto.OAuthQrStatusResponse
import com.edgecos.mobileapp.shared.dto.OAuthSessionRequest
import com.edgecos.mobileapp.shared.dto.OAuthSessionResponse
import com.edgecos.mobileapp.shared.dto.OrderDetailResponse
import com.edgecos.mobileapp.shared.dto.OrdersResponse
import com.edgecos.mobileapp.shared.dto.PasskeyCredentialsResponse
import com.edgecos.mobileapp.shared.dto.PasskeyLoginOptionsRequest
import com.edgecos.mobileapp.shared.dto.PasskeyLoginOptionsResponse
import com.edgecos.mobileapp.shared.dto.PasskeyLoginVerifyRequest
import com.edgecos.mobileapp.shared.dto.PasskeyRegisterOptionsResponse
import com.edgecos.mobileapp.shared.dto.PasskeyRegisterVerifyRequest
import com.edgecos.mobileapp.shared.dto.PayCancelRequest
import com.edgecos.mobileapp.shared.dto.PayCreateRequest
import com.edgecos.mobileapp.shared.dto.PayCreateResponse
import com.edgecos.mobileapp.shared.dto.PayQueryRequest
import com.edgecos.mobileapp.shared.dto.PayQueryResponse
import com.edgecos.mobileapp.shared.dto.PayRepayRequest
import com.edgecos.mobileapp.shared.dto.PlanDto
import com.edgecos.mobileapp.shared.dto.QuotaResponse
import com.edgecos.mobileapp.shared.dto.RegisterRequest
import com.edgecos.mobileapp.shared.dto.RenameRequest
import com.edgecos.mobileapp.shared.dto.SendEmailCodeRequest
import com.edgecos.mobileapp.shared.dto.ShareCreateRequest
import com.edgecos.mobileapp.shared.dto.ShareCreateResponse
import com.edgecos.mobileapp.shared.dto.ShareFilesRequest
import com.edgecos.mobileapp.shared.dto.ShareInfoResponse
import com.edgecos.mobileapp.shared.dto.UploadCompleteRequest
import com.edgecos.mobileapp.shared.dto.UploadConflictResponse
import com.edgecos.mobileapp.shared.dto.UploadInstantCompleteRequest
import com.edgecos.mobileapp.shared.dto.UploadTokenResponse
import com.edgecos.mobileapp.shared.dto.WorkOrderDto
import com.edgecos.mobileapp.shared.dto.WorkOrderReplyDto
import com.edgecos.mobileapp.shared.dto.WorkOrderReplyRequest
import com.edgecos.mobileapp.shared.dto.WorkOrderStatusRequest
import com.edgecos.mobileapp.shared.dto.WorkOrderSubmitRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * [EdgeApi] 的 Ktor 实现，供共享层与 iOS 使用。
 *
 * - Android 主链路仍是 Retrofit（`RetrofitEdgeApi` + `EdgeApiImpl`），本类默认不参与 Android 运行时；
 * - HTTP 方法 / 路径 / query / 请求体与 `RetrofitEdgeApi` 逐个对应（54 个方法）；
 * - `RetrofitEdgeApi` 中仅 Android 使用的 3 个下载方法（`download` / `downloadShareFile` /
 *   `accessDirectLink`）不属于共享 [EdgeApi] 契约，这里不实现（返回 `Response<Unit>`，需流式下载能力，
 *   留待后续下载工作包）；
 * - 非 2xx 响应按 Android `ResponseInterceptor` 的规则映射为 [EdgeError]，由 `runCatchingEdge`
 *   收敛后交给 Repository / UI；
 * - Cookie 由 [io.ktor.client.plugins.cookies.HttpCookies] 在内存中维护（httpOnly JWT），
 *   持久化见 `NetworkClient.kt` 的 TODO。
 *
 * @param baseUrl 后端 API 根地址，统一补全尾部 `/` 后拼接相对路径（默认值与 Android `BASE_URL`
 *   一致：`https://user.edgecos.com/api/`）。
 * @param client Ktor 客户端，默认由 [createPlatformHttpClient] 按平台创建；调用方负责在不再使用时关闭
 *   自己传入的 client。
 *
 * 构造器说明：主构造是 `internal`，Kotlin/Native 不会把它导出到 ObjC/Swift（避免 `HttpClient`
 * 进入共享 framework 的公开接口）；对外只暴露两个公开二级构造：
 * - [KtorEdgeApi] 无参：Swift `KtorEdgeApi()`
 * - [KtorEdgeApi] 单参：Swift `KtorEdgeApi(baseUrl:)`
 *
 * Android / 共享层内部仍可注入自定义 client（同 module 可见 internal 主构造）。
 */
class KtorEdgeApi internal constructor(
    baseUrl: String,
    private val client: HttpClient,
) : EdgeApi {

    /** 使用默认后端地址（`https://user.edgecos.com/api/`）与平台默认 client。 */
    constructor() : this("https://user.edgecos.com/api/", createPlatformHttpClient())

    /** 使用指定后端地址与平台默认 client。 */
    constructor(baseUrl: String) : this(baseUrl, createPlatformHttpClient())

    /** 统一以 `/` 结尾，后续请求 URL = [apiBaseUrl] + 相对路径。 */
    private val apiBaseUrl: String = baseUrl.trimEnd('/') + "/"

    // ==================== §2.1 注册 / 登录 / 邮箱验证码 ====================

    override suspend fun sendEmailCode(body: SendEmailCodeRequest) {
        postJson("send-email-code") { setBody(body) }
    }

    override suspend fun register(body: RegisterRequest): LoginResponse =
        postJson("register") { setBody(body) }.decode()

    override suspend fun login(body: LoginRequest): LoginResponse =
        postJson("login") { setBody(body) }.decode()

    // ==================== §2.11 Captcha 预检 ====================

    override suspend fun captchaPrecheck(body: CaptchaPrecheckRequest): CaptchaPrecheckResponse =
        postJson("captcha/precheck") { setBody(body) }.decode()

    // ==================== §2.4 用户中心 ====================

    override suspend fun getUserProfile(): LoginResponse = get("user/profile").decode()

    override suspend fun getUserQuota(): QuotaResponse = get("user/quota").decode()

    // ==================== §2.5 文件管理 ====================

    override suspend fun getFilesSummary(): FileSummaryDto = get("files/summary").decode()

    override suspend fun listFiles(path: String): List<FileNodeDto> =
        get("files") { parameter("path", path) }.decode()

    override suspend fun checkUploadConflict(filename: String, path: String): UploadConflictResponse =
        get("upload/conflict") {
            parameter("filename", filename)
            parameter("path", path)
        }.decode()

    override suspend fun getUploadToken(
        filename: String,
        filesize: Long,
        path: String,
        hash: String,
        overwrite: Boolean,
    ): UploadTokenResponse = get("upload/token") {
        parameter("filename", filename)
        parameter("filesize", filesize)
        parameter("path", path)
        parameter("hash", hash)
        parameter("overwrite", overwrite)
    }.decode()

    override suspend fun uploadComplete(body: UploadCompleteRequest) {
        postJson("upload/complete") { setBody(body) }
    }

    override suspend fun uploadInstantComplete(body: UploadInstantCompleteRequest) {
        postJson("upload/instant-complete") { setBody(body) }
    }

    /**
     * 把文件字节直传到后端返回的预签名 PUT 地址（OSS）。
     *
     * 与业务接口不同，目标地址是外部 OSS：HttpCookies 只对同域请求生效，
     * 因此不会把登录 Cookie 泄露给 OSS。
     *
     * @param onProgress 已发送字节数 / 总字节数
     */
    suspend fun uploadRaw(
        putUrl: String,
        bytes: ByteArray,
        contentType: String = "application/octet-stream",
        onProgress: (sent: Long, total: Long) -> Unit = { _, _ -> },
    ) {
        client.put(putUrl) {
            setBody(bytes)
            contentType(ContentType.parse(contentType))
            onUpload { sent, contentLength ->
                onProgress(sent, contentLength ?: bytes.size.toLong())
            }
        }.ensureSuccess("upload")
    }

    override suspend fun mkdir(body: MkdirRequest) {
        postJson("mkdir") { setBody(body) }
    }

    override suspend fun rename(body: RenameRequest) {
        postJson("rename") { setBody(body) }
    }

    override suspend fun move(body: MoveRequest) {
        postJson("move") { setBody(body) }
    }

    override suspend fun copy(body: CopyRequest) {
        postJson("copy") { setBody(body) }
    }

    override suspend fun delete(body: DeleteRequest) {
        postJson("delete") { setBody(body) }
    }

    override suspend fun batchMove(body: BatchMoveRequest) {
        postJson("batch/move") { setBody(body) }
    }

    override suspend fun batchCopy(body: BatchCopyRequest) {
        postJson("batch/copy") { setBody(body) }
    }

    override suspend fun batchDelete(body: BatchDeleteRequest) {
        postJson("batch/delete") { setBody(body) }
    }

    override suspend fun listTrash(): List<FileNodeDto> = get("trash").decode()

    override suspend fun restoreTrash(body: BatchIdsRequest) {
        postJson("trash/restore") { setBody(body) }
    }

    override suspend fun deleteTrash(body: BatchIdsRequest) {
        postJson("trash/delete") { setBody(body) }
    }

    override suspend fun batchRestoreTrash(body: BatchIdsRequest) {
        postJson("batch/trash/restore") { setBody(body) }
    }

    override suspend fun batchDeleteTrash(body: BatchIdsRequest) {
        postJson("batch/trash/delete") { setBody(body) }
    }

    // ==================== §2.11 公告 ====================

    override suspend fun getAnnouncements(): AnnouncementsResponse = get("announcements").decode()

    override suspend fun markAnnouncementRead(id: Long) {
        post("announcements/$id/read")
    }

    // ==================== 工单 ====================

    override suspend fun submitWorkOrder(body: WorkOrderSubmitRequest): WorkOrderDto =
        postJson("workorder") { setBody(body) }.decode()

    override suspend fun listWorkOrders(): List<WorkOrderDto> = get("workorder").decode()

    override suspend fun getWorkOrderDetail(id: Long): WorkOrderDto =
        get("workorder/$id").decode()

    override suspend fun replyWorkOrder(id: Long, body: WorkOrderReplyRequest): WorkOrderReplyDto =
        postJson("workorder/$id/reply") { setBody(body) }.decode()

    override suspend fun updateWorkOrderStatus(id: Long, body: WorkOrderStatusRequest): WorkOrderDto =
        patchJson("workorder/$id/status") { setBody(body) }.decode()

    // ==================== Passkey ====================

    override suspend fun getPasskeyCredentials(): PasskeyCredentialsResponse =
        get("auth/passkey/credentials").decode()

    override suspend fun getPasskeyRegisterOptions(body: Map<String, String>): PasskeyRegisterOptionsResponse =
        postJson("auth/passkey/register/options") { setBody(body) }.decode()

    override suspend fun verifyPasskeyRegister(body: PasskeyRegisterVerifyRequest) {
        postJson("auth/passkey/register/verify") { setBody(body) }
    }

    override suspend fun getPasskeyLoginOptions(body: PasskeyLoginOptionsRequest): PasskeyLoginOptionsResponse =
        postJson("auth/passkey/login/options") { setBody(body) }.decode()

    override suspend fun verifyPasskeyLogin(body: PasskeyLoginVerifyRequest): LoginResponse =
        postJson("auth/passkey/login/verify") { setBody(body) }.decode()

    override suspend fun deletePasskeyCredential(id: String) {
        delete("auth/passkey/credentials/$id")
    }

    // ==================== OAuth 第三方登录 ====================

    override suspend fun getOAuthWxSession(body: OAuthSessionRequest): OAuthSessionResponse =
        postJson("auth/szczk/wx/login-session") { setBody(body) }.decode()

    override suspend fun getOAuthQrStatus(state: String): OAuthQrStatusResponse =
        get("auth/szczk/qr/$state/status").decode()

    override suspend fun completeOAuthLogin(state: String) {
        post("auth/szczk/qr/$state/complete")
    }

    // ==================== §6 分享 ====================

    override suspend fun createShare(body: ShareCreateRequest): ShareCreateResponse =
        postJson("share") { setBody(body) }.decode()

    override suspend fun closeShare(fileId: String) {
        delete("share/file/$fileId")
    }

    override suspend fun getShareInfo(shareId: String): ShareInfoResponse =
        get("share/$shareId").decode()

    override suspend fun getShareFiles(shareId: String, body: ShareFilesRequest): List<FileNodeDto> =
        postJson("share/$shareId/files") { setBody(body) }.decode()

    // ==================== §7 直链 ====================

    override suspend fun generateDirectLink(body: DirectLinkCreateRequest): DirectLinkCreateResponse =
        postJson("generate-direct-link") { setBody(body) }.decode()

    // ==================== §8 套餐与订单 ====================

    override suspend fun getPlans(): List<PlanDto> = get("plans").decode()

    override suspend fun getMyPlan(): MyPlanResponse = get("my-plan").decode()

    override suspend fun getOrders(): OrdersResponse = get("orders").decode()

    override suspend fun getOrderDetail(tradeNo: String): OrderDetailResponse =
        get("orders/$tradeNo").decode()

    override suspend fun createPayment(body: PayCreateRequest): PayCreateResponse =
        postJson("pay/create") { setBody(body) }.decode()

    override suspend fun repayOrder(body: PayRepayRequest): PayCreateResponse =
        postJson("pay/repay") { setBody(body) }.decode()

    override suspend fun queryPayment(body: PayQueryRequest): PayQueryResponse =
        postJson("pay/query") { setBody(body) }.decode()

    override suspend fun cancelOrder(body: PayCancelRequest) {
        postJson("pay/cancel") { setBody(body) }
    }

    // ==================== 内部工具 ====================

    private fun url(path: String): String = apiBaseUrl + path.trimStart('/')

    /**
     * 发送请求并统一做错误映射；返回的响应体尚未消费。
     *
     * 这里不开启 `expectSuccess`，所有非 2xx 都由 [ensureSuccess] 处理，
     * 以便读取后端 `{code,message}` 后再抛 [EdgeError]。
     */
    private suspend fun send(
        method: HttpMethod,
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val response = client.request(url(path)) {
            this.method = method
            configure()
        }
        response.ensureSuccess(path)
        return response
    }

    private suspend fun get(
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = send(HttpMethod.Get, path, configure)

    private suspend fun post(
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = send(HttpMethod.Post, path, configure)

    /**
     * POST + JSON 请求体。显式设置 `Content-Type: application/json`，
     * Ktor 的 ContentNegotiation 依据该头选择 kotlinx.serialization 转换器；
     * 缺少它会退回原始对象 body，发送时报 "Fail to prepare request body"。
     */
    private suspend fun postJson(
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = send(HttpMethod.Post, path) {
        contentType(ContentType.Application.Json)
        configure()
    }

    /** PATCH + JSON 请求体，同 [postJson]。 */
    private suspend fun patchJson(
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = send(HttpMethod.Patch, path) {
        contentType(ContentType.Application.Json)
        configure()
    }

    private suspend fun delete(
        path: String,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = send(HttpMethod.Delete, path, configure)

    /** 成功响应按 ContentNegotiation 解码；仅用于 2xx（[ensureSuccess] 已先行校验）。 */
    private suspend inline fun <reified T> HttpResponse.decode(): T = body()

    /**
     * 非 2xx 响应 → [EdgeError] 映射，规则与 Android `ResponseInterceptor` 保持一致：
     *
     * | 条件 | 结果 |
     * | --- | --- |
     * | 401 + `AUTH_REQUIRED/AUTH_EXPIRED/AUTH_INVALID`（非 `/login`） | [EdgeError.AuthExpired] |
     * | 403 + `suspended_overquota` | [EdgeError.Suspended] |
     * | 403 + `REAL_NAME_REQUIRED` | [EdgeError.RealNameRequired] |
     * | 403 + `TRAFFIC_EXHAUSTED` | [EdgeError.TrafficExhausted] |
     * | 429 | [EdgeError.RateLimited]（读取 `Retry-After`） |
     * | 409 + `INSTANT_MISS` | [EdgeError.InstantMiss] |
     * | 其他 `code` 非空 | [EdgeError.Business] |
     * | 无 JSON `code` | [EdgeError.Network] |
     *
     * 与 Android 的差异：共享层没有 `AuthEventBus`/`AuthRepositoryHolder`，
     * 401 时不广播登出事件（由上层统一处理）。登录接口自身 401 不转 AuthExpired，
     * 避免登录失败被当成“登录已失效”。
     */
    private suspend fun HttpResponse.ensureSuccess(path: String) {
        if (status.isSuccess()) return

        // 先读体再解析：响应体只能消费一次。
        // 取消异常必须原样抛出，不能被 runCatching 吞成网络错误。
        val bodyText = try {
            bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            ""
        }
        val payload = runCatching { EdgeJson.parseToJsonElement(bodyText) as? JsonObject }.getOrNull()
        val code = payload?.stringOrNull("code")
        val serverMessage = payload?.stringOrNull("message")

        // 与 Retrofit 请求的 encodedPath 对齐：登录接口自身不触发 AuthExpired
        val requestPath = runCatching { call.request.url.encodedPath }.getOrDefault(path)
        val isLogin = requestPath.endsWith("/login")

        val error: EdgeError = when {
            status.value == 401 && code in AUTH_CODES && !isLogin ->
                EdgeError.AuthExpired(code ?: "AUTH_REQUIRED", "登录已失效")
            status.value == 403 && code == "suspended_overquota" ->
                EdgeError.Suspended("账号因套餐过期且容量超限已被封禁")
            status.value == 403 && code == "REAL_NAME_REQUIRED" ->
                EdgeError.RealNameRequired
            status.value == 403 && code == "TRAFFIC_EXHAUSTED" ->
                EdgeError.TrafficExhausted
            status.value == 429 ->
                EdgeError.RateLimited(headers["Retry-After"]?.toLongOrNull())
            status.value == 409 && code == "INSTANT_MISS" ->
                EdgeError.InstantMiss
            code != null ->
                EdgeError.Business(code, serverMessage ?: "请求失败：$code")
            else ->
                EdgeError.Network(
                    IllegalStateException("HTTP ${status.value}: ${status.description}"),
                )
        }
        throw error
    }

    private companion object {
        val AUTH_CODES = setOf("AUTH_REQUIRED", "AUTH_EXPIRED", "AUTH_INVALID")
    }
}

/** 从业务错误 JSON 中安全取字符串字段（字段缺失 / 非标量 / JSON null 均返回 null）。 */
private fun JsonObject.stringOrNull(key: String): String? =
    runCatching { get(key)?.jsonPrimitive?.contentOrNull }.getOrNull()?.takeIf { it.isNotEmpty() }
