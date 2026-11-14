package com.edgecos.mobileapp.data.remote

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/**
 * Retrofit + kotlinx.serialization 的轻量 Converter.Factory。
 *
 * 之所以自己实现而不引入第三方 converter：
 * 后续网络层会整体替换为 Ktor，这里只需要覆盖当前的 JSON 请求/响应转换，
 * 逻辑足够小，也避免额外依赖的 API 变动。
 */
class KotlinxJsonConverterFactory(
    private val format: StringFormat,
    private val contentType: MediaType,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val deserializer: DeserializationStrategy<Any> = format.serializersModule.serializer(type)
        return Converter<ResponseBody, Any> { body ->
            body.use { format.decodeFromString(deserializer, it.string()) }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody> {
        val serializer: SerializationStrategy<Any> = format.serializersModule.serializer(type)
        return Converter<Any, RequestBody> { value ->
            format.encodeToString(serializer, value).toRequestBody(contentType)
        }
    }
}
