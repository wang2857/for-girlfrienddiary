package com.synn.diary.data.lock

import java.security.SecureRandom
import java.util.Locale

/**
 * 恢复码：忘掉 4 位密码时唯一能救回日记的东西。
 *
 * 用 32 个"不会认错"的字符（去掉了 0/O、1/I/L 这类容易抄错的）
 * 生成 16 位，折成 4 组显示。熵是 16 × 5 = 80 bit，剩下的空间够，
 * 又短到能抄在一张纸上。
 */
object RecoveryCode {

    private const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
    const val LENGTH = 16

    private val random = SecureRandom()

    fun generate(): String = buildString(LENGTH) {
        repeat(LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
    }

    /** 显示成 "XXXX XXXX XXXX XXXX"，抄写时不容易串行。 */
    fun format(code: String): String =
        normalize(code).chunked(4).joinToString(" ")

    /** 用户可能带空格、小写、横杠，统一成大写字母数字再比对。 */
    fun normalize(raw: String): String =
        raw.filter { it.isLetterOrDigit() }.uppercase(Locale.ROOT)
}
