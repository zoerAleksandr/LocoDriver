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
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
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
import com.z_company.repository.ShareManager
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.route.viewmodel.SalaryCalculationHelper
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult

data class OpenRouteFormEvent(val basicId: String?, val isMakeCopy: Boolean)

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
    private val ruStoreAppUpdateManager: RuStoreAppUpdateManager by inject()
    private val shareManager: ShareManager by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val _openRouteFormEvent = MutableSharedFlow<OpenRouteFormEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openRouteFormEvent: SharedFlow<OpenRouteFormEvent> = _openRouteFormEvent.asSharedFlow()

    var timeWithoutHoliday by mutableLongStateOf(0L)
        private set

    var currentRoute by mutableStateOf<Route?>(null)

    private val routeParams = MutableStateFlow<Pair<MonthOfYear, Long>?>(null)

    // will switch to the latest listRoutesByMonth when routeParams changes
    private val routesFlow = routeParams
        .filterNotNull()
        .flatMapLatest { (month, tz) ->
            routeUseCase.listRoutesByMonth(month, tz)
        }

    // keep current salary setting for use in routesFlow processing
    private var currentSalarySetting: SalarySetting? = null

    private val _saveTimeEvent = MutableSharedFlow<String>(replay = 0)
    val saveTimeEvent: SharedFlow<String> = _saveTimeEvent.asSharedFlow()

    private val _workTimeInCurrentRoute = MutableSharedFlow<Long>(replay = 1)
    val workTimeInCurrentRoute = _workTimeInCurrentRoute.asSharedFlow()

    private var removeRouteJob: Job? = null
    private var setCalendarJob: Job? = null
    private var saveCurrentMonthJob: Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _previewRouteUiState = MutableStateFlow(PreviewRouteUiState())
    val previewRouteUiState = _previewRouteUiState.asStateFlow()

    private val _purchasesEvent = MutableSharedFlow<StartPurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val purchasesEvent = _purchasesEvent.asSharedFlow()

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
            routeHelper.setFavoriteRoute(route).collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        // сохраняем loading флаг если нужно
                    }

                    is ResultState.Success -> {
                        val text =
                            if (result.data) "Маршрут добавлен в избранное" else "Маршрут удален из избранного"
                        // уведомляем через SnackbarManager и сбрасываем state (чтобы не держать success в uiState)
                        snackbarManager.show(message = text)
                    }

                    is ResultState.Error -> {
                        // также уведомляем об ошибке
                        val message =
                            result.entity.message ?: result.entity.throwable?.message ?: "Ошибка"
                        snackbarManager.show(message = message)
                        _uiState.update { it.copy(removeRouteState = ResultState.Error(result.entity)) }
                    }
                }
            }
        }
    }

    fun checkPurchasesAvailability() {
        RuStoreBillingClient.checkPurchasesAvailability()
            .addOnSuccessListener { result ->
                when (result) {
                    is FeatureAvailabilityResult.Available -> {
                        _purchasesEvent.tryEmit(StartPurchasesEvent.PurchasesAvailability(result))
                    }

                    is FeatureAvailabilityResult.Unavailable -> {
                        snackbarManager.show(
                            message = "Ошибка ${result.cause.message}",
                            showOnceKey = "checkPurchasesAvailability"
                        )
                    }
                }
            }
            .addOnFailureListener { throwable ->
                snackbarManager.show(
                    message = "Ошибка ${throwable.message}",
                    showOnceKey = "checkPurchasesAvailability"
                )
            }
    }

    fun restorePurchases() {
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
                                                    snackbarManager.show(
                                                        message = "Ошибка ruStoreUseCase.getExpiryTimeMillis ${resultState.entity.message}",
                                                        showOnceKey = "restore_purchases_none"
                                                    )
                                                    job?.cancel()
                                                }
                                            }
                                        }
                                    }
                                    job.join()
                                }
                                if (maxEndTime > currentTimeInMillis) {
                                    sharedPreferenceStorage.setSubscriptionExpiration(maxEndTime)
                                    snackbarManager.show(
                                        message = "Покупки восстановлены",
                                        showOnceKey = "restore_purchases_success"
                                    )
                                }
                                if (maxEndTime < currentTimeInMillis) {
                                    snackbarManager.show(
                                        message = "Действующих подписок не найдено",
                                        showOnceKey = "restore_purchases_none"
                                    )
                                }
                            }
                        }
                        .addOnFailureListener {
                            snackbarManager.show(
                                message = "Ошибка получения данных от сервера",
                            )
                        }
                } catch (e: Exception) {
                    snackbarManager.show(message = e.message ?: "Ошибка при восстановлении")
                }
            }
        }
    }

    fun newRouteClick(basicId: String? = null) {
        _uiState.update {
            it.copy(
                isLoadingStateAddButton = true
            )
        }
        viewModelScope.launch {
            when (val decision =
                routeHelper.newRouteClick(basicId = basicId, isMakeCopy = basicId != null)) {
                is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                    _alertBeforePurchasesEvent.tryEmit(AlertBeforePurchasesEvent.ShowDialogNeedSubscribe)
                    _uiState.update { it.copy(isLoadingStateAddButton = false) }
                }

                is RouteActionsHelper.NewRouteResult.AlertSubscribeDialog -> {
                    _alertBeforePurchasesEvent.tryEmit(AlertBeforePurchasesEvent.ShowDialogAlertSubscribe)
                    _uiState.update { it.copy(isLoadingStateAddButton = false) }
                }

                is RouteActionsHelper.NewRouteResult.ShowNewRouteScreen -> {
                    _openRouteFormEvent.tryEmit(
                        OpenRouteFormEvent(
                            decision.basicId,
                            decision.isMakeCopy
                        )
                    )

                    _uiState.update {
                        it.copy(
                            isLoadingStateAddButton = false
                        )
                    }
                }

                is RouteActionsHelper.NewRouteResult.Error -> {
                    _uiState.update { it.copy(isLoadingStateAddButton = false) }
                }
            }
        }
    }

    private var currentUserSetting: UserSettings?
        get() {
            return _uiState.value.settingState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        set(value) {
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

    fun removeRoute(route: Route) {
        removeRouteJob?.cancel()
        removeRouteJob = routeUseCase.markAsRemoved(route).onEach { result ->
            when (result) {
                is ResultState.Loading -> {
                    // сохраняем loading флаг если нужно
                    _uiState.update { it.copy(removeRouteState = ResultState.Loading()) }
                }

                is ResultState.Success -> {
                    // уведомляем через SnackbarManager и сбрасываем state (чтобы не держать success в uiState)
                    snackbarManager.show(message = "Маршрут удалён")
                    _uiState.update { it.copy(removeRouteState = null) }
                }

                is ResultState.Error -> {
                    // также уведомляем об ошибке
                    val message =
                        result.entity.message ?: result.entity.throwable?.message ?: "Ошибка"
                    snackbarManager.show(message = message)
                    _uiState.update { it.copy(removeRouteState = ResultState.Error(result.entity)) }
                }
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun calculationOfTimeWithoutHoliday(routes: List<Route>, offsetInMoscow: Long) {
        currentMonthOfYear?.let { monthOfYear ->
            timeWithoutHoliday = routes.getWorkTimeWithoutHoliday(monthOfYear, offsetInMoscow)
        }
    }

    fun syncRoute(route: Route) {
        viewModelScope.launch {
            routeHelper.syncRoute(route).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        // show snackbar centrally
                        snackbarManager.show(message = result.data)
                    }

                    is ResultState.Error -> {
                        val message = result.entity.message ?: result.entity.throwable?.message
                        ?: "Ошибка синхронизации"
                        snackbarManager.show(message = message)
                    }

                    is ResultState.Loading -> {
                    }
                }
            }
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
            result.find {
                it.year == yearAndMonth.first && it.month == yearAndMonth.second
            }?.let { selectMonthOfYear ->
                currentMonthOfYear = selectMonthOfYear
                currentUserSetting = currentUserSetting?.copy(
                    selectMonthOfYear = selectMonthOfYear
                )
                saveCurrentMonthInLocal(selectMonthOfYear)
            }
        }
            .launchIn(viewModelScope)
    }

    private fun saveCurrentMonthInLocal(monthOfYear: MonthOfYear) {
        saveCurrentMonthJob?.cancel()
        saveCurrentMonthJob =
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).onEach {
                if (it is ResultState.Success) {
                    saveCurrentMonthJob?.cancel()
                }
            }.launchIn(viewModelScope)
    }

    private suspend fun loadMonthList() {
        val list = calendarUseCase.loadFlowMonthOfYearListState().first()
        _uiState.update { state ->
            state.copy(
                monthList = list.map { it.month }.distinct().sorted(),
                yearList = list.map { it.year }.distinct().sorted()
            )
        }
    }

    fun calculationHomeRest(route: Route?) {
        viewModelScope.launch {
            val result = routeHelper.calculationHomeRest(
                route = route,
            )
            when (result) {
                is ResultState.Success -> { /* result.data is Long? */
                    _previewRouteUiState.update {
                        it.copy(
                            homeRest = result.data
                        )
                    }
                }

                else -> {}
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadMonthList()
            checkLoginToAccount()
            initListStationAndLocomotiveSeries()
            sharedPreferenceStorage.enableShowingUpdatePresentation()
            initUpdateManager()
            initLoading()
        }
    }

    fun initLoading() {
        // build combinedData like before (keep it as StateFlow)
        val combinedData: StateFlow<InitialData> = combine(
            salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }
                .onStart { emit(null) },
            settingsUseCase.getUserSettingFlow().map { it as UserSettings? }
                .onStart { emit(null) },
        ) { us, ss ->
            InitialData(ss, us)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, InitialData())

        // Collect combinedData to update local settings and to push params to routeParams
        // Use collectLatest so if combinedData emits quickly many times, we process latest (but this collector is short)
        viewModelScope.launch {
            combinedData.collectLatest { initData ->
                // store latest salary and user settings to class fields for routesFlow processing
                currentSalarySetting = initData.salarySetting
                val userSettings = initData.userSettings

                if (userSettings != null && currentSalarySetting != null) {
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

                    // Update params that drive routesFlow. flatMapLatest on routesFlow will switch to the new month/timezone.
                    routeParams.value = userSettings.selectMonthOfYear to userSettings.timeZone
                } else {
                    // If settings or salary not ready, clear route params
                    routeParams.value = null
                }
            }
        }

        // Collect routesFlow in background: this is the single place that handles route lists.
        // flatMapLatest ensures that when routeParams changes, the previous loading is cancelled and new one begins.
        viewModelScope.launch(Dispatchers.IO) {
            routesFlow.collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        // optional: reflect loading state in UI if you want
                        // we update UI on Main thread
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(listItemState = mutableListOf()) }
                        }
                    }

                    is ResultState.Success -> {
                        // all further processing must run on IO or Default depending on heavy computations
                        val userSettings = currentUserSetting
                        val salarySetting = currentSalarySetting
                        if (userSettings != null && salarySetting != null) {
                            val dateAndTimeConverter = DateAndTimeConverter(userSettings)
                            val timeZone = dateAndTimeConverter.timeZoneText
                            val currentTimeCalendar =
                                getInstance(TimeZone.getTimeZone(timeZone))
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
                                        isHeavyTrains = isHeavyTrains(
                                            salarySetting,
                                            route
                                        ),
                                        isExtendedServicePhaseTrains = isExtendedServicePhaseTrains(
                                            salarySetting,
                                            route
                                        )
                                    )
                                    routeStateList.add(routeState)
                                }
                            }

                            currentRoute = null
                            routeList.forEach { route ->
                                if (route.isCurrentRoute(currentTimeInMillis)) {
                                    currentRoute = route
                                    route.basicData.timeStartWork?.let { startWork ->
                                        workTimer(startWork)
                                    }
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

                            // launch background jobs for calculations (same as before)
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
                        } else {
                            // settings not ready - update UI accordingly if needed
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(listItemState = mutableListOf())
                                }
                            }
                        }
                    }

                    is ResultState.Error -> {
                        withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    uiState = ResultState.Error(result.entity)
                                )
                            }
                        }
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