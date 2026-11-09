package com.synn.diary.ui.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodStyleTest {

    @Test
    fun `id 唯一，且默认是 emoji`() {
        val ids = MoodStyle.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(MoodStyle.EMOJI, MoodStyle.from(null))
        assertEquals(MoodStyle.EMOJI, MoodStyle.from("nope"))
    }

    @Test
    fun `每个系统 emoji 都能对应到心情`() {
        Mood.entries.forEach { mood ->
            assertEquals(mood, Mood.fromEmoji(mood.emoji))
            assertTrue(mood.emoji.isNotBlank())
        }
    }
}
