package com.synn.diary.data.db

import androidx.core.content.edit
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 数据库的口令。
 *
 * 流程：随机 32 字节 → 转成 hex 当 SQLCipher 口令 → 用 AndroidKeyStore 里的
 * AES-256-GCM 密钥把它裹起来，存进 `db_security` 偏好。
 *
 * 为什么不直接从密码派生：4 位 PIN 只有 1 万种，离线爆破几分钟就能拆开库，
 * 加密也就白做了。所以库口令跟 PIN 无关，PIN 只是 UI 闸门。
 *
 * 代价（要记住）：**卸装重装会带走 Keystore 里的密钥**，那时候库打不开，
 * 备份包（里面是加密库）也打不开。换机前先在旧机器上导出、并想清楚这一点。
 */
object DbKey {

    private const val PREFS = "db_security"
    private const val KEY_WRAPPED = "wrapped_key"
    private const val ALIAS = "diary_db_key"
    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    private const val PASSPHRASE_BYTES = 32

    /** 拿口令；没有就生成一个。返回 null 表示 Keystore 不可用。 */
    // 必须先落盘再建库，所以用 commit 版本的 edit
    fun passphrase(context: Context): String? = runCatching {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wrapped = prefs.getString(KEY_WRAPPED, null)
        if (wrapped != null) {
            unseal(wrapped)
        } else {
            val hex = ByteArray(PASSPHRASE_BYTES)
                .also { SecureRandom().nextBytes(it) }
                .joinToString("") { byte -> "%02x".format(byte) }
            // commit() 而不是 apply()：第一次建库前必须已经落盘
            prefs.edit(commit = true) { putString(KEY_WRAPPED, seal(hex)) }
            hex
        }
    }.getOrNull()

    private fun seal(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv + cipherText)
    }

    private fun unseal(sealed: String): String {
        val raw = Base64.getDecoder().decode(sealed)
        require(raw.size > IV_BYTES) { "数据库口令密文损坏" }
        val iv = raw.copyOfRange(0, IV_BYTES)
        val cipherText = raw.copyOfRange(IV_BYTES, raw.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }
}
