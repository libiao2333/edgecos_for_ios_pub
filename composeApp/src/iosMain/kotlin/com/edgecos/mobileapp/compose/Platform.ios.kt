package com.edgecos.mobileapp.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeData
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.coroutines.resume

actual fun openExternalUrl(url: String) {
    val nsUrl = NSURL(string = url)
    UIApplication.sharedApplication.openURL(nsUrl)
}

/**
 * 文件选择器的 delegate 必须被强引用，否则会在系统回调前被 ARC 释放。
 * 同时只允许一个选择器处于活动状态。
 */
private var activePickerDelegate: NSObject? = null

actual suspend fun pickFileForUpload(): PickedFile? = suspendCancellableCoroutine { continuation ->
    val presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
    if (presenter == null) {
        continuation.resume(null)
        return@suspendCancellableCoroutine
    }

    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(
            controller: UIDocumentPickerViewController,
            didPickDocumentsAtURLs: List<*>,
        ) {
            activePickerDelegate = null
            val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            val data = url?.let { NSData.dataWithContentsOfURL(it) }
            if (url == null || data == null) {
                continuation.resume(null)
                return
            }
            continuation.resume(PickedFile(url.lastPathComponent ?: "file", data.toByteArray()))
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            activePickerDelegate = null
            continuation.resume(null)
        }
    }
    activePickerDelegate = delegate

    val picker = UIDocumentPickerViewController(
        forOpeningContentTypes = listOf(UTTypeData),
        asCopy = true,
    )
    picker.delegate = delegate
    presenter.presentViewController(picker, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val result = ByteArray(size)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}

/**
 * 把 SF Symbols 渲染成位图交给 Compose 绘制。
 *
 * 走 PNG 编码而不是直接取 CGImage：SF Symbols 是矢量字体符号，
 * PNG 方式能稳定拿到按点位缩放后的清晰位图。
 */
actual fun loadSystemSymbol(name: String, pointSize: Double): ImageBitmap? {
    val configuration = UIImageSymbolConfiguration.configurationWithPointSize(pointSize)
    val image = UIImage.systemImageNamed(name, withConfiguration = configuration) ?: return null
    val png = UIImagePNGRepresentation(image) ?: return null
    return org.jetbrains.skia.Image.makeFromEncoded(png.toByteArray()).toComposeImageBitmap()
}
