package com.edgecos.mobileapp.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgecos.mobileapp.design.components.EdgeEmptyState
import com.edgecos.mobileapp.design.components.EdgeLoadingState
import com.edgecos.mobileapp.design.components.IOSGroupedList
import com.edgecos.mobileapp.design.components.IOSNavBar
import com.edgecos.mobileapp.design.theme.EdgeSpacing
import com.edgecos.mobileapp.domain.util.formatApiTime

/**
 * 公告列表页：复用 AnnouncementRepository 拉取全量公告。
 */
@Composable
fun AnnouncementListScreen(
    onBack: () -> Unit,
    viewModel: AnnouncementListViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { IOSNavBar(title = "公告通知", onBack = onBack) },
    ) { innerPadding ->
        if (state.isLoading && announcements.isEmpty()) {
            EdgeLoadingState(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        if (announcements.isEmpty()) {
            EdgeEmptyState(
                title = "暂无公告",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = EdgeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.md),
        ) {
            items(announcements) { ann ->
                // iOS 圆角卡片（统一用 IOSGroupedList：有阴影、与工单一致）
                IOSGroupedList {
                    Column(modifier = Modifier.padding(EdgeSpacing.lg)) {
                        Text(
                            text = ann.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = formatApiTime(ann.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = EdgeSpacing.xs),
                        )
                        Text(
                            text = ann.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = EdgeSpacing.sm),
                        )
                    }
                }
            }
        }
    }
}
