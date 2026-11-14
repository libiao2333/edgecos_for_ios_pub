package com.edgecos.mobileapp.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject

/**
 * 加密 CookieJar：把 token Cookie 持久化到 EncryptedSharedPreferences。
 *
 * - 文件名：`edgecos_prefs`
 * - 加密：AES256-GCM（由 [MasterKey] 提供）
 * - 仅持久化 host = `user.edgecos.com` 的 Cookie（其他临时 Cookie 仅存内存）
 *
 * 等价于前端 `axios.create({ withCredentials: true })` 行为。
 */
class EncryptedCookieJar(context: Context) : CookieJar {

    // 加密 SharedPreferences 在 root/损坏 Keystore/厂商 ROM 异常设备上会抛
    // GeneralSecurityException 或 IOException。失败时回退到普通 SharedPreferences，
    // 失去加密但保 App 不崩（优于白屏）。
    private val prefs: android.content.SharedPreferences = run {
        val appContext = context.applicationContext
        runCatching {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            android.util.Log.e("EncryptedCookieJar", "EncryptedSharedPreferences init failed, fallback to plain prefs", it)
            appContext.getSharedPreferences("${PREF_NAME}_fallback", android.content.Context.MODE_PRIVATE)
        }
    }

    private val memoryCache: MutableMap<String, MutableList<Cookie>> = mutableMapOf()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val list = memoryCache.getOrPut(host) { mutableListOf() }
        // 移除同名旧 Cookie 后追加新 Cookie
        for (c in cookies) {
            list.removeAll { it.name == c.name }
            list.add(c)
        }
        // 持久化（仅对目标域）
        if (host == TARGET_HOST || host.endsWith(".$TARGET_HOST")) {
            persist(host, list)
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val list = memoryCache[host] ?: loadFromDisk(host).also { memoryCache[host] = it }
        val now = System.currentTimeMillis()
        return list.filter { !it.persistent || it.expiresAt > now }
    }

    @Synchronized
    fun clear() {
        memoryCache.clear()
        prefs.edit().clear().apply()
    }

    /** 取出 token Cookie 值（用于 JWT 解析滑动续期） */
    @Synchronized
    fun getToken(): String? {
        val list = memoryCache[TARGET_HOST] ?: loadFromDisk(TARGET_HOST).also { memoryCache[TARGET_HOST] = it }
        return list.firstOrNull { it.name == "token" }?.value
    }

    /** 注入单个 Cookie（用于 OAuth 回调后从系统 CookieManager 同步 token） */
    @Synchronized
    fun putCookie(host: String, cookie: Cookie) {
        val list = memoryCache.getOrPut(host) { mutableListOf() }
        list.removeAll { it.name == cookie.name }
        list.add(cookie)
        if (host == TARGET_HOST || host.endsWith(".$TARGET_HOST")) {
            persist(host, list)
        }
    }

    private fun persist(host: String, cookies: List<Cookie>) {
        val arr = JSONArray()
        for (c in cookies) {
            arr.put(JSONObject().apply {
                put("name", c.name)
                put("value", c.value)
                put("expiresAt", c.expiresAt)
                put("domain", c.domain)
                put("path", c.path)
                put("secure", c.secure)
                put("httpOnly", c.httpOnly)
                put("persistent", c.persistent)
            })
        }
        prefs.edit().putString(host, arr.toString()).apply()
    }

    private fun loadFromDisk(host: String): MutableList<Cookie> {
        val json = prefs.getString(host, null) ?: return mutableListOf()
        return runCatching {
            val arr = JSONArray(json)
            val list = mutableListOf<Cookie>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val builder = Cookie.Builder()
                    .name(o.getString("name"))
                    .value(o.getString("value"))
                    .domain(o.optString("domain", TARGET_HOST))
                    .path(o.optString("path", "/"))
                if (o.optBoolean("secure")) builder.secure()
                if (o.optBoolean("httpOnly")) builder.httpOnly()
                if (o.optBoolean("persistent")) {
                    builder.expiresAt(o.optLong("expiresAt"))
                }
                list.add(builder.build())
            }
            list
        }.getOrDefault(mutableListOf())
    }

    companion object {
        private const val PREF_NAME = "edgecos_prefs"
        private const val TARGET_HOST = "user.edgecos.com"
    }
}
