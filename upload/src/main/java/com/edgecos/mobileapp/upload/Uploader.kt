package com.edgecos.mobileapp.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.edgecos.mobileapp.data.remote.RawHttpClient
import com.edgecos.mobileapp.shared.dto.UploadTokenResponse
import com.edgecos.mobileapp.data.repository.UploadRepository
import com.edgecos.mobileapp.domain.error.EdgeError
import com.edgecos.mobileapp.domain.error.EdgeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.net.URLConnection

/**
 * 单文件上传器（≤100MB）。
 *
 * 流程：
 * 1. 客户端 SHA-256 整个文件，组装 hash = "sha256:<hex>:<size>"
 * 2. GET /upload/token → instant=true 走 /upload/instant-complete；否则 PUT 直传 OSS + /upload/complete
 * 3. INSTANT_MISS fallback：用 skipInstant=true 再请求一次（由 [EdgeError.InstantMiss] 触发）
 *
 * 对齐前端 [Drive.jsx] 的 `uploadOneEnhanced`。
 */
class Uploader(
    private val context: Context,
    private val uploadRepo: UploadRepository,
) {

    /**
     * 上传单个文件。
     *
     * @param fileUri 文件内容 Uri（由系统文件选择器返回）
     * @param filename 文件名
     * @param path 目标路径（EdgeCOS 内部 POSIX 风格，如 `/` 或 `/docs`）
     * @param onProgress 进度回调（0-100）
     */
    suspend fun upload(
        fileUri: Uri,
        filename: String,
        path: String,
        onProgress: (Int) -> Unit = {},
    ): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val size = runCatching {
            resolver.openFileDescriptor(fileUri, "r")?.use { it.statSize } ?: 0L
        }.getOrDefault(0L)
        if (size <= 0L) return@withContext EdgeResult.Failure(EdgeError.Business("INVALID_FILE", "无法读取文件"))

        // 1. 计算 SHA-256
        val hash = runCatching {
            resolver.openInputStream(fileUri).use { Sha256Calculator.calculate(it!!, size) }
        }.getOrElse {
            return@withContext EdgeResult.Failure(EdgeError.Unknown("SHA-256 计算失败", it))
        }

        // 2. 请求上传 token（带秒传判断）
        val tokenResult = uploadRepo.getUploadToken(filename, size, path, hash)
        val token = when (tokenResult) {
            is EdgeResult.Success -> tokenResult.data
            is EdgeResult.Failure -> return@withContext tokenResult
        }

        when {
            token.instant -> {
                // 秒传命中
                uploadRepo.instantComplete(filename, path, size, hash)
            }
            else -> {
                // PUT 直传 OSS + complete
                val putResult = putToOss(token, fileUri, filename, size, onProgress)
                if (putResult is EdgeResult.Failure) return@withContext putResult

                val key = token.key ?: return@withContext EdgeResult.Failure(
                    EdgeError.Business("UPLOAD_KEY_MISSING", "OSS key 缺失"),
                )
                uploadRepo.complete(key, filename, path, size, hash)
            }
        }
    }

    /** PUT 直传 OSS（不带 Cookie，用 [RawHttpClient]） */
    private suspend fun putToOss(
        token: UploadTokenResponse,
        fileUri: Uri,
        filename: String,
        fileSize: Long,
        onProgress: (Int) -> Unit,
    ): EdgeResult<Unit> = withContext(Dispatchers.IO) {
        val url = token.url ?: return@withContext EdgeResult.Failure(
            EdgeError.Business("UPLOAD_URL_MISSING", "OSS URL 缺失"),
        )
        val contentType = URLConnection.guessContentTypeFromName(filename) ?: "application/octet-stream"

        try {
            val body = InputStreamRequestBody(
                contentType = contentType,
                contentResolver = context.contentResolver,
                uri = fileUri,
                fileSize = fileSize,
                onProgress = onProgress,
            )
            val request = okhttp3.Request.Builder().url(url).put(body).build()
            RawHttpClient.get().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext EdgeResult.Failure(
                        EdgeError.Network(IOException("OSS PUT 失败: ${response.code}")),
                    )
                }
            }
            onProgress(100)
            EdgeResult.Success(Unit)
        } catch (e: Exception) {
            EdgeResult.Failure(EdgeError.Network(e))
        }
    }
}

/**
 * 流式 RequestBody：包装 ContentResolver 的 InputStream，避免大文件 OOM。
 * 在 OkHttp 调用 [writeTo] 时分块读取并写入，同时回调进度。
 */
private class InputStreamRequestBody(
    private val contentType: String,
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val fileSize: Long,
    private val onProgress: (Int) -> Unit,
) : RequestBody() {

    override fun contentType(): MediaType? = contentType.toMediaTypeOrNull()

    override fun contentLength(): Long = fileSize

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri)
            ?: throw IOException("无法打开输入流: $uri")
        val buffer = ByteArray(64 * 1024)
        var totalWritten = 0L
        // 进度节流：仅百分比变化时回调，避免每个 64KB 分块都触发
        // 通知栏刷新 + 事件广播（100MB 文件约 1600 次 → 最多 101 次）
        var lastPercent = -1
        input.use { stream ->
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                totalWritten += read
                if (fileSize > 0) {
                    val percent = (totalWritten * 100 / fileSize).toInt()
                    if (percent != lastPercent) {
                        lastPercent = percent
                        onProgress(percent)
                    }
                }
            }
        }
    }
}
