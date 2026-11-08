package com.synn.diary.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    /**
     * 时间线：过滤已删除，以及"打开过但一个字没写"的空壳条目。
     * 空壳过滤放在这里有讲究——新建即入库能让每次按键都自动保存，代价是会留下一堆空行，
     * 用 SQL 过滤掉比在退出时判断"要不要删"更不容易出竞态。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM entry
        WHERE deletedAt IS NULL
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
          AND (:onlyFavorites = 0 OR favorite = 1)
        ORDER BY entryDate DESC, createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeTimeline(onlyFavorites: Int, limit: Int): Flow<List<EntryWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM entry
        WHERE deletedAt IS NULL
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
          AND id IN (SELECT entryId FROM entry_tag WHERE tagId = :tagId)
          AND (:onlyFavorites = 0 OR favorite = 1)
        ORDER BY entryDate DESC, createdAt DESC
        LIMIT :limit
        """,
    )
    fun observeByTag(tagId: Long, onlyFavorites: Int, limit: Int): Flow<List<EntryWithTags>>

    /** 中文场景先用 LIKE：几万条内够用，等数据量上来再换 FTS5 trigram。 */
    /** LIKE 兜底：短查询、或设备上 FTS5 装不上时用。 */
    @Transaction
    @Query(
        """
        SELECT * FROM entry
        WHERE deletedAt IS NULL
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
          AND locked = 0
          AND (title LIKE '%' || :keyword || '%' OR body LIKE '%' || :keyword || '%')
        ORDER BY entryDate DESC, createdAt DESC
        """,
    )
    fun search(keyword: String): Flow<List<EntryWithTags>>

    /** 走 FTS5 索引；SQL 里引用的是虚拟表，Room 编译期解析不了，所以用 RawQuery。 */
    @Transaction
    @RawQuery(observedEntities = [EntryEntity::class, EntryTagEntity::class, TagEntity::class])
    fun searchFts(query: SupportSQLiteQuery): Flow<List<EntryWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM entry
        WHERE deletedAt IS NULL
          AND entryDate = :epochDay
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
        ORDER BY createdAt ASC
        """,
    )
    fun observeDay(epochDay: Long): Flow<List<EntryWithTags>>

    /** 日历要的两样东西一次拿全：哪天有记录、以及那天的"最后一个心情"。 */
    @Query(
        """
        SELECT entryDate, mood, updatedAt FROM entry
        WHERE deletedAt IS NULL
          AND entryDate BETWEEN :fromDay AND :toDay
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
        """,
    )
    fun observeDayMoods(fromDay: Long, toDay: Long): Flow<List<DayMoodRow>>

    @Query("SELECT * FROM entry WHERE id = :id")
    suspend fun get(id: Long): EntryEntity?

    /** 合并导入用：一次拿全所有 uuid，判断"这篇是不是已经有了"。 */
    @Query("SELECT uuid FROM entry")
    suspend fun allUuids(): List<String>

    /** 只写环境字段，故意不动 updatedAt —— 否则"抓天气"会篡改用户对这条日记的最后修改时间。 */
    @Query(
        """
        UPDATE entry
        SET weatherText = :weather, locationText = :location, latitude = :lat, longitude = :lon
        WHERE id = :id
        """,
    )
    suspend fun updateWeather(id: Long, weather: String?, location: String?, lat: Double?, lon: Double?)

    @Insert
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("UPDATE entry SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE entry SET locked = :locked WHERE id = :id")
    suspend fun setLocked(id: Long, locked: Boolean)

    @Query("UPDATE entry SET locked = 0 WHERE locked = 1")
    suspend fun unlockAll()

    @Transaction
    @Query("SELECT * FROM entry WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<EntryWithTags>>

    @Query("UPDATE entry SET deletedAt = NULL WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("SELECT * FROM entry WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun expiredTrash(cutoff: Long): List<EntryEntity>

    @Query("DELETE FROM entry WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeExpired(cutoff: Long): Int

    @Query("SELECT * FROM entry WHERE deletedAt IS NOT NULL")
    suspend fun allTrashed(): List<EntryEntity>

    /** 回收站里的也要算：它们还等着被还原，不能把图删了。 */
    @Query("SELECT body FROM entry")
    suspend fun allBodies(): List<String>

    /** 纸张字段：自定义纸张是 `img:<文件名>`，清理孤儿背景图要用。 */
    @Query("SELECT background FROM entry")
    suspend fun allBackgrounds(): List<String?>

    @Query(
        """
        SELECT count(*) FROM entry
        WHERE deletedAt IS NULL AND entryDate = :epochDay
          AND (length(trim(title)) > 0 OR length(trim(body)) > 0)
        """,
    )
    suspend fun countOnDay(epochDay: Long): Int

    @Query("DELETE FROM entry WHERE deletedAt IS NOT NULL")
    suspend fun purgeAllTrash()

    // ---- 标签关联 ----

    @Insert
    suspend fun attachTag(ref: EntryTagEntity)

    @Query("DELETE FROM entry_tag WHERE entryId = :entryId AND tagId = :tagId")
    suspend fun detachTag(entryId: Long, tagId: Long)

    @Query("SELECT * FROM tag WHERE id IN (SELECT tagId FROM entry_tag WHERE entryId = :entryId) ORDER BY name")
    fun observeTagsOfEntry(entryId: Long): Flow<List<TagEntity>>
}
