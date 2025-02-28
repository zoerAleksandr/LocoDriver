package com.z_company.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.isEmailValid
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.ServicePhase
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.Back4AppManager
import com.z_company.use_case.AuthUseCase
import com.z_company.use_case.RemoteRouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsViewModel : ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val loginUseCase: LoginUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val back4AppManager: Back4AppManager by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null

    private var loadLoginJob: Job? = null
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
        TimeZoneRussia("Омск (MSK+3, UTC+6),)", oneHourInMillis * 3),
        TimeZoneRussia("Красноярск (MSK+4, UTC+7)", oneHourInMillis * 4),
        TimeZoneRussia("Иркутск (MSK+5, UTC+8)", oneHourInMillis * 5),
        TimeZoneRussia("Якутск (MSK+6, UTC+9)", oneHourInMillis * 6),
        TimeZoneRussia("Владивосток (MSK+7, UTC+10)", oneHourInMillis * 7),
        TimeZoneRussia("Магадан (MSK+8, UTC+11)", oneHourInMillis * 8),
        TimeZoneRussia("Камчатка (MSK+9, UTC+12)", oneHourInMillis * 9),
        TimeZoneRussia("Анадырь (MSK+10, UTC+13)", oneHourInMillis * 10),
    )

    private var currentUser: User?
        get() {
            return _uiState.value.userDetailsState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(userDetailsState = ResultState.Success(value))
            }
        }

    var currentEmail by mutableStateOf("")

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
    }

    fun deleteServicePhase(index: Int) {
        servicePhases.removeAt(index)
    }

    fun selectToUpdateServicePhase(phase: ServicePhase, index: Int) {
        _uiState.update {
            it.copy(
                selectedServicePhase = Pair(phase, index)
            )
        }
        showDialogAddServicePhase(phase)
    }

    fun setEmail(value: String) {
        currentEmail = value
        if (value.isEmailValid()) {
            _uiState.update {
                it.copy(resentVerificationEmailButton = true)
            }
        } else {
            _uiState.update {
                it.copy(resentVerificationEmailButton = false)
            }
        }
    }

    private fun loadPurchasesInfo() {
        viewModelScope.launch {
            val maxEndTime = sharedPreferenceStorage.getSubscriptionExpiration()
            val textEndTime = if (maxEndTime == 0L) {
                ""
            } else {
                ConverterLongToTime.getDateAndTimeStringFormat(maxEndTime)
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
        loadLogin()
        loadMonthList()
        loadPurchasesInfo()
    }

    fun refreshingUserData() {
        _uiState.update {
            it.copy(isRefreshing = true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            loadPurchasesInfo()
            loginUseCase.getUser().collect { resultState ->
                if (resultState is ResultState.Success) {
                    delay(500L)
                    _uiState.update {
                        it.copy(isRefreshing = false)
                    }
                    currentUser = resultState.data
                    currentEmail = currentUser?.email ?: ""
                }
                if (resultState is ResultState.Error) {
                    _uiState.update {
                        it.copy(isRefreshing = false)
                    }
                }
            }
        }
    }

    fun resetSaveState() {
        _uiState.update {
            it.copy(saveSettingsState = null)
        }
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            if (result is ResultState.Success) {
                _uiState.update { state ->
                    state.copy(
                        monthList = result.data.map { it.month }.distinct().sorted(),
                        yearList = result.data.map { it.year }.distinct().sorted()
                    )

                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun loadSettings() {
        loadSettingsJob?.cancel()
        loadSettingsJob = viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                _uiState.update {
                    it.copy(
                        settingDetails = result,
                    )
                }
                if (result is ResultState.Success) {
                    result.data?.let { userSettings ->
                        _uiState.update {
                            it.copy(
                                updateAt = userSettings.updateAt,
                                servicePhases = userSettings.servicePhases.toMutableStateList()
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { result ->
                if (result is ResultState.Success) {
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
    }

    private fun loadLogin() {
        loadLoginJob?.cancel()
        loadLoginJob =
            viewModelScope.launch(Dispatchers.IO) {
                loginUseCase.getUser().collect { resultState ->
                    _uiState.update {
                        it.copy(userDetailsState = resultState)
                    }
                    if (resultState is ResultState.Success) {
                        currentUser = resultState.data
                        currentEmail = currentUser?.email ?: ""
                    }
                }
            }
    }

    fun emailConfirmation() {
        viewModelScope.launch {
            val parseUser = ParseUser.getCurrentUser()
            parseUser.email = currentEmail
            parseUser.username = currentEmail
            parseUser.saveInBackground()
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
                        _uiState.update {
                            it.copy(
                                saveSettingsState = result
                            )
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

    fun logOut() {
        viewModelScope.launch {
            authUseCase.logout().collect { result ->
                if (result is ResultState.Success) {
                    routeUseCase.clearLocalRouteRepository().launchIn(viewModelScope)
                    remoteRouteUseCase.cancelingSync()
                }
                _uiState.update {
                    it.copy(
                        logOutState = result
                    )
                }
            }
        }
    }

    fun onDownloadFromRemote() {
        viewModelScope.launch {
            back4AppManager.loadRouteListFromRemote().collect { loadResult ->
                _uiState.update {
                    it.copy(
                        downloadState = loadResult,
                    )
                }
            }
        }
    }

    fun onUploadToServer() {
        viewModelScope.launch {
            back4AppManager.synchronizedStorage().collect { syncResult ->
                _uiState.update {
                    it.copy(
                        uploadState = syncResult,
                    )
                }
            }
        }
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

    fun changeDefaultLocoType(locoType: LocoType) {
        currentSettings = currentSettings?.copy(
            defaultLocoType = locoType
        )
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

    fun setTimeZone(timeZone: Long) {
        currentSettings = currentSettings?.copy(
            timeZone = timeZone
        )
    }
}

data class TimeZoneRussia(
    val description: String,
    val offsetOfMoscow: Long
)