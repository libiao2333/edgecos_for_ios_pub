package com.edgecos.mobileapp.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 更新检查结果。
 *
 * - [UpdateAvailable] 有新版本：远程 version code 大于本地 code
 * - [UpToDate] 已是最新：远程 code 等于或小于本地 code
 * - [CheckFailed] 检查失败（网络错误、JSON 解析错误等）
 */
sealed class UpdateResult {
    data class UpdateAvailable(
        val remoteVersion: String,
        val remoteCode: Int,
        val currentCode: Int,
        val downloadUrl: String,
        val description: String,
    ) : UpdateResult()

    data object UpToDate : UpdateResult()
    data class CheckFailed(val message: String) : UpdateResult()
}

/**
 * 应用更新检查器。
 *
 * 调用生产环境 API 获取最新版本信息，比较 [version]（versionCode）
 * 判断是否有新版本。[display] 为展示用版本名。
 *
 * 接口：GET https://user.edgecos.com/api/website/v1/client/version/android/aarch64
 * 返回字段：arch, createTime, description, display, hash, id, platform,
 *           releaseTime, status, url, version
 *
 * 使用独立的 OkHttpClient（不携带 Cookie，避免鉴权干扰）。
 * JSON 解析用 org.json.JSONObject 手动解析，避免 Moshi codegen 依赖问题。
 * 检查逻辑应在后台线程执行（[checkForUpdate] 是 suspend 函数）。
 *
 * 更新包地址取接口 url 字段，由客户端下载后自动拉起系统安装程序。
 */
object UpdateChecker {

    /** 生产环境版本检查 API（GET，无需鉴权） */
    private const val VERSION_URL =
        "https://user.edgecos.com/api/website/v1/client/version/android/aarch64"

    /** 请求超时（秒），避免弱网下卡死 */
    private const val TIMEOUT_SECONDS = 10L

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 检查更新。
     *
     * @param currentCode 当前应用的版本号（BuildConfig.VERSION_CODE）
     * @return [UpdateResult] 检查结果
     */
    suspend fun checkForUpdate(currentCode: Int): UpdateResult = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(VERSION_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@runCatching UpdateResult.CheckFailed("HTTP ${response.code}")
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@runCatching UpdateResult.CheckFailed("响应为空")
                }
                // 手动解析 JSON，避免依赖 Moshi codegen
                // 生产 API：version = versionCode（int），display = 版本名（string）
                val json = JSONObject(body)
                val remoteCode = json.optInt("version", 0)
                val remoteVersion = json.optString("display", "")
                val downloadUrl = json.optString("url", "")
                val description = json.optString("description", "")
                when {
                    remoteCode > currentCode -> UpdateResult.UpdateAvailable(
                        remoteVersion = remoteVersion,
                        remoteCode = remoteCode,
                        currentCode = currentCode,
                        downloadUrl = downloadUrl,
                        description = description,
                    )
                    else -> UpdateResult.UpToDate
                }
            }
        }.getOrElse { e ->
            UpdateResult.CheckFailed(e.message ?: "未知错误")
        }
    }
}
