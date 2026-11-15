package com.edgecos.mobileapp.feature.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgecos.mobileapp.BuildConfig
import com.edgecos.mobileapp.data.remote.UpdateChecker
import com.edgecos.mobileapp.data.remote.UpdateResult
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.components.IOSListRow
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import kotlinx.coroutines.launch
import com.edgecos.mobileapp.design.theme.IOSBlue
import com.edgecos.mobileapp.design.theme.IOSGreen
import com.edgecos.mobileapp.design.theme.IOSOrange
import com.edgecos.mobileapp.design.theme.IOSPurple
import com.edgecos.mobileapp.design.theme.IOSTeal

/**
 * 关于页：版本号、检查更新、用户服务协议、隐私政策、开源致谢。
 *
 * - 版本号取自 `BuildConfig.VERSION_NAME` / `VERSION_CODE`
 * - 协议页跳转到 [LegalDocScreen]，加载 EdgeCOS 网页端公开路由
 * - 开源致谢跳转到 [OpenSourceAcknowledgementsScreen]
 * - 检查更新：MVP 阶段仅 Dialog 提示，待后端 `/api/update/check` 接口上线后接入
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenOpenSourceAcknowledgements: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // null=未检查，非 null=有结果（含 UpdateAvailable/UpToDate/CheckFailed）
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "关于", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.xl),
        ) {
            // 应用信息卡片：图标 + 名称 + 版本号 居中
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.lg),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(EdgeSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                ) {
                    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val logoRes = if (isDark)
                        com.edgecos.mobileapp.R.drawable.icon_text_white
                    else
                        com.edgecos.mobileapp.R.drawable.icon_text_black
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "EdgeCOS",
                        modifier = Modifier.height(32.dp),
                    )
                    Spacer(Modifier.height(EdgeSpacing.xs))
                    Text(
                        text = "EdgeCOS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "边缘云存储客户端",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 检查更新
            IOSGroupedList(title = "更新") {
                IOSListRow(
                    title = "检查更新",
                    leadingIcon = Icons.Outlined.Refresh,
                    iconColor = IOSGreen,
                    trailingText = "v${BuildConfig.VERSION_NAME}",
                    showDivider = false,
                    onClick = {
                        if (isChecking) return@IOSListRow
                        isChecking = true
                        scope.launch {
                            updateResult = UpdateChecker.checkForUpdate(BuildConfig.VERSION_CODE)
                            isChecking = false
                        }
                    },
                )
            }

            // 法律与协议
            IOSGroupedList(title = "法律与协议") {
                IOSListRow(
                    title = "用户服务协议",
                    leadingIcon = Icons.Outlined.Description,
                    iconColor = IOSBlue,
                    showDivider = true,
                    onClick = onOpenUserAgreement,
                )
                IOSListRow(
                    title = "隐私政策",
                    leadingIcon = Icons.Outlined.PrivacyTip,
                    iconColor = IOSPurple,
                    showDivider = false,
                    onClick = onOpenPrivacyPolicy,
                )
            }

            // 开源致谢
            IOSGroupedList(title = "开源") {
                IOSListRow(
                    title = "开源致谢",
                    subtitle = "查看使用的开源项目",
                    leadingIcon = Icons.Outlined.VolunteerActivism,
                    iconColor = IOSOrange,
                    showDivider = false,
                    onClick = onOpenOpenSourceAcknowledgements,
                )
            }

            // 官网
            IOSGroupedList {
                IOSListRow(
                    title = "访问官网",
                    leadingIcon = Icons.Outlined.Language,
                    iconColor = IOSTeal,
                    showDivider = false,
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://user.edgecos.com")),
                            )
                        }
                    },
                )
            }

            // 版权与备案信息（居中，10sp，outline 色）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CopyrightText("Copyright © 2026 EdgeCOS. All Rights Reserved.")
                CopyrightText("版权所有，未经许可，不得对本软件进行逆向工程、修改或二次分发")
                CopyrightText("ICP备案号：蜀ICP备2025117706号-4")
                CopyrightText("公安网备案号：川公网安备51010602003341号")
            }
        }
    }

    // 手动检查更新结果弹窗
    updateResult?.let { result ->
        UpdateResultDialog(
            result = result,
            onDismiss = { updateResult = null },
        )
    }
}

@Composable
private fun CopyrightText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.outline,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
