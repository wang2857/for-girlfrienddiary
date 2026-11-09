package com.synn.diary.util

import org.junit.Assert.assertFalse
import org.junit.Test

class OsFlavorTest {

    @Test
    fun `非 vivo 机型不开玻璃效果`() {
        // 测试跑在 JVM 上，Build 是桩，读不到 vivo 属性
        assertFalse(OsFlavor.supportsLiquidGlass)
    }
}
