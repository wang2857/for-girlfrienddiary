package com.synn.diary.ui.mood

import androidx.compose.ui.graphics.Color

/**
 * 心情 = emoji + 中文名 + 一个颜色。
 *
 * 存储仍然只存 emoji（`entry.mood`），所以老数据不用迁移：
 * 认不出的 emoji 就当没有心情处理，颜色也会退回默认样式。
 */
enum class Mood(
    val emoji: String,
    val label: String,
    val color: Color,
) {
    HAPPY("😄", "开心", Color(0xFFFFB300)),
    EXCITED("🤩", "兴奋", Color(0xFFFF7043)),
    CALM("🙂", "平静", Color(0xFF66BB6A)),
    MEH("😐", "还行", Color(0xFF90A4AE)),
    TIRED("😴", "累", Color(0xFF7E57C2)),
    ANNOYED("😤", "烦", Color(0xFFEF5350)),
    DOWN("😔", "低落", Color(0xFF5C6BC0)),
    SAD("😢", "难过", Color(0xFF29B6F6)),
    ;

    companion object {
        fun fromEmoji(emoji: String?): Mood? = entries.firstOrNull { it.emoji == emoji }
    }
}
