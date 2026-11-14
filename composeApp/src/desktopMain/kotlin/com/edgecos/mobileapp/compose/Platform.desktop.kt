package com.edgecos.mobileapp.compose

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser

actual fun openExternalUrl(url: String) {
    runCatching {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    }
}

actual suspend fun pickFileForUpload(): PickedFile? = withContext(Dispatchers.IO) {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择要上传的文件"
        isMultiSelectionEnabled = false
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@withContext null
    val file = chooser.selectedFile ?: return@withContext null
    PickedFile(name = file.name, bytes = file.readBytes())
}

/** 桌面端没有 SF Symbols，返回 null 由调用方回退到 Material 图标。 */
actual fun loadSystemSymbol(name: String, pointSize: Double): ImageBitmap? = null
