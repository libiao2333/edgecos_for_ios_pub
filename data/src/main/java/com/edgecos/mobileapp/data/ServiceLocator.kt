package com.edgecos.mobileapp.data

import android.content.Context
import com.edgecos.mobileapp.data.auth.AndroidUserSessionStore
import com.edgecos.mobileapp.data.auth.AuthRepositoryHolder
import com.edgecos.mobileapp.data.cache.RoomAnnouncementCache
import com.edgecos.mobileapp.data.cache.RoomFileCache
import com.edgecos.mobileapp.data.db.EdgeDatabase
import com.edgecos.mobileapp.data.remote.ClientConfig
import com.edgecos.mobileapp.data.remote.EdgeApi
import com.edgecos.mobileapp.data.remote.EdgeApiImpl
import com.edgecos.mobileapp.data.remote.RetrofitEdgeApi
import com.edgecos.mobileapp.data.remote.RetrofitProvider
import com.edgecos.mobileapp.data.remote.UserStore
import com.edgecos.mobileapp.data.repository.AnnouncementRepository
import com.edgecos.mobileapp.data.repository.AuthRepository
import com.edgecos.mobileapp.data.repository.BillingRepository
import com.edgecos.mobileapp.data.repository.DirectLinkRepository
import com.edgecos.mobileapp.data.repository.FileRepository
import com.edgecos.mobileapp.data.repository.OAuthRepository
import com.edgecos.mobileapp.data.repository.PasskeyRepository
import com.edgecos.mobileapp.data.repository.ShareRepository
import com.edgecos.mobileapp.data.repository.UploadRepository
import com.edgecos.mobileapp.data.repository.WorkOrderRepository
import com.edgecos.mobileapp.data.theme.ThemeStore

/**
 * ServiceLocator：手动依赖注入容器。
 *
 * 在 App 启动时调用 [ServiceLocator.init]，注入 Context 与 BASE_URL。
 * 之后通过 [ServiceLocator.authRepository] 等访问单例。
 */
object ServiceLocator {

    @Volatile private var initialized = false

    lateinit var authRepository: AuthRepository
        private set
    lateinit var fileRepository: FileRepository
        private set
    lateinit var uploadRepository: UploadRepository
        private set
    lateinit var announcementRepository: AnnouncementRepository
        private set
    lateinit var workOrderRepository: WorkOrderRepository
        private set
    lateinit var passkeyRepository: PasskeyRepository
        private set
    lateinit var oauthRepository: OAuthRepository
        private set
    lateinit var shareRepository: ShareRepository
        private set
    lateinit var directLinkRepository: DirectLinkRepository
        private set
    lateinit var billingRepository: BillingRepository
        private set
    lateinit var database: EdgeDatabase
        private set
    lateinit var themeStore: ThemeStore
        private set

    fun init(context: Context, baseUrl: String, debug: Boolean = false) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            // 整个初始化链加一层 try-catch，避免任一子步骤抛异常直接让 App 启动崩溃。
            // 失败时仍标记 initialized=true（半功能状态），由 isInitialized 与各 lateinit 判空。
            // 真正的崩溃兜底由 EdgeCOSApp.onCreate 的 runCatching 接住。
            runCatching {
                database = EdgeDatabase.get(context)
                RetrofitProvider.init(context, baseUrl, debug)
                UserStore.init(context)
                themeStore = ThemeStore.init(context)

                // Retrofit 实现适配为平台无关的 EdgeApi，Repository 只依赖 EdgeApi
                val api: EdgeApi = EdgeApiImpl(RetrofitProvider.create<RetrofitEdgeApi>())
                authRepository = AuthRepository(api, AndroidUserSessionStore())
                fileRepository = FileRepository(api, RoomFileCache(database))
                uploadRepository = UploadRepository(api)
                announcementRepository = AnnouncementRepository(api, RoomAnnouncementCache(database))
                workOrderRepository = WorkOrderRepository(api)
                passkeyRepository = PasskeyRepository(api)
                oauthRepository = OAuthRepository(api)
                shareRepository = ShareRepository(api, ClientConfig.webBaseUrl)
                directLinkRepository = DirectLinkRepository(api)
                billingRepository = BillingRepository(api)

                // 绑定 AuthRepositoryHolder 给 ResponseInterceptor 使用
                AuthRepositoryHolder.bind(
                    clearUser = { authRepository.logout() },
                    currentUser = { authRepository.currentUser.value },
                )

                // 尝试恢复已登录用户
                authRepository.restoreUser()
            }.onFailure { e ->
                android.util.Log.e("ServiceLocator", "init partial failure", e)
            }

            initialized = true
        }
    }

    /**
     * ServiceLocator 是否已完成初始化（即便部分组件可能因异常未注入）。
     * 调用方在使用 lateinit 字段前应先检查此值，避免 UninitializedPropertyAccessException。
     */
    fun isInitialized(): Boolean = initialized
}
