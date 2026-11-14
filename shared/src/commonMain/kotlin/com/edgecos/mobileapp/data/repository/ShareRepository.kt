package com.edgecos.mobileapp.data.repository

import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.shared.dto.FileNodeDto
import com.edgecos.mobileapp.shared.dto.ShareCreateRequest
import com.edgecos.mobileapp.shared.dto.ShareFilesRequest
import com.edgecos.mobileapp.domain.error.EdgeResult
import com.edgecos.mobileapp.domain.error.runCatchingEdge
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.model.ShareCreateResult
import com.edgecos.mobileapp.domain.model.ShareInfo

/**
 * 分享 Repository：创建分享、关闭分享、查询分享信息、列出分享内文件。
 *
 * 前置条件：创建分享需实名认证（后端 requireRealName 中间件），
 * 未认证返回 403 REAL_NAME_REQUIRED，由 [EdgeError.RealNameRequired] 表达。
 */
class ShareRepository(private val api: EdgeApi, private val webBaseUrl: String) {

    /**
     * 创建分享。
     *
     * @param fileId      文件 ID（与 filePath 二选一）
     * @param filePath    文件完整路径（与 fileId 二选一）
     * @param fileName    文件名（必填）
     * @param expirationType 过期类型："1d" / "7d" / "30d" / "permanent" / "custom"
     * @param customExpiration 自定义过期时间（ISO 8601，仅 expirationType=custom 时必填）
     * @param extractionCode 提取码（4-8 位字母数字，可选）
     */
    suspend fun createShare(
        fileId: String? = null,
        filePath: String? = null,
        fileName: String,
        expirationType: String = "7d",
        customExpiration: String? = null,
        extractionCode: String? = null,
    ): EdgeResult<ShareCreateResult> = runCatchingEdge {
        val resp = api.createShare(
            ShareCreateRequest(fileId, filePath, fileName, expirationType, customExpiration, extractionCode),
        )
        // 拼接分享链接：Web 端基地址 + /s/{shareId}
        val base = webBaseUrl.trimEnd('/').removeSuffix("/api")
        val shareUrl = "$base/s/${resp.shareId}"
        ShareCreateResult(
            shareId = resp.shareId,
            reused = resp.reused,
            shareUrl = shareUrl,
            extractionCode = resp.activeShare?.extractionCode,
            expiration = resp.activeShare?.expiration,
        )
    }

    /** 关闭文件的分享（DELETE /share/file/:fileId） */
    suspend fun closeShare(fileId: String): EdgeResult<Unit> = runCatchingEdge {
        api.closeShare(fileId)
        Unit
    }

    /** 获取分享信息（无需登录） */
    suspend fun getShareInfo(shareId: String): EdgeResult<ShareInfo> = runCatchingEdge {
        val dto = api.getShareInfo(shareId)
        ShareInfo(
            fileName = dto.fileName,
            owner = dto.owner,
            createdAt = dto.createdAt,
            expiration = dto.expiration,
            needExtractionCode = dto.needExtractionCode,
            isFolder = dto.isFolder,
        )
    }

    /** 获取分享内文件列表（无需登录，文件夹分享用） */
    suspend fun getShareFiles(
        shareId: String,
        path: String = "/",
        code: String? = null,
    ): EdgeResult<List<FileNode>> = runCatchingEdge {
        api.getShareFiles(shareId, ShareFilesRequest(path, code)).map { it.toDomain() }
    }
}

private fun FileNodeDto.toDomain(): FileNode = FileNode(
    id = id,
    filename = filename,
    basename = basename,
    lastmod = lastmod,
    size = size.toLongOrNull() ?: 0L,
    type = if (type == "directory") FileType.DIRECTORY else FileType.FILE,
    thumbnail = thumbnail,
    hasThumbnail = hasThumbnail,
    activeShare = activeShare?.let {
        com.edgecos.mobileapp.domain.model.ActiveShare(
            shareId = it.shareId,
            expiration = it.expiration,
            extractionCode = it.extractionCode,
        )
    },
)
