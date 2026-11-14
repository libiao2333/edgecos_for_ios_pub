package com.edgecos.mobileapp.design.animation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * EdgeCOS 动画规格 — 基于 Apple Design SKILL（WWDC 2018/2020/2026 + Emil Kowalski animations.dev）。
 *
 * 核心原则：
 * - 按压反馈用高刚度弹簧：pointer-down 即刻响应，释放快速回弹（可中断、携带速度）
 * - 进入/退出用 ease-out（FastOutSlowInEasing 偏弱，用自定义强 ease-out）
 * - 手势驱动用 Spring（临界阻尼 1.0，可中断）
 * - 绝不从 scale(0) 开始 → 从 scale(0.95) + opacity(0) 入场
 * - UI 动画控制在 300ms 以内（分级：按压 100-160ms，下拉 150-250ms，模态 200-500ms）
 * - 交错入场：项间 50ms，不阻塞交互
 * - 仅动画化 transform / scale / alpha（GPU 属性），绝不动画 width/height/padding
 *
 * 【提速调优原则】动画形态不变（曲线类型 / 过冲比例 / 目标值一律不动），只压缩响应时间：
 * - Spring 刚度基准 StiffnessMedium（response≈0.16s，比旧值快 2~7 倍）；刚度越高越"跟手"
 * - 弹簧可被随时打断并携速反转（而非固定时长 tween 的"重新计时"），保证快速连点丝滑
 * - 时长类动画（入场 / 转场）在 Apple 建议区间内取更短值，迅速进入、更快让位
 */

object EdgeDurations {
    /** 按压反馈（SKILL §5：按钮按压 100-160ms） */
    const val Press = 160

    /** 小弹出 / tooltip（SKILL §5：125-200ms） */
    const val Popup = 200

    /** 下拉 / 选择器（SKILL §5：150-250ms） */
    const val Dropdown = 250

    /** 入场 / 退出（SKILL §5: 200-500ms，取 240ms：迅速进入，逼近感知瞬时又不失优雅） */
    const val Entrance = 240

    /** 模态 / 抽屉（SKILL §5：200-500ms） */
    const val Modal = 400

    /** 交错入场项间延迟（SKILL §17：30-80ms，取 50ms） */
    const val StaggerStep = 50
}

object EdgeEasing {
    /**
     * 强 ease-out（SKILL §4.2）：
     * 原文 `cubic-bezier(0.23, 1, 0.32, 1)` — 比系统 FastOutSlowInEasing 更强。
     * 用于进入/退出动画。
     */
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /**
     * 强 ease-in-out（SKILL §4.2）：
     * 原文 `cubic-bezier(0.77, 0, 0.175, 1)` — 屏幕内移动/变形用。
     */
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /**
     * iOS 抽屉曲线（SKILL §4.2）：
     * 原文 `cubic-bezier(0.32, 0.72, 0, 1)` — 抽屉/Sheet 用。
     */
    val Drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

object EdgeSpring {
    /**
     * 默认临界阻尼 Spring（SKILL §6.1）：
     * dampingRatio=1.0, stiffness=1500（对应 response≈0.16s）。
     * 无过冲，优雅不分散注意力 — 用于大多数 UI 运动。
     */
    fun default() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,   // 1.0 临界阻尼
        stiffness = Spring.StiffnessMedium,            // 1500 N/m, response≈0.16s
    )

    /**
     * 动量 Spring（SKILL §6.1）：
     * dampingRatio=0.75, stiffness=1500。
     * 轻微过冲 — 仅用于手势结束后的动量投射（拖拽/轻弹释放）。
     */
    fun momentum() = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,   // ~0.75
        stiffness = Spring.StiffnessMedium,
    )

    /**
     * 按压弹簧（SKILL §2：pointer-down 即刻响应）：
     * dampingRatio=1.0, stiffness=10000（response≈0.06s）。
     *
     * 幅度小（4% scale 位移），瞬时到位不突兀；释放快速回弹；
     * 固定时长 tween 起始速度为零、中断即"重新计时"，弹簧则全程携速，快速连点更顺滑。
     */
    fun press() = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh,              // 10000 N/m, response≈0.06s
    )
}

/** 按压反馈动画：scale(0.97) + easeOut + 160ms（SKILL §9.3） */
fun pressEasing() = tween<Float>(
    durationMillis = EdgeDurations.Press,
    easing = EdgeEasing.EaseOut,
)

/** 入场动画：tween + EaseOut + 240ms（SKILL §5，时长取区间下沿以更迅速） */
fun entranceEasing() = tween<Float>(
    durationMillis = EdgeDurations.Entrance,
    easing = EdgeEasing.EaseOut,
)

/** 从 scale(0.95) + opacity(0) 入场，绝不用 scale(0)（SKILL §9.1） */
const val EnterScaleStart = 0.95f
