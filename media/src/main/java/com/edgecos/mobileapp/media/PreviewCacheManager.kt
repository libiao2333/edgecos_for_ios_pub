package com.edgecos.mobileapp.media

import android.content.Context
import com.edgecos.mobileapp.data.remote.ClientConfig
import com.edgecos.mobileapp.data.remote.EdgeHttpClient
import com.edgecos.mobileapp.domain.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.security.MessageDigest

/**
 * 文件预览缓存管理器。
 *
 * 职责：
 * 1. 缓存预览下载的文件（按文件 id 命名，避免重复下载消耗流量）
 * 2. 记录"已对用户展示过流量提醒"的文件 id（首次打开才提醒）
 * 3. LRU 清理：缓存总大小超过 [MAX_CACHE_SIZE] 时按最后访问时间淘汰最旧文件
 *
 * 缓存目录：app 内部缓存 `preview_cache/`（系统可在存储空间不足时自动清理）
 */
object PreviewCacheManager {

    /** 缓存目录名 */
    private const val DIR_NAME = "preview_cache"

    /** 缓存大小上限：200MB */
    private const val MAX_CACHE_SIZE = 200L * 1024 * 1024

    /** 记录已提醒过的文件 id 的 SharedPreferences 文件名 */
    private const val PREFS_NAME = "preview_traffic_reminder"

    /** SharedPreferences 中存储已提醒文件 id 集合的 key */
    private const val KEY_REMINDED_IDS = "reminded_file_ids"

    /** 缓存目录（懒加载） */
    private fun cacheDir(context: Context): File {
        return File(context.cacheDir, DIR_NAME).also { if (!it.exists()) it.mkdirs() }
    }

    /**
     * 生成缓存文件。文件名规则：{文件id}_{文件名}，确保唯一且可读。
     */
    private fun cacheFile(context: Context, file: FileNode): File {
        // 对 id 做一次 hash，避免特殊字符
        val idHash = MessageDigest.getInstance("MD5")
            .digest(file.id.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
        val safeName = file.basename.replace(Regex("[^a-zA-Z0-9._\\-一-龥]"), "_")
        return File(cacheDir(context), "${idHash}_$safeName")
    }

    /**
     * 检查文件是否已缓存。
     */
    fun isCached(context: Context, file: FileNode): Boolean {
        return cacheFile(context, file).let { it.exists() && it.length() > 0 }
    }

    /**
     * 获取缓存文件（如果存在）。同时更新最后访问时间（用于 LRU）。
     */
    fun getCachedFile(context: Context, file: FileNode): File? {
        val f = cacheFile(context, file)
        return if (f.exists() && f.length() > 0) {
            f.setLastModified(System.currentTimeMillis())
            f
        } else null
    }

    /**
     * 下载文件并缓存。返回缓存文件，失败返回 null。
     * 如果缓存已存在则直接返回，不重复下载。
     *
     * @param url 下载地址（由调用方按用户选择的线路构建）：
     *            - 预览下载传入 [buildPreviewUrl]（line=cdn/free，302 → CDN）；
     *            - 普通下载传入 [buildDownloadUrl]（line=cdn/free，携带 Cookie）；
     *            - PDF 预览传入 /preview/pdf-url 返回的签名 CDN 直链（无需 Cookie）。
     */
    suspend fun downloadAndCache(
        context: Context,
        file: FileNode,
        url: String,
    ): File? {
        // 缓存命中
        getCachedFile(context, file)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                EdgeHttpClient.get().newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val target = cacheFile(context, file)
                    response.body?.byteStream()?.use { input ->
                        java.io.FileOutputStream(target).use { fos ->
                            input.copyTo(fos)
                        }
                    } ?: return@withContext null
                    // 下载完成后触发 LRU 清理
                    cleanIfNeeded(context)
                    target
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * 将文本内容写入缓存文件（用于 /file/content 返回的文本类预览，如 Markdown）。
     * 返回缓存文件，失败返回 null。
     */
    suspend fun writeTextToCache(context: Context, file: FileNode, content: String): File? =
        withContext(Dispatchers.IO) {
            try {
                val target = cacheFile(context, file)
                target.writeText(content, Charsets.UTF_8)
                cleanIfNeeded(context)
                target
            } catch (_: Exception) {
                null
            }
        }

    /**
     * 如果缓存总大小超过上限，按 LRU（最久未访问）淘汰旧文件。
     */
    private fun cleanIfNeeded(context: Context) {
        try {
            val dir = cacheDir(context)
            val files = dir.listFiles()?.toMutableList() ?: return
            var totalSize = files.sumOf { it.length() }
            if (totalSize <= MAX_CACHE_SIZE) return
            // 按 lastModified 升序（最旧的在前）
            files.sortBy { it.lastModified() }
            val it = files.iterator()
            while (it.hasNext() && totalSize > MAX_CACHE_SIZE) {
                val f = it.next()
                val size = f.length()
                if (f.delete()) {
                    totalSize -= size
                }
            }
        } catch (_: Exception) {
        }
    }

    // ============================================================
    // 流量提醒记录（SharedPreferences）
    // ============================================================

    /**
     * 检查是否已对该文件展示过流量提醒。
     * @return true 表示已提醒过（无需再次提醒）
     */
    fun isReminderShown(context: Context, file: FileNode): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_REMINDED_IDS, emptySet()) ?: emptySet()
        return file.id in ids
    }

    /**
     * 标记该文件已展示过流量提醒。
     */
    fun markReminderShown(context: Context, file: FileNode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = prefs.getStringSet(KEY_REMINDED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        ids.add(file.id)
        prefs.edit().putStringSet(KEY_REMINDED_IDS, ids).apply()
    }

    /**
     * 清除所有缓存文件（可在设置页提供"清除预览缓存"功能）。
     */
    fun clearAllCache(context: Context) {
        try {
            cacheDir(context).deleteRecursively()
            cacheDir(context).mkdirs()
        } catch (_: Exception) {
        }
    }

    /**
     * 获取当前缓存总大小（用于设置页展示）。
     */
    fun getCacheSize(context: Context): Long {
        return try {
            cacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
