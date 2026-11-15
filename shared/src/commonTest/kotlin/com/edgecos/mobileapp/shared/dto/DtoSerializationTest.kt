package com.edgecos.mobileapp.shared.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * commonMain 中 DTO 的序列化往返测试。
 *
 * Json 配置与 Android 侧 RetrofitProvider 保持一致：
 * ignoreUnknownKeys = true、coerceInputValues = true。
 */
class DtoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ==================== Files ====================

    @Test
    fun fileNodeDtoRoundTrip() {
        val original = FileNodeDto(
            id = "f-1",
            filename = "docs/report.pdf",
            basename = "report.pdf",
            lastmod = "2026-01-02T03:04:05Z",
            size = "1048576",
            type = "file",
            thumbnail = "https://cdn.example.com/t/f-1.jpg",
            hasThumbnail = true,
            activeShare = ActiveShareDto(
                shareId = "s-9",
                expiration = "2026-02-01T00:00:00Z",
                extractionCode = "ab12",
            ),
        )

        val decoded = json.decodeFromString<FileNodeDto>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("f-1", decoded.id)
        assertEquals("docs/report.pdf", decoded.filename)
        assertEquals("report.pdf", decoded.basename)
        assertEquals("1048576", decoded.size)
        assertEquals("file", decoded.type)
        assertEquals(true, decoded.hasThumbnail)
        assertEquals("s-9", decoded.activeShare?.shareId)
        assertEquals("ab12", decoded.activeShare?.extractionCode)
    }

    @Test
    fun fileNodeDtoUsesDefaultsForOptionalFields() {
        val original = FileNodeDto(
            id = "d-1",
            filename = "docs",
            basename = "docs",
            lastmod = "2026-01-02T03:04:05Z",
            size = "0",
            type = "directory",
        )

        val decoded = json.decodeFromString<FileNodeDto>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertNull(decoded.thumbnail)
        assertEquals(false, decoded.hasThumbnail)
        assertNull(decoded.activeShare)
        assertEquals("directory", decoded.type)
    }

    @Test
    fun fileNodeDtoIgnoresUnknownKeys() {
        val payload = """
            {
              "id": "f-2",
              "filename": "notes.md",
              "basename": "notes.md",
              "lastmod": "2026-01-02T03:04:05Z",
              "size": "12",
              "type": "file",
              "serverOnlyField": 123,
              "nested": { "a": true }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<FileNodeDto>(payload)

        assertEquals("f-2", decoded.id)
        assertEquals("notes.md", decoded.filename)
        assertEquals(12, decoded.size.toInt())
        assertEquals(false, decoded.hasThumbnail)
    }

    // ==================== Announcements ====================

    @Test
    fun announcementsResponseRoundTrip() {
        val original = AnnouncementsResponse(
            list = listOf(
                AnnouncementDto(
                    id = 101L,
                    title = "维护通知",
                    content = "系统将于今晚 23:00 维护。",
                    createdAt = "2026-01-01T00:00:00Z",
                    isRead = true,
                ),
                AnnouncementDto(
                    id = 102L,
                    title = "新功能上线",
                    content = "支持批量移动文件。",
                    createdAt = "2026-01-02T00:00:00Z",
                ),
            ),
            unread = 1L,
        )

        val decoded = json.decodeFromString<AnnouncementsResponse>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(2, decoded.list.size)
        assertEquals(1L, decoded.unread)
        assertEquals(101L, decoded.list[0].id)
        assertEquals("维护通知", decoded.list[0].title)
        assertEquals("系统将于今晚 23:00 维护。", decoded.list[0].content)
        assertEquals("2026-01-01T00:00:00Z", decoded.list[0].createdAt)
        assertTrue(decoded.list[0].isRead)
        // 未显式赋值的 isRead 走默认值 false
        assertEquals(false, decoded.list[1].isRead)
    }

    // ==================== Auth ====================

    @Test
    fun loginResponseRoundTrip() {
        val original = LoginResponse(
            user = UserDto(
                id = 42L,
                username = "alice",
                role = "user",
                email = "alice@example.com",
                qqNumber = "10001",
                qqOpenId = "qq-open-id",
                qqNickname = "Alice",
                qqAvatar = "https://cdn.example.com/avatar/alice.png",
                oauthAccounts = mapOf(
                    "qq" to OAuthAccountDto(
                        provider = "qq",
                        uid = "qq-uid-1",
                        nickname = "Alice",
                        avatar = "https://cdn.example.com/avatar/alice.png",
                        gender = "female",
                        location = "CN",
                        ip = "203.0.113.1",
                        accessToken = "token-1",
                        updatedAt = "2026-01-01T00:00:00Z",
                    ),
                ),
                realNameStatus = "verified",
                realNameMaskedName = "张*",
                realNameMaskedIdNo = "1101**********1234",
                realNameVerifiedAt = "2026-01-01T00:00:00Z",
            ),
        )

        val decoded = json.decodeFromString<LoginResponse>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(42L, decoded.user.id)
        assertEquals("alice", decoded.user.username)
        assertEquals("user", decoded.user.role)
        assertEquals("alice@example.com", decoded.user.email)
        assertEquals("verified", decoded.user.realNameStatus)
        assertEquals("张*", decoded.user.realNameMaskedName)

        val qqAccount = assertNotNull(decoded.user.oauthAccounts?.get("qq"))
        assertEquals("qq-uid-1", qqAccount.uid)
        assertEquals("Alice", qqAccount.nickname)
        assertEquals("token-1", qqAccount.accessToken)
    }

    @Test
    fun loginResponseUsesDefaultsForOptionalUserFields() {
        val original = LoginResponse(
            user = UserDto(
                id = 7L,
                username = "bob",
                role = "admin",
            ),
        )

        val decoded = json.decodeFromString<LoginResponse>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("unverified", decoded.user.realNameStatus)
        assertNull(decoded.user.email)
        assertNull(decoded.user.oauthAccounts)
        assertNull(decoded.user.realNameMaskedName)
    }

    // ==================== Billing ====================

    @Test
    fun payCreateResponseRoundTrip() {
        val original = PayCreateResponse(
            tradeNo = "TN2026010100001",
            traceId = "trace-1",
            reused = false,
            notice = null,
            status = "paid",
            order = OrderDto(
                id = 7L,
                tradeNo = "TN2026010100001",
                planId = 3L,
                amount = "29.90",
                status = "paid",
                paymentMethod = "balance",
                payTime = "2026-01-01T00:00:00Z",
                billingMonths = 1L,
                Plan = PlanDto(
                    id = 3L,
                    name = "标准版",
                    quota = "107374182400",
                    trafficQuota = "53687091200",
                    price = "29.90",
                    originalPrice = "39.90",
                    duration = 1L,
                    level = 1L,
                    isTrafficPackage = false,
                    discount = "0.75",
                    contactOnly = false,
                    contactEmail = "sales@example.com",
                ),
            ),
            bill = BillDto(
                id = 11L,
                tradeNo = "TN2026010100001",
                amount = "29.90",
                type = "consume",
                category = "plan",
                title = "购买标准版",
                status = "paid",
                paymentMethod = "balance",
                payTime = "2026-01-01T00:00:00Z",
            ),
            balance = "100.00",
            paymentMethod = "balance",
        )

        val decoded = json.decodeFromString<PayCreateResponse>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals("TN2026010100001", decoded.tradeNo)
        assertEquals("trace-1", decoded.traceId)
        assertEquals(false, decoded.reused)
        assertEquals("paid", decoded.status)
        assertEquals("100.00", decoded.balance)
        assertEquals("balance", decoded.paymentMethod)

        val order = assertNotNull(decoded.order)
        assertEquals(7L, order.id)
        assertEquals("29.90", order.amount)
        assertEquals("paid", order.status)
        assertEquals(1L, order.billingMonths)
        assertEquals("标准版", order.Plan?.name)
        assertEquals("107374182400", order.Plan?.quota)

        val bill = assertNotNull(decoded.bill)
        assertEquals(11L, bill.id)
        assertEquals("购买标准版", bill.title)

        // 网关支付字段未使用，序列化后应为 null
        assertNull(decoded.payUrl)
        assertNull(decoded.params)
        assertNull(decoded.html)
        assertNull(decoded.remainingSeconds)
    }

    @Test
    fun payCreateGatewayResponseRoundTrip() {
        val original = PayCreateResponse(
            tradeNo = "TN2026010100002",
            reused = true,
            payUrl = "https://pay.example.com/checkout?tradeNo=TN2026010100002",
            params = mapOf(
                "appId" to "wx-app-id",
                "timeStamp" to "1767225600",
                "nonceStr" to "nonce-1",
                "package" to "prepay_id=wx123",
                "signType" to "RSA",
                "paySign" to "sign-1",
            ),
            html = "<form id=\"pay\"></form>",
            payType = "wxpay",
            expireAt = "2026-01-01T00:15:00Z",
            remainingSeconds = 900L,
        )

        val decoded = json.decodeFromString<PayCreateResponse>(json.encodeToString(original))

        assertEquals(original, decoded)
        assertEquals(true, decoded.reused)
        assertEquals("wxpay", decoded.payType)
        assertEquals("https://pay.example.com/checkout?tradeNo=TN2026010100002", decoded.payUrl)
        assertEquals(900L, decoded.remainingSeconds)
        assertEquals("2026-01-01T00:15:00Z", decoded.expireAt)

        val params = assertNotNull(decoded.params)
        assertEquals(6, params.size)
        assertEquals("wx-app-id", params["appId"])
        assertEquals("prepay_id=wx123", params["package"])
        assertNull(decoded.order)
        assertNull(decoded.bill)
    }

    @Test
    fun payCreateResponseToleratesUnknownKeys() {
        val payload = """
            {
              "tradeNo": "TN2026010100003",
              "status": "pending",
              "paymentMethod": "alipay",
              "serverOnlyField": "ignored",
              "order": {
                "id": 9,
                "tradeNo": "TN2026010100003",
                "planId": 4,
                "amount": "9.90",
                "status": "pending",
                "extraPlanField": true
              }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<PayCreateResponse>(payload)

        assertEquals("TN2026010100003", decoded.tradeNo)
        assertEquals("pending", decoded.status)
        assertEquals("alipay", decoded.paymentMethod)
        assertEquals(9L, decoded.order?.id)
        assertEquals("9.90", decoded.order?.amount)
        assertNull(decoded.order?.Plan)
        assertEquals(1L, decoded.order?.billingMonths)
    }
}
