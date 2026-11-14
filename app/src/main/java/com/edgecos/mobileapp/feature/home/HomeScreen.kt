package com.edgecos.mobileapp.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSLargeTitleNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 首页：iOS HIG 风格。
 *
 * - 顶部 Large Title 导航栏（"EdgeCOS" Logo）
 * - 已登录：分组圆角卡片展示最近文件
 *   （① app 内最近 3 天打开的文件 → ② 修改时间最新的 3 个文件 → ③ 最近无文件）
 * - 未登录：居中 Logo + 登录/注册按钮（IOSButton Filled / Tinted）
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    isLoggedIn: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateAnnouncements: () -> Unit,
    onNavigateShareManagement: () -> Unit,
    onNavigateWorkOrder: () -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSLargeTitleNavBar(title = "首页", applyStatusBarInset = false) },
    ) { innerPadding ->
        if (!isLoggedIn) {
            UnloggedHomeContent(
                modifier = Modifier.padding(innerPadding),
                onLogin = onNavigateToLogin,
                onRegister = onNavigateToRegister,
            )
            return@Scaffold
        }

        val state by viewModel.state.collectAsState()

        // 每次进入首页（含切换 Tab 回来 / 从预览页返回）刷新「最近文件」
        LaunchedEffect(isLoggedIn) {
            if (isLoggedIn) viewModel.refresh()
        }

        if (state.isLoading && state.recentFiles.isEmpty()) {
            EdgeLoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
        ) {
            // iOS Grouped List：圆角卡片分组（title + Surface[tonalElevation=0, shadowElevation=1]）
            IOSGroupedList(title = "最近文件") {
                // 最近文件（最多 10 条）
                val recent = state.recentFiles.take(10)
                if (recent.isEmpty()) {
                    EdgeEmptyState(
                        title = "最近无文件",
                        subtitle = "在 app 内打开或上传文件后会显示在这里",
                        icon = Icons.Outlined.Description,
                    )
                } else {
                    recent.forEachIndexed { index, file ->
                        HomeFileRow(
                            file = file,
                            showDivider = index < recent.size - 1,
                        )
                    }
                }
            }

            // 错误提示
            state.error?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = EdgeSpacing.lg),
                )
            }

            // 底部留白：与浮动 TabBar 等高（TabBar 56dp + 底部边距 12dp + 导航栏安全区）
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp + EdgeSpacing.md),
            )
        }
    }
}

/**
 * iOS 风格列表项：圆角方块图标 + 文件名 + 已分享标签。
 */
@Composable
private fun HomeFileRow(
    file: com.edgecos.mobileapp.domain.model.FileNode,
    showDivider: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(horizontal = EdgeSpacing.lg, vertical = EdgeSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            // 文件图标：iOS 圆角方块背景
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (file.type == com.edgecos.mobileapp.domain.model.FileType.DIRECTORY)
                        Icons.Outlined.Folder else Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = file.basename,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (file.activeShare != null) {
                Text(
                    text = "已分享",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = EdgeSpacing.lg + 28.dp + EdgeSpacing.md),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun UnloggedHomeContent(
    modifier: Modifier,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(EdgeSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = com.edgecos.mobileapp.R.drawable.img_logo),
            contentDescription = "EdgeCOS",
            modifier = Modifier.size(120.dp),
        )
        Spacer(Modifier.height(EdgeSpacing.sm))
        Text(
            text = "欢迎使用EdgeCOS",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(EdgeSpacing.xxxl))

        // iOS 风格 Filled 主按钮
        IOSButton(
            text = "登录",
            onClick = onLogin,
            style = IOSButtonStyle.Filled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        )

        Spacer(Modifier.height(EdgeSpacing.md))

        // iOS 风格 Tinted 次按钮
        IOSButton(
            text = "新用户注册",
            onClick = onRegister,
            style = IOSButtonStyle.Tinted,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        )
    }
}
