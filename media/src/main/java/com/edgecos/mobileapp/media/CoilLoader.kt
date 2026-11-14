package com.edgecos.mobileapp.media

import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.request.Disposable
import coil.request.ImageRequest
import com.edgecos.mobileapp.data.remote.EdgeHttpClient

/**
 * Coil ImageLoader：复用 EdgeCosHttpClient 的 OkHttpClient，
 * 确保图片加载时自动携带 Cookie 鉴权（download?path=&id=&line=）。
 */
object CoilLoader {

    private var imageLoader: ImageLoader? = null

    fun get(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: ImageLoader.Builder(context)
                .okHttpClient {
                    EdgeHttpClient.get().newBuilder()
                        // 不要 followRedirects 覆盖默认，保持 Coil 的行为
                        .build()
                }
                .build()
                .also { imageLoader = it }
        }
    }
}
