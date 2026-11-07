package com.synn.diary.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一张插入到日记里的图片。
 *
 * 正文里用 `[[img:<relPath>]]` 标记位置，所以渲染时不必查库；
 * 这张表是为了导出、清理孤儿文件这类需要"某条日记有哪些图"的场景。
 */
@Entity(
    tableName = "attachment",
    foreignKeys = [
        ForeignKey(
            entity = EntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("entryId")],
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val entryId: Long,
    val relPath: String,
    val createdAt: Long,
)
