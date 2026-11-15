package com.edgecos.mobileapp.shared.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 后端数值字段 string/number 混用的兼容性测试。
 *
 * 线上真实问题：`GET /files` 的目录项返回 `"size": 1`（number），而文件项返回
 * `"size": "1048576"`（string）。DTO 原来声明为 String，严格反序列化直接抛
 * `Unexpected JSON token ... at path: $[0].size`，被 Repository 吞掉后表现为空列表。
 */
class FlexibleNumericDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** 线上 GET /files 的脱敏样本：目录 size=number、文件 size=string/number 混用。 */
    private val filesResponse = """
        [
          {
            "id": "1001",
            "filename": "Documents",
            "basename": "Documents",
            "lastmod": "2026-10-01T12:00:00.000Z",
            "size": 1,
            "type": "directory",
            "thumbnail": null,
            "hasThumbnail": false,
            "activeShare": null
          },
          {
            "id": "1002",
            "filename": "report.pdf",
            "basename": "report.pdf",
            "lastmod": "2026-10-02T08:30:00.000Z",
            "size": "1048576",
            "type": "file",
            "thumbnail": "https://cdn.example.com/thumb/1002.jpg",
            "hasThumbnail": true
          },
          {
            "id": "1003",
            "filename": "photo.jpg",
            "basename": "photo.jpg",
            "lastmod": "2026-10-03T09:15:00.000Z",
            "size": 2048,
            "type": "file",
            "hasThumbnail": false
          }
        ]
    """.trimIndent()

    @Test
    fun listFilesParsesNumericAndStringSize() {
        val files = json.decodeFromString<List<FileNodeDto>>(filesResponse)

        assertEquals(3, files.size)
        assertEquals("Documents", files[0].filename)
        assertEquals("1", files[0].size)
        assertEquals("directory", files[0].type)
        assertEquals("1048576", files[1].size)
        assertEquals("2048", files[2].size)
    }

    @Test
    fun fileSummaryAcceptsStringAndNumericCounts() {
        val payload = """
            {"fileCount":"12","folderCount":3,"recentCount":"4","sharedCount":5}
        """.trimIndent()

        val summary = json.decodeFromString<FileSummaryDto>(payload)

        assertEquals(12L, summary.fileCount)
        assertEquals(3L, summary.folderCount)
        assertEquals(4L, summary.recentCount)
        assertEquals(5L, summary.sharedCount)
    }

    @Test
    fun quotaAcceptsNumericBigInt() {
        val payload = """{"quota":107374182400,"used":"53687091200"}"""

        val quota = json.decodeFromString<QuotaResponse>(payload)

        assertEquals("107374182400", quota.quota)
        assertEquals("53687091200", quota.used)
    }

    @Test
    fun orderAcceptsMixedNumericTypes() {
        val payload = """
            {
              "id": "7",
              "tradeNo": "TN2026010100001",
              "planId": 3,
              "amount": 29.9,
              "status": "paid",
              "paymentMethod": "balance",
              "billingMonths": "1",
              "Plan": {
                "id": "3",
                "name": "标准版",
                "quota": 107374182400,
                "trafficQuota": "53687091200",
                "price": 29.9,
                "originalPrice": "39.9",
                "duration": "1",
                "level": 1,
                "isTrafficPackage": false
              }
            }
        """.trimIndent()

        val order = json.decodeFromString<OrderDto>(payload)

        assertEquals(7L, order.id)
        assertEquals(3L, order.planId)
        assertEquals("29.9", order.amount)
        assertEquals(1L, order.billingMonths)
        assertEquals("3", order.Plan?.id?.toString())
        assertEquals("107374182400", order.Plan?.quota)
        assertEquals("29.9", order.Plan?.price)
        assertEquals(1L, order.Plan?.duration)
    }

    @Test
    fun workOrderIdsAcceptString() {
        val payload = """
            {
              "id": "11",
              "userId": 9,
              "title": "无法下载",
              "content": "文件下载失败",
              "category": "file_download_cdn",
              "status": "pending",
              "createdAt": "2026-10-01T00:00:00Z",
              "WorkOrderReplies": [
                {"id": "21", "workOrderId": "11", "userId": "9", "content": "已处理", "isAdminReply": true, "createdAt": "2026-10-01T01:00:00Z"}
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
        val payload = """
            {"tradeNo":"TN1","status":"pending","remainingSeconds":"900","balance":100.5}
        """.trimIndent()

        val pay = json.decodeFromString<PayCreateResponse>(payload)

        assertEquals(900L, pay.remainingSeconds)
        assertEquals("100.5", pay.balance)
    }

    @Test
    fun serializationStillEmitsStringForSize() {
        val file = FileNodeDto(
            id = "1",
            filename = "a.txt",
            basename = "a.txt",
            lastmod = "2026-10-01T00:00:00Z",
            size = "123",
            type = "file",
        )

        val encoded = json.encodeToString(file)

        assertTrue(encoded.contains("\"size\":\"123\""), "size 应序列化为字符串: $encoded")
        assertEquals("123", json.decodeFromString<FileNodeDto>(encoded).size)
    }
}
