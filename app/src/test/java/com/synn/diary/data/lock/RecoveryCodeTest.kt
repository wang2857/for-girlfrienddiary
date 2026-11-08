package com.synn.diary.data.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {

    @Test
    fun `生成 16 位且不含易混字符`() {
        repeat(50) {
            val code = RecoveryCode.generate()
            assertEquals(RecoveryCode.LENGTH, code.length)
            assertTrue(code.all { c -> c in "23456789ABCDEFGHJKLMNPQRSTUVWXYZ" })
        }
    }

    @Test
    fun `格式化后是四组四位`() {
        assertEquals("ABCD EFGH JKLM NPQR", RecoveryCode.format("abcdefghjklmnpqr"))
        assertEquals("ABCD EFGH JKLM NPQR", RecoveryCode.format("AB CD-EF GHJK LMNP-QR"))
    }

    @Test
    fun `规范化去掉空格横杠并转大写`() {
        assertEquals("ABCDEFGH", RecoveryCode.normalize("ab cd-ef gh"))
        assertEquals("ABCDEFGH", RecoveryCode.normalize("  A B C D E F G H  "))
    }
}
