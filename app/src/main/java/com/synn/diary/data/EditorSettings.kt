package com.synn.diary.data

import android.content.Context

/** 编辑器里那些"看个人习惯"的显示开关（跟具体哪篇日记无关）。 */
class EditorSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("editor", Context.MODE_PRIVATE)

    /** 纸面横线：默认开着，像横线本。 */
    var ruledLines: Boolean
        get() = prefs.getBoolean(KEY_RULED_LINES, true)
        set(value) = prefs.edit().putBoolean(KEY_RULED_LINES, value).apply()

    private companion object {
        const val KEY_RULED_LINES = "ruled_lines"
    }
}
