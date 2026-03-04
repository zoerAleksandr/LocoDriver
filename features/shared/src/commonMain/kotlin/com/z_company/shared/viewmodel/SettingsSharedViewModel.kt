package com.z_company.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimeZoneRussia(
    val description: String,
    val offsetOfMoscow: Long,
)

data class SettingsUiState(
    val settingDetails: ResultState<UserSettings?> = ResultState.Loading(),
    val calendarState: ResultState<com.z_company.domain.entities.MonthOfYear?> = ResultState.Loading(),
    val saveSettingsState: ResultState<Unit>? = null,
    val updateAt: Long? = null,
    val monthList: List<Int> = listOf(),
    val yearList: List<Int> = listOf(),
    val showDialogAddServicePhase: Boolean = false,
    val selectedServicePhase: Pair<ServicePhase, Int>? = null,
    val servicePhases: List<ServicePhase> = emptyList(),
)

/**
 * Shared ViewModel for the settings screen.
 * Based on Android SettingsViewModel with full settings editing capabilities.
 */
class SettingsSharedViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _saveEvent = Channel<Unit>(Channel.CONFLATED)
    val saveEvent = _saveEvent.receiveAsFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null
    private var loadCalendarJob: Job? = null

    var currentSettings: UserSettings?
        get() {
            return _uiState.value.settingDetails.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(settingDetails = ResultState.Success(value))
            }
        }

    private var servicePhases: MutableList<ServicePhase>
        get() {
            return _uiState.value.servicePhases.toMutableList()
        }
        private set(value) {
            _uiState.update {
                it.copy(servicePhases = value)
            }
        }

    private val oneHourInMillis = 3_600_000L
    val timeZoneList = listOf(
        TimeZoneRussia("Калининград (MSK–1, UTC+2)", oneHourInMillis * -1),
        TimeZoneRussia("Москва (UTC+3)", oneHourInMillis * 0),
        TimeZoneRussia("Самара (MSK+1, UTC+4)", oneHourInMillis * 1),
        TimeZoneRussia("Екатеринбург (MSK+2, UTC+5)", oneHourInMillis * 2),
        TimeZoneRussia("Омск (MSK+3, UTC+6)", oneHourInMillis * 3),
        TimeZoneRussia("Красноярск (MSK+4, UTC+7)", oneHourInMillis * 4),
        TimeZoneRussia("Иркутск (MSK+5, UTC+8)", oneHourInMillis * 5),
        TimeZoneRussia("Якутск (MSK+6, UTC+9)", oneHourInMillis * 6),
        TimeZoneRussia("Владивосток (MSK+7, UTC+10)", oneHourInMillis * 7),
        TimeZoneRussia("Магадан (MSK+8, UTC+11)", oneHourInMillis * 8),
        TimeZoneRussia("Камчатка (MSK+9, UTC+12)", oneHourInMillis * 9),
        TimeZoneRussia("Анадырь (MSK+10, UTC+13)", oneHourInMillis * 10),
    )

    fun changeTimeFormat() {
        currentSettings?.let {
            currentSettings = it.copy(
                isDecimalTime = !it.isDecimalTime
            )
        }
    }

    fun showDialogAddServicePhase(servicePhase: ServicePhase) {
        _uiState.update {
            it.copy(showDialogAddServicePhase = true)
        }
    }

    fun hideDialogAddServicePhase() {
        _uiState.update {
            it.copy(showDialogAddServicePhase = false)
        }
    }

    fun addServicePhase(servicePhase: ServicePhase, index: Int = -1) {
        val list = servicePhases.toMutableList()
        if (index == -1) {
            list.add(servicePhase)
        } else {
            list[index] = servicePhase
        }
        servicePhases = list
        _uiState.update {
            it.copy(selectedServicePhase = null)
        }
        hideDialogAddServicePhase()
    }

    fun deleteServicePhase(index: Int) {
        val list = servicePhases.toMutableList()
        list.removeAt(index)
        servicePhases = list
    }

    fun selectToUpdateServicePhase(phase: ServicePhase, index: Int) {
        _uiState.update {
            it.copy(selectedServicePhase = Pair(phase, index))
        }
        showDialogAddServicePhase(phase)
    }

    init {
        loadSettings()
        loadMonthList()
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingsState = null)
        }
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            _uiState.update { state ->
                state.copy(
                    monthList = result.map { it.month }.distinct().sorted(),
                    yearList = result.map { it.year }.distinct().sorted()
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                _uiState.update {
                    it.copy(settingDetails = result)
                }
                if (result is ResultState.Success) {
                    result.data?.let { userSettings ->
                        _uiState.update {
                            it.copy(
                                updateAt = userSettings.updateAt,
                                servicePhases = userSettings.servicePhases.toList()
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { _ ->
                currentSettings?.let { setting ->
                    _uiState.update {
                        it.copy(
                            calendarState = ResultState.Success(setting.selectMonthOfYear)
                        )
                    }
                }
            }
        }
    }

    fun saveSettings() {
        val state = uiState.value.settingDetails
        if (state is ResultState.Success) {
            state.data?.let { settings ->
                settings.servicePhases = servicePhases.toMutableList()
                saveSettingsJob?.cancel()
                saveSettingsJob = viewModelScope.launch {
                    settingsUseCase.saveSetting(settings).collect { result ->
                        if (result is ResultState.Success) {
                            _saveEvent.trySend(Unit)
                        }
                    }
                }
            }
        }
    }

    fun changeMinTimeRest(time: Long) {
        currentSettings = currentSettings?.copy(
            minTimeRestPointOfTurnover = time
        )
    }

    fun changeDefaultWorkTime(timeInMillis: Long) {
        currentSettings = currentSettings?.copy(
            defaultWorkTime = timeInMillis
        )
    }

    fun changeDefaultLocoType() {
        currentSettings?.let { settings ->
            currentSettings = when (settings.defaultLocoType) {
                LocoType.ELECTRIC -> {
                    settings.copy(defaultLocoType = LocoType.DIESEL)
                }
                LocoType.DIESEL -> {
                    settings.copy(defaultLocoType = LocoType.ELECTRIC)
                }
            }
        }
    }

    fun changeMinTimeHomeRest(time: Long) {
        currentSettings = currentSettings?.copy(
            minTimeHomeRest = time
        )
    }

    fun changeStartNightTime(hour: Int, minute: Int) {
        currentSettings = currentSettings?.copy(
            nightTime = currentSettings!!.nightTime.copy(
                startNightHour = hour,
                startNightMinute = minute
            )
        )
    }

    fun changeEndNightTime(hour: Int, minute: Int) {
        currentSettings = currentSettings?.copy(
            nightTime = currentSettings!!.nightTime.copy(
                endNightHour = hour,
                endNightMinute = minute
            )
        )
    }

    fun changeUsingDefaultWorkTime(isUsing: Boolean) {
        currentSettings = currentSettings?.copy(
            usingDefaultWorkTime = isUsing
        )
    }

    fun changeConsiderFutureRoute(isConsider: Boolean) {
        currentSettings = currentSettings?.copy(
            isConsiderFutureRoute = isConsider
        )
    }

    fun changeShowBreak(isShow: Boolean) {
        currentSettings = currentSettings?.copy(
            isShowBreak = isShow
        )
    }

    fun setTimeZone(timeZone: Long) {
        currentSettings = currentSettings?.copy(
            timeZone = timeZone
        )
    }
}
