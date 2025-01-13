package com.z_company.route.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.setWorkTime
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
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability

class HomeViewModel(application: Application) : AndroidViewModel(application = application),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()
    var timeWithoutHoliday by mutableLongStateOf(0L)
        private set

    private var loadRouteJob: Job? = null
    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var setCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null
    private var loadSettingJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _previewRouteUiState = MutableStateFlow(PreviewRouteUiState())
    val previewRouteUiState = _previewRouteUiState.asStateFlow()

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
                    var maxEndTime = 0L
                    var job: Job? = null
                    billingClient.purchases.getPurchases()
                        .addOnSuccessListener { purchases ->
                            viewModelScope.launch {
                                purchases.forEach { purchase ->
                                    job?.cancel()
                                    job = this.launch(Dispatchers.IO) {
                                        if (purchase.purchaseState == PurchaseState.CONFIRMED) {
                                            ruStoreUseCase.getExpiryTimeMillis(
                                                purchase.productId,
                                                purchase.subscriptionToken ?: ""
                                            ).collect { resultState ->
                                                if (resultState is ResultState.Success) {
                                                    if (resultState.data > maxEndTime) {
                                                        maxEndTime = resultState.data
                                                    }
                                                    job?.cancel()
                                                }
                                                if (resultState is ResultState.Error) {
                                                    _uiState.update {
                                                        it.copy(
                                                            restoreSubscriptionState = ResultState.Error(
                                                                resultState.entity
                                                            )
                                                        )
                                                    }
                                                    job?.cancel()
                                                }
                                            }
                                        }
                                    }
                                    job?.join()
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
                            }
                        }
                        .addOnFailureListener {
                            Log.w("ZZZ", "${it.message}")
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
        viewModelScope.launch {
            var routesSize: Int
            withContext(Dispatchers.IO) {
                routesSize = routeUseCase.listRouteWithDeleting().size
            }
            withContext(Dispatchers.Main) {
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
    }

    fun showFormScreenReset() {
        _uiState.update {
            it.copy(
                showNewRouteScreen = false
            )
        }
    }

    var currentUserSetting: UserSettings?
        get() {
            return _uiState.value.settingState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(
                    settingState = ResultState.Success(value)
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
        loadSettingJob =
            viewModelScope.launch {
                settingsUseCase.getCurrentSettings().collect { result ->
                    _uiState.update {
                        it.copy(
                            settingState = result
                        )
                    }
                    if (result is ResultState.Success) {
                        result.data?.let { setting ->
                            _uiState.update {
                                it.copy(
                                    offsetInMoscow = setting.timeZone
                                )
                            }
                        }
                    }
                }
            }
    }

    fun loadRoutes() {
        currentMonthOfYear?.let { monthOfYear ->
            loadRouteJob?.cancel()
            viewModelScope.launch(Dispatchers.IO) {
                currentUserSetting?.let { settings ->
                    loadRouteJob = routeUseCase.listRoutesByMonth(monthOfYear, settings.timeZone)
                        .onEach { result ->
                            _uiState.update {
                                it.copy(routeListState = result)
                            }
                            if (result is ResultState.Success) {
                                val currentTimeInMillis = getInstance().timeInMillis
                                val routeList = if (settings.isConsiderFutureRoute) {
                                    result.data
                                } else {
                                    result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                                }
                                calculationTotalTime(routeList, settings.timeZone)
                                calculationOfTimeWithoutHoliday(routeList, settings.timeZone)
                                calculationOfNightTime(routeList)
                                calculationPassengerTime(routeList, settings.timeZone)
                                calculationHolidayTime(routeList, settings.timeZone)
                            }
                        }.launchIn(this)
                }
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
            val dayOffHours = currentMonthOfYear.getDayoffHours()
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

    private fun calculationPassengerTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update {
            it.copy(
                passengerTimeInRouteList = ResultState.Loading
            )
        }
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val passengerTime = routes.getPassengerTime(monthOfYear, offsetInMoscow)
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

    private fun calculationHolidayTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update {
            it.copy(
                holidayHours = ResultState.Loading
            )
        }
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val holidayTime = routes.getWorkingTimeOnAHoliday(monthOfYear, offsetInMoscow)
                _uiState.update {
                    it.copy(
                        holidayHours = ResultState.Success(holidayTime)
                    )
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


    fun isShowConfirmRemoveRoute(isShow: Boolean) {
        _uiState.update {
            it.copy(
                showConfirmRemoveRoute = isShow
            )
        }
    }

    fun removeRoute(route: Route) {
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

    private fun calculationOfTimeWithoutHoliday(routes: List<Route>, offsetInMoscow: Long) {
        currentMonthOfYear?.let { monthOfYear ->
            timeWithoutHoliday = routes.getWorkTimeWithoutHoliday(monthOfYear, offsetInMoscow)
        }
    }

    private fun calculationTotalTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update {
            it.copy(
                totalTimeWithHoliday = ResultState.Loading
            )
        }
        try {
            val stateSettings = uiState.value.settingState
            if (stateSettings is ResultState.Success) {
                stateSettings.data?.let { settings ->
                    val totalTime = routes.setWorkTime(settings.selectMonthOfYear, offsetInMoscow)
                    _uiState.update {
                        it.copy(
                            totalTimeWithHoliday = ResultState.Success(totalTime)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    totalTimeWithHoliday = ResultState.Error(ErrorEntity(e))
                )
            }
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
                    currentUserSetting = currentUserSetting?.copy(
                        selectMonthOfYear = selectMonthOfYear
                    )
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
        }.launchIn(viewModelScope)
    }

    private fun loadMinTimeRestInRoute() {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(
                            minTimeRest = result.data?.minTimeRestPointOfTurnover,
                            minTimeHomeRest = result.data?.minTimeHomeRest,
                        )
                    }
                }
            }
        }
    }

    fun calculationHomeRest(route: Route?) {
        val routesList = mutableListOf<Route>()
        viewModelScope.launch(Dispatchers.IO) {
            currentMonthOfYear?.let { monthOfYear ->
                currentUserSetting?.let { setting ->
                    this.launch {
                        routeUseCase.listRoutesByMonth(monthOfYear, setting.timeZone)
                            .collect { resultCurrentMonth ->
                                if (resultCurrentMonth is ResultState.Success) {
                                    routesList.addAll(resultCurrentMonth.data)
                                    this.cancel()
                                }
                            }
                    }.join()
                    this.launch {
                        val previousMonthOfYear = if (monthOfYear.month != 0) {
                            monthOfYear.copy(month = monthOfYear.month - 1)
                        } else {
                            monthOfYear.copy(
                                year = monthOfYear.year - 1,
                                month = 11
                            )
                        }
                        routeUseCase.listRoutesByMonth(previousMonthOfYear, setting.timeZone)
                            .collect { resultCurrentMonth ->
                                if (resultCurrentMonth is ResultState.Success) {
                                    routesList.addAll(resultCurrentMonth.data)
                                    this.cancel()
                                }
                            }
                    }.join()
                }
            }
            val sortedRouteList = routesList.sortedBy {
                it.basicData.timeStartWork
            }.distinct()
            val minTimeHomeRest = uiState.value.minTimeHomeRest
            if (routesList.contains(route)) {
                route?.let {
                    val homeRest = route.getHomeRest(
                        parentList = sortedRouteList,
                        minTimeHomeRest = minTimeHomeRest
                    )
                    _previewRouteUiState.update {
                        it.copy(
                            homeRestState = ResultState.Success(homeRest)
                        )
                    }
                }
            } else {
                _previewRouteUiState.update {
                    it.copy(
                        homeRestState = ResultState.Success(null)
                    )
                }
            }
        }
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
        loadMinTimeRestInRoute()
        setCurrentMonth(
            Pair(
                calendar.get(YEAR),
                calendar.get(MONTH)
            )
        )
        checkLoginToAccount()
        sharedPreferenceStorage.enableShowingUpdatePresentation()
    }
}