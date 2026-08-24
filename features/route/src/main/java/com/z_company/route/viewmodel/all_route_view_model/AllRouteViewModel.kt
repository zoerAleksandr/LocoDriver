package com.z_company.route.viewmodel.all_route_view_model

import android.app.Application
import android.util.Log
import com.z_company.core.sendToSentry
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.core.ResultState
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.core.util.VPN_ERROR_HINT
import com.z_company.core.util.friendlyNetworkErrorMessage
import com.z_company.core.util.isConnectivityErrorMessage
import com.z_company.core.util.isVpnActive
import com.z_company.repository.remote_rest.NetworkErrorMapper
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.calculateWorkTimeWithSettings
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.toMoneyString
import com.z_company.route.viewmodel.computeRouteTotalPayment
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import com.z_company.route.viewmodel.PreviewRouteUiState
import com.z_company.route.viewmodel.RouteActionsHelper
import com.z_company.route.viewmodel.home_view_model.AlertBeforePurchasesEvent
import com.z_company.route.viewmodel.home_view_model.ItemState
import com.z_company.route.viewmodel.home_view_model.OpenRouteFormEvent
import com.z_company.route.viewmodel.home_view_model.StartPurchasesEvent
import com.z_company.use_case.SubscriptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class RouteFilter {
    ALL,
    FAVORITES,
    HEAVY,
    EXTENDED_SERVICE,
    FOLLOWING_RESERVE,
    ONE_PERSON,
    OVER_12_HOURS,
    LONG_TRAINS,
    HAS_BREAK,
    PUSHER,
    DOUBLE_TRACTION,
    DOUBLED_TRAIN
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
    val showConfirmDialogRemoveRoute: Boolean = false,
    val isExpandedView: Boolean = false,
    // Показывать ли объединение «отдых в ПО» (трей с коннектором). По умолчанию вкл.
    val showTurnaroundRest: Boolean = true,
    // Итог оплаты по каждому маршруту (id → отформатированная сумма с валютой),
    // показывается в развёрнутой карточке блоком «Расчёт за смену».
    val routePayments: Map<String, String> = emptyMap(),
    // Отработанное время за месяц (с учётом настройки «Учитывать будущие маршруты»),
    // формат HH:MM — показывается рядом со счётчиком маршрутов.
    val monthWorkedTimeText: String = "",
    /** Фоновая синхронизация при открытии экрана. */
    val isBackgroundSyncing: Boolean = false,
    /** Режим множественного выбора (кнопка «Выбрать» в топбаре). */
    val isSelectionMode: Boolean = false,
    /** id выбранных маршрутов — действия нижней панели применяются к ним. */
    val selectedRouteIds: Set<String> = emptySet()
)

enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    WORKTIME_ASC,
    WORKTIME_DESC
}

class AllRouteViewModel(application: Application) : AndroidViewModel(application), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val subscriptionHelper: SubscriptionHelper by inject()
    private val sharedPreferenceStorage: SharedPreferencesRepositories by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val routesManager: RoutesManager by inject()
    private val shareRouteManager: ShareRouteManager by inject()
    private val syncManager: SyncManager by inject()

    private var removeRouteJob: Job? = null
    private var loadRoutesJob: Job? = null
    private var backgroundSyncJob: Job? = null

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

    /** Тихо подтянуть изменения при каждом открытии списка маршрутов. */
    fun syncOnScreenOpen() {
        if (backgroundSyncJob?.isActive == true) return
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
                e.sendToSentry("AllRouteViewModel", "syncOnScreenOpen")
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

    private val latestRawRoutes = MutableStateFlow<List<ItemState>>(emptyList())

    var offsetInMoscow: Long = 0L
    var timeCalculationContext: TimeCalculationContext = TimeCalculationContext(
        localTZ = kotlinx.datetime.TimeZone.of("GMT+3"),
        crossMonthTZ = kotlinx.datetime.TimeZone.of("GMT+3")
    )
        private set
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

    // Хронологический список доступных месяцев (year, month) — для листания стрелками.
    private val _monthYearList = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val monthYearList: StateFlow<List<Pair<Int, Int>>> = _monthYearList.asStateFlow()

    init {
        // Загружаем сохранённые настройки UI один раз при создании ViewModel
        val savedSort = sharedPreferenceStorage.getSortOption()?.let { SortOption.valueOf(it) }
            ?: SortOption.DATE_DESC
        val savedFiltersStrings = sharedPreferenceStorage.getSelectedFilters() ?: setOf(RouteFilter.ALL.name)
        val savedFilters = savedFiltersStrings.map { RouteFilter.valueOf(it) }.toSet()
        val savedExpanded = sharedPreferenceStorage.isExpandedView()
        val savedShowRest = sharedPreferenceStorage.isShowTurnaroundRest()
        _uiState.update {
            it.copy(
                sortOption = savedSort,
                selectedFilters = savedFilters,
                isExpandedView = savedExpanded,
                showTurnaroundRest = savedShowRest,
            )
        }

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

        // combinedData — поток настроек и salary
        val combinedData: Flow<LoadSettingData> = combine(
            salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }
                .onStart { emit(null) },
            settingsUseCase.getUserSettingFlow().map { it as UserSettings? }
                .onStart { emit(null) },
        ) { us, ss ->
            LoadSettingData(ss, us)
        }

        // Поток 1: реагирует на изменение настроек → загружает маршруты
        viewModelScope.launch {
            combinedData.collectLatest { initData ->
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

                dateAndTimeConverter = DateAndTimeConverter(user)
                offsetInMoscow = user.timeZone
                timeCalculationContext = TimeCalculationContext.from(user)
                minTimeRest = user.minTimeRestPointOfTurnover
                minTimeHomeRest = user.minTimeHomeRest

                // Отменяем предыдущую загрузку, запускаем новую
                loadRoutesJob?.cancel()
                loadRoutesJob = loadRoutes(user)
            }
        }

        // Поток 2: реагирует на latestRawRoutes и фильтры → применяет фильтрацию (без loadRoutes!)
        viewModelScope.launch {
            combine(
                latestRawRoutes,
                _uiState.map { it.selectedFilters }.distinctUntilChanged()
            ) { rawRoutes, filters ->
                rawRoutes to filters
            }.collectLatest { (rawRoutes, filters) ->
                val salary = salarySetting ?: return@collectLatest
                val user = userSettings ?: return@collectLatest

                val filtered = applyFilters(rawRoutes, filters, salarySetting = salary)
                // Отработанное за месяц по всем маршрутам (не по фильтру), с учётом
                // настройки «Учитывать будущие маршруты».
                val monthWorked = rawRoutes.map { it.route }.calculateWorkTimeWithSettings(
                    monthOfYear = user.selectMonthOfYear,
                    userSettings = user,
                    currentTimeInMillis = System.currentTimeMillis(),
                )
                _uiState.update {
                    it.copy(
                        filteredRoutes = filtered,
                        isLoading = false,
                        currentMonthOfYear = user.selectMonthOfYear,
                        monthWorkedTimeText = convertTimeToStringFormat(monthWorked)
                    )
                }
            }
        }

        // Поток 3: считает итог оплаты по каждому маршруту месяца для блока
        // «Расчёт за смену» в развёрнутой карточке. Реагирует на список маршрутов
        // и на настройки (тариф/зарплата). collectLatest отменяет незавершённый
        // пересчёт при новой эмиссии. Фильтры на суммы не влияют, поэтому считаем
        // по «сырым» маршрутам месяца.
        viewModelScope.launch(Dispatchers.Default) {
            combine(latestRawRoutes, combinedData) { raw, settings -> raw to settings }
                .collectLatest { (raw, settings) ->
                    val user = settings.userSettings ?: return@collectLatest
                    val salary = settings.salarySetting ?: return@collectLatest
                    // Сортировка по началу работы нужна для доплаты за переотдых.
                    val sortedRoutes = raw.map { it.route }
                        .sortedBy { it.basicData.timeStartWork ?: Long.MAX_VALUE }
                    val currency = currencySymbol(user.country)
                    val payments = LinkedHashMap<String, String>()
                    sortedRoutes.forEach { route ->
                        val total = try {
                            computeRouteTotalPayment(route, user, salary, sortedRoutes)
                        } catch (e: Exception) {
                            e.sendToSentry("AllRouteViewModel", "recomputePayments")
                            null
                        }
                        if (total != null) {
                            payments[route.basicData.id] = total.toMoneyString(currency)
                        }
                    }
                    _uiState.update { it.copy(routePayments = payments) }
                }
        }
    }

    // Эмитит ShareLinkData (text + subject), UI собирает Intent через ShareLinkData.toShareIntent()
    private val _shareRouteEvent =
        MutableSharedFlow<com.z_company.route.util.ShareLinkData>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    val shareRouteEvent: SharedFlow<com.z_company.route.util.ShareLinkData> =
        _shareRouteEvent.asSharedFlow()

    /**
     * Создаёт публичную ссылку на маршрут и эмитит ShareLinkData в [shareRouteEvent].
     * UI-слой (AllRouteScreen) подписывается, строит Intent и запускает share-sheet.
     */
    fun shareRoute(route: Route) {
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
                            _shareRouteEvent.emit(
                                com.z_company.route.util.ShareLinkData.fromRoute(route, result.data)
                            )
                        }
                        is ResultState.Error -> {
                            val raw = result.entity.message ?: result.entity.throwable?.message
                            val message = if (isConnectivityErrorMessage(raw) &&
                                isVpnActive(getApplication())
                            ) {
                                VPN_ERROR_HINT
                            } else {
                                friendlyNetworkErrorMessage(raw, "Не удалось создать ссылку")
                            }
                            snackbarManager.show(message)
                        }
                        is ResultState.Loading -> Unit
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("AllRouteViewModel", "shareRoute")
                Log.e("ShareRoute", "Ошибка шаринга: ${e.message}")
                val message = if (isVpnActive(getApplication())) VPN_ERROR_HINT
                else friendlyNetworkErrorMessage(e.message, "Не удалось создать ссылку")
                snackbarManager.show(message)
            }
        }
    }

    fun convertTimeToStringFormat(timeToLong: Long?): String {
        userSettings?.let { settings ->
            return if (settings.isDecimalTime) {
                ConverterLongToTime.getTimeInStringDecimalFormat(timeToLong)
            } else {
                ConverterLongToTime.getTimeInStringFormat(timeToLong)
            }
        }
        return ConverterLongToTime.getTimeInStringFormat(timeToLong)
    }

    fun restorePurchases() {
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            subscriptionHelper.restorePurchases(snackbarManager, token)
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
            // Сначала авторизация: без токена сразу подсказываем войти в аккаунт
            // и не идём в сеть.
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
            val label = SyncManager.routeLabel(route)
            routesManager.saveRouteInRemote(route, "Bearer $token").collect { resultState ->
                when (resultState) {
                    is ResultState.Success -> {
                        val warnings = resultState.data.warnings
                        routeUseCase.setSynchronizedRoute(route.basicData.id).first()
                        if (warnings.isNotEmpty()) {
                            val warningText = warnings.joinToString("\n")
                            snackbarManager.show(
                                message = "$label сохранен с предупреждениями:\n$warningText",
                                duration = androidx.compose.material3.SnackbarDuration.Long
                            )
                        } else {
                            snackbarManager.show(message = "Маршрут сохранен в облаке")
                        }
                        _uiState.update { it.copy(syncRouteState = null) }
                    }

                    is ResultState.Error -> {
                        val errorMsg = resultState.entity.message
                            ?: resultState.entity.throwable?.message
                            ?: "Ошибка синхронизации"
                        snackbarManager.show(
                            message = "$label: $errorMsg",
                            duration = androidx.compose.material3.SnackbarDuration.Long
                        )
                        _uiState.update { it.copy(syncRouteState = ResultState.Error(resultState.entity)) }
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
            // .collect (а не .first) — иначе берётся первый эмит Loading и homeRest
            // никогда не обновляется терминальным Success (как в HomeViewModel).
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

    /** Фактический отдых до следующей явки — для быстрого просмотра. */
    fun calculationActualRest(route: Route?) {
        // Сбрасываем прежнее значение, чтобы не показать чужой отдых до пересчёта.
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

    fun loadRoutes(userSettings: UserSettings): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            routeUseCase.listRoutesByMonth(userSettings.selectMonthOfYear, timeCalculationContext)
                .collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            // Полноэкранный спиннер — только для самой первой загрузки.
                            // Если список уже отрисован (например, идёт фоновая
                            // синхронизация и combinedData перезапустил loadRoutes из-за
                            // обновления настроек), не подменяем его спиннером — иначе
                            // экран «моргает» на каждое такое переоткрытие потока.
                            _uiState.update {
                                it.copy(
                                    isLoading = it.routes.isEmpty(),
                                    errorMessage = null
                                )
                            }
                        }

                        is ResultState.Success -> {
                            if (salarySetting != null && dateAndTimeConverter != null) {
                                val routeList = result.data

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
                                        ),
                                        isLongCompositionTrain = isLongCompositionTrain(salarySetting!!, route),
                                        isFuture = route.isFuture(offsetInMoscow),
                                        isTransition = route.isTransition(offsetInMoscow)
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
                            snackbarManager.show(message = message)
                        }
                    }
                }
        }
    }

    fun setSort(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        sharedPreferenceStorage.setSortOption(option.name)
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
        val newFiltersStrings = _uiState.value.selectedFilters.map { it.name }.toSet()
        sharedPreferenceStorage.setSelectedFilters(newFiltersStrings)
    }

    fun toggleExpandedView() {
        _uiState.update { it.copy(isExpandedView = !it.isExpandedView) }
        sharedPreferenceStorage.setIsExpandedView(_uiState.value.isExpandedView)
    }

    /**
     * Переключает показ объединения «отдых в ПО» и сохраняет выбор в prefs.
     * @return новое значение (true — объединение показывается).
     */
    fun toggleShowTurnaroundRest(): Boolean {
        val newValue = !_uiState.value.showTurnaroundRest
        _uiState.update { it.copy(showTurnaroundRest = newValue) }
        sharedPreferenceStorage.setShowTurnaroundRest(newValue)
        return newValue
    }

    // ─────────────────────────────────────────────────────────────
    // Режим множественного выбора маршрутов («Выбрать» в топбаре).
    // Действия нижней панели применяются к selectedRouteIds.
    // ─────────────────────────────────────────────────────────────

    fun enterSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = true, selectedRouteIds = emptySet()) }
    }

    fun exitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedRouteIds = emptySet()) }
    }

    fun toggleRouteSelection(routeId: String) {
        _uiState.update { state ->
            val selected = state.selectedRouteIds
            state.copy(
                selectedRouteIds = if (selected.contains(routeId)) selected.minus(routeId)
                else selected.plus(routeId)
            )
        }
    }

    /** Выделить все видимые (после фильтров) маршруты или снять выделение со всех. */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val visibleIds = state.filteredRoutes.map { it.route.basicData.id }.toSet()
            state.copy(
                selectedRouteIds = if (state.selectedRouteIds.containsAll(visibleIds) &&
                    visibleIds.isNotEmpty()
                ) emptySet() else visibleIds
            )
        }
    }

    /** Маршруты по текущему выделению (в порядке загруженного списка). */
    private fun selectedRoutes(): List<Route> {
        val ids = _uiState.value.selectedRouteIds
        return _uiState.value.routes
            .filter { ids.contains(it.route.basicData.id) }
            .map { it.route }
    }

    /** Массовое удаление (soft-delete, как одиночное — маршрут уходит в isDeleted). */
    fun deleteSelectedRoutes() {
        val routes = selectedRoutes()
        if (routes.isEmpty()) return
        removeRouteJob?.cancel()
        removeRouteJob = viewModelScope.launch {
            _uiState.update { it.copy(removeRouteState = ResultState.Loading()) }
            var deleted = 0
            var failed = 0
            routes.forEach { route ->
                when (routeUseCase.markAsRemoved(route).first { it !is ResultState.Loading }) {
                    is ResultState.Success -> deleted++
                    else -> failed++
                }
            }
            _uiState.update { it.copy(removeRouteState = null) }
            snackbarManager.show(
                message = when {
                    failed == 0 && deleted == 1 -> "Маршрут удалён"
                    failed == 0 -> "Удалено маршрутов: $deleted"
                    deleted == 0 -> "Не удалось удалить маршруты"
                    else -> "Удалено: $deleted, не удалось: $failed"
                }
            )
            exitSelectionMode()
        }
    }

    /**
     * Массовое избранное. Если среди выбранных есть хотя бы один не в избранном —
     * добавляем все; если все уже в избранном — снимаем со всех.
     */
    fun toggleFavoriteSelectedRoutes() {
        val routes = selectedRoutes()
        if (routes.isEmpty()) return
        val newValue = routes.any { !it.basicData.isFavorite }
        viewModelScope.launch {
            var changed = 0
            routes.forEach { route ->
                if (route.basicData.isFavorite != newValue) {
                    val result = routeUseCase
                        .setFavoriteRoute(route.basicData.id, newValue)
                        .first { it !is ResultState.Loading }
                    if (result is ResultState.Success) changed++
                }
            }
            snackbarManager.show(
                message = when {
                    changed == 0 -> "Ничего не изменилось"
                    newValue && changed == 1 -> "Маршрут добавлен в избранное"
                    newValue -> "Добавлено в избранное: $changed"
                    changed == 1 -> "Маршрут удален из избранного"
                    else -> "Убрано из избранного: $changed"
                }
            )
            exitSelectionMode()
        }
    }

    /**
     * Массовый шеринг: для каждого выбранного маршрута создаём публичную ссылку и
     * отправляем одним сообщением (см. [com.z_company.route.util.ShareLinkData.fromRoutes]).
     */
    fun shareSelectedRoutes() {
        val routes = selectedRoutes()
        if (routes.isEmpty()) return
        viewModelScope.launch {
            try {
                val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (rawToken.isNullOrBlank()) {
                    snackbarManager.show("Неавторизованный пользователь")
                    return@launch
                }
                val bearerToken = "Bearer $rawToken"
                val links = mutableListOf<Pair<Route, String>>()
                var errorMessage: String? = null
                routes.forEach { route ->
                    val result = shareRouteManager.createShareLink(route, bearerToken)
                        .first { it !is ResultState.Loading }
                    when (result) {
                        is ResultState.Success -> links.add(route to result.data)
                        is ResultState.Error -> {
                            val raw = result.entity.message ?: result.entity.throwable?.message
                            errorMessage = if (isConnectivityErrorMessage(raw) &&
                                isVpnActive(getApplication())
                            ) {
                                VPN_ERROR_HINT
                            } else {
                                friendlyNetworkErrorMessage(raw, "Не удалось создать ссылку")
                            }
                        }

                        else -> Unit
                    }
                }
                if (links.isEmpty()) {
                    snackbarManager.show(errorMessage ?: "Не удалось создать ссылку")
                    return@launch
                }
                _shareRouteEvent.emit(
                    com.z_company.route.util.ShareLinkData.fromRoutes(links)
                )
                if (links.size < routes.size) {
                    snackbarManager.show("Ссылки созданы не для всех маршрутов")
                }
                exitSelectionMode()
            } catch (e: Exception) {
                e.sendToSentry("AllRouteViewModel", "shareSelectedRoutes")
                val message = if (isVpnActive(getApplication())) VPN_ERROR_HINT
                else friendlyNetworkErrorMessage(e.message, "Не удалось создать ссылку")
                snackbarManager.show(message)
            }
        }
    }

    fun reload() {
        userSettings?.let { setting ->
            loadRoutesJob?.cancel()
            loadRoutesJob = loadRoutes(setting)
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
            if (filters.contains(RouteFilter.HAS_BREAK)) {
                ok = ok && (routeState.route.getBreakDuration() > 0L)
            }
            if (filters.contains(RouteFilter.PUSHER)) {
                ok = ok && routeState.route.trains.any { it.pusher != null }
            }
            if (filters.contains(RouteFilter.DOUBLE_TRACTION)) {
                ok = ok && routeState.route.trains.any { it.doubleTraction != null }
            }
            if (filters.contains(RouteFilter.DOUBLED_TRAIN)) {
                ok = ok && routeState.route.trains.any { it.doubledTrain != null }
            }
            ok
        }
    }

    // Expose set current month/year: find matching MonthOfYear and save via settingsUseCase
    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        // Выделение относится к маршрутам текущего месяца — при смене месяца
        // сбрасываем режим выбора, чтобы действия не ушли по «невидимым» id.
        if (_uiState.value.isSelectionMode) exitSelectionMode()
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
