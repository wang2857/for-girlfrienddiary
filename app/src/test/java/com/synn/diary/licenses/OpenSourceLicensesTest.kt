package com.synn.diary.licenses

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.synn.diary.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 「设置 → 关于 → 开源许可」读的是 res/raw/licenses.txt。
 * 这里守着两件事：文件真的被打进包里、内容确实是完整许可而不是占位符。
 */
@RunWith(RobolectricTestRunner::class)
class OpenSourceLicensesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val text: String by lazy {
        context.resources.openRawResource(R.raw.licenses).bufferedReader().use { it.readText() }
    }

    @Test
    fun `许可页带 MIT 全文`() {
        assertTrue(text.length > 6000)
        assertTrue(text.contains("MIT License"))
        assertTrue(text.contains("Permission is hereby granted, free of charge"))
        assertTrue(text.contains("Copyright (c) 2026 Synn"))
    }

    @Test
    fun `许可页带字体 OFL 全文与保留字体名`() {
        assertTrue(text.contains("SIL OPEN FONT LICENSE Version 1.1"))
        assertTrue(text.contains("Reserved Font Name"))
        assertTrue(text.contains("霞鹜文楷"))
        // OFL 要求随字体分发时保留版权声明
        assertTrue(text.contains("Copyright 2021-2026 LXGW"))
    }

    @Test
    fun `许可页列出随包分发的关键组件`() {
        listOf("SQLCipher", "Coil", "AndroidX Room", "Apache License 2.0", "BSD 3-Clause")
            .forEach { assertTrue("缺少 $it", text.contains(it)) }
    }
}
