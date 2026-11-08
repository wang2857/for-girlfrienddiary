package com.synn.diary.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatesTest {

    @Test
    fun `时间按 24 小时两位格式`() {
        val zone = ZoneId.of("Asia/Shanghai")
        val millis = ZonedDateTime.of(2026, 11, 8, 8, 5, 0, 0, zone)
            .toInstant().toEpochMilli()
        assertEquals("08:05", Dates.time(millis, zone))
    }

    @Test
    fun `零点不会显示成 24 点`() {
        val zone = ZoneId.of("UTC")
        val millis = ZonedDateTime.of(2026, 1, 2, 0, 0, 0, 0, zone)
            .toInstant().toEpochMilli()
        assertEquals("00:00", Dates.time(millis, zone))
    }

    @Test
    fun `epochDay 与 LocalDate 互转`() {
        val day = LocalDate.of(2026, 11, 8).toEpochDay()
        assertEquals("2026年11月8日", Dates.fullDate(day))
        assertEquals(day, Dates.date(day).toEpochDay())
    }

    @Test
    fun `一周从周一开始`() {
        val sunday = LocalDate.of(2026, 11, 8).toEpochDay() // 周日
        val monday = LocalDate.of(2026, 11, 2).toEpochDay()
        assertEquals(monday, Dates.weekStart(sunday))
        assertTrue(Dates.weekRangeLabel(monday).contains("11.02"))
    }
}
