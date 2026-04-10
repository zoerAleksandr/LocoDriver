package com.z_company.route.viewmodel.home_view_model

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import com.z_company.core.sendToSentry
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
import com.z_company.core.util.TimeManager
import com.z_company.core.widget.WidgetUpdater
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.findNextFutureRoute
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
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
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
    private val timeManager = TimeManager()
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
    private val shareRouteManager: ShareRouteManager by inject()
    private val syncManager: SyncManager by inject()
    private val widgetUpdater: WidgetUpdater by inject()

    var timeWithoutHoliday by mutableLongStateOf(0L)
        private set

    var todayWorkTime by mutableLongStateOf(0L)
        private set

    // Все маршруты (все месяцы) для поиска следующей явки
    private var allRoutesGlobal: List<Route> = emptyList()

    var isConsiderFutureRoute by mutableStateOf(false)
        private set

    var currentRoute by mutableStateOf<Route?>(null)

    private val routeParams = MutableStateFlow<Pair<MonthOfYear, TimeCalculationContext>?>(null)

    // will switch to the latest listRoutesByMonth when routeParams changes
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val routesFlow = routeParams
        .filterNotNull()
        .debounce(300)
        .flatMapLatest { (month, context) ->
            routeUseCase.routeListByMonthFlow(month, context)
                .map<List<Route>, ResultState<List<Route>>> { ResultState.Success(it) }
                .onStart { emit(ResultState.Loading()) }
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

    // Событие с готовой публичной ссылкой для "Поделиться" — экран открывает share-sheet.
    private val _shareLinkEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shareLinkEvent: SharedFlow<String> = _shareLinkEvent.asSharedFlow()

    private val _isCreatingShareLink = MutableStateFlow(false)
    val isCreatingShareLink: StateFlow<Boolean> = _isCreatingShareLink.asStateFlow()

    private val _workTimeInCurrentRoute = MutableSharedFlow<Long>(replay = 1)
    val workTimeInCurrentRoute = _workTimeInCurrentRoute.asSharedFlow()

    var nextFutureRoute by mutableStateOf<Route?>(null)

    private val _countdownToNextRoute = MutableSharedFlow<Long>(replay = 1)
    val countdownToNextRoute = _countdownToNextRoute.asSharedFlow()

    private var countdownTimerJob: Job? = null

    // Кэш последнего списка маршрутов — нужен для пересчёта при завершении маршрута
    // без повторного обращения к БД (используется в handleRouteEnded).
    private var cachedRouteList: List<Route> = emptyList()

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

    /**
     * Вызывается когда таймер обнаружил, что timeEndWork маршрута уже наступило
     * (пока приложение было открыто или телефон заблокирован).
     * Сбрасывает currentRoute и пересчитывает todayWorkTime + countdown без обращения к БД.
     */
    private fun handleRouteEnded() {
        currentRoute = null
        timerJob = null

        val userSettings = currentUserSetting ?: return
        val monthOfYear = currentMonthOfYear ?: return
        val routeList = cachedRouteList

        val currentTimeInMillis = Calendar.getInstance().timeInMillis

        // Пересчитываем время, отработанное за сегодня, с учётом только завершённых маршрутов
        if (isConsiderFutureRoute) {
            val completedRoutes = routeList.filter { route ->
                val end = route.basicData.timeEndWork ?: return@filter false
                end <= currentTimeInMillis
            }
            todayWorkTime = completedRoutes.getWorkTime(monthOfYear, TimeCalculationContext.from(userSettings))
        }

        // Запускаем обратный отсчёт до следующего маршрута
        val next = routeList.findNextFutureRoute(currentTimeInMillis)
        nextFutureRoute = next
        next?.basicData?.timeStartWork?.let { startWork ->
            countdownTimer(startWork)
        }
    }

    fun refreshTimer() {
        val route = currentRoute ?: return
        val startWork = route.basicData.timeStartWork ?: return
        workTimer(startWork)
    }

    fun workTimer(startWork: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val timeZone =
                uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val tz = TimeZone.getTimeZone(timeZone)
            val startWorkTime: Long = startWork

            val currentTimeCalendar = getInstance(tz)
            val second = currentTimeCalendar.get(Calendar.SECOND)
            val firstIncreasingTimeInMillis = (60 - second) * 1000L

            // Первый тик: если маршрут уже завершился до старта таймера — сразу обрабатываем
            val initEndWork = currentRoute?.basicData?.timeEndWork
            val initNow = currentTimeCalendar.timeInMillis
            if (initEndWork != null && initNow >= initEndWork) {
                _workTimeInCurrentRoute.tryEmit(initEndWork - startWorkTime)
                handleRouteEnded()
                return@launch
            }

            _workTimeInCurrentRoute.tryEmit(initNow - startWorkTime)
            delay(firstIncreasingTimeInMillis)
            while (currentRoute != null) {
                // Пересчитываем от реального времени — delay() может быть длиннее 60 сек
                // при блокировке экрана (Doze mode), поэтому накопительное += 60_000 даёт отставание.
                val now = getInstance(tz).timeInMillis
                val endWork = currentRoute?.basicData?.timeEndWork

                if (endWork != null && now >= endWork) {
                    // Маршрут завершился пока приложение было открыто / телефон заблокирован
                    _workTimeInCurrentRoute.tryEmit(endWork - startWorkTime)
                    handleRouteEnded()
                    break
                }

                _workTimeInCurrentRoute.tryEmit(now - startWorkTime)
                delay(60_000L)
            }
        }
    }

    fun countdownTimer(startWork: Long) {
        countdownTimerJob?.cancel()
        countdownTimerJob = viewModelScope.launch {
            val timeZone =
                uiState.value.dateAndTimeConverter?.timeZoneText ?: "GMT+3"
            val tz = TimeZone.getTimeZone(timeZone)
            val startWorkTime: Long = startWork

            val currentTimeCalendar = getInstance(tz)
            val second = currentTimeCalendar.get(Calendar.SECOND)
            val firstTickDelayMillis = (60 - second) * 1000L

            var difference = startWorkTime - currentTimeCalendar.timeInMillis
            if (difference <= 0) {
                _countdownToNextRoute.tryEmit(0L)
                return@launch
            }

            _countdownToNextRoute.tryEmit(difference)
            delay(firstTickDelayMillis)

            while (difference > 0) {
                // Пересчитываем от реального времени — без накопительного -= 60_000,
                // чтобы не отставать после блокировки экрана (Doze mode).
                difference = startWorkTime - getInstance(tz).timeInMillis
                if (difference <= 0) {
                    _countdownToNextRoute.tryEmit(0L)
                    nextFutureRoute = null
                    break
                }
                _countdownToNextRoute.tryEmit(difference)
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
            val now = timeManager.now()

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
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "onGoClicked")
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
            e.sendToSentry("HomeViewModel", "getDayOffTime")
            _uiState.update {
                it.copy(
                    dayOffHours = ResultState.Error(ErrorEntity(e))
                )
            }
        }
    }

    private fun calculationPassengerTime(routes: List<Route>, context: TimeCalculationContext) {
        _uiState.update {
            it.copy(
                passengerTimeInRouteList = ResultState.Loading()
            )
        }
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val passengerTime = routes.getPassengerTime(monthOfYear, context)
                _uiState.update {
                    it.copy(
                        passengerTimeInRouteList = ResultState.Success(passengerTime)
                    )
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationPassengerTime")
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
                e.sendToSentry("HomeViewModel", "calculationOfNightTime")
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
                e.sendToSentry("HomeViewModel", "calculationOfSingleLocomotiveTime")
                _uiState.update {
                    it.copy(
                        singleLocomotiveTimeState = ResultState.Error(ErrorEntity(e))
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
                e.sendToSentry("HomeViewModel", "calculationOfExtendedServicePhaseTime")
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
        val context = TimeCalculationContext.from(userSettings)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    onePersonOperationTime = ResultState.Loading()
                )
            }
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    val passengerTime = routes.getOnePersonOperationTimePassengerTrain(
                        monthOfYear, context
                    )
                    val time = routes.getOnePersonOperationTime(
                        monthOfYear, context
                    )
                    val resultTIme = time + passengerTime
                    _uiState.update {
                        it.copy(
                            onePersonOperationTime = ResultState.Success(resultTIme)
                        )
                    }
                }

            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "calculationOfOnePersonOperationTime")
                _uiState.update {
                    it.copy(
                        onePersonOperationTime = ResultState.Error(ErrorEntity(e))
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
                val timeState = salaryCalculationHelper.getTotalTimeLongTrainsFlow().first()
                _uiState.update {
                    it.copy(
                        longDistanceTrainsTime = ResultState.Success(timeState)
                    )
                }

            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "calculationOfLongDistanceTrainsTime")
                _uiState.update {
                    it.copy(
                        longDistanceTrainsTime = ResultState.Error(ErrorEntity(e))
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
                e.sendToSentry("HomeViewModel", "calculationOfHeavyTrainsTime")
                _uiState.update {
                    it.copy(
                        heavyTrainsTime = ResultState.Error(ErrorEntity(e))
                    )
                }
            }
        }
    }


    private fun calculationHolidayTime(routes: List<Route>, context: TimeCalculationContext) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    holidayHours = ResultState.Loading()
                )
            }
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    val holidayTime =
                        routes.getWorkingTimeOnAHoliday(monthOfYear, context).first()
                    _uiState.update {
                        it.copy(
                            holidayHours = ResultState.Success(holidayTime)
                        )
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "calculationHolidayTime")
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

    private suspend fun calculationOfTimeWithoutHoliday(routes: List<Route>, context: TimeCalculationContext) {
        currentMonthOfYear?.let { monthOfYear ->
            timeWithoutHoliday = routes.getWorkTimeWithoutHoliday(monthOfYear, context)
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

    /**
     * Создаёт публичную ссылку на [route] и эмитит её в [shareLinkEvent].
     * UI-слой (HomeScreen) открывает системный share-sheet по полученной ссылке.
     */
    fun shareRoute(route: Route) {
        if (_isCreatingShareLink.value) return
        _isCreatingShareLink.value = true
        viewModelScope.launch {
            try {
                val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (rawToken.isNullOrBlank()) {
                    snackbarManager.show("Неавторизованный пользователь")
                    return@launch
                }
                val bearerToken = "Bearer $rawToken"
                shareRouteManager.createShareLink(route, bearerToken).collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val shareText = buildShareText(route, result.data)
                            _shareLinkEvent.emit(shareText)
                        }
                        is ResultState.Error -> {
                            val message = result.entity.message
                                ?: result.entity.throwable?.message
                                ?: "Не удалось создать ссылку"
                            snackbarManager.show(message)
                        }
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "shareRoute")
                snackbarManager.show("Ошибка создания ссылки")
            } finally {
                _isCreatingShareLink.value = false
            }
        }
    }

    private fun buildShareText(route: Route, url: String): String {
        return buildString {
            append("Вам отправлен маршрут в приложении «Машинист»")
            // Дата и время
            route.basicData.timeStartWork?.let { ms ->
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                append(" от ${sdf.format(java.util.Date(ms))}")
            }
            // Маршрут следования: первая и последняя станция первого поезда
            val stations = route.trains
                .flatMap { it.stations }
                .sortedBy { it.orderIndex }
            val firstStation = stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val lastStation = stations.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
            if (firstStation != null && lastStation != null && firstStation != lastStation) {
                append(", $firstStation — $lastStation")
            }
            append("\n")
            append(url)
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    fun manualSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val userId = secureTokenStorage.getUserIdFlow().first()
            if (token.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        showSyncDialog = true,
                        syncType = com.z_company.route.viewmodel.SyncType.Upload,
                        syncUploadProgress = mapOf(
                            "UserSettings" to com.z_company.route.viewmodel.SyncStepState.Error("Неавторизованный пользователь"),
                            "SalarySettings" to com.z_company.route.viewmodel.SyncStepState.Error(""),
                            "ReleaseDays" to com.z_company.route.viewmodel.SyncStepState.Error(""),
                            "Routes" to com.z_company.route.viewmodel.SyncStepState.Error("")
                        ),
                        isSyncComplete = true
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    showSyncDialog = true,
                    syncType = com.z_company.route.viewmodel.SyncType.Upload,
                    syncUploadProgress = mapOf(
                        "UserSettings" to com.z_company.route.viewmodel.SyncStepState.Loading,
                        "SalarySettings" to com.z_company.route.viewmodel.SyncStepState.Loading,
                        "ReleaseDays" to com.z_company.route.viewmodel.SyncStepState.Loading,
                        "Routes" to com.z_company.route.viewmodel.SyncStepState.Loading
                    ),
                    isSyncComplete = false,
                    isSyncSuccess = false,
                    syncReportUserId = userId
                )
            }
            var networkErrorStopped = false
            try {
                syncManager.syncToRemote("Bearer $token").collect { state ->
                    if (networkErrorStopped) return@collect
                    when (state) {
                        is ResultState.Loading -> {}
                        is ResultState.Success -> {
                            val result = state.data
                            val newProgress = _uiState.value.syncUploadProgress.toMutableMap()
                            if (result.userSettingsSaved) newProgress["UserSettings"] = com.z_company.route.viewmodel.SyncStepState.Success("загружены")
                            if (result.salarySettingsSaved) newProgress["SalarySettings"] = com.z_company.route.viewmodel.SyncStepState.Success("загружены")
                            if (result.releaseDaysSaved) newProgress["ReleaseDays"] = com.z_company.route.viewmodel.SyncStepState.Success("загружены")
                            val routeErrors = result.routeErrors
                            if (result.routesSavedCount >= 0) {
                                if (routeErrors.isNotEmpty()) {
                                    newProgress["Routes"] = com.z_company.route.viewmodel.SyncStepState.Error("синхронизировано ${result.routesSavedCount} из ${routeErrors.size + result.routesSavedCount}")
                                } else {
                                    val details = buildString {
                                        append("загружены ${result.routesSavedCount}(шт)")
                                        if (result.routeWarnings.isNotEmpty()) append("\n${result.routeWarnings.joinToString("\n")}")
                                    }
                                    newProgress["Routes"] = com.z_company.route.viewmodel.SyncStepState.Success(details)
                                }
                            }
                            val isFullSuccess = result.timestamp != null && routeErrors.isEmpty()
                            _uiState.update {
                                it.copy(
                                    syncUploadProgress = newProgress,
                                    isSyncComplete = !isFullSuccess,
                                    isSyncSuccess = isFullSuccess,
                                    showSyncDialog = !isFullSuccess,
                                    syncRouteErrors = routeErrors,
                                    syncRoutesTotalAttempted = routeErrors.size + result.routesSavedCount,
                                    syncRoutesSavedCount = result.routesSavedCount
                                )
                            }
                            result.timestamp?.let { sharedPreferenceStorage.setLastSyncTimestamp(it) }
                        }
                        is ResultState.Error -> {
                            val msg = state.entity.message ?: ""
                            val cleanMsg = cleanSyncError(msg)
                            if (isNetworkErrorMessage(cleanMsg)) {
                                networkErrorStopped = true
                                _uiState.update { it.copy(isNetworkError = true, isSyncComplete = true) }
                                return@collect
                            }
                            val stepKey = parseSyncStep(msg)
                            val newProgress = _uiState.value.syncUploadProgress.toMutableMap()
                            if (stepKey != null) {
                                newProgress[stepKey] = com.z_company.route.viewmodel.SyncStepState.Error(message = cleanMsg)
                                _uiState.update { it.copy(syncUploadProgress = newProgress) }
                            } else {
                                newProgress.replaceAll { _, v ->
                                    if (v is com.z_company.route.viewmodel.SyncStepState.Loading)
                                        com.z_company.route.viewmodel.SyncStepState.Error(message = msg)
                                    else v
                                }
                                _uiState.update { it.copy(syncUploadProgress = newProgress, isSyncComplete = true) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                val newProgress = _uiState.value.syncUploadProgress.mapValues {
                    if (it.value is com.z_company.route.viewmodel.SyncStepState.Loading)
                        com.z_company.route.viewmodel.SyncStepState.Error(message = e.message ?: "Ошибка синхронизации")
                    else it.value
                }
                _uiState.update { it.copy(syncUploadProgress = newProgress, isSyncComplete = true) }
            }
        }
    }

    fun resetSyncState() {
        syncJob?.cancel()
        _uiState.update {
            it.copy(
                showSyncDialog = false,
                syncUploadProgress = emptyMap(),
                isSyncComplete = false,
                isSyncSuccess = false,
                isNetworkError = false,
                syncType = null,
                syncRouteErrors = emptyList(),
                syncRoutesTotalAttempted = 0,
                syncRoutesSavedCount = 0,
                syncReportUserId = null
            )
        }
    }

    private fun isNetworkErrorMessage(msg: String): Boolean =
        msg.contains("Нет соединения", ignoreCase = true) ||
        msg.contains("Unable to resolve", ignoreCase = true) ||
        msg.contains("Connection refused", ignoreCase = true) ||
        msg.contains("timeout", ignoreCase = true) ||
        msg.contains("Failed to connect", ignoreCase = true) ||
        msg.contains("Network is unreachable", ignoreCase = true) ||
        msg.contains("ECONNREFUSED", ignoreCase = true)
    private fun parseSyncStep(message: String): String? = when {
        message.contains("UserSettings") -> "UserSettings"
        message.contains("SalarySetting") -> "SalarySettings"
        message.contains("отвлечений") -> "ReleaseDays"
        else -> null
    }

    private fun cleanSyncError(message: String): String {
        val prefixes = listOf(
            "Ошибка сохранения UserSettings: ",
            "Ошибка сохранения SalarySetting: ",
            "Ошибка сохранения дней отвлечений: "
        )
        for (prefix in prefixes) {
            if (message.startsWith(prefix)) return message.removePrefix(prefix)
        }
        return message
    }

    private fun calculationTotalTime(routes: List<Route>, context: TimeCalculationContext) {
        _uiState.update {
            it.copy(
                totalTimeWithHoliday = ResultState.Loading()
            )
        }
        try {
            val stateSettings = uiState.value.settingState
            if (stateSettings is ResultState.Success) {
                stateSettings.data?.let { settings ->
                    val totalTime = routes.getWorkTime(settings.selectMonthOfYear, context)
                    _uiState.update {
                        it.copy(
                            totalTimeWithHoliday = ResultState.Success(totalTime)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationTotalTime")
            _uiState.update {
                it.copy(
                    totalTimeWithHoliday = ResultState.Error(ErrorEntity(e))
                )
            }
        }
    }

    private fun pushWidgetData(
        routeCount: Int,
        fullRouteList: List<Route>,
        userSettings: UserSettings,
        currentTimeInMillis: Long
    ) {
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

                val normHours = if (monthOfYear != null) {
                    "${monthOfYear.getPersonalNormaHours()}ч"
                } else ""

                val route = currentRoute
                val hasCurrentRoute = route != null
                val isDeparture = isNextDeparture()

                val dateAndTimeConverter = DateAndTimeConverter(userSettings)
                val reportTime = if (hasCurrentRoute) {
                    dateAndTimeConverter.getDateMiniAndTime(route?.basicData?.timeStartWork)
                } else ""

                // Button info (train number, status, time — under play/stop button)
                val buttonInfo = computeButtonInfo(route, dateAndTimeConverter)

                // Norm remaining / overtime
                val normHoursInt = monthOfYear?.getPersonalNormaHours() ?: 0
                val normMillis = normHoursInt.toLong() * 3_600_000L
                val diff = totalTimeMillis - normMillis
                val isOvertime = diff >= 0
                val remainingMillis = if (isOvertime) diff else -diff
                val normRemainingText = if (normHoursInt > 0) {
                    convertTimeToStringFormat(remainingMillis)
                } else ""

                // State info lines
                val stateInfo = computeStateInfo(
                    hasCurrentRoute = hasCurrentRoute,
                    currentRoute = route,
                    allRoutes = fullRouteList,
                    currentTimeInMillis = currentTimeInMillis,
                    userSettings = userSettings,
                    dateAndTimeConverter = dateAndTimeConverter
                )

                // Next report text
                val futureRoute = allRoutesGlobal.findNextFutureRoute(currentTimeInMillis)
                val nextReportText = if (futureRoute != null) {
                    "След. явка ${dateAndTimeConverter.getDateMiniAndTime(futureRoute.basicData.timeStartWork)}"
                } else "След. явка неизвестна"

                widgetUpdater.update(
                    totalTimeText = totalTimeText,
                    normHours = normHours,
                    monthYear = monthYear,
                    hasCurrentRoute = hasCurrentRoute,
                    reportTime = reportTime,
                    isDepartureNext = isDeparture,
                    lastActionText = "",
                    stateInfoLine1 = stateInfo.line1,
                    stateInfoLine2 = stateInfo.line2,
                    stateInfoLine3 = stateInfo.line3,
                    stateInfoLine4 = stateInfo.line4,
                    stateInfoLine5 = stateInfo.line5,
                    nextReportText = nextReportText,
                    normRemainingText = normRemainingText,
                    isOvertime = isOvertime,
                    trainNumberText = buttonInfo.trainNumber,
                    statusText = buttonInfo.statusText,
                    statusTimeText = buttonInfo.statusTime
                )
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "pushWidgetData")
                Log.w("HomeViewModel", "Widget update failed", e)
            }
        }
    }

    /** Button info: train number, status text, status time */
    private data class ButtonInfo(
        val trainNumber: String,  // "п. №3" or ""
        val statusText: String,   // "В пути" / "Стоянка" or ""
        val statusTime: String    // "с 13:45" or ""
    )

    /** Compute button info from last train: train number, status, time */
    private fun computeButtonInfo(
        route: Route?,
        dateAndTimeConverter: DateAndTimeConverter
    ): ButtonInfo {
        val lastTrain = route?.trains?.lastOrNull()
            ?: return ButtonInfo("", "", "")
        val stations = lastTrain.stations
        if (stations.isEmpty()) return ButtonInfo("", "", "")

        val trainNumber = lastTrain.number?.let { "п. №$it" } ?: ""

        val hasServicePhase = lastTrain.servicePhase != null
        val endIdx = if (hasServicePhase && stations.size >= 2)
            stations.lastIndex - 1 else stations.lastIndex

        for (i in endIdx downTo 0) {
            val s = stations[i]
            if (s.timeDeparture != null) {
                return ButtonInfo(
                    trainNumber = trainNumber,
                    statusText = "В пути",
                    statusTime = "с ${dateAndTimeConverter.getTime(s.timeDeparture)}"
                )
            }
            if (s.timeArrival != null) {
                return ButtonInfo(
                    trainNumber = trainNumber,
                    statusText = "Стоянка",
                    statusTime = "с ${dateAndTimeConverter.getTime(s.timeArrival)}"
                )
            }
        }
        return ButtonInfo(trainNumber, "", "")
    }

    /** State info lines (up to 5 lines for turnaround rest). */
    private data class WidgetStateInfo(
        val line1: String,
        val line2: String,
        val line3: String,
        val line4: String = "",
        val line5: String = ""
    )

    /** Compute state info: report time / rest info / empty. */
    private fun computeStateInfo(
        hasCurrentRoute: Boolean,
        currentRoute: Route?,
        allRoutes: List<Route>,
        currentTimeInMillis: Long,
        userSettings: UserSettings,
        dateAndTimeConverter: DateAndTimeConverter
    ): WidgetStateInfo {
        // State 1: Current route — show report time
        if (hasCurrentRoute && currentRoute != null) {
            val reportText =
                "Текущая явка ${dateAndTimeConverter.getDateMiniAndTime(currentRoute.basicData.timeStartWork)}"
            return WidgetStateInfo(reportText, "", "")
        }

        // State 2: No current route — find previous completed route
        val previousRoute = allRoutes
            .filter {
                it.basicData.timeEndWork != null &&
                    it.basicData.timeEndWork!! < currentTimeInMillis &&
                    it.basicData.timeStartWork != null
            }
            .maxByOrNull { it.basicData.timeEndWork ?: 0L }

        if (previousRoute != null) {
            val startWork = previousRoute.basicData.timeStartWork!!
            val endWork = previousRoute.basicData.timeEndWork!!
            val workTime = endWork - startWork

            if (previousRoute.basicData.restPointOfTurnover) {
                // Turnaround rest
                val minTime = userSettings.minTimeRestPointOfTurnover
                val shortRest = maxOf(workTime / 2, minTime)
                val fullRest = maxOf(workTime, minTime)
                val shortDuration =
                    "Короткий ${ConverterLongToTime.formatDurationFromMillis(shortRest)}"
                val shortEnd =
                    "до ${dateAndTimeConverter.getDateMiniAndTime(endWork + shortRest)}"
                val fullDuration =
                    "Полный ${ConverterLongToTime.formatDurationFromMillis(fullRest)}"
                val fullEnd =
                    "до ${dateAndTimeConverter.getDateMiniAndTime(endWork + fullRest)}"
                return WidgetStateInfo("Отдых в ПО", shortDuration, shortEnd, fullDuration, fullEnd)
            } else {
                // Home rest (simplified — single route, no chain)
                val rawDuration = (workTime.toDouble() * 2.6).toLong()
                val duration = maxOf(rawDuration, userSettings.minTimeHomeRest)
                val endRestTime = endWork + duration
                val durationLine =
                    "Продлится ${ConverterLongToTime.formatDurationFromMillis(duration)}"
                val endLine = "До ${dateAndTimeConverter.getDateMiniAndTime(endRestTime)}"
                return WidgetStateInfo("Домашний отдых", durationLine, endLine)
            }
        }

        // State 3: No routes at all
        return WidgetStateInfo("", "", "")
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
            routeHelper.calculationHomeRest(
                route = route,
            ).collect { result ->
                when (result) {
                    is ResultState.Success -> {
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
        viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.getListRoutesAsFlow().collect { allRoutes ->
                allRoutesGlobal = allRoutes
                val unsyncedCount = allRoutes.count { !it.basicData.isSynchronized }
                _uiState.update { it.copy(unsyncedRoutesCount = unsyncedCount) }
            }
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
                            timeCalculationContext = TimeCalculationContext.from(userSettings),
                            dateAndTimeConverter = dateAndTimeConverter,
                            minTimeRest = userSettings.minTimeRestPointOfTurnover,
                            minTimeHomeRest = userSettings.minTimeHomeRest,
                        )
                    }

                    // Update params that drive routesFlow. flatMapLatest on routesFlow will switch to the new month/timezone.
                    routeParams.value = userSettings.selectMonthOfYear to TimeCalculationContext.from(userSettings)
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
                        cachedRouteList = fullRouteList
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
                                        ),
                                        isLongCompositionTrain = isLongCompositionTrain(route)
                                    )
                                    routeStateList.add(routeState)
                                }
                            }

                            currentRoute = fullRouteList.findCurrentRoute(
                                currentTimeInMillis = currentTimeInMillis,
                                userSettings = userSettings
                            )

                            if (currentRoute != null) {
                                workTimer(currentRoute!!.basicData.timeStartWork!!)
                                nextFutureRoute = null
                                countdownTimerJob?.cancel()
                            } else {
                                nextFutureRoute = allRoutesGlobal.findNextFutureRoute(currentTimeInMillis)
                                nextFutureRoute?.basicData?.timeStartWork?.let { startWork ->
                                    countdownTimer(startWork)
                                }
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

                            isConsiderFutureRoute = userSettings.isConsiderFutureRoute
                            if (userSettings.isConsiderFutureRoute) {
                                // Суммируем отработанное время от начала месяца до текущего момента:
                                // только маршруты, у которых timeEndWork уже наступило.
                                // Используем ту же getWorkTime(monthOfYear) что и для месячного итога
                                // — корректно обрабатывает переходные маршруты.
                                currentMonthOfYear?.let { monthOfYear ->
                                    val completedRoutes = result.data.filter { route ->
                                        val end = route.basicData.timeEndWork ?: return@filter false
                                        end <= currentTimeInMillis
                                    }
                                    todayWorkTime = completedRoutes.getWorkTime(
                                        monthOfYear,
                                        TimeCalculationContext.from(userSettings)
                                    )
                                }
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

                                    val calcContext = TimeCalculationContext.from(userSettings)
                                    calculationOfOnePersonOperationTime(
                                        filteredRouteList,
                                        userSettings
                                    )
                                    calculationTotalTime(filteredRouteList, calcContext)
                                    calculationOfTimeWithoutHoliday(
                                        filteredRouteList,
                                        calcContext
                                    )
                                    calculationOfNightTime(filteredRouteList, userSettings)
                                    calculationOfSingleLocomotiveTime(filteredRouteList)
                                    calculationPassengerTime(
                                        filteredRouteList,
                                        calcContext
                                    )
                                    calculationHolidayTime(filteredRouteList, calcContext)
                                }
                                // Update widget after all calculations complete
                                pushWidgetData(
                                    routeCount = filteredRouteList.size,
                                    fullRouteList = fullRouteList,
                                    userSettings = userSettings,
                                    currentTimeInMillis = currentTimeInMillis
                                )
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