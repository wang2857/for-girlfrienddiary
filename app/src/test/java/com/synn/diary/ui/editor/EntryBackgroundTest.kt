package com.synn.diary.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EntryBackgroundTest {

    @Test
    fun `空值当成默认纸`() {
        assertEquals("default", EntryBackground.from(null).id)
        assertEquals("default", EntryBackground.from("").id)
    }

    @Test
    fun `预设 id 能还原`() {
        val cream = EntryBackground.from("cream")
        assertEquals("cream", cream.id)
        assertTrue(cream is EntryBackground.Preset)
    }

    @Test
    fun `自定义纸存 img 前缀`() {
        val custom = EntryBackground.from("img:bg_1.jpg")
        assertEquals("img:bg_1.jpg", custom.id)
        assertEquals("bg_1.jpg", (custom as EntryBackground.Custom).fileName)
    }

    @Test
    fun `不认识的 id 退回默认`() {
        assertEquals("default", EntryBackground.from("nope").id)
    }

    @Test
    fun `默认纸 isDefault 为真`() {
        assertTrue(EntryBackground.from("default").isDefault)
    }
}
