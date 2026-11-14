package com.edgecos.mobileapp.upload

import android.content.Context
import android.os.Build
import android.os.Environment
import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import com.edgecos.mobileapp.domain.error.EdgeError
import com.edgecos.mobileapp.domain.error.EdgeResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 文件下载器。
 *
 * - GET /download?path=&id=&line=，OkHttp followRedirects(true) 拿 302 后的 CDN URL
 * - 流式落盘到公共下载目录的 EdgeCOS 子文件夹
 * - 断点续传：HTTP Range 头（CDN 已支持）
 *
 * 对齐 TODO §3.6.7。
 */
class Downloader(private val context: Context) {

    /** 公共下载目录下的 EdgeCOS 子文件夹 */
    private val downloadDir: File
        get() {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "EdgeCOS",
            )
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * 下载文件到公共 Download/EdgeCOS/ 目录。
     *
     * @param downloadUrl 由后端 GET /download 返回的 URL（会 302 重定向到 CDN）
     * @param filename 落盘文件名
     * @param overwrite 覆盖模式：先删除同名旧文件再下载。更新包场景使用
     *   （重复下载时避免对已存在文件发起 Range 续传——服务器会返回 416，
     *   或续传出与当前版本不匹配的损坏包）
     * @param onProgress 进度回调（0-100）
     * @return 下载完成的本地文件
     */
    suspend fun download(
        downloadUrl: String,
        filename: String,
        overwrite: Boolean = false,
        onProgress: (Int) -> Unit = {},
    ): EdgeResult<File> = withContext(Dispatchers.IO) {
        // Android 11+ 无“管理所有文件”权限时写入公共目录必失败，提前给出可操作的清晰提示
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            return@withContext EdgeResult.Failure(
                EdgeError.Business(
                    "STORAGE_PERMISSION_REQUIRED",
                    "缺少存储权限，请在系统设置中开启“管理所有文件”后重试",
                ),
            )
        }

        val outputFile = File(downloadDir, filename)
        if (overwrite && outputFile.exists()) outputFile.delete()
        val existingSize = if (outputFile.exists()) outputFile.length() else 0L

        val requestBuilder = Request.Builder().url(downloadUrl)
        if (existingSize > 0) {
            requestBuilder.header("Range", "bytes=$existingSize-")
        }

        try {
            val response = EdgeHttpClient.get().newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                response.close()
                return@withContext EdgeResult.Failure(
                    EdgeError.Network(IOException("下载失败: ${response.code}")),
                )
            }

            val body = response.body ?: run {
                response.close()
                return@withContext EdgeResult.Failure(EdgeError.Network(IOException("响应体为空")))
            }

            // 正确处理续传：
            // - 206：服务器接受了 Range，从断点追加（append 模式）
            // - 200：服务器忽略了 Range（CDN 不支持/文件已变更），返回的是完整内容，
            //   必须截断重写，否则把完整内容追加到旧分片后会得到损坏文件
            val append = response.code == 206
            val startBytes = if (append) existingSize else 0L

            // contentLength 在 206 响应中是剩余部分大小，需加上已存在的字节数
            val contentLen = body.contentLength()
            val totalBytes = if (contentLen < 0) 0L else contentLen + startBytes

            // 进度节流：仅百分比变化时回调，避免每个 64KB 分块都触发通知/事件（100MB 文件约 1600 次 → 最多 101 次）
            var lastPercent = -1
            body.byteStream().use { input ->
                FileOutputStream(outputFile, append).use { fos ->
                    val tempBuffer = ByteArray(64 * 1024)
                    var bytesRead = startBytes
                    while (true) {
                        val read = input.read(tempBuffer)
                        if (read == -1) break
                        fos.write(tempBuffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            val percent = (bytesRead * 100 / totalBytes).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                    fos.flush()
                }
            }
            if (lastPercent != 100) onProgress(100)
            EdgeResult.Success(outputFile)
        } catch (e: CancellationException) {
            // 协程取消（页面销毁等）不是业务失败，继续向上传播，避免误报"下载失败"
            throw e
        } catch (e: Exception) {
            // 拦截器抛出的业务错误（鉴权失效/流量耗尽等）保留原类型，
            // 避免一律包装成网络错误丢失语义与 message
            EdgeResult.Failure(if (e is EdgeError) e else EdgeError.Network(e))
        }
    }
}
