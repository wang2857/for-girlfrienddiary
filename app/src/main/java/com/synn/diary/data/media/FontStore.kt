package com.synn.diary.data.media

import android.content.Context
import android.net.Uri
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 用户自己导入的正文字体。
 *
 * 只存文件、不建表：字体以 `file:<文件名>` 的形式记在 `entry.font` 上，
 * 和纸张一样是"这条日记的一部分"。字体文件坏了最多退回系统字体，不会影响正文。
 */
class FontStore(context: Context) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, "fonts").apply { mkdirs() }

    fun file(name: String): File = File(dir, name)

    fun list(): List<File> = dir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".ttf", ignoreCase = true) }
        ?.sortedByDescending { it.name }
        .orEmpty()

    /** 展示名：`<时间戳>-原文件名.ttf` 里取后半段。 */
    fun displayName(fileName: String): String =
        fileName.substringAfter('-', fileName).removeSuffix(".ttf")

    /** 导入一个 ttf/otf：复制到私有目录，校验文件头像字体才保留。 */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val raw = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            val clean = raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
                .takeLast(48)
                .ifBlank { "font.ttf" }
                .let { if (it.endsWith(".ttf", true)) it else "$it.ttf" }
            val target = File(dir, "${System.currentTimeMillis()}-$clean")
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            if (!looksLikeFont(target)) {
                target.delete()
                return@runCatching null
            }
            target.name
        }.getOrNull()
    }

    /** TrueType 的魔数：`00 01 00 00` / `true` / `OTTO` / `ttcf`。 */
    private fun looksLikeFont(file: File): Boolean {
        if (file.length() < 1024) return false
        val head = ByteArray(4)
        file.inputStream().use { input -> if (input.read(head) < 4) return false }
        val tag = String(head, Charsets.ISO_8859_1)
        return tag == "OTTO" || tag == "true" || tag == "ttcf" ||
            (head[0] == 0.toByte() && head[1] == 1.toByte() && head[2] == 0.toByte())
    }
}
