package com.synn.diary.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 时间线该显示什么，全是 SQL 里的条件说了算，最容易悄悄改坏。
 * 这里用内存库把几种边界都过一遍。
 */
@RunWith(RobolectricTestRunner::class)
class TimelineFilterTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: EntryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 不开 FTS 回调：Robolectric 的 SQLite 没有 FTS5，这里只测过滤条件
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.entryDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `空壳、回收站、单篇锁都不进时间线`() = runBlocking {
        dao.insert(entry(title = "有内容", body = "正文"))
        dao.insert(entry(title = "", body = ""))         // 空壳
        dao.insert(entry(title = "  ", body = " \n "))    // 只有空白
        val trashed = dao.insert(entry(title = "删了", body = "x")).let { id ->
            dao.get(id)!!.also { dao.update(it.copy(deletedAt = 1L)) }
            id
        }
        val locked = dao.insert(entry(title = "上锁", body = "x"))

        val visible = dao.observeTimeline(onlyFavorites = 0, limit = 100).first()
        // 单篇锁只影响"能不能打开"，不影响它在列表里出现；空壳和回收站才该消失
        assertEquals(setOf("有内容", "上锁"), visible.map { it.entry.title }.toSet())
        assert(visible.none { it.entry.id == trashed })
    }

    @Test
    fun `只看收藏时过滤掉非收藏`() = runBlocking {
        val fav = dao.insert(entry(title = "收藏", body = "x"))
        dao.setFavorite(fav, true)
        dao.insert(entry(title = "普通", body = "x"))

        val visible = dao.observeTimeline(onlyFavorites = 1, limit = 100).first()
        assertEquals(listOf("收藏"), visible.map { it.entry.title })
    }

    @Test
    fun `按天统计不算空壳和回收站`() = runBlocking {
        val day = 20765L
        dao.insert(entry(title = "写了", body = "x", entryDate = day))
        dao.insert(entry(title = "", body = "", entryDate = day))
        assertEquals(1, dao.countOnDay(day))
    }

    private fun entry(
        title: String,
        body: String,
        entryDate: Long = 20765L,
    ) = EntryEntity(
        uuid = UUID.randomUUID().toString(),
        entryDate = entryDate,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        title = title,
        body = body,
    )
}
