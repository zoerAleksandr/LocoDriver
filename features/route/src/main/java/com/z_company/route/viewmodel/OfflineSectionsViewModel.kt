package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.track.SectionSummary
import com.z_company.repository.remote_rest.tracks.OfflineDownloadResult
import com.z_company.repository.remote_rest.tracks.OfflineSectionInfo
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import com.z_company.route.util.MapTilePrefetch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OfflineStatus { NOT_DOWNLOADED, READY, OUTDATED, DOWNLOADING }

data class OfflineSectionItem(
    val id: String,
    val title: String,
    val lengthKm: Double,
    val status: OfflineStatus,
    val bytes: Long,
)

/** Диалог «скачать карту по мобильной сети»: данные уже сохранены, тайлы - по согласию. */
data class TilePrompt(val sectionId: String, val name: String, val mb: Double)

/** Прогресс скачивания карты (тайлов) с возможностью отмены. */
data class TileProgress(val sectionId: String, val name: String, val done: Long, val total: Long)

data class OfflineSectionsState(
    val loading: Boolean = true,
    val needLogin: Boolean = false,
    val items: List<OfflineSectionItem> = emptyList(),
    val query: String = "",
    val onlyDownloaded: Boolean = false,
    val downloadedCount: Int = 0,
    val outdatedCount: Int = 0,
    val totalBytes: Long = 0L,
    val busy: Boolean = false,
    val message: String? = null,
    val tilePrompt: TilePrompt? = null,
    val tileProgress: TileProgress? = null,
)

/**
 * «Скачанные участки»: пользователь сам выбирает участки и скачивает их на телефон - профиль пути, станции и ось.
 * Дальше экран «Профиль пути» и определение плеча работают по сохранённому без сети. Список участков и метки версий - из того же
 * репозитория, что и карта ([SectionProfileRepository]); скачанное, но устаревшее на сервере помечается и обновляется кнопкой.
 */
class OfflineSectionsViewModel(private val repo: SectionProfileRepository) : ViewModel() {
    private val summaries = MutableStateFlow<List<SectionSummary>>(emptyList())
    private val offline = MutableStateFlow<Map<String, OfflineSectionInfo>>(emptyMap())
    private val downloading = MutableStateFlow<Set<String>>(emptySet())
    private val _state = MutableStateFlow(OfflineSectionsState())
    val state: StateFlow<OfflineSectionsState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val needLogin = com.z_company.repository.remote_rest.RemoteRestClient.advisorBearerToken().isNullOrBlank()
            if (needLogin) {
                _state.update { it.copy(loading = false, needLogin = true) }
                return@launch
            }
            // Сначала то, что на диске (мгновенно, без сети), затем обновление списка.
            summaries.value = withContext(Dispatchers.Default) { repo.summaries(refresh = false) }
            offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
            publish(loading = summaries.value.isEmpty())
            val fresh = withContext(Dispatchers.Default) { repo.summaries(refresh = true) }
            if (fresh.isNotEmpty()) summaries.value = fresh
            publish(loading = false)
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
        publish()
    }

    fun setOnlyDownloaded(value: Boolean) {
        _state.update { it.copy(onlyDownloaded = value) }
        publish()
    }

    fun download(id: String, context: android.content.Context) {
        viewModelScope.launch {
            if (downloadOne(id, showSuccess = true) == OfflineDownloadResult.OK) tilesAfter(id, context)
        }
    }

    /** Данные сохранены - тайлы с ожиданием (последовательно, чтобы прогресс не путался). */
    private suspend fun tilesAfter(id: String, context: android.content.Context) {
        afterData(id, context)
    }

    /**
     * Данные сохранены - докачать тайлы подложки вдоль оси (п. 4 плана): по Wi-Fi молча,
     * по мобильной сети - вопрос с оценкой размера (размер считается по числу тайлов в полосе).
     */
    private fun afterData(id: String, context: android.content.Context) {
        viewModelScope.launch {
            val section = withContext(Dispatchers.Default) { repo.cached(id) } ?: return@launch
            val mb = MapTilePrefetch.estimateMb(MapTilePrefetch.estimateTileCount(section.axis.points))
            if (MapTilePrefetch.isUnmetered(context)) {
                startTiles(context, section)
            } else {
                _state.update { it.copy(tilePrompt = TilePrompt(id, section.name, mb)) }
            }
        }
    }

    /** Пользователь согласился качать карту по мобильной сети. */
    fun acceptTiles(context: android.content.Context) {
        val prompt = _state.value.tilePrompt ?: return
        _state.update { it.copy(tilePrompt = null) }
        viewModelScope.launch {
            val section = withContext(Dispatchers.Default) { repo.cached(prompt.sectionId) }
            if (section == null) {
                _state.update { it.copy(message = "Тайлы карты не скачались. Попробуйте позже.") }
            } else {
                startTiles(context, section)
            }
        }
    }

    /** Скачивание тайлов с диалогом прогресса и кнопкой «Отмена». Одновременно - одна загрузка. */
    private suspend fun startTiles(context: android.content.Context, section: com.z_company.domain.track.SectionProfile) {
        if (_state.value.tileProgress != null) {
            _state.update { it.copy(message = "Карта уже скачивается - дождитесь или отмените текущую.") }
            return
        }
        var finished = false
        _state.update { it.copy(tileProgress = TileProgress(section.id, section.name, 0L, 0L)) }
        val ok = MapTilePrefetch.download(context, section.id, section.axisHash, section.axis.points) { done, total ->
            // Поздние статусы после завершения не воскрешают закрытый диалог.
            if (!finished) _state.update { it.copy(tileProgress = TileProgress(section.id, section.name, done, total)) }
        }
        finished = true
        _state.update {
            it.copy(
                tileProgress = null,
                message = when {
                    ok -> "Участок и карта сохранены на телефоне"
                    else -> "Скачивание карты остановлено"
                },
            )
        }
    }

    /** Отмена скачивания карты (кнопка в диалоге прогресса); диалог закрывается всегда. */
    fun cancelTiles(context: android.content.Context) {
        val p = _state.value.tileProgress ?: return
        MapTilePrefetch.cancel(context, p.sectionId)
        _state.update { it.copy(tileProgress = null, message = "Скачивание карты отменено") }
    }

    fun declineTiles() {
        _state.update { it.copy(tilePrompt = null, message = "Участок сохранён без карты. Тайлы можно докачать позже.") }
    }

    /** Обновить все скачанные, что изменились на сервере. Останавливается на первом лимите/отказе. */
    fun updateOutdated(context: android.content.Context) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val ids = _state.value.items.filter { it.status == OfflineStatus.OUTDATED }.map { it.id }
            var done = 0
            for (id in ids) {
                val r = downloadOne(id, showSuccess = false)
                if (r == OfflineDownloadResult.OK) { done++; tilesAfter(id, context) }
                else if (r != OfflineDownloadResult.FAILED) break
            }
            _state.update { it.copy(busy = false, message = if (done > 0) "Обновлено участков: $done" else it.message) }
        }
    }

    fun remove(id: String, context: android.content.Context) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) { repo.removeOffline(id) }
            withContext(Dispatchers.Default) { runCatching { MapTilePrefetch.delete(context, id) } }
            offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
            publish()
        }
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private suspend fun downloadOne(id: String, showSuccess: Boolean): OfflineDownloadResult {
        downloading.update { it + id }
        publish()
        val result = withContext(Dispatchers.Default) { repo.downloadForOffline(id) }
        offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
        downloading.update { it - id }
        publish()
        val text = when (result) {
            OfflineDownloadResult.OK -> if (showSuccess) "Участок сохранён на телефоне" else null
            OfflineDownloadResult.RATE_LIMITED -> "Достигнут суточный предел загрузки участков. Продолжите позже."
            OfflineDownloadResult.DEVICE_REJECTED -> "Устройство не прошло проверку подлинности: загрузка участков недоступна."
            OfflineDownloadResult.NOT_AVAILABLE -> "У этого участка пока нет данных для загрузки."
            OfflineDownloadResult.FAILED -> "Не удалось скачать участок. Проверьте соединение с сервером."
        }
        if (text != null) _state.update { it.copy(message = text) }
        return result
    }

    private fun publish(loading: Boolean = _state.value.loading) {
        val have = offline.value
        val busyIds = downloading.value
        val all = summaries.value.filter { it.hasProfile || have.containsKey(it.id) }.map { s ->
            val info = have[s.id]
            val status = when {
                s.id in busyIds -> OfflineStatus.DOWNLOADING
                info == null -> OfflineStatus.NOT_DOWNLOADED
                s.updatedAt > info.updatedAt.toLong() -> OfflineStatus.OUTDATED
                else -> OfflineStatus.READY
            }
            OfflineSectionItem(s.id, s.name, s.lengthKm, status, info?.bytes ?: 0L)
        }
        val q = _state.value.query.trim().lowercase()
        val only = _state.value.onlyDownloaded
        // Порядок только по названию: скачанный участок не «убегает» из-под пальца в другую часть списка.
        val shown = all
            .filter { (q.isEmpty() || it.title.lowercase().contains(q)) && (!only || it.status != OfflineStatus.NOT_DOWNLOADED) }
            .sortedBy { it.title }
        _state.update {
            it.copy(
                loading = loading,
                needLogin = false,
                items = shown,
                downloadedCount = all.count { i -> have.containsKey(i.id) },
                outdatedCount = all.count { i -> i.status == OfflineStatus.OUTDATED },
                totalBytes = have.values.sumOf { v -> v.bytes },
            )
        }
    }
}
