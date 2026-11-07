package com.synn.diary.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synn.diary.data.EntryRepository
import com.synn.diary.data.db.EntryWithTags
import com.synn.diary.data.db.TagEntity
import com.synn.diary.util.Dates
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModel(private val repository: EntryRepository) : ViewModel() {

    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val selectedMood = MutableStateFlow<String?>(null)
    private val onlyFavorites = MutableStateFlow(false)

    val selectedTag: StateFlow<Long?> = selectedTagId.asStateFlow()
    val moodFilter: StateFlow<String?> = selectedMood.asStateFlow()
    val favoritesOnly: StateFlow<Boolean> = onlyFavorites.asStateFlow()

    val tags: StateFlow<List<TagEntity>> = repository.tags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 只记录"被折叠的组"，默认全部展开。 */
    private val collapsed = MutableStateFlow<Set<String>>(emptySet())

    /** 标签走 SQL 过滤，心情在这层再筛一遍：两者是 AND 关系。 */
    private val entries = combine(selectedTagId, selectedMood, onlyFavorites) { tagId, mood, favorites ->
        Triple(tagId, mood, favorites)
    }.flatMapLatest { (tagId, mood, favorites) ->
        repository.timeline(tagId, favorites).map { list ->
            if (mood == null) list else list.filter { it.entry.mood == mood }
        }
    }

    val rows: StateFlow<List<TimelineRow>> = combine(entries, collapsed) { list, folded ->
        buildRows(list, folded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTag(tagId: Long?) {
        selectedTagId.value = tagId
    }

    fun toggleMood(emoji: String) {
        selectedMood.value = if (selectedMood.value == emoji) null else emoji
    }

    fun toggleFavoritesOnly() {
        onlyFavorites.value = !onlyFavorites.value
    }

    fun toggleFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setFavorite(id, favorite) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }

    fun setLocked(id: Long, locked: Boolean) {
        viewModelScope.launch { repository.setLocked(id, locked) }
    }

    fun clearFilters() {
        selectedTagId.value = null
        selectedMood.value = null
        onlyFavorites.value = false
    }

    fun toggleGroup(key: String) {
        collapsed.update { if (key in it) it - key else it + key }
    }

    /**
     * 年 → 月 → 周 摊平成一行列表。
     *
     * 排序不用再排：SQL 已经按 entryDate 倒序给出，groupBy 会保持首次出现顺序，
     * 所以年、月、周自然就是倒序的。
     */
    private fun buildRows(
        entries: List<EntryWithTags>,
        folded: Set<String>,
    ): List<TimelineRow> {
        if (entries.isEmpty()) return emptyList()

        val byYear = entries.groupBy { epochDay -> LocalDate.ofEpochDay(epochDay.entry.entryDate).year }
        // 只有一年的数据时不显示"2026年"这一层，省掉一个永远展开的标题
        val showYears = byYear.size > 1

        val rows = mutableListOf<TimelineRow>()

        for ((year, yearEntries) in byYear) {
            val yearKey = "y:$year"
            if (showYears) {
                rows += TimelineRow.Year(year, yearEntries.size, yearKey !in folded)
                if (yearKey in folded) continue
            }

            val byMonth = yearEntries.groupBy { LocalDate.ofEpochDay(it.entry.entryDate).monthValue }
            for ((month, monthEntries) in byMonth) {
                val monthKey = "m:$year-$month"
                rows += TimelineRow.Month(year, month, monthEntries.size, monthKey !in folded)
                if (monthKey in folded) continue

                val byWeek = monthEntries.groupBy { Dates.weekStart(it.entry.entryDate) }
                for ((weekStart, weekEntries) in byWeek) {
                    val weekKey = "w:$weekStart"
                    rows += TimelineRow.Week(
                        weekStart = weekStart,
                        label = Dates.weekRangeLabel(weekStart),
                        count = weekEntries.size,
                        expanded = weekKey !in folded,
                        isCurrent = Dates.isCurrentWeek(weekStart),
                    )
                    if (weekKey in folded) continue
                    weekEntries.forEach { rows += TimelineRow.Entry(it) }
                }
            }
        }
        return rows
    }
}
