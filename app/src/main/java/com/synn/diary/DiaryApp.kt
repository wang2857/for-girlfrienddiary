package com.synn.diary

import android.app.Application
import com.synn.diary.data.AppContainer
import com.synn.diary.data.backup.AutoBackupScheduler
import com.synn.diary.notify.ReminderScheduler
import com.synn.diary.util.CrashLog
import kotlinx.coroutines.launch

class DiaryApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 第一件事就装崩溃捕获：后面的初始化万一炸了也要留下证据
        CrashLog.install(this)
        // vivo 公平运行内存机制：接收 TRIM / KILL 广播并回执
        container.memoryPressureBridge.initialize()

        // 不变量：应用锁没开时不该存在"上锁的日记"（没有凭证就没法验证）
        if (!container.lockManager.enabled.value) {
            container.appScope.launch {
                runCatching { container.entryRepository.unlockAllEntries() }
            }
        }

        // 提醒开关开着就确保排期还在（系统清理 / 换机后补排）
        container.appScope.launch {
            runCatching { ReminderScheduler.ensureScheduled(this@DiaryApp) }
        }

        // 自动备份同理：开关开着但任务被清了就补排
        container.appScope.launch {
            runCatching { AutoBackupScheduler.ensureScheduled(this@DiaryApp) }
        }

        // 启动时清掉没人引用的图片（删标记不删文件的兜底）
        container.appScope.launch {
            runCatching { container.entryRepository.cleanupOrphanImages() }
        }

        // 启动时清掉回收站里超过保留期的条目
        container.appScope.launch {
            runCatching { container.entryRepository.purgeExpired(container.trashSettings.retentionDays) }
        }
    }
}
