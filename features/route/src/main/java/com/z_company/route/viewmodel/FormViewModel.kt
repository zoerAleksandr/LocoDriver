package com.z_company.route.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.NightTime
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getOverRestTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.passengerTrainNumberList
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.PassengerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.TrainUseCase
import com.z_company.domain.util.CalculateNightTime
import com.z_company.domain.util.SharedRouteHolder
import com.z_company.domain.util.sum
import com.z_company.domain.util.toIntOrZero
import com.z_company.repository.SecureDataStore
import com.z_company.repository.remote_rest.SyncManager
import io.sentry.kotlin.multiplatform.Sentry
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.review.RuStoreReviewManagerFactory
import ru.rustore.sdk.review.model.ReviewInfo
import java.util.UUID

class FormViewModel(
    private val routeId: String?,
    private val isCopy: Boolean = false,
    private val application: Application,
) : AndroidViewModel(application), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val locoUseCase: LocomotiveUseCase by inject()
    private val trainUseCase: TrainUseCase by inject()
    private val passengerUseCase: PassengerUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val subscriptionHelper: SubscriptionHelper by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val shareRouteManager: com.z_company.repository.remote_rest.ShareRouteManager by inject()
    private val secureTokenStorage: com.z_company.repository.SecureTokenStorage by inject()
    private val syncManager: SyncManager by inject()

    val reviewManager = RuStoreReviewManagerFactory.create(application.applicationContext)

    // Основные состояния
    private val _uiState = MutableStateFlow(RouteFormUiState())
    val uiState: StateFlow<RouteFormUiState> = _uiState.asStateFlow()

    private val _salaryForRouteState = MutableStateFlow(SalaryForRouteState())
    val salaryForRouteState: StateFlow<SalaryForRouteState> = _salaryForRouteState.asStateFlow()

    private val _dialogRestUiState = MutableStateFlow(DialogRestUiState())
    val dialogRestUiState: StateFlow<DialogRestUiState> = _dialogRestUiState.asStateFlow()

    // Текущее route (реактивное)
    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute.asStateFlow()

    // События
    private val _events = MutableSharedFlow<FormScreenEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<FormScreenEvent> = _events.asSharedFlow()

    // Ссылка для "Поделиться" из overflow-меню
    private val _shareLinkEvent = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shareLinkEvent: SharedFlow<String> = _shareLinkEvent.asSharedFlow()

    private val _alertBeforePurchasesEvent = MutableSharedFlow<AlertBeforePurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val alertBeforePurchasesEvent: SharedFlow<AlertBeforePurchasesEvent> =
        _alertBeforePurchasesEvent.asSharedFlow()

    private val _purchasesEvent = MutableSharedFlow<StartPurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val purchasesEvent: SharedFlow<StartPurchasesEvent> = _purchasesEvent.asSharedFlow()

    // Дополнительные flows
    private val _holidayTime = MutableStateFlow<Long?>(null)
    val holidayTime: StateFlow<Long?> = _holidayTime.asStateFlow()

    private val _userSetting = MutableStateFlow<UserSettings?>(null)
    val userSetting: StateFlow<UserSettings?> = _userSetting.asStateFlow()

    private val _salarySetting = MutableStateFlow<SalarySetting?>(null)
    val salarySetting: StateFlow<SalarySetting?> = _salarySetting.asStateFlow()

    private val _dateAndTimeConverter = MutableStateFlow<DateAndTimeConverter?>(null)
    val dateAndTimeConverter: StateFlow<DateAndTimeConverter?> = _dateAndTimeConverter.asStateFlow()


    private val _showPassenger12hSheet = MutableStateFlow(false)
    val showPassenger12hSheet: StateFlow<Boolean> = _showPassenger12hSheet.asStateFlow()

    // Маршрут загружен по публичной ссылке и ещё не сохранён — показать шторку
    private val _isSharedPreview = MutableStateFlow(false)
    val isSharedPreview: StateFlow<Boolean> = _isSharedPreview.asStateFlow()

    fun dismissSharedPreviewSheet() { _isSharedPreview.value = false }

    /**
     * Дубль по timeStartWork при попытке сохранения.
     * Когда non-null — FormScreen показывает шторку «Маршрут с такой явкой уже сохранён»
     * с действиями «Заменить» / «Оставить оба» / «Отмена».
     * exitAfterSave — если true, после сохранения (из обоих действий) вызывается exit
     * (используется при сохранении shared-маршрута из шторки приветствия).
     */
    data class DuplicateRouteState(
        val existingRoute: Route,
        val exitAfterSave: Boolean
    )

    private val _duplicateRouteSheet = MutableStateFlow<DuplicateRouteState?>(null)
    val duplicateRouteSheet: StateFlow<DuplicateRouteState?> = _duplicateRouteSheet.asStateFlow()

    fun dismissDuplicateSheet() { _duplicateRouteSheet.value = null }

    var timeZoneText: String = "GMT+3"

    private var isNewRoute = routeId == NULLABLE_ID

    // true — маршрут уже записан в БД, автосейв разрешён
    // Для существующих маршрутов (не новых и не копий) — сразу true
    private var isPersistedToDb = !isNewRoute && !isCopy
    private var autoSaveJob: Job? = null

    private var loadRouteJob: Job? = null
    private var saveRouteJob: Job? = null
    private var loadSettingsJob: Job? = null
    private var deleteLocoJob: Job? = null
    private var deleteTrainJob: Job? = null
    private var deletePassengerJob: Job? = null

    private val deletedLocoList = mutableListOf<Locomotive>()
    private val deletedTrainList = mutableListOf<Train>()
    private val deletedPassengerList = mutableListOf<Passenger>()

    private var countLoadRoute = 0

    private var currentMonthOfYear: MonthOfYear? = null
    private var currentTimeZoneOffset: Long? = null
    private var nightTime: NightTime? = null
    private var defaultWorkTime: Long? = null
    private var usingDefaultWorkTime: Boolean = false

    init {
        initializeFlows()
        loadData()
    }

    // Инициализация реактивных flows
    private fun initializeFlows() {
        // Flow для настроек
        viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collectLatest { result ->
                if (result is ResultState.Success) {
                    _userSetting.value = result.data
                    _dateAndTimeConverter.value = DateAndTimeConverter(result.data)
                    timeZoneText = settingsUseCase.getTimeZone(result.data.timeZone)
                    currentMonthOfYear = result.data.selectMonthOfYear
                    currentTimeZoneOffset = result.data.timeZone
                    nightTime = result.data.nightTime
                    defaultWorkTime = result.data.defaultWorkTime
                    usingDefaultWorkTime = result.data.usingDefaultWorkTime
                }
            }
        }

        viewModelScope.launch {
            _salarySetting.value = salarySettingUseCase.salarySettingFlow().first()
        }

        // Комбинированный flow для всех вычислений (реактивно на изменения route и settings)
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                currentRoute,
                userSetting,
                salarySetting
            ) { route, settings, salSetting ->
                Triple(route, settings, salSetting)
            }.collectLatest { (route, settings, salSetting) ->
                if (route != null && settings != null && salSetting != null) {
                    val routeList = listOf(route)
                    val salaryCalculationHelper = SalaryCalculationHelper(
                        userSettings = settings,
                        salarySetting = salSetting,
                        routeList = routeList
                    )
                    coroutineScope {
                        launch { calculateSalary(salaryCalculationHelper, route, settings) }
                        launch { getNightTimeInRoute(route) }
                        launch { getHolidayTimeInRoute(route, settings) }
                        launch { calculationHomeRest(route) }
                        launch { getMinTimeRest(route) }
                        launch { getFullRest(route) }
                        launch { isValidTime(route) }
                        launch { getPassengerTime(route) }
                    }
                }
            }
        }
    }

    // Асинхронная загрузка данных
    private fun loadData() {
        loadRouteJob?.cancel()
        loadRouteJob = viewModelScope.launch(Dispatchers.IO) {
            // Проверяем: это маршрут импортированный по публичной ссылке (tentative preview)
            // Маршрут уже лежит в БД с isDeleted=true, грузится как обычно ниже.
            if (routeId != null && SharedRouteHolder.consume(routeId)) {
                _isSharedPreview.value = true
            }

            if (isNewRoute) {
                val newRoute = Route(basicData = BasicData(id = UUID.randomUUID().toString()))
                _currentRoute.value = newRoute
                // Сразу сохраняем в БД — автосейв будет работать с первого изменения поля
                performInitialSave(newRoute)
            } else {
                routeId?.let { id ->
                    routeUseCase.routeDetails(id).collectLatest { result ->
                        if (result is ResultState.Success) {
                            val loadedRoute = if (isCopy) result.data?.copy(
                                basicData = result.data!!.basicData.copy(
                                    id = UUID.randomUUID().toString()
                                )
                            ) else result.data
                            _uiState.update { it.copy(routeDetailState = result) }
                            countLoadRoute += 1
                            if (isCopy && countLoadRoute == 1) {
                                // Копия: сохраняем в БД сразу после загрузки, не ждём
                                // перехода в подразделы — пользователь может их не открывать
                                loadedRoute?.let { performInitialSave(it) }
                            }
                            if (countLoadRoute == 1) {
                                // Первая загрузка: устанавливаем весь маршрут из БД
                                _currentRoute.value = loadedRoute
                            } else {
                                // Возврат с подэкрана (Loco/Train/Passenger/Photo):
                                // обновляем только подразделы, BasicData берём из памяти —
                                // чтобы не затирать то, что пользователь ввёл в поля.
                                // changesHave() НЕ вызываем — это DB-событие, а не ввод пользователя,
                                // иначе возникает бесконечный цикл: autosave→DB emit→changesHave→autosave
                                loadedRoute?.let { dbRoute ->
                                    _currentRoute.update { inMemory ->
                                        inMemory?.copy(
                                            locomotives = dbRoute.locomotives,
                                            trains = dbRoute.trains,
                                            passengers = dbRoute.passengers,
                                            photos = dbRoute.photos
                                        ) ?: dbRoute
                                    }
                                }
                            }
                        } else {
                            _uiState.update { it.copy(routeDetailState = result) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Первичное сохранение нового маршрута в БД сразу при открытии FormScreen.
     * После успеха — подписываемся на изменения через Flow и включаем автосейв.
     * Не вызывает changesHave() — не помечает состояние «есть несохранённые правки».
     */
    private fun performInitialSave(route: Route) {
        viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.saveRoute(route).collect { result ->
                if (result is ResultState.Success) {
                    isPersistedToDb = true
                    subscribeToChanges(route.basicData.id)
                }
            }
        }
    }

    /** Автосейв с debounce 500 мс. Запускается только если маршрут уже в БД.
     *  Сохранение выполняется внутри autoSaveJob — не трогает saveRouteJob,
     *  который используется для явных сохранений (performSave, preSaveRoute). */
    private fun triggerAutoSave() {
        if (!isPersistedToDb) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            withContext(Dispatchers.IO) {
                currentRoute.value?.let { route ->
                    routeUseCase.saveRoute(route).collect { /* тихий автосейв */ }
                }
            }
        }
    }

    override fun onCleared() {
        autoSaveJob?.cancel()
        loadRouteJob?.cancel()
        saveRouteJob?.cancel()
        loadSettingsJob?.cancel()
        deleteLocoJob?.cancel()
        deleteTrainJob?.cancel()
        deletePassengerJob?.cancel()
        // Гарантированное финальное сохранение при уходе с экрана
        if (isPersistedToDb) {
            CoroutineScope(NonCancellable + Dispatchers.IO).launch {
                currentRoute.value?.let { route ->
                    routeUseCase.saveRoute(route).collect {}
                }
            }
        }
        super.onCleared()
    }

    // передаем время в нужном формате в зависимости от выбора пользователя
    fun convertTimeToStringFormat(timeToLong: Long?): String {
        userSetting.value?.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
        }
        return ConverterLongToTime.getTimeInStringFormat(timeToLong)
    }

    // Реактивные вычисления
    private suspend fun calculateSalary(
        salaryCalculationHelper: SalaryCalculationHelper,
        route: Route,
        setting: UserSettings
    ) {
        val workTime = route.getWorkTime()
        if (workTime == null) {
            _salaryForRouteState.update { it.copy(isCalculated = false) }
            return
        }

        val isSetTariffRate = setting.selectMonthOfYear.tariffRate != 0.0

        // Оборачиваем параллельные вычисления в coroutineScope для создания вложенного scope корутин.
        // Это позволит запустить все .first() асинхронно и дождаться их завершения.
        coroutineScope {
            // Создаём Deferred для каждого асинхронного вызова .first().
            // Deferred — это объект, который представляет будущий результат вычисления.
            // Мы используем async { ... } для запуска в параллель.
            val deferredMoneyAtTariffRate = async {
                var value = salaryCalculationHelper.getMoneyAtWorkTimeAtTariffSingleRoute().first()
                if (value < 0.0) value = 0.0
                value  // Возвращаем значение для await позже
            }

            val deferredMoneyAtNightHours = async {
                salaryCalculationHelper.getMoneyAtNightTimeFlow().first()
            }

            val deferredZonalSurchargeMoney = async {
                salaryCalculationHelper.getMoneyZonalSurchargeFlow().first()
            }

            val deferredMoneyAtPassengerTime = async {
                salaryCalculationHelper.getMoneyAtPassengerFlow().first()
            }

            val deferredMoneyAtHoliday = async {
                salaryCalculationHelper.getMoneyAtHolidayFlow().first()
            }

            val deferredSurchargeAtExtendedServicePhase = async {
                salaryCalculationHelper.getMoneyListSurchargeExtendedServicePhaseFlow().first()
                    .sum()
            }

            val deferredSurchargeAtHeavyTrains = async {
                salaryCalculationHelper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
            }

            val deferredSurchargeAtLongTrains = async {
                salaryCalculationHelper.getMoneyListSurchargeLongTrainsFlow().first().sum()
            }

            val deferredSurchargeAtDoubledTrainFirst = async {
                salaryCalculationHelper.getMoneyDoubledTrainFirstSurchargeFlow(listOf(route)).first()
            }
            val deferredSurchargeAtDoubledTrainSecond = async {
                salaryCalculationHelper.getMoneyDoubledTrainSecondSurchargeFlow(listOf(route)).first()
            }

            val deferredMoneyAtQualificationClass = async {
                salaryCalculationHelper.getMoneyAtQualificationClassFlow().first()
            }

            val deferredNordicSurcharge = async {
                salaryCalculationHelper.getMoneyNordicSurcharge().first()
            }

            val deferredDistrictSurcharge = async {
                salaryCalculationHelper.getMoneyDistrictSurcharge().first()
            }

            val deferredMoneyAtHarmfulness = async {
                salaryCalculationHelper.getMoneyHarmfulnessFlow().first()
            }

            val deferredOtherSurchargeMoney = async {
                salaryCalculationHelper.getMoneyOtherSurchargeFlow().first()
            }

            // Расчёт переотдыха: ищем предыдущий маршрут с restPointOfTurnover
            val deferredOverRestMoney = async {
                val monthRoutes = routeUseCase.listRoutesByMonth(
                    setting.selectMonthOfYear, setting.timeZone
                ).first { it is ResultState.Success }
                if (monthRoutes is ResultState.Success) {
                    val sorted = monthRoutes.data.sortedBy { it.basicData.timeStartWork }
                    val currentIndex = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
                    if (currentIndex > 0) {
                        val prevRoute = sorted[currentIndex - 1]
                        if (prevRoute.basicData.restPointOfTurnover) {
                            val overRestTime = prevRoute.getOverRestTime(route, setting.minTimeRestPointOfTurnover)
                            if (overRestTime > 0L) {
                                overRestTime.times(setting.selectMonthOfYear.tariffRate * (2.0 / 3.0)) / 3_600_000.toDouble()
                            } else 0.0
                        } else 0.0
                    } else 0.0
                } else 0.0
            }

            // Синхронная логика (не требует async)
            var isPassengerTrain = false
            passengerTrainNumberList.forEach { interval ->
                route.trains.forEach { train ->
                    if (interval.contains(train.number.toIntOrZero())) {
                        isPassengerTrain = true
                        return@forEach
                    }
                }
            }

            // Deferred для moneyAtOnePerson (зависит от isPassengerTrain, но .first() асинхронный)
            val deferredMoneyAtOnePerson = async {
                if (isPassengerTrain) {
                    salaryCalculationHelper.getMoneyOnePersonOperationPassengerTrainFlow().first()
                } else {
                    salaryCalculationHelper.getMoneyOnePersonOperationFlow().first()
                }
            }

            // Дожидаемся завершения всех асинхронных задач с помощью await().
            // Мы вызываем await() на каждом Deferred, чтобы получить реальные значения.
            // Это блокирует выполнение до тех пор, пока все вычисления не завершатся.
            val moneyAtTariffRate = deferredMoneyAtTariffRate.await()
            val moneyAtNightHours = deferredMoneyAtNightHours.await()
            val zonalSurchargeMoney = deferredZonalSurchargeMoney.await()
            val moneyAtPassengerTime = deferredMoneyAtPassengerTime.await()
            val moneyAtHoliday = deferredMoneyAtHoliday.await()

            val surchargeAtExtendedServicePhase = deferredSurchargeAtExtendedServicePhase.await()
            val surchargeAtHeavyTrains = deferredSurchargeAtHeavyTrains.await()
            val surchargeAtLongTrains = deferredSurchargeAtLongTrains.await()
            val moneyAtQualificationClass = deferredMoneyAtQualificationClass.await()
            val nordicSurcharge = deferredNordicSurcharge.await()
            val districtSurcharge = deferredDistrictSurcharge.await()
            val moneyAtHarmfulness = deferredMoneyAtHarmfulness.await()
            val otherSurchargeMoney = deferredOtherSurchargeMoney.await()
            val moneyAtOnePerson = deferredMoneyAtOnePerson.await()
            val surchargeAtDoubledTrainFirst = deferredSurchargeAtDoubledTrainFirst.await()
            val surchargeAtDoubledTrainSecond = deferredSurchargeAtDoubledTrainSecond.await()
            val overRestMoney = deferredOverRestMoney.await()

            // Теперь, когда все значения получены, выполняем суммирование
            val surchargeAtTrains =
                surchargeAtExtendedServicePhase + surchargeAtHeavyTrains + surchargeAtLongTrains + surchargeAtDoubledTrainFirst + surchargeAtDoubledTrainSecond

            val otherSurcharge =
                moneyAtQualificationClass + nordicSurcharge + districtSurcharge + moneyAtHarmfulness + otherSurchargeMoney

            val totalMoney =
                moneyAtTariffRate + moneyAtNightHours + zonalSurchargeMoney + moneyAtPassengerTime + moneyAtHoliday + surchargeAtTrains + moneyAtOnePerson + otherSurcharge + overRestMoney

            // Логи (оставляем как есть)
            Log.d("zzz", "moneyAtTariffRate $moneyAtTariffRate")
            Log.d("zzz", "moneyAtHoliday $moneyAtHoliday")

            // Обновление состояния только после всех вычислений
            _salaryForRouteState.update {
                it.copy(
                    isCalculated = true,
                    isSetTariffRate = isSetTariffRate,
                    totalPayment = totalMoney,
                    paymentAtTariffRate = moneyAtTariffRate,
                    paymentAtNightTime = moneyAtNightHours,
                    zonalSurchargeMoney = zonalSurchargeMoney,
                    paymentAtPassengerTime = moneyAtPassengerTime,
                    paymentHolidayMoney = moneyAtHoliday,
                    surchargesAtTrain = surchargeAtTrains,
                    paymentAtOnePerson = moneyAtOnePerson,
                    otherSurcharge = otherSurcharge,
                    overRestMoney = overRestMoney
                )
            }

        }
    }

    private suspend fun getNightTimeInRoute(route: Route) {
        nightTime?.let { time ->
            val nightMillis = CalculateNightTime.getNightTime(
                startMillis = route.basicData.timeStartWork,
                endMillis = route.basicData.timeEndWork,
                hourStart = time.startNightHour,
                minuteStart = time.startNightMinute,
                hourEnd = time.endNightHour,
                minuteEnd = time.endNightMinute,
                offsetInMoscow = currentTimeZoneOffset ?: 0L,
                breakStartMillis = route.basicData.timeStartBreak,
                breakEndMillis = route.basicData.timeEndBreak
            ).first()
            _uiState.update { it.copy(nightTime = nightMillis) }
        }
    }

    private suspend fun getHolidayTimeInRoute(route: Route, settings: UserSettings) {
        val holidayTime =
            listOf(route).getWorkingTimeOnAHoliday(settings.selectMonthOfYear, settings.timeZone)
                .first()
        _holidayTime.value = holidayTime
    }

    private suspend fun calculationHomeRest(route: Route) {
        routeHelper.calculationHomeRest(route).collectLatest { result ->
            if (result is ResultState.Success) {
                _dialogRestUiState.update {
                    it.copy(
                        homeRestDuration = result.data?.first ?: 0L,
                        timeEndHomeRest = result.data?.second
                    )
                }
            }
        }
    }

    private suspend fun getMinTimeRest(route: Route) {
        routeHelper.calculateShortRest(route).collectLatest { result ->
            _dialogRestUiState.update {
                it.copy(
                    minTimeDuration = result?.first,
                    timeEndMinTimeRestPointOfTurnover = result?.second
                )
            }
        }
    }

    private suspend fun getFullRest(route: Route) {
        routeHelper.calculateFullRest(route).collectLatest { result ->
            _dialogRestUiState.update {
                it.copy(
                    fullTimeDuration = result?.first,
                    timeEndFullTimeRestPointOfTurnover = result?.second
                )
            }
        }
    }

    fun isValidTime(route: Route) {
        viewModelScope.launch {
            val isRouteValid = routeUseCase.isValidBasicData(route).first()
            _uiState.update { it.copy(errorMessage = if (isRouteValid is ResultState.Error) isRouteValid.entity.message else null) }
        }
    }


    private fun getPassengerTime(route: Route) {
        val passengerTime = route.getPassengerTime()
        _uiState.update { it.copy(passengerTime = passengerTime) }
    }

    // Setters для обновления route (реактивно)
    fun setNumber(number: String) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(number = number.ifBlank { null })) }
        changesHave()
    }

    fun checkedOnePersonOperation(checked: Boolean) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(isOnePersonOperation = checked)) }
        changesHave()
    }

    fun setNotes(notes: String) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(notes = notes.ifBlank { null })) }
        changesHave()
    }

    fun setTimeStartWork(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeStartWork = time)) }
        changesHave()
    }

    fun setTimeEndWork(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeEndWork = time)) }
        changesHave()
        if (time != null) {
            checkWorkTimeExceeds12h()
        }
    }

    fun setTimeStartBreak(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeStartBreak = time)) }
        changesHave()
    }

    fun setTimeEndBreak(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeEndBreak = time)) }
        changesHave()
    }

    private fun checkWorkTimeExceeds12h() {
        val route = currentRoute.value ?: return
        val start = route.basicData.timeStartWork ?: return
        val end = route.basicData.timeEndWork ?: return
        val duration = end - start
        val twelveHours = 12 * 3_600_000L
        if (duration <= twelveHours) return

        val dontAsk = sharedPreferenceStorage.isPassenger12hDontAskAgain()
        if (dontAsk) {
            if (sharedPreferenceStorage.isPassenger12hAutoAccepted()) {
                val (timeDep, timeArr) = getPrefilledPassengerTimes()
                val stationDep = getPrefilledDepartureStation()
                savePassengerFromSheet(stationDep, null, timeDep, timeArr)
            }
            return
        }
        _showPassenger12hSheet.value = true
    }

    fun getPrefilledPassengerTimes(): Pair<Long, Long> {
        val route = currentRoute.value!!
        val start = route.basicData.timeStartWork!!
        val end = route.basicData.timeEndWork!!
        val twelveHours = 12 * 3_600_000L
        val oneMinute = 60_000L
        val departure = (start + twelveHours + oneMinute).let { it - it % 60_000L }
        val arrival = (end - oneMinute).let { it - it % 60_000L }
        return departure to arrival
    }

    fun getPrefilledDepartureStation(): String? {
        return currentRoute.value?.trains?.lastOrNull()?.stations?.lastOrNull()?.stationName
    }

    fun savePassengerFromSheet(
        stationDeparture: String?,
        stationArrival: String?,
        timeDeparture: Long,
        timeArrival: Long
    ) {
        viewModelScope.launch {
            val route = currentRoute.value ?: return@launch
            val basicId = route.basicData.id

            // Сначала сохраняем маршрут с актуальным временем работы,
            // чтобы subscribeToChanges не перезаписал его старыми значениями из БД
            preSaveRoute()

            val passenger = Passenger(
                basicId = basicId,
                stationDeparture = stationDeparture,
                stationArrival = stationArrival,
                timeDeparture = timeDeparture - timeDeparture % 60_000L,
                timeArrival = timeArrival - timeArrival % 60_000L
            )

            passengerUseCase.savePassenger(passenger)
                .onEach { result ->
                    if (result is ResultState.Success) {
                        subscribeToChanges(basicId)
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun dismissPassenger12hSheet() {
        _showPassenger12hSheet.value = false
    }

    fun onRestChanged(isRest: Boolean) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(restPointOfTurnover = isRest)) }
        changesHave()
    }

    fun onDeleteLoco(locomotive: Locomotive) {
        _currentRoute.update { route ->
            route?.copy(locomotives = route.locomotives.filter { it != locomotive } as MutableList<Locomotive>)
        }
        deletedLocoList.add(locomotive)
        changesHave()
    }

    fun onDeleteTrain(train: Train) {
        _currentRoute.update { route ->
            route?.copy(trains = route.trains.filter { it != train } as MutableList<Train>)
        }
        deletedTrainList.add(train)
        changesHave()
    }

    fun onDeletePassenger(passenger: Passenger) {
        _currentRoute.update { route ->
            route?.copy(passengers = route.passengers.filter { it != passenger } as MutableList<Passenger>)
        }
        deletedPassengerList.add(passenger)
        changesHave()
    }

    /**
     * Нормализует timeStartWork до минутной точности для сравнения дублей.
     * DateTimePickerApp уже обнуляет секунды/миллисекунды, но дополнительная
     * нормализация защищает от случаев, когда время могло попасть с precision > минута
     * (например, из import-а с сервера).
     */
    private fun normalizeToMinute(timeMs: Long): Long = (timeMs / 60_000L) * 60_000L

    /**
     * Ищет в локальной БД маршрут с таким же timeStartWork, что и у текущего.
     * Возвращает найденный маршрут или null.
     * Исключает сам текущий маршрут (по basicData.id). Удалённые (isDeleted=true)
     * автоматически отфильтрованы getListRoutes() через SQL-запрос.
     */
    private fun findDuplicateByStartWork(current: Route): Route? {
        val currentStart = current.basicData.timeStartWork ?: return null
        val currentMinute = normalizeToMinute(currentStart)
        val all = routeUseCase.getListRoutes()
        return all.firstOrNull { other ->
            other.basicData.id != current.basicData.id &&
                other.basicData.timeStartWork?.let { normalizeToMinute(it) == currentMinute } == true
        }
    }

    /**
     * Проверка на дубль перед сохранением. Если дубль найден — показывает шторку
     * (FormScreen реагирует на _duplicateRouteSheet), иначе сохраняет сразу.
     * Вызывается из onSaveClick (топ-бар) и из обработчика «Сохранить» в shared-preview шторке.
     *
     * @param exitAfterSave если true — после успешного сохранения ставит exitFromScreen=true
     *                      (используется shared-preview потоком, чтобы не мерцало окно)
     */
    fun checkDuplicateAndSave(exitAfterSave: Boolean = false) {
        val route = _currentRoute.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val duplicate = findDuplicateByStartWork(route)
            if (duplicate != null) {
                _duplicateRouteSheet.value = DuplicateRouteState(
                    existingRoute = duplicate,
                    exitAfterSave = exitAfterSave
                )
            } else {
                performSave(exitAfterSave = exitAfterSave)
            }
        }
    }

    /**
     * Действие «Заменить» в шторке дубля: удалить старый маршрут, затем сохранить текущий.
     * Принимает state явно, так как AppBottomSheet авто-вызывает onDismissRequest перед
     * action callback-ом, и _duplicateRouteSheet.value к моменту вызова уже null.
     */
    fun confirmReplaceDuplicate(state: DuplicateRouteState) {
        _duplicateRouteSheet.value = null
        viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.removeRoute(state.existingRoute).collect { /* ignore */ }
            performSave(exitAfterSave = state.exitAfterSave)
        }
    }

    /** Действие «Оставить оба» в шторке дубля: просто сохранить текущий, оставив оба. */
    fun confirmKeepBothDuplicates(state: DuplicateRouteState) {
        _duplicateRouteSheet.value = null
        viewModelScope.launch(Dispatchers.IO) {
            performSave(exitAfterSave = state.exitAfterSave)
        }
    }

    /**
     * Фактическое сохранение текущего маршрута. Применяет снятие isDeleted
     * для shared preview, вычищает удалённые child-сущности, эмитит событие
     * FormScreenEvent.RouteSaved. Если exitAfterSave=true — дополнительно
     * сразу ставит exitFromScreen=true (используется в shared-preview потоке
     * для предотвращения мерцания).
     */
    private fun performSave(exitAfterSave: Boolean) {
        saveRouteJob?.cancel()
        saveRouteJob = viewModelScope.launch(Dispatchers.IO) {
            currentRoute.value?.let { route ->
                val routeToSave = if (_isSharedPreview.value || route.basicData.isDeleted) {
                    route.copy(basicData = route.basicData.copy(isDeleted = false))
                } else {
                    route
                }
                if (exitAfterSave) {
                    _isSharedPreview.value = false
                    _uiState.update { it.copy(exitFromScreen = true) }
                    routeUseCase.saveRoute(routeToSave).collect { /* ignore */ }
                    return@let
                }
                routeUseCase.saveRoute(routeToSave).collectLatest { result ->
                    Log.d("zzz", "saveResult $result")
                    _uiState.update { it.copy(saveRouteState = result) }
                    if (result is ResultState.Success) {
                        isPersistedToDb = true
                        _isSharedPreview.value = false
                        deletedLocoList.forEach { loco ->
                            locoUseCase.removeLoco(loco).collect {}
                        }
                        deletedTrainList.forEach { train ->
                            trainUseCase.removeTrain(train).collect {}
                        }
                        deletedPassengerList.forEach { passenger ->
                            passengerUseCase.removePassenger(passenger).collect {}
                        }
                        _events.emit(FormScreenEvent.RouteSaved)
                        // Запускаем синхронизацию в scope, не привязанном к ViewModel:
                        // exitScreen() вызывается сразу после RouteSaved → ViewModel
                        // уничтожается → viewModelScope отменяется до завершения sync.
                        val savedRouteId = routeToSave.basicData.id
                        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                            if (!rawToken.isNullOrBlank()) {
                                syncManager.syncRoute(savedRouteId, "Bearer $rawToken")
                                    .collect { }
                            }
                        }
                    }
                }
            }
        }
    }

    // Сохранение — публичный метод для обратной совместимости (вызывается из onSaveClick
    // после subscription check). Делегирует на checkDuplicateAndSave.
    fun saveRoute() {
        checkDuplicateAndSave(exitAfterSave = false)
    }

    fun resetSaveState() {
        _uiState.update { it.copy(saveRouteState = null) }
    }

    // Выход без сохранения
    fun exitWithoutSave() {
        viewModelScope.launch {
            if (isNewRoute) {
                currentRoute.value?.let { route ->
                    routeUseCase.removeRoute(route).collect { result ->
                        if (result is ResultState.Success) {
                            _uiState.update {
                                it.copy(
                                    confirmExitDialogShow = false,
                                    exitFromScreen = true
                                )
                            }
                        }
                    }
                }
            } else {
                _uiState.update { it.copy(confirmExitDialogShow = false, exitFromScreen = true) }
            }
            countLoadRoute = 0
            sharedPreferenceStorage.setTokenIsChangeHave(false)
        }
    }

    // Изменения
    private fun changesHave() {
        sharedPreferenceStorage.setTokenIsChangeHave(true)
        if (!_uiState.value.changesHaveState) {
            _uiState.update { it.copy(changesHaveState = true) }
        }
        triggerAutoSave()
    }

    fun restorePurchases() {
        viewModelScope.launch {
            val token = SecureDataStore.getAuthBearerTokenFlow(application).first()
            subscriptionHelper.restorePurchases(snackbarManager, token)
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            // Sentry-лог для пользователя VKID 17260416
            val vkId = SecureDataStore.getVkIdFlow(application).first()
            if (vkId == "17260416") {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val subscriptionPeriod = setting.subscriptionPeriod
                Sentry.captureMessage("[VKID:$vkId] onSaveClick: subscriptionPeriod=$subscriptionPeriod (${java.util.Date(subscriptionPeriod)})")
            }
            // Подписка проверена до входа на экран (кнопка + нижнего меню навигации)
            checkDuplicateAndSave(exitAfterSave = false)
        }
    }

    fun setFavoriteRoute() {
        _currentRoute.update { route ->
            route?.copy(basicData = route.basicData.copy(isFavorite = !route.basicData.isFavorite))
        }
        changesHave()
    }

    /** Поделиться маршрутом — создаёт публичную ссылку и эмитит в shareLinkEvent. */
    fun onShareClick() {
        val route = _currentRoute.value ?: return
        viewModelScope.launch {
            try {
                val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (rawToken.isNullOrBlank()) {
                    snackbarManager.show("Войдите в аккаунт, чтобы делиться маршрутами")
                    return@launch
                }
                val bearerToken = "Bearer $rawToken"
                shareRouteManager.createShareLink(route, bearerToken).collect { result ->
                    when (result) {
                        is ResultState.Success -> {
                            val shareText = buildShareText(route, result.data)
                            _shareLinkEvent.emit(shareText)
                        }
                        is ResultState.Error -> snackbarManager.show(
                            result.entity.message ?: "Не удалось создать ссылку"
                        )
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                Log.e("FormViewModel", "onShareClick", e)
                snackbarManager.show("Ошибка создания ссылки")
            }
        }
    }

    private fun buildShareText(route: Route, url: String): String {
        return buildString {
            append("Маршрут из приложения «Машинист» \uD83D\uDE82")
            append("\n")
            route.basicData.timeStartWork?.let { ms ->
                val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
                append(" от ${sdf.format(java.util.Date(ms))}")
            }
            append("\n")
            val stations = route.trains
                .flatMap { it.stations }
                .sortedBy { it.orderIndex }
            val firstStation = stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val lastStation = stations.lastOrNull()?.stationName?.takeIf { it.isNotBlank() }
            if (firstStation != null && lastStation != null && firstStation != lastStation) {
                append(", $firstStation — $lastStation")
            }
            append("\n\n")
            append("Чтобы открыть маршрут нажмите на ссылку внизу")
            append("\n\n")
            append(url)
        }
    }

    /** Удалить маршрут (пометить как удалённый). */
    fun onDeleteRoute() {
        val route = _currentRoute.value ?: return
        viewModelScope.launch {
            routeUseCase.markAsRemoved(route).collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update { it.copy(exitFromScreen = true) }
                }
            }
        }
    }

    /**
     * Сохранить shared-маршрут и выйти без мерцания.
     * Перед сохранением проверяет дубль по timeStartWork — если найден, показывает
     * шторку «Маршрут с такой явкой уже сохранён». Если дубля нет — сразу сохраняет
     * и выходит.
     *
     * Дубль может быть найден, т.к. shared preview уже лежит в БД с isDeleted=true
     * (скрыт из getListRoutes), а реальный дубль — с isDeleted=false (виден).
     */
    fun saveSharedRouteAndExit() {
        checkDuplicateAndSave(exitAfterSave = true)
    }

    // предложение пользователю оценить приложение
    private var reviewInfo: ReviewInfo? = null

    suspend fun prepareReviewDialog() = coroutineScope {
        val count = async { getRoutesCount() }.await()
        val isShow = isShowReviewDialog(count)
        if (isShow) {
            reviewManager.requestReviewFlow()
                .addOnSuccessListener { info ->
                    reviewInfo = info
                }
                .addOnFailureListener { throwable ->
                    Log.w("ZZZ", "prepareReviewDialog throwable = ${throwable.message}")
                }
        }
    }

    private fun isShowReviewDialog(count: Int): Boolean {
        return (count > 10 && count % 5 == 0)
    }

    private suspend fun getRoutesCount(): Int {
        delay(10L)
        return routeUseCase.listRouteWithDeleting().size
    }

    private fun showReviewDialog(reviewInfo: ReviewInfo) {
        reviewManager.launchReviewFlow(reviewInfo)
            .addOnSuccessListener {
                Log.i("ZZZ", "showReviewDialog Success")
            }
            .addOnFailureListener { throwable ->
                Log.w("ZZZ", "showReviewDialog Failure = $throwable")
            }
    }

    private fun getLocoList(basicId: String): MutableList<Locomotive> {
        return locoUseCase.getLocomotiveList(basicId).toMutableList()
    }

    private fun getTrainList(basicId: String): MutableList<Train> {
        return trainUseCase.getTrainListByBasicId(basicId).toMutableList()
    }

    private fun getPassengerList(basicId: String): MutableList<Passenger> {
        return passengerUseCase.getPassengerListByBasicId(basicId).toMutableList()
    }

    fun preSaveRoute() {
        currentRoute.value?.let { route ->
            saveRouteJob?.cancel()
            saveRouteJob = routeUseCase.saveRoute(route).onEach { saveRouteState ->
                if (saveRouteState is ResultState.Success) {
                    isPersistedToDb = true
                    subscribeToChanges(route.basicData.id)
                    changesHave()
                }
            }.launchIn(viewModelScope)
        }
    }

    fun onAddChildEntity(basicId: String, entityType: ChildEntityType) {
        // Подписка проверена до входа на экран (кнопка + нижнего меню навигации).
        // Перед переходом сохраняем маршрут, чтобы дочерний раздел (Локо/Поезд/Пассажир)
        // гарантированно нашёл родительскую запись в БД по basicId.
        preSaveRoute()
        viewModelScope.launch {
            _events.emit(FormScreenEvent.NavigateToChildForm(basicId, entityType))
        }
    }

    private fun subscribeToChanges(routeId: String) {
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.routeDetails(routeId).onEach { routeState ->
            _uiState.update { it.copy(routeDetailState = routeState) }
            if (routeState is ResultState.Success) {
                val dbRoute = routeState.data ?: return@onEach
                // Обновляем только подразделы из БД.
                // BasicData (номер, времена, примечания) хранится в памяти —
                // перезапись из БД затирает текущий ввод пользователя.
                _currentRoute.update { inMemory ->
                    inMemory?.copy(
                        locomotives = dbRoute.locomotives,
                        trains = dbRoute.trains,
                        passengers = dbRoute.passengers,
                        photos = dbRoute.photos
                    ) ?: dbRoute
                }
            }
        }.launchIn(viewModelScope)
    }
//
//    private fun calculateRestTime(route: Route) {
//        viewModelScope.launch {
//            if (route.basicData.restPointOfTurnover) {
//                getMinTimeRest(route)
//                getFullRest(route)
//            } else {
//                calculationHomeRest(route)
//            }
//        }
//    }

}