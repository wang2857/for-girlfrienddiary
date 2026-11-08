package com.synn.diary.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.room.Room
import com.synn.diary.data.db.AppDatabase
import com.synn.diary.data.db.DbEncryption
import com.synn.diary.data.db.DbKey
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import com.synn.diary.data.media.FontStore
import com.synn.diary.data.media.ImageStore
import com.synn.diary.data.backup.BackupManager
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

    /** 数据库口令：随机生成、Keystore 裹起来，跟 4 位密码无关。 */
    private val dbPassphrase: String = DbKey.passphrase(appContext)
        ?: error("拿不到数据库口令（Keystore 不可用）")

    private val database: AppDatabase = run {
        // SQLCipher 的 native 库得自己加载，AAR 不会替你做
        System.loadLibrary("sqlcipher")
        // 不再兼容明文库：发现就删掉，让 Room 重新建一个加密的
        DbEncryption.discardPlaintext(appContext.getDatabasePath("diary.db"))
        Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "diary.db",
        )
            .openHelperFactory(SupportOpenHelperFactory(dbPassphrase.toByteArray()))
            // v8→v9 是正经迁移；更早的版本没有升级链，按产品决定直接重建
            .addMigrations(AppDatabase.MIGRATION_8_9)
            .addCallback(AppDatabase.SEARCH_INDEX_CALLBACK)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    val weatherService = WeatherService(appContext)

    val imageStore = ImageStore(appContext)

    val fontStore = FontStore(appContext)

    val trashSettings = TrashSettings(appContext)

    val editorSettings = EditorSettings(appContext)

    val backupManager = BackupManager(appContext, database, imageStore, fontStore, dbPassphrase) {
        // 恢复码的明文存在 Keystore 封装的副本里，导出时用它包口令（跨设备备份）
        lockManager.revealRecoveryCode()
    }

    /**
     * 选完文件到确认导入之间会经历"切后台 → 应用锁上锁 → 回来重建界面"，
     * 这个待办放在应用级，否则用户选完文件回来发现什么都没发生。
     */
    val pendingImportUri = MutableStateFlow<android.net.Uri?>(null)

    /**
     * 正在用系统界面（文件选择器 / 分享面板）。
     * 这类界面会把应用切到后台，若此时上锁，用户回来要先解锁、还丢掉了刚才的选择，
     * 所以这期间不上锁。
     */
    var externalUiInProgress = false

    /** 用户从每日提醒点进来：解锁后直接开一篇新日记。 */
    val pendingNewEntry = MutableStateFlow(false)

    val entryRepository = EntryRepository(
        entryDao = database.entryDao(),
        tagDao = database.tagDao(),
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
