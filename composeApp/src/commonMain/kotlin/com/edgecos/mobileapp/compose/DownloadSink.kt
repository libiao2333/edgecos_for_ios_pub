package com.edgecos.mobileapp.compose

/**
 * 下载落盘目标：把分块数据写入应用沙盒里的文件。
 *
 * iOS 落到 Documents（可通过系统分享导出到「文件」App），桌面落到用户 Downloads 目录。
 * 之所以做成流式而不是把整个文件读进内存，是为了支持大文件下载（几百 MB 不会 OOM）。
 */
expect class DownloadSink(fileName: String) {
    /** 追加写入一块数据，[length] 为有效字节数。 */
    fun write(buffer: ByteArray, length: Int)

    /** 关闭句柄并返回本地文件路径。 */
    fun finish(): String

    /** 放弃本次下载并删除半成品。 */
    fun abort()
}

/** 下载完成后交给系统处理：iOS 弹分享面板，桌面打开文件所在目录。 */
expect fun shareLocalFile(path: String)
