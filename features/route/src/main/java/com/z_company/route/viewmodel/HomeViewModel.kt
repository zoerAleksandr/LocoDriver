package com.z_company.route.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getDayOffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTotalWorkTime
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.Calendar.getInstance
import com.z_company.route.extention.getEndTimeSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability

class HomeViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    private val billingClient: RuStoreBillingClient by inject()
    var totalTime by mutableLongStateOf(0L)
        private set

    private var loadRouteJob: Job? = null
    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var setCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null
    private var loadSettingJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _checkPurchasesEvent = MutableSharedFlow<StartPurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val checkPurchasesEvent = _checkPurchasesEvent.asSharedFlow()

    private val _alertBeforePurchasesEvent = MutableSharedFlow<AlertBeforePurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val alertBeforePurchasesEvent = _alertBeforePurchasesEvent.asSharedFlow()

    fun checkPurchasesAvailability(context: Context) {
        RuStoreBillingClient.checkPurchasesAvailability(context)
            .addOnSuccessListener { result ->
                _uiState.update {
                    it.copy(
                        isLoadingStateAddButton = false
                    )
                }
                _checkPurchasesEvent.tryEmit(StartPurchasesEvent.PurchasesAvailability(result))
            }
            .addOnFailureListener { throwable ->
                _uiState.update {
                    it.copy(
                        isLoadingStateAddButton = false
                    )
                }
                _checkPurchasesEvent.tryEmit(StartPurchasesEvent.Error(throwable))
            }
    }

    fun restorePurchases() {
        _uiState.update {
            it.copy(
                restoreSubscriptionState = ResultState.Loading
            )
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val currentTimeInMillis = getInstance().timeInMillis
                    val purchases = billingClient.purchases.getPurchases().await()
                    var maxEndTime = 0L
                    purchases.forEach { purchase ->
                        val purchaseEndTime =
                            purchase.getEndTimeSubscription(billingClient).first()
                        if (purchaseEndTime > maxEndTime) {
                            maxEndTime = purchaseEndTime
                        }
                    }
                    if (maxEndTime > currentTimeInMillis) {
                        sharedPreferenceStorage.setSubscriptionExpiration(maxEndTime)
                        _uiState.update {
                            it.copy(
                                restoreSubscriptionState = ResultState.Success("Покупки восстановлены")
                            )
                        }
                    }
                    if (maxEndTime < currentTimeInMillis) {
                        _uiState.update {
                            it.copy(
                                restoreSubscriptionState = ResultState.Success("Действующих подписок не найдено")
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            restoreSubscriptionState = ResultState.Error(ErrorEntity())
                        )
                    }
                }
            }
        }
    }

    fun resetSubscriptionState() {
        _uiState.update {
            it.copy(
                restoreSubscriptionState = null
            )
        }
    }

    fun newRouteClick() {
        _uiState.update {
            it.copy(
                isLoadingStateAddButton = true
            )
        }
        val currentTime = getInstance().timeInMillis
        val endTimeSubscription = sharedPreferenceStorage.getSubscriptionExpiration()
        val listState = uiState.value.routeListState

        if (listState is ResultState.Success) {
            val routesSize = listState.data.size
            if (endTimeSubscription < currentTime && endTimeSubscription != 0L) {
                _alertBeforePurchasesEvent.tryEmit(
                    AlertBeforePurchasesEvent.ShowDialogNeedSubscribe
                )
                _uiState.update {
                    it.copy(
                        isLoadingStateAddButton = false
                    )
                }
            } else if (routesSize >= 10 && endTimeSubscription == 0L) {
                _alertBeforePurchasesEvent.tryEmit(
                    AlertBeforePurchasesEvent.ShowDialogNeedSubscribe
                )
                _uiState.update {
                    it.copy(
                        isLoadingStateAddButton = false
                    )
                }
            } else if (routesSize < 10 && endTimeSubscription == 0L) {
                _alertBeforePurchasesEvent.tryEmit(
                    AlertBeforePurchasesEvent.ShowDialogAlertSubscribe
                )
                _uiState.update {
                    it.copy(
                        isLoadingStateAddButton = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showNewRouteScreen = true,
                        isLoadingStateAddButton = false
                    )
                }
            }
        }
    }

    fun showFormScreenReset() {
        _uiState.update {
            it.copy(
                showNewRouteScreen = false
            )
        }
    }

    var currentMonthOfYear: MonthOfYear?
        get() {
            return _uiState.value.monthSelected.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(monthSelected = ResultState.Success(value))
            }
            value?.let {
                getDayOffTime(value)
            }
        }

    private fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = settingsUseCase.getCurrentSettings().onEach { result ->
            _uiState.update {
                it.copy(
                    settingState = result
                )
            }
            if (result is ResultState.Success) {
                currentMonthOfYear = result.data?.selectMonthOfYear
            }
        }.launchIn(viewModelScope)
    }

    fun loadRoutes() {
        currentMonthOfYear?.let { monthOfYear ->
            loadRouteJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                loadRouteJob = routeUseCase.listRoutesByMonth(monthOfYear).onEach { result ->
                    _uiState.update {
                        it.copy(routeListState = result)
                    }
                    if (result is ResultState.Success) {
                        calculationOfTotalTime(result.data)
                        calculationOfNightTime(result.data)
                        calculationPassengerTime(result.data)
                    }
                }.launchIn(this)
            }
        }
    }

    private fun getDayOffTime(currentMonthOfYear: MonthOfYear) {
        try {
            _uiState.update {
                it.copy(
                    dayOffHours = ResultState.Loading
                )
            }
            val dayOffHours = currentMonthOfYear.getDayOffHours()
            _uiState.update {
                it.copy(
                    dayOffHours = ResultState.Success(dayOffHours)
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    dayOffHours = ResultState.Error(ErrorEntity(e))
                )
            }
        }
    }

    private fun calculationPassengerTime(routes: List<Route>) {
        _uiState.update {
            it.copy(
                passengerTimeInRouteList = ResultState.Loading
            )
        }
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val passengerTime = routes.getPassengerTime(monthOfYear)
                _uiState.update {
                    it.copy(
                        passengerTimeInRouteList = ResultState.Success(passengerTime)
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    passengerTimeInRouteList = ResultState.Error(ErrorEntity(e))
                )
            }
        }
    }

    private fun calculationOfNightTime(routes: List<Route>) {
        _uiState.update {
            it.copy(
                nightTimeInRouteList = ResultState.Loading
            )
        }
        try {
            val stateSettings = uiState.value.settingState
            if (stateSettings is ResultState.Success) {
                stateSettings.data?.let { settings ->
                    val nightTimeState = routes.getNightTime(settings)
                    _uiState.update {
                        it.copy(
                            nightTimeInRouteList = ResultState.Success(nightTimeState)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    nightTimeInRouteList = ResultState.Error(ErrorEntity(e))
                )
            }
        }
    }

    fun remove(route: Route) {
        removeRouteJob?.cancel()
        removeRouteJob = routeUseCase.markAsRemoved(route).onEach { result ->
            _uiState.update {
                it.copy(removeRouteState = result)
            }
        }.launchIn(viewModelScope)
    }

    fun resetRemoveRouteState() {
        _uiState.update {
            it.copy(removeRouteState = null)
        }
    }

    private fun calculationOfTotalTime(routes: List<Route>) {
        currentMonthOfYear?.let { monthOfYear ->
            totalTime = routes.getTotalWorkTime(monthOfYear)
        }
    }

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                result.data.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    currentMonthOfYear = selectMonthOfYear
                    loadRoutes()
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

    private fun loadMinTimeRestInRoute() {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(
                            minTimeRest = result.data?.minTimeRest,
                            minTimeHomeRest = result.data?.minTimeHomeRest,
                        )
                    }
                }
            }
        }
    }

    fun calculationHomeRest(route: Route?): Long? {
        val minTimeHomeRest = uiState.value.minTimeHomeRest
        uiState.value.routeListState.let { listState ->
            if (listState is ResultState.Success) {
                if (listState.data.contains(route)) {
                    route?.let {
                        return route.getHomeRest(
                            parentList = listState.data,
                            minTimeHomeRest = minTimeHomeRest
                        )
                    }
                }
            }
        }
        return null
    }

    private fun checkLoginToAccount() {
        if (sharedPreferenceStorage.tokenIsFirstAppEntry()) {
            _uiState.update {
                it.copy(
                    showFirstEntryToAccountDialog = true
                )
            }
        }
    }

    fun disableFirstEntryToAccountDialog() {
        _uiState.update {
            it.copy(
                showFirstEntryToAccountDialog = false
            )
        }
        sharedPreferenceStorage.setTokenIsFirstAppEntry(false)
    }

    init {
        val calendar = getInstance()
        loadSetting()
        loadMonthList()
        setCurrentMonth(
            Pair(
                calendar.get(YEAR),
                calendar.get(MONTH)
            )
        )
        loadMinTimeRestInRoute()
        checkLoginToAccount()
    }
}