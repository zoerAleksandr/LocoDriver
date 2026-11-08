package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.track.SectionSummary
import com.z_company.repository.remote_rest.tracks.OfflineDownloadResult
import com.z_company.repository.remote_rest.tracks.OfflineSectionInfo
import com.z_company.repository.remote_rest.tracks.SectionProfileRepository
import com.z_company.route.util.MapTilePrefetch
import com.z_company.route.util.OfflineTilePacks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

enum class OfflineStatus { NOT_DOWNLOADED, READY, OUTDATED, DOWNLOADING }

data class OfflineSectionItem(
    val id: String,
    val title: String,
    val lengthKm: Double,
    val status: OfflineStatus,
    val bytes: Long,
)

/** Диалог «скачать участок с картой» по мобильной сети: данные и карта скачиваются только вместе. */
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
 * «Скачанные участки»: пользователь сам выбирает участки и скачивает их на телефон - профиль пути, станции и ось
 * вместе с картой (тайлы подложки). Скачивание только целиком: данных без карты не бывает. Скачанным считается
 * участок, у которого есть пак тайлов: профиль без карты (авто-кэш или прерванная загрузка) в «Скачано» не
 * попадает. Дальше экран «Профиль пути» и определение плеча работают по сохранённому без сети. Список участков и
 * метки версий - из того же репозитория, что и карта ([SectionProfileRepository]); скачанное, но устаревшее на
 * сервере помечается и обновляется кнопкой.
 */
class OfflineSectionsViewModel(
    private val repo: SectionProfileRepository,
    private val app: android.content.Context,
) : ViewModel() {
    private val summaries = MutableStateFlow<List<SectionSummary>>(emptyList())
    private val offline = MutableStateFlow<Map<String, OfflineSectionInfo>>(emptyMap())
    /** Полностью скачанные паки: id участка -> хэш оси и размер. */
    private val packs = MutableStateFlow<Map<String, OfflineTilePacks>>(emptyMap())
    private val downloading = MutableStateFlow<Set<String>>(emptySet())
    /** Любое скачивание, обновление или удаление выполняется строго по одному. */
    private val operationMutex = Mutex()
    /** Решения по шторке мобильной сети: id участка -> «Скачать» (true) / «Отмена» (false). */
    private val tileDecisions = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    /** Отмена кнопкой в прогрессе - для сообщения «Скачивание отменено» вместо ошибки. */
    private val cancelRequested = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
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
            packs.value = withContext(Dispatchers.Default) { runCatching { MapTilePrefetch.downloadedPacks(app) }.getOrDefault(emptyMap()) }
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

    /** Скачивание целиком: данные участка и карта (тайлы) вместе. Одновременно - одна загрузка. */
    fun download(id: String) {
        viewModelScope.launch {
            if (!operationMutex.tryLock()) {
                _state.update { it.copy(message = "Дождитесь завершения текущего скачивания.") }
                return@launch
            }
            try {
                withBusy(id) { downloadAtomically(id) }
            } finally {
                operationMutex.unlock()
            }
        }
    }

    /** Профиль и карта заменяют прежнюю рабочую копию только вместе. */
    private suspend fun downloadAtomically(id: String): Boolean {
        repo.beginOfflineReplace(id)
        var committed = false
        try {
            if (downloadOne(id) != OfflineDownloadResult.OK) return false
            if (!afterData(id)) return false
            val axisHash = repo.cached(id)?.axisHash ?: return false
            repo.commitOfflineReplace(id)
            withContext(Dispatchers.Default) { runCatching { MapTilePrefetch.commit(app, id, axisHash) } }
            committed = true
            offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
            packs.value = withContext(Dispatchers.Default) {
                runCatching { MapTilePrefetch.downloadedPacks(app) }.getOrDefault(packs.value)
            }
            return true
        } finally {
            if (!committed) {
                repo.rollbackOfflineReplace(id)
                offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
            }
            publish()
        }
    }

    /** Пометить участок загружаемым на всё время данных и карты. */
    private suspend fun withBusy(id: String, block: suspend () -> Unit) {
        downloading.update { it + id }
        publish()
        try {
            block()
        } finally {
            downloading.update { it - id }
            publish()
        }
    }

    /**
     * Данные сохранены - докачать тайлы подложки вдоль оси: по Wi-Fi молча, по мобильной сети - вопрос с оценкой
     * размера (размер считается по числу тайлов в полосе). Данные и карта только вместе: отказ, отмена или сбой
     * тайлов откатывает новую версию профиля, сохраняя прежнюю рабочую копию. true - участок целиком на телефоне.
     */
    private suspend fun afterData(id: String): Boolean {
        val section = withContext(Dispatchers.Default) { repo.cached(id) }
        if (section == null) {
            _state.update { it.copy(message = "Не удалось сохранить участок. Попробуйте позже.") }
            return false
        }
        val accepted = if (MapTilePrefetch.isUnmetered(app)) {
            true
        } else {
            val decision = CompletableDeferred<Boolean>()
            tileDecisions[id] = decision
            val mb = MapTilePrefetch.estimateMb(MapTilePrefetch.estimateTileCount(section.axis.points))
            _state.update { it.copy(tilePrompt = TilePrompt(id, section.name, mb)) }
            try {
                decision.await()
            } finally {
                tileDecisions.remove(id)
                _state.update { state ->
                    if (state.tilePrompt?.sectionId == id) state.copy(tilePrompt = null) else state
                }
            }
        }
        if (!accepted) {
            _state.update { it.copy(message = "Скачивание отменено") }
            return false
        }
        return startTiles(section)
    }

    /** Пользователь согласился качать карту по мобильной сети (данные уже сохранены - вместе с картой). */
    fun acceptTiles() {
        val prompt = _state.value.tilePrompt ?: return
        _state.update { it.copy(tilePrompt = null) }
        tileDecisions[prompt.sectionId]?.complete(true)
    }

    /** «Отмена» в шторке мобильной сети: данные и карта скачиваются только вместе, участок не сохранён. */
    fun cancelDownload() {
        val prompt = _state.value.tilePrompt ?: return
        _state.update { it.copy(tilePrompt = null) }
        tileDecisions[prompt.sectionId]?.complete(false)
    }

    /** Скачивание тайлов с диалогом прогресса и кнопкой «Отмена». true - пак на телефоне. */
    private suspend fun startTiles(section: com.z_company.domain.track.SectionProfile): Boolean {
        if (_state.value.tileProgress != null) {
            _state.update { it.copy(message = "Карта уже скачивается - дождитесь или отмените текущую.") }
            return false
        }
        var finished = false
        _state.update { it.copy(tileProgress = TileProgress(section.id, section.name, 0L, 0L)) }
        val ok = MapTilePrefetch.download(app, section.id, section.axisHash, section.axis.points) { done, total ->
            // Поздние статусы после завершения не воскрешают закрытый диалог.
            if (!finished) _state.update { it.copy(tileProgress = TileProgress(section.id, section.name, done, total)) }
        }
        finished = true
        if (ok) {
            packs.value = withContext(Dispatchers.Default) { runCatching { MapTilePrefetch.downloadedPacks(app) }.getOrDefault(packs.value) }
            _state.update { it.copy(tileProgress = null, message = "Участок и карта сохранены на телефоне") }
        } else {
            val byUser = cancelRequested.remove(section.id)
            _state.update {
                it.copy(
                    tileProgress = null,
                    message = if (byUser) "Скачивание отменено"
                    else "Не удалось скачать карту. Прежняя версия участка сохранена; нажмите «Скачать», чтобы попробовать снова",
                )
            }
        }
        publish()
        return ok
    }

    /** Отмена новой карты: staging удаляется, прежняя рабочая версия остаётся. */
    fun cancelTiles() {
        val p = _state.value.tileProgress ?: return
        cancelRequested.add(p.sectionId)
        MapTilePrefetch.cancel(app, p.sectionId)
        _state.update { it.copy(tileProgress = null) }
    }

    /** Обновить все скачанные, что изменились на сервере. Останавливается на первом лимите/отказе. */
    fun updateOutdated() {
        viewModelScope.launch {
            if (!operationMutex.tryLock()) {
                _state.update { it.copy(message = "Дождитесь завершения текущего скачивания.") }
                return@launch
            }
            _state.update { it.copy(busy = true) }
            try {
                val ids = _state.value.items.filter { it.status == OfflineStatus.OUTDATED }.map { it.id }
                var done = 0
                for (id in ids) {
                    var stop = false
                    withBusy(id) {
                        if (downloadAtomically(id)) done++ else stop = true
                    }
                    if (stop) break
                }
                _state.update { it.copy(message = if (done > 0) "Обновлено участков: $done" else it.message) }
            } finally {
                _state.update { it.copy(busy = false) }
                operationMutex.unlock()
            }
        }
    }

    fun remove(id: String) {
        viewModelScope.launch {
            if (!operationMutex.tryLock()) {
                _state.update { it.copy(message = "Дождитесь завершения текущей операции.") }
                return@launch
            }
            try {
                withContext(Dispatchers.Default) { repo.removeOffline(id) }
                withContext(Dispatchers.Default) { runCatching { MapTilePrefetch.delete(app, id) } }
                offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
                packs.value = packs.value - id
                publish()
            } finally {
                operationMutex.unlock()
            }
        }
    }

    override fun onCleared() {
        tileDecisions.values.forEach { it.complete(false) }
        _state.value.tileProgress?.let { MapTilePrefetch.cancel(app, it.sectionId) }
        super.onCleared()
    }

    fun messageShown() = _state.update { it.copy(message = null) }

    private suspend fun downloadOne(id: String): OfflineDownloadResult {
        val result = withContext(Dispatchers.Default) { repo.downloadForOffline(id) }
        offline.value = withContext(Dispatchers.Default) { repo.offlineSections() }
        val text = when (result) {
            OfflineDownloadResult.OK -> null // успех объявляется после карты: данные и карта скачиваются вместе
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
        val tiles = packs.value
        val busyIds = downloading.value
        val all = summaries.value.filter { it.hasProfile || have.containsKey(it.id) }.map { s ->
            val info = have[s.id]
            val pack = tiles[s.id]
            val matchingPackBytes = info?.let { pack?.bytesFor(it.axisHash) }
            val status = when {
                s.id in busyIds -> OfflineStatus.DOWNLOADING
                info == null -> OfflineStatus.NOT_DOWNLOADED
                pack == null -> OfflineStatus.NOT_DOWNLOADED
                matchingPackBytes == null -> OfflineStatus.OUTDATED
                s.updatedAt > info.updatedAt.toLong() -> OfflineStatus.OUTDATED
                else -> OfflineStatus.READY
            }
            OfflineSectionItem(s.id, s.name, s.lengthKm, status, (info?.bytes ?: 0L) + (pack?.totalBytes ?: 0L))
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
                downloadedCount = all.count { i -> i.status == OfflineStatus.READY || i.status == OfflineStatus.OUTDATED },
                outdatedCount = all.count { i -> i.status == OfflineStatus.OUTDATED },
                totalBytes = all.sumOf { i -> if (i.status == OfflineStatus.NOT_DOWNLOADED) 0L else i.bytes },
            )
        }
    }
}
