package com.edgecos.mobileapp.data.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主题模式：
 * - SYSTEM：跟随系统暗黑模式（静态品牌色）
 * - LIGHT：强制明亮（静态品牌色）
 * - DARK：强制暗黑（静态品牌色）
 * - MONET：莫奈动态取色（跟随系统壁纸，明暗随系统）
 */
enum class ThemeMode(val storageKey: String, val displayName: String) {
    SYSTEM("system", "跟随系统"),
    LIGHT("light", "明亮模式"),
    DARK("dark", "暗黑模式"),
    MONET("monet", "莫奈取色");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/**
 * 主题偏好存储：用普通 SharedPreferences（无需加密）。
 *
 * 暴露 [mode] StateFlow，UI 观察后实时刷新主题。
 */
class ThemeStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(ThemeMode.fromKey(prefs.getString(KEY_MODE, null)))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.storageKey).apply()
        _mode.value = mode
    }

    companion object {
        private const val PREF_NAME = "edgecos_theme"
        private const val KEY_MODE = "mode"

        @Volatile
        private var instance: ThemeStore? = null

        fun init(context: Context): ThemeStore = instance ?: synchronized(this) {
            instance ?: ThemeStore(context).also { instance = it }
        }

        fun get(): ThemeStore = instance ?: error("ThemeStore 未初始化，请先调用 init(context)")
    }
}
