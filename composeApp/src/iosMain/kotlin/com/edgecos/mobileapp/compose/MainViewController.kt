package com.edgecos.mobileapp.compose

import androidx.compose.ui.window.ComposeUIViewController

/** iOS 入口：Swift 侧通过 `ComposeApp.MainViewController()` 拿到承载 Compose 的 UIViewController。 */
fun MainViewController() = ComposeUIViewController { App() }
