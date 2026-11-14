package com.edgecos.mobileapp.domain.util

import kotlin.math.round

/**
 * 字节格式化：1024 进制，输出形如 `5 GB` / `10.5 GB`。
 *
 * 与网页端 `Frontend-main/src/lib/format.js` 的 `formatSize` 保持一致。
 * 入参为 Long（commonMain 无 java.math.BigInteger）。
 */
fun formatBytes(bytes: Long, digits: Int = 2): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB", "EB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "${formatFixed(value, digits)} ${units[unitIndex]}"
}

/**
 * 定点小数格式化，替代 JVM 专属的 String.format / BigDecimal.setScale。
 * 末尾 0 会被裁掉，保持与原实现 stripTrailingZeros 一致的输出。
 */
private fun formatFixed(value: Double, digits: Int): String {
    var factor = 1L
    repeat(digits) { factor *= 10 }
    val scaled = round(value * factor).toLong()
    val intPart = scaled / factor
    val fracPart = scaled % factor
    if (fracPart == 0L || digits == 0) return intPart.toString()
    val frac = fracPart.toString().padStart(digits, '0').trimEnd('0')
    return if (frac.isEmpty()) intPart.toString() else "$intPart.$frac"
}

/**
 * API 时间格式化：将后端返回的 ISO 8601 时间字符串（如 `2026-07-28T16:15:47.484Z`）
 * 格式化为 `2026-07-28 16:15`（去掉 T 和 Z，精确到分钟，不含毫秒和秒）。
 *
 * 支持的输入格式：
 * - `2026-07-28T16:15:47.484Z`（ISO 8601 带时区）
 * - `2026-07-28T16:15:47Z`（无毫秒）
 * - `2026-07-28 16:15:47`（已格式化的字符串，直接截断到分钟）
 *
 * 说明：后端时间统一为 UTC，原实现用 ZonedDateTime 解析后按原时区输出，
 * 因此这里直接截断字符即可得到等价结果，无需 JVM 的 DateTimeFormatter。
 */
fun formatApiTime(raw: String): String {
    if (raw.isBlank()) return raw
    val normalized = raw.replace('T', ' ')
    return if (normalized.length >= 16) normalized.substring(0, 16) else raw
}
