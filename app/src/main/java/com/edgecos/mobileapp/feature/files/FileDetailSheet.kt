package com.edgecos.mobileapp.feature.files

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.model.FileNode
import com.edgecos.mobileapp.domain.model.FileType
import com.edgecos.mobileapp.domain.util.formatApiTime
import com.edgecos.mobileapp.domain.util.formatBytes

/**
 * 文件详情底部弹窗：长按文件行触发。
 *
 * iOS HIG 风格：
 * - 纯色背景（surface），不用毛玻璃（Sheet 本身已有遮罩）
 * - 顶部圆角 24dp（iOS Sheet 标准）
 * - 操作按钮用 IOSButton.Tinted 风格
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailSheet(
    file: FileNode,
    onDismiss: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onDirectLink: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = EdgeSpacing.xl,
                    end = EdgeSpacing.xl,
                    top = EdgeSpacing.lg,
                    bottom = EdgeSpacing.xxl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 文件类型图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = fileTypeIcon(file),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // 文件名
            Text(
                text = file.basename,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(EdgeSpacing.sm))

            // 信息行（iOS Grouped List 风格）
            DetailRow("大小", formatBytes(file.size))
            DetailRow("修改日期", file.lastmod)
            DetailRow("路径", file.filename)
            val typeLabel = if (file.type == FileType.DIRECTORY) "文件夹" else "文件"
            DetailRow("类型", typeLabel)

            Spacer(Modifier.height(EdgeSpacing.lg))

            // 操作按钮：圆形图标 + 下方文字说明（iOS 风格）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IOSCircleIconButton(
                    icon = Icons.Outlined.OpenInBrowser,
                    label = "打开",
                    onClick = onPreview,
                )
                IOSCircleIconButton(
                    icon = Icons.Outlined.Download,
                    label = "下载",
                    onClick = onDownload,
                )
                IOSCircleIconButton(
                    icon = Icons.Outlined.Share,
                    label = "分享",
                    onClick = onShare,
                )
                IOSCircleIconButton(
                    icon = Icons.Outlined.Link,
                    label = "直链",
                    onClick = onDirectLink,
                )
                IOSCircleIconButton(
                    icon = Icons.Outlined.Delete,
                    label = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDelete,
                )
            }
        }
    }
}

/**
 * iOS 风格圆形图标动作项（图标 + 下方文字说明）。
 *
 * - 圆形图标 48dp（满足触控目标最小值）
 * - 文字标签：labelSmall，颜色与图标一致（删除项即为红色 error 色）
 * - 整个条目（图标 + 文字）均可点击，ripple 裁剪为圆角矩形
 * - 无毛玻璃（纯色叠加，性能安全）
 */
@Composable
private fun IOSCircleIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = EdgeSpacing.xs, vertical = EdgeSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                // 文字标签已提供可访问性名称，图标不再重复朗读
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = EdgeSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** 根据文件类型返回对应图标 */
private fun fileTypeIcon(file: FileNode): ImageVector {
    if (file.type == FileType.DIRECTORY) return Icons.Outlined.InsertDriveFile
    val ext = file.basename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif" -> Icons.Outlined.Image
        "mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp" -> Icons.Outlined.Movie
        "mp3", "wav", "ogg", "flac", "aac", "m4a" -> Icons.Outlined.AudioFile
        "txt", "md", "json", "xml", "csv", "log", "pdf" -> Icons.Outlined.TextSnippet
        else -> Icons.Outlined.Description
    }
}
