package com.edgecos.mobileapp.common

import android.app.Dialog
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.edgecos.mobileapp.design.theme.EdgeCOSTheme

/**
 * 在非 Compose 页面（如纯 View 体系的 [com.edgecos.mobileapp.feature.auth.OAuthLoginActivity]）
 * 中承载 Compose 弹窗的宿主。
 *
 * 【为什么需要】App 内弹窗已统一为 Compose 组件
 * [com.edgecos.mobileapp.design.components.EdgeAlertDialog]，
 * 但全局强制更新守卫 [com.edgecos.mobileapp.UpdateGuard] 需要在任意 Activity 上弹出，
 * 其中 OAuth 登录页没有 Compose 环境。此宿主用 ComposeView 包一层透明 Dialog，
 * 让所有页面共享同一套弹窗样式。
 *
 * 注意：Dialog 的 window 不在 Activity 的 View 树中，ComposeView 需要手动
 * 补齐 lifecycle / savedState 所有者，否则组合无法创建。
 */
class ComposeDialogHost(
    private val activity: ComponentActivity,
) {

    private var dialog: Dialog? = null

    /** 弹窗是否正在显示 */
    val isShowing: Boolean get() = dialog?.isShowing == true

    /**
     * 显示一个 Compose 弹窗。
     *
     * @param cancelable 是否允许返回键 / 点击外部关闭（弹窗内部按钮不受此限制）
     * @param onDismiss 弹窗被关闭（任意方式）后的回调
     * @param content 弹窗内容；参数 `dismiss` 用于主动关闭
     */
    fun show(
        cancelable: Boolean = true,
        onDismiss: () -> Unit = {},
        content: @Composable (dismiss: () -> Unit) -> Unit,
    ) {
        dismiss()
        if (activity.isFinishing || activity.isDestroyed) return

        val hostDialog = Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar)
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(activity)
            setViewTreeSavedStateRegistryOwner(activity)
            setContent {
                EdgeCOSTheme {
                    content { hostDialog.dismiss() }
                }
            }
        }
        hostDialog.setContentView(composeView)
        hostDialog.setCancelable(cancelable)
        hostDialog.setCanceledOnTouchOutside(cancelable)
        hostDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        hostDialog.setOnDismissListener {
            if (dialog === hostDialog) dialog = null
            onDismiss()
        }
        hostDialog.show()
        dialog = hostDialog
    }

    /** 关闭当前弹窗（若有） */
    fun dismiss() {
        val current = dialog ?: return
        dialog = null
        if (current.isShowing) current.dismiss() else current.cancel()
    }
}
