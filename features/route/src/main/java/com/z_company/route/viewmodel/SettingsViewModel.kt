package com.z_company.route.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.isEmailValid
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null
    private var autoSaveJob: Job? = null
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
            scheduleAutoSave()
        }

    private var servicePhases: SnapshotStateList<ServicePhase>
        get() {
            return _uiState.value.servicePhases ?: mutableStateListOf()
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    servicePhases = value
                )
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

    fun changeTimeFormat(){
        currentSettings?.let {
            currentSettings = it.copy(
                isDecimalTime = !it.isDecimalTime
            )
        }
    }

    fun showDialogAddServicePhase(servicePhase: ServicePhase) {
        _uiState.update {
            it.copy(
                showDialogAddServicePhase = true
            )
        }
    }

    fun hideDialogAddServicePhase() {
        _uiState.update {
            it.copy(
                showDialogAddServicePhase = false
            )
        }
    }

    fun addServicePhase(servicePhase: ServicePhase, index: Int = -1) {
        if (index == -1) {
            servicePhases.add(servicePhase)
        } else {
            servicePhases[index] = servicePhase
        }
        _uiState.update {
            it.copy(
                selectedServicePhase = null
            )
        }
        hideDialogAddServicePhase()
        scheduleAutoSave()
    }

    fun removeStationName(name: String) {
        viewModelScope.launch {
            settingsUseCase.removeStation(name)
        }
    }

    fun deleteServicePhase(phase: ServicePhase) {
        val removed = servicePhases.removeAll { it.id == phase.id }
        if (removed) {
            scheduleAutoSave()
        }
    }

    fun selectToUpdateServicePhase(phase: ServicePhase, index: Int) {
        _uiState.update {
            it.copy(
                selectedServicePhase = Pair(phase, index)
            )
        }
        showDialogAddServicePhase(phase)
    }

    private fun loadPurchasesInfo() {
        viewModelScope.launch {
            val maxEndTime = sharedPreferenceStorage.getSubscriptionExpiration()
            val textEndTime = if (maxEndTime == 0L) {
                ""
            } else {
                uiState.value.dateAndTimeConverter?.getDateMiniAndTime(maxEndTime) ?: ""
            }
            _uiState.update {
                it.copy(
                    purchasesEndTime = ResultState.Success(textEndTime)
                )
            }
        }
    }

    init {
        loadSettings()
        loadMonthList()
        loadPurchasesInfo()
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
            try {
                val result = settingsUseCase.getFlowCurrentSettingsState()
                    .first { it is ResultState.Success }
                _uiState.update {
                    it.copy(
                        settingDetails = result,
                    )
                }
                if (result is ResultState.Success) {
                    result.data?.let { userSettings ->
                        val merged = userSettings.copy(
                            isShowLocoHeating = sharedPreferenceStorage.isShowLocoHeating(),
                            isShowLocoAuxiliary = sharedPreferenceStorage.isShowLocoAuxiliary(),
                            isShowLocoStatistics = sharedPreferenceStorage.isShowLocoStatistics(),
                            isShowLocoNorma = sharedPreferenceStorage.isShowLocoNorma(),
                            isShowOtherCurrent = sharedPreferenceStorage.isShowOtherCurrent()
                        )
                        _uiState.update {
                            it.copy(
                                settingDetails = ResultState.Success(merged),
                                dateAndTimeConverter = DateAndTimeConverter(merged),
                                updateAt = merged.updateAt,
                                servicePhases = merged.servicePhases.toMutableStateList()
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "loadSettings")
            }
        }
        viewModelScope.launch {
            try {
                calendarUseCase.loadFlowMonthOfYearListState().collect { result ->
                    currentSettings?.let { setting ->
                        _uiState.update {
                            it.copy(
                                calendarState = ResultState.Success(setting.selectMonthOfYear)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SettingsViewModel", "loadCalendar")
            }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(300)
            saveSettingsQuietly()
        }
    }

    private fun saveSettingsQuietly() {
        val state = uiState.value.settingDetails
        if (state is ResultState.Success) {
            state.data?.let { settings ->
                settings.servicePhases = servicePhases.toMutableList()
                saveSettingsJob?.cancel()
                saveSettingsJob = viewModelScope.launch {
                    settingsUseCase.saveSetting(settings).collect { }
                }
            }
        }
    }

    fun saveServicePhases() {
        scheduleAutoSave()
    }

    fun changeMinTimeRest(time: Long) {
        currentSettings = currentSettings?.copy(
            minTimeRestPointOfTurnover = time
        )
    }

    fun resetUploadState() {
        _uiState.update {
            it.copy(
                uploadState = null
            )
        }
    }

    fun resetDownloadState() {
        _uiState.update {
            it.copy(
                downloadState = null
            )
        }
    }

    fun changeDefaultWorkTime(timeInMillis: Long) {
        currentSettings = currentSettings?.copy(
            defaultWorkTime = timeInMillis
        )
    }

    fun changeDefaultLocoType() {
        currentSettings?.let { settings ->
            currentSettings = when (settings.defaultLocoType){
                LocoType.ELECTRIC -> {
                    settings.copy(
                        defaultLocoType = LocoType.DIESEL
                    )
                }

                LocoType.DIESEL -> {
                    settings.copy(
                        defaultLocoType = LocoType.ELECTRIC
                    )
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

    fun changeShowLocoHeating(value: Boolean) {
        sharedPreferenceStorage.setShowLocoHeating(value)
        currentSettings = currentSettings?.copy(isShowLocoHeating = value)
    }

    fun changeShowLocoAuxiliary(value: Boolean) {
        sharedPreferenceStorage.setShowLocoAuxiliary(value)
        currentSettings = currentSettings?.copy(isShowLocoAuxiliary = value)
    }

    fun changeShowLocoStatistics(value: Boolean) {
        sharedPreferenceStorage.setShowLocoStatistics(value)
        currentSettings = currentSettings?.copy(isShowLocoStatistics = value)
    }

    fun changeShowLocoNorma(value: Boolean) {
        sharedPreferenceStorage.setShowLocoNorma(value)
        currentSettings = currentSettings?.copy(isShowLocoNorma = value)
    }

    fun changeShowOtherCurrent(value: Boolean) {
        sharedPreferenceStorage.setShowOtherCurrent(value)
        currentSettings = currentSettings?.copy(isShowOtherCurrent = value)
    }

    fun setTimeZone(timeZone: Long) {
        currentSettings = currentSettings?.copy(
            timeZone = timeZone
        )
    }

    private val _passenger12hOption = MutableStateFlow(resolvePassenger12hOption())
    val passenger12hOption = _passenger12hOption.asStateFlow()

    private fun resolvePassenger12hOption(): Passenger12hOption {
        val dontAsk = sharedPreferenceStorage.isPassenger12hDontAskAgain()
        if (!dontAsk) return Passenger12hOption.ALWAYS_ASK
        return if (sharedPreferenceStorage.isPassenger12hAutoAccepted()) {
            Passenger12hOption.AUTO
        } else {
            Passenger12hOption.NEVER_ASK
        }
    }

    fun setPassenger12hOption(option: Passenger12hOption) {
        when (option) {
            Passenger12hOption.ALWAYS_ASK -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(false)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(false)
            }
            Passenger12hOption.AUTO -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(true)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(true)
            }
            Passenger12hOption.NEVER_ASK -> {
                sharedPreferenceStorage.setPassenger12hDontAskAgain(true)
                sharedPreferenceStorage.setPassenger12hAutoAccepted(false)
            }
        }
        _passenger12hOption.value = option
    }
}

enum class Passenger12hOption(val label: String, val hint: String) {
    ALWAYS_ASK(
        "Всегда спрашивать",
        "При работе свыше 12-ти часов будет предложено создать следование пассажиром."
    ),
    AUTO(
        "Автоматически",
        "При включении время свыше 12 часов будет автоматически записано как следование пассажиром."
    ),
    NEVER_ASK(
        "Никогда не спрашивать",
        "Шторка не будет показана."
    )
}

data class TimeZoneRussia(
    val description: String,
    val offsetOfMoscow: Long
)