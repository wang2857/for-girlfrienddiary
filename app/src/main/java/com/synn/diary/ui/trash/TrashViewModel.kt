package com.synn.diary.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synn.diary.data.EntryRepository
import com.synn.diary.data.TrashSettings
import com.synn.diary.data.db.EntryWithTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: EntryRepository,
    private val settings: TrashSettings,
) : ViewModel() {

    val items: StateFlow<List<EntryWithTags>> = repository.trash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _retentionDays = MutableStateFlow(settings.retentionDays)
    val retentionDays: StateFlow<Int> = _retentionDays.asStateFlow()

    fun setRetention(days: Int) {
        _retentionDays.value = days
        settings.retentionDays = days
        // 改成更短的保留期时立刻生效，别等下次启动
        viewModelScope.launch { repository.purgeExpired(days) }
    }

    fun restore(id: Long) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun purge(id: Long) {
        viewModelScope.launch { repository.purge(id) }
    }

    fun emptyTrash() {
        viewModelScope.launch { repository.emptyTrash() }
    }
}
