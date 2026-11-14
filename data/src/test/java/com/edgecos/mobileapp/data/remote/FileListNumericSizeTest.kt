package com.edgecos.mobileapp.data.remote

import com.edgecos.mobileapp.shared.dto.OrderDto
import com.edgecos.mobileapp.shared.dto.PayCreateResponse
import com.edgecos.mobileapp.shared.dto.WorkOrderDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

/**
 * Android 真实网络栈（Retrofit + kotlinx.serialization Converter）解析线上
 * `GET /files` 数值混用响应的回归测试。
 *
 * 线上样本：目录项 "size": 1（number）、文件项 "size": "1048576"（string）。
 * fixture：data/src/test/resources/files_response_numeric_size.json
 */
class FileListNumericSizeTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val filesResponse: String by lazy {
        checkNotNull(javaClass.classLoader?.getResource("files_response_numeric_size.json")) {
            "缺少测试 fixture: files_response_numeric_size.json"
        }.readText()
    }

    @Test
    fun listFilesParsesNumericSizeThroughRetrofitStack() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(filesResponse),
            )

            val retrofit = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(
                    KotlinxJsonConverterFactory(json, "application/json".toMediaType()),
                )
                .build()

            val api = retrofit.create(RetrofitEdgeApi::class.java)
            val files = api.listFiles("/")

            assertEquals(3, files.size)
            assertEquals("Documents", files[0].filename)
            assertEquals("1", files[0].size)
            assertEquals("1048576", files[1].size)
            assertEquals("2048", files[2].size)
            assertTrue(files.any { it.type == "directory" })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun fileSummaryAcceptsStringCountsThroughRetrofitStack() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"fileCount":"12","folderCount":3,"recentCount":"4","sharedCount":5}"""),
            )

            val retrofit = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(
                    KotlinxJsonConverterFactory(json, "application/json".toMediaType()),
                )
                .build()

            val summary = retrofit.create(RetrofitEdgeApi::class.java).getFilesSummary()

            assertEquals(12L, summary.fileCount)
            assertEquals(3L, summary.folderCount)
            assertEquals(4L, summary.recentCount)
            assertEquals(5L, summary.sharedCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun quotaAcceptsNumericBigIntThroughRetrofitStack() = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""{"quota":107374182400,"used":"53687091200"}"""),
            )

            val retrofit = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(
                    KotlinxJsonConverterFactory(json, "application/json".toMediaType()),
                )
                .build()

            val quota = retrofit.create(RetrofitEdgeApi::class.java).getUserQuota()

            assertEquals("107374182400", quota.quota)
            assertEquals("53687091200", quota.used)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun orderAcceptsMixedNumericTypes() {
        val payload = """
            {
              "id": "7", "tradeNo": "TN2026010100001", "planId": 3, "amount": 29.9,
              "status": "paid", "paymentMethod": "balance", "billingMonths": "1",
              "Plan": {
                "id": "3", "name": "标准版", "quota": 107374182400,
                "trafficQuota": "53687091200", "price": 29.9, "originalPrice": "39.9",
                "duration": "1", "level": 1, "isTrafficPackage": false
              }
            }
        """.trimIndent()

        val order = json.decodeFromString<OrderDto>(payload)

        assertEquals(7L, order.id)
        assertEquals(3L, order.planId)
        assertEquals("29.9", order.amount)
        assertEquals(1L, order.billingMonths)
        assertEquals("107374182400", order.Plan?.quota)
        assertEquals(29.9, order.Plan?.price?.toDouble() ?: 0.0, 0.0001)
    }

    @Test
    fun workOrderIdsAcceptString() {
        val payload = """
            {
              "id": "11", "userId": 9, "title": "无法下载", "content": "文件下载失败",
              "category": "file_download_cdn", "status": "pending",
              "createdAt": "2026-10-01T00:00:00Z",
              "WorkOrderReplies": [
                {"id":"21","workOrderId":"11","userId":"9","content":"已处理","isAdminReply":true,"createdAt":"2026-10-01T01:00:00Z"}
              ]
            }
        """.trimIndent()

        val order = json.decodeFromString<WorkOrderDto>(payload)

        assertEquals(11L, order.id)
        assertEquals(9L, order.userId)
        assertEquals(21L, order.WorkOrderReplies?.first()?.id)
        assertEquals(11L, order.WorkOrderReplies?.first()?.workOrderId)
    }

    @Test
    fun payResponseAcceptsRemainingSecondsAsString() {
        val payload = """{"tradeNo":"TN1","status":"pending","remainingSeconds":"900","balance":100.5}"""

        val pay = json.decodeFromString<PayCreateResponse>(payload)

        assertEquals(900L, pay.remainingSeconds)
        assertEquals("100.5", pay.balance)
    }
}
