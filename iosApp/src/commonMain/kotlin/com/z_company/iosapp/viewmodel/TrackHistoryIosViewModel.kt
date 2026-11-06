@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.Route
import com.z_company.domain.track.NO_TRAIN_ID
import com.z_company.domain.track.TrackInfo
import com.z_company.domain.track.TrackOrphanCleaner
import com.z_company.domain.track.TrackPacking
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.track.TrackUploadState
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.GnssPlausibility
import com.z_company.domain.util.displayTimeZone
import com.z_company.iosapp.repository.IosTrackConsentStore
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.tracks.TrackCardResponse
import com.z_company.repository.remote_rest.tracks.TrackHistoryLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ═════════════════════════════════════════════════════════════════════════════
// «Мои треки» (история) — SCREEN_SPECS §17.8.
// ═════════════════════════════════════════════════════════════════════════════

/** Точка ломаной мини-карты карточки. */
data class TrackPreviewPoint(val lat: Double, val lon: Double)

/** Статус карточки (§17.8): откуда карточка и на каком этапе отправки. */
object TrackHistoryCardStatus {
    /** Только на устройстве: нет согласия/подписки/входа либо трек «без поезда». */
    const val DEVICE_ONLY = "DEVICE_ONLY"
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    /** Сервер отказал окончательно (лимит хранилища). */
    const val REJECTED = "REJECTED"
    /** Есть на сервере, на этом устройстве нет. */
    const val SERVER_ONLY = "SERVER_ONLY"
}

data class TrackHistoryCard(
    val id: String,
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    /** «N км · в пути N ч M мин»; пока сводка не достроена — только «в пути …». */
    val distanceDurationLabel: String,
    /** «Явка: д МММ гггг, чч:мм» в поясе отображения; пусто — рейса/явки нет. */
    val workStartLabel: String,
    /** Ломаная мини-карты (≤60 точек); пусто — пустая плитка. */
    val preview: List<TrackPreviewPoint>,
    val isFavorite: Boolean,
    val status: String,
    /** Трек записан без поезда (`NO_TRAIN_ID`): всегда только на устройстве. */
    val noTrain: Boolean,
    val isLocal: Boolean,
)

data class TrackHistoryState(
    val loading: Boolean = true,
    val items: List<TrackHistoryCard> = emptyList(),
    val consent: Boolean = false,
    val remoteLoading: Boolean = false,
    /** Серверная часть не загрузилась — показаны только локальные треки. */
    val remoteError: Boolean = false,
    val canLoadMore: Boolean = false,
    val subscribed: Boolean = false,
    /** Идёт «Удалить всю историю». */
    val clearingAll: Boolean = false,
)

/**
 * Связь «поезд трека -> рейс» (порт Android `TrackRouteResolver`): заголовок карточки,
 * явка рейса, сироты. Читает ВСЕ рейсы, включая корзину: сиротой поезд считается,
 * только если его нет нигде (§6.9).
 */
internal class IosTrackRouteResolver(
    private val routeUseCase: RouteUseCase,
    private val trackRepository: TrackRepository,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {
    private var cached: Pair<Long, List<Route>>? = null

    /** Полная выборка рейсов тяжёлая; на один прогон хватает снимка не старше минуты. */
    suspend fun allRoutes(): List<Route>? {
        cached?.let { (at, routes) -> if (nowMs() - at < CACHE_MS) return routes }
        val loaded = withContext(Dispatchers.Default) {
            runCatching { routeUseCase.listRouteWithDeleting() }.getOrNull()
        } ?: return null
        cached = nowMs() to loaded
        return loaded
    }

    /** Сбросить снимок рейсов (перед открытием списка треков). */
    fun invalidate() {
        cached = null
    }

    /** Явка рейса (`basicData.timeStartWork`) по поезду трека; null — рейса/явки нет. */
    suspend fun workStartByTrain(trainId: String): Long? =
        allRoutes()?.firstOrNull { r -> r.trains.any { it.trainId == trainId } }?.basicData?.timeStartWork

    /** Явка рейса по его id (серверный трек: у карточки есть только `routeId`). */
    suspend fun workStartByRoute(routeId: String): Long? =
        allRoutes()?.firstOrNull { it.basicData.id == routeId }?.basicData?.timeStartWork

    /** Заголовок карточки («№ 2345 · Откуда — Куда») или null, если поезд/рейс не найдены. */
    suspend fun titleFor(trainId: String): String? {
        val routes = allRoutes() ?: return null
        for (route in routes) {
            val train = route.trains.firstOrNull { it.trainId == trainId } ?: continue
            if (route.basicData.isDeleted) return null
            return TrackPacking.title(train)
        }
        return null
    }

    /** Идентификаторы поездов всех рейсов (включая корзину); null — рейсы прочитать не удалось. */
    suspend fun existingTrainIds(): Set<String>? =
        allRoutes()?.flatMap { r -> r.trains.map { it.trainId } }?.toSet()

    /** Очистка осиротевших треков (§6.9); безопасна к вызову сколь угодно часто. */
    suspend fun cleanOrphans(graceMs: Long = TrackOrphanCleaner.GRACE_MS): Int {
        val routes = allRoutes() ?: return 0
        val trainIds = routes.flatMap { r -> r.trains.map { it.trainId } }.toSet()
        return withContext(Dispatchers.Default) {
            runCatching { TrackOrphanCleaner.clean(trackRepository, trainIds, nowMs(), graceMs) }.getOrDefault(0)
        }
    }

    private companion object {
        const val CACHE_MS = 60_000L
    }
}

/**
 * ViewModel «Мои треки» (§17.8): карточки локальных треков (Track.db) и серверных
 * (`GET /v1/tracks` — TrackHistoryLoader), сироты удаляются при открытии.
 * Координаты не логируются, в сообщениях пользователю их нет.
 */
class TrackHistoryIosViewModel(
    private val trackRepository: TrackRepository,
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val consentStore: IosTrackConsentStore,
    private val secureTokenStorage: SecureTokenStorage,
    private val loader: TrackHistoryLoader,
) : ViewModel() {

    private val resolver = IosTrackRouteResolver(routeUseCase, trackRepository)

    private val _state = MutableStateFlow(TrackHistoryState(consent = consentStore.saveTracks.value))
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Кэш сводок: trackId -> (число точек, готовая карточка). */
    private var statsCache = HashMap<String, Pair<Long, TrackHistoryCard>>()
    private var remoteCursor: String? = null

    /** Пояс отображения времени (§17.8: `displayTimeZone`). */
    private var zoneId: String = "GMT+3"

    fun watchState(callback: (TrackHistoryState) -> Unit) {
        viewModelScope.launch { _state.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        viewModelScope.launch { _messages.collect { callback(it) } }
    }

    /** Открытие экрана: сироты, локальный список, серверная часть (§17.8). */
    fun onScreenOpened() {
        viewModelScope.launch {
            val zone = runCatching { settingsUseCase.getUserSettingFlow().first().displayTimeZone() }
                .getOrDefault("GMT+3")
            zoneId = zone
            val subscribed = hasSubscription()
            _state.value = _state.value.copy(consent = consentStore.saveTracks.value, subscribed = subscribed)
            resolver.invalidate()
            runCatching { resolver.cleanOrphans(TrackOrphanCleaner.EXPLICIT_GRACE_MS) }
            loadLocal()
            loadRemote(reset = true)
        }
    }

    fun refresh() = onScreenOpened()

    fun loadMore() {
        if (_state.value.remoteLoading || remoteCursor == null) return
        viewModelScope.launch { loadRemote(reset = false) }
    }

    /** Звезда: локально `isFavorite` + `favoriteDirty`, серверный трек — сразу `PATCH`. */
    fun toggleFavorite(card: TrackHistoryCard) {
        val newValue = !card.isFavorite
        update(card.id) { it.copy(isFavorite = newValue) }
        viewModelScope.launch {
            if (card.isLocal) {
                withContext(Dispatchers.Default) { trackRepository.setFavorite(card.id, newValue) }
                postMessage(if (newValue) MSG_FAV_ADDED else MSG_FAV_REMOVED)
            } else {
                val ok = runRemote { token -> loader.setFavorite(token, card.id, newValue) }
                if (ok) {
                    postMessage(if (newValue) MSG_FAV_ADDED else MSG_FAV_REMOVED)
                } else {
                    update(card.id) { it.copy(isFavorite = card.isFavorite) }
                }
            }
        }
    }

    /** Удалить карточку: локальный — сразу (серверное удаление через очередь), серверный — запросом. */
    fun delete(card: TrackHistoryCard) {
        viewModelScope.launch {
            if (card.isLocal) {
                withContext(Dispatchers.Default) {
                    trackRepository.deleteTrack(card.id, queueRemoteDelete = true)
                }
                statsCache.remove(card.id)
                _state.value = _state.value.copy(items = _state.value.items.filter { it.id != card.id })
            } else if (runRemote { token -> loader.delete(token, card.id) }) {
                _state.value = _state.value.copy(items = _state.value.items.filter { it.id != card.id })
            } else {
                postMessage(MSG_NETWORK)
            }
        }
    }

    /**
     * «Удалить всю историю» (§17.7): локальные треки удаляются сразу, серверные —
     * одной операцией `DELETE /tracks {confirm:true}` (очередь не нужна).
     */
    fun deleteAllHistory() {
        if (_state.value.clearingAll) return
        viewModelScope.launch {
            _state.value = _state.value.copy(clearingAll = true)
            withContext(Dispatchers.Default) {
                runCatching { trackRepository.deleteAll() }
            }
            statsCache.clear()
            _state.value = _state.value.copy(items = emptyList(), clearingAll = false, canLoadMore = false)
            remoteCursor = null
            if (_state.value.subscribed) {
                runRemote { token -> loader.deleteAllRemote(token) }
            }
            postMessage(MSG_HISTORY_DELETED)
        }
    }

    fun dispose() {
        // Коллекции живут в viewModelScope и закрываются с ним.
    }

    // ── Внутреннее ──────────────────────────────────────────────────────────

    /** Подписка активна (тот же критерий, что у синхронизации маршрутов). */
    private suspend fun hasSubscription(): Boolean = runCatching {
        settingsUseCase.getUserSettingFlow().first().subscriptionPeriod >
            kotlin.time.Clock.System.now().toEpochMilliseconds()
    }.getOrDefault(false)

    private suspend fun bearer(): String? =
        secureTokenStorage.getAuthBearerTokenFlow().first()
            ?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }

    private suspend fun loadLocal() {
        val trainIds = resolver.existingTrainIds().orEmpty()
        val infos = withContext(Dispatchers.Default) {
            TrackOrphanCleaner.visible(trackRepository.tracks(), trainIds)
        }
        val canSend = consentStore.saveTracks.value && _state.value.subscribed
        val remoteOnly = _state.value.items.filter { !it.isLocal }
        val first = infos.map { skeleton(it, canSend) }
        _state.value = _state.value.copy(
            loading = false,
            items = mergeCards(first, remoteOnly),
        )
        for (info in infos) {
            val cached = statsCache[info.trackId]
            val pointCount = withContext(Dispatchers.Default) { trackRepository.pointCount(info.trackId) }
            if (cached != null && cached.first == pointCount) {
                val known = cached.second
                update(info.trackId) {
                    known.copy(isFavorite = info.isFavorite, status = statusOf(info, canSend), endedAt = info.endedAt)
                }
                continue
            }
            val full = withContext(Dispatchers.Default) { build(info, canSend) }
            statsCache[info.trackId] = pointCount to full
            update(info.trackId) { full }
        }
    }

    private suspend fun skeleton(info: TrackInfo, canSend: Boolean): TrackHistoryCard =
        statsCache[info.trackId]?.second?.copy(
            isFavorite = info.isFavorite,
            status = statusOf(info, canSend),
            endedAt = info.endedAt,
        ) ?: TrackHistoryCard(
            id = info.trackId,
            title = if (info.trainId == NO_TRAIN_ID) NO_TRAIN_TITLE else "Трек",
            startedAt = info.startedAt,
            endedAt = info.endedAt,
            distanceDurationLabel = durationLabel(info.endedAt - info.startedAt),
            workStartLabel = "",
            preview = emptyList(),
            isFavorite = info.isFavorite,
            status = statusOf(info, canSend),
            noTrain = info.trainId == NO_TRAIN_ID,
            isLocal = true,
        )

    private suspend fun build(info: TrackInfo, canSend: Boolean): TrackHistoryCard {
        val points = withContext(Dispatchers.Default) { trackRepository.points(info.trackId) }
        // Ложные метки времени/скачки (спуфинг) не должны раздувать «время в пути» и длину.
        val clean = withContext(Dispatchers.Default) {
            GnssPlausibility.sanitize(points).ifEmpty { points }
        }
        val stats = withContext(Dispatchers.Default) { TrackPacking.stats(clean) }
        val noTrain = info.trainId == NO_TRAIN_ID
        val title = if (noTrain) NO_TRAIN_TITLE else resolver.titleFor(info.trainId) ?: "Трек"
        val startedAt = clean.firstOrNull()?.timeMs ?: info.startedAt
        val endedAt = clean.lastOrNull()?.timeMs ?: info.endedAt
        val workStart = if (noTrain) null else runCatching { resolver.workStartByTrain(info.trainId) }.getOrNull()
        val preview = withContext(Dispatchers.Default) { TrackPacking.preview(clean, 60) }
        return TrackHistoryCard(
            id = info.trackId,
            title = title,
            startedAt = startedAt,
            endedAt = endedAt,
            distanceDurationLabel = distanceDurationLabel(stats.distanceKm, endedAt - startedAt),
            workStartLabel = workStart?.let { workStartLabel(it, zoneId) } ?: "",
            preview = preview.map { TrackPreviewPoint(it.latitude, it.longitude) },
            isFavorite = info.isFavorite,
            status = statusOf(info, canSend),
            noTrain = noTrain,
            isLocal = true,
        )
    }

    /** [canSend] — согласие №1 И активная подписка; трек без поезда на сервер не уходит никогда. */
    private fun statusOf(info: TrackInfo, canSend: Boolean): String = when {
        info.trainId == NO_TRAIN_ID -> TrackHistoryCardStatus.DEVICE_ONLY
        info.uploadState == TrackUploadState.UPLOADED -> TrackHistoryCardStatus.SENT
        info.uploadState == TrackUploadState.REJECTED -> TrackHistoryCardStatus.REJECTED
        !canSend -> TrackHistoryCardStatus.DEVICE_ONLY
        else -> TrackHistoryCardStatus.PENDING
    }

    /** Серверные карточки, которых нет на устройстве. Нужны вход и согласие; ошибка не мешает локальному списку. */
    private suspend fun loadRemote(reset: Boolean) {
        if (!consentStore.saveTracks.value) return
        if (!_state.value.subscribed) return
        val token = bearer() ?: return
        if (reset) remoteCursor = null
        _state.value = _state.value.copy(remoteLoading = true, remoteError = false)
        try {
            val page = loader.page(token, remoteCursor)
            remoteCursor = page.nextCursor
            val localIds = withContext(Dispatchers.Default) {
                trackRepository.tracks().map { it.trackId }.toSet()
            }
            // Удалённые локально, но ещё не удалённые на сервере (очередь) не показываем.
            val pendingDelete = withContext(Dispatchers.Default) {
                trackRepository.pendingRemoteDeletes().toSet()
            }
            val add = page.tracks
                .filter { it.id !in localIds && it.id !in pendingDelete }
                .map { c -> remoteCard(c) }
            val keep = if (reset) _state.value.items.filter { it.isLocal } else _state.value.items
            val known = keep.map { it.id }.toSet()
            _state.value = _state.value.copy(
                items = mergeCards(keep, add.filter { it.id !in known }),
                remoteLoading = false,
                canLoadMore = remoteCursor != null,
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = _state.value.copy(remoteLoading = false, remoteError = true, canLoadMore = false)
        }
    }

    private suspend fun remoteCard(c: TrackCardResponse): TrackHistoryCard {
        val preview = runCatching { loader.previewPoints(c) }.getOrDefault(emptyList())
        val workStart = c.routeId?.let { id ->
            runCatching { resolver.workStartByRoute(id) }.getOrNull()
        }
        return TrackHistoryCard(
            id = c.id,
            title = c.title.ifBlank { "Трек" },
            startedAt = c.startedAt,
            endedAt = c.endedAt,
            distanceDurationLabel = distanceDurationLabel(c.distanceKm, (c.endedAt - c.startedAt).coerceAtLeast(0L)),
            workStartLabel = workStart?.let { workStartLabel(it, zoneId) } ?: "",
            preview = preview.map { TrackPreviewPoint(it.latitude, it.longitude) },
            isFavorite = c.isFavorite,
            status = TrackHistoryCardStatus.SERVER_ONLY,
            noTrain = false,
            isLocal = false,
        )
    }

    /** Локальная копия приоритетна; новые сверху по времени начала (§17.8). */
    private fun mergeCards(local: List<TrackHistoryCard>, remote: List<TrackHistoryCard>): List<TrackHistoryCard> {
        val seen = HashSet<String>()
        return (local + remote).filter { seen.add(it.id) }.sortedByDescending { it.startedAt }
    }

    private fun update(id: String, transform: (TrackHistoryCard) -> TrackHistoryCard) {
        _state.value = _state.value.copy(items = _state.value.items.map { if (it.id == id) transform(it) else it })
    }

    private suspend fun runRemote(block: suspend (String) -> Unit): Boolean {
        val token = bearer() ?: return false
        return try {
            block(token)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private fun postMessage(text: String) {
        viewModelScope.launch { _messages.emit(text) }
    }

    private companion object {
        const val NO_TRAIN_TITLE = "Без поезда"
        const val MSG_FAV_ADDED = "Трек добавлен в избранное"
        const val MSG_FAV_REMOVED = "Трек убран из избранного"
        const val MSG_NETWORK = "Не получилось: проверьте подключение к интернету"
        const val MSG_HISTORY_DELETED = "История треков удалена"

        private val MONTHS_GENITIVE = listOf(
            "янв", "фев", "мар", "апр", "мая", "июн",
            "июл", "авг", "сен", "окт", "ноя", "дек",
        )

        /** «N км · в пути N ч M мин»; до 10 км — одна цифра после запятой (§17.8). */
        fun distanceDurationLabel(distanceKm: Double, durationMs: Long): String {
            val dur = durationLabel(durationMs)
            return if (distanceKm <= 0.0) dur else "${distanceLabel(distanceKm)} · $dur"
        }

        fun distanceLabel(distanceKm: Double): String =
            if (distanceKm < 10.0) {
                val rounded = (distanceKm * 10).toLong() / 10.0
                val whole = rounded.toLong()
                val tenth = ((rounded - whole) * 10).toLong()
                "$whole,$tenth км"
            } else {
                "${(distanceKm + 0.5).toLong()} км"
            }

        fun durationLabel(durationMs: Long): String {
            val totalMin = (durationMs.coerceAtLeast(0L) / 60_000L)
            val h = totalMin / 60
            val m = totalMin % 60
            return when {
                h > 0 && m > 0L -> "в пути $h ч $m мин"
                h > 0 -> "в пути $h ч"
                else -> "в пути $m мин"
            }
        }

        /** «Явка: д МММ гггг, чч:мм» в поясе отображения (§17.8). */
        fun workStartLabel(ms: Long, zoneId: String): String {
            val zone = runCatching { TimeZone.of(zoneId) }.getOrDefault(TimeZone.UTC)
            val dt = Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone)
            val month = MONTHS_GENITIVE.getOrElse(dt.monthNumber - 1) { "" }
            val hh = dt.hour.toString().padStart(2, '0')
            val mm = dt.minute.toString().padStart(2, '0')
            return "Явка: ${dt.dayOfMonth} $month ${dt.year}, $hh:$mm"
        }
    }
}
