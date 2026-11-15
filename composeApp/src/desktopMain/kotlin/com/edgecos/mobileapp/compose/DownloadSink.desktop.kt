package com.edgecos.mobileapp.compose

import java.io.File
import java.io.FileOutputStream
import java.awt.Desktop

actual class DownloadSink actual constructor(fileName: String) {
    private val file: File = File(System.getProperty("user.home"), "Downloads")
        .apply { mkdirs() }
        .resolve(fileName)
    private val out = FileOutputStream(file)

    actual fun write(buffer: ByteArray, length: Int) {
        out.write(buffer, 0, length)
    }

    actual fun finish(): String {
        out.close()
        return file.absolutePath
    }

    actual fun abort() {
        runCatching { out.close() }
        file.delete()
    }
}

actual fun shareLocalFile(path: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(File(path).parentFile)
        }
    }
}
