package com.synn.diary.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AttachmentDao {

    @Insert
    suspend fun insert(attachment: AttachmentEntity): Long

    @Query("SELECT * FROM attachment WHERE entryId = :entryId ORDER BY id ASC")
    suspend fun forEntry(entryId: Long): List<AttachmentEntity>

    @Query("DELETE FROM attachment WHERE relPath = :relPath")
    suspend fun deleteByPath(relPath: String)
}
