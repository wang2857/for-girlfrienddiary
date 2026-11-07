package com.synn.diary.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synn.diary.data.EntryRepository
import com.synn.diary.data.db.EntryWithTags
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SearchViewModel(private val repository: EntryRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<EntryWithTags>>(emptyList())
    val results: StateFlow<List<EntryWithTags>> = _results.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(value: String) {
        _query.value = value
        searchJob?.cancel()

        val keyword = value.trim()
        if (keyword.isEmpty()) {
            _results.value = emptyList()
            return
        }

        // 250ms 防抖：中文输入法会连续上屏，逐字查库没必要
        searchJob = viewModelScope.launch {
            delay(250)
            _results.value = repository.search(keyword).first()
        }
    }
}
