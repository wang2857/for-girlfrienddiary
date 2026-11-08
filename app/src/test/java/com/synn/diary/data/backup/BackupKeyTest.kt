package com.synn.diary.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupKeyTest {

    private val passphrase = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val recovery = "JG9T QKXZ K4UF R4VF"

    @Test
    fun `往返能拿回口令，且大小写空格不影响`() {
        val blob = BackupKey.wrap(passphrase, recovery)
        assertEquals(passphrase, BackupKey.unwrap(blob, recovery))
        assertEquals(passphrase, BackupKey.unwrap(blob, "jg9tqkxzk4ufr4vf"))
    }

    @Test
    fun `恢复码不对解不开`() {
        val blob = BackupKey.wrap(passphrase, recovery)
        assertNull(BackupKey.unwrap(blob, "AAAA BBBB CCCC DDDD"))
    }

    @Test
    fun `密文被篡改解不开`() {
        val blob = BackupKey.wrap(passphrase, recovery)
        blob[blob.size - 1] = (blob.last().toInt() xor 0x01).toByte()
        assertNull(BackupKey.unwrap(blob, recovery))
    }

    @Test
    fun `两次封装盐不同，密文也不同`() {
        val a = BackupKey.wrap(passphrase, recovery)
        val b = BackupKey.wrap(passphrase, recovery)
        assertNotEquals(a.toList(), b.toList())
        assertEquals(passphrase, BackupKey.unwrap(a, recovery))
        assertEquals(passphrase, BackupKey.unwrap(b, recovery))
    }

    @Test
    fun `太短的数据直接拒绝`() {
        assertFalse(BackupKey.unwrap(ByteArray(8), recovery) != null)
    }
}
