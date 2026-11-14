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

/**
 * EdgeCOS 后端 API 的平台无关声明。
 *
 * 只描述「调用什么、传什么、返回什么」，不含 Retrofit/Ktor 等框架注解；
 * Android 侧由 `RetrofitEdgeApi` + `EdgeApiImpl` 实现，iOS 侧后续由 Ktor 实现。
 *
 * BASE_URL = `https://user.edgecos.com/api`
 * 鉴权：Cookie（httpOnly JWT）。
 */
interface EdgeApi {

    // ==================== §2.1 注册 / 登录 / 邮箱验证码 ====================

    suspend fun sendEmailCode(body: SendEmailCodeRequest)

    suspend fun register(body: RegisterRequest): LoginResponse

    suspend fun login(body: LoginRequest): LoginResponse

    // ==================== §2.11 Captcha 预检 ====================

    suspend fun captchaPrecheck(body: CaptchaPrecheckRequest): CaptchaPrecheckResponse

    // ==================== §2.4 用户中心 ====================

    suspend fun getUserProfile(): LoginResponse

    suspend fun getUserQuota(): QuotaResponse

    // ==================== §2.5 文件管理 ====================

    suspend fun getFilesSummary(): FileSummaryDto

    suspend fun listFiles(path: String = "/"): List<FileNodeDto>

    suspend fun checkUploadConflict(filename: String, path: String): UploadConflictResponse

    suspend fun getUploadToken(
        filename: String,
        filesize: Long,
        path: String,
        hash: String,
        overwrite: Boolean = false,
    ): UploadTokenResponse

    suspend fun uploadComplete(body: UploadCompleteRequest)

    suspend fun uploadInstantComplete(body: UploadInstantCompleteRequest)

    suspend fun mkdir(body: MkdirRequest)

    suspend fun rename(body: RenameRequest)

    suspend fun move(body: MoveRequest)

    suspend fun copy(body: CopyRequest)

    suspend fun delete(body: DeleteRequest)

    suspend fun batchMove(body: BatchMoveRequest)

    suspend fun batchCopy(body: BatchCopyRequest)

    suspend fun batchDelete(body: BatchDeleteRequest)

    suspend fun listTrash(): List<FileNodeDto>

    suspend fun restoreTrash(body: BatchIdsRequest)

    suspend fun deleteTrash(body: BatchIdsRequest)

    suspend fun batchRestoreTrash(body: BatchIdsRequest)

    suspend fun batchDeleteTrash(body: BatchIdsRequest)

    // ==================== §2.11 公告 ====================

    suspend fun getAnnouncements(): AnnouncementsResponse

    suspend fun markAnnouncementRead(id: Long)

    // ==================== 工单 ====================

    suspend fun submitWorkOrder(body: WorkOrderSubmitRequest): WorkOrderDto

    suspend fun listWorkOrders(): List<WorkOrderDto>

    suspend fun getWorkOrderDetail(id: Long): WorkOrderDto

    suspend fun replyWorkOrder(id: Long, body: WorkOrderReplyRequest): WorkOrderReplyDto

    suspend fun updateWorkOrderStatus(id: Long, body: WorkOrderStatusRequest): WorkOrderDto

    // ==================== Passkey ====================

    suspend fun getPasskeyCredentials(): PasskeyCredentialsResponse

    suspend fun getPasskeyRegisterOptions(body: Map<String, String> = emptyMap()): PasskeyRegisterOptionsResponse

    suspend fun verifyPasskeyRegister(body: PasskeyRegisterVerifyRequest)

    suspend fun getPasskeyLoginOptions(body: PasskeyLoginOptionsRequest): PasskeyLoginOptionsResponse

    suspend fun verifyPasskeyLogin(body: PasskeyLoginVerifyRequest): LoginResponse

    suspend fun deletePasskeyCredential(id: String)

    // ==================== OAuth 第三方登录 ====================

    suspend fun getOAuthWxSession(body: OAuthSessionRequest): OAuthSessionResponse

    suspend fun getOAuthQrStatus(state: String): OAuthQrStatusResponse

    suspend fun completeOAuthLogin(state: String)

    // ==================== §6 分享 ====================

    suspend fun createShare(body: ShareCreateRequest): ShareCreateResponse

    suspend fun closeShare(fileId: String)

    suspend fun getShareInfo(shareId: String): ShareInfoResponse

    suspend fun getShareFiles(shareId: String, body: ShareFilesRequest): List<FileNodeDto>

    // ==================== §7 直链 ====================

    suspend fun generateDirectLink(body: DirectLinkCreateRequest): DirectLinkCreateResponse

    // ==================== §8 套餐与订单 ====================

    suspend fun getPlans(): List<PlanDto>

    suspend fun getMyPlan(): MyPlanResponse

    suspend fun getOrders(): OrdersResponse

    suspend fun getOrderDetail(tradeNo: String): OrderDetailResponse

    suspend fun createPayment(body: PayCreateRequest): PayCreateResponse

    suspend fun repayOrder(body: PayRepayRequest): PayCreateResponse

    suspend fun queryPayment(body: PayQueryRequest): PayQueryResponse

    suspend fun cancelOrder(body: PayCancelRequest)
}
