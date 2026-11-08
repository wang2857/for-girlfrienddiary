package com.synn.diary.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synn.diary.DiaryApp

/** 每天跑一次自动备份；失败了让 WorkManager 按退避重试。 */
class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!AutoBackup.isEnabled(applicationContext)) return Result.success()
        val container = (applicationContext as DiaryApp).container
        return if (AutoBackup.run(applicationContext, container).isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
