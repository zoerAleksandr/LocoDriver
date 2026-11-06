@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.CarInspector
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.TrainAssist
import com.z_company.domain.entities.route.TrainDataVersion
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.domain.entities.route.UtilsForEntities.trainCategory
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.sanitizeNumericInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * iOS-аналог Android `TrainFormViewModel` (SCREEN_SPECS §6): экран «Поезд».
 *
 * Регистрируется как Koin `factory` — по экземпляру на открытый экран. Swift вызывает [dispose]
 * в `deinit` обёртки (аналог `onCleared`: финальное сохранение + guard пустого нового поезда, §6.1).
 * Автосейв — через 500 мс после любого изменения, «Готово» = просто выход.
 *
 * Не переносится на iOS (Android-only, §6.9 / §6.2 п.6а): карта поезда, «Автозапись», «Ограничения
 * поезда», «ВУ-45», подбор названия новой станции по GPS, автоподбор/замер плеча по карте.
 */
class TrainScreenIosViewModel(
    private val trainUseCase: TrainUseCase,
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val saveMutex = Mutex()
    private val settingsMutex = Mutex()
    private val _state = MutableStateFlow(TrainScreenState())

    private data class StationForm(
        val id: String,
        val name: String? = null,
        val arrival: Long? = null,
        val departure: Long? = null,
        val track: String? = null,
        val isFinal: Boolean = false,
        val isPassing: Boolean = false,
        val segTrack: String? = null,
        val segNotes: String? = null,
    )

    private var started = false
    private var basicId = ""
    private var isNewTrain = true
    private var train: Train? = null
    private var stations: MutableList<StationForm> = mutableListOf()
    private var phase: ServicePhase? = null
    private var settings: UserSettings? = null
    private var routeStartWork: Long? = null
    private var routeEndWork: Long? = null
    private var routeKnown = false
    private var isReversed = false
    private var showSegments = true
    private var validationError: String? = null
    private var saveError: String? = null
    private var autoSaveJob: Job? = null
    private var isLoading = true

    // ═══ Подписки для Swift ═══════════════════════════════════════════════════

    fun watchState(callback: (TrainScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    // ═══ Старт / загрузка (§6.1) ══════════════════════════════════════════════

    fun start(basicId: String, trainId: String?) {
        if (started) return
        started = true
        this.basicId = basicId
        scope.launch {
            isReversed = sharedPrefs.isReversedSortStationList()
            showSegments = sharedPrefs.isShowSegments()
            settings = settingsUseCase.getUserSettingFlow().first()
            // Маршрут для валидации читается один раз при открытии формы (§6.1).
            try {
                val result = routeUseCase.routeDetails(basicId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                (result as? ResultState.Success)?.data?.let {
                    routeStartWork = it.basicData.timeStartWork
                    routeEndWork = it.basicData.timeEndWork
                    routeKnown = true
                }
            } catch (e: Exception) {
                // Не критично: без маршрута валидация пропускается.
            }
            if (trainId == null) {
                isNewTrain = true
                train = Train(basicId = basicId)
            } else {
                val loaded = trainUseCase.getTrainById(trainId)
                    .first { it !is ResultState.Loading }
                val data = (loaded as? ResultState.Success)?.data
                if (data == null) {
                    isNewTrain = true
                    train = Train(basicId = basicId)
                } else {
                    isNewTrain = false
                    train = data.withoutZeroParams()
                    stations = data.stations.map {
                        StationForm(
                            id = it.stationId,
                            name = it.stationName,
                            arrival = it.timeArrival,
                            departure = it.timeDeparture,
                            track = it.trackNumber,
                            isFinal = it.isFinalStation,
                            isPassing = it.isPassingStation,
                            segTrack = it.segmentTrackNumber,
                            segNotes = it.segmentNotes,
                        )
                    }.toMutableList()
                    phase = data.servicePhase
                }
            }
            isLoading = false
            publish()
        }
    }

    private val zero = Regex("0+([.,]0+)?")
    private fun String?.zeroToNull(): String? = this?.takeUnless { zero.matches(it.trim()) }

    /** Нули из БД/синхронизации («0», «0.0») — «не задано» (§6.2 п.3). */
    private fun Train.withoutZeroParams(): Train = copy(
        distance = distance.zeroToNull(),
        weight = weight.zeroToNull(),
        axle = axle.zeroToNull(),
        conditionalLength = conditionalLength.zeroToNull(),
        dataVersions = dataVersions.map {
            it.copy(
                weight = it.weight.zeroToNull(),
                axle = it.axle.zeroToNull(),
                conditionalLength = it.conditionalLength.zeroToNull(),
            )
        },
    )

    // ═══ Номера и характеристики (§6.2 п.2–3) ═════════════════════════════════

    fun setNumber(value: String) { mutate { it.copy(number = value.ifBlank { null }) } }

    /** «+»: текущий номер уходит в доп. номера, поле очищается. */
    fun addCurrentNumberToAdditional() {
        mutate { t ->
            val n = t.number?.trim().orEmpty()
            if (n.isEmpty()) t else t.copy(
                number = null,
                additionalNumbers = (t.additionalNumbers + n).toMutableList(),
            )
        }
    }

    fun removeAdditionalNumber(index: Int) {
        mutate { t ->
            if (index !in t.additionalNumbers.indices) t
            else t.copy(additionalNumbers = t.additionalNumbers.toMutableList().also { it.removeAt(index) })
        }
    }

    fun setDistance(value: String) { mutate { it.copy(distance = value.ifBlank { null }) } }

    fun setWeight(value: String) {
        val v = value.sanitizeNumericInput().ifBlank { null }
        mutate { it.copy(weight = v, dataVersions = it.dataVersions.updateLast { ver -> ver.copy(weight = v) }) }
    }

    fun setAxle(value: String) {
        val v = value.sanitizeNumericInput().ifBlank { null }
        mutate { it.copy(axle = v, dataVersions = it.dataVersions.updateLast { ver -> ver.copy(axle = v) }) }
    }

    fun setConditionalLength(value: String) {
        val v = value.sanitizeNumericInput(allowDecimal = true).ifBlank { null }
        mutate { it.copy(conditionalLength = v, dataVersions = it.dataVersions.updateLast { ver -> ver.copy(conditionalLength = v) }) }
    }

    private fun List<TrainDataVersion>.updateLast(transform: (TrainDataVersion) -> TrainDataVersion): List<TrainDataVersion> =
        if (isEmpty()) this else toMutableList().also { it[lastIndex] = transform(it[lastIndex]) }

    // ═══ История данных (§6.7) ═════════════════════════════════════════════════

    /** Значения для полей блока «Изменить данные поезда» в шторке станции. BUG-26: для новой станции — `stationId == null`. */
    fun dataPrefill(stationId: String?): TrainVersionUi {
        val t = train ?: return TrainVersionUi(-1, "", "", "", "")
        val v = t.dataVersions.firstOrNull { it.stationId == stationId }
        return TrainVersionUi(
            index = -1,
            title = "",
            weight = (v?.weight ?: t.weight).orEmpty(),
            axle = (v?.axle ?: t.axle).orEmpty(),
            conditionalLength = (v?.conditionalLength ?: t.conditionalLength).orEmpty().withoutZeroFraction(),
        )
    }

    fun addTrainDataVersion(stationId: String, stationName: String?, weight: String, axle: String, conditionalLength: String) {
        mutate { t ->
            val versions = t.dataVersions.toMutableList()
            if (versions.isEmpty()) {
                versions += TrainDataVersion(weight = t.weight, axle = t.axle, conditionalLength = t.conditionalLength)
            }
            val now = Clock.System.now().toEpochMilliseconds()
            val fresh = TrainDataVersion(
                stationId = stationId,
                stationName = stationName?.ifBlank { null },
                weight = weight.sanitizeNumericInput().ifBlank { null },
                axle = axle.sanitizeNumericInput().ifBlank { null },
                conditionalLength = conditionalLength.sanitizeNumericInput(allowDecimal = true).ifBlank { null },
                changedAt = now,
            )
            val existing = versions.indexOfFirst { it.stationId == stationId }
            if (existing >= 0) {
                versions[existing] = fresh.copy(changedAt = versions[existing].changedAt.takeIf { it != 0L } ?: now)
            } else {
                versions += fresh
            }
            val latest = versions.last()
            t.copy(weight = latest.weight, axle = latest.axle, conditionalLength = latest.conditionalLength, dataVersions = versions)
        }
    }

    /** Правка карточки в шторке истории: НЕ нормализуется (поле принимает только цифры, «.» и «,»). */
    fun updateTrainDataVersion(index: Int, weight: String, axle: String, conditionalLength: String) {
        mutate { t ->
            if (index !in t.dataVersions.indices) return@mutate t
            val versions = t.dataVersions.toMutableList()
            versions[index] = versions[index].copy(
                weight = weight.ifBlank { null },
                axle = axle.ifBlank { null },
                conditionalLength = conditionalLength.ifBlank { null },
            )
            val latest = versions.last()
            t.copy(weight = latest.weight, axle = latest.axle, conditionalLength = latest.conditionalLength, dataVersions = versions)
        }
    }

    fun deleteTrainDataVersion(index: Int) {
        mutate { t ->
            if (index !in t.dataVersions.indices) return@mutate t
            val versions = t.dataVersions.toMutableList().apply { removeAt(index) }
            val latest = versions.lastOrNull()
            t.copy(
                weight = latest?.weight ?: t.weight,
                axle = latest?.axle ?: t.axle,
                conditionalLength = latest?.conditionalLength ?: t.conditionalLength,
                dataVersions = versions,
            )
        }
    }

    // ═══ Вспомогательная тяга (§6.2 п.7) ═══════════════════════════════════════
    // kind: 0 — толкач, 1 — двойная тяга, 2 — сдвоенный поезд. field: series/number/driver/notes.

    fun addAssist(kind: Int) { mutate { replaceAssist(it, kind) { _ -> TrainAssist() } } }
    fun removeAssist(kind: Int) { mutate { replaceAssist(it, kind) { _ -> null } } }

    fun setAssistField(kind: Int, field: String, value: String) {
        val v = value.ifBlank { null }
        mutate {
            replaceAssist(it, kind) { a ->
                val base = a ?: TrainAssist()
                when (field) {
                    "series" -> base.copy(locomotiveSeries = v)
                    "number" -> base.copy(locomotiveNumber = v)
                    "driver" -> base.copy(driverName = v)
                    "notes" -> base.copy(notes = v)
                    else -> base
                }
            }
        }
    }

    /** Направление: «Я толкаю»/«Я первый» = true. До выбора — null. */
    fun setAssistDirection(kind: Int, isFirst: Boolean) {
        mutate { replaceAssist(it, kind) { a -> (a ?: TrainAssist()).copy(isFirst = isFirst) } }
    }

    private fun replaceAssist(t: Train, kind: Int, f: (TrainAssist?) -> TrainAssist?): Train = when (kind) {
        0 -> t.copy(pusher = f(t.pusher))
        1 -> t.copy(doubleTraction = f(t.doubleTraction))
        else -> t.copy(doubledTrain = f(t.doubledTrain))
    }

    /** Закрытие шторки тяги любым способом сохраняет серии в общий список серий (`saveAssistSeries`). */
    fun saveAssistSeries() {
        val t = train ?: return
        val list = listOfNotNull(t.pusher?.locomotiveSeries, t.doubleTraction?.locomotiveSeries, t.doubledTrain?.locomotiveSeries)
            .filter { it.isNotBlank() }
        if (list.isEmpty()) return
        scope.launch {
            list.forEach { settingsUseCase.setLocomotiveSeries(it) }
            settings = settingsUseCase.getUserSettingFlow().first()
            publish()
        }
    }

    /** Подсказки серии: `startsWith`, полное совпадение скрыто. */
    fun suggestSeries(query: String): List<String> {
        val all = settings?.locomotiveSeriesList.orEmpty().map { it.trim() }.filter { it.isNotBlank() }
        if (query.isEmpty()) return all
        return all.filter { it.startsWith(query, ignoreCase = true) }.filterNot { it.equals(query, ignoreCase = true) }
    }

    fun removeSeriesSuggestion(name: String) {
        scope.launch {
            settingsUseCase.removeLocomotiveSeries(name)
            settings = settingsUseCase.getUserSettingFlow().first()
            publish()
        }
    }

    // ═══ Вагонник (§6.2 п.8) ═══════════════════════════════════════════════════

    fun addCarInspector() { if (train?.carInspector == null) mutate { it.copy(carInspector = CarInspector()) } }
    fun removeCarInspector() { mutate { it.copy(carInspector = null) } }
    fun setCarInspectorFullName(value: String) {
        mutate { it.copy(carInspector = (it.carInspector ?: CarInspector()).copy(fullName = value.ifBlank { null })) }
    }
    fun setCarInspectorTabNumber(value: String) {
        mutate { it.copy(carInspector = (it.carInspector ?: CarInspector()).copy(tabNumber = value.ifBlank { null })) }
    }
    /** BUG-24 (паритет с Android): очистить время нельзя — только удалить вагонника целиком; параметр 0 здесь не используется. */
    fun setCarInspectorCouplingTime(ms: Long) {
        mutate { it.copy(carInspector = (it.carInspector ?: CarInspector()).copy(couplingTime = ms.takeIf { v -> v > 0L })) }
    }

    // ═══ Плечо (§6.2 п.4, §6.6) ════════════════════════════════════════════════

    /** Выбор плеча (`setSelectedServicePhase`); null — снять плечо (расстояние очищается). */
    fun selectPhase(id: String?) {
        val selected = id?.let { pid -> settings?.servicePhases?.firstOrNull { it.id == pid } }
        applyPhase(selected)
    }

    private fun applyPhase(selected: ServicePhase?) {
        val forTrain = selected?.forTrain()
        if (forTrain != null) {
            val changedPhase = forTrain != phase || phase == null
            setDistanceInternal(forTrain.distance.takeIf { it > 0 }?.toString() ?: "")
            if (changedPhase) {
                if (stations.isEmpty()) {
                    stations.add(StationForm(id = newId(), name = forTrain.departureStation))
                    stations.add(StationForm(id = newId(), name = forTrain.arrivalStation))
                } else {
                    if (stations.first().name != forTrain.departureStation) {
                        stations[0] = stations[0].copy(name = forTrain.departureStation)
                    }
                    if (stations.size == 1) {
                        stations.add(StationForm(id = newId(), name = forTrain.arrivalStation))
                    } else {
                        val last = stations.lastIndex
                        if (stations[last].name != forTrain.arrivalStation) {
                            stations[last] = stations[last].copy(name = forTrain.arrivalStation)
                        }
                    }
                }
            }
        } else {
            setDistanceInternal("")
        }
        phase = forTrain
        changed(validate = true)
    }

    private fun setDistanceInternal(value: String) {
        train = train?.copy(distance = value.ifBlank { null })
    }

    /**
     * Создание/правка плеча из шторки (§6.6, упрощённо: без промежуточных станций и карты).
     * [id] = null — новое. Правки пишутся в `UserSettings.servicePhases` + флаг отложенной синхронизации.
     */
    fun savePhase(id: String?, from: String, to: String, distance: Int, rate: Double, alsoReverse: Boolean) {
        scope.launch {
            settingsMutex.withLock {
                val s = settingsUseCase.getUserSettingFlow().first()
                val base = id?.let { pid -> s.servicePhases.firstOrNull { it.id == pid } }
                val phaseToSave = base?.copy(departureStation = from, arrivalStation = to, distance = distance, linearMileageRate = rate)
                    ?: ServicePhase(departureStation = from, arrivalStation = to, distance = distance, linearMileageRate = rate)
                val list = s.servicePhases
                val updated = if (list.any { it.id == phaseToSave.id }) list.map { if (it.id == phaseToSave.id) phaseToSave else it } else list + phaseToSave
                val withReverse = if (alsoReverse && base == null) updated + phaseToSave.reversed() else updated
                settingsUseCase.saveSetting(s.copy(servicePhases = withReverse)).first()
                sharedPrefs.setSettingsSyncPending(true)
                settings = settingsUseCase.getUserSettingFlow().first()
                if (phase?.id == phaseToSave.id) {
                    // Выбранное плечо обновилось: ссылка и расстояние в поезде.
                    phase = phaseToSave.forTrain()
                    setDistanceInternal(distance.takeIf { it > 0 }?.toString() ?: "")
                    changed(validate = false)
                }
            }
            publish()
        }
    }

    /** Удалить плечо; если оно было выбрано — выбор снимается. Подтверждение — в UI (`.dsDeleteAlert`). */
    fun deletePhase(id: String) {
        scope.launch {
            settingsMutex.withLock {
                val s = settingsUseCase.getUserSettingFlow().first()
                settingsUseCase.saveSetting(s.copy(servicePhases = s.servicePhases.filterNot { it.id == id })).first()
                sharedPrefs.setSettingsSyncPending(true)
                settings = settingsUseCase.getUserSettingFlow().first()
                if (phase?.id == id) {
                    phase = null
                    changed(validate = false)
                }
            }
            publish()
        }
    }

    // ═══ Станции (§6.2 п.5, §6.4, §6.5) ═══════════════════════════════════════

    private fun newId(): String = Station().stationId

    /** Стабильный UUID для новой станции (используется и станцией, и её версией данных, §6.7). */
    fun makeStationId(): String = newId()

    /**
     * Сохранение из шторки станции. [index] = -1 — новая станция ([newStationId] — заранее выданный id).
     * Полностью пустая новая станция не создаётся. У первой станции флаги сбрасываются, у проходной
     * отправление отбрасывается; новая при плече и ≥ 2 станциях встаёт перед последней.
     */
    fun saveStation(
        index: Int, newStationId: String?, name: String, track: String,
        arrival: Long?, departure: Long?, isFinal: Boolean, isPassing: Boolean,
    ) {
        val isFirst = index == 0 || (index == -1 && stations.isEmpty())
        val fin = if (isFirst) false else isFinal
        val pass = if (isFirst) false else isPassing
        val dep = if (pass) null else departure
        val nm = name.ifBlank { null }
        val tr = track.trim().take(6).ifBlank { null }
        if (index == -1) {
            if (nm == null && tr == null && arrival == null && departure == null && !isFinal && !isPassing) return
            val form = StationForm(newStationId ?: newId(), nm, arrival, dep, tr, fin, pass)
            if (phase != null && stations.size >= 2) stations.add(stations.lastIndex, form) else stations.add(form)
        } else if (index in stations.indices) {
            stations[index] = stations[index].copy(name = nm, arrival = arrival, departure = dep, track = tr, isFinal = fin, isPassing = pass)
        }
        changed(validate = true)
    }

    fun deleteStation(index: Int) {
        if (index !in stations.indices) return
        stations.removeAt(index)
        changed(validate = true)
    }

    /** `moveStation(from, to)` — индексы в исходном порядке (вызывающий учитывает обратный порядок отображения). */
    fun moveStation(from: Int, to: Int) {
        if (from !in stations.indices || to < 0 || to >= stations.size || from == to) return
        val item = stations.removeAt(from)
        stations.add(to, item)
        changed(validate = false)
    }

    /** Сохранение шторки перегона между станциями [afterIndex] и [afterIndex] + 1 (`saveSegmentFromSheet`). */
    fun saveSegment(afterIndex: Int, fromName: String, toName: String, track: String, notes: String) {
        val toIndex = afterIndex + 1
        val tr = track.trim().take(6).ifBlank { null }
        val nt = notes.ifBlank { null }
        if (afterIndex in stations.indices) stations[afterIndex] = stations[afterIndex].copy(name = fromName.ifBlank { null })
        if (toIndex in stations.indices) {
            stations[toIndex] = stations[toIndex].copy(name = toName.ifBlank { null }, segTrack = tr, segNotes = nt)
        } else if (toIndex == stations.size && toName.isNotBlank()) {
            // «Будущий перегон»: название в «До» создаёт вторую станцию.
            stations.add(StationForm(id = newId(), name = toName, segTrack = tr, segNotes = nt))
        }
        changed(validate = true)
    }

    fun toggleSortOrder() {
        isReversed = !isReversed
        sharedPrefs.toggleStationsSortOrder(isReversed)
        publish()
    }

    /** Возвращает новое значение: true — перегоны показаны. */
    fun toggleSegmentsVisibility(): Boolean {
        showSegments = !showSegments
        sharedPrefs.setShowSegments(showSegments)
        publish()
        return showSegments
    }

    /** Подсказки станций: `startsWith`, полное совпадение (без регистра, е/ё, пробелов) скрыто. */
    fun suggestStations(query: String): List<String> {
        val all = settings?.stationList.orEmpty().map { it.trim() }.filter { it.isNotBlank() }
        val q = query.trim()
        if (q.isEmpty()) return all.take(30)
        val nq = normalize(q)
        if (all.any { normalize(it) == nq }) return emptyList()
        return all.filter { normalize(it).startsWith(nq) }.take(30)
    }

    fun removeStationSuggestion(name: String) {
        scope.launch {
            settingsUseCase.removeStation(name)
            settings = settingsUseCase.getUserSettingFlow().first()
            publish()
        }
    }

    private fun normalize(s: String) = s.lowercase().replace('ё', 'е').replace('-', ' ').trim().replace(Regex("\\s+"), " ")

    // ═══ GO (§6.2 п.6) ═════════════════════════════════════════════════════════

    private fun findLastFilledTime(): Pair<Int, Boolean>? {
        // Boolean: true — отправление, false — прибытие.
        val endIdx = if (phase != null && stations.size >= 2) stations.lastIndex - 1 else stations.lastIndex
        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.isPassing) continue
            if (s.departure != null) return i to true
            if (s.arrival != null) return i to false
        }
        return null
    }

    /** BUG-23 (паритет с Android): время с секундами (не кратно минуте) — для секундомера. */
    fun go() {
        val now = Clock.System.now().toEpochMilliseconds()
        if (stations.isEmpty()) {
            stations.add(StationForm(id = newId(), departure = now))
            changed(validate = true)
            return
        }
        val last = findLastFilledTime()
        if (last == null) {
            stations[0] = stations[0].copy(departure = now)
        } else if (!last.second) {
            // Последнее — прибытие → отправление той же станции.
            stations[last.first] = stations[last.first].copy(departure = now)
        } else {
            var next = last.first + 1
            while (next <= stations.lastIndex && stations[next].isPassing) next++
            val isPhaseArrival = phase != null && next == stations.lastIndex
            if (next <= stations.lastIndex && !isPhaseArrival) {
                stations[next] = stations[next].copy(arrival = now)
            } else {
                val form = StationForm(id = newId(), arrival = now)
                if (phase != null && stations.size >= 2) stations.add(stations.lastIndex, form) else stations.add(form)
            }
        }
        changed(validate = true)
    }

    // ═══ Секундомер (§6.2 п.6) ═════════════════════════════════════════════════

    fun stopwatch(nowMs: Long): TrainStopwatchUi {
        val none = TrainStopwatchUi(0, "", 0)
        var bestTime = Long.MIN_VALUE
        var bestIsDeparture = false
        var bestIndex = -1
        stations.forEachIndexed { i, s ->
            if (s.isPassing) return@forEachIndexed
            s.arrival?.takeIf { it <= nowMs }?.let {
                // При равном времени прибытия и отправления событием считается отправление.
                if (it > bestTime) { bestTime = it; bestIsDeparture = false; bestIndex = i }
            }
            s.departure?.takeIf { it <= nowMs }?.let {
                if (it >= bestTime) { bestTime = it; bestIsDeparture = true; bestIndex = i }
            }
        }
        if (bestIndex < 0) return none
        if (!bestIsDeparture) {
            val st = stations[bestIndex]
            val phaseEnd = phase?.let { normalize(it.arrivalStation) == normalize(st.name.orEmpty()) && st.name != null } == true
            if (st.isFinal || phaseEnd) return none
        }
        val totalSec = ((nowMs - bestTime).coerceAtLeast(0L)) / 1000
        val totalMin = totalSec / 60
        val totalHours = totalMin / 60
        val days = totalHours / 24
        val text = when {
            days > 0 -> "${days}д ${totalHours % 24}ч ${totalMin % 60}мин"
            totalHours > 0 -> "${totalHours}ч ${totalMin % 60}мин"
            totalMin >= 5 -> "${totalMin}мин"
            else -> "$totalMin:${(totalSec % 60).toString().padStart(2, '0')}"
        }
        val level = when {
            bestIsDeparture -> 0
            totalMin >= 30 -> 2
            totalMin >= 5 -> 1
            else -> 0
        }
        return TrainStopwatchUi(if (bestIsDeparture) 2 else 1, text, level)
    }

    // ═══ Сохранение (§6.1) ═════════════════════════════════════════════════════

    private fun mutate(transform: (Train) -> Train) {
        val t = train ?: return
        train = transform(t)
        changed(validate = false)
    }

    private fun changed(validate: Boolean) {
        publish()
        if (validate) checkValid()
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500)
            persist()
        }
    }

    private fun checkValid() {
        val t = train ?: return
        if (!routeKnown) { validationError = null; publish(); return }
        val snapshot = t.copy(stations = buildStations(t.trainId))
        val basic = com.z_company.domain.entities.route.BasicData(timeStartWork = routeStartWork, timeEndWork = routeEndWork)
        scope.launch {
            val route = Route(basicData = basic, trains = mutableListOf(snapshot))
            val state = routeUseCase.isValidTrain(route).first()
            validationError = if (state is ResultState.Error) state.entity.message else null
            publish()
        }
    }

    private fun buildStations(trainId: String): MutableList<Station> = stations.map { s ->
        Station(
            stationId = s.id,
            trainId = trainId,
            stationName = s.name,
            timeArrival = s.arrival,
            timeDeparture = s.departure,
            trackNumber = s.track,
            isFinalStation = s.isFinal,
            isPassingStation = s.isPassing,
            segmentTrackNumber = s.segTrack,
            segmentNotes = s.segNotes,
        )
    }.toMutableList()

    private fun buildTrain(): Train? {
        val t = train ?: return null
        return t.copy(stations = buildStations(t.trainId), servicePhase = phase)
    }

    /** Пустой ли поезд (§6.1). BUG-21: вагонник не учитывается — как в Android. */
    private fun isTrainEmpty(t: Train): Boolean {
        val stationsEmpty = stations.all { s ->
            s.name.isNullOrBlank() && s.arrival == null && s.departure == null && s.track.isNullOrBlank()
        }
        return t.number.isNullOrBlank() && t.additionalNumbers.all { it.isBlank() } &&
            t.distance.isNullOrBlank() && t.weight.isNullOrBlank() && t.axle.isNullOrBlank() &&
            t.conditionalLength.isNullOrBlank() && t.pusher == null && t.doubleTraction == null &&
            t.doubledTrain == null && t.servicePhase == null && phase == null && stationsEmpty
    }

    private suspend fun persist() {
        val t = buildTrain() ?: return
        if (isNewTrain && isTrainEmpty(t)) return
        saveMutex.withLock {
            trainUseCase.saveTrain(t).collect { result ->
                if (result is ResultState.Error) {
                    saveError = "Ошибка: ${result.entity.message ?: result.entity.throwable?.message ?: ""}"
                    publish()
                }
            }
            val names = t.stations.mapNotNull { it.stationName?.trim() }.filter { it.isNotBlank() }
            if (names.isNotEmpty()) settingsUseCase.setStations(names)
        }
    }

    /** Немедленное сохранение (сворачивание приложения, уход с экрана). */
    fun flush() {
        autoSaveJob?.cancel()
        scope.launch { persist() }
    }

    /** Финальное сохранение при уходе с экрана (аналог `onCleared`). */
    fun dispose() {
        autoSaveJob?.cancel()
        val t = buildTrain()
        if (t != null && !(isNewTrain && isTrainEmpty(t))) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch {
                saveMutex.withLock { trainUseCase.saveTrain(t).collect {} }
                val names = t.stations.mapNotNull { it.stationName?.trim() }.filter { it.isNotBlank() }
                if (names.isNotEmpty()) settingsUseCase.setStations(names)
            }
        }
        scope.cancel()
    }

    fun clearSaveError() { saveError = null; publish() }

    // ═══ Состояние для Swift ═══════════════════════════════════════════════════

    private fun publish() { _state.value = buildState() }

    private fun displayOffsetSeconds(): Int {
        val tz = settings?.displayTimeZone() ?: "GMT+3"
        val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
        return hours * 3600
    }

    private fun String.withoutZeroFraction(): String =
        if (contains('.') && matches(Regex("-?\\d+\\.0+"))) substringBefore('.') else this

    private fun formatDuration(millis: Long): String {
        if (millis <= 0) return "0 мин"
        val totalMinutes = millis / 60_000
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "$h ч $m мин"
            h > 0 -> "$h ч"
            else -> "$m мин"
        }
    }

    private fun speedText(v: Double): String {
        val r = (v * 10).roundToInt()
        return "${r / 10},${r % 10}"
    }

    private fun hhmm(ms: Long, offsetSeconds: Int): String {
        val minutes = floor((ms + offsetSeconds * 1000L) / 60_000.0).toLong()
        val inDay = ((minutes % 1440) + 1440) % 1440
        return "${(inDay / 60).toString().padStart(2, '0')}:${(inDay % 60).toString().padStart(2, '0')}"
    }

    private fun isPassengerNumber(number: String?): Boolean {
        val n = number?.toIntOrNull() ?: return false
        return UtilsForEntities.passengerTrainNumberList.any { n in it }
    }

    private fun versionWord(n: Int): String {
        val m100 = n % 100
        val m10 = n % 10
        val w = when {
            m100 in 11..14 -> "версий"
            m10 == 1 -> "версия"
            m10 in 2..4 -> "версии"
            else -> "версий"
        }
        return "Данные менялись · $n $w"
    }

    private fun assistUi(a: TrainAssist?) = TrainAssistUi(
        present = a != null,
        series = a?.locomotiveSeries.orEmpty(),
        number = a?.locomotiveNumber.orEmpty(),
        driver = a?.driverName.orEmpty(),
        notes = a?.notes.orEmpty(),
        isFirst = a?.isFirst,
    )

    private fun TrainAssist.hasData() = !locomotiveSeries.isNullOrBlank() || !locomotiveNumber.isNullOrBlank() ||
        !driverName.isNullOrBlank() || !notes.isNullOrBlank() || isFirst != null

    private fun assistRow(label: String, a: TrainAssist, direction: String?): TrainSummaryRowUi {
        val details = buildString {
            a.locomotiveSeries?.takeIf { it.isNotBlank() }?.let { append(it) }
            a.locomotiveNumber?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" "); append("№$it") }
            a.driverName?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append("ТЧМ $it") }
            a.notes?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append(it) }
        }
        return TrainSummaryRowUi(if (direction != null) "$label · $direction" else label, details)
    }

    private fun travel(from: StationForm, to: StationForm): Long? {
        val fromDeparture = from.departure ?: from.arrival ?: return null
        val fromArrival = from.arrival ?: from.departure ?: return null
        val toDeparture = to.departure ?: to.arrival ?: return null
        val toArrival = to.arrival ?: to.departure ?: return null
        val diff = if (fromDeparture <= toArrival) toArrival - fromDeparture else fromArrival - toDeparture
        return diff.takeIf { it > 0 }
    }

    private fun dwellText(minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}ч$m'"
            h > 0 -> "${h}ч"
            else -> "$m'"
        }
    }

    private fun buildState(): TrainScreenState {
        val t = train ?: return TrainScreenState(isLoading = isLoading, displayOffsetSeconds = displayOffsetSeconds())
        val passenger = isPassengerNumber(t.number)
        val offset = displayOffsetSeconds()
        val n = stations.size

        val stationUi = stations.mapIndexed { i, s ->
            var text: String? = null
            var level = 0
            if (!s.isFinal && !s.isPassing && s.arrival != null && s.departure != null) {
                val diff = (s.departure - s.departure % 60_000L) - (s.arrival - s.arrival % 60_000L)
                if (diff > 0) {
                    val min = diff / 60_000L
                    text = dwellText(min)
                    level = when {
                        passenger && min > 20 -> 2
                        min > 30 -> 2
                        min > 5 -> 1
                        else -> 0
                    }
                }
            }
            TrainStationUi(
                id = s.id, name = s.name.orEmpty(), trackNumber = s.track.orEmpty(),
                arrival = s.arrival, departure = s.departure, isFinal = s.isFinal, isPassing = s.isPassing,
                segmentTrack = s.segTrack.orEmpty(), segmentNotes = s.segNotes.orEmpty(),
                dwellText = text, dwellLevel = level,
            )
        }

        // Строки в исходном порядке, затем разворот целиком (§6.2 п.5).
        val orig = mutableListOf<TrainRowUi>()
        for (i in 0 until n) {
            orig += TrainRowUi("s-${stations[i].id}", false, i, i == 0, i == n - 1, null, "", "", false)
            if (i < n - 1) {
                val next = stations[i + 1]
                val future = phase != null && i == n - 2 && next.arrival == null && n >= 2
                orig += TrainRowUi(
                    "g-${stations[i].id}", true, i, false, false,
                    travel(stations[i], next)?.let { formatDuration(it) },
                    next.segTrack.orEmpty(), next.segNotes.orEmpty(), future,
                )
            }
        }
        if (phase == null && n >= 1 && !stations[n - 1].isFinal) {
            orig += TrainRowUi("future", true, n - 1, false, false, null, "", "", true)
        }
        val rows = if (isReversed) orig.asReversed().toList() else orig

        // Сводка (исходный порядок).
        var total: String? = null
        var section: String? = null
        var tech: String? = null
        if (n >= 2) {
            val start = stations.first().departure ?: stations.first().arrival
            val end = stations.last().arrival ?: stations.last().departure
            if (start != null && end != null && end > start) {
                val totalMs = end - start
                total = formatDuration(totalMs)
                val dist = t.distance?.toDoubleOrNull()
                if (dist != null && dist > 0) {
                    section = speedText(dist / (totalMs / 3_600_000.0))
                    var stop = 0L
                    for (i in 1 until n - 1) {
                        val a = stations[i].arrival
                        val d = stations[i].departure
                        if (a != null && d != null && d > a) stop += d - a
                    }
                    val running = totalMs - stop
                    if (running > 0) tech = speedText(dist / (running / 3_600_000.0))
                }
            }
        }

        val versions = t.dataVersions.mapIndexed { i, v ->
            TrainVersionUi(
                index = i,
                title = if (v.stationId == null) "Исходные данные" else (v.stationName ?: "Станция"),
                weight = v.weight.orEmpty(),
                axle = v.axle.orEmpty(),
                conditionalLength = v.conditionalLength.orEmpty().withoutZeroFraction(),
            )
        }

        val assistRows = buildList {
            t.pusher?.takeIf { it.hasData() }?.let {
                add(assistRow("Толкач", it, when (it.isFirst) { true -> "Я толкаю"; false -> "Меня толкают"; null -> null }))
            }
            t.doubleTraction?.takeIf { it.hasData() }?.let { add(assistRow("Двойная тяга", it, null)) }
            t.doubledTrain?.takeIf { it.hasData() }?.let {
                add(assistRow("Сдвоенный поезд", it, when (it.isFirst) { true -> "Я первый"; false -> "Я второй"; null -> null }))
            }
        }

        val ci = t.carInspector
        val ciHasData = ci != null && (!ci.fullName.isNullOrBlank() || !ci.tabNumber.isNullOrBlank() || ci.couplingTime != null)
        val ciSummary = if (ciHasData && ci != null) {
            val details = buildString {
                ci.fullName?.takeIf { it.isNotBlank() }?.let { append(it) }
                ci.tabNumber?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append(" · "); append("таб. $it") }
            }
            val title = ci.couplingTime?.let { "Вагонник · прицепка ${hhmm(it, offset)}" } ?: "Вагонник"
            TrainSummaryRowUi(title, details)
        } else null

        // Длина состава в метрах (§6.2 п.3): грузовой коэффициент 14 м, пассажирский — из настроек.
        val units = t.conditionalLength?.takeIf { it != "0" }?.toDoubleOrNull()
        val lengthText = units?.let {
            val per = if (passenger) (settings?.passengerWagonLengthMeters ?: 24.5) else 14.0
            "· ${(it * per).toInt()} м"
        }

        val allPhases = settings?.servicePhases.orEmpty()
        val last = findLastFilledTime()

        return TrainScreenState(
            isLoading = isLoading,
            isNew = isNewTrain,
            number = t.number.orEmpty(),
            additionalNumbers = t.additionalNumbers.toList(),
            distance = t.distance.orEmpty(),
            weight = t.weight.orEmpty(),
            axle = t.axle.orEmpty(),
            conditionalLength = t.conditionalLength.orEmpty(),
            categoryText = if (t.number.isNullOrBlank()) null else t.trainCategory(),
            lengthMetersText = lengthText,
            hasPhase = phase != null,
            phaseTitle = phase?.let { "${it.departureStation} — ${it.arrivalStation}" }.orEmpty(),
            phaseDistanceText = phase?.let { "${it.distance} км" }.orEmpty(),
            phases = allPhases.map { TrainPhaseUi(it.id, it.departureStation, it.arrivalStation, it.distance, it.linearMileageRate, it.id == phase?.id) },
            pusher = assistUi(t.pusher),
            doubleTraction = assistUi(t.doubleTraction),
            doubledTrain = assistUi(t.doubledTrain),
            assistSummary = assistRows,
            hasCarInspector = ci != null,
            carInspectorFullName = ci?.fullName.orEmpty(),
            carInspectorTabNumber = ci?.tabNumber.orEmpty(),
            carInspectorCouplingTime = ci?.couplingTime,
            carInspectorSummary = ciSummary,
            assistHasData = t.pusher?.hasData() == true || t.doubleTraction?.hasData() == true || t.doubledTrain?.hasData() == true,
            inspectorHasData = ciHasData,
            stations = stationUi,
            rows = rows,
            isReversed = isReversed,
            showSegments = showSegments,
            versions = versions,
            versionsText = if (t.dataVersions.size > 1) versionWord(t.dataVersions.size) else null,
            summaryTotalTime = total,
            summarySectionSpeed = section,
            summaryTechSpeed = tech,
            goIsDeparture = stations.isEmpty() || last == null || !last.second,
            errorMessage = validationError,
            saveError = saveError,
            displayOffsetSeconds = offset,
        )
    }
}
