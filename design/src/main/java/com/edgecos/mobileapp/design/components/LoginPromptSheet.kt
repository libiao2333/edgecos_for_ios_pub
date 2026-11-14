package com.edgecos.mobileapp.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 未登录引导底部弹窗：iOS HIG 风格。
 *
 * - 纯色背景（surface），不用毛玻璃
 * - 顶部圆角 24dp（iOS Sheet 标准）
 * - 主按钮用 IOSButton.Filled，次按钮用 IOSButton.Plain
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPromptSheet(
    onDismiss: () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
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
                    top = EdgeSpacing.xl,
                    bottom = EdgeSpacing.xxl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(EdgeSpacing.sm))

            Text(
                text = "请先登录",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "登录后即可使用文件管理、分享、套餐等全部功能",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = EdgeSpacing.xl),
            )

            Spacer(Modifier.height(EdgeSpacing.lg))

            // 登录按钮（Filled）
            IOSButton(
                text = "登录",
                onClick = onLogin,
                style = IOSButtonStyle.Filled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )

            // 注册按钮（Plain）
            IOSButton(
                text = "新用户注册",
                onClick = onRegister,
                style = IOSButtonStyle.Plain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
        }
    }
}
