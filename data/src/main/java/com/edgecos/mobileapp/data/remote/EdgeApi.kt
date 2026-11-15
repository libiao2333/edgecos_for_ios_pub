package com.edgecos.mobileapp.data.remote

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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Android 侧 Retrofit 接口声明。
 *
 * 只负责 HTTP 映射，不直接对外暴露；由 [EdgeApiImpl] 适配为平台无关的
 * [EdgeApi] 供 Repository 使用。
 *
 * 注意：无响应体的接口统一返回 Unit。kotlinx.serialization 无法反序列化
 * `Map<String, Any?>`（Any 没有 serializer），返回 Unit 时 Retrofit 也不会解析响应体。
 */
interface RetrofitEdgeApi {

    // ==================== §2.1 注册 / 登录 / 邮箱验证码 ====================

    @POST("send-email-code")
    suspend fun sendEmailCode(@Body body: SendEmailCodeRequest)

    @POST("register")
    suspend fun register(@Body body: RegisterRequest): LoginResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    // ==================== §2.11 Captcha 预检 ====================

    @POST("captcha/precheck")
    suspend fun captchaPrecheck(@Body body: CaptchaPrecheckRequest): CaptchaPrecheckResponse

    // ==================== §2.4 用户中心 ====================

    @GET("user/profile")
    suspend fun getUserProfile(): LoginResponse

    @GET("user/quota")
    suspend fun getUserQuota(): QuotaResponse

    // ==================== §2.5 文件管理 ====================

    @GET("files/summary")
    suspend fun getFilesSummary(): FileSummaryDto

    @GET("files")
    suspend fun listFiles(@Query("path") path: String = "/"): List<FileNodeDto>

    @GET("upload/conflict")
    suspend fun checkUploadConflict(
        @Query("filename") filename: String,
        @Query("path") path: String,
    ): UploadConflictResponse

    @GET("upload/token")
    suspend fun getUploadToken(
        @Query("filename") filename: String,
        @Query("filesize") filesize: Long,
        @Query("path") path: String,
        @Query("hash") hash: String,
        @Query("overwrite") overwrite: Boolean = false,
    ): UploadTokenResponse

    @POST("upload/complete")
    suspend fun uploadComplete(@Body body: UploadCompleteRequest)

    @POST("upload/instant-complete")
    suspend fun uploadInstantComplete(@Body body: UploadInstantCompleteRequest)

    @POST("mkdir")
    suspend fun mkdir(@Body body: MkdirRequest)

    @POST("rename")
    suspend fun rename(@Body body: RenameRequest)

    @POST("move")
    suspend fun move(@Body body: MoveRequest)

    @POST("copy")
    suspend fun copy(@Body body: CopyRequest)

    @POST("delete")
    suspend fun delete(@Body body: DeleteRequest)

    @POST("batch/move")
    suspend fun batchMove(@Body body: BatchMoveRequest)

    @POST("batch/copy")
    suspend fun batchCopy(@Body body: BatchCopyRequest)

    @POST("batch/delete")
    suspend fun batchDelete(@Body body: BatchDeleteRequest)

    @GET("trash")
    suspend fun listTrash(): List<FileNodeDto>

    @POST("trash/restore")
    suspend fun restoreTrash(@Body body: BatchIdsRequest)

    @POST("trash/delete")
    suspend fun deleteTrash(@Body body: BatchIdsRequest)

    @POST("batch/trash/restore")
    suspend fun batchRestoreTrash(@Body body: BatchIdsRequest)

    @POST("batch/trash/delete")
    suspend fun batchDeleteTrash(@Body body: BatchIdsRequest)

    // ==================== §2.11 公告 ====================

    @GET("announcements")
    suspend fun getAnnouncements(): AnnouncementsResponse

    @POST("announcements/{id}/read")
    suspend fun markAnnouncementRead(@Path("id") id: Long)

    // ==================== 工单 ====================

    @POST("workorder")
    suspend fun submitWorkOrder(@Body body: WorkOrderSubmitRequest): WorkOrderDto

    @GET("workorder")
    suspend fun listWorkOrders(): List<WorkOrderDto>

    @GET("workorder/{id}")
    suspend fun getWorkOrderDetail(@Path("id") id: Long): WorkOrderDto

    @POST("workorder/{id}/reply")
    suspend fun replyWorkOrder(
        @Path("id") id: Long,
        @Body body: WorkOrderReplyRequest,
    ): WorkOrderReplyDto

    @PATCH("workorder/{id}/status")
    suspend fun updateWorkOrderStatus(
        @Path("id") id: Long,
        @Body body: WorkOrderStatusRequest,
    ): WorkOrderDto

    // ==================== Passkey ====================

    @GET("auth/passkey/credentials")
    suspend fun getPasskeyCredentials(): PasskeyCredentialsResponse

    @POST("auth/passkey/register/options")
    suspend fun getPasskeyRegisterOptions(@Body body: Map<String, String> = emptyMap()): PasskeyRegisterOptionsResponse

    @POST("auth/passkey/register/verify")
    suspend fun verifyPasskeyRegister(@Body body: PasskeyRegisterVerifyRequest)

    @POST("auth/passkey/login/options")
    suspend fun getPasskeyLoginOptions(@Body body: PasskeyLoginOptionsRequest): PasskeyLoginOptionsResponse

    @POST("auth/passkey/login/verify")
    suspend fun verifyPasskeyLogin(@Body body: PasskeyLoginVerifyRequest): LoginResponse

    @DELETE("auth/passkey/credentials/{id}")
    suspend fun deletePasskeyCredential(@Path("id") id: String)

    // ==================== OAuth 第三方登录 ====================

    @POST("auth/szczk/wx/login-session")
    suspend fun getOAuthWxSession(@Body body: OAuthSessionRequest): OAuthSessionResponse

    @GET("auth/szczk/qr/{state}/status")
    suspend fun getOAuthQrStatus(@Path("state") state: String): OAuthQrStatusResponse

    @POST("auth/szczk/qr/{state}/complete")
    suspend fun completeOAuthLogin(@Path("state") state: String)

    // ==================== 下载（302 重定向，由 OkHttp followRedirects 处理） ====================

    @GET("download")
    suspend fun download(
        @Query("path") path: String,
        @Query("id") id: String,
        @Query("line") line: String = "default",
    ): retrofit2.Response<Unit>

    // ==================== §6 分享 ====================

    @POST("share")
    suspend fun createShare(@Body body: ShareCreateRequest): ShareCreateResponse

    @DELETE("share/file/{fileId}")
    suspend fun closeShare(@Path("fileId") fileId: String)

    @GET("share/{shareId}")
    suspend fun getShareInfo(@Path("shareId") shareId: String): ShareInfoResponse

    @POST("share/{shareId}/files")
    suspend fun getShareFiles(
        @Path("shareId") shareId: String,
        @Body body: ShareFilesRequest,
    ): List<FileNodeDto>

    @GET("share/{shareId}/download")
    suspend fun downloadShareFile(
        @Path("shareId") shareId: String,
        @Query("code") code: String?,
        @Query("path") path: String,
    ): retrofit2.Response<Unit>

    // ==================== §7 直链 ====================

    @POST("generate-direct-link")
    suspend fun generateDirectLink(@Body body: DirectLinkCreateRequest): DirectLinkCreateResponse

    @GET("f/{linkCode}")
    suspend fun accessDirectLink(@Path("linkCode") linkCode: String): retrofit2.Response<Unit>

    // ==================== §8 套餐与订单 ====================

    @GET("plans")
    suspend fun getPlans(): List<PlanDto>

    @GET("my-plan")
    suspend fun getMyPlan(): MyPlanResponse

    @GET("orders")
    suspend fun getOrders(): OrdersResponse

    @GET("orders/{tradeNo}")
    suspend fun getOrderDetail(@Path("tradeNo") tradeNo: String): OrderDetailResponse

    @POST("pay/create")
    suspend fun createPayment(@Body body: PayCreateRequest): PayCreateResponse

    @POST("pay/repay")
    suspend fun repayOrder(@Body body: PayRepayRequest): PayCreateResponse

    @POST("pay/query")
    suspend fun queryPayment(@Body body: PayQueryRequest): PayQueryResponse

    @POST("pay/cancel")
    suspend fun cancelOrder(@Body body: PayCancelRequest)
}
