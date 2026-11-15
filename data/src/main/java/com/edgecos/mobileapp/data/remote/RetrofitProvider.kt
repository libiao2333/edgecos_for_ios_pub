package com.edgecos.mobileapp.data.remote

import android.content.Context
import com.edgecos.mobileapp.shared.dto.UserDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

/**
 * Retrofit 实例提供者。
 * 由 ServiceLocator 初始化。
 *
 * JSON 序列化使用 kotlinx.serialization（KMP 友好），不再依赖 Moshi。
 */
object RetrofitProvider {

    @Volatile
    private var retrofit: Retrofit? = null

    /** 全局 JSON 配置：忽略未知字段，兼容后端后续新增字段 */
    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun init(context: Context, baseUrl: String, debug: Boolean = false) {
        EdgeHttpClient.init(context, debug)
        ClientConfig.init(context, baseUrl)
        if (retrofit != null) return
        synchronized(this) {
            retrofit ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(EdgeHttpClient.get())
                .addConverterFactory(KotlinxJsonConverterFactory(json, "application/json".toMediaType()))
                .build()
                .also { retrofit = it }
        }
    }

    fun <T> create(service: Class<T>): T = retrofit?.create(service)
        ?: error("RetrofitProvider 未初始化，请先调用 init(context, baseUrl)")

    inline fun <reified T> create(): T = create(T::class.java)
}

/**
 * 当前用户持久化：用 EncryptedSharedPreferences 存储 User JSON。
 */
object UserStore {
    private const val PREF_NAME = "edgecos_user"
    private const val KEY_USER = "current_user"

    @Volatile
    private var prefs: android.content.SharedPreferences? = null

    @Volatile
    private var cached: UserDto? = null

    fun init(context: Context) {
        val appContext = context.applicationContext
        // 加密 SharedPreferences 在 root/损坏 Keystore/厂商 ROM 异常设备上会抛异常，
        // 失败时回退到普通 SharedPreferences，保 App 不崩（与 EncryptedCookieJar 同策略）
        prefs = runCatching {
            val masterKey = androidx.security.crypto.MasterKey.Builder(appContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            androidx.security.crypto.EncryptedSharedPreferences.create(
                appContext,
                PREF_NAME,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrElse {
            android.util.Log.e("UserStore", "EncryptedSharedPreferences init failed, fallback to plain prefs", it)
            appContext.getSharedPreferences("${PREF_NAME}_fallback", android.content.Context.MODE_PRIVATE)
        }
        // 启动时加载缓存
        cached = prefs?.getString(KEY_USER, null)?.let { json ->
            runCatching {
                RetrofitProvider.json.decodeFromString<UserDto>(json)
            }.getOrNull()
        }
    }

    fun save(user: UserDto) {
        cached = user
        val json = RetrofitProvider.json.encodeToString(user)
        prefs?.edit()?.putString(KEY_USER, json)?.apply()
    }

    fun get(): UserDto? = cached

    /**
     * 当前用户 JSON（字段结构与 Web 前端 localStorage.user 一致），未登录返回 null。
     * 供 WebView 打开网页端页面时镜像登录态使用。
     */
    fun getJson(): String? = cached?.let { user ->
        runCatching { RetrofitProvider.json.encodeToString(user) }.getOrNull()
    }

    fun clear() {
        cached = null
        prefs?.edit()?.remove(KEY_USER)?.apply()
    }
}
