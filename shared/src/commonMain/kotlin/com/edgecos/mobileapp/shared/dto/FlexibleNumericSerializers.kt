@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.edgecos.mobileapp.shared.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.nullable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * 后端 DTO 的“string / number 兼容”序列化器。
 *
 * 背景：后端部分接口对同一字段并不总是返回固定 JSON 类型。例如 `GET /files`
 * 的文件项 `size` 通常是大整数字符串，但目录项会返回数字（`"size": 1`）。
 * kotlinx.serialization 默认严格校验类型，字符串字段遇到数字会抛
 * `Unexpected JSON token`，进而被 Repository 的 `runCatchingEdge` 吞成空列表。
 *
 * 这里统一把 JSON string / number / boolean 的文本内容归一化为目标类型：
 * - 字符串字段：数字也转成字符串（BIGINT 不丢精度）；
 * - Long/Int/Double 字段：字符串也尝试解析成数字；
 * - 序列化仍然输出原类型，不影响请求体。
 */
private fun JsonElement.flexibleText(): String = when (this) {
    is JsonNull -> throw SerializationException("期望非空 string/number，实际为 null")
    is JsonPrimitive -> content
    else -> throw SerializationException("期望 string/number，实际为 $this")
}

private fun JsonElement.flexibleLong(): Long =
    flexibleText().toLongOrNull()
        ?: throw SerializationException("期望 long，实际为 $this")

private fun JsonElement.flexibleInt(): Int =
    flexibleText().toIntOrNull()
        ?: throw SerializationException("期望 int，实际为 $this")

private fun JsonElement.flexibleDouble(): Double =
    flexibleText().toDoubleOrNull()
        ?: throw SerializationException("期望 double，实际为 $this")

private fun Decoder.jsonElement(): JsonElement =
    (this as? JsonDecoder)?.decodeJsonElement()
        ?: throw SerializationException("Flexible 序列化器仅支持 JSON")

/** 接受 JSON string 或 number，统一解码为 String。 */
object FlexibleStringSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String = decoder.jsonElement().flexibleText()

    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
}

/** [FlexibleStringSerializer] 的 nullable 版本。 */
object FlexibleNullableStringSerializer : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleString?", PrimitiveKind.STRING).nullable

    override fun deserialize(decoder: Decoder): String? {
        val element = decoder.jsonElement()
        return if (element is JsonNull) null else element.flexibleText()
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}

/** 接受 JSON string 或 number，统一解码为 Long。 */
object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long = decoder.jsonElement().flexibleLong()

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

/** [FlexibleLongSerializer] 的 nullable 版本。 */
object FlexibleNullableLongSerializer : KSerializer<Long?> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleLong?", PrimitiveKind.LONG).nullable

    override fun deserialize(decoder: Decoder): Long? {
        val element = decoder.jsonElement()
        return if (element is JsonNull) null else element.flexibleLong()
    }

    override fun serialize(encoder: Encoder, value: Long?) {
        if (value == null) encoder.encodeNull() else encoder.encodeLong(value)
    }
}

/** 接受 JSON string 或 number，统一解码为 Int。 */
object FlexibleIntSerializer : KSerializer<Int> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int = decoder.jsonElement().flexibleInt()

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/** 接受 JSON string 或 number，统一解码为 Double。 */
object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor = PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double = decoder.jsonElement().flexibleDouble()

    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
}
