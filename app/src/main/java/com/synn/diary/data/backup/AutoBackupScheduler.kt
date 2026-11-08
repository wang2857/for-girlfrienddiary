package com.synn.diary.data.backup

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** 把自动备份排进 WorkManager：每天凌晨 3 点跑一次。 */
object AutoBackupScheduler {

    private const val WORK_NAME = "auto-backup"
    private val BACKUP_TIME: LocalTime = LocalTime.of(3, 0)

    fun schedule(context: Context) {
        // 选凌晨 3 点：手机在充电、没人用，读写数据库不会和写作抢
        val delayMinutes = Duration.between(
            LocalDateTime.now(),
            nextTrigger(),
        ).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        AutoBackup.setEnabled(context, enabled)
        if (enabled) schedule(context) else cancel(context)
    }

    /** 应用启动时校正：开关开着但任务被系统清掉了就补排一次。 */
    fun ensureScheduled(context: Context) {
        if (AutoBackup.isEnabled(context)) schedule(context)
    }

    private fun nextTrigger(): LocalDateTime {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(BACKUP_TIME)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }
}
