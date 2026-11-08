package com.synn.diary.data.backup

import android.content.Context
import android.content.SharedPreferences
import com.synn.diary.data.AppContainer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 自动备份：每天往应用私有目录里放一份完整 zip，只留最近几份。
 *
 * 它防的是"手滑"——误清空回收站、导入覆盖导入、数据库被写坏。
 * 防不了卸载和丢手机，那种事要靠手动导出到手机外面。
 */
object AutoBackup {

    private const val PREFS = "auto_backup"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_LAST_AT = "last_at"
    private const val KEY_LAST_NOTE = "last_note"

    /** 留最近 7 份。再多意义不大，占的空间却是实打实的。 */
    const val MAX_COPIES = 7

    /** 7 份里如果图片多，总量可能很吓人；超过这个数就从最旧的开始删。 */
    private const val MAX_BYTES = 600L * 1024 * 1024

    fun dir(context: Context): File = File(context.filesDir, "backup").apply { mkdirs() }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun lastRunAt(context: Context): Long = prefs(context).getLong(KEY_LAST_AT, 0L)

    fun lastNote(context: Context): String? = prefs(context).getString(KEY_LAST_NOTE, null)

    /** 现有的备份，新的在前。 */
    fun files(context: Context): List<File> = dir(context).listFiles()
        ?.filter { it.isFile && it.name.endsWith(".zip") }
        ?.sortedByDescending { it.name }
        .orEmpty()

    suspend fun run(context: Context, container: AppContainer): Result<BackupInfo> =
        withContext(Dispatchers.IO) {
            // 上次如果在打包中途被取消（用户切走了设置页），会留下一个 .part
            dir(context).listFiles()?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }

            val result = runCatching {
                container.backupManager.exportToFile(File(dir(context), "diary-${stamp()}.zip"))
            }
            prefs(context).edit()
                .putLong(KEY_LAST_AT, System.currentTimeMillis())
                .putString(KEY_LAST_NOTE, result.fold({ "成功" }, { it.message ?: "失败" }))
                .apply()
            if (result.isSuccess) prune(context)
            result
        }

    private fun prune(context: Context) {
        // 先按份数砍，再按总大小砍
        files(context).drop(MAX_COPIES).forEach { it.delete() }

        // files() 里剩下的按新到旧排，从最旧的开始删到总体积达标
        val kept = files(context).sortedBy { it.name }
        var total = kept.sumOf { it.length() }
        for (file in kept) {
            if (total <= MAX_BYTES) break
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
}
