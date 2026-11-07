package com.synn.diary.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/** 时间线 / 搜索 / 日历共用的读取模型：一条日记带上它的标签。 */
data class EntryWithTags(
    @Embedded val entry: EntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = EntryTagEntity::class,
            parentColumn = "entryId",
            entityColumn = "tagId",
        ),
    )
    val tags: List<TagEntity> = emptyList(),
)
