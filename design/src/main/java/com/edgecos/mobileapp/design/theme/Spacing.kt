package com.edgecos.mobileapp.design.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ==================== iOS HIG 间距与尺寸规范 ====================
// 参考 Apple Human Interface Guidelines — Layout
//
// 间距系统：
// - 页边距（horizontal margin）：16dp（iPhone 标准）
// - 组间间距（section gap）：20dp
// - 控件间间距：8dp / 12dp
// - 列表项内边距：16dp horizontal
//
// 尺寸规范：
// - 最小触控目标：44dp（iOS 最小可点击区域）
// - NavigationBar 高度：44dp
// - TabBar 高度：49dp
// - 列表项最小高度：44dp（单行）/ 60dp（双行）

object EdgeSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp       // 标准页边距
    val xl: Dp = 20.dp       // 组间间距
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 48.dp
}

/**
 * iOS HIG 圆角半径（dp 单位，供非 Compose 场景使用）。
 */
object EdgeRadius {
    const val none = 0
    const val small = 6       // 小标签
    const val medium = 10     // 按钮/输入框
    const val large = 12      // 卡片/分组列表
    const val xlarge = 16     // 大卡片/Sheet 顶部
    const val xxlarge = 24    // 全屏模态
}

/**
 * iOS HIG 控件尺寸。
 */
object EdgeSize {
    val iconSmall = 16.dp
    val iconMedium = 22.dp
    val iconLarge = 28.dp
    val touchTarget = 44.dp         // iOS 最小触控目标
    val buttonHeight = 50.dp        // iOS 按钮高度（含充足内边距）
    val buttonHeightSmall = 36.dp
    val inputHeight = 56.dp
    val navBarHeight = 44.dp        // iOS NavigationBar
    val tabBarHeight = 49.dp        // iOS TabBar
    val bottomBarHeight = 83.dp     // TabBar + 安全区
    val cardMinHeight = 44.dp       // 列表项最小高度
    val listRowHeight = 44.dp       // 单行列表项
    val listRowHeightDouble = 60.dp // 双行列表项
}
