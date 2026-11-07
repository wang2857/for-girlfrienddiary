package com.synn.diary.data.db

/**
 * 日历用的一行摘要：某天某条日记的心情和它的修改时间。
 * 同一分钟里有多条时，由调用方按 updatedAt 取最后修改那条的心情。
 */
data class DayMoodRow(
    val entryDate: Long,
    val mood: String?,
    val updatedAt: Long,
)
