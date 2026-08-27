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
import com.z_company.core.util.friendlyNetworkErrorMessage
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.toMoneyString
import com.z_company.route.viewmodel.computeRouteTotalPayment
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
import com.z_company.domain.entities.route.UtilsForEntities.filterByConsiderFutureRoute
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
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.NetworkErrorMapper
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability
data class OpenRouteFormEvent(val basicId: String?, val isMakeCopy: Boolean)

/** Тип отдыха между маршрутами для блока состояния на главном экране. */
enum class RestBlockType { POINT_OF_TURNOVER, HOME }

/**
 * Состояние блока отдыха на главном экране. Времена — абсолютные (ms epoch),
 * живые счётчики («Отдыхаете», «осталось») экран считает от текущего времени.
 *
 * @param restStart      время начала отдыха = сдача предыдущего маршрута.
 * @param shortRestEnd   конец короткого отдыха (только для [RestBlockType.POINT_OF_TURNOVER]).
 * @param fullRestEnd    конец полного отдыха (для дома — единственная граница).
 */
data class RestBlockState(
    val type: RestBlockType,
    val restStart: Long,
    val shortRestEnd: Long?,
    val fullRestEnd: Long,
    val minRestEnd: Long? = null,
)

class HomeViewModel : ViewModel(), KoinComponent {
    private val timeManager = TimeManager()
    private val routeUseCase: RouteUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val normaUseCase: NormaUseCase by inject()
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

    // Событие с готовыми текстом и темой для "Поделиться" — экран открывает share-sheet.
    private val _shareLinkEvent = MutableSharedFlow<com.z_company.route.util.ShareLinkData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shareLinkEvent: SharedFlow<com.z_company.route.util.ShareLinkData> = _shareLinkEvent.asSharedFlow()

    private val _isCreatingShareLink = MutableStateFlow(false)
    val isCreatingShareLink: StateFlow<Boolean> = _isCreatingShareLink.asStateFlow()

    private val _workTimeInCurrentRoute = MutableSharedFlow<Long>(replay = 1)
    val workTimeInCurrentRoute = _workTimeInCurrentRoute.asSharedFlow()

    var nextFutureRoute by mutableStateOf<Route?>(null)

    // Блок отдыха (в ПО / домашний) для главного экрана. Кандидат: экран сам решает,
    // показывать ли, с учётом приоритета (см. HomeScreen) и живого времени.
    var restBlockState by mutableStateOf<RestBlockState?>(null)
        private set

    // Одноразовый таймер: когда полный отдых заканчивается при открытом приложении —
    // пересчитывает состояние (следующий маршрут / пусто), чтобы блок исчез сам.
    private var restTransitionJob: Job? = null

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

    // Отсортированный список доступных (год, месяц) — для переключения месяца стрелками.
    private val _monthYearList = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val monthYearList: StateFlow<List<Pair<Int, Int>>> = _monthYearList.asStateFlow()

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
            val subscriptionEndTime = value?.subscriptionPeriod ?: 0L
            val subscriptionActive = subscriptionEndTime > System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    settingState = ResultState.Success(value),
                    hasActiveSubscription = subscriptionActive,
                    subscriptionEndTime = subscriptionEndTime
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

        // Запускаем обратный отсчёт до следующего маршрута.
        // Ищем по всем месяцам (allRoutesGlobal), а не по выбранному —
        // блок «Следующий маршрут» не должен зависеть от выбранного месяца.
        val next = allRoutesGlobal.findNextFutureRoute(currentTimeInMillis)
        nextFutureRoute = next
        next?.basicData?.timeStartWork?.let { startWork ->
            countdownTimer(startWork)
        }

        // Маршрут завершился при открытом приложении — мог начаться отдых.
        viewModelScope.launch { recomputeRestBlock(allRoutesGlobal) }
    }

    /**
     * Пересчитывает [currentRoute] и [nextFutureRoute] от полного списка маршрутов
     * [allRoutes] (все месяцы). Вызывается реактивно при любом изменении маршрутов
     * (getListRoutesAsFlow), чтобы блоки «Текущий/Следующий маршрут» на главном экране
     * мгновенно отражали ситуацию.
     *
     * Раньше эти поля пересчитывались только в routesFlow.collect, который реагирует на
     * список маршрутов ВЫБРАННОГО месяца. Из-за этого удаление маршрута из другого месяца
     * (например «следующего») не убирало блок до повторного входа на экран, а внутри
     * выбранного месяца был race с обновлением allRoutesGlobal в отдельном коллекторе.
     *
     * Идемпотентна: секундомер (workTimer) и обратный отсчёт (countdownTimer)
     * перезапускаются только при реальной смене маршрута (по id или timeStartWork), иначе
     * они сбрасывались бы на каждом эмите БД.
     */
    private fun updateCurrentAndNextRoute(allRoutes: List<Route>) {
        val userSettings = currentUserSetting ?: return
        val tz = TimeZone.getTimeZone(DateAndTimeConverter(userSettings).timeZoneText)
        val currentTimeInMillis = getInstance(tz).timeInMillis

        val prevCurrent = currentRoute
        val newCurrent = allRoutes.findCurrentRoute(currentTimeInMillis, userSettings)
        currentRoute = newCurrent

        if (newCurrent != null) {
            val currentChanged =
                prevCurrent?.basicData?.id != newCurrent.basicData.id ||
                    prevCurrent?.basicData?.timeStartWork != newCurrent.basicData.timeStartWork
            if (currentChanged) {
                newCurrent.basicData.timeStartWork?.let { workTimer(it) }
            }
            nextFutureRoute = null
            countdownTimerJob?.cancel()
        } else {
            // Текущего маршрута нет — останавливаем секундомер, если он шёл
            if (prevCurrent != null) timerJob?.cancel()

            val prevNext = nextFutureRoute
            val newNext = allRoutes.findNextFutureRoute(currentTimeInMillis)
            nextFutureRoute = newNext
            if (newNext != null) {
                val nextChanged =
                    prevNext?.basicData?.id != newNext.basicData.id ||
                        prevNext?.basicData?.timeStartWork != newNext.basicData.timeStartWork
                if (nextChanged) {
                    newNext.basicData.timeStartWork?.let { countdownTimer(it) }
                }
            } else {
                countdownTimerJob?.cancel()
                _countdownToNextRoute.tryEmit(0L)
            }
        }
    }

    /**
     * Пересчитывает [restBlockState] от полного списка маршрутов [allRoutes].
     *
     * Определяет последний завершённый маршрут и по его признаку
     * [BasicData.restPointOfTurnover] выбирает тип отдыха:
     *  - true  → отдых в пункте оборота (короткий + полный),
     *  - false → домашний отдых (одна граница — полный отдых).
     *
     * Расчёт короткого/полного отдыха делегируется в [RouteActionsHelper] (те же формулы,
     * что и в шторке отдыха), чтобы значения совпадали по всему приложению.
     *
     * Границы показа блока:
     *  - Отдых в ПО длится до явки следующего (обратного) маршрута — короткий/полный
     *    отдых лишь маркеры внутри окна. Если следующего маршрута нет — до конца полного
     *    отдыха.
     *  - Домашний отдых — до конца полного (рекомендованного) отдыха.
     *
     * Если открыт текущий маршрут — отдых не показываем.
     */
    private suspend fun recomputeRestBlock(allRoutes: List<Route>) {
        restTransitionJob?.cancel()

        if (currentRoute != null) {
            restBlockState = null
            return
        }

        val userSettings = currentUserSetting
        val tz = if (userSettings != null) {
            TimeZone.getTimeZone(DateAndTimeConverter(userSettings).timeZoneText)
        } else {
            TimeZone.getDefault()
        }
        val now = getInstance(tz).timeInMillis

        val previous = allRoutes
            .filter {
                val end = it.basicData.timeEndWork
                it.basicData.timeStartWork != null && end != null && end < now
            }
            .maxByOrNull { it.basicData.timeEndWork ?: 0L }

        if (previous == null) {
            restBlockState = null
            return
        }

        val restStart = previous.basicData.timeEndWork ?: run {
            restBlockState = null
            return
        }

        // Граница окна отдыха — до неё блок показывается, по её достижении скрывается.
        val boundary: Long?
        val newState: RestBlockState? = if (previous.basicData.restPointOfTurnover) {
            val shortEnd = routeHelper.calculateShortRest(previous).first()?.second
            val fullEnd = routeHelper.calculateFullRest(previous).first()?.second
            // Отдых в ПО — до явки следующего (обратного) маршрута; если его нет —
            // до конца полного отдыха.
            val nextStart = allRoutes.findNextFutureRoute(now)?.basicData?.timeStartWork
            boundary = nextStart ?: fullEnd
            if (boundary == null || now >= boundary) null
            else RestBlockState(
                type = RestBlockType.POINT_OF_TURNOVER,
                restStart = restStart,
                shortRestEnd = shortEnd,
                fullRestEnd = fullEnd ?: boundary,
            )
        } else {
            val homeResult = routeHelper.calculationHomeRest(previous)
                .first { it is ResultState.Success || it is ResultState.Error }
            val homeCalculation = (homeResult as? ResultState.Success)?.data
            val fullEnd = homeCalculation?.endTime
            boundary = fullEnd
            if (fullEnd == null || now >= fullEnd) null
            else RestBlockState(
                type = RestBlockType.HOME,
                restStart = restStart,
                shortRestEnd = null,
                fullRestEnd = fullEnd,
                minRestEnd = homeCalculation?.minEndTime,
            )
        }

        restBlockState = newState

        // Планируем автоскрытие блока в момент достижения границы окна отдыха.
        if (newState != null && boundary != null) {
            val delayMs = boundary - now
            if (delayMs > 0) {
                restTransitionJob = viewModelScope.launch {
                    delay(delayMs)
                    updateCurrentAndNextRoute(allRoutesGlobal)
                    recomputeRestBlock(allRoutesGlobal)
                }
            }
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
                    // Время явки следующего маршрута наступило — он становится текущим.
                    // Пересчитываем блоки в отдельной корутине (текущая — countdownTimerJob —
                    // будет отменена внутри updateCurrentAndNextRoute), чтобы «Следующий маршрут»
                    // сразу же сменился на «Текущий маршрут» без ожидания эмита из БД.
                    viewModelScope.launch {
                        updateCurrentAndNextRoute(allRoutesGlobal)
                        recomputeRestBlock(allRoutesGlobal)
                    }
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
            // Секунды сохраняем (не обрезаем до минуты) — для точного старта секундомера.
            val now = timeManager.nowExact()

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
            val dayOffHours = currentMonthOfYear.getDayoffHours(
                sharedPreferenceStorage.getWorkScheduleProfile()
            )
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
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val passengerTime = routes.getPassengerTime(monthOfYear, context)
                _uiState.update {
                    it.copy(passengerTimeInRouteList = ResultState.Success(passengerTime))
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationPassengerTime")
            _uiState.update {
                it.copy(passengerTimeInRouteList = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private suspend fun calculationOfNightTime(routes: List<Route>, settings: UserSettings) {
        try {
            val nightTimeState = routes.getNightTime(settings)
            _uiState.update {
                it.copy(nightTimeInRouteList = ResultState.Success(nightTimeState))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfNightTime")
            _uiState.update {
                it.copy(nightTimeInRouteList = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    private suspend fun calculationOfSingleLocomotiveTime(routes: List<Route>) {
        try {
            val timeState = routes.getSingleLocomotiveTime()
            _uiState.update {
                it.copy(singleLocomotiveTimeState = ResultState.Success(timeState))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfSingleLocomotiveTime")
            _uiState.update {
                it.copy(singleLocomotiveTimeState = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    private suspend fun calculationOfExtendedServicePhaseTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        try {
            val timeState =
                salaryCalculationHelper.getTotalTimeSurchargeServicePhaseFlow().first()
            _uiState.update {
                it.copy(extendedServicePhaseTime = ResultState.Success(timeState))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfExtendedServicePhaseTime")
            _uiState.update {
                it.copy(extendedServicePhaseTime = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    private suspend fun calculationOfOnePersonOperationTime(
        routes: List<Route>, userSettings: UserSettings
    ) {
        val context = TimeCalculationContext.from(userSettings)
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val passengerTime = routes.getOnePersonOperationTimePassengerTrain(
                    monthOfYear, context
                )
                val time = routes.getOnePersonOperationTime(monthOfYear, context)
                val resultTIme = time + passengerTime
                _uiState.update {
                    it.copy(onePersonOperationTime = ResultState.Success(resultTIme))
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfOnePersonOperationTime")
            _uiState.update {
                it.copy(onePersonOperationTime = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    private suspend fun calculationOfLongDistanceTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        try {
            val timeState = salaryCalculationHelper.getTotalTimeLongTrainsFlow().first()
            _uiState.update {
                it.copy(longDistanceTrainsTime = ResultState.Success(timeState))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfLongDistanceTrainsTime")
            _uiState.update {
                it.copy(longDistanceTrainsTime = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    private suspend fun calculationOfHeavyTrainsTime(
        salaryCalculationHelper: SalaryCalculationHelper
    ) {
        try {
            val timeState = salaryCalculationHelper.getTotalTimeHeavyTrainsFlow().first()
            _uiState.update {
                it.copy(heavyTrainsTime = ResultState.Success(timeState))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationOfHeavyTrainsTime")
            _uiState.update {
                it.copy(heavyTrainsTime = ResultState.Error(ErrorEntity(e)))
            }
        }
    }


    private suspend fun calculationHolidayTime(routes: List<Route>, context: TimeCalculationContext) {
        try {
            currentMonthOfYear?.let { monthOfYear ->
                val holidayTime =
                    routes.getWorkingTimeOnAHoliday(monthOfYear, context).first()
                _uiState.update {
                    it.copy(holidayHours = ResultState.Success(holidayTime))
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationHolidayTime")
            _uiState.update {
                it.copy(nightTimeInRouteList = ResultState.Error(ErrorEntity(e)))
            }
        }
    }

    /**
     * Job для recalc-эффекта от изменения salarySetting.
     * Cancel старого + start нового при каждом эмите — debounce-like поведение.
     */
    private var recalcOnSalarySettingChangeJob: Job? = null

    /**
     * Запускает все calculation-функции с текущими routeList/settings.
     * Вызывается:
     *  1. После загрузки новых маршрутов из БД (routesFlow.collect)
     *  2. При изменении salarySetting/userSettings (реактивный пересчёт)
     *
     * Использует Dispatchers.Default + coroutineScope для параллельного запуска.
     * Не блокирует UI — calculations занимают ~50-200ms на DefaultDispatcher.
     */
    private fun runAllCalculations(
        fullRouteList: List<Route>,
        userSettings: UserSettings,
        salarySetting: SalarySetting,
        currentTimeInMillis: Long,
    ) {
        // Единая логика фильтрации: см. UtilsForEntities.filterByConsiderFutureRoute
        val filteredRouteList = fullRouteList.filterByConsiderFutureRoute(
            isConsiderFutureRoute = userSettings.isConsiderFutureRoute,
            currentTimeInMillis = currentTimeInMillis,
        )

        isConsiderFutureRoute = userSettings.isConsiderFutureRoute
        if (userSettings.isConsiderFutureRoute) {
            currentMonthOfYear?.let { monthOfYear ->
                val completedRoutes = fullRouteList.filter { route ->
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
            allRoutes = filteredRouteList,
            workScheduleProfile = sharedPreferenceStorage.getWorkScheduleProfile(),
        )

        viewModelScope.launch(Dispatchers.Default) {
            val calcContext = TimeCalculationContext.from(userSettings)
            coroutineScope {
                launch { calculationOfExtendedServicePhaseTime(salaryCalculationHelper) }
                launch { calculationOfLongDistanceTrainsTime(salaryCalculationHelper) }
                launch { calculationOfHeavyTrainsTime(salaryCalculationHelper) }
                launch { calculationOfOnePersonOperationTime(filteredRouteList, userSettings) }
                launch { calculationOfNightTime(filteredRouteList, userSettings) }
                launch { calculationOfSingleLocomotiveTime(filteredRouteList) }
                launch { calculationHolidayTime(filteredRouteList, calcContext) }
                launch { calculationToBeCredited(salaryCalculationHelper) }
                // Эти 3 функции синхронные/легкие — запускаем последовательно
                calculationTotalTime(filteredRouteList, calcContext)
                calculationOfTimeWithoutHoliday(filteredRouteList, calcContext)
                calculationPassengerTime(filteredRouteList, calcContext)
            }
            // Update widget after all calculations complete
            pushWidgetData(
                routeCount = filteredRouteList.size,
                fullRouteList = fullRouteList,
                userSettings = userSettings,
                currentTimeInMillis = currentTimeInMillis
            )
        }
    }

    /** Считает итоговую сумму зарплаты «К выдаче» — для отображения в TopAppBar HomeScreen. */
    private suspend fun calculationToBeCredited(helper: SalaryCalculationHelper) {
        try {
            val toBeCredited = helper.getMoneyToBeCredited().first()
            _uiState.update {
                it.copy(toBeCredited = ResultState.Success(toBeCredited))
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationToBeCredited")
            _uiState.update {
                it.copy(toBeCredited = ResultState.Error(ErrorEntity(e)))
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
            // Сначала авторизация: без токена отправлять маршрут бессмысленно —
            // сразу подсказываем войти в аккаунт и не идём в сеть.
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                snackbarManager.show(message = "Войдите в аккаунт, чтобы синхронизировать маршрут")
                return@launch
            }
            // Синхронизация — платная функция. Без активной подписки ручной
            // upload в облако запрещён (см. RouteActionsHelper.hasActiveSubscription).
            if (!routeHelper.hasActiveSubscription()) {
                snackbarManager.show(message = "Синхронизация доступна по подписке")
                return@launch
            }
            routesManager.saveRouteInRemote(route, "Bearer $token").collect { resultState ->
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
                            _shareLinkEvent.emit(
                                com.z_company.route.util.ShareLinkData.fromRoute(route, result.data)
                            )
                        }
                        is ResultState.Error -> {
                            val raw = result.entity.message ?: result.entity.throwable?.message
                            snackbarManager.show(
                                friendlyNetworkErrorMessage(raw, "Не удалось создать ссылку")
                            )
                        }
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "shareRoute")
                snackbarManager.show(
                    friendlyNetworkErrorMessage(e.message, "Не удалось создать ссылку")
                )
            } finally {
                _isCreatingShareLink.value = false
            }
        }
    }

    // buildShareText удалён — логика перенесена в ShareLinkData.fromRoute()

    private var syncJob: kotlinx.coroutines.Job? = null
    private var backgroundSyncJob: kotlinx.coroutines.Job? = null

    /** Тихо подтянуть изменения при каждом открытии главного экрана. */
    fun syncOnScreenOpen() {
        if (backgroundSyncJob?.isActive == true || syncJob?.isActive == true) return
        if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return
        backgroundSyncJob = viewModelScope.launch(Dispatchers.IO) {
            if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return@launch
            if (!routeHelper.hasActiveSubscription()) return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch
            _uiState.update { it.copy(isBackgroundSyncing = true) }
            try {
                var failureMessage: String? = null
                var pendingDeletionCount = 0
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> {
                            failureMessage = NetworkErrorMapper.syncFailureMessage(
                                state.entity.message,
                                state.entity.throwable,
                            )
                        }
                        is ResultState.Success -> {
                            pendingDeletionCount = state.data.pendingDeletionRouteIds.size
                        }
                        else -> {}
                    }
                }
                // Тихий фоновый sync не удаляет маршруты сам, если это значительный объём —
                // просто подсказываем, где подтвердить (см. ProfileViewModel.confirmPendingRouteDeletions).
                val message = failureMessage ?: if (pendingDeletionCount > 0) {
                    "На сервере пропало маршрутов: $pendingDeletionCount. Подтвердите удаление в Профиле."
                } else null
                message?.let { snackbarManager.show(it) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "syncOnScreenOpen")
                snackbarManager.show(NetworkErrorMapper.syncFailureMessage(e.message, e))
            } finally {
                _uiState.update { it.copy(isBackgroundSyncing = false) }
            }
        }
    }

    fun stopScreenSync() {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = null
        _uiState.update { it.copy(isBackgroundSyncing = false) }
    }

    fun manualSync() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch(Dispatchers.IO) {
            // Синхронизация — платная функция. Без активной подписки полный
            // upload в облако запрещён (тот же гейт, что в SyncWorker).
            if (!routeHelper.hasActiveSubscription()) {
                snackbarManager.show(message = "Синхронизация доступна по подписке")
                return@launch
            }
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

    // Экран «Нет интернета» — только для реальных транспортных ошибок; ответ
    // сервера с кодом ошибки (валидация 422, сбой 5xx) показывается как ошибка шага.
    private fun isNetworkErrorMessage(msg: String): Boolean =
        NetworkErrorMapper.isConnectivityMessage(msg)
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
        try {
            val stateSettings = uiState.value.settingState
            if (stateSettings is ResultState.Success) {
                stateSettings.data?.let { settings ->
                    val totalTime = routes.getWorkTime(settings.selectMonthOfYear, context)
                    _uiState.update {
                        it.copy(totalTimeWithHoliday = ResultState.Success(totalTime))
                    }
                }
            }
        } catch (e: Exception) {
            e.sendToSentry("HomeViewModel", "calculationTotalTime")
            _uiState.update {
                it.copy(totalTimeWithHoliday = ResultState.Error(ErrorEntity(e)))
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

                // Берём норму из NormaUseCase (актуальная, с регионом) если доступна,
                // иначе fallback на getPersonalNormaHours() из MonthOfYear
                val normaHoursInt = _uiState.value.normaHours
                    ?: monthOfYear?.getPersonalNormaHours()
                    ?: 0
                val normHours = if (normaHoursInt > 0) "${normaHoursInt}ч" else ""

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
                val normMillis = normaHoursInt.toLong() * 3_600_000L
                val diff = totalTimeMillis - normMillis
                val isOvertime = diff >= 0
                val remainingMillis = if (isOvertime) diff else -diff
                val normRemainingText = if (normaHoursInt > 0) {
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
                    _monthYearList.value = list
                        .map { it.year to it.month }
                        .distinct()
                        .sortedWith(compareBy({ it.first }, { it.second }))
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
                                homeRest = result.data?.endTime,
                                minHomeRest = result.data?.minEndTime
                            )
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    /** Фактический отдых до следующей явки — для быстрого просмотра. */
    fun calculationActualRest(route: Route?) {
        _previewRouteUiState.update { it.copy(actualRestDuration = null, actualRestUntil = null) }
        viewModelScope.launch {
            routeHelper.calculationActualRest(route).collect { result ->
                if (result is ResultState.Success) {
                    _previewRouteUiState.update {
                        it.copy(
                            actualRestDuration = result.data?.first,
                            actualRestUntil = result.data?.second,
                        )
                    }
                }
            }
        }
    }

    /**
     * Оплата за смену для быстрого просмотра на Главном (в AllRoute она уже есть
     * в `routePayments`, а здесь считаем по требованию). Считает даже для будущих
     * маршрутов, как и список «Все маршруты».
     */
    fun computeRoutePayment(route: Route?) {
        _previewRouteUiState.update { it.copy(paymentText = null) }
        val user = currentUserSetting ?: return
        val salary = currentSalarySetting ?: return
        if (route == null) return
        viewModelScope.launch(Dispatchers.Default) {
            val monthRoutesSorted = routeUseCase.getListRoutes()
                .sortedBy { it.basicData.timeStartWork ?: Long.MAX_VALUE }
            val total = try {
                computeRouteTotalPayment(route, user, salary, monthRoutesSorted)
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "computeRoutePayment")
                null
            }
            val text = total?.toMoneyString(currencySymbol(user.country))
            _previewRouteUiState.update { it.copy(paymentText = text) }
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

    /**
     * Реактивно наблюдает за нормой часов через NormaUseCase.
     * Пересчитывается при любом изменении выбранного месяца,
     * региональных праздников или дней отвлечений.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeNorma() {
        viewModelScope.launch {
            try {
                _uiState
                    .map { it.monthSelected }
                    .distinctUntilChanged()
                    .flatMapLatest { monthState ->
                        val month = (monthState as? ResultState.Success)?.data
                        if (month != null) {
                            normaUseCase.normaHoursFlow(month.year, month.month)
                                .map { it as Int? }
                        } else {
                            flowOf(null)
                        }
                    }
                    .collect { hours ->
                        _uiState.update { it.copy(normaHours = hours) }
                    }
            } catch (e: Exception) {
                e.sendToSentry("HomeViewModel", "observeNorma")
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
        observeNorma()
        viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.getListRoutesAsFlow().collect { allRoutes ->
                allRoutesGlobal = allRoutes
                val unsyncedCount = allRoutes.count { !it.basicData.isSynchronized }
                // Считаем отдельно (с учётом удалённых) — та же логика, что в
                // RouteActionsHelper.newRouteClick, чтобы индикатор бесплатного
                // периода совпадал с реальным лимитом.
                val freeRoutesUsed = routeHelper.freeRoutesUsedCount()
                _uiState.update {
                    it.copy(
                        unsyncedRoutesCount = unsyncedCount,
                        freeRoutesUsedCount = freeRoutesUsed
                    )
                }
                // Реактивно обновляем блоки «Текущий/Следующий маршрут» при любом
                // изменении маршрутов (удаление/добавление/правка) в любом месяце.
                updateCurrentAndNextRoute(allRoutes)
                recomputeRestBlock(allRoutes)
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

                    // Настройки могут прийти позже первого эмита getListRoutesAsFlow()
                    // (init запускает оба коллектора параллельно) — тогда
                    // updateCurrentAndNextRoute() уже отработал с currentUserSetting == null
                    // и вышел по раннему return, «Следующий маршрут» не посчитался и
                    // навсегда завис в null (следующий пересчёт — только при изменении
                    // списка маршрутов). Досчитываем здесь: обе функции идемпотентны.
                    updateCurrentAndNextRoute(allRoutesGlobal)
                    viewModelScope.launch { recomputeRestBlock(allRoutesGlobal) }

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
                    val newParams = userSettings.selectMonthOfYear to TimeCalculationContext.from(userSettings)
                    val isMonthOrTzChanged = routeParams.value != newParams
                    routeParams.value = newParams

                    // РЕАКТИВНЫЙ ПЕРЕСЧЁТ: если month/TZ не менялись, routesFlow не
                    // перезапустится → calculations не пересчитаются. Но если изменились
                    // настройки доплат (salarySetting) или isConsiderFutureRoute —
                    // пересчёт нужен. Используем cachedRouteList без повторной загрузки
                    // из БД.
                    if (!isMonthOrTzChanged) {
                        cachedRouteList?.let { routes ->
                            recalcOnSalarySettingChangeJob?.cancel()
                            recalcOnSalarySettingChangeJob = viewModelScope.launch {
                                kotlinx.coroutines.delay(150) // debounce от частых эмитов
                                val tz = java.util.TimeZone.getTimeZone(
                                    DateAndTimeConverter(userSettings).timeZoneText
                                )
                                runAllCalculations(
                                    fullRouteList = routes,
                                    userSettings = userSettings,
                                    salarySetting = currentSalarySetting!!,
                                    currentTimeInMillis = getInstance(tz).timeInMillis,
                                )
                            }
                        }
                    }
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
                                val offsetMs = userSettings.timeZone
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
                                        isLongCompositionTrain = isLongCompositionTrain(salarySetting, route),
                                        isFuture = route.isFuture(offsetMs),
                                        isTransition = route.isTransition(offsetMs)
                                    )
                                    routeStateList.add(routeState)
                                }
                            }

                            // Блоки «Текущий/Следующий маршрут» и «Отдых» НЕ зависят от
                            // выбранного месяца: их считает коллектор getListRoutesAsFlow()
                            // (все месяцы) через updateCurrentAndNextRoute()/recomputeRestBlock().
                            // Раньше здесь currentRoute/nextFutureRoute пересчитывались из
                            // fullRouteList выбранного месяца — при просмотре истории
                            // (прошлый месяц) это искажало состояние: текущий маршрут пропадал.
                            // Оставляем этот коллектор только для расчётов по выбранному месяцу.

//                            withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(
                                    listItemState = routeStateList.toList()
                                )
//                                }
                            }

                            // Запуск всех вычислений вынесен в отдельный метод —
                            // используется и здесь (новые маршруты), и при изменении
                            // salarySetting/userSettings (реактивный пересчёт без
                            // повторной загрузки маршрутов из БД).
                            runAllCalculations(
                                fullRouteList = fullRouteList,
                                userSettings = userSettings,
                                salarySetting = salarySetting,
                                currentTimeInMillis = currentTimeInMillis,
                            )
                        } else {
                            // settings not ready - update UI accordingly if needed
//                            withContext(Dispatchers.Main) {
                            _uiState.update {
                                it.copy(listItemState = emptyList())
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
