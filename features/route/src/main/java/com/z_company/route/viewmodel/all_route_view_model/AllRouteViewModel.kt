package com.z_company.route.viewmodel.all_route_view_model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ErrorEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SalarySetting
import com.z_company.domain.entities.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.OpenRouteFormEvent
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.use_case.RuStoreUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.rustore.sdk.billingclient.RuStoreBillingClient
import ru.rustore.sdk.billingclient.model.purchase.PurchaseState
import ru.rustore.sdk.billingclient.utils.pub.checkPurchasesAvailability
import ru.rustore.sdk.core.feature.model.FeatureAvailabilityResult
import java.util.Calendar.getInstance
import java.util.TimeZone

enum class RouteFilter {
    ALL,
    FAVORITES,
    HEAVY,
    EXTENDED_SERVICE,
    FOLLOWING_RESERVE,
    ONE_PERSON,
    OVER_12_HOURS,
    LONG_TRAINS
}

data class RoutesUiState(
    val isLoading: Boolean = true,
    val routes: List<ItemState> = emptyList(),
    val filteredRoutes: List<ItemState> = emptyList(),
    val selectedFilters: Set<RouteFilter> = setOf(RouteFilter.ALL),
    val errorMessage: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val currentMonthOfYear: MonthOfYear? = null,
    val syncRouteState: ResultState<String>? = null,
    val removeRouteState: ResultState<Unit>? = null,
    val restoreSubscriptionState: ResultState<String>? = null,
    val showConfirmDialogRemoveRoute: Boolean = false
)

enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    WORKTIME_ASC,
    WORKTIME_DESC
}

class AllRouteViewModel() : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val billingClient: RuStoreBillingClient by inject()
    private val ruStoreUseCase: RuStoreUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private var removeRouteJob: Job? = null

    private val _uiState = MutableStateFlow(RoutesUiState())
    val uiState: StateFlow<RoutesUiState> = _uiState.asStateFlow()

    private val _openRouteFormEvent = MutableSharedFlow<OpenRouteFormEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openRouteFormEvent: SharedFlow<OpenRouteFormEvent> = _openRouteFormEvent.asSharedFlow()

    private val _alertBeforePurchasesEvent = MutableSharedFlow<AlertBeforePurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val alertBeforePurchasesEvent = _alertBeforePurchasesEvent.asSharedFlow()

    private val _purchasesEvent = MutableSharedFlow<StartPurchasesEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val purchasesEvent = _purchasesEvent.asSharedFlow()

    private val _previewRouteUiState = MutableStateFlow(PreviewRouteUiState())
    val previewRouteUiState = _previewRouteUiState.asStateFlow()

    private val latestRawRoutes = MutableStateFlow<List<ItemState>>(emptyList())

    var offsetInMoscow: Long = 0L
    var dateAndTimeConverter: DateAndTimeConverter? = null
    var minTimeRest: Long = 0L
    var minTimeHomeRest: Long = 0L

    private var salarySetting: SalarySetting? = null
    private var userSettings: UserSettings? = null

    // month/year lists for pickers
    private val _monthList = MutableStateFlow<List<Int>>(emptyList())
    val monthList: StateFlow<List<Int>> = _monthList.asStateFlow()

    private val _yearList = MutableStateFlow<List<Int>>(emptyList())
    val yearList: StateFlow<List<Int>> = _yearList.asStateFlow()

    init {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState()
                .collect { list ->
                    val months = list.map { it.month }.distinct().sorted()
                    val years = list.map { it.year }.distinct().sorted()
                    _monthList.value = months
                    _yearList.value = years
                }
        }

        // combinedData — поток настроек и salary (без stateIn, можно оставить stateIn если нужно)
        val combinedData: Flow<LoadSettingData> = combine(
            salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }
                .onStart { emit(null) },
            settingsUseCase.getUserSettingFlow().map { it as UserSettings? }
                .onStart { emit(null) },
        ) { us, ss ->
            LoadSettingData(ss, us)
        }

        // 1) объединяем combinedData с latestRawRoutes и выбранными фильтрами
        viewModelScope.launch {
            combine(
                combinedData,
                latestRawRoutes,
                _uiState.map { it.selectedFilters }.distinctUntilChanged()
            ) { initData, rawRoutes, filters ->
                Triple(initData, rawRoutes, filters)
            }.collectLatest { (initData, rawRoutes, filters) ->
                // если настройки ещё не готовы — можно очистить или выставить загрузку
                userSettings = initData.userSettings
                salarySetting = initData.salarySetting
                val user = initData.userSettings
                val salary = initData.salarySetting
                if (user == null || salary == null) {
                    _uiState.update {
                        it.copy(
                            filteredRoutes = emptyList(),
                            isLoading = true,
                        )
                    }
                    return@collectLatest
                }

                // конвертер и т.п.
                dateAndTimeConverter = DateAndTimeConverter(user)
                val currentMonth = initData.userSettings.selectMonthOfYear
                minTimeRest = user.minTimeRestPointOfTurnover
                minTimeHomeRest = user.minTimeHomeRest

                loadRoutes(user)
                // строим состояния маршрутов (ItemState) — если rawRoutes уже в нужном виде, можно использовать их напрямую
                val routeStateList = rawRoutes // если latestRawRoutes хранит ItemState
                // применяем фильтры
                val filtered = applyFilters(routeStateList, filters, salarySetting = salary)
                _uiState.update {
                    it.copy(
                        filteredRoutes = filtered,
                        isLoading = false,
                        currentMonthOfYear = currentMonth,
                    )
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
                 snackbarManager.show(message = "Ошибка ${throwable.message}", showOnceKey = "checkPurchasesAvailability")
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
                                    snackbarManager.show(
                                        message = "Покупки восстановлены",
                                        showOnceKey = "restore_purchases_success"
                                    )
                                    _uiState.update { it.copy(restoreSubscriptionState = null) }
                                }
                                if (maxEndTime < currentTimeInMillis) {
                                    snackbarManager.show(
                                        message = "Действующих подписок не найдено",
                                        showOnceKey = "restore_purchases_none"
                                    )
                                    _uiState.update { it.copy(restoreSubscriptionState = null) }
                                }
                            }
                        }
                        .addOnFailureListener {
                            snackbarManager.show(
                                message = "Ошибка получения данных от сервера",
                            )
                            _uiState.update { it.copy(restoreSubscriptionState = null) }
                        }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            restoreSubscriptionState = ResultState.Error(
                                ErrorEntity(e)
                            )
                        )
                    }
                    snackbarManager.show(message = e.message ?: "Ошибка при восстановлении")
                }
            }
        }
    }

    fun newRouteClick(basicId: String? = null) {
        viewModelScope.launch {
            when (val decision =
                routeHelper.newRouteClick(basicId = basicId, isMakeCopy = basicId != null)) {
                is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                    _alertBeforePurchasesEvent.tryEmit(AlertBeforePurchasesEvent.ShowDialogNeedSubscribe)
                }

                is RouteActionsHelper.NewRouteResult.AlertSubscribeDialog -> {
                    _alertBeforePurchasesEvent.tryEmit(AlertBeforePurchasesEvent.ShowDialogAlertSubscribe)
                }

                is RouteActionsHelper.NewRouteResult.ShowNewRouteScreen -> {
                    _openRouteFormEvent.tryEmit(
                        OpenRouteFormEvent(
                            decision.basicId,
                            decision.isMakeCopy
                        )
                    )
                }

                is RouteActionsHelper.NewRouteResult.Error -> {
                    _uiState.update { it.copy() }
                }
            }
        }
    }

    fun syncRoute(route: Route) {
        viewModelScope.launch {
            routeHelper.syncRoute(route).collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        // show snackbar centrally
                        snackbarManager.show(message = result.data)
                        _uiState.update { it.copy(syncRouteState = null) }
                    }

                    is ResultState.Error -> {
                        val message = result.entity.message ?: result.entity.throwable?.message
                        ?: "Ошибка синхронизации"
                        snackbarManager.show(message = message)
                        _uiState.update { it.copy(syncRouteState = ResultState.Error(result.entity)) }
                    }

                    is ResultState.Loading -> {
                        _uiState.update { it.copy(syncRouteState = ResultState.Loading()) }
                    }
                }
            }
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

    fun deleteRoute(route: Route) {
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

    fun loadRoutes(userSettings: UserSettings) {
        viewModelScope.launch {
            routeUseCase.listRoutesByMonth(userSettings.selectMonthOfYear, userSettings.timeZone)
                .onStart { _uiState.update { it.copy(isLoading = true, errorMessage = null) } }
                .collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }

                        is ResultState.Success -> {
                            if (salarySetting != null && dateAndTimeConverter != null) {
                                val timeZone = dateAndTimeConverter!!.timeZoneText
                                val currentTimeCalendar =
                                    getInstance(TimeZone.getTimeZone(timeZone))
                                val currentTimeInMillis = currentTimeCalendar.timeInMillis

                                val routeList = if (userSettings.isConsiderFutureRoute) {
                                    result.data
                                } else {
                                    result.data.filter { it.basicData.timeStartWork!! < currentTimeInMillis }
                                }
                                val routeStateList = mutableListOf<ItemState>()
                                routeList.forEach { route ->
                                    val routeState = ItemState(
                                        route = route,
                                        isHoliday = isHolidayTimeInRoute(
                                            userSettings.selectMonthOfYear,
                                            userSettings,
                                            route
                                        ),
                                        isHeavyTrains = isHeavyTrains(salarySetting!!, route),
                                        isExtendedServicePhaseTrains = isExtendedServicePhaseTrains(
                                            salarySetting!!,
                                            route
                                        )
                                    )
                                    routeStateList.add(routeState)
                                }

                                val data = routeStateList
                                latestRawRoutes.value = data
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        routes = data,
                                        errorMessage = null
                                    )
                                }
                            }
                        }

                        is ResultState.Error -> {
                            val message = result.entity.message ?: result.entity.throwable?.message
                            ?: "Ошибка загрузки маршрутов"
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = message
                                )
                            }
                            // показываем snackbar централизованно
                            snackbarManager.show(message = message)
                        }
                    }
                }
        }
    }

    fun setSort(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun toggleFilter(filter: RouteFilter) {
        _uiState.update { current ->
            val newSet = current.selectedFilters.toMutableSet()
            if (filter == RouteFilter.ALL) {
                newSet.clear()
                newSet.add(RouteFilter.ALL)
            } else {
                newSet.remove(RouteFilter.ALL)
                if (newSet.contains(filter)) newSet.remove(filter) else newSet.add(filter)
                if (newSet.isEmpty()) newSet.add(RouteFilter.ALL)
            }
            current.copy(selectedFilters = newSet)
        }
    }

    fun reload() {
        userSettings?.let { setting ->
            loadRoutes(setting)
        }
    }

    private fun applyFilters(
        routesState: List<ItemState>,
        filters: Set<RouteFilter>,
        salarySetting: SalarySetting
    ): List<ItemState> {
        if (filters.contains(RouteFilter.ALL)) return routesState

        val over12hMillis = 43_200_000L

        return routesState.filter { routeState ->

            var ok = true

            if (filters.contains(RouteFilter.FAVORITES)) {
                ok = ok && (routeState.route.basicData?.isFavorite == true)
            }
            if (filters.contains(RouteFilter.HEAVY)) {
                ok = ok && runCatching {
                    isHeavyTrains(
                        salarySetting,
                        routeState.route
                    )
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.EXTENDED_SERVICE)) {
                ok = ok && runCatching {
                    isExtendedServicePhaseTrains(
                        salarySetting,
                        routeState.route
                    )
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.LONG_TRAINS)) {
                ok =
                    ok && runCatching {
                        routeState.route.getLongDistanceTime(/* lengthIsLongDistance: Int */0) > 0L
                    }.getOrDefault(
                        false
                    )
            }
            if (filters.contains(RouteFilter.FOLLOWING_RESERVE)) {
                val has = routeState.route.trains.any { train ->
                    runCatching {
                        train.timeFollowingSingleLocomotive(
                            routeState.route.basicData?.timeStartWork,
                            routeState.route.basicData?.timeEndWork
                        )
                    }.getOrDefault(0L) > 0L
                }
                ok = ok && has
            }
            if (filters.contains(RouteFilter.ONE_PERSON)) {
                ok = ok && (routeState.route.basicData?.isOnePersonOperation == true)
            }
            if (filters.contains(RouteFilter.OVER_12_HOURS)) {
                val start = routeState.route.basicData?.timeStartWork ?: 0L
                val end = routeState.route.basicData?.timeEndWork ?: 0L
                ok = ok && (end > start && (end - start) > over12hMillis)
            }
            ok
        }
    }

    // Expose set current month/year: find matching MonthOfYear and save via settingsUseCase
    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                val found =
                    list.find { it.year == yearAndMonth.first && it.month == yearAndMonth.second }
                found?.let { month ->
                    // save in local settings
                    settingsUseCase.setCurrentMonthOfYear(month).collect { result ->
                        // if success — update local monthOfYear and reload routes
                        if (result is ResultState.Success) {
                            // reload routes after change
//                            loadRoutes()
                        }
                    }
                }
            }
        }
    }
}

data class LoadSettingData(
    val userSettings: UserSettings? = null,
    val salarySetting: SalarySetting? = null,
)