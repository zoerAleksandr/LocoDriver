package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.route.Route
import com.z_company.domain.helpers.RouteRestCalculator
import com.z_company.domain.helpers.SalaryCalculationHelper
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.sum
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * iOS ViewModel для экрана FormScreen.
 *
 * Предоставляет:
 *  - Основные данные маршрута (номер, время работы, заметки).
 *  - Переключатель работы в одно лицо.
 *  - Расчёт ЗП за маршрут (категории + итог) через [SalaryCalculationHelper].
 *  - Блок отдыха (короткий / полный / домашний) через [RouteRestCalculator].
 *  - Действия: сохранить, добавить в избранное, поделиться, удалить.
 *
 * Состояние сбрасывается при вызове [loadRoute].
 */
@OptIn(ExperimentalTime::class)
class FormIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val shareRouteManager: ShareRouteManager,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val routeRestCalculator: RouteRestCalculator,
) : ViewModel() {

    // ── Основное состояние ────────────────────────────────────────────────
    private val _route = MutableStateFlow<Route?>(null)
    val route: StateFlow<Route?> = _route.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isDeleted = MutableStateFlow(false)
    val isDeleted: StateFlow<Boolean> = _isDeleted.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _shareLink = MutableStateFlow<String?>(null)
    val shareLink: StateFlow<String?> = _shareLink.asStateFlow()

    // ── ЗП состояние (каждая категория в копейках / рублях) ───────────────
    private val _salary = MutableStateFlow(SalaryForRouteState())
    val salary: StateFlow<SalaryForRouteState> = _salary.asStateFlow()

    // ── Отдых состояние ───────────────────────────────────────────────────
    private val _rest = MutableStateFlow(RestState())
    val rest: StateFlow<RestState> = _rest.asStateFlow()

    private var loadJob: Job? = null
    private var calcJob: Job? = null

    // ── Кэш настроек «стандартное время работы» ─────────────────────────────
    // Обновляются реактивно из SettingsUseCase. Используются в setTimeStartWork,
    // чтобы при первичном указании времени явки автоматически проставить окончание.
    private var defaultWorkTime: Long? = null
    private var usingDefaultWorkTime: Boolean = false

    init {
        viewModelScope.launch {
            settingsUseCase.getFlowCurrentSettingsState().collectLatest { result ->
                if (result is ResultState.Success) {
                    defaultWorkTime = result.data.defaultWorkTime
                    usingDefaultWorkTime = result.data.usingDefaultWorkTime
                }
            }
        }
    }

    /**
     * Сбрасывает одноразовые «событийные» флаги (isSaved, isDeleted, shareLink, errorMessage).
     *
     * Используется Swift-обёрткой при открытии экрана формы: ViewModel — `single`
     * в Koin, поэтому между показами формы в нём может остаться `isSaved = true`
     * от прошлого сохранения. Если не сбросить ДО подписки SwiftUI onChange,
     * то при повторном открытии будет дёргать dismiss() по «старому» событию.
     */
    fun resetTransientState() {
        _isSaved.value = false
        _isDeleted.value = false
        _shareLink.value = null
        _errorMessage.value = null
    }

    /** Загружает маршрут по [routeId]. Если routeId == null — создаёт новый пустой маршрут. */
    fun loadRoute(routeId: String?) {
        loadJob?.cancel()
        calcJob?.cancel()
        _isSaved.value = false
        _isDeleted.value = false
        _shareLink.value = null
        _errorMessage.value = null
        _salary.value = SalaryForRouteState()
        _rest.value = RestState()

        if (routeId == null) {
            // Новый маршрут: автоматически ставим начало работы = сейчас.
            // Если в настройках включено usingDefaultWorkTime — через
            // setTimeStartWork так же автоматически проставится окончание.
            val emptyRoute = Route()
            _route.value = emptyRoute
            _isLoading.value = false
            setTimeStartWork(Clock.System.now().toEpochMilliseconds())
            return
        }

        loadJob = viewModelScope.launch {
            _isLoading.value = true
            routeUseCase.routeDetails(routeId).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _route.value = result.data ?: Route()
                        _isLoading.value = false
                        startCalculations()
                    }
                    is ResultState.Error -> {
                        _errorMessage.value = result.entity.message ?: "Ошибка загрузки маршрута"
                        _isLoading.value = false
                    }
                    is ResultState.Loading -> _isLoading.value = true
                }
            }
        }
    }

    // ── Редакторы полей ────────────────────────────────────────────────────
    // Номер и заметки НЕ участвуют в расчёте ЗП и отдыха — обновляем только
    // состояние маршрута без перезапуска тяжёлого startCalculations(). Иначе
    // на каждое нажатие клавиши в TextField запускается пересчёт (18 Flow-цепочек
    // ЗП + загрузка маршрутов за 2 месяца для домашнего отдыха), из-за чего
    // курсор в поле ввода подтормаживает относительно ввода.
    fun updateNumber(value: String) = updateBasicNoRecalc { it.copy(number = value.ifBlank { null }) }
    fun updateNotes(value: String) = updateBasicNoRecalc { it.copy(notes = value.ifBlank { null }) }

    /**
     * Устанавливает время явки. Если в настройках включено «использовать
     * стандартное время работы» и окончание ещё не указано, автоматически
     * проставляет окончание = [ms] + defaultWorkTime.
     */
    fun setTimeStartWork(ms: Long?) {
        val current = _route.value ?: return
        val currentEnd = current.basicData.timeEndWork
        val autoEnd: Long? = if (
            ms != null &&
            usingDefaultWorkTime &&
            (defaultWorkTime ?: 0L) > 0L &&
            (currentEnd == null || currentEnd == 0L)
        ) {
            ms + (defaultWorkTime ?: 0L)
        } else {
            currentEnd
        }
        updateBasic { it.copy(timeStartWork = ms, timeEndWork = autoEnd) }
    }

    fun setTimeEndWork(ms: Long?) = updateBasic { it.copy(timeEndWork = ms) }
    fun setOnePersonOperation(checked: Boolean) =
        updateBasic { it.copy(isOnePersonOperation = checked) }

    private inline fun updateBasic(transform: (com.z_company.domain.entities.route.BasicData) -> com.z_company.domain.entities.route.BasicData) {
        val current = _route.value ?: return
        _route.value = current.copy(basicData = transform(current.basicData))
        startCalculations()
    }

    /**
     * Обновляет basicData БЕЗ запуска тяжёлых расчётов ЗП/отдыха.
     * Используется для полей, которые не влияют на расчёты (номер, заметки).
     */
    private inline fun updateBasicNoRecalc(transform: (com.z_company.domain.entities.route.BasicData) -> com.z_company.domain.entities.route.BasicData) {
        val current = _route.value ?: return
        _route.value = current.copy(basicData = transform(current.basicData))
    }

    // ── Расчёты (salary + rest) ───────────────────────────────────────────
    private fun startCalculations() {
        val currentRoute = _route.value ?: return
        calcJob?.cancel()
        calcJob = viewModelScope.launch {
            try {
                val settings = settingsUseCase.getUserSettingFlow().first()
                val salarySetting = salarySettingUseCase.salarySettingFlow().first()
                val helper = SalaryCalculationHelper(
                    userSettings = settings,
                    salarySetting = salarySetting,
                    routeList = listOf(currentRoute),
                )

                val tariff = helper.getMoneyAtWorkTimeAtTariffSingleRoute().first()
                val night = helper.getMoneyAtNightTimeFlow().first()
                val zonal = helper.getMoneyZonalSurchargeFlow().first()
                val passenger = helper.getMoneyAtPassengerFlow().first()
                val holiday = helper.getMoneyAtHolidayFlow().first()
                val servicePhase = helper.getMoneyListSurchargeExtendedServicePhaseFlow().first().sum()
                val heavy = helper.getMoneyListSurchargeExtendedHeavyTrainsFlow().first().sum()
                val long = helper.getMoneyListSurchargeLongTrainsFlow().first().sum()
                val doubledFirst = helper.getMoneyDoubledTrainFirstSurchargeFlow().first()
                val doubledSecond = helper.getMoneyDoubledTrainSecondSurchargeFlow().first()
                val onePerson = helper.getMoneyOnePersonOperationFlow().first() +
                        helper.getMoneyOnePersonOperationPassengerTrainFlow().first()
                val qualification = helper.getMoneyAtQualificationClassFlow().first()
                val harmful = helper.getMoneyHarmfulnessFlow().first()
                val other = helper.getMoneyOtherSurchargeFlow().first()
                val nordic = helper.getMoneyNordicSurcharge().first()
                val district = helper.getMoneyDistrictSurcharge().first()
                val overRest = helper.getMoneyOverRestFlow().first()

                val surchargesAtTrain =
                    servicePhase + heavy + long + doubledFirst + doubledSecond
                val otherSurcharge =
                    qualification + nordic + district + harmful + other
                val total =
                    tariff + night + zonal + passenger + holiday + surchargesAtTrain +
                            onePerson + otherSurcharge + overRest

                _salary.value = SalaryForRouteState(
                    isCalculated = true,
                    isSetTariffRate = settings.selectMonthOfYear.dateSetTariffRate != null,
                    totalPayment = total,
                    paymentAtTariffRate = tariff,
                    paymentAtNightTime = night,
                    zonalSurchargeMoney = zonal,
                    paymentAtPassengerTime = passenger,
                    paymentHolidayMoney = holiday,
                    surchargesAtTrain = surchargesAtTrain,
                    paymentAtOnePerson = onePerson,
                    otherSurcharge = otherSurcharge,
                    overRestMoney = overRest,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                // расчёт ЗП — не ломаем экран, но логируем
                _errorMessage.value = "Ошибка расчёта ЗП: ${t.message ?: t::class.simpleName}"
            }

            // отдых — независимо. Любые ошибки расчёта домашнего отдыха кладём
            // ТОЛЬКО в RestState.errorMessage (для показа внутри шторки),
            // а не в _errorMessage (который триггерит SwiftUI alert).
            try {
                val startMs = currentRoute.basicData.timeStartWork
                val endMs = currentRoute.basicData.timeEndWork
                val currentWorkDuration = if (startMs != null && endMs != null && endMs > startMs) {
                    endMs - startMs
                } else null

                // Короткий / полный отдых — считаем всегда, они локальные.
                val short = routeRestCalculator.calculateShortRest(currentRoute).first()
                val full = routeRestCalculator.calculateFullRest(currentRoute).first()

                // Домашний отдых — может вернуть Error, который мы кладём только
                // в RestState.errorMessage.
                val homeResult = routeRestCalculator.calculationHomeRest(currentRoute).first {
                    it is ResultState.Success || it is ResultState.Error
                }
                val homeData = (homeResult as? ResultState.Success)?.data
                val homeError = (homeResult as? ResultState.Error)?.entity?.message

                _rest.value = RestState(
                    homeDuration = homeData?.duration,
                    homeEndTime = homeData?.endTime,
                    shortDuration = short?.first,
                    shortEndTime = short?.second,
                    fullDuration = full?.first,
                    fullEndTime = full?.second,
                    currentWorkDuration = currentWorkDuration,
                    chainWorkTotal = homeData?.chainWorkTotal,
                    chainPORestTotal = homeData?.chainPORestTotal,
                    chainSize = homeData?.chainSize ?: 0,
                    errorMessage = homeError,
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
                // не ломаем экран — молча, без алерта
            }
        }
    }

    // ── Действия пользователя ────────────────────────────────────────────

    /**
     * Переключает тип отдыха: «Домашний» ↔ «Отдых в ПО».
     * Флаг хранится в [BasicData.restPointOfTurnover].
     */
    fun toggleRestType() {
        updateBasic { it.copy(restPointOfTurnover = !it.restPointOfTurnover) }
    }

    /** Добавить / убрать из избранного. */
    fun toggleFavorite() {
        val current = _route.value ?: return
        val newState = !current.basicData.isFavorite
        _route.value = current.copy(basicData = current.basicData.copy(isFavorite = newState))
        viewModelScope.launch {
            routeUseCase.setFavoriteRoute(current.basicData.id, newState).collect { /* ignore */ }
        }
    }

    /** Сохранить и запустить per-route синхронизацию. Выставит [isSaved]=true по завершении. */
    fun saveRoute() {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            routeUseCase.saveRoute(currentRoute).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _isSaved.value = true
                        // Запускаем sync в отдельной корутине — не блокируем UI
                        viewModelScope.launch {
                            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                            if (!rawToken.isNullOrBlank()) {
                                syncManager.syncRoute(
                                    currentRoute.basicData.id,
                                    "Bearer $rawToken"
                                ).collect { /* ignore */ }
                            }
                        }
                    }
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Ошибка сохранения"
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /** Создать публичную ссылку и выставить [shareLink]. */
    fun shareRoute() {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (rawToken.isNullOrBlank()) {
                _errorMessage.value = "Войдите в аккаунт, чтобы делиться маршрутами"
                return@launch
            }
            shareRouteManager.createShareLink(currentRoute, "Bearer $rawToken").collect { result ->
                when (result) {
                    is ResultState.Success -> _shareLink.value = result.data
                    is ResultState.Error -> _errorMessage.value =
                        result.entity.message ?: "Не удалось создать ссылку"
                    is ResultState.Loading -> {}
                }
            }
        }
    }

    /** Помечает маршрут как удалённый (soft-delete), выставит [isDeleted]=true. */
    fun deleteRoute() {
        val currentRoute = _route.value ?: return
        viewModelScope.launch {
            routeUseCase.markAsRemoved(currentRoute).collect { result ->
                if (result is ResultState.Success) _isDeleted.value = true
            }
        }
    }

    /** Очистить [shareLink] после показа Share Sheet. */
    fun consumeShareLink() {
        _shareLink.value = null
    }

    /** Очистить [errorMessage]. */
    fun consumeError() {
        _errorMessage.value = null
    }

    // ── watchState helpers (для Swift-обёрток) ────────────────────────────
    fun watchRoute(callback: (Route?) -> Unit) {
        viewModelScope.launch { route.collect { callback(it) } }
    }
    fun watchIsLoading(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isLoading.collect { callback(it) } }
    }
    fun watchIsSaved(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isSaved.collect { callback(it) } }
    }
    fun watchIsDeleted(callback: (Boolean) -> Unit) {
        viewModelScope.launch { isDeleted.collect { callback(it) } }
    }
    fun watchErrorMessage(callback: (String?) -> Unit) {
        viewModelScope.launch { errorMessage.collect { callback(it) } }
    }
    fun watchShareLink(callback: (String?) -> Unit) {
        viewModelScope.launch { shareLink.collect { callback(it) } }
    }
    fun watchSalary(callback: (SalaryForRouteState) -> Unit) {
        viewModelScope.launch { salary.collect { callback(it) } }
    }
    fun watchRest(callback: (RestState) -> Unit) {
        viewModelScope.launch { rest.collect { callback(it) } }
    }

    // ── DTO для UI ────────────────────────────────────────────────────────

    data class SalaryForRouteState(
        val isCalculated: Boolean = false,
        val isSetTariffRate: Boolean = false,
        val totalPayment: Double? = null,
        val paymentAtTariffRate: Double? = null,
        val zonalSurchargeMoney: Double? = null,
        val paymentAtNightTime: Double? = null,
        val paymentAtPassengerTime: Double? = null,
        val paymentHolidayMoney: Double? = null,
        val surchargesAtTrain: Double? = null,
        val paymentAtOnePerson: Double? = null,
        val otherSurcharge: Double? = null,
        val overRestMoney: Double? = null,
    )

    data class RestState(
        val homeDuration: Long? = null,
        val homeEndTime: Long? = null,
        val shortDuration: Long? = null,
        val shortEndTime: Long? = null,
        val fullDuration: Long? = null,
        val fullEndTime: Long? = null,
        /** Продолжительность работы текущего маршрута (timeEndWork - timeStartWork). */
        val currentWorkDuration: Long? = null,
        /** Суммарная продолжительность работы всей цепочки маршрутов для домашнего отдыха. */
        val chainWorkTotal: Long? = null,
        /** Суммарный отдых в ПО между маршрутами цепочки (0 если цепочка из одного маршрута). */
        val chainPORestTotal: Long? = null,
        /** Размер цепочки маршрутов. */
        val chainSize: Int = 0,
        /** Сообщение об ошибке расчёта домашнего отдыха (для показа внутри шторки, не алертом). */
        val errorMessage: String? = null,
    )
}
