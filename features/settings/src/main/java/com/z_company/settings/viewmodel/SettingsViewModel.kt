package com.z_company.settings.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parse.ParseUser
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.isEmailValid
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.MonthOfYear
import com.z_company.use_case.LoginUseCase
import com.z_company.domain.entities.User
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.route.LocoType
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.Back4AppManager
import com.z_company.route.extention.getEndTimeSubscription
import com.z_company.use_case.AuthUseCase
import com.z_company.use_case.RemoteRouteUseCase
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
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.billingclient.RuStoreBillingClient

class SettingsViewModel : ViewModel(), KoinComponent {
    private val authUseCase: AuthUseCase by inject()
    private val loginUseCase: LoginUseCase by inject()
    private val remoteRouteUseCase: RemoteRouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val back4AppManager: Back4AppManager by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    private var loadSettingsJob: Job? = null
    private var saveSettingsJob: Job? = null

    private var loadLoginJob: Job? = null
    private var setCalendarJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null


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
            withContext(Dispatchers.IO) {
                try {
                    val purchases = billingClient.purchases.getPurchases().await()
                    var maxEndTime = 0L
                    purchases.forEach { purchase ->
                        val purchaseEndTime =
                            purchase.getEndTimeSubscription(billingClient).first()
                        if (purchaseEndTime > maxEndTime) {
                            maxEndTime = purchaseEndTime
                        }
                    }
                    sharedPreferenceStorage.setSubscriptionExpiration(maxEndTime)
                    val textEndTime = if (maxEndTime == 0L) {
                        ""
                    } else {
                        ConverterLongToTime.getDateAndTimeStringFormat(maxEndTime)
                    }

                    viewModelScope.launch {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    purchasesEndTime = ResultState.Success(textEndTime)
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    viewModelScope.launch {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    purchasesEndTime = ResultState.Error(ErrorEntity())
                                )
                            }
                        }
                    }
                }
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
                if (resultState is ResultState.Error){
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

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                result.data.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    currentSettings = currentSettings?.copy(
                        selectMonthOfYear = selectMonthOfYear
                    )
                    saveCurrentMonthInLocal(selectMonthOfYear)
                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun saveCurrentMonthInLocal(monthOfYear: MonthOfYear) {
        saveCurrentMonthJob?.cancel()
        saveCurrentMonthJob =
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).launchIn(viewModelScope)
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
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
            settingsUseCase.getCurrentSettings().collect { result ->
                _uiState.update {
                    it.copy(
                        settingDetails = result,
                    )
                }
                if (result is ResultState.Success) {
                    result.data?.let { userSettings ->
                        _uiState.update {
                            it.copy(
                                updateAt = ResultState.Success(userSettings.updateAt),
                            )
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            calendarUseCase.loadMonthOfYearList().collect { result ->
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

    fun onSync() {
        viewModelScope.launch {
            back4AppManager.synchronizedStorage().collect { result ->
                _uiState.update {
                    it.copy(
                        updateRepositoryState = result
                    )
                }
            }
        }
    }

    fun resetRepositoryState() {
        _uiState.update {
            it.copy(
                updateRepositoryState = ResultState.Success(Unit)
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
}