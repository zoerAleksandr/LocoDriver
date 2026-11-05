package com.z_company.iosapp.viewmodel

import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.OtherWorkUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.StationDirectoryUseCase
import com.z_company.domain.util.displayTimeZone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Строка шторки выбора локомотива (§8.3). */
data class OtherWorkLocoItem(val id: String, val label: String)

/** Состояние экрана «Прочая работа» (SCREEN_SPECS §8) для Swift. */
data class OtherWorkScreenState(
    val isLoading: Boolean = true,
    val workType: String = "",
    val station: String = "",
    val timeStart: Long? = null,
    val timeEnd: Long? = null,
    val notes: String = "",
    val resultTime: Long? = null,
    val errorMessage: String? = null,
    /** Предопределённые + пользовательские типы без пустых и дублей (§8.2). */
    val typeList: List<String> = emptyList(),
    /** Пользовательские типы (у них в шторке есть корзина). */
    val customTypes: List<String> = emptyList(),
    /** Не null — показать шторку выбора локомотива (несколько локо в маршруте). */
    val locoPicker: List<OtherWorkLocoItem>? = null,
    val displayOffsetSeconds: Int = 10800,
)

/**
 * iOS-аналог Android `OtherWorkFormViewModel` (SCREEN_SPECS §8): автосейв 500 мс,
 * финал в [dispose]. Регистрируется как Koin `factory` — по экземпляру на экран.
 */
class OtherWorkScreenIosViewModel(
    private val otherWorkUseCase: OtherWorkUseCase,
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val stationDirectory: StationDirectoryUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(OtherWorkScreenState())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    private var started = false
    private var isNew = true
    private var changed = false
    private var work: OtherWork? = null
    private var route: Route? = null
    private var settings: UserSettings? = null
    private var resultTime: Long? = null
    private var errorMessage: String? = null
    private var typeList: List<String> = OtherWork.PREDEFINED_TYPES
    private var stationNames: List<String> = emptyList()
    private var locoPicker: List<Locomotive>? = null
    private var autoSaveJob: Job? = null

    fun watchState(callback: (OtherWorkScreenState) -> Unit) {
        scope.launch { _state.collect { callback(it) } }
    }

    /** Одноразовые сообщения (snackbar) — «Укажите время явки…» и т. п. */
    fun watchMessages(callback: (String) -> Unit) {
        scope.launch { _messages.collect { callback(it) } }
    }

    // ═══ Старт (§8.1) ═════════════════════════════════════════════════════════

    fun start(basicId: String, otherWorkId: String?) {
        if (started) return
        started = true
        scope.launch {
            if (otherWorkId == null) {
                isNew = true
                // Предзаполнение последним типом — не ввод (§8.1).
                work = OtherWork(basicId = basicId, workType = sharedPrefs.getLastOtherWorkType())
            } else {
                isNew = false
                val loaded = otherWorkUseCase.getOtherWorkById(otherWorkId)
                    .first { it is ResultState.Success || it is ResultState.Error }
                work = (loaded as? ResultState.Success)?.data ?: OtherWork(otherWorkId = otherWorkId, basicId = basicId)
                recalc()
            }
            val s = settingsUseCase.getUserSettingFlow().first()
            applySettings(s)
            publish(isLoading = false)

            scope.launch {
                routeUseCase.routeDetails(basicId).collect { st ->
                    if (st is ResultState.Success) route = st.data
                }
            }
            scope.launch {
                settingsUseCase.getFlowCurrentSettingsState().collect { st ->
                    if (st is ResultState.Success) st.data?.let { applySettings(it); publish() }
                }
            }
        }
    }

    private fun applySettings(s: UserSettings) {
        settings = s
        typeList = (OtherWork.PREDEFINED_TYPES + s.otherWorkTypeList).filter { it.isNotBlank() }.distinct()
        stationNames = formMergeStationNames(s.stationList, stationDirectory.stations().map { it.name })
    }

    // ═══ Редактирование ═══════════════════════════════════════════════════════

    fun setWorkType(type: String) {
        val value = type.ifBlank { null }
        edit { it.copy(workType = value) }
        if (value != null) sharedPrefs.setLastOtherWorkType(value)
    }

    fun setStation(value: String) = edit { it.copy(station = value.ifBlank { null }) }
    fun setNotes(value: String) = edit { it.copy(notes = value.ifBlank { null }) }

    fun setTimeStart(ms: Long?) {
        edit { it.copy(timeStart = ms) }
    }

    fun setTimeEnd(ms: Long?) {
        edit { it.copy(timeEnd = ms) }
    }

    /** «От явки до окончания работы» (§8.2). */
    fun applyTimeFromWork() {
        val start = route?.basicData?.timeStartWork
        val end = route?.basicData?.timeEndWork
        if (start == null || end == null) {
            _messages.tryEmit("Укажите время явки и окончания работы в маршруте")
            return
        }
        edit { it.copy(timeStart = start, timeEnd = end) }
    }

    /** «От приёмки до сдачи локомотива» (§8.2): 0 локо — сообщение, 1 — сразу, иначе шторка. */
    fun applyTimeFromLoco() {
        val locos = route?.locomotives.orEmpty()
        when {
            locos.isEmpty() -> _messages.tryEmit("В маршруте нет локомотива")
            locos.size == 1 -> applyLocoTime(locos.first())
            else -> {
                locoPicker = locos.toList()
                publish()
            }
        }
    }

    fun selectLoco(id: String) {
        val loco = locoPicker?.firstOrNull { it.locoId == id }
        locoPicker = null
        publish()
        loco?.let { applyLocoTime(it) }
    }

    fun dismissLocoPicker() {
        locoPicker = null
        publish()
    }

    private fun applyLocoTime(loco: Locomotive) {
        val startBase = loco.timeBarrierOut ?: loco.timeEndOfAcceptance
        val endBase = loco.timeBarrierIn ?: loco.timeStartOfDelivery
        if (startBase == null || endBase == null) {
            _messages.tryEmit("Укажите время КП или окончание приёмки и начало сдачи локомотива")
            return
        }
        edit { it.copy(timeStart = startBase + 60_000L, timeEnd = endBase - 60_000L) }
    }

    /** Свой тип (§8.2): обрезка, совпадение с предопределённым — просто выбор; новый — в начало списка. */
    fun addCustomType(name: String) {
        val value = name.trim()
        if (value.isBlank()) return
        setWorkType(value)
        if (!OtherWork.PREDEFINED_TYPES.contains(value)) {
            scope.launch {
                settingsUseCase.setOtherWorkType(value)
            }
        }
    }

    /** Удаление своего типа (§8.2): выбранный в открытой записи сбрасывается в null. */
    fun deleteCustomType(name: String) {
        scope.launch { settingsUseCase.removeOtherWorkType(name) }
        typeList = typeList.filterNot { it == name && it !in OtherWork.PREDEFINED_TYPES }
        if (work?.workType == name) edit { it.copy(workType = null) } else publish()
    }

    // ═══ Станции ══════════════════════════════════════════════════════════════

    fun stationSuggestions(query: String): List<String> {
        val q = query.trim()
        return stationNames.filter { formStationNameMatches(it, q) && it != query }
    }

    fun removeStationName(value: String) {
        stationNames = stationNames.filterNot { it == value }
        scope.launch { settingsUseCase.removeStation(value) }
    }

    fun recentTimes(key: String): List<Long> = sharedPrefs.getRecentTimes(key)
    fun rememberTime(key: String, ms: Long) = sharedPrefs.addRecentTime(key, ms)

    // ═══ Сохранение ═══════════════════════════════════════════════════════════

    private fun edit(block: (OtherWork) -> OtherWork) {
        val w = work ?: return
        work = block(w)
        changed = true
        recalc()
        publish()
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500)
            save()
        }
    }

    private fun recalc() {
        val start = work?.timeStart
        val end = work?.timeEnd
        if (start != null && end != null && end <= start) {
            errorMessage = "Окончание должно быть позже начала."
            resultTime = null
        } else {
            errorMessage = null
            resultTime = if (start != null && end != null) end - start else null
        }
    }

    private suspend fun save() {
        if (errorMessage != null) return
        val w = work ?: return
        otherWorkUseCase.saveOtherWork(w).first { it !is ResultState.Loading }
    }

    private fun isEmpty(w: OtherWork) =
        w.workType.isNullOrBlank() && w.station.isNullOrBlank() && w.timeStart == null && w.timeEnd == null && w.notes.isNullOrBlank()

    fun flush() {
        autoSaveJob?.cancel()
        scope.launch { save() }
    }

    /** Финальная запись (аналог `onCleared`). */
    fun dispose() {
        autoSaveJob?.cancel()
        val w = work
        if (w != null && errorMessage == null && !(isNew && (!changed || isEmpty(w)))) {
            CoroutineScope(NonCancellable + Dispatchers.Default).launch {
                otherWorkUseCase.saveOtherWork(w).first { it !is ResultState.Loading }
                // Станция — в общий список только финальным значением.
                w.station?.takeIf { it.isNotBlank() }?.let { settingsUseCase.setStations(listOf(it)) }
            }
        }
        scope.cancel()
    }

    // ═══ Публикация ═══════════════════════════════════════════════════════════

    private fun displayOffsetSeconds(): Int {
        // BUG-07 (паритет с Android): «GMT+05:30» не разбирается → GMT+3.
        val tz = settings?.displayTimeZone() ?: "GMT+3"
        val hours = tz.removePrefix("GMT").removePrefix("UTC").toIntOrNull() ?: 3
        return hours * 3600
    }

    private fun locoLabel(loco: Locomotive, ordinal: Int): String {
        val s = loco.series?.takeIf { it.isNotBlank() }
        val n = loco.number?.takeIf { it.isNotBlank() }
        return when {
            s != null && n != null -> "$s-$n"
            s != null -> "$s б/н"
            n != null -> "${loco.type.text} $n"
            else -> "Локомотив $ordinal"
        }
    }

    private fun publish(isLoading: Boolean = _state.value.isLoading) {
        val w = work
        val picker = locoPicker?.mapIndexed { i, l -> OtherWorkLocoItem(l.locoId, locoLabel(l, i + 1)) }
        _state.value = OtherWorkScreenState(
            isLoading = isLoading,
            workType = w?.workType.orEmpty(),
            station = w?.station.orEmpty(),
            timeStart = w?.timeStart,
            timeEnd = w?.timeEnd,
            notes = w?.notes.orEmpty(),
            resultTime = resultTime,
            errorMessage = errorMessage,
            typeList = typeList,
            customTypes = typeList.filterNot { it in OtherWork.PREDEFINED_TYPES },
            locoPicker = picker,
            displayOffsetSeconds = displayOffsetSeconds(),
        )
    }
}
