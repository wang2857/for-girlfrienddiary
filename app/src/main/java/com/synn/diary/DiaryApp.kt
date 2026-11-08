package com.synn.diary

import android.app.Application
import com.synn.diary.data.AppContainer
import kotlinx.coroutines.launch

class DiaryApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // vivo 公平运行内存机制：接收 TRIM / KILL 广播并回执
        container.memoryPressureBridge.initialize()

        // 不变量：应用锁没开时不该存在"上锁的日记"（没有凭证就没法验证）
        if (!container.lockManager.enabled.value) {
            container.appScope.launch {
                runCatching { container.entryRepository.unlockAllEntries() }
            }
        }

        // 启动时清掉回收站里超过保留期的条目
        container.appScope.launch {
            runCatching { container.entryRepository.purgeExpired(container.trashSettings.retentionDays) }
        }
    }
}
