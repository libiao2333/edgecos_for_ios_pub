package com.edgecos.mobileapp.shared.platform

import java.util.UUID

actual fun randomUuidString(): String = UUID.randomUUID().toString()
