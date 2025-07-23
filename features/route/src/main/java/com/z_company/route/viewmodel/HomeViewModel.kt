package com.z_company.route.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInHeavyTrain
import com.z_company.domain.entities.route.UtilsForEntities.getTimeInServicePhase
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.timeInLongInPeriod
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.toIntOrZero
import com.z_company.repository.Back4AppManager
import com.z_company.repository.ShareManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar.getInstance
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability
import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
import java.util.Calendar
import java.util.TimeZone

class HomeViewModel(application: Application) : AndroidViewModel(application = application),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()
    private val back4AppManager: Back4AppManager by inject()
    private val ruStoreAppUpdateManager: RuStoreAppUpdateManager by inject()
    private val shareManager: ShareManager by inject()

    var timeWithoutHoliday by mutableLongStateOf(0L)
        private set

    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var setCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null
    private var loadSettingJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var salarySetting: SalarySetting

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

    private val _updateEvents = MutableSharedFlow<UpdateEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val updateEvents = _updateEvents.asSharedFlow()

    override fun onCleared() {
        super.onCleared()
        ruStoreAppUpdateManager.unregisterListener(installStateUpdateListener)
    }

    private fun initUpdateManager() {
        ruStoreAppUpdateManager.getAppUpdateInfo()
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
                    ruStoreAppUpdateManager.registerListener(installStateUpdateListener)
                    ruStoreAppUpdateManager
                        .startUpdateFlow(appUpdateInfo, AppUpdateOptions.Builder().build())
                        .addOnSuccessListener { resultCode ->
                            when (resultCode) {
                                Activity.RESULT_CANCELED -> {
                                    // Пользователь отказался от скачивания
                                }

                                Activity.RESULT_OK -> {
                                    // Пользователь согласился на скачивание
                                }
                            }


                        }
                        .addOnFailureListener { throwable ->
                            Log.e("ZZZ", "startUpdateFlow error", throwable)
                        }
                }
            }
            .addOnFailureListener { throwable ->
                Log.e("ZZZ", "getAppUpdateInfo error", throwable)
            }
    }

    private val installStateUpdateListener = InstallStateUpdateListener { installState ->
        when (installState.installStatus) {
            InstallStatus.DOWNLOADED -> {
                _updateEvents.tryEmit(UpdateEvent.UpdateCompleted)
            }

            InstallStatus.DOWNLOADING -> {
                val totalBytes = installState.totalBytesToDownload
                val bytesDownloaded = installState.bytesDownloaded
                // Здесь можно отобразить прогресс скачивания
            }

            InstallStatus.FAILED -> {
                Log.e("ZZZ", "Downloading error")
            }
        }
    }

    fun completeUpdateRequested() {
        ruStoreAppUpdateManager.completeUpdate(
            AppUpdateOptions.Builder().appUpdateType(
                AppUpdateType.FLEXIBLE
            ).build()
        )
            .addOnFailureListener { throwable ->
                Log.e("ZZZ", "completeUpdate error", throwable)
            }
    }

    fun setFavoriteRoute(route: Route) {
        viewModelScope.launch {
            routeUseCase.setFavoriteRoute(
                routeId = route.basicData.id,
                isFavorite = !route.basicData.isFavorite
            ).collect {}
        }
    }

    fun isHeavyTrains(route: Route): Boolean {
        val surchargeListSorted = salarySetting.surchargeHeavyTrainsList.sortedBy {
            it.weight
        }
        val timeList: MutableList<Long> = mutableListOf()
        surchargeListSorted.forEachIndexed { index, _ ->
            var totalTimeHeavyTrain = 0L
            totalTimeHeavyTrain += route.getTimeInHeavyTrain(
                surchargeListSorted.map { it.weight.toIntOrZero() },
                index
            )

            timeList.add(totalTimeHeavyTrain)
        }
        return timeList.sum() > 0
    }

    fun isExtendedServicePhaseTrains(route: Route): Boolean {
        val phaseList =
            salarySetting.surchargeExtendedServicePhaseList.sortedBy {
                it.distance
            }

        val timeList: MutableList<Long> = mutableListOf()
        phaseList.forEachIndexed { index, _ ->
            var totalTimeInServicePhase = 0L
            val timeInRoute = route.getTimeInServicePhase(
                phaseList.map { it.distance.toIntOrNull() ?: 0 },
                index
            )
            totalTimeInServicePhase += timeInRoute

            timeList.add(totalTimeInServicePhase)
        }
        timeList
        return timeList.sum() > 0
    }

    fun isHolidayTimeInRoute(route: Route): Boolean {
        var holidayTime = 0L
        currentMonthOfYear?.let { monthOfYear ->
            currentUserSetting?.let { userSetting ->
                val timeZone = settingsUseCase.getTimeZone(userSetting.timeZone)
                val holidayList = monthOfYear.days.filter { it.tag == TagForDay.HOLIDAY }
                if (holidayList.isNotEmpty()) {
                    holidayList.forEach { day ->
                        val startHolidayInLong =
                            getInstance(TimeZone.getTimeZone(timeZone)).also {
                                it.set(Calendar.YEAR, monthOfYear.year)
                                it.set(Calendar.MONTH, monthOfYear.month)
                                it.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
                                it.set(Calendar.HOUR_OF_DAY, 0)
                                it.set(Calendar.MINUTE, 0)
                                it.set(Calendar.SECOND, 0)
                                it.set(Calendar.MILLISECOND, 0)
                            }.timeInMillis

                        val endHoliday = getInstance(TimeZone.getTimeZone(timeZone)).also {
                            it.set(Calendar.YEAR, monthOfYear.year)
                            it.set(Calendar.MONTH, monthOfYear.month)
                            it.set(Calendar.DAY_OF_MONTH, day.dayOfMonth)
                            it.set(Calendar.HOUR_OF_DAY, 0)
                            it.set(Calendar.MINUTE, 0)
                            it.set(Calendar.SECOND, 0)
                            it.set(Calendar.MILLISECOND, 0)
                        }
                        endHoliday.add(Calendar.DATE, 1)

                        val endHolidayInLong = endHoliday.timeInMillis

                        route.timeInLongInPeriod(
                            startDate = startHolidayInLong - userSetting.timeZone,
                            endDate = endHolidayInLong - userSetting.timeZone
                        )?.let { timeInPeriod ->
                            if (timeInPeriod > 0) {
                                holidayTime += timeInPeriod
                            }
                        }
                    }
                }
            }
        }
        return holidayTime > 0
    }

    fun checkPurchasesAvailability() {
        RuStoreBillingClient.checkPurchasesAvailability()
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
                restoreSubscriptionState = ResultState.Loading()
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
        val gracePeriod = 24 * 3_600_000 // 1 day grace period
        val endTimeSubscription = sharedPreferenceStorage.getSubscriptionExpiration() + gracePeriod
        viewModelScope.launch {
            var routesSize: Int
            withContext(Dispatchers.IO) {
                routesSize = routeUseCase.listRouteWithDeleting().size
            }
            withContext(Dispatchers.Main) {
                if (endTimeSubscription < currentTime && sharedPreferenceStorage.getSubscriptionExpiration() != 0L) {
                    _alertBeforePurchasesEvent.tryEmit(
                        AlertBeforePurchasesEvent.ShowDialogNeedSubscribe
                    )
                    _uiState.update {
                        it.copy(
                            isLoadingStateAddButton = false
                        )
                    }
                } else if (routesSize > 10 && sharedPreferenceStorage.getSubscriptionExpiration() == 0L) {
                    _alertBeforePurchasesEvent.tryEmit(
                        AlertBeforePurchasesEvent.ShowDialogNeedSubscribe
                    )
                    _uiState.update {
                        it.copy(
                            isLoadingStateAddButton = false
                        )
                    }
                } else if (routesSize <= 10 && sharedPreferenceStorage.getSubscriptionExpiration() == 0L) {
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

    private var currentUserSetting: UserSettings?
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

    fun loadSetting() {
        loadSettingJob?.cancel()
        loadSettingJob = viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
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
                        currentMonthOfYear = setting.selectMonthOfYear
                        loadMinTimeRestInRoute()
                        loadMonthList()
                        loadRoutes(setting)
                    }
                    loadSettingJob?.cancel()
                }
            }
        }
    }

    private fun loadRoutes(settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.listRoutesByMonth(settings.selectMonthOfYear, settings.timeZone)
                .collect { result ->
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(routeListState = result)
                        }
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
                        calculationOfNightTime(routeList, settings)
                        calculationPassengerTime(routeList, settings.timeZone)
                        calculationHolidayTime(routeList, settings.timeZone)
                    }
                }
        }
    }

    private fun getDayOffTime(currentMonthOfYear: MonthOfYear) {
        try {
            _uiState.update {
                it.copy(
                    dayOffHours = ResultState.Loading()
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
                passengerTimeInRouteList = ResultState.Loading()
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

    private fun calculationOfNightTime(routes: List<Route>, settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        nightTimeInRouteList = ResultState.Loading()
                    )
                }
            }
            try {
                val nightTimeState = routes.getNightTime(settings)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            nightTimeInRouteList = ResultState.Success(nightTimeState)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            nightTimeInRouteList = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
            }
        }
    }

    private fun calculationHolidayTime(routes: List<Route>, offsetInMoscow: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        holidayHours = ResultState.Loading()
                    )
                }
            }
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    val holidayTime =
                        routes.getWorkingTimeOnAHoliday(monthOfYear, offsetInMoscow).first()
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                holidayHours = ResultState.Success(holidayTime)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            nightTimeInRouteList = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
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

    private suspend fun calculationOfTimeWithoutHoliday(routes: List<Route>, offsetInMoscow: Long) {
        currentMonthOfYear?.let { monthOfYear ->
            timeWithoutHoliday = routes.getWorkTimeWithoutHoliday(monthOfYear, offsetInMoscow)
        }
    }

    fun syncRoute(route: Route) {
        viewModelScope.launch {
            back4AppManager.saveOneRouteToRemoteStorage(route).collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(
                            syncRouteState = ResultState.Success("Маршрут сохранен в облаке")
                        )
                    }
                }
                if (result is ResultState.Error) {
                    _uiState.update {
                        it.copy(
                            syncRouteState = ResultState.Success("Ошибка сохранения ${result.entity.message}")
                        )
                    }
                }
            }
        }
    }

    fun resetSyncRouteState() {
        _uiState.update {
            it.copy(
                syncRouteState = null
            )
        }
    }

    private fun calculationTotalTime(routes: List<Route>, offsetInMoscow: Long) {
        _uiState.update {
            it.copy(
                totalTimeWithHoliday = ResultState.Loading()
            )
        }
        try {
            val stateSettings = uiState.value.settingState
            if (stateSettings is ResultState.Success) {
                stateSettings.data?.let { settings ->
                    val totalTime = routes.getWorkTime(settings.selectMonthOfYear, offsetInMoscow)
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
        setCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            if (result is ResultState.Success) {
                result.data.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    currentMonthOfYear = selectMonthOfYear
                    currentUserSetting = currentUserSetting?.copy(
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
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).onEach {
                if (it is ResultState.Success) {
                    loadSetting()
                    saveCurrentMonthJob?.cancel()
                }
            }.launchIn(viewModelScope)
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
        }.launchIn(viewModelScope)
    }

    private fun loadMinTimeRestInRoute() {
        viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collect { result ->
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

    // ДЛЯ ТОГО, ЧТОБЫ СФОРМИРОВАЛИСЬ СПИСКИ ДЛЯ DROPDOWN MENU СЕРИЙ ЛОКОМОТИВОВ И СТАНЦИЙ
    private fun initListStationAndLocomotiveSeries() {
        if (!sharedPreferenceStorage.tokenIsLoadStationAndLocomotiveSeries()) {
            viewModelScope.launch(
                Dispatchers.IO
            ) {
                val seriesList = mutableListOf<String>()
                val stationList = mutableListOf<String>()

                val routes = routeUseCase.getListRoutes()

                routes.forEach { route ->
                    route.locomotives.forEach { locomotive ->
                        locomotive.series?.let { series ->
                            seriesList.add(series)
                        }
                    }
                    route.trains.forEach { train ->
                        train.stations.forEach { station ->
                            station.stationName?.let { name ->
                                stationList.add(name)
                            }
                        }
                    }
                }
                this.launch {
                    settingsUseCase.setLocomotiveSeriesList(seriesList)
                }
                this.launch {
                    settingsUseCase.setStations(stationList)
                }
                sharedPreferenceStorage.setTokenIsLoadStationAndLocomotiveSeries(true)
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

    fun getUriToRoute(route: Route): Intent {
        return shareManager.createShareIntent(route)
    }

    fun getTextWorkTime(route: Route): String {
        return currentUserSetting?.timeZone?.let { timeZone ->
            val startWork =
                DateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)

            val isDifference = DateAndTimeConverter.isDifferenceDate(
                first = route.basicData.timeStartWork,
                second = route.basicData.timeEndWork
            )

            val endWork = if (isDifference) {
                DateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeEndWork)
            } else {
                DateAndTimeConverter.getTime(route.basicData.timeEndWork)
            }
            return "$startWork - $endWork"
        } ?: ""
    }

    fun getDateAndTimeText(long: Long?): String {
        return DateAndTimeConverter.getDateMiniAndTime(value = long)
    }

    private fun loadSalarySetting() {
        viewModelScope.launch {
            salarySetting = salarySettingUseCase.salarySettingFlow().first()
        }
    }

    init {
        loadSalarySetting()
        loadSetting()
        checkLoginToAccount()
        initListStationAndLocomotiveSeries()
        sharedPreferenceStorage.enableShowingUpdatePresentation()
        initUpdateManager()
    }
}