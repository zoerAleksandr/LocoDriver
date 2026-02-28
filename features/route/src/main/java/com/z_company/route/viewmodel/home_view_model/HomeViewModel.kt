package com.z_company.route.viewmodel.home_view_model

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.widget.WidgetUpdater
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTime
import com.z_company.domain.entities.route.UtilsForEntities.getOnePersonOperationTimePassengerTrain
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.route.viewmodel.SalaryCalculationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
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
import kotlinx.coroutines.flow.debounce
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
import java.util.Calendar
import java.util.Calendar.getInstance
import java.util.TimeZone
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
data class OpenRouteFormEvent(val basicId: String?, val isMakeCopy: Boolean)

class HomeViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val ruStoreAppUpdateManager: RuStoreAppUpdateManager by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val routesManager: RoutesManager by inject()
    private val widgetUpdater: WidgetUpdater by inject()

    var timeWithoutHoliday by mutableLongStateOf(0L)
        private set

    var currentRoute by mutableStateOf<Route?>(null)

    private val routeParams = MutableStateFlow<Pair<MonthOfYear, Long>?>(null)

    // will switch to the latest listRoutesByMonth when routeParams changes
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val routesFlow = routeParams
        .filterNotNull()
        .debounce(300)
        .flatMapLatest { (month, tz) ->
            routeUseCase.listRoutesByMonth(month, tz)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // кэш 5 сек после отписки
            initialValue = ResultState.Loading()
        )

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

    private val _updateEvents = MutableSharedFlow<UpdateEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val updateEvents = _updateEvents.asSharedFlow()

    // month/year lists for pickers
    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList.asStateFlow()

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList.asStateFlow()

    override fun onCleared() {
        super.onCleared()
        ruStoreAppUpdateManager.unregisterListener(installStateUpdateListener)
    }

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        currentUserSetting?.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
        }
        return ConverterLongToTime.getTimeInStringFormat(timeToLong)
    }

    fun initUpdateManager() {
        ruStoreAppUpdateManager.getAppUpdateInfo()
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability == UpdateAvailability.Companion.UPDATE_AVAILABLE) {
                    ruStoreAppUpdateManager.registerListener(installStateUpdateListener)
                    ruStoreAppUpdateManager
                        .startUpdateFlow(appUpdateInfo, AppUpdateOptions.Builder().build())
                        .addOnSuccessListener { resultCode ->
                            when (resultCode) {
                                Activity.RESULT_CANCELED -> {}
                                Activity.RESULT_OK -> {}
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
            InstallStatus.Companion.DOWNLOADING -> {}
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
            val timeZone =
                uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val currentTimeCalendar = getInstance(TimeZone.getTimeZone(timeZone))
            val currentTime: Long = currentTimeCalendar.timeInMillis
            val startWorkTime: Long = startWork + uiState.value.offsetInMoscow

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
        if (train == null) return true
        val stations = train.stations
        if (stations.isEmpty()) return true

        // При наличии плеча обслуживания — пропускаем последнюю станцию (конечная точка плеча)
        val hasServicePhase = train.servicePhase != null
        val endIdx = if (hasServicePhase && stations.size >= 2)
            stations.lastIndex - 1
        else
            stations.lastIndex

        // Сканируем с конца, ищем последнее заполненное время
        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.timeDeparture != null) return false  // последнее — departure → следующее arrival
            if (s.timeArrival != null) return true      // последнее — arrival → следующее departure
        }
        return true // ничего не заполнено → departure
    }

    fun isNextDeparture(): Boolean {
        val lastTrain = currentRoute?.trains?.lastOrNull()
        return nextIsDeparture(lastTrain)
    }

    fun onGoClicked() {
        viewModelScope.launch {
            val current = currentRoute?.trains?.lastOrNull()
            val timeZone = uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val now = getInstance(TimeZone.getTimeZone(timeZone)).apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val updatedTrain = withContext(Dispatchers.Default) {
                if (current == null) {
                    Train(stations = mutableListOf(Station(timeDeparture = now)))
                } else {
                    val stations = current.stations.toMutableList()
                    if (stations.isEmpty()) {
                        stations.add(Station(timeDeparture = now))
                    } else {
                        // Найти последнее заполненное время (сканируем с конца)
                        // При наличии плеча обслуживания — пропускаем последнюю станцию
                        val hasServicePhase = current.servicePhase != null
                        val endIdx = if (hasServicePhase && stations.size >= 2)
                            stations.lastIndex - 1
                        else
                            stations.lastIndex

                        var filled = false
                        for (i in endIdx downTo 0) {
                            val s = stations[i]
                            if (s.timeDeparture != null) {
                                // Последнее — departure → arrival на следующей станции
                                val nextIdx = i + 1
                                val isServicePhaseArrival = hasServicePhase && nextIdx == stations.lastIndex

                                if (nextIdx <= stations.lastIndex && !isServicePhaseArrival) {
                                    stations[nextIdx] = stations[nextIdx].copy(timeArrival = now)
                                } else {
                                    // Вставить новую станцию перед последней при наличии плеча
                                    if (hasServicePhase && stations.size >= 2) {
                                        stations.add(stations.lastIndex, Station(timeArrival = now))
                                    } else {
                                        stations.add(Station(timeArrival = now))
                                    }
                                }
                                filled = true
                                break
                            }
                            if (s.timeArrival != null) {
                                // Последнее — arrival → departure на той же станции
                                stations[i] = s.copy(timeDeparture = now)
                                filled = true
                                break
                            }
                        }
                        if (!filled) {
                            // Ничего не заполнено → departure первой станции
                            stations[0] = stations[0].copy(timeDeparture = now)
                        }
                    }
                    current.copy(stations = stations)
                }
            }

            try {
                val text = if (isNextDeparture()) "Сохранено время отправления" else "Сохранено время прибытия"
                val timeText = uiState.value.dateAndTimeConverter?.getTime(now) ?: ""
                trainUseCase.updateTrain(updatedTrain).collect { saveResult ->
                    if (saveResult is ResultState.Success) {
                        // Обновить currentRoute локально, чтобы isNextDeparture() видел актуальные данные.
                        // Создаём новый список (не мутируем старый), иначе mutableStateOf не обнаружит изменение
                        currentRoute?.let { route ->
                            val newTrains = route.trains.toMutableList()
                            val trainIndex = newTrains.indexOfFirst { it.trainId == updatedTrain.trainId }
                            if (trainIndex >= 0) {
                                newTrains[trainIndex] = updatedTrain
                            } else {
                                newTrains.add(updatedTrain)
                            }
                            currentRoute = route.copy(trains = newTrains)
                        }
                        _saveTimeEvent.emit("$text $timeText")
                    }
                }
            } catch (_: Exception) { }
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

    @SuppressLint("SuspiciousIndentation")
    private fun calculationOfNightTime(routes: List<Route>, settings: UserSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    nightTimeInRouteList = ResultState.Loading()
                )
            }
            try {
                val nightTimeState = routes.getNightTime(settings)
                _uiState.update {
                    it.copy(
                        nightTimeInRouteList = ResultState.Success(nightTimeState)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        nightTimeInRouteList = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }

    private fun calculationOfSingleLocomotiveTime(routes: List<Route>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    singleLocomotiveTimeState = ResultState.Loading()
                )
            }
            try {
                val timeState = routes.getSingleLocomotiveTime()
                _uiState.update {
                    it.copy(
                        singleLocomotiveTimeState = ResultState.Success(timeState)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        singleLocomotiveTimeState = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }

    private fun calculationOfLongDistanceTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    longDistanceTrainsTime = ResultState.Loading()
                )
            }
            try {
                val timeState = salaryCalculationHelper.getTimeLongDistanceTrainFlow().first()
                _uiState.update {
                    it.copy(
                        longDistanceTrainsTime = ResultState.Success(timeState)
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        longDistanceTrainsTime = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }

    private fun calculationOfExtendedServicePhaseTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    extendedServicePhaseTime = ResultState.Loading()
                )
            }
            try {
                val timeState =
                    salaryCalculationHelper.getTotalTimeSurchargeServicePhaseFlow().first()
                _uiState.update {
                    it.copy(
                        extendedServicePhaseTime = ResultState.Success(timeState)
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        extendedServicePhaseTime = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }

    private fun calculationOfOnePersonOperationTime(
        routes: List<Route>, userSettings: UserSettings
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    onePersonOperationTime = ResultState.Loading()
                )
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
                    _uiState.update {
                        it.copy(
                            onePersonOperationTime = ResultState.Success(resultTIme)
                        )
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        onePersonOperationTime = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }

    private fun calculationOfHeavyTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    heavyTrainsTime = ResultState.Loading()
                )
            }
            try {
                val timeState = salaryCalculationHelper.getTotalTimeHeavyTrainsFlow().first()
                _uiState.update {
                    it.copy(
                        heavyTrainsTime = ResultState.Success(timeState)
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        heavyTrainsTime = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }


    private fun calculationHolidayTime(routes: List<Route>, offsetInMoscow: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    holidayHours = ResultState.Loading()
                )
            }
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    val holidayTime =
                        routes.getWorkingTimeOnAHoliday(monthOfYear, offsetInMoscow).first()
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
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val fullToken = "Bearer $token"
            if (token == null) {
                snackbarManager.show(message = "Неавторизованный пользователь")
            } else {
                routesManager.saveRouteInRemote(route, fullToken).collect { resultState ->
                    when (resultState) {
                        is ResultState.Success -> {
                            // show snackbar centrally
                            routeUseCase.setSynchronizedRoute(route.basicData.id).first()
                            snackbarManager.show(message = "Маршрут сохранен в облаке")
                        }

                        is ResultState.Error -> {
                            val message =
                                resultState.entity.message ?: resultState.entity.throwable?.message
                                ?: "Ошибка синхронизации"
                            snackbarManager.show(message = message)
                        }

                        is ResultState.Loading -> {
                        }
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

    private fun pushWidgetData(routeCount: Int) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val totalTimeMillis = (state.totalTimeWithHoliday as? ResultState.Success)?.data ?: 0L
                val totalTimeText = convertTimeToStringFormat(totalTimeMillis)

                val monthOfYear = currentMonthOfYear
                val monthYear = if (monthOfYear != null) {
                    val monthNames = arrayOf(
                        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
                    )
                    "${monthNames.getOrElse(monthOfYear.month - 1) { "" }} ${monthOfYear.year}"
                } else ""

                val normPercent = if (monthOfYear != null) {
                    val norma = monthOfYear.days.sumOf { day ->
                        if (!day.isReleaseDay) {
                            when (day.tag) {
                                TagForDay.WORKING_DAY -> 8
                                TagForDay.SHORTENED_DAY -> 7
                                else -> 0
                            }
                        } else 0
                    }
                    if (norma > 0) {
                        val percent = (totalTimeMillis / (norma * 3_600_000.0) * 100).toInt()
                        "$percent%"
                    } else "0%"
                } else "0%"

                val route = currentRoute
                val hasCurrentRoute = route != null
                val trainNumber = route?.trains?.lastOrNull()?.number ?: ""
                val isDeparture = isNextDeparture()

                // Try to get the latest work time from the replay cache
                val currentWorkTimeMillis = _workTimeInCurrentRoute.replayCache.firstOrNull() ?: 0L
                val workTimeText = if (hasCurrentRoute) {
                    ConverterLongToTime.getTimeInStringFormat(currentWorkTimeMillis)
                } else {
                    "00:00"
                }

                widgetUpdater.update(
                    totalTimeText = totalTimeText,
                    normPercent = normPercent,
                    monthYear = monthYear,
                    hasCurrentRoute = hasCurrentRoute,
                    trainNumber = trainNumber,
                    workTime = workTimeText,
                    isDepartureNext = isDeparture,
                    routeCount = routeCount.toString()
                )
            } catch (e: Exception) {
                Log.w("HomeViewModel", "Widget update failed", e)
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

    private fun loadMonthList() {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState()
                .collect { list ->
                    val months = list.map { it.month }.distinct().sorted()
                    val years = list.map { it.year }.distinct().sorted()
                    _monthList.value = months
                    _yearList.value = years
                }
        }
    }

    fun calculationHomeRest(route: Route?) {
        viewModelScope.launch {
            val result = routeHelper.calculationHomeRest(
                route = route,
            ).first()
            when (result) {
                is ResultState.Success -> { /* result.data is Long? */
                    _previewRouteUiState.update {
                        it.copy(
                            homeRest = result.data?.second
                        )
                    }
                }

                else -> {}
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

    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadMonthList()
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
                        // Не очищаем список — показываем старые данные, пока загружаются новые
                    }

                    is ResultState.Success -> {
                        val fullRouteList = result.data
                        val userSettings = currentUserSetting
                        val salarySetting = currentSalarySetting
                        if (userSettings != null && salarySetting != null) {
                            val dateAndTimeConverter = DateAndTimeConverter(userSettings)
                            val timeZone = dateAndTimeConverter.timeZoneText
                            val currentTimeCalendar =
                                getInstance(TimeZone.getTimeZone(timeZone))
                            val currentTimeInMillis = currentTimeCalendar.timeInMillis


                            val routeStateList = mutableListOf<ItemState>()
                            currentMonthOfYear?.let { monthOfYear ->
                                fullRouteList.forEach { route ->
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

                            currentRoute = fullRouteList.findCurrentRoute(
                                currentTimeInMillis = currentTimeInMillis,
                                userSettings = userSettings
                            )

                            currentRoute?.let {
                                workTimer(it.basicData.timeStartWork!!)
                            }

//                            withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    listItemState = routeStateList
                                )
//                                }
                            }

                            val filteredRouteList = if (userSettings.isConsiderFutureRoute) {
                                result.data
                            } else {
                                result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                            }

                            val salaryCalculationHelper = SalaryCalculationHelper(
                                userSettings = userSettings,
                                salarySetting = salarySetting,
                                routeList = filteredRouteList
                            )

                            // launch background jobs for calculations (same as before)
                            viewModelScope.launch(Dispatchers.Default) { // Default лучше для CPU-intensive
                                coroutineScope {
                                    calculationOfExtendedServicePhaseTime(salaryCalculationHelper)
                                    calculationOfLongDistanceTrainsTime(salaryCalculationHelper)
                                    calculationOfHeavyTrainsTime(salaryCalculationHelper)

                                    calculationOfOnePersonOperationTime(
                                        filteredRouteList,
                                        userSettings
                                    )
                                    calculationTotalTime(filteredRouteList, userSettings.timeZone)
                                    calculationOfTimeWithoutHoliday(
                                        filteredRouteList,
                                        userSettings.timeZone
                                    )
                                    calculationOfNightTime(filteredRouteList, userSettings)
                                    calculationOfSingleLocomotiveTime(filteredRouteList)
                                    calculationPassengerTime(
                                        filteredRouteList,
                                        userSettings.timeZone
                                    )
                                    calculationHolidayTime(filteredRouteList, userSettings.timeZone)
                                }
                                // Update widget after all calculations complete
                                pushWidgetData(filteredRouteList.size)
                            }
                        } else {
                            // settings not ready - update UI accordingly if needed
//                            withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(listItemState = mutableListOf())
                            }
//                            }
                        }
                    }

                    is ResultState.Error -> {
//                        withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                uiState = ResultState.Error(result.entity)
                            )
                        }
//                        }
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