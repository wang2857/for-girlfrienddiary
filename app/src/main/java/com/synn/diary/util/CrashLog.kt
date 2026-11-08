package com.synn.diary.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 闪退现场。
 *
 * 写普通文件而不是接第三方服务：这应用不联网，日志也只该留在手机里。
 * 上限 10 份，多了删最旧的；用户能在设置里直接看和复制。
 */
object CrashLog {

    private const val MAX_FILES = 10

    fun dir(context: Context): File = File(context.filesDir, "crash").apply { mkdirs() }

    /** 现有的日志，新的在前。 */
    fun files(context: Context): List<File> = dir(context).listFiles()
        ?.filter { it.isFile && it.name.endsWith(".txt") }
        ?.sortedByDescending { it.name }
        .orEmpty()

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 记日志本身出问题也不能把原始崩溃吞掉
            runCatching { write(app, thread, throwable) }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            }
        }
    }

    fun clear(context: Context) {
        files(context).forEach { runCatching { it.delete() } }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stack = StringWriter().also { writer ->
            throwable.printStackTrace(PrintWriter(writer))
        }.toString()

        val text = buildString {
            appendLine("时间：${formatTime(System.currentTimeMillis())}")
            appendLine("版本：${versionName(context)}")
            appendLine("机型：${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("系统：Android ${Build.VERSION.RELEASE}（SDK ${Build.VERSION.SDK_INT}）")
            appendLine("线程：${thread.name}")
            appendLine()
            append(stack)
        }

        val file = File(dir(context), "crash-${stamp()}.txt")
        file.writeText(text)
        files(context).drop(MAX_FILES).forEach { it.delete() }
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(millis))

    private fun stamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())

    private fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "未知"
}
