package com.synn.diary.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPolicyTest {

    @Test
    fun `短于 3 个字符走 LIKE`() {
        assertFalse(SearchPolicy.usesFullText("a", ftsAvailable = true))
        assertFalse(SearchPolicy.usesFullText("ab", ftsAvailable = true))
        assertFalse(SearchPolicy.usesFullText("日记", ftsAvailable = true))
    }

    @Test
    fun `3 个字符及以上走索引`() {
        assertTrue(SearchPolicy.usesFullText("abc", ftsAvailable = true))
        assertTrue(SearchPolicy.usesFullText("日记本", ftsAvailable = true))
        assertTrue(SearchPolicy.usesFullText("  abc  ", ftsAvailable = true))
    }

    @Test
    fun `emoji 按码点算，两个 emoji 仍走 LIKE`() {
        // 一个 emoji 在 UTF-16 里占两个 length，所以 "😀😀".length == 4
        assertFalse(SearchPolicy.usesFullText("😀😀", ftsAvailable = true))
        assertTrue(SearchPolicy.usesFullText("😀😀😀", ftsAvailable = true))
    }

    @Test
    fun `空白查询不查`() {
        assertFalse(SearchPolicy.usesFullText("   ", ftsAvailable = true))
    }

    @Test
    fun `索引不可用时一律 LIKE`() {
        assertFalse(SearchPolicy.usesFullText("日记本", ftsAvailable = false))
    }
}
