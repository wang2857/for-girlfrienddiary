package com.synn.diary.ui.timeline

import com.synn.diary.data.db.EntryWithTags

/** 时间线展开后的扁平行：三级标题 + 日记本身。 */
sealed interface TimelineRow {

    val key: String

    data class Year(
        val year: Int,
        val count: Int,
        val expanded: Boolean,
    ) : TimelineRow {
        override val key: String = "y:$year"
    }

    data class Month(
        val year: Int,
        val month: Int,
        val count: Int,
        val expanded: Boolean,
    ) : TimelineRow {
        override val key: String = "m:$year-$month"
    }

    data class Week(
        val weekStart: Long,
        val label: String,
        val count: Int,
        val expanded: Boolean,
        val isCurrent: Boolean,
    ) : TimelineRow {
        override val key: String = "w:$weekStart"
    }

    data class Entry(
        val item: EntryWithTags,
    ) : TimelineRow {
        override val key: String = "e:${item.entry.id}"
    }
}
