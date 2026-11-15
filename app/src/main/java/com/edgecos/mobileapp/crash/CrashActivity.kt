package com.edgecos.mobileapp.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.edgecos.mobileapp.MainActivity
import com.edgecos.mobileapp.UpdateGuard
import com.edgecos.mobileapp.design.components.IOSButton
import com.edgecos.mobileapp.design.components.IOSButtonStyle
import com.edgecos.mobileapp.design.theme.EdgeSpacing

/**
 * 崩溃兜底页。
 *
 * 触发场景：
 * - 应用发生未捕获异常时由 [CrashHandler] 启动
 * - 用户在"关于"页主动点击"模拟崩溃"按钮测试兜底链路
 *
 * 页面职责：
 * - 用温柔文案安抚用户（避免"应用已停止运行"的恐慌感）
 * - 展示崩溃摘要（异常类、组件、线程）
 * - 提供"重启应用"按钮
 * - 提供"复制日志"按钮，便于用户反馈
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 强制更新守卫：崩溃兜底页同样拦截（任何 Activity 都不能绕过更新）
        UpdateGuard.check(this)

        val exceptionClass = intent.getStringExtra(EXTRA_EXCEPTION_CLASS) ?: "Unknown"
        val exceptionMessage = intent.getStringExtra(EXTRA_EXCEPTION_MESSAGE) ?: ""
        val component = intent.getStringExtra(EXTRA_COMPONENT) ?: "unknown"
        val threadName = intent.getStringExtra(EXTRA_THREAD_NAME) ?: "unknown"

        setContent {
            MaterialTheme {
                CrashScreen(
                    exceptionClass = exceptionClass,
                    exceptionMessage = exceptionMessage,
                    component = component,
                    threadName = threadName,
                    onRestart = { restartApp() },
                    onExit = { finishAffinity() },
                )
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
        finishAffinity()
    }

    companion object {
        const val EXTRA_EXCEPTION_CLASS = "extra_exception_class"
        const val EXTRA_EXCEPTION_MESSAGE = "extra_exception_message"
        const val EXTRA_COMPONENT = "extra_component"
        const val EXTRA_THREAD_NAME = "extra_thread_name"
    }
}

@Composable
private fun CrashScreen(
    exceptionClass: String,
    exceptionMessage: String,
    component: String,
    threadName: String,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    var showDetails by remember { mutableStateOf(false) }

    // 拉取最新的崩溃日志内容（用户复制时用）
    val latestCrashLog = remember {
        CrashReporter.listCrashFiles(context)
            .firstOrNull { it.extension == "txt" }
            ?.let { f -> CrashReporter.readCrashFile(context, f.name) }
            ?: "暂无崩溃日志文件"
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = EdgeSpacing.lg)
                .padding(bottom = EdgeSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(EdgeSpacing.lg),
        ) {
            Spacer(Modifier.height(EdgeSpacing.xl))

            // 友好的图标
            Icon(
                imageVector = Icons.Outlined.BugReport,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            // 安抚文案
            Text(
                text = "Oops！出了点小问题",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "别担心，这不是你的错。\n我们已经记录了这次问题，工程师会全力排查修复。给你带来不便，我们深表歉意 🙇",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 错误摘要卡片（iOS 圆角卡片风格）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(EdgeSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
                ) {
                    Text(
                        text = "错误摘要",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    DetailRow("异常类型", exceptionClass.substringAfterLast('.'))
                    DetailRow("发生组件", component)
                    DetailRow("线程", threadName)
                    if (exceptionMessage.isNotBlank()) {
                        DetailRow("错误信息", exceptionMessage.take(200))
                    }
                }
            }

            // 展开详情（iOS Tinted 按钮）
            IOSButton(
                text = if (showDetails) "收起完整日志" else "查看完整日志",
                onClick = { showDetails = !showDetails },
                style = IOSButtonStyle.Tinted,
                modifier = Modifier.fillMaxWidth(),
            )

            if (showDetails) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = latestCrashLog,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EdgeSpacing.md),
                    )
                }
            }

            Spacer(Modifier.height(EdgeSpacing.sm))

            // 主操作按钮（iOS Filled）
            IOSButton(
                text = "重启应用",
                onClick = onRestart,
                style = IOSButtonStyle.Filled,
                leadingIcon = Icons.Outlined.Refresh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )

            // 次要操作（iOS Tinted）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EdgeSpacing.sm),
            ) {
                IOSButton(
                    text = "复制日志",
                    onClick = {
                        // 复制日志到剪贴板，方便用户反馈
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("EdgeCOS Crash Log", latestCrashLog),
                        )
                    },
                    style = IOSButtonStyle.Tinted,
                    leadingIcon = Icons.Outlined.ContentCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                )
                IOSButton(
                    text = "退出应用",
                    onClick = onExit,
                    style = IOSButtonStyle.Tinted,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}
