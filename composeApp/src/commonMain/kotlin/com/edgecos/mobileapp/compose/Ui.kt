package com.edgecos.mobileapp.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToLong

// ==================== 主题 ====================

/** iOS 系统色（与 Android `design/theme/Color.kt` 的色值逐项对齐）。 */
object IOSColors {
    // Apple HIG 系统强调色
    val Blue = Color(0xFF007AFF)
    val BlueDark = Color(0xFF0A84FF)
    val Green = Color(0xFF34C759)
    val Orange = Color(0xFFFF9500)
    val Pink = Color(0xFFFF2D55)
    val Purple = Color(0xFFAF52DE)
    val Teal = Color(0xFF30B0C7)
    val Red = Color(0xFFFF3B30)
    val Gray = Color(0xFF8E8E93)
    val Gray2 = Color(0xFFAEAEB2)
    val Gray3 = Color(0xFFC7C7CC)
    val Gray4 = Color(0xFFD1D1D6)
    val Gray5 = Color(0xFFE5E5EA)
    val Gray6 = Color(0xFFF2F2F7)

    // 深色中性色
    val GrayDark = Color(0xFF98989F)
    val Gray2Dark = Color(0xFF636366)
    val Gray3Dark = Color(0xFF48484A)
    val Gray4Dark = Color(0xFF3A3A3C)
    val Gray5Dark = Color(0xFF2C2C2E)
    val Gray6Dark = Color(0xFF1C1C1E)

    // 语义背景（对应 systemGroupedBackground / secondarySystemGroupedBackground）
    val BackgroundLight = Color(0xFFF2F2F7)
    val SurfaceLight = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF1C1C1E)
    val SurfaceDark = Color(0xFF2C2C2E)
    val SurfaceVariantDark = Color(0xFF3A3A3C)

    // 文本
    val LabelLight = Color(0xFF000000)
    val LabelDark = Color(0xFFFFFFFF)
    val SecondaryLabelLight = Color(0x993C3C43)
    val SecondaryLabelDark = Color(0x99EBEBF5)

    // 分隔线
    val SeparatorLight = Color(0xFFE5E5EA)
    val SeparatorDark = Color(0xFF545458)
}

@Composable
fun EdgeTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        // iOS 深色体系：纯黑底 + 两级卡片灰，避免整屏糊成一片
        darkColorScheme(
            primary = IOSColors.BlueDark,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0A2540),
            onPrimaryContainer = Color(0xFFB8D4FF),
            secondary = IOSColors.GrayDark,
            onSecondary = Color.Black,
            background = Color(0xFF000000),
            onBackground = Color.White,
            surface = Color(0xFF1C1C1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2E),
            onSurfaceVariant = IOSColors.SecondaryLabelDark,
            outline = Color(0xFF48484A),
            outlineVariant = Color(0xFF38383A),
            error = Color(0xFFFF453A),
        )
    } else {
        lightColorScheme(
            primary = IOSColors.Blue,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8F0FE),
            onPrimaryContainer = Color(0xFF001D3A),
            secondary = IOSColors.Gray,
            onSecondary = Color.White,
            background = IOSColors.BackgroundLight,
            onBackground = IOSColors.LabelLight,
            surface = IOSColors.SurfaceLight,
            onSurface = IOSColors.LabelLight,
            surfaceVariant = Color(0xFFEFEFF4),
            onSurfaceVariant = IOSColors.SecondaryLabelLight,
            outline = IOSColors.Gray3,
            outlineVariant = IOSColors.SeparatorLight,
            error = IOSColors.Red,
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

// ==================== Liquid Glass（iOS 26） ====================

/**
 * 液态玻璃材质参数。
 *
 * @param tintAlpha 底色叠加透明度（越低透出越多，0.5~0.85 之间最接近 iOS 26 观感）
 * @param highlightAlpha 边缘高光强度（模拟玻璃折射的镜面反射）
 * @param shadowElevation 浮层投影高度
 */
data class GlassMaterial(
    val tintAlpha: Float = 0.72f,
    val highlightAlpha: Float = 0.5f,
    val shadowElevation: Dp = 12.dp,
) {
    companion object {
        /** 标准浮层（导航栏） */
        val Standard = GlassMaterial(tintAlpha = 0.78f, highlightAlpha = 0.45f, shadowElevation = 6.dp)

        /** 轻量卡片 */
        val Light = GlassMaterial(tintAlpha = 0.62f, highlightAlpha = 0.35f, shadowElevation = 4.dp)

        /** 浮动 TabBar：需要更强的不透明度保证图标清晰 */
        val Strong = GlassMaterial(tintAlpha = 0.86f, highlightAlpha = 0.6f, shadowElevation = 16.dp)
    }
}

/**
 * 液态玻璃材质：底色半透明 + 顶部镜面高光渐变 + 一圈细高光描边 + 柔和投影。
 *
 * Compose 无法模糊「自身下方」的内容（`Modifier.blur` 只会模糊自己），
 * 因此这里与 Android 端保持一致：用高透明度表面 + 高光/描边模拟玻璃质感，
 * 同时叠加一层从左上到右下的白色渐变，制造 iOS 26 的镜面折射观感。
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    material: GlassMaterial = GlassMaterial.Standard,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlight = if (isDark) Color.White.copy(alpha = material.highlightAlpha * 0.55f)
    else Color.White.copy(alpha = material.highlightAlpha)

    return this
        .shadow(
            elevation = material.shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.16f),
            clip = false,
        )
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface.copy(alpha = material.tintAlpha))
        .background(
            Brush.linearGradient(
                colors = listOf(
                    highlight.copy(alpha = highlight.alpha * 0.9f),
                    Color.Transparent,
                    highlight.copy(alpha = highlight.alpha * 0.25f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    highlight,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                ),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
            shape = shape,
        )
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

// ==================== 通用组件 ====================

/**
 * iOS HIG 风格导航栏：44dp 高、标题绝对居中（17sp Semibold）、左侧圆形返回按钮。
 *
 * 材质为液态玻璃，页面滚动时内容会从下方隐约透出。
 */
@Composable
fun NavBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(0.dp), GlassMaterial.Standard),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp),
            )
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "‹",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** NavBar 右侧文字按钮（Apple HIG tint 蓝，17sp Regular）。 */
@Composable
fun NavTextAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

/** 底部 Tab 项。 */
data class GlassTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

/**
 * iOS 26 风格浮动胶囊 TabBar（与 Android `IOSGlassTabBar` 视觉一致）。
 *
 * - 浮动在内容下方，四周留白，胶囊形
 * - 液态玻璃材质：半透明底 + 顶部高光渐变 + 细描边
 * - 选中态用 label 色 + 淡色 pill（Apple HIG TabBar 约定）
 */
@Composable
fun GlassTabBar(
    items: List<GlassTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 6.dp, bottom = 10.dp)
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.86f else 0.92f),
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.10f else 0.34f),
                        Color.White.copy(alpha = if (isDark) 0.02f else 0.04f),
                        Color.Transparent,
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.55f else 0.18f),
                shape = shape,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        else Color.Transparent,
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = item.label,
                        tint = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 页面容器：可滚动列（含底部留白）。 */
@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(20.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 32.dp),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** iOS Inset Grouped List：可选分组标题 + 圆角卡片。 */
@Composable
fun GroupedList(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (title != null) {
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/** 卡片内分隔线（左侧缩进，iOS 风格）。 */
@Composable
fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** 导航行：前导彩色图标方块 + 文案 + 右侧值 + chevron。 */
@Composable
fun NavRow(
    label: String,
    icon: ImageVector? = null,
    iconColor: Color = IOSColors.Blue,
    value: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                label,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Text(
                    value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onClick != null) {
                Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (showDivider) RowDivider()
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text)
    }
}

@Composable
fun EmptyState(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("重试") }
    }
}

/** 云朵 Logo（Canvas 绘制，避免引入图片资源）。 */
@Composable
fun CloudLogo(size: Int = 96) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size.dp)) {
        val blue = IOSColors.Blue
        val w = this.size.width
        val h = this.size.height
        drawCircle(blue, radius = w * 0.22f, center = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.58f))
        drawCircle(blue, radius = w * 0.28f, center = androidx.compose.ui.geometry.Offset(w * 0.55f, h * 0.5f))
        drawCircle(blue, radius = w * 0.2f, center = androidx.compose.ui.geometry.Offset(w * 0.75f, h * 0.62f))
        drawRoundRect(
            color = blue,
            topLeft = androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
        )
    }
}

/**
 * 文件类型图标。
 *
 * iOS 优先使用系统 SF Symbols（`folder.fill` / `photo.fill` …），
 * 桌面预览没有 SF Symbols，回退到 Material 图标，保证两端都能构建。
 */
@Composable
fun FileBadge(isDirectory: Boolean, fileName: String = "") {
    val symbolName = symbolForFile(fileName, isDirectory)
    val tint = tintForFile(fileName, isDirectory)
    val bitmap = remember(symbolName) { loadSystemSymbol(symbolName, 20.0) }

    Box(
        modifier = Modifier.size(34.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                colorFilter = ColorFilter.tint(tint),
                modifier = Modifier.size(24.dp),
            )
        } else {
            Icon(
                imageVector = if (isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun AvatarBadge(text: String, size: Int = 48) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(IOSColors.Blue),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** 分段控件（iOS Segmented Control 风格）。 */
@Composable
fun SegmentedControl(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ==================== 工具函数 ====================

fun joinPath(base: String, name: String): String =
    if (base == "/") "/$name" else "$base/$name"

fun describeError(error: Throwable): String =
    error.message?.takeIf { it.isNotBlank() } ?: "操作失败，请稍后重试"

fun formatBytes(raw: String): String {
    val bytes = raw.toLongOrNull() ?: return raw
    return formatBytes(bytes)
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) {
        "$bytes B"
    } else {
        "${(value * 10).roundToLong() / 10.0} ${units[index]}"
    }
}

// ==================== SHA-256（纯 Kotlin，供秒传判断使用） ====================

private val SHA256_K = intArrayOf(
    0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(), 0x3956c25b, 0x59f111f1,
    0x923f82a4.toInt(), 0xab1c5ed5.toInt(), 0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(), 0xe49b69c1.toInt(),
    0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc,
    0x76f988da, 0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
    0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967, 0x27b70a85, 0x2e1b2138,
    0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
    0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(), 0xd192e819.toInt(),
    0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070, 0x19a4c116, 0x1e376c08, 0x2748774c,
    0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, 0x748f82ee, 0x78a5636f,
    0x84c87814.toInt(), 0x8cc70208.toInt(), 0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(),
    0xc67178f2.toInt(),
)

private fun Int.rotateRight(bits: Int): Int = (this ushr bits) or (this shl (32 - bits))

/** 计算 SHA-256 的小写十六进制串（与 Android `Sha256Calculator` 的 hex 输出一致）。 */
fun sha256Hex(input: ByteArray): String {
    val bitLength = input.size.toLong() * 8
    val padLength = (56 - (input.size + 1) % 64 + 64) % 64
    val message = ByteArray(input.size + 1 + padLength + 8)
    input.copyInto(message)
    message[input.size] = 0x80.toByte()
    for (i in 0 until 8) {
        message[message.size - 1 - i] = ((bitLength ushr (8 * i)) and 0xFF).toByte()
    }

    val hash = intArrayOf(
        0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
        0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19,
    )
    val w = IntArray(64)

    var offset = 0
    while (offset < message.size) {
        for (i in 0 until 16) {
            val j = offset + i * 4
            w[i] = ((message[j].toInt() and 0xFF) shl 24) or
                ((message[j + 1].toInt() and 0xFF) shl 16) or
                ((message[j + 2].toInt() and 0xFF) shl 8) or
                (message[j + 3].toInt() and 0xFF)
        }
        for (i in 16 until 64) {
            val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
            val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
            w[i] = w[i - 16] + s0 + w[i - 7] + s1
        }

        var a = hash[0]
        var b = hash[1]
        var c = hash[2]
        var d = hash[3]
        var e = hash[4]
        var f = hash[5]
        var g = hash[6]
        var h = hash[7]

        for (i in 0 until 64) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val ch = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + ch + SHA256_K[i] + w[i]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val maj = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + maj
            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        hash[0] += a
        hash[1] += b
        hash[2] += c
        hash[3] += d
        hash[4] += e
        hash[5] += f
        hash[6] += g
        hash[7] += h
        offset += 64
    }

    return hash.joinToString("") { value ->
        ((value.toLong() and 0xFFFFFFFFL) or 0x100000000L).toString(16).substring(1)
    }
}

// ==================== 平台桥接 ====================

/** 打开外部链接（下载直链 / 协议网页）。 */
expect fun openExternalUrl(url: String)

/** 用户通过系统文件选择器选中的待上传文件（内容已整体读入内存）。 */
class PickedFile(val name: String, val bytes: ByteArray)

/** 打开系统文件选择器；用户取消时返回 null。 */
expect suspend fun pickFileForUpload(): PickedFile?

/**
 * 加载系统符号图标（iOS 为 SF Symbols），返回位图；平台不支持时返回 null。
 *
 * @param name SF Symbols 名称，如 `folder.fill`
 * @param pointSize 符号点数（决定位图分辨率）
 */
expect fun loadSystemSymbol(name: String, pointSize: Double): ImageBitmap?

/** API 时间格式化：`2026-07-28T16:15:47.484Z` → `2026-07-28 16:15`（与 Android 一致）。 */
fun formatApiTime(raw: String): String {
    if (raw.isBlank()) return raw
    val normalized = raw.replace('T', ' ')
    return if (normalized.length >= 16) normalized.substring(0, 16) else raw
}

/** 按文件名/类型选择 SF Symbols 图标名。 */
fun symbolForFile(name: String, isDirectory: Boolean): String {
    if (isDirectory) return "folder.fill"
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "svg" -> "photo.fill"
        "mp4", "mov", "mkv", "avi", "webm", "m4v" -> "video.fill"
        "mp3", "wav", "flac", "m4a", "aac", "ogg" -> "music.note"
        "pdf" -> "doc.richtext.fill"
        "doc", "docx", "txt", "md", "rtf" -> "doc.text.fill"
        "xls", "xlsx", "csv" -> "tablecells.fill"
        "ppt", "pptx", "key" -> "rectangle.on.rectangle.fill"
        "zip", "rar", "7z", "tar", "gz" -> "archivebox.fill"
        "apk", "ipa", "exe", "dmg", "msi" -> "shippingbox.fill"
        else -> "doc.fill"
    }
}

/** 文件图标主色（与 iOS 文件 App 的配色习惯一致）。 */
fun tintForFile(name: String, isDirectory: Boolean): Color {
    if (isDirectory) return Color(0xFF3C9BF0)
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "heic", "bmp", "svg" -> Color(0xFF34C759)
        "mp4", "mov", "mkv", "avi", "webm", "m4v" -> Color(0xFFAF52DE)
        "mp3", "wav", "flac", "m4a", "aac", "ogg" -> Color(0xFFFF2D55)
        "pdf" -> Color(0xFFFF3B30)
        "xls", "xlsx", "csv" -> Color(0xFF30B0C7)
        "ppt", "pptx", "key" -> Color(0xFFFF9500)
        "zip", "rar", "7z", "tar", "gz" -> Color(0xFF8E8E93)
        else -> Color(0xFF007AFF)
    }
}
