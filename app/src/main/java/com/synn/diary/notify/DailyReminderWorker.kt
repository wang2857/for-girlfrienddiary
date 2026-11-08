package com.synn.diary.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synn.diary.DiaryApp
import java.time.LocalDate

/**
 * 每天一次的提醒。
 *
 * 只在「当天还没写」时才发——OriginOS 的通知规范明确反对"仅为了让用户回到应用"的通知，
 * 已经写过的日子再提醒就属于打扰。
 */
class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as DiaryApp).container

        if (!DailyReminder.isEnabled(applicationContext)) return Result.success()

        val wroteToday = runCatching {
            container.entryRepository.hasEntryOn(LocalDate.now().toEpochDay())
        }.getOrElse { return Result.retry() }

        if (wroteToday) return Result.success()

        DailyReminder.notify(applicationContext)
        return Result.success()
    }
}
