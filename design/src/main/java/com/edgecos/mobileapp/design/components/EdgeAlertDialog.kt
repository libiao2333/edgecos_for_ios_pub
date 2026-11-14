package com.edgecos.mobileapp.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.edgecos.mobileapp.design.theme.EdgeSize
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 全局统一弹窗（iOS HIG 风格）——App 内所有 Alert 类弹窗的唯一实现。
 *
 * 【为什么统一】此前各页面直接用 Material3 `AlertDialog` 或原生
 * `android.app.AlertDialog`，圆角、间距、按钮样式、可取消行为各不相同；
 * 原生弹窗还因宿主主题为 `android:Theme.Material.Light.NoActionBar`
 * 出现按钮文字不可见的问题。所有弹窗改为本组件后视觉与交互完全一致。
 *
 * 【视觉规范】
 * - 容器：20dp 圆角 + surface 底色 + 24dp 阴影，宽度上限 320dp（iOS Alert 比例）
 * - 标题：居中、SemiBold；可选图标显示在标题上方并居中
 * - 正文：居中、次要文字色
 * - 按钮：水平并排（左取消 / 右确认）各占一半；仅一个按钮时占满整行
 * - 触控目标：≥44dp（[EdgeSize.touchTarget]）
 *
 * @param onDismissRequest 点击外部 / 返回键 / 取消按钮时的关闭回调
 * @param title 标题
 * @param message 正文（可选，居中展示）
 * @param icon 标题上方的图标（可选，居中）
 * @param iconTint 图标颜色
 * @param confirmText 确认按钮文案；传 null 表示不显示确认按钮
 * @param onConfirm 确认按钮回调
 * @param dismissText 取消按钮文案；传 null 表示不显示取消按钮（单按钮弹窗）
 * @param onDismiss 取消按钮回调；默认走 [onDismissRequest]
 * @param confirmEnabled 确认按钮是否可点
 * @param destructive 破坏性操作（删除等）：确认按钮使用 error 色
 * @param dismissOnBackPress/ dismissOnClickOutside 是否允许返回键 / 点击外部关闭
 * @param content 自定义内容槽（表单、选项列表等，左对齐；与 [message] 可同时使用）
 */
@Composable
fun EdgeAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmText: String? = "确定",
    onConfirm: (() -> Unit)? = onDismissRequest,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 320.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = EdgeSpacing.xl,
                    vertical = EdgeSpacing.xl,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(EdgeSpacing.md))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (message != null) {
                    Spacer(Modifier.height(EdgeSpacing.sm))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (content != null) {
                    Spacer(Modifier.height(EdgeSpacing.lg))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }

                if (confirmText != null || dismissText != null) {
                    Spacer(Modifier.height(EdgeSpacing.xl))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                    ) {
                        if (dismissText != null) {
                            EdgeDialogAction(
                                text = dismissText,
                                onClick = onDismiss ?: onDismissRequest,
                                modifier = Modifier.weight(1f),
                                primary = false,
                            )
                        }
                        if (confirmText != null) {
                            EdgeDialogAction(
                                text = confirmText,
                                onClick = { onConfirm?.invoke() },
                                modifier = Modifier.weight(1f),
                                primary = true,
                                enabled = confirmEnabled,
                                destructive = destructive,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 弹窗内的操作按钮：与 [IOSButton] 同一套观感（Filled / Plain 两态），
 * 额外支持破坏性配色与等宽并排布局。
 */
@Composable
private fun EdgeDialogAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.secondary
    val containerColor = when {
        !primary -> Color.Transparent
        enabled -> accent
        else -> accent.copy(alpha = 0.3f)
    }
    val contentColor = when {
        !primary -> if (enabled) accent else accent.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSecondary
    }

    Row(
        modifier = modifier
            .heightIn(min = EdgeSize.touchTarget)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = EdgeSpacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}
