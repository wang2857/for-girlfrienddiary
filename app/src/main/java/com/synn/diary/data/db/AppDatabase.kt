package com.synn.diary.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EntryEntity::class, TagEntity::class, EntryTagEntity::class, AttachmentEntity::class],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    abstract fun tagDao(): TagDao

    abstract fun attachmentDao(): AttachmentDao

    companion object {

        /**
         * v1 → v2：只新增 tag / entry_tag 两张表，entry 表一个字段都没动。
         * 手写迁移而不是 destructive migration —— 手机上已经装了 v1，升级不能丢记录。
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tag` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tag_name` ON `tag` (`name`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `entry_tag` (" +
                        "`entryId` INTEGER NOT NULL, " +
                        "`tagId` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`entryId`, `tagId`), " +
                        "FOREIGN KEY(`entryId`) REFERENCES `entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`tagId`) REFERENCES `tag`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_tag_tagId` ON `entry_tag` (`tagId`)")
            }
        }

        /**
         * v5 → v6：加收藏与单篇锁两个布尔列。老记录默认 false / false。
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `favorite` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `locked` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * v4 → v5：entry 加一个可空的 background，存纸张底色 id。老记录一律 NULL（默认纸）。
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `background` TEXT")
            }
        }

        /**
         * v3 → v4：新增 attachment 表。正文里存的是 `[[img:文件名]]` 标记，图片文件放 files/images/。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `attachment` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`entryId` INTEGER NOT NULL, " +
                        "`relPath` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`entryId`) REFERENCES `entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachment_entryId` ON `attachment` (`entryId`)")
            }
        }

        /**
         * v2 → v3：entry 加四个可空列（天气、地点、经纬度）。老记录一律 NULL，不编造历史天气。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `weatherText` TEXT")
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `locationText` TEXT")
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `latitude` REAL")
                db.execSQL("ALTER TABLE `entry` ADD COLUMN `longitude` REAL")
            }
        }
    }
}
