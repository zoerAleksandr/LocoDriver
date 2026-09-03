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
import com.z_company.domain.entities.route.OtherWork
import com.z_company.domain.entities.route.RoutePartner
import com.z_company.domain.entities.route.Passenger
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.reidentifyForImport
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.route.UtilsForEntities.getOverRestTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getPureWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.passengerTrainNumberList
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.LocomotiveUseCase
import com.z_company.domain.use_cases.OtherWorkUseCase
import com.z_company.domain.use_cases.RoutePartnerUseCase
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
    private val otherWorkUseCase: OtherWorkUseCase by inject()
    private val routePartnerUseCase: RoutePartnerUseCase by inject()
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
    private val _shareLinkEvent = MutableSharedFlow<com.z_company.route.util.ShareLinkData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shareLinkEvent: SharedFlow<com.z_company.route.util.ShareLinkData> = _shareLinkEvent.asSharedFlow()

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

    private val _nightWarnState = MutableStateFlow<NightWarnState?>(null)
    val nightWarnState: StateFlow<NightWarnState?> = _nightWarnState.asStateFlow()

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
    private val deletedOtherWorkList = mutableListOf<OtherWork>()
    private var deleteOtherWorkJob: Job? = null

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

        // Реактивная подписка (не .first()): при изменении настроек зарплаты
        // (например, «средний час») в других экранах форма пересчитывает оплату
        // сразу по возвращении, а не только при следующем открытии.
        viewModelScope.launch {
            salarySettingUseCase.salarySettingFlow().collect { _salarySetting.value = it }
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
                        allRoutes = routeList,
                        workScheduleProfile = sharedPreferenceStorage.getWorkScheduleProfile(),
                    )
                    coroutineScope {
                        launch { calculateSalary(salaryCalculationHelper, route, settings) }
                        launch { getNightTimeInRoute(route) }
                        launch { getHolidayTimeInRoute(route, settings) }
                        launch { calculationHomeRest(route) }
                        launch { getMinTimeRest(route) }
                        launch { getFullRest(route) }
                        launch { calculationActualRest(route) }
                        launch { isValidTime(route) }
                        launch { getPassengerTime(route) }
                        launch { computeNightWarn(route, settings) }
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
                // НЕ сохраняем сразу — пустой маршрут не должен попадать в БД,
                // если пользователь передумал и закрыл форму без изменений.
                // Initial save произойдёт автоматически при первом изменении в поле
                // (через triggerAutoSave) или при переходе в подраздел (preSaveRoute).
            } else {
                routeId?.let { id ->
                    routeUseCase.routeDetails(id).collectLatest { result ->
                        if (result is ResultState.Success) {
                            // Копия: переприсваиваем ВСЕ идентификаторы (BasicData +
                            // локомотивы/поезда/пассажиры/прочая работа/напарники) и
                            // сбрасываем ссылки облачной синхронизации. Иначе дочерние
                            // записи сохраняются с прежним basicId (foreign key) и
                            // остаются привязанными к оригиналу — в копию не попадают
                            // (симптом: «прочая работа» не переносится в копию).
                            val loadedRoute = if (isCopy) result.data?.reidentifyForImport()
                            else result.data
                            _uiState.update { it.copy(routeDetailState = result) }
                            countLoadRoute += 1
                            // Копию НЕ сохраняем сразу — ждём первого изменения от пользователя
                            // (та же логика что и для нового маршрута).
                            if (countLoadRoute == 1) {
                                // Первая загрузка: устанавливаем весь маршрут из БД
                                _currentRoute.value = loadedRoute?.withWorkStartFromPassenger()
                            } else {
                                // Возврат с подэкрана (Loco/Train/Passenger/Photo):
                                // обновляем только подразделы, BasicData берём из памяти —
                                // чтобы не затирать то, что пользователь ввёл в поля.
                                // changesHave() НЕ вызываем — это DB-событие, а не ввод пользователя,
                                // иначе возникает бесконечный цикл: autosave→DB emit→changesHave→autosave
                                loadedRoute?.let { dbRoute ->
                                    _currentRoute.update { inMemory ->
                                        (inMemory?.copy(
                                            locomotives = dbRoute.locomotives,
                                            trains = dbRoute.trains,
                                            passengers = dbRoute.passengers,
                                            otherWorks = dbRoute.otherWorks,
                                            partners = dbRoute.partners,
                                            photos = dbRoute.photos,
                                            // timeEndWork set from LocoFormScreen delivery sheet — take from DB.
                                            // timeStartWork может меняться на экране «Пассажиром»
                                            // («явка по прибытию») — тоже берём из БД.
                                            basicData = inMemory.basicData.copy(
                                                timeEndWork = dbRoute.basicData.timeEndWork,
                                                timeStartWork = dbRoute.basicData.timeStartWork,
                                                // резерв прежней явки тоже из БД — иначе автосейв затрёт
                                                timeStartWorkBeforeArrival = dbRoute.basicData.timeStartWorkBeforeArrival
                                            )
                                        ) ?: dbRoute)
                                            // Инвариант: если пассажир помечен «явкой по прибытию», явка
                                            // всегда равна его прибытию — не полагаемся на тайминг записи в БД.
                                            .withWorkStartFromPassenger()
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

    /** Автосейв с debounce 500 мс.
     *  - Если маршрут ещё не в БД (новый или копия) — первый вызов делает initial save
     *    и подписывается на изменения от БД.
     *  - Если уже в БД — просто обновляет существующую запись.
     *  Сохранение выполняется внутри autoSaveJob — не трогает saveRouteJob,
     *  который используется для явных сохранений (performSave, preSaveRoute). */
    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            withContext(Dispatchers.IO) {
                currentRoute.value?.let { route ->
                    val wasInDb = isPersistedToDb
                    routeUseCase.saveRoute(route).collect { result ->
                        if (!wasInDb && result is ResultState.Success) {
                            isPersistedToDb = true
                            subscribeToChanges(route.basicData.id)
                        }
                    }
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
        deleteOtherWorkJob?.cancel()
        // Финальное сохранение при уходе с экрана —
        // ТОЛЬКО если маршрут уже в БД И были несохранённые изменения.
        // Без этого условия пустой/неизменённый маршрут перезаписывался бы при каждом
        // открытии формы, оставляя в БД пустые черновики.
        if (isPersistedToDb && _uiState.value.changesHaveState) {
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
        val workTime = route.getPureWorkTime()
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

            // Фактическое тарифное время (после вычета пассажира в смене и т.п.) —
            // чтобы подсказка «Почасовая оплата» совпадала с суммой.
            val deferredWorkTimeForPay = async {
                salaryCalculationHelper.getWorkTimeAtTariffSingleRouteFlow().first().coerceAtLeast(0L)
            }

            val deferredMoneyAtNightHours = async {
                salaryCalculationHelper.getMoneyAtNightTimeFlow().first()
            }

            val deferredZonalSurchargeMoney = async {
                salaryCalculationHelper.getMoneyZonalSurchargeFlow().first()
            }

            val deferredZonalPercent = async {
                salaryCalculationHelper.getPercentZonalSurchargeFlow().first()
            }

            val deferredZonalTime = async {
                salaryCalculationHelper.getTimeZonalSurchargeFlow().first()
            }

            val deferredMoneyAtPassengerTime = async {
                salaryCalculationHelper.getMoneyAtPassengerFlow().first()
            }
            // Оплата проезда пассажиром до явки — по тарифу, отдельно от работы.
            val deferredMoneyAtPassengerOutside = async {
                salaryCalculationHelper.getMoneyAtPassengerOutsideWorkFlow().first()
            }

            val deferredMoneyAtHoliday = async {
                salaryCalculationHelper.getMoneyAtHolidayFlow().first()
            }

            val deferredLinearMileageDistance = async {
                salaryCalculationHelper.getLinearMileageDistanceFlow().first()
            }

            val deferredLinearMileageMoney = async {
                salaryCalculationHelper.getMoneyLinearMileageFlow().first()
            }

            val deferredLinearMileageAccruals = async {
                salaryCalculationHelper.getLinearMileageAccrualsFlow().first()
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
                                (overRestTime.times(setting.selectMonthOfYear.tariffRate * (2.0 / 3.0)) / 3_600_000.toDouble()) to overRestTime
                            } else 0.0 to 0L
                        } else 0.0 to 0L
                    } else 0.0 to 0L
                } else 0.0 to 0L
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

            // База времени «в одно лицо» — для пояснения-формулы в шторке
            val deferredOnePersonTime = async {
                if (isPassengerTrain) {
                    salaryCalculationHelper.getTimeOnePersonOperationPassengerTrainFlow().first()
                } else {
                    salaryCalculationHelper.getTimeOnePersonOperationFlow().first()
                }
            }

            // Дожидаемся завершения всех асинхронных задач с помощью await().
            // Мы вызываем await() на каждом Deferred, чтобы получить реальные значения.
            // Это блокирует выполнение до тех пор, пока все вычисления не завершатся.
            val moneyAtTariffRate = deferredMoneyAtTariffRate.await()
            val workTimeForPay = deferredWorkTimeForPay.await()
            val moneyAtNightHours = deferredMoneyAtNightHours.await()
            val zonalSurchargeMoney = deferredZonalSurchargeMoney.await()
            val moneyAtPassengerTime = deferredMoneyAtPassengerTime.await()
            val moneyAtPassengerOutside = deferredMoneyAtPassengerOutside.await()
            val moneyAtHoliday = deferredMoneyAtHoliday.await()
            val linearMileageDistance = deferredLinearMileageDistance.await()
            val linearMileageMoney = deferredLinearMileageMoney.await()
            val linearMileageAccruals = deferredLinearMileageAccruals.await()

            val surchargeAtExtendedServicePhase = deferredSurchargeAtExtendedServicePhase.await()
            val surchargeAtHeavyTrains = deferredSurchargeAtHeavyTrains.await()
            val surchargeAtLongTrains = deferredSurchargeAtLongTrains.await()
            val moneyAtQualificationClass = deferredMoneyAtQualificationClass.await()
            val nordicSurcharge = deferredNordicSurcharge.await()
            val districtSurcharge = deferredDistrictSurcharge.await()
            val moneyAtHarmfulness = deferredMoneyAtHarmfulness.await()
            val otherSurchargeMoney = deferredOtherSurchargeMoney.await()
            val moneyAtOnePerson = deferredMoneyAtOnePerson.await()
            val onePersonTime = deferredOnePersonTime.await()
            val surchargeAtDoubledTrainFirst = deferredSurchargeAtDoubledTrainFirst.await()
            val surchargeAtDoubledTrainSecond = deferredSurchargeAtDoubledTrainSecond.await()
            val (overRestMoney, overRestTime) = deferredOverRestMoney.await()
            val zonalPercent = deferredZonalPercent.await()
            val zonalTime = deferredZonalTime.await()
            val currentSalarySetting = _salarySetting.value
            val onePersonPercent = if (isPassengerTrain) {
                currentSalarySetting?.onePersonOperationPassengerTrainPercent
            } else {
                currentSalarySetting?.onePersonOperationPercent
            }

            // Теперь, когда все значения получены, выполняем суммирование
            val surchargeAtTrains =
                surchargeAtExtendedServicePhase + surchargeAtHeavyTrains + surchargeAtLongTrains + surchargeAtDoubledTrainFirst + surchargeAtDoubledTrainSecond

            // Какие именно доплаты за поезд применены — для расшифровки в шторке
            val trainSurchargeTypes = buildList {
                if (surchargeAtHeavyTrains != 0.0) add("тяжеловесный")
                if (surchargeAtLongTrains != 0.0) add("длинносоставный")
                if (surchargeAtExtendedServicePhase != 0.0) add("удлинённое плечо")
                if (surchargeAtDoubledTrainFirst + surchargeAtDoubledTrainSecond != 0.0) add("сдвоенный")
            }

            val otherSurcharge =
                moneyAtQualificationClass + nordicSurcharge + districtSurcharge + moneyAtHarmfulness + otherSurchargeMoney

            // Командировка: если маршрут попадает в дни командировки, все обычные
            // составляющие выше равны 0 (маршрут исключён из расчёта), а оплата —
            // только по среднему часу.
            val businessTripMoney = salaryCalculationHelper.getMoneyBusinessTripFlow().first()
            val hasBusinessTripPart = salaryCalculationHelper.hasBusinessTripRoutes()
            val isEntirelyBusinessTrip = salaryCalculationHelper.isEntirelyBusinessTrip()

            // Жёсткий инвариант командировки: даже расчёты, которым по ошибке
            // передали маршрут напрямую (например, сдвоенный поезд или переотдых),
            // не должны попасть ни в строки шторки, ни в итоговую сумму.
            if (isEntirelyBusinessTrip) {
                _salaryForRouteState.update {
                    SalaryForRouteState(
                        isCalculated = true,
                        isSetTariffRate = isSetTariffRate,
                        totalPayment = businessTripMoney,
                        businessTripMoney = businessTripMoney,
                        isBusinessTrip = true,
                        tariffRate = setting.selectMonthOfYear.tariffRate,
                    )
                }
                return@coroutineScope
            }

            val totalMoney =
                moneyAtTariffRate + moneyAtNightHours + zonalSurchargeMoney + moneyAtPassengerTime + moneyAtPassengerOutside + moneyAtHoliday + linearMileageMoney + surchargeAtTrains + moneyAtOnePerson + otherSurcharge + overRestMoney + businessTripMoney

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
                    paymentAtPassengerOutsideTime = moneyAtPassengerOutside,
                    paymentHolidayMoney = moneyAtHoliday,
                    linearMileageDistance = linearMileageDistance,
                    linearMileageMoney = linearMileageMoney,
                    linearMileageAccruals = linearMileageAccruals,
                    surchargesAtTrain = surchargeAtTrains,
                    paymentAtOnePerson = moneyAtOnePerson,
                    otherSurcharge = otherSurcharge,
                    overRestMoney = overRestMoney,
                    businessTripMoney = businessTripMoney,
                    isBusinessTrip = hasBusinessTripPart,
                    tariffRate = setting.selectMonthOfYear.tariffRate,
                    workTimeForPay = workTimeForPay,
                    zonalPercent = zonalPercent,
                    zonalTime = zonalTime,
                    nightPercent = currentSalarySetting?.nightTimePercent,
                    onePersonPercent = onePersonPercent,
                    onePersonTime = onePersonTime,
                    overRestTime = overRestTime,
                    trainSurchargeTypes = trainSurchargeTypes
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

    /**
     * Предупреждение «вторая ночь подряд»: текущий маршрут захватывает ночное
     * окно, и предыдущий по времени маршрут (в пределах ~36 ч) — тоже,
     * если между маршрутами не было домашнего отдыха.
     */
    private suspend fun computeNightWarn(route: Route, settings: UserSettings) {
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        if (start == null || end == null) {
            _nightWarnState.value = null
            return
        }
        val offset = currentTimeZoneOffset ?: 0L

        // Правило «вторая ночь подряд» считается по фиксированному окну 00:00–05:00
        // (не по пользовательским настройкам ночных часов для оплаты).
        val nightStartHour = 0
        val nightStartMinute = 0
        val nightEndHour = 5
        val nightEndMinute = 0

        suspend fun nightOf(s: Long, e: Long, bs: Long?, be: Long?): Long =
            CalculateNightTime.getNightTime(
                startMillis = s, endMillis = e,
                hourStart = nightStartHour, minuteStart = nightStartMinute,
                hourEnd = nightEndHour, minuteEnd = nightEndMinute,
                offsetInMoscow = offset,
                breakStartMillis = bs, breakEndMillis = be
            ).first() ?: 0L

        fun intervalsOf(s: Long, e: Long): List<Pair<Long, Long>> =
            CalculateNightTime.getNightIntervals(
                startMillis = s, endMillis = e,
                hourStart = nightStartHour, minuteStart = nightStartMinute,
                hourEnd = nightEndHour, minuteEnd = nightEndMinute,
                offsetInMoscow = offset
            )

        val thisNight = nightOf(start, end, route.basicData.timeStartBreak, route.basicData.timeEndBreak)
        if (thisNight <= 0L) {
            _nightWarnState.value = null
            return
        }

        // Случай 1: сам маршрут захватывает два ночных окна.
        val nightWindows = CalculateNightTime.getNightWindowsCount(
            startMillis = start, endMillis = end,
            hourStart = nightStartHour, minuteStart = nightStartMinute,
            hourEnd = nightEndHour, minuteEnd = nightEndMinute,
            offsetInMoscow = offset
        )
        if (nightWindows >= 2) {
            _nightWarnState.value = NightWarnState(
                listOf(NightWarnRow("Этот маршрут", start, end, thisNight, intervalsOf(start, end)))
            )
            return
        }

        // Случай 2: предыдущий по времени маршрут (в пределах ~36 ч) тоже ночной.
        val monthRoutesState = routeUseCase
            .listRoutesByMonth(settings.selectMonthOfYear, settings.timeZone)
            .first { it is ResultState.Success }
        val prev = (monthRoutesState as? ResultState.Success)?.data
            ?.filter { it.basicData.id != route.basicData.id }
            ?.filter { it.basicData.timeStartWork != null && it.basicData.timeEndWork != null }
            ?.filter { (it.basicData.timeStartWork ?: 0L) < start }
            ?.maxByOrNull { it.basicData.timeStartWork ?: 0L }
        val prevStart = prev?.basicData?.timeStartWork
        val prevEnd = prev?.basicData?.timeEndWork
        val consecutiveLimit = 36L * 3_600_000L
        if (
            prev == null ||
            prevStart == null ||
            prevEnd == null ||
            !prev.basicData.restPointOfTurnover ||
            start - prevEnd > consecutiveLimit
        ) {
            _nightWarnState.value = null
            return
        }
        val prevNight = nightOf(prevStart, prevEnd, prev.basicData.timeStartBreak, prev.basicData.timeEndBreak)
        if (prevNight <= 0L) {
            _nightWarnState.value = null
            return
        }

        _nightWarnState.value = NightWarnState(
            listOf(
                NightWarnRow("Предыдущий маршрут", prevStart, prevEnd, prevNight, intervalsOf(prevStart, prevEnd)),
                NightWarnRow("Этот маршрут", start, end, thisNight, intervalsOf(start, end))
            )
        )
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
                        homeRestDuration = result.data?.duration ?: 0L,
                        timeEndHomeRest = result.data?.endTime,
                        timeEndMinHomeRest = result.data?.minEndTime
                    )
                }
            }
        }
    }

    private suspend fun calculationActualRest(route: Route) {
        routeHelper.calculationActualRest(route).collectLatest { result ->
            if (result is ResultState.Success) {
                _dialogRestUiState.update {
                    it.copy(
                        actualRestDuration = result.data?.first,
                        timeEndActualRest = result.data?.second,
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

    // Отбрасываем секунды и миллисекунды — время работы должно быть кратно минуте.
    private fun Long?.truncateToMinute(): Long? = this?.let { it - it % 60_000L }

    /**
     * Инвариант «явки по прибытию»: если есть пассажир с флагом isWorkStartByArrival и
     * заданным прибытием, явка маршрута = его прибытию. Применяем при загрузке/слиянии
     * маршрута, чтобы экран не зависел от тайминга записи timeStartWork в БД.
     */
    private fun Route.withWorkStartFromPassenger(): Route {
        val arrival = passengers.firstOrNull {
            it.isWorkStartByArrival && it.timeArrival != null
        }?.timeArrival ?: return this
        return if (basicData.timeStartWork == arrival) this
        else copy(basicData = basicData.copy(timeStartWork = arrival))
    }

    fun setTimeStartWork(time: Long?) {
        val start = time.truncateToMinute()
        _currentRoute.update { route ->
            route?.copy(
                basicData = route.basicData.copy(
                    timeStartWork = start,
                    timeEndWork = if (start != null && usingDefaultWorkTime) {
                        start + (defaultWorkTime ?: 0L)
                    } else {
                        route.basicData.timeEndWork
                    }
                )
            )
        }
        changesHave()
        if (start != null && usingDefaultWorkTime) {
            checkWorkTimeExceeds12h()
        }
    }

    /**
     * Отключить «явку по прибытию»: снять флаг со всех пассажиров, чтобы инвариант в
     * saveRoute больше не перезаписывал timeStartWork прибытием. Нужно, когда пользователь
     * решил задать явку вручную прямо на этом экране. Резерв прежней явки очищаем — новую
     * пользователь выставит сам.
     */
    fun disableWorkStartByArrival() {
        _currentRoute.update { route ->
            route?.copy(
                passengers = route.passengers
                    .map { it.copy(isWorkStartByArrival = false) }
                    .toMutableList(),
                basicData = route.basicData.copy(timeStartWorkBeforeArrival = null)
            )
        }
        changesHave()
    }

    fun setTimeEndWork(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeEndWork = time.truncateToMinute())) }
        changesHave()
        if (time != null) {
            checkWorkTimeExceeds12h()
        }
    }

    fun setTimeStartBreak(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeStartBreak = time.truncateToMinute())) }
        changesHave()
    }

    fun setTimeEndBreak(time: Long?) {
        _currentRoute.update { it?.copy(basicData = it.basicData.copy(timeEndBreak = time.truncateToMinute())) }
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
        // Сразу удаляем из БД: подписка routeDetails вернёт актуальный список без этого элемента.
        // Отложенное удаление через deletedLocoList приводило к тому, что DB emit
        // от autosave восстанавливал элемент обратно в UI до явного «Сохранить».
        deletedLocoList.add(locomotive)
        viewModelScope.launch(Dispatchers.IO) {
            locoUseCase.removeLoco(locomotive).collect {}
        }
        changesHave()
    }

    fun onDeleteTrain(train: Train) {
        _currentRoute.update { route ->
            route?.copy(trains = route.trains.filter { it != train } as MutableList<Train>)
        }
        deletedTrainList.add(train)
        viewModelScope.launch(Dispatchers.IO) {
            trainUseCase.removeTrain(train).collect {}
        }
        changesHave()
    }

    fun onDeletePassenger(passenger: Passenger) {
        _currentRoute.update { route ->
            route?.copy(passengers = route.passengers.filter { it != passenger } as MutableList<Passenger>)
        }
        deletedPassengerList.add(passenger)
        viewModelScope.launch(Dispatchers.IO) {
            passengerUseCase.removePassenger(passenger).collect {}
        }
        changesHave()
    }

    fun onDeleteOtherWork(otherWork: OtherWork) {
        _currentRoute.update { route ->
            route?.copy(otherWorks = route.otherWorks.filter { it != otherWork } as MutableList<OtherWork>)
        }
        deletedOtherWorkList.add(otherWork)
        deleteOtherWorkJob = viewModelScope.launch(Dispatchers.IO) {
            otherWorkUseCase.removeOtherWork(otherWork).collect {}
        }
        changesHave()
    }

    /** Удаление напарника из маршрута (свайп в блоке «Напарники» + подтверждение). */
    fun onDeletePartner(partner: RoutePartner) {
        _currentRoute.update { route ->
            route?.copy(partners = route.partners.filter { it != partner } as MutableList<RoutePartner>)
        }
        viewModelScope.launch(Dispatchers.IO) {
            routePartnerUseCase.removePartner(partner).collect {}
        }
        changesHave()
    }

    /**
     * «Добавить напарника» из формы маршрута → экран мультивыбора из справочника.
     * Сохраняем маршрут (гарантируем basicId в БД), затем навигация.
     */
    fun onAddPartnersClick() {
        val basicId = currentRoute.value?.basicData?.id ?: return
        preSaveRoute()
        viewModelScope.launch {
            _events.emit(FormScreenEvent.NavigateToPartnerPicker(basicId))
        }
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
                        deletedOtherWorkList.forEach { otherWork ->
                            otherWorkUseCase.removeOtherWork(otherWork).collect {}
                        }
                        _events.emit(FormScreenEvent.RouteSaved)
                        // Оба тоста показываем из этого scope ПОСЛЕ навигации:
                        // FormScreen ещё активен как SnackbarManager-коллектор в момент
                        // RouteSaved — он поглощает событие из Channel и исчезает вместе
                        // с экраном. Delay даёт навигации завершиться (~300мс анимация),
                        // после чего HomeScreen становится активным коллектором.
                        val savedRouteId = routeToSave.basicData.id
                        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                            delay(400)
                            snackbarManager.show("Маршрут сохранен")
                            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                            if (rawToken.isNullOrBlank()) return@launch
                            syncManager.syncRoute(savedRouteId, "Bearer $rawToken")
                                .collect { result ->
                                    if (result is ResultState.Success) {
                                        snackbarManager.show("Маршрут сохранен в облаке")
                                    }
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
        // Нет несохранённых изменений — не пересохраняем маршрут при выходе, просто
        // закрываем экран. Покрывает два случая: пустой новый черновик (открыли и сразу
        // «назад») и открытие существующего маршрута на просмотр без правок — раньше он
        // всё равно перезаписывался. Shared-preview и isDeleted исключаем: им нужна
        // фактическая запись (снятие isDeleted).
        val needsPersistAnyway =
            _isSharedPreview.value || currentRoute.value?.basicData?.isDeleted == true
        if (!_uiState.value.changesHaveState && !needsPersistAnyway) {
            sharedPreferenceStorage.setTokenIsChangeHave(false)
            _uiState.update { it.copy(exitFromScreen = true) }
            return
        }
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
                            _shareLinkEvent.emit(
                                com.z_company.route.util.ShareLinkData.fromRoute(route, result.data)
                            )
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

    // buildShareText удалён — логика перенесена в ShareLinkData.fromRoute()

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
        // Если маршрут уже в БД и нет несохранённых изменений — переход в подраздел
        // (Локомотив/Поезд/Пассажир) не требует записи. Это убирает лишние save при
        // открытии существующего маршрута и тапе по «Локомотив».
        if (isPersistedToDb && !_uiState.value.changesHaveState) return
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
                // BasicData (номер, примечания) хранится в памяти —
                // перезапись из БД затирает текущий ввод пользователя.
                // Исключения — времена, которые задаются на дочерних экранах:
                //   • timeEndWork — из шторки сдачи в LocoFormScreen;
                //   • timeStartWork / timeStartWorkBeforeArrival — «явка по прибытию»
                //     на экране «Пассажиром». Их берём из БД, иначе изменение с
                //     дочернего экрана не отражается до повторного входа в форму.
                _currentRoute.update { inMemory ->
                    (inMemory?.copy(
                        locomotives = dbRoute.locomotives,
                        trains = dbRoute.trains,
                        passengers = dbRoute.passengers,
                        otherWorks = dbRoute.otherWorks,
                        partners = dbRoute.partners,
                        photos = dbRoute.photos,
                        basicData = inMemory.basicData.copy(
                            timeEndWork = dbRoute.basicData.timeEndWork,
                            timeStartWork = dbRoute.basicData.timeStartWork,
                            timeStartWorkBeforeArrival = dbRoute.basicData.timeStartWorkBeforeArrival
                        )
                    ) ?: dbRoute)
                        // Инвариант: если пассажир помечен «явкой по прибытию», явка
                        // всегда равна его прибытию — не зависим от тайминга записи в БД.
                        .withWorkStartFromPassenger()
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
