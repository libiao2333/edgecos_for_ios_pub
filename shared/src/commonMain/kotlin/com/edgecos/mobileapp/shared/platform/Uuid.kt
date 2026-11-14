package com.edgecos.mobileapp.shared.platform

/**
 * 生成一个随机的 UUID 字符串（标准 8-4-4-4-12 格式）。
 *
 * 用于需要客户端幂等键的场景（如 `POST /pay/create` 的 `clientRequestId`），
 * 因此必须平台无关：Android 侧用 JDK 的 UUID，iOS 侧用 Foundation 的 NSUUID。
 */
expect fun randomUuidString(): String
