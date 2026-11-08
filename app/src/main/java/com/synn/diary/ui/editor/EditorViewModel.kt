package com.synn.diary.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.synn.diary.data.EntryRepository
import com.synn.diary.data.media.FontStore
import com.synn.diary.data.media.ImageStore
import com.synn.diary.data.db.EntryEntity
import com.synn.diary.data.db.TagEntity
import com.synn.diary.data.weather.WeatherService
import com.synn.diary.util.Dates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditorViewModel(
    private val repository: EntryRepository,
    private val weatherService: WeatherService,
    private val imageStore: ImageStore,
    private val fontStore: FontStore,
    private val appScope: CoroutineScope,
    private val entryId: Long,
) : ViewModel() {

    data class UiState(
        val entryDate: Long = 0L,
        val createdAt: Long = 0L,
        val title: String = "",
        val body: String = "",
        val mood: String? = null,
        val loaded: Boolean = false,
        val savedAt: Long = 0L,
        val deleted: Boolean = false,
        val background: EntryBackground = EntryBackground.default,
        val fontId: String = "system",
        val weatherText: String? = null,
        val locationText: String? = null,
        val capturing: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 当前这条日记的标签，随数据库变化自动更新。 */
    val entryTags: StateFlow<List<TagEntity>> = repository.tagsOfEntry(entryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 所有标签，给选择弹窗用。 */
    val allTags: StateFlow<List<TagEntity>> = repository.tags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            val entry = repository.get(entryId)
            _state.value = if (entry == null) {
                UiState(entryDate = Dates.todayEpochDay(), loaded = true)
            } else {
                UiState(
                    entryDate = entry.entryDate,
                    createdAt = entry.createdAt,
                    title = entry.title,
                    body = entry.body,
                    mood = entry.mood,
                    background = EntryBackground.from(entry.background),
                    fontId = entry.font ?: "system",
                    loaded = true,
                    savedAt = entry.updatedAt,
                    weatherText = entry.weatherText,
                    locationText = entry.locationText,
                )
            }
            entry?.let { captureEnvironmentIfNew(it) }
        }
    }

    /**
     * 只对"刚新建的条目"抓环境：
     * 老记录（天气功能上线前写的、或两天前写了一半的）不去补历史天气——那会是编造。
     */
    private fun captureEnvironmentIfNew(entry: EntryEntity) {
        // 用区间而不是"小于"：补写/导入的条目 createdAt 可能在未来，
        // 那种情况不该触发抓取，否则会给历史记录安上今天的环境。
        val age = System.currentTimeMillis() - entry.createdAt
        if (age !in 0..CAPTURE_WINDOW_MS) return
        if (entry.weatherText != null || entry.locationText != null) return

        _state.update { it.copy(capturing = true) }
        android.util.Log.d("EditorVM", "capture start entry=$entryId")

        // 跑在 appScope 上而不是 viewModelScope：写日记常常几秒钟就退出，
        // 用 viewModelScope 的话这次抓取会被连带取消，天气就永远补不上。
        appScope.launch {
            val snapshot = runCatching { weatherService.capture() }.getOrNull()
            if (snapshot != null) {
                repository.updateWeather(
                    id = entryId,
                    weatherText = snapshot.weatherText,
                    locationText = snapshot.locationText,
                    latitude = snapshot.latitude,
                    longitude = snapshot.longitude,
                )
            }
            android.util.Log.d("EditorVM", "capture done entry=$entryId snapshot=$snapshot")
            _state.update {
                it.copy(
                    weatherText = snapshot?.weatherText ?: it.weatherText,
                    locationText = snapshot?.locationText ?: it.locationText,
                    capturing = false,
                )
            }
        }
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value) }
        scheduleSave()
    }

    fun onBodyChange(value: String) {
        _state.update { it.copy(body = value) }
        scheduleSave()
    }

    /** 「保存」按钮：跳过防抖立刻落盘，保存完再回调（用来提示用户）。 */
    fun saveNow(onSaved: () -> Unit) {
        saveJob?.cancel()
        saveJob = null
        viewModelScope.launch {
            persist()
            onSaved()
        }
    }

    /** 从相册选一张当纸张：先落盘，再切过去。 */
    fun importBackground(uri: android.net.Uri) {
        viewModelScope.launch {
            val name = imageStore.importBackground(uri) ?: return@launch
            setBackground(EntryBackground.Custom(name))
        }
    }

    /** 导入用户自己的字体文件；成功后直接切过去。 */
    fun importFont(uri: Uri, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val fileName = fontStore.import(uri)
            if (fileName != null) setFont("file:$fileName")
            onDone(fileName)
        }
    }

    /** 换正文字体：只影响这条日记，存字符串 id。 */
    fun setFont(fontId: String) {
        _state.update { it.copy(fontId = fontId) }
        scheduleSave()
    }

    fun setBackground(background: EntryBackground) {
        _state.update { it.copy(background = background) }
        scheduleSave()
    }

    fun toggleMood(mood: String) {
        _state.update { it.copy(mood = if (it.mood == mood) null else mood) }
        scheduleSave()
    }

    /** 选中的图先落盘再入库，最后把文件名交回去——正文里要写的就是这个名字。 */
    fun importImage(uri: Uri, onImported: (String?) -> Unit) {
        viewModelScope.launch {
            val fileName = imageStore.import(uri)
            android.util.Log.d("EditorVM", "importImage uri=$uri -> $fileName")
            onImported(fileName)
        }
    }

    fun removeImage(fileName: String) {
        viewModelScope.launch { imageStore.delete(fileName) }
    }

    fun addTag(name: String) {
        viewModelScope.launch { repository.addTagToEntry(entryId, name) }
    }

    fun removeTag(tagId: Long) {
        viewModelScope.launch { repository.removeTagFromEntry(entryId, tagId) }
    }

    /**
     * 退出编辑器时调用：不等 debounce，且跑在 Application 作用域上——
     * 用户点返回的瞬间这个 ViewModel 就被清了，viewModelScope 里的写盘会被取消。
     */
    fun flush() {
        saveJob?.cancel()
        saveJob = null
        appScope.launch { persist() }
    }

    fun delete(onDeleted: () -> Unit) {
        saveJob?.cancel()
        _state.update { it.copy(deleted = true) }
        viewModelScope.launch {
            repository.softDelete(entryId)
            onDeleted()
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private suspend fun persist() {
        val current = _state.value
        // 空条目不入库：新建后一个字没写就退出，时间线里不该多一条空白。
        if (!current.loaded || current.deleted) return
        if (current.title.isBlank() && current.body.isBlank()) return
        repository.save(
            id = entryId,
            title = current.title,
            body = current.body,
            mood = current.mood,
            background = current.background.id,
            font = current.fontId.takeIf { it != "system" },
        )
        _state.update { it.copy(savedAt = System.currentTimeMillis()) }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 700L
        const val CAPTURE_WINDOW_MS = 2 * 60 * 1000L
    }
}
