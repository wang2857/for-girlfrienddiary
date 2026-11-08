package com.synn.diary.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 跨设备备份的"钥匙套"。
 *
 * 数据库口令本来被本机的 Keystore 包着，换机就解不开了。跨设备备份里再放一份
 * 口令，用**恢复码**派生的密钥封起来：新手机导入时输入当年抄下的 16 位恢复码，
 * 就能把口令解出来、把备份库重新加密成本机口令。
 *
 * 格式：`[salt 16][iv 12][ciphertext+tag]`，PBKDF2-HMAC-SHA256，20 万次迭代。
 */
object BackupKey {

    private const val ITERATIONS = 200_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    fun wrap(dbPassphrase: String, recoveryCode: String): ByteArray {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val key = derive(recoveryCode, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val cipherText = cipher.doFinal(dbPassphrase.toByteArray(Charsets.UTF_8))
        return salt + cipher.iv + cipherText
    }

    /** 解不出来（恢复码不对 / 数据损坏）返回 null。 */
    fun unwrap(blob: ByteArray, recoveryCode: String): String? = runCatching {
        require(blob.size > SALT_BYTES + IV_BYTES)
        val salt = blob.copyOfRange(0, SALT_BYTES)
        val iv = blob.copyOfRange(SALT_BYTES, SALT_BYTES + IV_BYTES)
        val cipherText = blob.copyOfRange(SALT_BYTES + IV_BYTES, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(recoveryCode, salt), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }.getOrNull()

    private fun derive(recoveryCode: String, salt: ByteArray): SecretKeySpec {
        // 恢复码统一成大写字母数字，跟密码校验那边一致
        val normalized = recoveryCode.filter { it.isLetterOrDigit() }.uppercase()
        val spec = PBEKeySpec(normalized.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
        return SecretKeySpec(bytes, "AES")
    }
}
