package com.edgecos.mobileapp.feature.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.design.theme.EdgeSpacing

data class OAuthProvider(
    val name: String,
    val displayName: String,
    val brandColor: Color, // 平台品牌色（仅用于按钮文字与图标着色，不用作背景）
    val isQrOnly: Boolean = false,
)

private val OAUTH_PROVIDERS = listOf(
    OAuthProvider("wx", "微信", Color(0xFF07C160), isQrOnly = true),
    OAuthProvider("qq", "QQ", Color(0xFF12B7F5)),
    OAuthProvider("alipay", "支付宝", Color(0xFF1677FF)),
    OAuthProvider("github", "GitHub", Color(0xFF24292E)),
    OAuthProvider("gitee", "Gitee", Color(0xFFC71D23)),
    OAuthProvider("microsoft", "Microsoft", Color(0xFF00A4EF)),
)

fun buildOAuthUrl(provider: String): String {
    return "https://user.edgecos.com/api/auth/szczk/$provider/login"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthLoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            onDismiss()
            onLoginSuccess()
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        ProviderListContent(
            onSelectProvider = { provider ->
                val url = buildOAuthUrl(provider.name)
                launcher.launch(OAuthLoginActivity.newIntent(context, url))
            },
        )
    }
}

@Composable
private fun ProviderListContent(
    onSelectProvider: (OAuthProvider) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EdgeSpacing.lg)
            .padding(bottom = EdgeSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
    ) {
        Text(
            text = "第三方登录",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "选择以下平台账号登录 EdgeCOS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        val chunked = OAUTH_PROVIDERS.chunked(2)
        chunked.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
            ) {
                row.forEach { provider ->
                    OAuthProviderButton(
                        provider = provider,
                        onClick = { onSelectProvider(provider) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * iOS 风格 OAuth 提供商按钮：Tinted 风格，使用品牌色作内容色。
 *
 * - 背景：primary.copy(alpha=0.08f) 中性淡色（不抢品牌色）
 * - 文字与图标：使用 provider.brandColor（保留品牌识别）
 * - 圆角 12dp，高度 52dp
 */
@Composable
private fun OAuthProviderButton(
    provider: OAuthProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = EdgeSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (provider.isQrOnly) {
            Icon(
                Icons.Outlined.QrCode,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = provider.brandColor,
            )
            Spacer(modifier = Modifier.width(EdgeSpacing.sm))
        }
        Text(
            text = provider.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = provider.brandColor,
        )
    }
}
