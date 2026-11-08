package com.synn.diary.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.synn.diary.data.db.AppDatabase
import com.synn.diary.data.db.EntryDao
import com.synn.diary.data.db.EntryEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FTS5 触发器只能在真实 SQLite（带 FTS5）上测，所以放 androidTest。
 * Robolectric 的 SQLite 没有 fts5 模块。
 */
@RunWith(AndroidJUnit4::class)
class FtsSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EntryDao
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        System.loadLibrary("sqlcipher")
        dbFile = context.getDatabasePath("fts-sync-test.db").apply {
            parentFile?.mkdirs()
            File(path + "-wal").delete()
            File(path + "-shm").delete()
            delete()
        }
        db = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.name)
            .openHelperFactory(SupportOpenHelperFactory("fts-sync-test-pass".toByteArray()))
            .addCallback(AppDatabase.SEARCH_INDEX_CALLBACK)
            .allowMainThreadQueries()
            .build()
        dao = db.entryDao()
    }

    @After
    fun tearDown() {
        db.close()
        dbFile.delete()
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
    }

    @Test
    fun `插入会进索引，更新会重写，删除会清掉`() = runBlocking {
        val id = dao.insert(entry(body = "永和九年岁在癸丑"))

        assertEquals(1, ftsCount())
        assertEquals(listOf(id), searchIds("和九年"))

        val current = dao.get(id)!!
        dao.update(current.copy(body = "换了内容，兰亭已矣"))
        assertEquals(1, ftsCount())
        assertTrue(searchIds("和九年").isEmpty())
        // 注意：trigram 只索引 3 字窗口，"兰亭"这种两字词本来就命不中（生产里会退回 LIKE）
        assertEquals(listOf(id), searchIds("兰亭已"))

        dao.hardDelete(id)
        assertEquals(0, ftsCount())
        assertTrue(searchIds("兰亭").isEmpty())
    }

    @Test
    fun `两条都命中时按时间倒序`() = runBlocking {
        val older = dao.insert(entry(body = "今天读书笔记", createdAt = 1000))
        val newer = dao.insert(entry(body = "今天读书摘抄", createdAt = 2000))

        val ids = searchIds("今天读")
        assertEquals(listOf(newer, older), ids)
    }

    private suspend fun ftsCount(): Int = db.query("SELECT count(*) FROM entry_fts", null).use { c ->
        c.moveToFirst()
        c.getInt(0)
    }

    private suspend fun searchIds(keyword: String): List<Long> {
        val sql = """
            SELECT * FROM entry
            WHERE entry.deletedAt IS NULL
              AND entry.id IN (SELECT rowid FROM entry_fts WHERE entry_fts MATCH ?)
            ORDER BY entry.entryDate DESC, entry.createdAt DESC
        """.trimIndent()
        return dao.searchFts(SimpleSQLiteQuery(sql, arrayOf("\"$keyword\"")))
            .first()
            .map { it.entry.id }
    }

    private fun entry(
        body: String,
        createdAt: Long = System.currentTimeMillis(),
    ) = EntryEntity(
        uuid = UUID.randomUUID().toString(),
        entryDate = 20765,
        createdAt = createdAt,
        updatedAt = createdAt,
        title = "",
        body = body,
    )
}
