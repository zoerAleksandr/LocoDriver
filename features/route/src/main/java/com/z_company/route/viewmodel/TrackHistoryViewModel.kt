package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackInfo
import com.z_company.domain.track.TrackPacking
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackTrainResolution
import com.z_company.domain.track.TrackUploadState
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.displayTimeZone
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.tracks.TrackHistoryLoader
import com.z_company.repository.remote_rest.tracks.TracksApiException
import com.z_company.route.autorecord.TrackConsentStore
import com.z_company.route.autorecord.TrackRouteResolver
import com.z_company.route.autorecord.TrackUploadTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Откуда карточка и на каком этапе отправки (SCREEN_SPECS §17.8). */
enum class TrackHistoryStatus {
    /** Согласия на сохранение на сервере нет или вход не выполнен — трек только на устройстве. */
    DEVICE_ONLY,
    PENDING,
    SENT,

    /** Сервер отказал окончательно (лимит хранилища и т.п.). */
    REJECTED,

    /** Есть на сервере, на этом устройстве нет (другое устройство / очищено). */
    SERVER_ONLY,
}

data class TrackHistoryItem(
    val id: String,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val distanceKm: Double?,
    val avgSpeedKmh: Double?,
    val maxSpeedKmh: Double?,
    /** Ломаная мини-карты (широта, долгота); пусто — значок. */
    val preview: List<Pair<Double, Double>>,
    val isFavorite: Boolean,
    val status: TrackHistoryStatus,
) {
    val isLocal: Boolean get() = status != TrackHistoryStatus.SERVER_ONLY
    val durationMs: Long get() = (endedAt - startedAt).coerceAtLeast(0L)
}

/** Открытый для просмотра трек: точки и пояс времени. */
class OpenedTrack(val title: String, val points: List<TrackPoint>, val zone: String)

data class TrackHistoryUiState(
    val loading: Boolean = true,
    val items: List<TrackHistoryItem> = emptyList(),
    val consent: Boolean = false,
    /** Идёт запрос серверной части списка. */
    val remoteLoading: Boolean = false,
    /** Серверная часть не загрузилась (нет сети и т.п.) — показываем только локальные. */
    val remoteError: Boolean = false,
    val canLoadMore: Boolean = false,
    /** Идёт скачивание/чтение трека для просмотра. */
    val opening: Boolean = false,
    val opened: OpenedTrack? = null,
    /** Одноразовое сообщение пользователю (снекбар); сбрасывается [messageShown]. */
    val message: String? = null,
    /** Пояс отображения времени (МСК или местное для KZ). */
    val zone: String = "GMT+3",
)

/**
 * «Мои треки» (Настройки, SCREEN_SPECS §17.8): карточки локальных треков (Track.db) плюс треки, которые
 * есть только на сервере. Координаты не логируются, в сообщениях пользователю их нет.
 */
class TrackHistoryViewModel : ViewModel(), KoinComponent {
    private val repository: TrackRepository by inject()
    private val resolver: TrackRouteResolver by inject()
    private val loader: TrackHistoryLoader by inject()
    private val consent: TrackConsentStore by inject()
    private val tokenStorage: SecureTokenStorage by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val trigger: TrackUploadTrigger by inject()

    private val _state = MutableStateFlow(TrackHistoryUiState(consent = consent.saveTracks.value))
    val state: StateFlow<TrackHistoryUiState> = _state.asStateFlow()

    private var remoteCursor: String? = null
    private val statsCache = HashMap<String, Pair<Long, TrackHistoryItem>>()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val zone = runCatching { settingsUseCase.getUserSettingFlow().first().displayTimeZone() }.getOrDefault("GMT+3")
            _state.update { it.copy(consent = consent.saveTracks.value, zone = zone) }
            // Треки удалённых рейсов/поездов не показываем: сначала убираем осиротевшие.
            runCatching { resolver.cleanOrphans() }
            loadLocal()
            loadRemote(reset = true)
        }
        // Заодно пробуем отправить накопленное (без сети/входа воркер просто выйдет).
        trigger.trigger()
    }

    private suspend fun loadLocal() {
        val infos = withContext(Dispatchers.Default) { repository.tracks() }
        val canSend = consent.saveTracks.value
        // Сначала быстрый список без сводок, затем карточки достраиваются по одной.
        val remoteOnly = _state.value.items.filter { it.status == TrackHistoryStatus.SERVER_ONLY }
        val first = infos.map { skeleton(it, canSend, statsCache[it.trackId]) }
        _state.update { it.copy(loading = false, items = merge(first, remoteOnly)) }
        for (info in infos) {
            val cached = statsCache[info.trackId]
            val pointCount = withContext(Dispatchers.Default) { repository.pointCount(info.trackId) }
            if (cached != null && cached.first == pointCount) continue
            val full = withContext(Dispatchers.Default) { build(info, canSend) }
            statsCache[info.trackId] = pointCount to full
            _state.update { st -> st.copy(items = st.items.map { if (it.id == info.trackId) full else it }) }
        }
    }

    private fun merge(local: List<TrackHistoryItem>, remote: List<TrackHistoryItem>): List<TrackHistoryItem> {
        val localIds = local.map { it.id }.toSet()
        return (local + remote.filter { it.id !in localIds }).sortedByDescending { it.startedAt }
    }

    private fun skeleton(info: TrackInfo, canSend: Boolean, cached: Pair<Long, TrackHistoryItem>?): TrackHistoryItem =
        (cached?.second ?: TrackHistoryItem(
            id = info.trackId, title = "Трек", startedAt = info.startedAt, endedAt = info.endedAt,
            distanceKm = null, avgSpeedKmh = null, maxSpeedKmh = null, preview = emptyList(),
            isFavorite = info.isFavorite, status = TrackHistoryStatus.DEVICE_ONLY,
        )).copy(isFavorite = info.isFavorite, status = statusOf(info, canSend), endedAt = info.endedAt)

    private suspend fun build(info: TrackInfo, canSend: Boolean): TrackHistoryItem {
        val points = repository.points(info.trackId)
        val stats = TrackPacking.stats(points)
        val title = (resolver.resolve(info.trainId) as? TrackTrainResolution.Found)?.title ?: "Трек"
        return TrackHistoryItem(
            id = info.trackId,
            title = title,
            startedAt = points.firstOrNull()?.timeMs ?: info.startedAt,
            endedAt = points.lastOrNull()?.timeMs ?: info.endedAt,
            distanceKm = stats.distanceKm,
            avgSpeedKmh = stats.avgSpeedKmh,
            maxSpeedKmh = stats.maxSpeedKmh,
            preview = TrackPacking.preview(points, 60).map { it.latitude to it.longitude },
            isFavorite = info.isFavorite,
            status = statusOf(info, canSend),
        )
    }

    private fun statusOf(info: TrackInfo, canSend: Boolean) = when {
        info.uploadState == TrackUploadState.UPLOADED -> TrackHistoryStatus.SENT
        info.uploadState == TrackUploadState.REJECTED -> TrackHistoryStatus.REJECTED
        !canSend -> TrackHistoryStatus.DEVICE_ONLY
        else -> TrackHistoryStatus.PENDING
    }

    private suspend fun bearer(): String? =
        tokenStorage.getAuthBearerTokenFlow().first()?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    /** Серверные карточки, которых нет на устройстве. Нужны вход и согласие; ошибка не мешает локальному списку. */
    private suspend fun loadRemote(reset: Boolean) {
        if (!consent.saveTracks.value) return
        val token = bearer() ?: return
        if (reset) remoteCursor = null
        _state.update { it.copy(remoteLoading = true, remoteError = false) }
        try {
            val page = loader.page(token, remoteCursor)
            remoteCursor = page.nextCursor
            val localIds = repository.tracks().map { it.trackId }.toSet()
            val add = page.tracks.filter { it.id !in localIds }.map { c ->
                TrackHistoryItem(
                    id = c.id,
                    title = c.title.ifBlank { "Трек" },
                    startedAt = c.startedAt,
                    endedAt = c.endedAt,
                    distanceKm = c.distanceKm,
                    avgSpeedKmh = c.avgSpeedKmh,
                    maxSpeedKmh = c.maxSpeedKmh,
                    preview = loader.previewPoints(c).map { it.latitude to it.longitude },
                    isFavorite = c.isFavorite,
                    status = TrackHistoryStatus.SERVER_ONLY,
                )
            }
            _state.update { st ->
                val keep = if (reset) st.items.filter { it.isLocal } else st.items
                val known = keep.map { it.id }.toSet()
                st.copy(
                    items = (keep + add.filter { it.id !in known }).sortedByDescending { it.startedAt },
                    remoteLoading = false,
                    canLoadMore = remoteCursor != null,
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update { it.copy(remoteLoading = false, remoteError = true, canLoadMore = false) }
        }
    }

    fun loadMore() {
        if (_state.value.remoteLoading || remoteCursor == null) return
        viewModelScope.launch { loadRemote(reset = false) }
    }

    fun toggleFavorite(item: TrackHistoryItem) {
        val newValue = !item.isFavorite
        _state.update { st -> st.copy(items = st.items.map { if (it.id == item.id) it.copy(isFavorite = newValue) else it }) }
        viewModelScope.launch {
            if (item.isLocal) {
                withContext(Dispatchers.Default) { repository.setFavorite(item.id, newValue) }
                trigger.trigger()
            } else {
                val ok = runRemote { token -> loader.setFavorite(token, item.id, newValue) }
                if (!ok) {
                    _state.update { st -> st.copy(items = st.items.map { if (it.id == item.id) it.copy(isFavorite = item.isFavorite) else it }) }
                }
            }
        }
    }

    fun delete(item: TrackHistoryItem) {
        viewModelScope.launch {
            if (item.isLocal) {
                // На сервере удалит воркер (очередь переживает перезапуск и отсутствие сети).
                withContext(Dispatchers.Default) { repository.deleteTrack(item.id, queueRemoteDelete = true) }
                statsCache.remove(item.id)
                _state.update { st -> st.copy(items = st.items.filter { it.id != item.id }) }
                trigger.trigger()
            } else if (runRemote { token -> loader.delete(token, item.id) }) {
                _state.update { st -> st.copy(items = st.items.filter { it.id != item.id }) }
            }
        }
    }

    /** «Удалить всю историю»: все локальные треки и все серверные копии. */
    fun deleteAll() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                repository.tracks().forEach { repository.deleteTrack(it.trackId, queueRemoteDelete = true) }
            }
            statsCache.clear()
            _state.update { it.copy(items = emptyList(), canLoadMore = false) }
            remoteCursor = null
            // Серверные треки, которых не было на устройстве, удаляем одной операцией сразу.
            val token = bearer()
            if (token != null) {
                try {
                    loader.deleteAllRemote(token)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    postMessage("Серверные копии удалим, когда появится связь")
                }
            }
            trigger.trigger()
        }
    }

    /** Открыть трек на карте: локальный — из Track.db, серверный — скачиваем и декодируем. */
    fun open(item: TrackHistoryItem) {
        if (_state.value.opening) return
        _state.update { it.copy(opening = true) }
        viewModelScope.launch {
            try {
                val points: List<TrackPoint> = if (item.isLocal) {
                    withContext(Dispatchers.Default) { repository.points(item.id) }
                } else {
                    val token = bearer()
                    if (token == null) emptyList() else loader.download(token, item.id)
                }
                if (points.isEmpty()) {
                    postMessage("Не удалось открыть трек")
                    _state.update { it.copy(opening = false) }
                    return@launch
                }
                val zone = runCatching { settingsUseCase.getUserSettingFlow().first().displayTimeZone() }.getOrDefault("GMT+3")
                _state.update { it.copy(opening = false, opened = OpenedTrack(item.title, points, zone)) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(opening = false) }
                postMessage(if (e is TracksApiException) "Не удалось загрузить трек с сервера" else "Не удалось открыть трек")
            }
        }
    }

    /** Данные для карты (прореживание, стоянки) готовим вне главного потока. */
    suspend fun mapTrack(opened: OpenedTrack): TrainFormViewModel.TrainMapTrack = withContext(Dispatchers.Default) {
        val dwells = TrackAnalysis.detectDwells(opened.points)
        TrainFormViewModel.TrainMapTrack(
            points = opened.points,
            drawPoints = TrackAnalysis.decimate(opened.points),
            stops = TrackAnalysis.stopMarks(opened.points, dwells, emptyList()),
            zone = opened.zone,
        )
    }

    fun closeOpened() = _state.update { it.copy(opened = null) }
    fun messageShown() = _state.update { it.copy(message = null) }
    private fun postMessage(text: String) = _state.update { it.copy(message = text) }

    private suspend fun runRemote(block: suspend (String) -> Unit): Boolean {
        val token = bearer() ?: run {
            postMessage("Войдите в аккаунт, чтобы изменить серверный трек")
            return false
        }
        return try {
            block(token)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            postMessage("Не получилось: проверьте подключение к интернету")
            false
        }
    }
}
