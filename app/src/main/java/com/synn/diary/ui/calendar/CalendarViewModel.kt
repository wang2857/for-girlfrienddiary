package com.synn.diary.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synn.diary.data.EntryRepository
import com.synn.diary.data.db.EntryWithTags
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(repository: EntryRepository) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _selectedDay = MutableStateFlow<Long?>(null)
    val selectedDay: StateFlow<Long?> = _selectedDay.asStateFlow()

    /**
     * 每个"有记录的日子"对应的心情。
     *
     * key 存在 = 这天有记录；value 为 null = 有记录但没设心情。
     * 一天有多条时取 updatedAt 最大的那条的心情——也就是"当天最后修改的日记"。
     */
    val dayInfo: StateFlow<Map<Long, String?>> = _month
        .flatMapLatest { ym ->
            repository.dayMoods(
                fromDay = ym.atDay(1).toEpochDay(),
                toDay = ym.atEndOfMonth().toEpochDay(),
            )
        }
        .map { rows ->
            rows.groupBy { it.entryDate }
                .mapValues { (_, sameDay) ->
                    sameDay.filter { it.mood != null }
                        .maxByOrNull { it.updatedAt }
                        ?.mood
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val dayEntries: StateFlow<List<EntryWithTags>> = _selectedDay
        .flatMapLatest { day ->
            if (day == null) flowOf(emptyList()) else repository.entriesOfDay(day)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousMonth() {
        _month.update { it.minusMonths(1) }
        _selectedDay.value = null
    }

    fun nextMonth() {
        _month.update { it.plusMonths(1) }
        _selectedDay.value = null
    }

    /** 从年月选择器直接跳到某个月。 */
    fun goToMonth(target: YearMonth) {
        _month.value = target
        _selectedDay.value = null
    }

    fun selectDay(epochDay: Long) {
        _selectedDay.value = if (_selectedDay.value == epochDay) null else epochDay
    }
}
