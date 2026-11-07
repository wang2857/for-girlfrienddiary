package com.synn.diary.data

import android.content.Context
import androidx.room.Room
import com.synn.diary.data.db.AppDatabase
import com.synn.diary.data.media.ImageStore
import com.synn.diary.data.lock.LockManager
import com.synn.diary.data.lock.LockStore
import com.synn.diary.data.memory.MemoryPressureBridge
import com.synn.diary.data.weather.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 手写依赖容器。
 *
 * 首版故意不上 Hilt：这个体量的依赖图只有三行，KSP/KAPT 与 AGP 的版本耦合反而更容易
 * 在升级时炸。等真正出现需要作用域（后台任务、多进程）的依赖时再换成 Hilt，接口不用动。
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "diary.db",
    )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
        )
        .build()

    val weatherService = WeatherService(appContext)

    val imageStore = ImageStore(appContext)

    val trashSettings = TrashSettings(appContext)

    val entryRepository = EntryRepository(
        entryDao = database.entryDao(),
        tagDao = database.tagDao(),
        attachmentDao = database.attachmentDao(),
        imageStore = imageStore,
    )

    val memoryPressureBridge = MemoryPressureBridge(appContext)

    val lockStore = LockStore(appContext)
    val lockManager = LockManager(lockStore)

    /**
     * 和 UI 生命周期无关的写盘作用域：用户点返回时编辑器 ViewModel 会被销毁，
     * 但最后一笔内容必须落库，所以这里用 Application 级的 scope 兜底。
     */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
