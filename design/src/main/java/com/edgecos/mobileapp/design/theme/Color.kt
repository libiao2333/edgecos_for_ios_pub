package com.edgecos.mobileapp.design.theme

import androidx.compose.ui.graphics.Color

// ==================== iOS HIG 系统色板 ====================
// 参考 Apple Human Interface Guidelines — Color
// https://developer.apple.com/design/human-interface-guidelines/color

// 系统蓝（Accent）— iOS 默认强调色
val IOSBlue = Color(0xFF007AFF)          // light
val IOSBlueDark = Color(0xFF0A84FF)      // dark

// 系统绿（Success）
val IOSGreen = Color(0xFF34C759)
val IOSGreenDark = Color(0xFF30D158)

// 系统红（Error / Destructive）
val IOSRed = Color(0xFFFF3B30)
val IOSRedDark = Color(0xFFFF453A)

// 系统橙（Warning）
val IOSOrange = Color(0xFFFF9500)
val IOSOrangeDark = Color(0xFFFF9F0A)

// 系统紫（Purple）
val IOSPurple = Color(0xFFAF52DE)
val IOSPurpleDark = Color(0xFFBF5AF2)

// 系统粉（Pink）
val IOSPink = Color(0xFFFF2D55)
val IOSPinkDark = Color(0xFFFF375F)

// 系统青（Teal）
val IOSTeal = Color(0xFF30B0C7)
val IOSTealDark = Color(0xFF40C8E0)

// ==================== 中性灰阶（iOS System Gray）====================
// Light
val IOSGray = Color(0xFF8E8E93)
val IOSGray2 = Color(0xFFAEAEB2)
val IOSGray3 = Color(0xFFC7C7CC)
val IOSGray4 = Color(0xFFD1D1D6)
val IOSGray5 = Color(0xFFE5E5EA)
val IOSGray6 = Color(0xFFF2F2F7)

// Dark
val IOSGrayDark = Color(0xFF98989F)
val IOSGray2Dark = Color(0xFF636366)
val IOSGray3Dark = Color(0xFF48484A)
val IOSGray4Dark = Color(0xFF3A3A3C)
val IOSGray5Dark = Color(0xFF2C2C2E)
val IOSGray6Dark = Color(0xFF1C1C1E)

// 文本与背景
val IOSLabel = Color(0xFF000000)             // light 主文本
val IOSLabelDark = Color(0xFFFFFFFF)         // dark 主文本
val IOSSecondaryLabel = Color(0x993C3C43)    // light 次要文本 60%
val IOSSecondaryLabelDark = Color(0x99EBEBF5) // dark 次要文本 60%
val IOSTertiaryLabel = Color(0x4D3C3C43)     // light 三级文本 30%
val IOSTertiaryLabelDark = Color(0x4DEBEBF5) // dark 三级文本 30%

// 背景层级（Light）
val IOSSystemBackground = Color(0xFFFFFFFF)
val IOSSecondarySystemBackground = Color(0xFFF2F2F7)    // 分组背景
val IOSTertiarySystemBackground = Color(0xFFFFFFFF)     // 卡片内

// 背景层级（Dark）
// iOS HIG 深色背景用深灰而非纯黑，减少视觉疲劳
val IOSSystemBackgroundDark = Color(0xFF1C1C1E)
val IOSSecondarySystemBackgroundDark = Color(0xFF2C2C2E)
val IOSTertiarySystemBackgroundDark = Color(0xFF3A3A3C)

// 分隔线（iOS Separator）
val IOSSeparator = Color(0x4C3C3C43)        // light 0.29x ≈ 0x4C
val IOSSeparatorDark = Color(0xA6545458)    // dark 0.65x ≈ 0xA6

// ==================== 语义映射（供 MaterialTheme 使用）====================
// 全面对齐 Apple Human Interface Guidelines 配色：
// - primary = systemBlue（#007AFF / #0A84FF）：作为 tint 强调色，用于链接/选中态/可交互元素，
//   这是 Apple 的标志性强调色，克制使用以保证视觉舒适
// - background = systemGroupedBackground（#F2F2F7 / #1C1C1E）：分组页面背景（浅灰），
//   与卡片白色形成层次差，这是 Apple 卡片式列表的经典视觉
// - surface = secondarySystemGroupedBackground（#FFFFFF / #2C2C2E）：卡片背景（纯白/深灰），
//   靠背景色差建立层次，无需边框
// Light
val LightPrimary = IOSBlue                 // #007AFF systemBlue，Apple 标志强调色
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE8F0FE)
val LightOnPrimaryContainer = Color(0xFF001D3A)
val LightSecondary = IOSGray
val LightOnSecondary = Color(0xFFFFFFFF)
// systemGroupedBackground：浅灰分组背景，与白色卡片形成层次
val LightBackground = IOSSecondarySystemBackground   // #F2F2F7
val LightOnBackground = IOSLabel
// secondarySystemGroupedBackground：卡片背景（纯白），与浅灰背景形成层次差
val LightSurface = IOSTertiarySystemBackground      // #FFFFFF
val LightOnSurface = IOSLabel
val LightSurfaceVariant = IOSGray6
val LightOnSurfaceVariant = IOSSecondaryLabel
val LightOutline = IOSGray3
val LightOutlineVariant = Color(0xFFE5E5EA)         // separator，Apple 分隔线
val LightError = IOSRed
val LightErrorContainer = Color(0xFFFFEDEA)
val LightSuccess = IOSGreen
val LightWarning = IOSOrange

// Dark
val DarkPrimary = IOSBlueDark              // #0A84FF systemBlue (dark)
val DarkOnPrimary = Color(0xFFFFFFFF)
val DarkPrimaryContainer = Color(0xFF0A2540)
val DarkOnPrimaryContainer = Color(0xFFB8D4FF)
val DarkSecondary = IOSGrayDark
val DarkOnSecondary = Color(0xFF000000)
// systemGroupedBackground (dark)：深灰分组背景
val DarkBackground = IOSSecondarySystemBackgroundDark  // #2C2C2E
val DarkOnBackground = IOSLabelDark
// secondarySystemGroupedBackground (dark)：卡片背景（更深一档），与背景形成层次
val DarkSurface = IOSTertiarySystemBackgroundDark     // #3A3A3C
val DarkOnSurface = IOSLabelDark
val DarkSurfaceVariant = IOSGray6Dark
val DarkOnSurfaceVariant = IOSSecondaryLabelDark
val DarkOutline = IOSGray3Dark
val DarkOutlineVariant = Color(0xFF545458)            // separator (dark)
val DarkError = IOSRedDark
val DarkErrorContainer = Color(0xFF4C0D0A)
val DarkSuccess = IOSGreenDark
val DarkWarning = IOSOrangeDark

// ==================== 兼容别名（向后兼容旧代码引用）====================
// 保留旧名称，指向新色板，避免业务页大面积报错
val BrandPrimary = IOSBlue
val BrandPrimaryDark = IOSBlueDark
val BrandPrimaryLight = Color(0xFF4A90E8)
val BrandOnPrimary = Color(0xFFFFFFFF)

val Neutral0 = IOSSystemBackground
val Neutral50 = IOSGray6
val Neutral100 = IOSGray5
val Neutral200 = IOSGray4
val Neutral300 = IOSGray3
val Neutral400 = IOSGray2
val Neutral500 = IOSGray
val Neutral600 = Color(0xFF6B7280)
val Neutral700 = Color(0xFF48484A)
val Neutral800 = IOSGray6Dark
val Neutral900 = IOSLabel
val Neutral1000 = Color(0xFF000000)

val Success = IOSGreen
val Warning = IOSOrange
val Error = IOSRed
val Info = IOSBlue
