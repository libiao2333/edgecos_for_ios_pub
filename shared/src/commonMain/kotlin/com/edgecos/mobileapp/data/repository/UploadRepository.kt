package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.UploadCompleteRequest
import com.edgecos.mobileapp.shared.dto.UploadInstantCompleteRequest
import com.edgecos.mobileapp.shared.dto.UploadTokenResponse
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge

/**
 * 上传 Repository：秒传判断、单文件上传完成回调、分片上传（后续阶段）。
 */
class UploadRepository(private val api: EdgeApi) {

    /**
     * 获取上传 token（秒传判断）。
     *
     * @param hash 格式 `sha256:<hex>:<size>`
     * @return INSTANT 命中则 [UploadTokenResponse.instant]=true；否则返回 PUT 直传 URL
     */
    suspend fun getUploadToken(
        filename: String,
        filesize: Long,
        path: String,
        hash: String,
        overwrite: Boolean = false,
    ): EdgeResult<UploadTokenResponse> = runCatchingEdge {
        api.getUploadToken(filename, filesize, path, hash, overwrite)
    }

    /** 秒传完成 */
    suspend fun instantComplete(
        filename: String,
        path: String,
        size: Long,
        hash: String,
        overwrite: Boolean = false,
    ): EdgeResult<Unit> = runCatchingEdge {
        api.uploadInstantComplete(UploadInstantCompleteRequest(filename, path, size, hash, overwrite))
        Unit
    }

    /** 单文件上传完成（PUT 直传 OSS 后回调） */
    suspend fun complete(
        key: String,
        filename: String,
        path: String,
        size: Long,
        hash: String,
        overwrite: Boolean = false,
    ): EdgeResult<Unit> = runCatchingEdge {
        api.uploadComplete(UploadCompleteRequest(key, filename, path, size, hash, overwrite))
        Unit
    }
}
