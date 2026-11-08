package com.synn.diary.data.db

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsQueryTest {

    @Test
    fun `普通关键词包成短语`() {
        assertEquals("\"日记\"", FullTextIndex.matchQuery("日记"))
    }

    @Test
    fun `关键词里的引号会转义，不会破坏语法`() {
        assertEquals("\"a\"\"b\"", FullTextIndex.matchQuery("a\"b"))
    }

    @Test
    fun `AND OR 这类词被短语包住，不会被当成操作符`() {
        assertEquals("\"a OR b\"", FullTextIndex.matchQuery("a OR b"))
    }
}
