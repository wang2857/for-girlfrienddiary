package com.synn.diary.data.lock

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * 应用锁的密码存储。
 *
 * 只存 PBKDF2 哈希 + 随机盐，不存明文、也不存可逆密文——
 * 密码忘记就只能重置（日记本身还在，只是要重新设置锁）。
 */
class LockStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit { putBoolean(KEY_BIOMETRIC, value) }

    /** 默认不许截屏；用户明确打开才放行。 */
    var allowScreenshots: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_SCREENSHOTS, false)
        set(value) = prefs.edit { putBoolean(KEY_ALLOW_SCREENSHOTS, value) }

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit {
            putString(KEY_SALT, encode(salt))
            putString(KEY_HASH, encode(hash))
        }
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null)?.let(::decode) ?: return false
        val expected = prefs.getString(KEY_HASH, null)?.let(::decode) ?: return false
        // 定长比较，避免按字节提前退出泄露信息（本地场景影响很小，但没成本）
        return MessageDigest.isEqual(hash(pin, salt), expected)
    }

    // ---- 恢复码 ----
    // 和密码一样只存哈希：恢复码的意义是"忘密码时还能证明这手机是你的"，
    // 存明文等于把它变成第二把随时能捡到的钥匙。

    fun hasRecoveryCode(): Boolean = prefs.contains(KEY_RECOVERY_HASH)

    fun setRecoveryCode(code: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit {
            putString(KEY_RECOVERY_SALT, encode(salt))
            putString(KEY_RECOVERY_HASH, encode(hash(RecoveryCode.normalize(code), salt)))
        }
    }

    fun verifyRecoveryCode(code: String): Boolean {
        val salt = prefs.getString(KEY_RECOVERY_SALT, null)?.let(::decode) ?: return false
        val expected = prefs.getString(KEY_RECOVERY_HASH, null)?.let(::decode) ?: return false
        return MessageDigest.isEqual(hash(RecoveryCode.normalize(code), salt), expected)
    }

    // ---- 恢复码的"抄丢了也能再看" ----
    // 哈希只能验、不能还原，所以另存一份用 Keystore 密钥封起来的明文。
    // 查看时 UI 会先要求输 4 位密码，捡到手机的人看不到。

    /** 老版本只存过哈希，没有密文——那种情况没法再显示，只能重设。 */
    fun canRevealRecoveryCode(): Boolean = prefs.contains(KEY_RECOVERY_SEALED)

    fun sealRecoveryCode(code: String): Boolean = runCatching {
        prefs.edit {
            putString(KEY_RECOVERY_SEALED, RecoverySealer.seal(RecoveryCode.normalize(code)))
        }
        true
    }.getOrDefault(false)

    fun revealRecoveryCode(): String? = runCatching {
        prefs.getString(KEY_RECOVERY_SEALED, null)?.let(RecoverySealer::unseal)
    }.getOrNull()

    fun clear() {
        prefs.edit { clear() }
        // 密钥跟着一起删：留着密文也没用了，还省得以后有人拿它做文章
        RecoverySealer.forget()
    }

    private fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_BIOMETRIC = "biometric"
        const val KEY_ALLOW_SCREENSHOTS = "allow_screenshots"
        const val KEY_SALT = "salt"
        const val KEY_HASH = "hash"
        const val KEY_RECOVERY_SALT = "recovery_salt"
        const val KEY_RECOVERY_HASH = "recovery_hash"
        const val KEY_RECOVERY_SEALED = "recovery_sealed"
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_BITS = 256
    }
}
