package com.synn.diary.data

import com.synn.diary.data.db.EntryDao
import com.synn.diary.data.db.EntryEntity
import com.synn.diary.data.db.DayMoodRow
import com.synn.diary.data.db.EntryTagEntity
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.data.db.FullTextIndex
import com.synn.diary.data.db.TagDao
import com.synn.diary.data.db.TagEntity
import com.synn.diary.util.Dates
import java.util.UUID
import com.synn.diary.data.body.BodyMarkers
import com.synn.diary.data.media.ImageStore
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow

private const val PREFIX_CUSTOM_BACKGROUND = "img:"

class EntryRepository(
    private val entryDao: EntryDao,
    private val tagDao: TagDao,
    private val imageStore: ImageStore,
) {

    // ---- 读 ----

    fun timeline(
        tagId: Long? = null,
        onlyFavorites: Boolean = false,
        limit: Int = 200,
    ): Flow<List<EntryWithTags>> {
        val fav = if (onlyFavorites) 1 else 0
        return if (tagId == null) entryDao.observeTimeline(fav, limit) else entryDao.observeByTag(tagId, fav, limit)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = entryDao.setFavorite(id, favorite)

    suspend fun setLocked(id: Long, locked: Boolean) = entryDao.setLocked(id, locked)

    suspend fun find(id: Long): EntryEntity? = entryDao.get(id)

    /** 关闭应用锁时用：没有密码就没法再验证，索性把单篇锁一并解开，避免日记打不开。 */
    suspend fun unlockAllEntries() = entryDao.unlockAll()

    // ---- 回收站 ----

    fun trash(): Flow<List<EntryWithTags>> = entryDao.observeTrash()

    /** 当天有没有写过（空条目不算）。 */
    suspend fun hasEntryOn(epochDay: Long): Boolean = entryDao.countOnDay(epochDay) > 0

    suspend fun restore(id: Long) = entryDao.restore(id)

    /** 彻底删除：先清掉图片文件，再删行（行删掉后 entry_tag / attachment 由外键级联清理）。 */
    suspend fun purge(id: Long) {
        val entry = entryDao.get(id) ?: return
        BodyMarkers.allImages(entry.body).forEach { imageStore.delete(it) }
        entryDao.hardDelete(id)
    }

    /**
     * 清掉没被任何正文引用的图片文件。
     *
     * 只有点图片浮层的 × 才会删文件；如果直接删掉正文里的 `[[img:...]]` 标记，
     * 文件会永远留在磁盘上。启动时扫一遍兜底。
     */
    suspend fun cleanupOrphanImages(): Int {
        val referenced = entryDao.allBodies()
            .flatMap { BodyMarkers.allImages(it) }
            .toSet()
        // 自定义纸张也要算：换纸之后旧背景图就没人引用了
        val referencedBackgrounds = entryDao.allBackgrounds()
            .filterNotNull()
            .filter { it.startsWith(PREFIX_CUSTOM_BACKGROUND) }
            .map { it.removePrefix(PREFIX_CUSTOM_BACKGROUND) }
            .toSet()
        // 刚导入还没写进正文的文件别动（留 1 小时缓冲）
        val cutoff = System.currentTimeMillis() - 60 * 60 * 1000
        var removed = 0
        imageStore.listFiles().forEach { (name, modified) ->
            if (name !in referenced && modified < cutoff) {
                imageStore.delete(name)
                removed++
            }
        }
        imageStore.listBackgrounds().forEach { (name, modified) ->
            if (name !in referencedBackgrounds && modified < cutoff) {
                imageStore.deleteBackground(name)
                removed++
            }
        }
        return removed
    }

    /** 清空回收站。 */
    suspend fun emptyTrash() {
        entryDao.allTrashed().forEach { entry ->
            BodyMarkers.allImages(entry.body).forEach { imageStore.delete(it) }
        }
        entryDao.purgeAllTrash()
    }

    /** 把超过保留期的回收站条目彻底清掉，返回清理数量。 */
    suspend fun purgeExpired(retentionDays: Int): Int {
        // 0 = 永久保留，永远不自动清理
        if (retentionDays <= 0) return 0
        val cutoff = System.currentTimeMillis() - retentionDays.toLong() * 24 * 60 * 60 * 1000
        val expired = entryDao.expiredTrash(cutoff)
        expired.forEach { entry ->
            BodyMarkers.allImages(entry.body).forEach { imageStore.delete(it) }
        }
        return entryDao.purgeExpired(cutoff)
    }

    /**
     * 搜索：3 个字符（含中文，按字符算）以上走 FTS5 索引，更短的退回 LIKE。
     *
     * trigram 分词器只索引 3 字窗口，所以两字查询注定命不中，不如直接 LIKE——
     * 短词本身区分度低，全表扫一遍也不慢。
     */
    fun search(keyword: String): Flow<List<EntryWithTags>> {
        val clean = keyword.trim()
        if (clean.length < 3 || !FullTextIndex.enabled) return entryDao.search(clean)
        return entryDao.searchFts(
            SimpleSQLiteQuery(
                """
                SELECT * FROM entry
                WHERE deletedAt IS NULL
                  AND locked = 0
                  AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
                  AND id IN (SELECT rowid FROM entry_fts WHERE entry_fts MATCH ?)
                ORDER BY entryDate DESC, createdAt DESC
                """.trimIndent(),
                arrayOf(FullTextIndex.matchQuery(clean)),
            ),
        )
    }

    fun entriesOfDay(epochDay: Long): Flow<List<EntryWithTags>> = entryDao.observeDay(epochDay)

    fun dayMoods(fromDay: Long, toDay: Long): Flow<List<DayMoodRow>> =
        entryDao.observeDayMoods(fromDay, toDay)

    fun tags(): Flow<List<TagEntity>> = tagDao.observeAll()

    fun tagsOfEntry(entryId: Long): Flow<List<TagEntity>> = entryDao.observeTagsOfEntry(entryId)

    suspend fun get(id: Long): EntryEntity? = entryDao.get(id)

    // ---- 写 ----

    suspend fun create(entryDate: Long = Dates.todayEpochDay()): Long {
        val now = System.currentTimeMillis()
        return entryDao.insert(
            EntryEntity(
                uuid = UUID.randomUUID().toString(),
                entryDate = entryDate,
                createdAt = now,
                updatedAt = now,
                title = "",
                body = "",
            ),
        )
    }

    /**
     * 只更新内容字段，不覆盖 deletedAt —— 否则"删除后又触发一次自动保存"会把条目复活。
     */
    suspend fun save(id: Long, title: String, body: String, mood: String?, background: String?) {
        val current = entryDao.get(id) ?: return
        entryDao.update(
            current.copy(
                title = title,
                body = body,
                mood = mood,
                background = background,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateWeather(
        id: Long,
        weatherText: String?,
        locationText: String?,
        latitude: Double?,
        longitude: Double?,
    ) {
        entryDao.updateWeather(id, weatherText, locationText, latitude, longitude)
    }

    suspend fun softDelete(id: Long) {
        val current = entryDao.get(id) ?: return
        entryDao.update(current.copy(deletedAt = System.currentTimeMillis()))
    }

    /** 标签名去重：同名直接复用已有的，不重复建。 */
    suspend fun addTagToEntry(entryId: Long, name: String): Long? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val existing = tagDao.findByName(clean)
        val tagId = existing?.id ?: tagDao.insert(
            TagEntity(name = clean, createdAt = System.currentTimeMillis()),
        )
        if (tagId <= 0L) return null
        entryDao.attachTag(EntryTagEntity(entryId = entryId, tagId = tagId))
        return tagId
    }

    suspend fun removeTagFromEntry(entryId: Long, tagId: Long) {
        entryDao.detachTag(entryId, tagId)
    }

    suspend fun deleteTag(tagId: Long) {
        tagDao.delete(tagId)
    }
}
