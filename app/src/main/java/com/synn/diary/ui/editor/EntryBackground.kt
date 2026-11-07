package com.synn.diary.ui.editor

import androidx.compose.ui.graphics.Color

/**
 * 日记纸张的底色。存的是 id（字符串），老记录没有这个字段就是默认。
 *
 * 明暗两套颜色：浅色纸在深色模式下会晃眼，所以各给一份。
 */
enum class EntryBackground(
    val id: String,
    val label: String,
    val light: Color,
    val dark: Color,
) {
    DEFAULT("default", "默认", Color.Transparent, Color.Transparent),
    CREAM("cream", "米黄", Color(0xFFFBF3E0), Color(0xFF2A2620)),
    GREEN("green", "淡绿", Color(0xFFE8F3E4), Color(0xFF1F2A1E)),
    BLUE("blue", "淡蓝", Color(0xFFE6EFF9), Color(0xFF1C2430)),
    GRAY("gray", "淡灰", Color(0xFFF2F2F2), Color(0xFF232323)),
    ;

    fun color(isDark: Boolean): Color = if (isDark) dark else light

    companion object {
        fun from(id: String?): EntryBackground = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
