package com.edgecos.mobileapp.feature.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.components.IOSListRow
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 开源致谢：iOS HIG 风格。
 *
 * - 顶部 Large Title 导航栏
 * - 致谢说明文本
 * - 分组圆角卡片展示各开源依赖
 */
@Composable
fun OpenSourceAcknowledgementsScreen(
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "开源致谢", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
            ) {
                Text(
                    text = "EdgeCOS Android 客户端基于以下优秀的开源项目构建，在此致以诚挚谢意。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
                )

                IOSGroupedList(title = "Kotlin 与协程") {
                    listOf(
                        Triple("Kotlin", "JetBrains", "https://kotlinlang.org/"),
                        Triple("Kotlinx Coroutines", "JetBrains", "https://github.com/Kotlin/kotlinx.coroutines"),
                        Triple("Kotlinx Serialization", "JetBrains", "https://github.com/Kotlin/kotlinx.serialization"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 2,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "AndroidX 与 Jetpack") {
                    listOf(
                        Triple("AndroidX Core KTX", "Google", "https://developer.android.com/jetpack/androidx"),
                        Triple("AndroidX Lifecycle", "Google", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
                        Triple("AndroidX Activity Compose", "Google", "https://developer.android.com/jetpack/androidx/releases/activity"),
                        Triple("AndroidX Navigation Compose", "Google", "https://developer.android.com/jetpack/compose/navigation"),
                        Triple("AndroidX Room", "Google", "https://developer.android.com/jetpack/androidx/releases/room"),
                        Triple("AndroidX Work Manager", "Google", "https://developer.android.com/jetpack/androidx/releases/work"),
                        Triple("AndroidX Security Crypto", "Google", "https://developer.android.com/jetpack/androidx/releases/security"),
                        Triple("AndroidX Biometric", "Google", "https://developer.android.com/jetpack/androidx/releases/biometric"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 7,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "Compose UI") {
                    listOf(
                        Triple("Jetpack Compose", "Google", "https://developer.android.com/jetpack/compose"),
                        Triple("Compose Material3", "Google", "https://developer.android.com/jetpack/androidx/releases/compose-material3"),
                        Triple("Compose Material Icons Extended", "Google", "https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 2,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "网络与序列化") {
                    listOf(
                        Triple("OkHttp", "Square, Inc.", "https://square.github.io/okhttp/"),
                        Triple("Retrofit", "Square, Inc.", "https://square.github.io/retrofit/"),
                        Triple("Moshi", "Square, Inc.", "https://github.com/square/moshi"),
                        Triple("Okio", "Square, Inc.", "https://square.github.io/okio/"),
                        Triple("PersistentCookieJar", "Fran Montiel", "https://github.com/franmontiel/PersistentCookieJar"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 4,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "安全") {
                    IOSListRow(
                        title = "JJWT",
                        subtitle = "by Okta, Inc.",
                        showDivider = false,
                        onClick = { onOpenLink("https://github.com/jwtk/jjwt") },
                    )
                }

                IOSGroupedList(title = "图片") {
                    IOSListRow(
                        title = "Coil",
                        subtitle = "by Coil-kt",
                        showDivider = false,
                        onClick = { onOpenLink("https://coil-kt.github.io/coil/") },
                    )
                }

                IOSGroupedList(title = "媒体") {
                    listOf(
                        Triple("AndroidX Media3 ExoPlayer", "Google", "https://developer.android.com/media/media3"),
                        Triple("Media3 UI", "Google", "https://developer.android.com/reference/androidx/media3/ui/PlayerView"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 1,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "文档预览") {
                    listOf(
                        Triple("JitViewer", "Apache License 2.0", "https://github.com/nicepkg/jit-viewer"),
                        Triple("AndroidDocViewer", "by seapeak233", "https://github.com/seapeak233/AndroidDocViewer"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = author,
                            showDivider = idx < 1,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "工具链") {
                    listOf(
                        Triple("Android Gradle Plugin", "Google", "https://developer.android.com/build/releases/gradle-plugin"),
                        Triple("Gradle", "Gradle, Inc.", "https://gradle.org/"),
                        Triple("KSP", "Google", "https://github.com/google/ksp"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 2,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }

                IOSGroupedList(title = "测试") {
                    listOf(
                        Triple("JUnit 4", "JUnit Team", "https://junit.org/junit4/"),
                        Triple("AndroidX Test Espresso", "Google", "https://developer.android.com/training/testing/espresso"),
                        Triple("MockWebServer", "Square, Inc.", "https://github.com/square/okhttp/tree/master/mockwebserver"),
                    ).forEachIndexed { idx, (name, author, url) ->
                        IOSListRow(
                            title = name,
                            subtitle = "by $author",
                            showDivider = idx < 2,
                            onClick = { onOpenLink(url) },
                        )
                    }
                }
            }
        }
    }
}
