package com.edgecos.mobileapp.upload

import java.io.InputStream
import java.security.MessageDigest

/**
 * SHA-256 流式计算器。
 *
 * 分块 8MB 更新，避免大文件一次性加载到内存。
 * hash 字符串格式：`sha256:<hex>:<size>`（与前端 [Drive.jsx] 对齐）。
 */
object Sha256Calculator {

    private const val BUFFER_SIZE = 8 * 1024 * 1024 // 8MB

    /**
     * 计算输入流的 SHA-256。
     *
     * @param totalSize 文件总字节数，用于拼装 hash 字符串。
     * @return `sha256:<hex>:<size>` 格式字符串
     */
    fun calculate(input: InputStream, totalSize: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        var read: Int
        while (true) {
            read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
        val hex = digest.digest().joinToString("") { "%02x".format(it) }
        return "sha256:$hex:$totalSize"
    }
}
