package com.edgecos.mobileapp.design.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== iOS HIG 配色方案 ====================
// 基色板定义于 Color.kt，此处组装为 Material3 ColorScheme。
// 保留 dynamicColor 选项（Android 12+ 莫奈），但默认关闭以维持 iOS 视觉一致性。

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    errorContainer = LightErrorContainer,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    errorContainer = DarkErrorContainer,
)

/**
 * EdgeCOS 应用主题（iOS HIG 风格）：统一取色入口。
 *
 * 4 种方案由调用方根据 [ThemeMode] 解析为 (darkTheme, dynamicColor) 传入：
 * - SYSTEM → (isSystemInDarkTheme, false)
 * - LIGHT  → (false, false)
 * - DARK   → (true, false)
 * - MONET  → (isSystemInDarkTheme, true)
 *
 * @param darkTheme 是否暗黑模式
 * @param dynamicColor 是否启用莫奈动态取色（Android 12+，跟随壁纸）
 */
@Composable
fun EdgeCOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // iOS 风格：状态栏图标色随主题切换
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            // 状态栏透明：让各页面背景（含预览页的纯黑/纯图）延伸到状态栏下方，实现真正全屏
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // 导航栏也透明，保证沉浸式全屏
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EdgeTypography,
        shapes = EdgeShapes,
        content = content,
    )
}
