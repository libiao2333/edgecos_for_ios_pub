package com.edgecos.mobileapp.compose

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual class DownloadSink actual constructor(fileName: String) {
    private val path: String = run {
        val dir = (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String) ?: NSTemporaryDirectory()
        "$dir/$fileName"
    }

    private var handle: NSFileHandle? = run {
        NSFileManager.defaultManager.createFileAtPath(path, contents = null, attributes = null)
        NSFileHandle.fileHandleForWritingAtPath(path)
    }

    actual fun write(buffer: ByteArray, length: Int) {
        val fileHandle = handle ?: return
        buffer.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = length.toULong())
            fileHandle.writeData(data)
        }
    }

    actual fun finish(): String {
        handle?.closeFile()
        handle = null
        return path
    }

    actual fun abort() {
        handle?.closeFile()
        handle = null
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
    }
}

/** 弹出系统分享面板：用户可存到「文件」、AirDrop 或转发。 */
actual fun shareLocalFile(path: String) {
    val url = NSURL.fileURLWithPath(path)
    val controller = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var top = root
    while (true) {
        top = top.presentedViewController ?: break
    }
    top.presentViewController(controller, animated = true, completion = null)
}
