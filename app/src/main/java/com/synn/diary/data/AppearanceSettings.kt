package com.synn.diary.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

/**
 * 外观开关：目前就一个"跟随系统取色"。
 *
 * 用 Compose 的 mutableState 而不是只读 prefs：设置页一改，整个 Activity 立刻重组，
 * 不用等下次冷启动。
 */
class AppearanceSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("appearance", Context.MODE_PRIVATE)

    var dynamicColor: Boolean by mutableStateOf(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
        private set

    /** 深浅色：system / light / dark。 */
    var themeMode: String by mutableStateOf(prefs.getString(KEY_THEME, "system")!!)
        private set

    fun setThemeModeId(value: String) {
        themeMode = value
        prefs.edit { putString(KEY_THEME, value) }
    }

    /** 心情图标风格：bear / yellow / ring / crayon。 */
    var moodStyle: String by mutableStateOf(prefs.getString(KEY_MOOD_STYLE, "bear")!!)
        private set

    fun setMoodStyleId(value: String) {
        moodStyle = value
        prefs.edit { putString(KEY_MOOD_STYLE, value) }
    }

    /** 液态玻璃：OriginOS 7 默认开，其它系统默认关（只能做到近似效果）。 */
    var glassEffect: Boolean by mutableStateOf(
        prefs.getBoolean(KEY_GLASS, com.synn.diary.util.OsFlavor.supportsLiquidGlass),
    )
        private set

    fun setGlassEffectEnabled(value: Boolean) {
        glassEffect = value
        prefs.edit { putBoolean(KEY_GLASS, value) }
    }

    fun setDynamicColorEnabled(value: Boolean) {
        dynamicColor = value
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, value) }
    }

    private companion object {
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_GLASS = "glass_effect"
        const val KEY_THEME = "theme_mode"
        const val KEY_MOOD_STYLE = "mood_style"
    }
}
