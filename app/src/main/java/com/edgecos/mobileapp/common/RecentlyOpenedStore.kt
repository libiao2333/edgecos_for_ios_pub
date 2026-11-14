package com.edgecos.mobileapp.common

import android.content.Context
import android.content.SharedPreferences
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import org.json.JSONArray
import org.json.JSONObject

/**
 * 「最近打开」记录：持久化用户在 app 内打开（预览）过的文件。
 *
 * 作为首页「最近文件」的第一优先级数据源：
 * ① app 内最近 3 天打开的文件 → ② 修改时间最新的 3 个文件 → ③ 最近无文件
 */
object RecentlyOpenedStore {

    private const val PREFS_NAME = "recently_opened_files"
    private const val KEY_ENTRIES = "entries"

    /** 最多保留的打开记录条数（首页最多展示 10 条） */
    private const val MAX_ENTRIES = 30

    /** 第一优先级的时间窗口：3 天 */
    private const val WINDOW_MILLIS = 3L * 24 * 60 * 60 * 1000

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 记录一次文件打开。
     *
     * 按文件 id 去重（同一文件重复打开只更新打开时间并置顶），目录不记录。
     */
    fun record(context: Context, file: FileNode) {
        if (file.type != FileType.FILE || file.id.isEmpty()) return
        val now = System.currentTimeMillis()
        val entries = readEntries(context)
            .filter { it.openedAt >= now - WINDOW_MILLIS && it.id != file.id }
            .toMutableList()
        entries.add(
            0,
            Entry(
                id = file.id,
                filename = file.filename,
                basename = file.basename,
                size = file.size.toString(),
                openedAt = now,
            ),
        )
        writeEntries(context, entries.take(MAX_ENTRIES))
    }

    /**
     * 读取 app 内最近 3 天打开过的文件（按打开时间倒序，最多 10 条）。
     *
     * 注意：会读 SharedPreferences（磁盘 IO），调用方应在 IO 线程执行。
     */
    fun recentWithin3Days(context: Context): List<FileNode> {
        val cutoff = System.currentTimeMillis() - WINDOW_MILLIS
        return readEntries(context)
            .filter { it.openedAt >= cutoff }
            .sortedByDescending { it.openedAt }
            .take(10)
            .map { it.toFileNode() }
    }

    private fun readEntries(context: Context): List<Entry> {
        val raw = prefs(context).getString(KEY_ENTRIES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id")
                    if (id.isEmpty()) continue
                    add(
                        Entry(
                            id = id,
                            filename = obj.optString("filename"),
                            basename = obj.optString("basename"),
                            size = obj.optString("size", "0"),
                            openedAt = obj.optLong("openedAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeEntries(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject().apply {
                    put("id", entry.id)
                    put("filename", entry.filename)
                    put("basename", entry.basename)
                    put("size", entry.size)
                    put("openedAt", entry.openedAt)
                },
            )
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply()
    }

    private data class Entry(
        val id: String,
        val filename: String,
        val basename: String,
        val size: String,
        val openedAt: Long,
    ) {
        fun toFileNode(): FileNode = FileNode(
            id = id,
            filename = filename,
            basename = basename,
            // 打开记录不保存修改时间，首页也不展示
            lastmod = "",
            size = size.toLongOrNull() ?: 0L,
            type = FileType.FILE,
        )
    }
}
