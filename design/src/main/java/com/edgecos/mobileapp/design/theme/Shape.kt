package com.edgecos.mobileapp.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// ==================== iOS HIG 圆角规范 ====================
// 参考 Apple Human Interface Guidelines — Materials and Apppearance
//
// iOS 使用连续曲率圆角（Continuous Corner），Compose 用 RoundedCornerShape 近似。
// 圆角半径：
// - 控件/按钮/输入框：10dp
// - 卡片/分组列表：12dp
// - 大卡片/Sheet 顶部：16dp
// - 全屏模态/底部 Sheet：24dp
// - 胶囊形（Tag/开关）：完全圆角

val EdgeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),    // 小标签
    small = RoundedCornerShape(10.dp),        // 按钮/输入框/小卡片
    medium = RoundedCornerShape(12.dp),       // 卡片/分组列表
    large = RoundedCornerShape(16.dp),        // 大卡片/Sheet 顶部
    extraLarge = RoundedCornerShape(24.dp),   // 全屏模态
)
