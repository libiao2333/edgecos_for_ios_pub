package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.DirectLinkCreateRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.DirectLink

/**
 * 直链 Repository：生成直链。
 *
 * 前置条件：实名认证 + 流量未耗尽（后端 requireRealName + getTrafficState 中间件）。
 * - 未实名返回 403 REAL_NAME_REQUIRED → [com.edgecos.mobileapp.domain.error.EdgeError.RealNameRequired]
 * - 流量耗尽返回 403 TRAFFIC_EXHAUSTED → [com.edgecos.mobileapp.domain.error.EdgeError.TrafficExhausted]
 * - 限流：5min/10 次/userId+fileId
 */
class DirectLinkRepository(private val api: EdgeApi) {

    /**
     * 生成直链。
     *
     * @param fileId   文件 ID（与 filePath 二选一）
     * @param filePath 文件完整路径（与 fileId 二选一）
     */
    suspend fun generateDirectLink(
        fileId: String? = null,
        filePath: String? = null,
    ): EdgeResult<DirectLink> = runCatchingEdge {
        val resp = api.generateDirectLink(DirectLinkCreateRequest(fileId, filePath))
        DirectLink(resp.linkCode, resp.directUrl)
    }
}
