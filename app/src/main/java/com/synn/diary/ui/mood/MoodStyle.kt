package com.synn.diary.ui.mood

/** 心情图标的四套画法；默认手绘小熊。 */
enum class MoodStyle(val id: String, val label: String) {
    BEAR("bear", "小熊"),
    YELLOW("yellow", "鹅黄"),
    RING("ring", "圆环"),
    CRAYON("crayon", "蜡笔"),
    ;

    companion object {
        fun from(id: String?): MoodStyle = entries.firstOrNull { it.id == id } ?: BEAR
    }
}
