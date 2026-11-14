package com.edgecos.mobileapp.shared.platform

import platform.Foundation.NSUUID

/** iOS 实现：委托 Foundation 的 `NSUUID`（其 `UUIDString` 已是标准大写格式）。 */
actual fun randomUuidString(): String = NSUUID().UUIDString
