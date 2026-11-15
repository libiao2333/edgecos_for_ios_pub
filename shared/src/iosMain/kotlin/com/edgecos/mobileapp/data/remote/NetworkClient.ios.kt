package com.edgecos.mobileapp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS 侧 Ktor 客户端（Darwin 引擎，基于 NSURLSession）。
 *
 * 保持最小配置：Darwin 引擎的配置项在不同 Ktor 版本间存在差异，这里只安装
 * 共享默认插件（JSON / Cookie / 日志），其余使用 NSURLSession 默认行为
 * （默认跟随 302 等重定向）。
 */
actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    installEdgeDefaults()
}
