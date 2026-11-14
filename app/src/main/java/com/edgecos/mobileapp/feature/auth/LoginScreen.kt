package com.edgecos.mobileapp.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 登录页：iOS HIG 风格。
 *
 * - 顶部返回按钮（毛玻璃 NavBar 风格）
 * - 大标题 + 副标题
 * - iOS 风格输入框（圆角填充背景）
 * - Filled 主按钮 + Tinted 次按钮
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showOAuthSheet by remember { mutableStateOf(false) }

    val passkeyViewModel = remember { PasskeyViewModel() }
    val passkeyState by passkeyViewModel.state.collectAsState()

    LaunchedEffect(state.loginSuccess) {
        if (state.loginSuccess) onLoginSuccess()
    }

    LaunchedEffect(passkeyState.loginSuccess) {
        if (passkeyState.loginSuccess) onLoginSuccess()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "登录", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = EdgeSpacing.lg)
                    .padding(bottom = EdgeSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
            ) {
                Spacer(Modifier.height(EdgeSpacing.lg))

                // 大标题
                Text(
                    text = "欢迎回来",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "登录您的 EdgeCOS 账号",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(EdgeSpacing.xl))

                // iOS 风格输入框
                OutlinedTextField(
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Username },
                    label = { Text("账号 / 手机号 / 邮箱") },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                    label = { Text("密码") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::togglePasswordVisible) {
                            Icon(
                                imageVector = if (state.passwordVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (state.passwordVisible) "隐藏密码" else "显示密码",
                            )
                        }
                    },
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )

                Spacer(Modifier.height(EdgeSpacing.md))

                // 错误提示
                if (state.error != null) {
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (passkeyState.error != null) {
                    Text(
                        text = passkeyState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // 登录按钮（Filled）
                IOSButton(
                    text = "登录",
                    onClick = viewModel::login,
                    style = IOSButtonStyle.Filled,
                    enabled = !state.isLoading && !passkeyState.isLoading,
                    isLoading = state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                )

                // Passkey 按钮（Tinted）
                IOSButton(
                    text = "Passkey 登录",
                    onClick = { passkeyViewModel.startLogin(context) },
                    style = IOSButtonStyle.Tinted,
                    enabled = !state.isLoading && !passkeyState.isLoading,
                    isLoading = passkeyState.isLoading,
                    leadingIcon = Icons.Outlined.Fingerprint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                )

                Spacer(Modifier.height(EdgeSpacing.sm))

                // 分隔线
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  第三方登录  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // 第三方登录按钮（Tinted）
                IOSButton(
                    text = "QQ / 微信 / GitHub 登录",
                    onClick = { showOAuthSheet = true },
                    style = IOSButtonStyle.Tinted,
                    enabled = !state.isLoading && !passkeyState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    leadingIcon = Icons.Outlined.Login,
                )

                Spacer(Modifier.height(EdgeSpacing.lg))

                // 注册入口
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "还没有账号？",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "立即注册",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onNavigateToRegister() },
                    )
                }
            }
        }
    }

    if (showOAuthSheet) {
        OAuthLoginSheet(
            onDismiss = { showOAuthSheet = false },
            onLoginSuccess = onLoginSuccess,
        )
    }
}
