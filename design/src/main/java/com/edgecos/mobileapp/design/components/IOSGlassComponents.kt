package com.edgecos.mobileapp.design.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import kotlin.math.roundToInt

// ==================== iOS 26 HIG 轻量级毛玻璃组件 ====================
//
// 【重要说明】Compose 的 Modifier.blur 模糊的是该 Composable 自身的渲染内容，
// 而非下层内容。因此无法像 iOS UINavigationBar 那样真正模糊下层滚动内容。
// 本组件采用"高透明度纯色叠加 + 微边框"模拟毛玻璃视觉：
// - 导航栏/TabBar 浮动在内容上方（z-index 高）
// - 半透明背景让下层内容隐约透出（alpha 0.85~0.92）
// - 微边框增强层次感
// - 性能开销极低，中低端机可稳定 60fps
//
// 【iOS 26 设计适配】
// - 浮动 TabBar：胶囊形状，四周有边距，浮在内容之上（内容可滚动到 TabBar 下方）
// - 浮动导航栏：顶部浮动，半透明背景
// - 无 Liquid Glass 动效（静态材质，不随滚动变化）

/**
 * 毛玻璃材质参数。
 *
 * @param overlayAlpha 纯色叠加层透明度（0.85~0.95，低于 0.85 透出内容过多影响可读性）
 * @param borderAlpha 微边框颜色透明度（0.1~0.3）
 */
data class GlassMaterial(
    val overlayAlpha: Float = 0.9f,
    val borderAlpha: Float = 0.15f,
) {
    companion object {
        /** 标准毛玻璃（导航栏）：高透明度叠加，保证文字可读性 */
        val Standard = GlassMaterial(overlayAlpha = 0.9f, borderAlpha = 0.15f)

        /** 浅毛玻璃（控制卡片）：中等透明度叠加 */
        val Light = GlassMaterial(overlayAlpha = 0.85f, borderAlpha = 0.1f)

        /** 强毛玻璃（浮动 TabBar）：最高透明度叠加，保证图标清晰 */
        val Strong = GlassMaterial(overlayAlpha = 0.92f, borderAlpha = 0.2f)
    }
}

/**
 * 应用毛玻璃材质到 Modifier。
 *
 * 模拟毛玻璃视觉（非真模糊）：
 * 1. background(surface.copy(alpha)) — 高透明度纯色叠加，下层内容隐约透出
 * 2. border(1.dp, outline.copy(alpha)) — 微边框增强层次感
 *
 * 注意：调用方需确保此 Modifier 的 Composable 浮动在内容上方（z-index 高），
 * 否则下层内容不会透出，半透明背景无意义。
 */
@Composable
fun Modifier.glassMaterial(
    material: GlassMaterial = GlassMaterial.Standard,
): Modifier = this
    .background(MaterialTheme.colorScheme.surface.copy(alpha = material.overlayAlpha))
    .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = material.borderAlpha),
        shape = RoundedCornerShape(0.dp),  // 调用方负责 clip 形状
    )

/**
 * iOS 风格毛玻璃导航栏（浮动式）。
 *
 * 适用场景：页面顶部，下方有滚动内容可透出。
 *
 * - 标题居中，17pt Semibold
 * - 左侧返回按钮（蓝色圆形背景 + 箭头图标）
 * - 右侧操作文字（蓝色）
 * - 高度 44dp + 状态栏安全区
 * - 背景使用毛玻璃材质（模糊下层滚动内容）
 *
 * @param material 毛玻璃材质（默认 Standard）
 */
@Composable
fun IOSGlassNavBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    material: GlassMaterial = GlassMaterial.Standard,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassMaterial(material),
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = EdgeSpacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧返回（圆形按钮）
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))  // 圆形
                        .clickable(onClick = onBack)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            // 居中标题
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            // 右侧操作
            if (trailingText != null && onTrailingClick != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onTrailingClick)
                        .padding(vertical = 6.dp),
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }
        }
    }
}

/**
 * iOS 26 风格浮动胶囊 TabBar（底部标签栏）。
 *
 * 【设计特点】
 * - 浮动在内容之上（不贴边），四周有 16dp 边距
 * - 胶囊形状（完全圆角）
 * - 毛玻璃材质（静态 blur + 半透明叠加）
 * - 内容可滚动到 TabBar 下方
 * - 高度 56dp + 导航栏安全区
 *
 * 【使用方式】
 * - 放在 Scaffold 的 bottomBar 中
 * - Scaffold 不需要给内容区域留底部 padding（内容可延伸到 TabBar 下方）
 * - 但需要给内容区域底部留出 TabBar 高度的额外空间（避免最后一条被完全遮挡）
 *
 * @param items 标签项列表
 * @param selectedIndex 当前选中索引
 * @param onSelect 选择回调
 * @param material 毛玻璃材质（默认 Strong，浮动 TabBar 需要更强模糊）
 */
@Composable
fun IOSGlassTabBar(
    items: List<IOSTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    material: GlassMaterial = GlassMaterial.Strong,
    applyOuterPadding: Boolean = true,
) {
    val tabHeight = 56.dp
    val capsuleRadius = tabHeight / 2  // 28dp，胶囊形
    val innerPadding = 4.dp  // 内部左右安全边距
    val itemSpacing = 4.dp  // 高亮项之间的间距
    val highlightVerticalPadding = innerPadding  // 上下安全留白与左右一致
    val highlightHeight = tabHeight - highlightVerticalPadding * 2  // 48dp
    val highlightRadius = highlightHeight / 2  // 24dp

    val density = LocalDensity.current

    // 拖动状态：非 null 表示正在拖动，值为手指 x 坐标（px）
    var dragOffsetPx by remember { mutableStateOf<Float?>(null) }
    // 容器宽度（px），用于拖动时计算 tab 宽度和吸附索引
    var containerWidthPx by remember { mutableStateOf(0) }

    val baseModifier = if (applyOuterPadding) {
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = EdgeSpacing.lg)
            .padding(bottom = EdgeSpacing.md)
    } else {
        modifier.fillMaxWidth()
    }

    Box(modifier = baseModifier) {
        // 胶囊容器 + 滑动指示器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tabHeight)
                .onSizeChanged { containerWidthPx = it.width }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(capsuleRadius),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                    clip = false,
                )
                .clip(RoundedCornerShape(capsuleRadius))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = material.overlayAlpha))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = material.borderAlpha),
                    shape = RoundedCornerShape(capsuleRadius),
                )
                // 水平拖动手势：拖动高光切换页面（iOS 风格）
                .pointerInput(items.size) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragOffsetPx = offset.x
                        },
                        onDragEnd = {
                            dragOffsetPx?.let { pos ->
                                // 计算手指位置对应的 tab 索引
                                val innerPaddingPx = with(density) { innerPadding.toPx() }
                                val spacingPx = with(density) { itemSpacing.toPx() }
                                val tabWidthPx = (containerWidthPx - 2 * innerPaddingPx -
                                    (items.size - 1) * spacingPx) / items.size
                                val tabIndex = ((pos - innerPaddingPx) / (tabWidthPx + spacingPx))
                                    .roundToInt()
                                    .coerceIn(0, items.size - 1)
                                onSelect(tabIndex)
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
            // 用 BoxWithConstraints 获取实际宽度，计算滑动指示器位置
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = innerPadding),
            ) {
                // 计算每个 tab 的宽度（等分，减去间距）
                val availableWidth = maxWidth
                val totalSpacing = itemSpacing * (items.size - 1)
                val tabWidth = (availableWidth - totalSpacing) / items.size

                // 拖动时：高光目标跟随手指（高刚度弹簧，准 1:1 跟手，几乎无滞后）
                // 释放时：高光目标为选中索引位置（保留 LowBouncy 回弹形态，刚度提升使吸附更迅捷）
                val targetOffset = if (dragOffsetPx != null && containerWidthPx > 0) {
                    with(density) {
                        (dragOffsetPx!! - innerPadding.toPx() - tabWidth.toPx() / 2).toDp()
                    }
                } else {
                    (tabWidth + itemSpacing) * selectedIndex
                }
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = if (dragOffsetPx != null) {
                        spring(stiffness = Spring.StiffnessHigh)
                    } else {
                        spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMedium,
                        )
                    },
                    label = "tabIndicator",
                )

                // 滑动指示器（绝对定位，上下留白）
                // 选中态高亮用 secondary（systemGray），符合 Apple HIG 灰色选中风格
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(tabWidth)
                        .height(highlightHeight)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(highlightRadius))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                )

                // tab 项（等宽，无独立背景，点击切换；移除灰色 ripple 以保证沉浸感）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(tabHeight),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = index == selectedIndex
                        val tabInteractionSource = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = tabInteractionSource,
                                    indication = null,  // 移除灰色 ripple，保证沉浸感
                                    onClick = { onSelect(index) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                modifier = Modifier,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.icon,
                                    contentDescription = item.label,
                                    // 选中态用 onSurface（label 黑色），未选中用 onSurfaceVariant
                                    // Apple HIG TabBar 选中态用 label 色而非 tint 色
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 项数据。
 *
 * @param icon 未选中图标
 * @param selectedIcon 选中图标
 * @param label 标签文字
 */
data class IOSTabItem(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String,
)

/**
 * iOS 风格毛玻璃卡片。
 *
 * 适用场景：浮层卡片（如音乐播放器迷你栏、控制中心卡片）。
 *
 * - 12dp 圆角
 * - 背景使用毛玻璃材质
 * - 内容由调用方提供
 *
 * 注意：此卡片仅用于浮层场景，不要用于普通列表项。
 * 普通列表项用 [EdgeCard]（纯色背景）即可。
 *
 * @param material 毛玻璃材质（默认 Light）
 */
@Composable
fun IOSGlassCard(
    modifier: Modifier = Modifier,
    material: GlassMaterial = GlassMaterial.Light,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .glassMaterial(material),
    ) {
        content()
    }
}

/**
 * 沉浸式预览页顶部栏（浮动式）。
 *
 * 【iOS 26 沉浸式阅读风格】
 * - 返回按钮始终浮动在内容之上（圆形 tinted 背景），便于随时返回
 * - 标题栏仅在内容滚动到顶部时显示（淡入淡出），滚动后隐藏以实现沉浸式阅读
 * - 右侧可放自定义操作内容（页码指示器、切换按钮等），始终可见
 * - 状态栏安全区由调用方处理（本组件只应用 statusBars padding）
 *
 * @param title 标题（通常为文件名）
 * @param onBack 返回回调
 * @param titleVisible 标题是否可见（内容滚动到顶部时为 true）
 * @param trailingContent 右侧自定义内容（优先级高于 trailingIcon）
 * @param trailingIcon 右侧操作图标（trailingContent 为 null 时使用）
 * @param onTrailingClick 右侧操作回调（trailingContent 为 null 时使用）
 */
@Composable
fun IOSImmersivePreviewHeader(
    title: String,
    onBack: () -> Unit,
    titleVisible: Boolean,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    // 标题栏透明度：滚动到顶部时显示，滚动后淡出（提速：StiffnessMediumLow → StiffnessMedium）
    val barAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "previewHeaderAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        // 标题栏背景 + 标题文字（淡入淡出，无分割线，保持沉浸感）
        if (barAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .alpha(barAlpha)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = EdgeSpacing.xxl + 36.dp + EdgeSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 左侧返回按钮（始终可见，浮动圆形 tinted 背景）
        Box(
            modifier = Modifier
                .padding(start = EdgeSpacing.lg, top = 4.dp, bottom = 4.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        // 右侧内容（始终可见，浮动）
        // 优先使用自定义 trailingContent，其次回退到 trailingIcon 圆形按钮
        if (trailingContent != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = EdgeSpacing.lg, top = 4.dp, bottom = 4.dp),
            ) {
                trailingContent()
            }
        } else if (trailingIcon != null && onTrailingClick != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = EdgeSpacing.lg, top = 4.dp, bottom = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onTrailingClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * 浮动页数指示器（iOS 26 沉浸式风格）。
 *
 * 【设计特点】
 * - 浮动在预览页右上角（作为 IOSImmersivePreviewHeader 的 trailingContent）
 * - 胶囊形 tinted 背景（primary alpha 0.15f），与返回按钮风格一致
 * - 排版：当前页数 / 总页数（斜杠分隔）
 * - 数字变化使用 AnimatedContent 平滑过渡（淡入淡出 + 轻微纵向位移）
 *
 * @param currentPage 当前页码（从 1 开始）
 * @param totalPages 总页数
 * @param modifier 修饰符（调用方负责定位）
 * @param visible 是否可见（页数无效时隐藏）
 */
@Composable
fun IOSPageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    visible: Boolean = shouldShowIndicator(currentPage, totalPages),
) {
    if (!visible) return
    // 整体淡入淡出（提速：StiffnessMediumLow → StiffnessMedium）
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pageIndicatorAlpha",
    )
    Box(
        modifier = modifier
            .alpha(alpha)
            .shadow(
                elevation = 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.10f),
                clip = false,
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = EdgeSpacing.md, vertical = EdgeSpacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 当前页数（带动画；显式快速弹簧，避免默认 StiffnessMediumLow 的滞后感）
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it / 4 } +
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))) togetherWith
                        (slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it / 4 } +
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "currentPage",
            ) { page ->
                Text(
                    text = page.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = " / ",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            // 总页数（带动画，文档加载完成时由 0 变为实际页数；同样使用快速弹簧）
            AnimatedContent(
                targetState = totalPages,
                transitionSpec = {
                    (slideInVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { it / 4 } +
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMedium))) togetherWith
                        (slideOutVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) { -it / 4 } +
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "totalPages",
            ) { pages ->
                Text(
                    text = pages.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** 页数指示器可见性判定：页数有效且总页数 > 0 时显示 */
private fun shouldShowIndicator(currentPage: Int, totalPages: Int): Boolean {
    return totalPages > 0 && currentPage in 1..totalPages
}

/**
 * 路径栏（macOS Finder 风格面包屑）。
 *
 * 【设计特点】
 * - 与顶部导航栏对齐的独立水平 bar（同 horizontal padding，背景与导航栏一致）
 * - 面包屑形式：根目录 › 子目录 › 子目录 › 当前
 * - 每段为可点击的胶囊按钮；当前段（默认最后一段）高亮（primary tinted）
 * - 段间用 KeyboardArrowRight 分隔（遵循项目约定，不使用 ChevronRight）
 * - 路径过长时水平滚动（自动滚动到当前段）
 * - 移除灰色 ripple，保证沉浸感
 *
 * @param segments 路径段列表，如 ["根目录", "Documents", "Work"]
 * @param onSegmentClick 点击第 i 段回调（0 = 根目录）
 * @param currentIndex 当前高亮段索引（默认最后一段）
 */
@Composable
fun IOSPathBar(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    currentIndex: Int = segments.lastIndex.coerceAtLeast(0),
) {
    if (segments.isEmpty()) return
    val scrollState = rememberScrollState()
    // 路径变化时自动滚动到当前段（让当前段可见）
    LaunchedEffect(currentIndex, segments.size) {
        if (segments.size > 0) {
            // 估算当前段大致位置（每段约 80dp），滚动到可见区域
            // 提速：StiffnessMediumLow → StiffnessMedium
            scrollState.animateScrollTo(
                (currentIndex * 320).coerceAtMost(scrollState.maxValue),
                spring(stiffness = Spring.StiffnessMedium),
            )
        }
    }

    // 路径树背景跟随页面背景（systemGroupedBackground），与文件列表卡片（白色）形成层次
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.xs),
        ) {
            segments.forEachIndexed { index, name ->
                if (index > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                val isCurrent = index == currentIndex
                val interactionSource = remember { MutableInteractionSource() }
                // 段落背景色与文字样式平滑过渡（参考 TabBar 切换动画；提速：StiffnessLow → StiffnessMedium）
                val segmentBg by animateColorAsState(
                    targetValue = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else Color.Transparent,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "pathSegBg",
                )
                val textColor by animateColorAsState(
                    targetValue = if (isCurrent) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    label = "pathSegColor",
                )
                Box(
                    modifier = Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(50))
                        .background(segmentBg)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSegmentClick(index) },
                        )
                        .padding(horizontal = EdgeSpacing.md, vertical = EdgeSpacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
