package com.synn.diary.ui.mood

/** 心情图标的画法。默认还是系统 emoji，换成手绘更统一、也更"可爱"。 */
enum class MoodStyle(val id: String, val label: String) {
    EMOJI("emoji", "系统 emoji"),
    FACE("face", "小圆脸"),
    CAT("cat", "小猫"),
    BEAN("bean", "小团子"),
    ;

    companion object {
        fun from(id: String?): MoodStyle = entries.firstOrNull { it.id == id } ?: EMOJI
    }
}
