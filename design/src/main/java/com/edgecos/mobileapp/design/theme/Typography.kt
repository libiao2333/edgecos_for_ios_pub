package com.edgecos.mobileapp.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==================== iOS HIG 字体规范 ====================
// 参考 Apple Human Interface Guidelines — Typography
// https://developer.apple.com/design/human-interface-guidelines/typography
//
// 字体选择策略：
// - iOS 使用 SF Pro，Android 无该字体
// - 本项目使用系统默认 SansSerif（Roboto），不打包 Inter 字体文件
//   原因：打包字体增加 APK 体积 ~400KB，且 Roboto 与 SF Pro 视觉接近度已足够
// - 若未来需要严格 Inter，可通过 Google Fonts Downloadable Fonts 动态加载
//
// 字号映射 iOS Text Styles → Material3 Typography：
// - displayLarge    ← Large Title 34pt Bold
// - displayMedium   ← Title 1 28pt Bold
// - displaySmall    ← Title 2 22pt Bold
// - headlineLarge   ← Title 3 20pt Semibold
// - headlineMedium  ← Headline 17pt Semibold
// - headlineSmall   ← Subhead 15pt Semibold
// - titleLarge      ← Title 3 20pt Semibold（页面标题）
// - titleMedium     ← Headline 17pt Semibold
// - titleSmall      ← Subhead 15pt Medium
// - bodyLarge       ← Body 17pt Regular
// - bodyMedium      ← Callout 16pt Regular
// - bodySmall       ← Footnote 13pt Regular
// - labelLarge      ← Subhead 15pt Medium
// - labelMedium     ← Footnote 13pt Medium
// - labelSmall      ← Caption 1 12pt Medium
//
// 动态字体：TextStyle 默认跟随系统 fontScale，无需额外配置。
// HIG Dynamic Type 通过 Android 系统字体大小设置自动生效。

private val Base = TextStyle(
    letterSpacing = (-0.4).sp,  // iOS 标题负字距，更紧凑
    lineHeight = 22.sp,
    fontFamily = FontFamily.Default,  // 系统默认（Roboto / SansSerif）
)

val EdgeTypography = Typography(
    // 大标题（Large Title）
    displayLarge = Base.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = 0.37.sp,  // iOS Large Title 字距
    ),
    // 标题 1
    displayMedium = Base.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.36.sp,
    ),
    // 标题 2
    displaySmall = Base.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp,
    ),
    // 标题 3
    headlineLarge = Base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp,
    ),
    // Headline
    headlineMedium = Base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Subhead
    headlineSmall = Base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // 页面标题（Title 3 语义）
    titleLarge = Base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.38.sp,
    ),
    // Headline 语义
    titleMedium = Base.copy(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Subhead 语义
    titleSmall = Base.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // 正文（Body 17pt）
    bodyLarge = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp,
    ),
    // Callout 16pt
    bodyMedium = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.3).sp,
    ),
    // Footnote 13pt
    bodySmall = Base.copy(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
    ),
    // Subhead Medium
    labelLarge = Base.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp,
    ),
    // Footnote Medium
    labelMedium = Base.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp,
    ),
    // Caption 1
    labelSmall = Base.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
)
