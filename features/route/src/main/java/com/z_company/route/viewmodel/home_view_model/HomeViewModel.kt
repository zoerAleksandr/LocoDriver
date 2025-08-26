package com.z_company.route.viewmodel.home_view_model

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getHomeRest
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTimePassengerTrain
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.repository.Back4AppManager
import com.z_company.repository.ShareManager
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.SalaryCalculationHelper
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability
import java.util.Calendar
import java.util.Calendar.getInstance
import java.util.TimeZone

class HomeViewModel(application: Application) : AndroidViewModel(application = application),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
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

    var currentRoute by mutableStateOf<Route?>(null)

    private val _saveTimeEvent = MutableSharedFlow<String>(replay = 0)
    val saveTimeEvent: SharedFlow<String> = _saveTimeEvent.asSharedFlow()

    private val _workTimeInCurrentRoute = MutableSharedFlow<Long>(replay = 1)
    val workTimeInCurrentRoute = _workTimeInCurrentRoute.asSharedFlow()

    private var removeRouteJob: Job? = null
    private var loadCalendarJob: Job? = null
    private var setCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null

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
                if (appUpdateInfo.updateAvailability == UpdateAvailability.Companion.UPDATE_AVAILABLE) {
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
            InstallStatus.Companion.DOWNLOADED -> {
                _updateEvents.tryEmit(UpdateEvent.UpdateCompleted)
            }

            InstallStatus.Companion.DOWNLOADING -> {
                val totalBytes = installState.totalBytesToDownload
                val bytesDownloaded = installState.bytesDownloaded
                // Здесь можно отобразить прогресс скачивания
            }

            InstallStatus.Companion.FAILED -> {
                Log.e("ZZZ", "Downloading error")
            }
        }
    }

    fun completeUpdateRequested() {
        ruStoreAppUpdateManager.completeUpdate(
            AppUpdateOptions.Builder().appUpdateType(
                AppUpdateType.Companion.FLEXIBLE
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

    fun checkPurchasesAvailability() {
        RuStoreBillingClient.Companion.checkPurchasesAvailability()
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

    var timerJob: Job? = null

    fun workTimer(startWork: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val timeZone = uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val currentTimeCalendar = getInstance(TimeZone.getTimeZone(timeZone))
            val currentTime: Long = currentTimeCalendar.timeInMillis
            val startWorkTime: Long = startWork

            val second = currentTimeCalendar
                .get(Calendar.SECOND)

            val remainingSecond = 60 - second

            val firstIncreasingTimeInMillis = remainingSecond * 1000L
            var difference = currentTime - startWorkTime

            _workTimeInCurrentRoute.tryEmit(difference)
            delay(firstIncreasingTimeInMillis)
            while (currentRoute != null) {
                difference += 60_000L
                _workTimeInCurrentRoute.tryEmit(difference)
                delay(60_000L)
            }
        }
    }

    // Determine what will be filled next:
    // returns true if next fill is timeDeparture, false if next fill is timeArrival
    private fun nextIsDeparture(train: Train?): Boolean {
        if (train == null) return false // no train -> we'll fill arrival first
        val stations = train.stations
        if (stations.isEmpty()) return true // create first station -> fill arrival
        if (stations.size == 1 && stations.first().timeDeparture == null) return true

        val last = stations.last()
        return when {
            last.timeArrival == null -> false // next is arrival
            last.timeDeparture == null -> true // next is departure
            else -> false // both present -> new station -> arrival
        }
    }

    fun isNextDeparture(): Boolean {
        val lastTrain = currentRoute?.trains?.lastOrNull()
        return nextIsDeparture(lastTrain)
    }

    fun onGoClicked() {
        viewModelScope.launch {
            val current = currentRoute?.trains?.lastOrNull()
            val timeZone = uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val currentTimeCalendar = getInstance(TimeZone.getTimeZone(timeZone))

            val now = currentTimeCalendar.timeInMillis

            val updatedTrain = withContext(Dispatchers.Default) {
                if (current == null) {
                    val firstStation = Station(timeArrival = now)
                    Train(
                        stations = mutableListOf(firstStation)
                    )
                } else {
                    val stations = current.stations.toMutableList()
                    if (stations.isEmpty()) {
                        stations.add(
                            Station(
                                timeDeparture = now,
                            )
                        )
                    } else if (stations.size == 1 && stations.first().timeDeparture == null) {
                        stations[0] = stations[0].copy(timeDeparture = now)
                    } else {
                        val last = stations.last()
                        when {
                            last.timeArrival == null -> {
                                stations[stations.lastIndex] = last.copy(timeArrival = now)
                            }

                            last.timeDeparture == null -> {
                                stations[stations.lastIndex] = last.copy(timeDeparture = now)
                            }

                            else -> {
                                stations.add(
                                    Station(
                                        timeArrival = now,
                                    )
                                )
                            }
                        }
                    }
                    current.copy(stations = stations)
                }
            }

            // persist in DB
            try {
                val text =
                    if (isNextDeparture()) "Сохранено время отправления" else "Сохранено время прибытия"
                val timeText = uiState.value.dateAndTimeConverter?.getTime(now) ?: ""
                val resultText = "$text $timeText"
                trainUseCase.updateTrain(updatedTrain).collect { saveResult ->
                    if (saveResult is ResultState.Success) {
                        _saveTimeEvent.emit(resultText)
                    }
                }
            } catch (e: Exception) {
                // optionally handle error: add error event flow, show snackbar with error, etc.
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

    private fun calculationOfSingleLocomotiveTime(routes: List<Route>) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        singleLocomotiveTimeState = ResultState.Loading()
                    )
                }
            }
            try {
                val timeState = routes.getSingleLocomotiveTime()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            singleLocomotiveTimeState = ResultState.Success(timeState)
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            singleLocomotiveTimeState = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
            }
        }
    }

    private fun calculationOfLongDistanceTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        longDistanceTrainsTime = ResultState.Loading()
                    )
                }
            }
            try {
                val timeState = salaryCalculationHelper.getTimeLongDistanceTrainFlow().first()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            longDistanceTrainsTime = ResultState.Success(timeState)
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            longDistanceTrainsTime = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
            }
        }
    }

    private fun calculationOfExtendedServicePhaseTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        extendedServicePhaseTime = ResultState.Loading()
                    )
                }
            }
            try {
                val timeState =
                    salaryCalculationHelper.getTimeListSurchargeServicePhaseFlow().first().sum()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            extendedServicePhaseTime = ResultState.Success(timeState)
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            extendedServicePhaseTime = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
            }
        }
    }

    private fun calculationOfOnePersonOperationTime(
        routes: List<Route>, userSettings: UserSettings
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        onePersonOperationTime = ResultState.Loading()
                    )
                }
            }
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    val passengerTime = routes.getOnePersonOperationTimePassengerTrain(
                        monthOfYear, userSettings.timeZone
                    )
                    val time = routes.getOnePersonOperationTime(
                        monthOfYear, userSettings.timeZone
                    )
                    val resultTIme = time + passengerTime
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                onePersonOperationTime = ResultState.Success(resultTIme)
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            onePersonOperationTime = ResultState.Error(ErrorEntity(e))
                        )
                    }
                }
            }
        }
    }

    private fun calculationOfHeavyTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        heavyTrainsTime = ResultState.Loading()
                    )
                }
            }
            try {
                val timeState =
                    salaryCalculationHelper.getTimeListSurchargeHeavyTrainsFlow().first().sum()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            heavyTrainsTime = ResultState.Success(timeState)
                        )
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            heavyTrainsTime = ResultState.Error(ErrorEntity(e))
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
//            if (result is ResultState.Success) {
            result.find {
                it.year == yearAndMonth.first && it.month == yearAndMonth.second
            }?.let { selectMonthOfYear ->
                currentMonthOfYear = selectMonthOfYear
                currentUserSetting = currentUserSetting?.copy(
                    selectMonthOfYear = selectMonthOfYear
                )
                saveCurrentMonthInLocal(selectMonthOfYear)
            }
//            }
        }
            .launchIn(viewModelScope)
    }

    private fun saveCurrentMonthInLocal(monthOfYear: MonthOfYear) {
        saveCurrentMonthJob?.cancel()
        saveCurrentMonthJob =
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).onEach {
                if (it is ResultState.Success) {
//                    loadSetting()
                    saveCurrentMonthJob?.cancel()
                }
            }.launchIn(viewModelScope)
    }

    //
    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { list ->
            _uiState.update { state ->
                state.copy(
                    monthList = list.map { it.month }.distinct().sorted(),
                    yearList = list.map { it.year }.distinct().sorted()
                )
            }
        }.launchIn(viewModelScope)
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
            uiState.value.dateAndTimeConverter?.let { dateAndTimeConverter ->
                val startWork =
                    dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeStartWork)

                val isDifference = dateAndTimeConverter.isDifferenceDate(
                    first = route.basicData.timeStartWork,
                    second = route.basicData.timeEndWork
                )

                val endWork = if (isDifference) {
                    dateAndTimeConverter.getDateMiniAndTime(value = route.basicData.timeEndWork)
                } else {
                    dateAndTimeConverter.getTime(route.basicData.timeEndWork)
                }
                return "$startWork - $endWork"
            } ?: ""
        } ?: ""
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            initLoading()
            loadMonthList()
            checkLoginToAccount()
            initListStationAndLocomotiveSeries()
            sharedPreferenceStorage.enableShowingUpdatePresentation()
            initUpdateManager()
        }
    }

    suspend fun initLoading() {
        val combinedData: StateFlow<InitialData> = combine(
            salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }
                .onStart { emit(null) },
            settingsUseCase.getUserSettingFlow().map { it as UserSettings? }
                .onStart { emit(null) },
        ) { us, ss ->
            InitialData(ss, us)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, InitialData())

        combinedData.collect { initData ->
            val userSettings = initData.userSettings
            val salarySetting = initData.salarySetting
            if (userSettings != null && salarySetting != null) {
                currentUserSetting = userSettings
                currentMonthOfYear = userSettings.selectMonthOfYear

                val dateAndTimeConverter = DateAndTimeConverter(userSettings)
                _uiState.update {
                    it.copy(
                        uiState = ResultState.Success(Unit),
                        offsetInMoscow = userSettings.timeZone,
                        dateAndTimeConverter = dateAndTimeConverter,
                        minTimeRest = userSettings.minTimeRestPointOfTurnover,
                        minTimeHomeRest = userSettings.minTimeHomeRest,
                    )
                }

                routeUseCase.listRoutesByMonth(
                    userSettings.selectMonthOfYear,
                    userSettings.timeZone
                ).collect { result ->
                    if (result is ResultState.Success) {
                        val timeZone = dateAndTimeConverter.timeZoneText
                        val currentTimeCalendar = getInstance(TimeZone.getTimeZone(timeZone))
                        val currentTimeInMillis = currentTimeCalendar.timeInMillis
                        val routeList = if (userSettings.isConsiderFutureRoute) {
                            result.data
                        } else {
                            result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                        }
                        val routeStateList = mutableListOf<ItemState>()
                        currentMonthOfYear?.let { monthOfYear ->
                            routeList.forEach { route ->
                                val routeState = ItemState(
                                    route = route,
                                    isHoliday = isHolidayTimeInRoute(
                                        monthOfYear,
                                        userSettings,
                                        route
                                    ),
                                    isHeavyTrains = isHeavyTrains(salarySetting, route),
                                    isExtendedServicePhaseTrains = isExtendedServicePhaseTrains(
                                        salarySetting,
                                        route
                                    )
                                )
                                routeStateList.add(routeState)
                            }
                        }

                        routeList.forEach { route ->
                            if (route.isCurrentRoute(currentTimeInMillis)) {
                                currentRoute = route
                                route.basicData.timeStartWork?.let { startWork ->
                                    workTimer(startWork)
                                }
//                                initStateIsOneTheWay()
                            }
                        }

                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    listItemState = routeStateList
                                )
                            }
                        }

                        val salaryCalculationHelper = SalaryCalculationHelper(
                            userSettings = userSettings,
                            salarySetting = salarySetting,
                            routeList = routeList
                        )

                        calculationOfExtendedServicePhaseTime(salaryCalculationHelper)
                        calculationOfLongDistanceTrainsTime(salaryCalculationHelper)
                        calculationOfHeavyTrainsTime(salaryCalculationHelper)

                        calculationOfOnePersonOperationTime(routeList, userSettings)
                        calculationTotalTime(routeList, userSettings.timeZone)
                        calculationOfTimeWithoutHoliday(routeList, userSettings.timeZone)
                        calculationOfNightTime(routeList, userSettings)
                        calculationOfSingleLocomotiveTime(routeList)
                        calculationPassengerTime(routeList, userSettings.timeZone)
                        calculationHolidayTime(routeList, userSettings.timeZone)
                    }
                }
            }
        }
    }
}

data class InitialData(
    val userSettings: UserSettings? = null,
    val salarySetting: SalarySetting? = null,
)