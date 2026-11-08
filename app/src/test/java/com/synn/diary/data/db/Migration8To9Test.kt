package com.synn.diary.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * v8 → v9 的真迁移测试：用 Room 导出的 8.json 建出真实的老库，
 * 跑 MIGRATION_8_9，再确认"列加上了、数据还在"。
 */
@RunWith(RobolectricTestRunner::class)
class Migration8To9Test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `加 font 列且旧数据保留`() {
        val dbFile = context.getDatabasePath("mig8.db").apply {
            parentFile?.mkdirs()
            delete()
            File(path + "-wal").delete()
            File(path + "-shm").delete()
        }

        createVersion8Database(dbFile)

        val helper = openHelper(dbFile.name)
        try {
            val db = helper.writableDatabase
            assertEquals(8, db.version)
            // 迁移前没有 font 列
            assertNull(columns(db, "entry")["font"])

            AppDatabase.MIGRATION_8_9.migrate(db)

            assertEquals("TEXT", columns(db, "entry")["font"])
            db.query("SELECT title, body FROM entry WHERE id = 1").use { c ->
                assertEquals(true, c.moveToFirst())
                assertEquals("旧标题", c.getString(0))
                assertEquals("旧正文", c.getString(1))
            }
        } finally {
            helper.close()
        }
    }

    /** 照着 app/schemas/.../8.json 建库，避免手抄 DDL 抄错。 */
    private fun createVersion8Database(file: File) {
        val schema = File("schemas/com.synn.diary.data.db.AppDatabase/8.json")
        require(schema.exists()) { "找不到导出的 schema：${schema.absolutePath}" }
        val json = JSONObject(schema.readText()).getJSONObject("database")

        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        try {
            val entities = json.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                val create = entity.getString("createSql").replace("\${TABLE_NAME}", table)
                db.execSQL(create)
                entity.optJSONArray("indices")?.let { indices ->
                    for (j in 0 until indices.length()) {
                        val index = indices.getJSONObject(j)
                        val indexCreate = index.getString("createSql")
                            .replace("\${TABLE_NAME}", table)
                        db.execSQL(indexCreate)
                    }
                }
            }
            db.execSQL(
                "INSERT INTO entry (uuid, entryDate, createdAt, updatedAt, title, body, favorite, locked) " +
                    "VALUES ('uuid-1', 20765, 1, 1, '旧标题', '旧正文', 0, 0)",
            )
            db.version = 8
        } finally {
            db.close()
        }
    }

    private fun openHelper(name: String): SupportSQLiteOpenHelper {
        val callback = object : SupportSQLiteOpenHelper.Callback(8) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config)
    }

    private fun columns(db: SupportSQLiteDatabase, table: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val nameIndex = c.getColumnIndex("name")
            val typeIndex = c.getColumnIndex("type")
            while (c.moveToNext()) map[c.getString(nameIndex)] = c.getString(typeIndex)
        }
        return map
    }
}
