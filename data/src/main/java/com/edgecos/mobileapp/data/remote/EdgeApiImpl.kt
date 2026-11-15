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
 * 把 Retrofit 生成的 [RetrofitEdgeApi] 适配为平台无关的 [EdgeApi]。
 *
 * 纯转发，不承载业务逻辑；后续 iOS 侧会提供基于 Ktor 的实现。
 */
class EdgeApiImpl(
    private val delegate: RetrofitEdgeApi,
) : EdgeApi {

    override suspend fun sendEmailCode(body: SendEmailCodeRequest) {
        delegate.sendEmailCode(body)
    }

    override suspend fun register(body: RegisterRequest): LoginResponse = delegate.register(body)

    override suspend fun login(body: LoginRequest): LoginResponse = delegate.login(body)

    override suspend fun captchaPrecheck(body: CaptchaPrecheckRequest): CaptchaPrecheckResponse =
        delegate.captchaPrecheck(body)

    override suspend fun getUserProfile(): LoginResponse = delegate.getUserProfile()

    override suspend fun getUserQuota(): QuotaResponse = delegate.getUserQuota()

    override suspend fun getFilesSummary(): FileSummaryDto = delegate.getFilesSummary()

    override suspend fun listFiles(path: String): List<FileNodeDto> = delegate.listFiles(path)

    override suspend fun checkUploadConflict(filename: String, path: String): UploadConflictResponse =
        delegate.checkUploadConflict(filename, path)

    override suspend fun getUploadToken(
        filename: String,
        filesize: Long,
        path: String,
        hash: String,
        overwrite: Boolean,
    ): UploadTokenResponse = delegate.getUploadToken(filename, filesize, path, hash, overwrite)

    override suspend fun uploadComplete(body: UploadCompleteRequest) {
        delegate.uploadComplete(body)
    }

    override suspend fun uploadInstantComplete(body: UploadInstantCompleteRequest) {
        delegate.uploadInstantComplete(body)
    }

    override suspend fun mkdir(body: MkdirRequest) {
        delegate.mkdir(body)
    }

    override suspend fun rename(body: RenameRequest) {
        delegate.rename(body)
    }

    override suspend fun move(body: MoveRequest) {
        delegate.move(body)
    }

    override suspend fun copy(body: CopyRequest) {
        delegate.copy(body)
    }

    override suspend fun delete(body: DeleteRequest) {
        delegate.delete(body)
    }

    override suspend fun batchMove(body: BatchMoveRequest) {
        delegate.batchMove(body)
    }

    override suspend fun batchCopy(body: BatchCopyRequest) {
        delegate.batchCopy(body)
    }

    override suspend fun batchDelete(body: BatchDeleteRequest) {
        delegate.batchDelete(body)
    }

    override suspend fun listTrash(): List<FileNodeDto> = delegate.listTrash()

    override suspend fun restoreTrash(body: BatchIdsRequest) {
        delegate.restoreTrash(body)
    }

    override suspend fun deleteTrash(body: BatchIdsRequest) {
        delegate.deleteTrash(body)
    }

    override suspend fun batchRestoreTrash(body: BatchIdsRequest) {
        delegate.batchRestoreTrash(body)
    }

    override suspend fun batchDeleteTrash(body: BatchIdsRequest) {
        delegate.batchDeleteTrash(body)
    }

    override suspend fun getAnnouncements(): AnnouncementsResponse = delegate.getAnnouncements()

    override suspend fun markAnnouncementRead(id: Long) {
        delegate.markAnnouncementRead(id)
    }

    override suspend fun submitWorkOrder(body: WorkOrderSubmitRequest): WorkOrderDto =
        delegate.submitWorkOrder(body)

    override suspend fun listWorkOrders(): List<WorkOrderDto> = delegate.listWorkOrders()

    override suspend fun getWorkOrderDetail(id: Long): WorkOrderDto = delegate.getWorkOrderDetail(id)

    override suspend fun replyWorkOrder(id: Long, body: WorkOrderReplyRequest): WorkOrderReplyDto =
        delegate.replyWorkOrder(id, body)

    override suspend fun updateWorkOrderStatus(id: Long, body: WorkOrderStatusRequest): WorkOrderDto =
        delegate.updateWorkOrderStatus(id, body)

    override suspend fun getPasskeyCredentials(): PasskeyCredentialsResponse = delegate.getPasskeyCredentials()

    override suspend fun getPasskeyRegisterOptions(body: Map<String, String>): PasskeyRegisterOptionsResponse =
        delegate.getPasskeyRegisterOptions(body)

    override suspend fun verifyPasskeyRegister(body: PasskeyRegisterVerifyRequest) {
        delegate.verifyPasskeyRegister(body)
    }

    override suspend fun getPasskeyLoginOptions(body: PasskeyLoginOptionsRequest): PasskeyLoginOptionsResponse =
        delegate.getPasskeyLoginOptions(body)

    override suspend fun verifyPasskeyLogin(body: PasskeyLoginVerifyRequest): LoginResponse =
        delegate.verifyPasskeyLogin(body)

    override suspend fun deletePasskeyCredential(id: String) {
        delegate.deletePasskeyCredential(id)
    }

    override suspend fun getOAuthWxSession(body: OAuthSessionRequest): OAuthSessionResponse =
        delegate.getOAuthWxSession(body)

    override suspend fun getOAuthQrStatus(state: String): OAuthQrStatusResponse =
        delegate.getOAuthQrStatus(state)

    override suspend fun completeOAuthLogin(state: String) {
        delegate.completeOAuthLogin(state)
    }

    override suspend fun createShare(body: ShareCreateRequest): ShareCreateResponse =
        delegate.createShare(body)

    override suspend fun closeShare(fileId: String) {
        delegate.closeShare(fileId)
    }

    override suspend fun getShareInfo(shareId: String): ShareInfoResponse = delegate.getShareInfo(shareId)

    override suspend fun getShareFiles(shareId: String, body: ShareFilesRequest): List<FileNodeDto> =
        delegate.getShareFiles(shareId, body)

    override suspend fun generateDirectLink(body: DirectLinkCreateRequest): DirectLinkCreateResponse =
        delegate.generateDirectLink(body)

    override suspend fun getPlans(): List<PlanDto> = delegate.getPlans()

    override suspend fun getMyPlan(): MyPlanResponse = delegate.getMyPlan()

    override suspend fun getOrders(): OrdersResponse = delegate.getOrders()

    override suspend fun getOrderDetail(tradeNo: String): OrderDetailResponse = delegate.getOrderDetail(tradeNo)

    override suspend fun createPayment(body: PayCreateRequest): PayCreateResponse = delegate.createPayment(body)

    override suspend fun repayOrder(body: PayRepayRequest): PayCreateResponse = delegate.repayOrder(body)

    override suspend fun queryPayment(body: PayQueryRequest): PayQueryResponse = delegate.queryPayment(body)

    override suspend fun cancelOrder(body: PayCancelRequest) {
        delegate.cancelOrder(body)
    }
}
