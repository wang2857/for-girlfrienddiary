package com.synn.diary.data.db

import android.database.Cursor
import android.util.Log
import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase

/**
 * 明文库 → 加密库的一次性迁移。
 *
 * 判断方式：看文件头是不是 `SQLite format 3`（明文 SQLite 的魔数）。加密库的头是随机的。
 *
 * 迁移不是用 `ATTACH + sqlcipher_export` 做的——那条路在真机上会以
 * "unable to open database (code 14)" 失败（空口令打开的连接里 attach 加密库），
 * 所以改成最笨也最可控的办法：照抄 schema → 逐表搬数据 → 建索引 → 校验条数 → 原子替换。
 */
object DbEncryption {

    private const val TAG = "DbEncryption"
    private const val PLAINTEXT_MAGIC = "SQLite format 3"

    /** 建表顺序有依赖：entry 要在 entry_tag 之前。 */
    private val TABLE_ORDER = listOf("entry", "tag", "entry_tag", "room_master_table", "android_metadata")

    fun isPlaintext(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        val header = ByteArray(16)
        file.inputStream().use { input ->
            if (input.read(header) < 16) return false
        }
        return String(header, Charsets.US_ASCII).startsWith(PLAINTEXT_MAGIC)
    }

    /**
     * 库存在且是明文就转成加密库；已经是密文就什么都不做。
     *
     * 失败时原库保持不动（也只是多一个临时文件），不会把数据写坏。
     */
    fun migrateIfPlaintext(file: File, passphrase: String, tempDir: File): Boolean {
        if (!file.exists()) return true
        if (!isPlaintext(file)) return true

        Log.i(TAG, "发现明文库，开始迁移到 SQLCipher")
        val encrypted = File(tempDir, "diary.db.enc")
        encrypted.delete()

        val plainCount = runCatching {
            val plain = android.database.sqlite.SQLiteDatabase.openDatabase(
                file.path,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
            )
            try {
                val target = SQLiteDatabase.openOrCreateDatabase(encrypted.path, passphrase, null, null)
                try {
                    copySchema(plain, target)
                    TABLE_ORDER.forEach { table -> copyRows(plain, target, table) }
                    copyIndexes(plain, target)
                    target.execSQL("PRAGMA user_version = ${plain.version}")
                } finally {
                    target.close()
                }
                countEntries(plain)
            } finally {
                plain.close()
            }
        }.getOrElse { error ->
            Log.e(TAG, "迁移失败，保留原库", error)
            encrypted.delete()
            return false
        }

        val encryptedCount = runCatching {
            val db = SQLiteDatabase.openOrCreateDatabase(encrypted.path, passphrase, null, null)
            try {
                countEntries(db)
            } finally {
                db.close()
            }
        }.getOrElse { error ->
            Log.e(TAG, "加密库校验失败，保留原库", error)
            encrypted.delete()
            return false
        }

        if (encryptedCount != plainCount) {
            Log.e(TAG, "条数对不上（$plainCount → $encryptedCount），保留原库")
            encrypted.delete()
            return false
        }

        // 原库的 WAL/SHM 是明文时代的残留，必须先删，否则新库会读到旧页
        File(file.path + "-wal").delete()
        File(file.path + "-shm").delete()
        if (!encrypted.renameTo(file)) {
            encrypted.copyTo(file, overwrite = true)
            encrypted.delete()
        }
        Log.i(TAG, "迁移完成：$plainCount 条")
        return true
    }

    /** 只抄 CREATE TABLE，索引等数据搬完再建。 */
    private fun copySchema(
        plain: android.database.sqlite.SQLiteDatabase,
        target: SQLiteDatabase,
    ) {
        val statements = mutableListOf<String>()
        plain.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type='table' AND sql IS NOT NULL " +
                "AND name NOT LIKE 'sqlite_%'",
            null,
        ).use { c ->
            while (c.moveToNext()) c.getString(0)?.let(statements::add)
        }
        // room_master_table / android_metadata 可能先于业务表出现，按依赖顺序执行
        val ordered = statements.sortedBy { sql ->
            val name = TABLE_ORDER.indexOfFirst { sql.contains("`$it`", ignoreCase = true) }
            if (name < 0) TABLE_ORDER.size else name
        }
        ordered.forEach { target.execSQL(it) }
    }

    private fun copyIndexes(
        plain: android.database.sqlite.SQLiteDatabase,
        target: SQLiteDatabase,
    ) {
        plain.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL",
            null,
        ).use { c ->
            while (c.moveToNext()) c.getString(0)?.let { target.execSQL(it) }
        }
    }

    private fun copyRows(
        plain: android.database.sqlite.SQLiteDatabase,
        target: SQLiteDatabase,
        table: String,
    ) {
        if (!tableExists(plain, table)) return
        plain.rawQuery("SELECT * FROM `$table`", null).use { c ->
            val columns = c.columnNames
            if (columns.isEmpty()) return
            val names = columns.joinToString(",") { "`$it`" }
            val placeholders = columns.joinToString(",") { "?" }
            val insert = "INSERT INTO `$table` ($names) VALUES ($placeholders)"
            while (c.moveToNext()) {
                val args = Array<Any?>(columns.size) { index ->
                    when (c.getType(index)) {
                        Cursor.FIELD_TYPE_NULL -> null
                        Cursor.FIELD_TYPE_INTEGER -> c.getLong(index)
                        Cursor.FIELD_TYPE_FLOAT -> c.getDouble(index)
                        Cursor.FIELD_TYPE_BLOB -> c.getBlob(index)
                        else -> c.getString(index)
                    }
                }
                target.execSQL(insert, args)
            }
        }
    }

    private fun tableExists(db: android.database.sqlite.SQLiteDatabase, table: String): Boolean =
        db.rawQuery(
            "SELECT count(*) FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(table),
        ).use { c -> c.moveToFirst() && c.getInt(0) > 0 }

    private fun countEntries(db: android.database.sqlite.SQLiteDatabase): Int =
        db.rawQuery("SELECT count(*) FROM entry", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }

    private fun countEntries(db: SQLiteDatabase): Int {
        val cursor = db.rawQuery("SELECT count(*) FROM entry", null)
        return try {
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } finally {
            cursor.close()
        }
    }
}
