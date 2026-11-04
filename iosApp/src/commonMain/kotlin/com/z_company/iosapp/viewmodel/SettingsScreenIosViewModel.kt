@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.LocomotiveSeriesRepository
import com.z_company.domain.repositories.PartnerRepository
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.repositories.StationNormRepository
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Состояние экрана «Настройки» (SCREEN_SPECS §17) для Swift.
 * Локальные настройки устройства (§17: тема, показ полей локомотива, «Свыше 12 часов») — из prefs, не из UserSettings.
 */
data class SettingsScreenState(
    val settings: UserSettings?,
    val seriesCount: Int,
    val stationCount: Int,
    val phasesCount: Int,
    val partnersCount: Int,
    /** Норма выбранного месяца, минуты (null — ещё не посчитана). */
    val normaMinutes: Int?,
    val showLocoHeating: Boolean,
    val showLocoAuxiliary: Boolean,
    val showOtherCurrent: Boolean,
    /** MODE_SYSTEM / MODE_LIGHT / MODE_DARK. */
    val themeMode: String,
    /** ALWAYS_ASK / AUTO / NEVER_ASK. */
    val passenger12h: String,
)

/**
 * iOS-аналог SettingsViewModel (Android). Правка поля сразу видна в UI, запись в БД — через 300 мс
 * после последней правки (дебаунс, §17); незавершённая запись выполняется в onCleared.
 * Страна/регион: только запись полей (загрузка календаря — сквозная задача С1, не сделана).
 */
class SettingsScreenIosViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val prefs: SharedPreferencesRepositories,
    private val seriesRepository: LocomotiveSeriesRepository,
    private val stationRepository: StationNormRepository,
    private val partnerRepository: PartnerRepository,
    private val normaUseCase: NormaUseCase,
) : ViewModel() {

    private val current = MutableStateFlow<UserSettings?>(null)
    private val deviceTick = MutableStateFlow(0)
    private var saveJob: Job? = null
    private var dirty = false

    private val settingsSource = settingsUseCase.getUserSettingFlow()

    private val normaFlow = current.map { it?.selectMonthOfYear?.let { m -> m.year to m.month } }
        .flatMapLatest { ym ->
            if (ym == null) flowOf<Int?>(null) else normaUseCase.normaHoursFlow(ym.first, ym.second).map<Int, Int?> { it * 60 }
        }

    private val counts = combine(
        seriesRepository.getAllFlow(), stationRepository.getAllFlow(), partnerRepository.getAllFlow(), current,
    ) { series, stations, partners, s ->
        val serNames = series.map { it.name.trim().lowercase() }.toSet()
        val stNames = stations.map { it.name.trim().lowercase() }.toSet()
        val legacySeries = s?.locomotiveSeriesList.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet() - serNames
        val legacyStations = s?.stationList.orEmpty().map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet() - stNames
        listOf(series.size + legacySeries.size, stations.size + legacyStations.size, partners.size)
    }

    private val state = MutableStateFlow(emptyState())

    private fun emptyState() = SettingsScreenState(null, 0, 0, 0, 0, null, false, false, false, "MODE_SYSTEM", "ALWAYS_ASK")

    init {
        viewModelScope.launch {
            settingsSource.collect { s -> if (!dirty) current.value = s }
        }
        viewModelScope.launch {
            combine(current, counts, normaFlow, deviceTick) { s, c, norma, _ -> build(s, c, norma) }
                .collect { state.value = it }
        }
    }

    private fun build(s: UserSettings?, c: List<Int>, norma: Int?) = SettingsScreenState(
        settings = s,
        seriesCount = c[0],
        stationCount = c[1],
        phasesCount = s?.servicePhases?.size ?: 0,
        partnersCount = c[2],
        normaMinutes = norma,
        showLocoHeating = prefs.isShowLocoHeating(),
        showLocoAuxiliary = prefs.isShowLocoAuxiliary(),
        showOtherCurrent = prefs.isShowOtherCurrent(),
        themeMode = prefs.getThemeMode() ?: "MODE_SYSTEM",
        passenger12h = when {
            prefs.isPassenger12hDontAskAgain() && prefs.isPassenger12hAutoAccepted() -> "AUTO"
            prefs.isPassenger12hDontAskAgain() -> "NEVER_ASK"
            else -> "ALWAYS_ASK"
        },
    )

    fun watchState(callback: (SettingsScreenState) -> Unit) {
        viewModelScope.launch { state.collect { callback(it) } }
    }

    // ── автосохранение ────────────────────────────────────────────────────

    private fun edit(transform: (UserSettings) -> UserSettings) {
        val s = current.value ?: return
        current.value = transform(s)
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            flush()
        }
    }

    private fun flush() {
        val s = current.value ?: return
        if (!dirty) return
        dirty = false
        viewModelScope.launch {
            settingsUseCase.saveSetting(s.copy(updateAt = settingsUseCase.nextUpdateAt(s.updateAt))).collect {}
            prefs.setSettingsSyncPending(true)
        }
    }

    /** Вызывать из Swift при уходе с экрана: незавершённое сохранение выполняется принудительно. */
    fun flushPending() {
        saveJob?.cancel()
        flush()
    }

    override fun onCleared() {
        flush()
        super.onCleared()
    }

    // ── Основные ──────────────────────────────────────────────────────────

    fun setDefaultLocoType(electric: Boolean) = edit { it.copy(defaultLocoType = if (electric) LocoType.ELECTRIC else LocoType.DIESEL) }
    fun setDecimalTime(value: Boolean) = edit { it.copy(isDecimalTime = value) }
    fun setUsingDefaultWorkTime(value: Boolean) = edit { it.copy(usingDefaultWorkTime = value) }
    fun setDefaultWorkTime(millis: Long) = edit { it.copy(defaultWorkTime = millis) }
    fun setUseStandardTimePicker(value: Boolean) = edit { it.copy(useStandardTimePicker = value) }

    fun setThemeMode(mode: String) {
        prefs.setThemeMode(mode)
        deviceTick.value += 1
    }

    fun setPassenger12h(option: String) {
        prefs.setPassenger12hDontAskAgain(option != "ALWAYS_ASK")
        prefs.setPassenger12hAutoAccepted(option == "AUTO")
        deviceTick.value += 1
    }

    // ── Маршрут ───────────────────────────────────────────────────────────

    fun setShowBreak(v: Boolean) = edit { it.copy(isShowBreak = v) }
    fun setShowOnePerson(v: Boolean) = edit { it.copy(isShowOnePersonSwitch = v) }
    fun setShowPartner(v: Boolean) = edit { it.copy(isShowPartner = v) }
    fun setShowLocomotive(v: Boolean) = edit { it.copy(isShowLocomotive = v) }
    fun setShowTrain(v: Boolean) = edit { it.copy(isShowTrain = v) }
    fun setShowPassenger(v: Boolean) = edit { it.copy(isShowPassenger = v) }
    fun setShowOtherWork(v: Boolean) = edit { it.copy(isShowOtherWork = v) }

    // ── Локомотив ─────────────────────────────────────────────────────────

    fun setShowLocoHeating(v: Boolean) { prefs.setShowLocoHeating(v); deviceTick.value += 1 }
    fun setShowLocoAuxiliary(v: Boolean) { prefs.setShowLocoAuxiliary(v); deviceTick.value += 1 }
    fun setShowOtherCurrent(v: Boolean) { prefs.setShowOtherCurrent(v); deviceTick.value += 1 }
    fun setConsiderHeatingInTotal(v: Boolean) = edit { it.copy(isConsiderLocoHeatingInTotal = v) }
    fun setConsiderAuxiliaryInTotal(v: Boolean) = edit { it.copy(isConsiderLocoAuxiliaryInTotal = v) }

    // ── Поезд ─────────────────────────────────────────────────────────────

    fun setPassengerWagonLength(meters: Double) = edit { it.copy(passengerWagonLengthMeters = meters) }

    // ── Учёт ──────────────────────────────────────────────────────────────

    fun setNightTime(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) = edit {
        it.copy(nightTime = it.nightTime.copy(
            startNightHour = startHour, startNightMinute = startMinute,
            endNightHour = endHour, endNightMinute = endMinute,
        ))
    }
    fun setConsiderFutureRoute(v: Boolean) = edit { it.copy(isConsiderFutureRoute = v) }

    // ── Отдых ─────────────────────────────────────────────────────────────

    fun setMinTimeRest(millis: Long) = edit { it.copy(minTimeRestPointOfTurnover = millis) }
    fun setMinTimeRestSecond(millis: Long) = edit { it.copy(minTimeRestPointOfTurnoverSecond = millis) }
    fun setMinTimeHomeRest(millis: Long) = edit { it.copy(minTimeHomeRest = millis) }

    // ── Норма / регион (только запись полей, §17.2; загрузка календаря — С1) ─

    fun setTimeZone(millis: Long) = edit { it.copy(timeZone = millis) }
    fun setCrossMonthTimezone(local: Boolean) =
        edit { it.copy(crossMonthTimezone = if (local) CrossMonthTimezone.LOCAL else CrossMonthTimezone.MOSCOW) }

    fun setCountry(country: String) = edit {
        it.copy(
            country = country,
            timeZone = when (country) { "KZ" -> 7_200_000L; "BY" -> 0L; else -> it.timeZone },
            crossMonthTimezone = if (country == "RU") it.crossMonthTimezone else CrossMonthTimezone.LOCAL,
            region = if (country == it.country) it.region else null,
        )
    }

    // ── Плечи ─────────────────────────────────────────────────────────────

    /** Редактирование заменяет плечо с тем же id, новое добавляется в конец; [alsoReverse] добавляет обратное. */
    fun savePhase(phase: ServicePhase, alsoReverse: Boolean) = edit { s ->
        val list = s.servicePhases
        val updated = if (list.any { it.id == phase.id }) list.map { if (it.id == phase.id) phase else it } else list + phase
        s.copy(servicePhases = if (alsoReverse) updated + phase.reversed() else updated)
    }

    fun deletePhase(id: String) = edit { s -> s.copy(servicePhases = s.servicePhases.filter { it.id != id }) }

    fun createPhase(from: String, to: String, distance: Int, rate: Double): ServicePhase =
        ServicePhase(departureStation = from, arrivalStation = to, distance = distance, linearMileageRate = rate)

    fun editedPhase(base: ServicePhase, from: String, to: String, distance: Int, rate: Double): ServicePhase =
        base.copy(departureStation = from, arrivalStation = to, distance = distance, linearMileageRate = rate)
}
