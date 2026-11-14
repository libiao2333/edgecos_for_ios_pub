package com.edgecos.mobileapp.shared.platform

import java.util.UUID

/** Android 实现：委托 JDK 的随机 UUID。 */
actual fun randomUuidString(): String = UUID.randomUUID().toString()
