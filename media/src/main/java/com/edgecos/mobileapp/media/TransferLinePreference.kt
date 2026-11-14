package com.edgecos.mobileapp.media

import android.content.Context

/**
 * 文件传输线路（与网页端线路偏好完全一致）。
 *
 * 网页端 Drive.jsx `lineParams` / Preferences.jsx 中，线路有三种状态：
 * - [ASK]：「每次询问」——预览/下载前弹窗让用户选择（默认）
 * - [CDN]：「CDN 极速线路」——速度更快，计入流量（付费 CDN，服务端校验流量）
 * - [FREE]：「免费线路」——不计流量，单连接限速（腾讯 EdgeOne 免费 CDN）
 *
 * 注意：请求参数 `line` 只会取 `cdn` / `free` 两个值（与网页端一致），
 * [ASK] 不应出现在请求中；[requestParam] 对 [ASK] 回退为 `cdn`。
 */
enum class TransferLine(val value: String) {
    ASK("ask"),
    CDN("cdn"),
    FREE("free");

    /** 请求参数值：`line=cdn` 或 `line=free`（与网页端 `lineParams` 一致） */
    val requestParam: String
        get() = if (this == FREE) "free" else "cdn"

    companion object {
        /** 从存储值解析（未知/空值回退为「每次询问」，与网页端默认一致） */
        fun fromValue(value: String?): TransferLine =
            entries.firstOrNull { it.value == value } ?: ASK
    }
}

/**
 * 线路偏好存取（对齐网页端 Preferences.jsx / Drive.jsx 的 localStorage 偏好）。
 *
 * 网页端存储键：
 * - `edgecos_preview_line_preference_v1`（预览线路）
 * - `edgecos_download_line_preference_v1`（下载线路）
 *
 * 网页端语义：默认 `'ask'`（每次询问）；仅存 `cdn` / `free`，
 * 选择「每次询问」时移除存储键（removeItem）。本实现保持一致。
 */
object LinePreferenceManager {

    private const val PREFS_NAME = "edgecos_line_preferences"
    private const val KEY_PREVIEW_LINE = "edgecos_preview_line_preference_v1"
    private const val KEY_DOWNLOAD_LINE = "edgecos_download_line_preference_v1"

    /** 预览线路偏好（默认「每次询问」） */
    fun previewLine(context: Context): TransferLine =
        TransferLine.fromValue(prefs(context).getString(KEY_PREVIEW_LINE, null))

    /** 下载线路偏好（默认「每次询问」） */
    fun downloadLine(context: Context): TransferLine =
        TransferLine.fromValue(prefs(context).getString(KEY_DOWNLOAD_LINE, null))

    /** 保存预览线路偏好（ASK 时移除记录，与网页端 removeItem 行为一致） */
    fun setPreviewLine(context: Context, line: TransferLine) {
        prefs(context).edit().apply {
            if (line == TransferLine.ASK) remove(KEY_PREVIEW_LINE)
            else putString(KEY_PREVIEW_LINE, line.value)
        }.apply()
    }

    /** 保存下载线路偏好（ASK 时移除记录，与网页端 removeItem 行为一致） */
    fun setDownloadLine(context: Context, line: TransferLine) {
        prefs(context).edit().apply {
            if (line == TransferLine.ASK) remove(KEY_DOWNLOAD_LINE)
            else putString(KEY_DOWNLOAD_LINE, line.value)
        }.apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
