package com.synn.diary.util

import android.icu.util.ChineseCalendar
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object Dates {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val locale: Locale = Locale.CHINA

    private val monthHeaderFormatter = DateTimeFormatter.ofPattern("yyyy年M月", locale)
    private val dayFormatter = DateTimeFormatter.ofPattern("M月d日", locale)
    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", locale)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", locale)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
    private val weekdayShortFormatter = DateTimeFormatter.ofPattern("EEE", locale)

    fun todayEpochDay(): Long = LocalDate.now(zone).toEpochDay()

    fun date(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun monthHeader(epochDay: Long): String = date(epochDay).format(monthHeaderFormatter)

    fun dayShort(epochDay: Long): String = date(epochDay).format(dayFormatter)

    fun weekday(epochDay: Long): String = date(epochDay).format(weekdayFormatter)

    fun fullDate(epochDay: Long): String = date(epochDay).format(fullDateFormatter)

    fun time(epochMillis: Long): String =
        LocalTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone).format(timeFormatter)

    fun weekdayShort(epochDay: Long): String = date(epochDay).format(weekdayShortFormatter)

    /** 这一周的周一（周一分组的起点）。 */
    fun weekStart(epochDay: Long): Long =
        date(epochDay).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()

    fun weekRangeLabel(weekStartEpochDay: Long): String {
        val start = date(weekStartEpochDay)
        val end = start.plusDays(6)
        val fmt = DateTimeFormatter.ofPattern("M.dd", locale)
        return "${start.format(fmt)}–${end.format(fmt)}"
    }

    fun isCurrentWeek(weekStartEpochDay: Long): Boolean =
        weekStartEpochDay == weekStart(todayEpochDay())

    /**
     * 农历日期，形如「九月廿九」「闰六月初一」。
     *
     * 用平台自带的 ICU ChineseCalendar（API 24+），不引第三方历法库：
     * 农历换算规则复杂（闰月、大小月），自己实现必错。
     */
    fun lunar(epochDay: Long): String? = lunarParts(epochDay)?.let { (month, day) -> month + day }

    /** 只要"日"：日历格子里放不下月份。 */
    fun lunarDay(epochDay: Long): String? = lunarParts(epochDay)?.second

    /**
     * 日历格子里用的农历标签：
     * 初一显示月份（「十月」「闰六月」），其余显示日（「廿九」）。
     * 这是老黄历的惯例——月份只在初一出现一次，一眼就能看出月首在哪。
     */
    fun lunarLabel(epochDay: Long): Pair<String, Boolean>? = lunarParts(epochDay)?.let { (month, day) ->
        if (day == "初一") month to true else day to false
    }

    private fun lunarParts(epochDay: Long): Pair<String, String>? = runCatching {
        val millis = date(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()
        val calendar = ChineseCalendar().apply { timeInMillis = millis }
        val monthIndex = calendar.get(ChineseCalendar.MONTH).coerceIn(0, 11)
        val day = calendar.get(ChineseCalendar.DAY_OF_MONTH).coerceIn(1, 30)
        val isLeap = calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1
        val month = (if (isLeap) "闰" else "") + LUNAR_MONTHS[monthIndex]
        month to lunarDay(day)
    }.getOrNull()

    private fun lunarDay(day: Int): String = when {
        day <= 10 -> "初" + LUNAR_DIGITS[day - 1]
        day < 20 -> "十" + LUNAR_DIGITS[day - 11]
        day == 20 -> "二十"
        day < 30 -> "廿" + LUNAR_DIGITS[day - 21]
        else -> "三十"
    }

    private val LUNAR_DIGITS = arrayOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    private val LUNAR_MONTHS = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月",
    )
}
