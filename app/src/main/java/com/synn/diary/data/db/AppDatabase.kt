package com.synn.diary.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库 v9。
 *
 * 0.35.0 起**不再维护升级链**：这个 app 从没发布过，旧库的唯一出路是删掉重建
 * （容器里配了 `fallbackToDestructiveMigration`）。这样 schema 可以随便调，
 * 不用为几年前的结构背迁移代码；真要发布，再把迁移补起来。
 */
@Database(
    entities = [EntryEntity::class, TagEntity::class, EntryTagEntity::class],
    version = 9,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    abstract fun tagDao(): TagDao

    companion object {

        /** v8 → v9：加一列正文字体。加列是安全的窄改动，所以这条走迁移、不清库。 */
        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `font` TEXT")
            }
        }

        /** 建库/开库时把全文索引装好（幂等）。 */
        val SEARCH_INDEX_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) = FullTextIndex.install(db)

            override fun onOpen(db: SupportSQLiteDatabase) = FullTextIndex.install(db)
        }
    }
}
