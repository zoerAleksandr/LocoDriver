package com.z_company.route.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
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
import com.z_company.domain.util.sum
import com.z_company.domain.util.toIntOrZero
import com.z_company.route.Const.NULLABLE_ID
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.review.RuStoreReviewManagerFactory
import ru.rustore.sdk.review.model.ReviewInfo
import java.util.UUID

class FormViewModel(
    private val routeId: String?,
    private val isCopy: Boolean = false,
    application: Application,
) : ViewModel(), KoinComponent {
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


    var timeZoneText: String = "GMT+3"

    private var isNewRoute = routeId == NULLABLE_ID

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
            if (isNewRoute) {
                _currentRoute.value =
                    Route(basicData = BasicData(id = UUID.randomUUID().toString()))
            } else {
                routeId?.let { id ->
                    routeUseCase.routeDetails(id).collectLatest { result ->
                        if (result is ResultState.Success) {
                            val loadedRoute = if (isCopy) result.data?.copy(
                                basicData = result.data!!.basicData.copy(
                                    id = UUID.randomUUID().toString()
                                )
                            ) else result.data
                            _currentRoute.value = loadedRoute
                            _uiState.update { it.copy(routeDetailState = result) }
                            countLoadRoute += 1
                            if (countLoadRoute > 1) {
                                changesHave()
                            }
                        } else {
                            _uiState.update { it.copy(routeDetailState = result) }
                        }
                    }
                }
            }
        }
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

            val deferredSurchargeAtLongDistanceTrain = async {
                salaryCalculationHelper.getMoneyLongDistanceTrainFlow().first()
            }

            val deferredSurchargeAtExtendedServicePhase = async {
                salaryCalculationHelper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
            }

            val deferredSurchargeAtHeavyTrains = async {
                salaryCalculationHelper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
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
            val surchargeAtLongDistanceTrain = deferredSurchargeAtLongDistanceTrain.await()
            val surchargeAtExtendedServicePhase = deferredSurchargeAtExtendedServicePhase.await()
            val surchargeAtHeavyTrains = deferredSurchargeAtHeavyTrains.await()
            val moneyAtQualificationClass = deferredMoneyAtQualificationClass.await()
            val nordicSurcharge = deferredNordicSurcharge.await()
            val districtSurcharge = deferredDistrictSurcharge.await()
            val moneyAtHarmfulness = deferredMoneyAtHarmfulness.await()
            val otherSurchargeMoney = deferredOtherSurchargeMoney.await()
            val moneyAtOnePerson = deferredMoneyAtOnePerson.await()

            // Теперь, когда все значения получены, выполняем суммирование
            val surchargeAtTrains = surchargeAtLongDistanceTrain + surchargeAtExtendedServicePhase + surchargeAtHeavyTrains

            val otherSurcharge = moneyAtQualificationClass + nordicSurcharge + districtSurcharge + moneyAtHarmfulness + otherSurchargeMoney

            val totalMoney = moneyAtTariffRate + moneyAtNightHours + zonalSurchargeMoney + moneyAtPassengerTime + moneyAtHoliday + surchargeAtTrains + moneyAtOnePerson + otherSurcharge

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
                        otherSurcharge = otherSurcharge
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
                offsetInMoscow = currentTimeZoneOffset ?: 0L
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

    // Сохранение
    fun saveRoute() {
        Log.d("zzz", "saveRoute")
        saveRouteJob?.cancel()
        saveRouteJob = viewModelScope.launch(Dispatchers.IO) {
            currentRoute.value?.let { route ->
                val locomotives = getLocoList(route.basicData.id)
                val trains = getTrainList(route.basicData.id)
                val passengers = getPassengerList(route.basicData.id)
                val routeToSave = route.copy(
                    locomotives = locomotives,
                    trains = trains,
                    passengers = passengers
                )
                routeUseCase.saveRoute(routeToSave).collectLatest { result ->
                    Log.d("zzz", "saveResult $result")
                    _uiState.update { it.copy(saveRouteState = result) }
                    if (result is ResultState.Success) {
                        deletedLocoList.forEach { loco ->
                            locoUseCase.removeLoco(loco)
                        }
                        deletedTrainList.forEach { train ->
                            trainUseCase.removeTrain(train)
                        }
                        deletedPassengerList.forEach { passenger ->
                            passengerUseCase.removePassenger(passenger)
                        }
                        _events.emit(FormScreenEvent.RouteSaved)
                    }
                }
            }
        }
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
    }

    fun checkPurchasesAvailability() {
        viewModelScope.launch(Dispatchers.IO) {
            when (val checkResult = subscriptionHelper.checkPurchasesAvailabilitySuspend()) {
                is ResultState.Success -> {
                    _purchasesEvent.emit(StartPurchasesEvent.PurchasesAvailability(checkResult.data))
                }

                is ResultState.Error -> {
                    snackbarManager.show(
                        message = "Ошибка ${checkResult.entity.message}",
                        showOnceKey = "checkPurchasesAvailability"
                    )
                }

                else -> {}
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            subscriptionHelper.restorePurchases(snackbarManager)
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            when (routeHelper.newRouteClick()) {
                is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                    _alertBeforePurchasesEvent.emit(AlertBeforePurchasesEvent.ShowDialogNeedSubscribe)
                }

                is RouteActionsHelper.NewRouteResult.AlertSubscribeDialog -> {
                    _alertBeforePurchasesEvent.emit(AlertBeforePurchasesEvent.ShowDialogAlertSubscribe)
                }

                is RouteActionsHelper.NewRouteResult.ShowNewRouteScreen -> {
                    saveRoute()
                }

                is RouteActionsHelper.NewRouteResult.Error -> {}
            }
        }
    }

    fun setFavoriteRoute() {
        _currentRoute.update { route ->
            route?.copy(basicData = route.basicData.copy(isFavorite = !route.basicData.isFavorite))
        }
        changesHave()
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
                    subscribeToChanges(route.basicData.id)
                    changesHave()
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun subscribeToChanges(routeId: String) {
        loadRouteJob?.cancel()
        loadRouteJob = routeUseCase.routeDetails(routeId).onEach { routeState ->
            _uiState.update { it.copy(routeDetailState = routeState) }
            if (routeState is ResultState.Success) {
                _currentRoute.value = routeState.data
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

    override fun onCleared() {
        super.onCleared()
        loadRouteJob?.cancel()
        saveRouteJob?.cancel()
        loadSettingsJob?.cancel()
        deleteLocoJob?.cancel()
        deleteTrainJob?.cancel()
        deletePassengerJob?.cancel()
    }
}