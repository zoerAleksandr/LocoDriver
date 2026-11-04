package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.minus
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

/** Состояние экрана «Пассажиром» (SCREEN_SPECS §7) для Swift. Время — миллисекунды UTC. */
data class PassengerScreenState(
    val isLoading: Boolean = true,
    val trainNumber: String = "",
    val stationDeparture: String = "",
    val stationArrival: String = "",
    val timeDeparture: Long? = null,
    val timeArrival: Long? = null,
    val notes: String = "",
    val isWorkStartByArrival: Boolean = false,
    val workStartOffsetMinutes: Int = 0,
    /** Явка маршрута по этому пассажиру: прибытие + смещение. */
    val workStartTime: Long? = null,
    /** Прибытие − отправление (если оба заданы и данные валидны). */
    val resultTime: Long? = null,
    val errorMessage: String? = null,
    /** depBeforeStart / arrAfterEnd / depAfterEnd / arrBeforeStart или null (§7.3 п.3). */
    val outsideWarning: String? = null,
    val routeWorkStart: Long? = null,
    val routeWorkEnd: Long? = null,
    val displayOffsetSeconds: Int = 10800,
)

/**
 * iOS-аналог Android `PassengerFormViewModel` (SCREEN_SPECS §7): автосейв 500 мс, финал
 * в [dispose] (аналог `onCleared`), «явка по прибытию» (§7.2).
 * Регистрируется как Koin `factory` — по экземпляру на открытый экран.
 */
class PassengerScreenIosViewModel(
    private val passengerUseCase: PassengerUseCase,
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val stationDirectory: StationDirectoryUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(PassengerScreenState())

    private var started = false
    private var basicId = ""
    private var isNew = true
    private var passenger: Passenger? = null
    private var settings: UserSettings? = null
    private var routeWorkStart: Long? = null
    private var routeWorkEnd: Long? = null
    private var resultTime: Long? = null
    private var errorMessage: String? = null
    private var stationNames: List<String> = emptyList()
    private var autoSaveJob: Job? = null

    fun watchState(callback: (PassengerScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    // ═══ Старт (§7.1) ═════════════════════════════════════════════════════════

    fun start(basicId: String, passengerId: String?) {
        if (started) return
        started = true
        this.basicId = basicId
        scope.launch {
            if (passengerId == null) {
                isNew = true
                passenger = Passenger(basicId = basicId)
            } else {
                isNew = false
                val loaded = passengerUseCase.getPassengerById(passengerId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                passenger = (loaded as? ResultState.Success)?.data ?: Passenger(passengerId = passengerId, basicId = basicId)
            }
            settings = settingsUseCase.getUserSettingFlow().first()
            validate()
            publish(isLoading = false)

            // Окно «явка–сдача» маршрута — реактивно (§7.3 п.3).
            scope.launch {
                routeUseCase.routeDetails(basicId).collect { state ->
                    if (state is ResultState.Success) {
                        state.data?.let {
                            routeWorkStart = it.basicData.timeStartWork
                            routeWorkEnd = it.basicData.timeEndWork
                            publish(isLoading = false)
                        }
                    }
                }
            }
            // Подсказки станций: настройки + справочник станций (без сортировки по GPS).
            scope.launch {
                settingsUseCase.getFlowCurrentSettingsState().collect { s ->
                    if (s is ResultState.Success) {
                        s.data?.let { st ->
                            settings = st
                            stationNames = formMergeStationNames(st.stationList, stationDirectory.stations().map { it.name })
                        }
                    }
                }
            }
        }
    }

    // ═══ Редактирование ═══════════════════════════════════════════════════════

    fun setTrainNumber(value: String) = edit { it.copy(trainNumber = value.ifBlank { null }) }
    fun setStationDeparture(value: String) = edit { it.copy(stationDeparture = value.ifBlank { null }) }
    fun setStationArrival(value: String) = edit { it.copy(stationArrival = value.ifBlank { null }) }
    fun setNotes(value: String) = edit { it.copy(notes = value.ifBlank { null }) }

    fun setTimeDeparture(ms: Long?) {
        edit { it.copy(timeDeparture = ms) }
        validate()
        publish()
    }

    fun setTimeArrival(ms: Long?) {
        edit { it.copy(timeArrival = ms) }
        validate()
        publish()
        // Источник «явки по прибытию»: держим явку маршрута синхронной с прибытием.
        passenger?.takeIf { it.isWorkStartByArrival }?.let { p -> scope.launch { syncWorkStartToRoute(p) } }
    }

    /** Смещение явки от прибытия (мин), запоминается в настройках (§7.2). */
    fun setWorkStartOffsetMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, Passenger.MAX_WORK_START_OFFSET_MINUTES)
        edit { it.copy(workStartOffsetMinutes = clamped) }
        publish()
        rememberWorkStartOffset(clamped)
        passenger?.takeIf { it.isWorkStartByArrival }?.let { p -> scope.launch { syncWorkStartToRoute(p) } }
    }

    fun setWorkStartByArrival(enabled: Boolean) {
        val current = passenger ?: return
        val offset = if (enabled && !current.isWorkStartByArrival) {
            (settings?.lastEnteredWorkStartOffsetMinutes ?: Passenger.DEFAULT_WORK_START_OFFSET_MINUTES)
                .coerceIn(0, Passenger.MAX_WORK_START_OFFSET_MINUTES)
        } else {
            current.workStartOffsetMinutes
        }
        val updated = current.copy(isWorkStartByArrival = enabled, workStartOffsetMinutes = offset)
        passenger = updated
        publish()
        scheduleAutoSave()
        scope.launch { syncWorkStartToRoute(updated) }
    }

    private fun rememberWorkStartOffset(minutes: Int) {
        val s = settings ?: return
        if (s.lastEnteredWorkStartOffsetMinutes == minutes) return
        settings = s.copy(lastEnteredWorkStartOffsetMinutes = minutes)
        CoroutineScope(NonCancellable + Dispatchers.Default).launch {
            settingsUseCase.setWorkStartOffsetMinutes(minutes).collect { }
        }
    }

    private fun edit(block: (Passenger) -> Passenger) {
        val p = passenger ?: return
        passenger = block(p)
        publish()
        scheduleAutoSave()
    }

    // ═══ Станции ══════════════════════════════════════════════════════════════

    /** Подсказки станций; пустой запрос — весь список (раскрывается по фокусу, §7.3 п.5). */
    fun stationSuggestions(query: String): List<String> {
        val q = query.trim()
        return stationNames.filter { formStationNameMatches(it, q) && !it.equals(query, ignoreCase = false) }
    }

    fun removeStationName(value: String) {
        stationNames = stationNames.filterNot { it == value }
        scope.launch { settingsUseCase.removeStation(value) }
    }

    fun recentTimes(key: String): List<Long> = sharedPrefs.getRecentTimes(key)
    fun rememberTime(key: String, ms: Long) = sharedPrefs.addRecentTime(key, ms)

    // ═══ Валидация (§7.1) ═════════════════════════════════════════════════════

    private fun validate() {
        val p = passenger ?: return
        scope.launch {
            val probe = Route(
                passengers = mutableListOf(
                    Passenger(
                        timeArrival = p.timeArrival,
                        timeDeparture = p.timeDeparture,
                        isWorkStartByArrival = p.isWorkStartByArrival,
                    )
                )
            )
            val result = routeUseCase.isValidPassenger(probe).first()
            if (result is ResultState.Error) {
                errorMessage = result.entity.message
                resultTime = null
            } else {
                errorMessage = null
                resultTime = p.timeArrival - p.timeDeparture
            }
            publish()
        }
    }

    // ═══ Сохранение (§7.1) ════════════════════════════════════════════════════

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500)
            save()
        }
    }

    /** Автосейв: не пишет при ошибке валидации. */
    private suspend fun save() {
        if (errorMessage != null) return
        val p = passenger ?: return
        syncWorkStartToRoute(p)
        passengerUseCase.savePassenger(p).first { it !is ResultState.Loading }
    }

    private fun isPassengerEmpty(p: Passenger) =
        p.trainNumber.isNullOrBlank() && p.stationDeparture.isNullOrBlank() && p.stationArrival.isNullOrBlank() &&
            p.timeArrival == null && p.timeDeparture == null && p.notes.isNullOrBlank() && !p.isWorkStartByArrival

    /**
     * Финальная запись при уходе с экрана (аналог `onCleared`). BUG-29 (паритет с Android):
     * сохраняет пассажира даже при ошибке валидации.
     */
    fun dispose() {
        autoSaveJob?.cancel()
        val p = passenger
        if (p != null && !(isNew && isPassengerEmpty(p))) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch {
                syncWorkStartToRoute(p)
                passengerUseCase.savePassenger(p).first { it !is ResultState.Loading }
                // Станции регистрируются в общем списке только финальным значением.
                val names = listOfNotNull(p.stationDeparture, p.stationArrival).filter { it.isNotBlank() }
                if (names.isNotEmpty()) settingsUseCase.setStations(names)
            }
        }
        scope.cancel()
    }

    /** Немедленная запись (сворачивание приложения), не дожидаясь дебаунса. */
    fun flush() {
        autoSaveJob?.cancel()
        scope.launch { save() }
    }

    /** §7.2: перенос состояния «явки по прибытию» в маршрут. */
    private suspend fun syncWorkStartToRoute(current: Passenger) {
        val routeId = current.basicId.ifBlank { basicId }
        val routeState = routeUseCase.routeDetails(routeId).first { it is ResultState.Success }
        val fresh = (routeState as? ResultState.Success)?.data ?: return

        val merged = fresh.passengers
            .map { p ->
                when {
                    p.passengerId == current.passengerId -> current
                    current.isWorkStartByArrival -> p.copy(isWorkStartByArrival = false)
                    else -> p
                }
            }
            .toMutableList()
            .also { list -> if (list.none { it.passengerId == current.passengerId }) list.add(current) }

        val basic = fresh.basicData
        val newBasic = if (current.isWorkStartByArrival && current.timeArrival != null) {
            basic.copy(
                timeStartWorkBeforeArrival = basic.timeStartWorkBeforeArrival ?: basic.timeStartWork,
                timeStartWork = current.workStartTime,
            )
        } else if (merged.none { it.isWorkStartByArrival }) {
            basic.copy(
                timeStartWork = basic.timeStartWorkBeforeArrival ?: basic.timeStartWork,
                timeStartWorkBeforeArrival = null,
            )
        } else {
            basic
        }
        routeUseCase.saveRoute(fresh.copy(basicData = newBasic, passengers = merged)).first { it !is ResultState.Loading }
    }

    // ═══ Публикация состояния ═════════════════════════════════════════════════

    private fun displayOffsetSeconds(): Int {
        // BUG-07 (паритет с Android): «GMT+05:30» не разбирается → GMT+3.
        val tz = settings?.displayTimeZone() ?: "GMT+3"
        val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
        return hours * 3600
    }

    private fun outsideWarning(p: Passenger): String? {
        val ws = routeWorkStart
        val we = routeWorkEnd
        val dep = p.timeDeparture
        val arr = p.timeArrival
        if (p.isWorkStartByArrival || ws == null || we == null) return null
        if (dep != null && arr != null && arr <= dep) return null
        return when {
            dep != null && dep < ws -> "depBeforeStart"
            arr != null && arr > we -> "arrAfterEnd"
            dep != null && dep > we -> "depAfterEnd"
            arr != null && arr < ws -> "arrBeforeStart"
            else -> null
        }
    }

    private fun publish(isLoading: Boolean = _state.value.isLoading) {
        val p = passenger
        _state.value = if (p == null) {
            PassengerScreenState(isLoading = isLoading, displayOffsetSeconds = displayOffsetSeconds())
        } else {
            PassengerScreenState(
                isLoading = isLoading,
                trainNumber = p.trainNumber.orEmpty(),
                stationDeparture = p.stationDeparture.orEmpty(),
                stationArrival = p.stationArrival.orEmpty(),
                timeDeparture = p.timeDeparture,
                timeArrival = p.timeArrival,
                notes = p.notes.orEmpty(),
                isWorkStartByArrival = p.isWorkStartByArrival,
                workStartOffsetMinutes = p.workStartOffsetMinutes,
                workStartTime = p.workStartTime,
                resultTime = resultTime,
                errorMessage = errorMessage,
                outsideWarning = outsideWarning(p),
                routeWorkStart = routeWorkStart,
                routeWorkEnd = routeWorkEnd,
                displayOffsetSeconds = displayOffsetSeconds(),
            )
        }
    }
}

/** Совпадение с начала названия или с начала любого слова (Android `formStationNameMatches`). */
internal fun formStationNameMatches(name: String, query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    if (name.startsWith(q, ignoreCase = true)) return true
    return name.split(' ', '-', '.', '/').any { it.isNotEmpty() && it.startsWith(q, ignoreCase = true) }
}

/** Единый список подсказок: станции настроек + справочник, без дублей (Android `mergeStationSuggestions`). */
internal fun formMergeStationNames(settingsNames: List<String>, directoryNames: List<String>): List<String> {
    val seen = HashSet<String>()
    return (settingsNames + directoryNames).map { it.trim() }.filter { it.isNotEmpty() && seen.add(it.lowercase()) }
}
