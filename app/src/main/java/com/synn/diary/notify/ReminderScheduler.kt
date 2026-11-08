package com.synn.diary.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.util.concurrent.TimeUnit

/** 负责把每日提醒排进 WorkManager。 */
object ReminderScheduler {

    private const val WORK_NAME = "daily-reminder"

    fun schedule(context: Context) {
        // 从现在算到下一个 21:00，作为首次延迟；之后每 24 小时一次。
        // 国内 ROM 的省电策略可能把它推迟几十分钟，能接受——提醒本身不需要精确到分。
        val delayMinutes = Duration.between(
            java.time.LocalDateTime.now(),
            DailyReminder.nextTrigger(),
        ).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            // 已经排过就保留原来的节奏，别每次打开设置都重置计时
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** 设置里切开关时调用。 */
    fun setEnabled(context: Context, enabled: Boolean) {
        DailyReminder.setEnabled(context, enabled)
        if (enabled) schedule(context) else cancel(context)
    }

    /** 应用启动时校正：开关开着但任务被系统清掉了就补排一次。 */
    fun ensureScheduled(context: Context) {
        if (DailyReminder.isEnabled(context)) schedule(context)
    }
}
