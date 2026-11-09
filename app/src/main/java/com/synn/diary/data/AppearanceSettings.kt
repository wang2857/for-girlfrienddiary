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

    fun setDynamicColorEnabled(value: Boolean) {
        dynamicColor = value
        prefs.edit { putBoolean(KEY_DYNAMIC_COLOR, value) }
    }

    private companion object {
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
    }
}
