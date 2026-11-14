package com.edgecos.mobileapp.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import kotlin.math.roundToInt

// ==================== iOS HIG 专用组件 ====================
// 这些组件是 iOS HIG 改造新增的，提供更地道的 iOS 视觉。
// 业务页可按需迁移使用。

/**
 * iOS 风格大标题导航栏。
 *
 * - 标题左对齐，34pt Bold（iOS Large Title）
 * - 可选左侧返回按钮（蓝色，带"返回"文字）
 * - 可选右侧操作按钮（蓝色文字）
 * - 高度自适应，含状态栏安全区
 *
 * @param title 标题
 * @param onBack 返回回调（null 时不显示返回按钮）
 * @param trailingIcon 右侧图标（可选）
 * @param onTrailingClick 右侧图标点击回调
 * @param applyStatusBarInset 是否处理状态栏安全区。
 *        - 独立页面（从 RootNav 进入）：true（默认），由本组件处理状态栏 inset
 *        - 嵌套子页面（如 MainScreen 内的 Home/Files/Profile）：false，由外层 Scaffold 处理
 */
@Composable
fun IOSLargeTitleNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
    applyStatusBarInset: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (applyStatusBarInset) {
                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = EdgeSpacing.lg),
        ) {
            // 顶部小导航栏（返回按钮 + 右侧操作）
            if (onBack != null || trailingIcon != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 左侧返回（灰色圆形按钮 + 白色图标）
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))  // 圆形
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onBack,
                                )
                                .background(MaterialTheme.colorScheme.secondary),  // 灰色填充
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onPrimary,  // 白色内容
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    // 右侧操作（灰色圆形按钮 + 白色图标）
                    if (trailingIcon != null && onTrailingClick != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(50))  // 圆形
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onTrailingClick,
                                )
                                .background(MaterialTheme.colorScheme.secondary),  // 灰色填充
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = trailingIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,  // 白色内容
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            // 大标题
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

/**
 * iOS 风格小标题导航栏（非大标题模式）。
 *
 * - 标题居中，17pt Semibold
 * - 左侧返回按钮（蓝色，带"返回"文字）
 * - 右侧操作文字（蓝色）
 * - 高度 44dp + 状态栏
 *
 * @param applyStatusBarInset 是否处理状态栏安全区（嵌套子页面传 false）
 */
@Composable
fun IOSNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    applyStatusBarInset: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        // 用 Box 叠加：标题按屏幕宽度居中（底层），返回按钮浮在左侧（上层）
        Box(
            modifier = Modifier
                .then(
                    if (applyStatusBarInset) {
                        Modifier.windowInsetsPadding(WindowInsets.statusBars)
                    } else {
                        Modifier
                    },
                )
                .fillMaxWidth()
                // 高度大于按钮（44dp），给按钮上下留安全距离，避免下方内容紧贴
                .height(44.dp)
                .padding(horizontal = EdgeSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            // 居中标题（按屏幕宽度居中）
            // 左右各预留返回按钮宽度 + 间距，避免长标题与按钮重叠
            val titleSidePadding = if (onBack != null) 36.dp + EdgeSpacing.md else 0.dp
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = titleSidePadding),
            )

            // 左侧返回（灰色圆形按钮 + 白色图标）
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))  // 圆形
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        )
                        .background(MaterialTheme.colorScheme.secondary),  // 灰色填充
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onPrimary,  // 白色内容
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // 右侧操作
            if (trailingText != null && onTrailingClick != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTrailingClick,
                        )
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * iOS 风格分组列表容器。
 *
 * - 分组背景色（surfaceVariant，浅灰）
 * - 内容卡片白色，圆角 12dp
 * - 组标题（可选）：左对齐 13pt，色 onSurfaceVariant
 * - 组脚注（可选）：左对齐 13pt，色 onSurfaceVariant
 *
 * @param title 分组标题（可选）
 * @param footer 分组脚注（可选）
 * @param content 列表项内容
 */
@Composable
fun IOSGroupedList(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EdgeSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (title != null) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
            )
        }
        // 卡片容器（Apple HIG：白色卡片放在浅灰分组背景上，靠色差建立层次，无需边框）
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

/**
 * iOS 风格分组列表项。
 *
 * - 最小高度 44dp
 * - 左侧前导图标（可选，圆角方块背景）
 * - 中间标题/副标题
 * - 右侧后缀文字 + chevron 箭头
 * - 项间分隔线（左缩进到文字对齐处）
 *
 * @param showDivider 是否显示底部分隔线（组内最后一项通常不显示）
 */
@Composable
fun IOSListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    iconColor: Color? = null,
    trailingText: String? = null,
    trailingIcon: ImageVector? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {},
) {
    // 图标色调：未指定时回退 primary（保持兼容），指定时用 Apple HIG 系统色（色彩缤纷）
    val resolvedIconColor = iconColor ?: MaterialTheme.colorScheme.primary
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                // 移除 Material 默认灰框（indication = null），保持 iOS 列表观感
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            if (leadingIcon != null) {
                // Apple HIG：彩色图标 + 对应色调 15% tinted 背景
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))  // iOS 26 圆角方块
                        .background(resolvedIconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = resolvedIconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = trailingIcon ?: Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = EdgeSpacing.lg),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

/**
 * iOS 风格按钮（Filled / Tinted / Plain 三种样式），对齐 Apple HIG。
 *
 * - [IOSButtonStyle.Filled]（默认）：systemGray 实心底 + 白字，作为中性主操作按钮
 *   （Apple HIG 中 systemBlue 用于关键主操作，systemGray 用于中性主操作）
 * - [IOSButtonStyle.Tinted]：systemGray 15% 底 + systemGray 字，次操作
 * - [IOSButtonStyle.Plain]：透明底 + systemBlue 字，链接操作（Apple 链接标志色）
 *
 * @param enabled 是否启用
 */
enum class IOSButtonStyle { Filled, Tinted, Plain }

@Composable
fun IOSButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IOSButtonStyle = IOSButtonStyle.Filled,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    isLoading: Boolean = false,
) {
    // Filled/Tinted 用 secondary（systemGray）作为中性操作色，避免大面积蓝色
    // Plain 用 primary（systemBlue）保持 Apple 链接标志色
    val accentColor = MaterialTheme.colorScheme.secondary
    val containerColor = when (style) {
        IOSButtonStyle.Filled -> if (enabled) accentColor
        else accentColor.copy(alpha = 0.3f)
        IOSButtonStyle.Tinted -> accentColor.copy(alpha = 0.15f)
        IOSButtonStyle.Plain -> Color.Transparent
    }
    val contentColor = when (style) {
        IOSButtonStyle.Filled -> MaterialTheme.colorScheme.onSecondary
        IOSButtonStyle.Tinted -> accentColor
        IOSButtonStyle.Plain -> if (enabled) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    }
    val shape = if (style == IOSButtonStyle.Plain) RoundedCornerShape(10.dp)
    else RoundedCornerShape(12.dp)  // iOS 26 更圆润的按钮

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick,
            )
            .padding(
                horizontal = if (style == IOSButtonStyle.Plain) EdgeSpacing.sm else EdgeSpacing.lg,
                vertical = if (style == IOSButtonStyle.Plain) EdgeSpacing.sm else 14.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                fontWeight = if (style == IOSButtonStyle.Plain) FontWeight.Medium else FontWeight.SemiBold,
            )
        }
    }
}

/**
 * iOS 风格分段控件（Segmented Control）— 灵动版。
 *
 * 【iOS 26 风格】与浮动 TabBar 视觉统一：
 * - 胶囊形外框（完全圆角），半透明背景
 * - 各选项等宽分配（weight=1f），高亮为移动指示器
 * - 选中项：品牌色 12% 背景 + 品牌色文字
 * - 未选中：透明背景 + 次要文字色
 * - 最小高度 36dp，触控目标≥44dp 由外层 padding 保证
 *
 * 【灵动交互】与底部导航 TabBar 同一套交互语言：
 * - 点击切换：指示器以轻弹弹簧滑动到目标（保留 LowBouncy 过冲形态，刚度提速）
 * - 水平拖动：指示器 1:1 跟手（StiffnessHigh 高刚度弹簧，几乎无滞后）
 * - 释放：按手指位置吸附到最近选项，弹簧携速接力，无"砖墙"感
 *
 * @param options 选项列表
 * @param selectedIndex 当前选中索引
 * @param onSelect 选择回调
 */
@Composable
fun IOSSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (options.isEmpty()) return
    val segmentHeight = 36.dp  // 控件高度（胶囊高度）
    val capsuleRadius = 18.dp  // 胶囊圆角 = 高度的一半（36dp / 2）
    val innerPadding = 3.dp    // 指示器与外框的间隙
    val itemSpacing = 2.dp
    val density = LocalDensity.current

    // 拖动状态：非 null 表示正在拖动，值为手指 x 坐标（px）
    var dragOffsetPx by remember { mutableStateOf<Float?>(null) }
    // 容器宽度（px），用于拖动时计算选项宽度和吸附索引
    var containerWidthPx by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(segmentHeight)
            .clip(RoundedCornerShape(capsuleRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                shape = RoundedCornerShape(capsuleRadius),
            )
            .onSizeChanged { containerWidthPx = it.width }
            // 水平拖动手势：拖动指示器切换选项（与 TabBar 同一交互）
            .pointerInput(options.size) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragOffsetPx = offset.x },
                    onDragEnd = {
                        dragOffsetPx?.let { pos ->
                            val innerPaddingPx = innerPadding.toPx()
                            val spacingPx = itemSpacing.toPx()
                            val itemWidthPx = (containerWidthPx - 2 * innerPaddingPx -
                                (options.size - 1) * spacingPx) / options.size
                            if (itemWidthPx > 0f) {
                                val index = ((pos - innerPaddingPx) / (itemWidthPx + spacingPx))
                                    .roundToInt()
                                    .coerceIn(0, options.size - 1)
                                onSelect(index)
                            }
                        }
                        dragOffsetPx = null
                    },
                    onDragCancel = { dragOffsetPx = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragOffsetPx = change.position.x
                    },
                )
            },
    ) {
        // 布局与手势统一使用 px 坐标系（containerWidthPx 由 onSizeChanged 提供）
        val innerPaddingPx = with(density) { innerPadding.toPx() }
        val spacingPx = with(density) { itemSpacing.toPx() }
        // 单项宽度（px）：容器宽 - 两侧间隙 - 项间距；未测量前为 0（指示器一帧不可见）
        val itemWidthPx = if (containerWidthPx > 0) {
            (containerWidthPx - 2 * innerPaddingPx - (options.size - 1) * spacingPx) / options.size
        } else {
            0f
        }
        val safeIndex = selectedIndex.coerceIn(0, options.size - 1)
        // 指示器可移动的最大偏移（px）
        val maxOffsetPx = (itemWidthPx + spacingPx) * (options.size - 1)

        // 拖动时：指示器中心跟随手指（高刚度弹簧，准 1:1 跟手，几乎无滞后）
        // 释放/点击时：指示器吸附到选中项（保留 LowBouncy 回弹形态，刚度提升使滑动更迅捷）
        val targetOffsetPx = if (dragOffsetPx != null && itemWidthPx > 0f) {
            (dragOffsetPx!! - innerPaddingPx - itemWidthPx / 2).coerceIn(0f, maxOffsetPx)
        } else {
            (itemWidthPx + spacingPx) * safeIndex
        }
        val indicatorOffset by animateDpAsState(
            targetValue = with(density) { targetOffsetPx.toDp() },
            animationSpec = if (dragOffsetPx != null) {
                spring(stiffness = Spring.StiffnessHigh)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium,
                )
            },
            label = "segmentIndicator",
        )

        // 滑动指示器（选中高亮，胶囊形），定位在内边距区域内
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset + innerPadding, y = innerPadding)
                .width(with(density) { itemWidthPx.toDp() })
                .height(segmentHeight - innerPadding * 2)
                .clip(RoundedCornerShape(capsuleRadius - 2.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        )

        // 选项（等宽，透明底，点击切换；文字颜色平滑过渡）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == safeIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "segmentTextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // 移除 Material 默认灰框（indication = null），只保留胶囊指示器
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
