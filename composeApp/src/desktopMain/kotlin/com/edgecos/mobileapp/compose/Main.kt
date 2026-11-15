package com.edgecos.mobileapp.compose

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** 桌面预览入口：窗口按 iPhone 竖屏比例，用来核对 iOS 版式。 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EdgeCOS",
        state = rememberWindowState(width = 390.dp, height = 760.dp),
    ) {
        App()
    }
}
