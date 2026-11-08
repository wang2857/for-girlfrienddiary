package com.synn.diary.data.lock

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 恢复码的"可再显示"版本：用 AndroidKeyStore 里的一把 AES-256-GCM 密钥封起来。
 *
 * 为什么不是用 4 位密码派生密钥：改密码时手上没有旧密码，重新封装就得要求用户
 * 先输旧密码，或者让旧的密文永远解不开。用 Keystore 密钥就跟密码解耦了，
 * 改密码、用恢复码重置都不影响。
 *
 * 代价要说清楚：能拿到 root / adb 备份的人也能解开它。不过这应用本来就是明文
 * SQLite，真正防的是"捡到手机的人"，不是"拿到文件的人"。
 */
internal object RecoverySealer {

    private const val ALIAS = "diary_recovery_code"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BYTES = 256

    fun seal(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        // iv 拼在密文前面：GCM 的 iv 不需要保密，解密时按长度切回来就行
        return Base64.getEncoder().encodeToString(cipher.iv + cipherText)
    }

    fun unseal(sealed: String): String {
        val raw = Base64.getDecoder().decode(sealed)
        require(raw.size > IV_BYTES) { "恢复码密文损坏" }
        val iv = raw.copyOfRange(0, IV_BYTES)
        val cipherText = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    /** 关掉应用锁时把密钥一起删掉，密文也就成了废纸。 */
    fun forget() {
        runCatching { keyStore().deleteEntry(ALIAS) }
    }

    private fun key(): SecretKey {
        val store = keyStore()
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BYTES)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
}
