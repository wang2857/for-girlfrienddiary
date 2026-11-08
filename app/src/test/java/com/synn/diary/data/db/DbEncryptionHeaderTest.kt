package com.synn.diary.data.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DbEncryptionHeaderTest {

    @Test
    fun `明文 SQLite 文件头`() {
        val header = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII) + ByteArray(0)
        assertTrue(DbEncryption.isPlaintextHeader(header.copyOf(16)))
    }

    @Test
    fun `加密库的头是随机的，不像明文`() {
        val random = ByteArray(16) { (it * 37 + 11).toByte() }
        assertFalse(DbEncryption.isPlaintextHeader(random))
    }

    @Test
    fun `太短不判定为明文`() {
        assertFalse(DbEncryption.isPlaintextHeader("SQLite".toByteArray()))
    }
}
