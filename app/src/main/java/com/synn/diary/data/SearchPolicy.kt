package com.synn.diary.data

/**
 * 搜索走哪条路：FTS 索引还是 LIKE。
 *
 * 单独抽出来是为了能测：trigram 只索引 3 个**字符**的窗口，
 * 而 Kotlin 的 `String.length` 数的是 UTF-16 单元——一个 emoji 占 2 个。
 * 按 length 判断会把"两个 emoji"当成 4 个字丢给 FTS，结果一条也搜不到。
 */
object SearchPolicy {

    const val MIN_FTS_CHARS = 3

    fun usesFullText(query: String, ftsAvailable: Boolean): Boolean {
        if (!ftsAvailable) return false
        val clean = query.trim()
        if (clean.isEmpty()) return false
        return clean.codePointCount(0, clean.length) >= MIN_FTS_CHARS
    }
}
