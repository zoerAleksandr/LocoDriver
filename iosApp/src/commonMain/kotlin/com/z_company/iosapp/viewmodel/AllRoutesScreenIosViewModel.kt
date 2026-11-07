@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.calculateWorkTimeWithSettings
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getLongDistanceTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.entities.route.UtilsForEntities.timeFollowingSingleLocomotive
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.track.TrackRepository
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.displayTimeZone
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.NetworkErrorMapper
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * iOS-ViewModel экрана «Все маршруты» — SCREEN_SPECS §9 (порт Android
 * `AllRouteViewModel` + `RouteActionsHelper` + `PullToSyncViewModel.refresh`).
 *
 * Логика (фильтры/сортировка/оплата/трои «отдых в ПО»/множественный выбор)
 * живёт здесь; Swift подписывается через `watch*`-колбэки (AGENTS.md).
 */
class AllRoutesScreenIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
    private val routesManager: RoutesManager,
    private val shareRouteManager: ShareRouteManager,
    private val trackRepository: TrackRepository? = null,
) : ViewModel() {

    /** Флаги доплат карточки (аналог Android `ItemState`). */
    private data class ItemFlags(
        val route: Route,
        val isHoliday: Boolean = false,
        val isHeavyTrains: Boolean = false,
        val isExtendedServicePhaseTrains: Boolean = false,
        val isLongCompositionTrain: Boolean = false,
        val isFuture: Boolean = false,
        val isTransition: Boolean = false,
    )

    // ── Внутренние данные ───────────────────────────────────────────────────
    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null
    private var monthYearList: List<Pair<Int, Int>> = emptyList()
    private var monthList: List<Int> = emptyList()
    private var yearList: List<Int> = emptyList()
    private val latestRawRoutes = MutableStateFlow<List<ItemFlags>>(emptyList())
    /** Тик для пересчёта оплаты при изменении настроек. */
    private val settingsTick = MutableStateFlow(0L)
    private var loadRoutesJob: Job? = null
    private var backgroundSyncJob: Job? = null
    private var removeRouteJob: Job? = null

    // ── Выходы для Swift ────────────────────────────────────────────────────
    private val _ui = MutableStateFlow(AllRoutesUi())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    private val _share = MutableSharedFlow<SharePayloadUi>(extraBufferCapacity = 4)

    init {
        // Сохранённые настройки UI — один раз при создании (§9.0.1).
        val savedSort = sharedPrefs.getSortOption()?.let { name ->
            runCatching { SortOption.valueOf(name) }.getOrNull()
        } ?: SortOption.DATE_DESC
        val savedFilters = (sharedPrefs.getSelectedFilters() ?: setOf(RouteFilter.ALL.name))
            .mapNotNull { name -> runCatching { RouteFilter.valueOf(name) }.getOrNull() }
            .toSet()
            .ifEmpty { setOf(RouteFilter.ALL) }
        _ui.value = _ui.value.copy(
            sortOption = savedSort,
            selectedFilters = savedFilters.map { it.name },
            isExpandedView = sharedPrefs.isExpandedView(),
            showTurnaroundRest = sharedPrefs.isShowTurnaroundRest(),
        )

        observeMonthList()
        observeSettings()
        observeFiltered()
        observePayments()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Загрузка
    // ═══════════════════════════════════════════════════════════════════════

    private fun observeMonthList() {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                monthList = list.map { it.month }.distinct().sorted()
                yearList = list.map { it.year }.distinct().sorted()
                monthYearList = list
                    .map { it.year to it.month }
                    .distinct()
                    .sortedWith(compareBy({ it.first }, { it.second }))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }
                    .onStart { emit(null) },
                settingsUseCase.getUserSettingFlow().map { it as UserSettings? }
                    .onStart { emit(null) },
            ) { ss, us -> ss to us }
                .collectLatest { (ss, us) ->
                    salarySetting = ss
                    userSettings = us
                    settingsTick.value = settingsTick.value + 1L
                    if (us == null || ss == null) {
                        _ui.value = _ui.value.copy(isLoading = true)
                        return@collectLatest
                    }
                    loadRoutesJob?.cancel()
                    loadRoutesJob = loadRoutes(us)
                    publishMonthHeader(us)
                }
        }
    }

    private fun observeFiltered() {
        viewModelScope.launch {
            combine(
                latestRawRoutes,
                _ui.map { it.selectedFilters }.distinctUntilChanged(),
            ) { raw, filters -> raw to filters }
                .collectLatest { (raw, filters) ->
                    val us = userSettings ?: return@collectLatest
                    val ss = salarySetting ?: return@collectLatest
                    val filterSet = filters.mapNotNull { name ->
                        runCatching { RouteFilter.valueOf(name) }.getOrNull()
                    }.toSet()
                    val filtered = applyFilters(raw, filterSet, ss)
                    // Отработанное за месяц — по ВСЕМ маршрутам месяца (не по фильтру),
                    // с учётом «Учитывать будущие маршруты» (§9.0.1).
                    val monthWorked = raw.map { it.route }.calculateWorkTimeWithSettings(
                        monthOfYear = us.selectMonthOfYear,
                        userSettings = us,
                        currentTimeInMillis = nowMs(),
                    )
                    rebuild(filtered, monthWorked)
                }
        }
    }

    /** «Расчёт за смену» по каждому маршруту месяца (§9.0.1); фильтры не влияют. */
    private fun observePayments() {
        viewModelScope.launch(Dispatchers.Default) {
            // Пересчёт при изменении маршрутов и настроек (тариф/зарплата).
            combine(latestRawRoutes, settingsTick) { raw, _ -> raw }
                .collectLatest { raw ->
                    val us = userSettings ?: return@collectLatest
                    val ss = salarySetting ?: return@collectLatest
                    val monthRoutes = raw.map { it.route }
                    // Для переотдыха первому маршруту месяца нужен последний
                    // маршрут предыдущего (отдых в ПО на стыке месяцев).
                    val candidates = monthRoutes + routeUseCase.adjacentRoutesOfMonthFlow(
                        us.selectMonthOfYear, TimeCalculationContext.from(us)
                    ).first()
                    val currency = currencySymbol(us.country)
                    val payments = LinkedHashMap<String, String>()
                    monthRoutes.forEach { route ->
                        // Ошибка расчёта маршрута — суммы просто нет (§9.0.1).
                        val total = try {
                            computeRouteTotalPayment(route, us, ss, candidates)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            null
                        }
                        if (total != null) {
                            payments[route.basicData.id] = paymentText(total, currency)
                        }
                    }
                    rebuild(payments = payments)
                }
        }
    }

    private fun loadRoutes(us: UserSettings): Job =
        viewModelScope.launch(Dispatchers.Default) {
            routeUseCase.listRoutesByMonth(us.selectMonthOfYear, TimeCalculationContext.from(us))
                .collect { result ->
                    when (result) {
                        is ResultState.Loading -> {
                            // Спиннер — только самая первая загрузка (§9.0.1).
                            _ui.value = _ui.value.copy(
                                isLoading = _ui.value.rows.isEmpty(),
                                errorMessage = null,
                            )
                        }

                        is ResultState.Success -> {
                            val ss = salarySetting ?: return@collect
                            val list = result.data.map { route ->
                                ItemFlags(
                                    route = route,
                                    isHoliday = isHolidayTimeInRoute(
                                        us.selectMonthOfYear, us, route
                                    ),
                                    isHeavyTrains = isHeavyTrains(ss, route, us.servicePhases),
                                    isExtendedServicePhaseTrains = isExtendedServicePhaseTrains(ss, route),
                                    isLongCompositionTrain = isLongCompositionTrain(
                                        ss, route, us.servicePhases
                                    ),
                                    // BUG-16 (паритет с Android): isFuture прибавляет к now
                                    // смещение пояса пользователя от Москвы.
                                    isFuture = route.isFuture(us.timeZone),
                                    isTransition = route.isTransition(us.timeZone),
                                )
                            }
                            latestRawRoutes.value = list
                            _ui.value = _ui.value.copy(isLoading = false, errorMessage = null)
                        }

                        is ResultState.Error -> {
                            val message = NetworkErrorMapper.syncFailureMessage(
                                result.entity.message, result.entity.throwable
                            ).ifBlank { "Ошибка загрузки маршрутов" }
                            _ui.value = _ui.value.copy(
                                isLoading = false,
                                errorMessage = message,
                            )
                            _messages.tryEmit(message)
                        }
                    }
                }
        }

    // ═══════════════════════════════════════════════════════════════════════
    // Сборка UI
    // ═══════════════════════════════════════════════════════════════════════

    private var lastPayments: Map<String, String> = emptyMap()

    private fun rebuild(
        filtered: List<ItemFlags>? = null,
        monthWorked: Long? = null,
        payments: Map<String, String>? = null,
    ) {
        payments?.let { lastPayments = it }
        val us = userSettings ?: return
        val cur = _ui.value
        val sorted = (filtered ?: currentFiltered()).let { sortRoutes(it, cur.sortOption) }
        val rows = buildRows(sorted, lastPayments, us)
        val selectedIds = selectedRouteIds.toSet()
        _ui.value = cur.copy(
            isReady = true,
            isLoading = false,
            rows = rows,
            routesCount = latestRawRoutes.value.size,
            monthWorkedTimeText = monthWorked?.let {
                Fmt.duration(it, us.isDecimalTime)
            } ?: cur.monthWorkedTimeText,
            selectedCount = selectedIds.size,
            allVisibleSelected = sorted.isNotEmpty() &&
                sorted.all { selectedIds.contains(it.route.basicData.id) },
            selectedAllFavorite = selectedIds.isNotEmpty() &&
                latestRawRoutes.value
                    .filter { selectedIds.contains(it.route.basicData.id) }
                    .all { it.route.basicData.isFavorite },
        )
        publishMonthHeader(us)
    }

    private fun currentFiltered(): List<ItemFlags> {
        val ss = salarySetting ?: return emptyList()
        val set = _ui.value.selectedFilters.mapNotNull { name ->
            runCatching { RouteFilter.valueOf(name) }.getOrNull()
        }.toSet()
        return applyFilters(latestRawRoutes.value, set, ss)
    }

    private fun publishMonthHeader(us: UserSettings) {
        val month = us.selectMonthOfYear
        val index = monthYearList.indexOfFirst { it.first == month.year && it.second == month.month }
        _ui.value = _ui.value.copy(
            monthTitle = MONTH_NAMES.getOrElse(month.month) { "" },
            yearTitle = month.year.toString(),
            hasPrevMonth = index > 0,
            hasNextMonth = index in 0 until monthYearList.lastIndex,
            monthOptions = monthList,
            yearOptions = yearList,
        )
    }

    private fun sortRoutes(routes: List<ItemFlags>, option: SortOption): List<ItemFlags> =
        when (option) {
            // Без явки — в конце списка (§9.0.1).
            SortOption.DATE_ASC -> routes.sortedBy {
                it.route.basicData.timeStartWork ?: Long.MAX_VALUE
            }

            SortOption.DATE_DESC -> routes.sortedByDescending {
                it.route.basicData.timeStartWork ?: Long.MIN_VALUE
            }

            SortOption.WORKTIME_ASC -> routes.sortedBy {
                it.route.getWorkTime() ?: 0L
            }

            SortOption.WORKTIME_DESC -> routes.sortedByDescending {
                it.route.getWorkTime() ?: 0L
            }
        }

    /** Плоский список строк с треями «поездка с отдыхом в ПО» (§9.0.1). */
    private fun buildRows(
        sorted: List<ItemFlags>,
        payments: Map<String, String>,
        us: UserSettings,
    ): List<AllRouteRowUi> {
        val isDecimal = us.isDecimalTime
        val tz = displayTz(us)
        val month = us.selectMonthOfYear
        val ctx = TimeCalculationContext.from(us)
        val showTurnaround = _ui.value.showTurnaroundRest
        val isExpanded = _ui.value.isExpandedView
        val result = mutableListOf<AllRouteRowUi>()

        // Коннекторы между соседями: null — пара не связана.
        val links: List<TurnaroundRestLink?> = if (showTurnaround && sorted.size > 1) {
            (0 until sorted.size - 1).map { i ->
                turnaroundRestBetween(sorted[i].route, sorted[i + 1].route)
            }
        } else {
            emptyList()
        }

        var groupIndex = -1
        var i = 0
        while (i < sorted.size) {
            // Начало трея: связка с СЛЕДУЮЩИМ (i,i+1) или с ПРЕДЫДУЩИМ (i-1,i).
            val startsGroup = showTurnaround && (
                links.getOrNull(i) != null || (i > 0 && links.getOrNull(i - 1) != null)
                )
            if (startsGroup && (i == 0 || links.getOrNull(i - 1) == null)) {
                groupIndex++
                // Длина трея: идём по цепочке связок.
                var end = i
                while (links.getOrNull(end) != null) end++
                val linkCount = end - i
                val workSum = (i..end).sumOf { idx ->
                    runCatching {
                        sorted[idx].route.getWorkTimeInMonth(month, ctx)
                    }.getOrNull() ?: 0L
                }
                val title = if (linkCount == 1) {
                    "Поездка с отдыхом · ${Fmt.duration(workSum, isDecimal)}"
                } else {
                    "Поездка с $linkCount отдыхами · ${Fmt.duration(workSum, isDecimal)}"
                }
                result += AllRouteRowUi(
                    kind = "tripHeader",
                    title = title,
                    groupIndex = groupIndex,
                )
            }
            val rowGroup = if (startsGroup) groupIndex else -1
            result += AllRouteRowUi(
                kind = "route",
                card = buildCard(sorted[i], i, sorted.size, payments, us, tz, month, ctx),
                groupIndex = rowGroup,
            )
            val link = links.getOrNull(i)
            if (link != null) {
                val stationPart = if (link.station.isNotBlank()) " · ${link.station}" else ""
                result += AllRouteRowUi(
                    kind = "tripConnector",
                    title = "Отдых в ПО$stationPart",
                    connectorStation = link.station,
                    connectorDurationText = Fmt.duration(link.durationMs, isDecimal),
                    groupIndex = groupIndex,
                )
            }
            i++
        }
        return result
    }

    private fun buildCard(
        flags: ItemFlags,
        index: Int,
        size: Int,
        payments: Map<String, String>,
        us: UserSettings,
        tz: TimeZone,
        month: MonthOfYear,
        ctx: TimeCalculationContext,
    ): AllRouteCardUi {
        val route = flags.route
        val start = route.basicData.timeStartWork
        val end = route.basicData.timeEndWork
        val isDifferenceDate = start != null && end != null &&
            Fmt.date(start, tz) != Fmt.date(end, tz)
        val endText = when {
            end == null -> ""
            isDifferenceDate || start == null -> Fmt.dateMiniTime(end, tz)
            else -> Fmt.time(end, tz)
        }
        val workInMonth = runCatching { route.getWorkTimeInMonth(month, ctx) }.getOrNull()
        val icons = buildList {
            if (flags.isHoliday) add("holiday")
            if (route.getBreakDuration() > 0L) add("break")
            if (flags.isLongCompositionTrain) add("length")
            if (flags.isHeavyTrains) add("weight")
            if (flags.isExtendedServicePhaseTrains) add("shoulder")
            if (route.basicData.isOnePersonOperation) add("solo")
            if ((route.getPassengerTime() ?: 0L) > 0L) add("passenger")
            val workTime = route.getWorkTime()
            if (workTime != null && workTime > 12 * Fmt.HOUR) add("over12")
            if (route.trains.any { it.pusher != null }) add("pusher")
            if (route.trains.any { it.doubleTraction != null }) add("double")
            if (route.trains.any { it.doubledTrain != null }) add("coupled")
        }
        return AllRouteCardUi(
            basicId = route.basicData.id,
            numberText = "#${size - index}",
            routeNumberText = route.basicData.number?.takeIf { it.isNotBlank() }
                ?.let { "№$it" } ?: "№б/н",
            startText = start?.let { Fmt.dateMiniTime(it, tz) } ?: "",
            endText = endText,
            endTextWithDate = end?.let { Fmt.dateMiniTime(it, tz) } ?: "",
            durationText = Fmt.duration(workInMonth, us.isDecimalTime),
            summary = routeSummary(route),
            isFuture = flags.isFuture,
            isTransition = flags.isTransition,
            icons = icons,
            isSynchronized = route.basicData.isSynchronized,
            isFavorite = route.basicData.isFavorite,
            paymentText = payments[route.basicData.id] ?: "",
            locoLines = route.locomotives.map { loco ->
                listOfNotNull(
                    loco.series?.takeIf { it.isNotBlank() },
                    loco.number?.takeIf { it.isNotBlank() }?.let { "№$it" },
                ).joinToString(" ")
            },
            trainLines = sortedTrains(route).map { train ->
                val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
                val first = train.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
                val last = if (train.stations.size > 1) {
                    train.stations.last().stationName?.takeIf { it.isNotBlank() }
                } else null
                val path = when {
                    first != null && last != null -> "$first — $last"
                    else -> first ?: last ?: ""
                }
                "$tn$path".trim()
            },
            passengerLines = route.passengers.map { p ->
                val tn = if (!p.trainNumber.isNullOrBlank()) "№${p.trainNumber} " else ""
                val from = p.stationDeparture?.takeIf { it.isNotBlank() }
                val to = p.stationArrival?.takeIf { it.isNotBlank() }
                val path = when {
                    from != null && to != null -> "$from — $to"
                    else -> from ?: to ?: ""
                }
                "$tn$path".trim()
            },
            otherWorkLines = route.otherWorks.map { w ->
                val type = w.workType?.takeIf { it.isNotBlank() }
                val station = w.station?.takeIf { it.isNotBlank() }
                when {
                    type != null && station != null -> "$type — $station"
                    else -> type ?: station ?: ""
                }
            },
            notesText = route.basicData.notes.orEmpty(),
        )
    }

    /** Поезда в порядке §9.1: по времени отправления первой станции, без времени — в конце. */
    private fun sortedTrains(route: Route): List<com.z_company.domain.entities.route.Train> =
        route.trains.sortedWith(
            compareBy(nullsLast<Long>()) { it.stations.firstOrNull()?.timeDeparture }
        )

    /** Свёрнутая строка карточки (§9.0.1): первый поезд, иначе первая прочая работа. */
    private fun routeSummary(route: Route): String {
        val train = sortedTrains(route).firstOrNull()
        if (train != null) {
            val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
            val first = train.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = if (train.stations.size > 1) {
                train.stations.last().stationName?.takeIf { it.isNotBlank() }
            } else null
            val path = when {
                first != null && last != null -> "$first — $last"
                else -> first ?: last ?: ""
            }
            return "$tn$path".trim()
        }
        val otherWork = route.otherWorks.firstOrNull() ?: return ""
        val type = otherWork.workType?.takeIf { it.isNotBlank() }
        val station = otherWork.station?.takeIf { it.isNotBlank() }
        return when {
            type != null && station != null -> "$type — $station"
            else -> type ?: station ?: ""
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Фильтры (§9.0.1) — все выбранные объединяются через И.
    // ═══════════════════════════════════════════════════════════════════════

    private fun applyFilters(
        routesState: List<ItemFlags>,
        filters: Set<RouteFilter>,
        salarySetting: SalarySetting,
    ): List<ItemFlags> {
        if (filters.contains(RouteFilter.ALL)) return routesState

        val over12hMillis = 43_200_000L

        return routesState.filter { state ->
            var ok = true
            if (filters.contains(RouteFilter.FAVORITES)) {
                ok = ok && state.route.basicData.isFavorite
            }
            if (filters.contains(RouteFilter.HEAVY)) {
                ok = ok && runCatching {
                    isHeavyTrains(salarySetting, state.route)
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.EXTENDED_SERVICE)) {
                ok = ok && runCatching {
                    isExtendedServicePhaseTrains(salarySetting, state.route)
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.LONG_TRAINS)) {
                // BUG-31 (паритет с Android): фильтр «Длинные поезда» — это НЕ то же
                // самое, что isLongCompositionTrain: сюда попадает любой поезд с
                // целой частью У.Д. > 0 (getLongDistanceTime(0)).
                ok = ok && runCatching {
                    state.route.getLongDistanceTime(0) > 0L
                }.getOrDefault(false)
            }
            if (filters.contains(RouteFilter.FOLLOWING_RESERVE)) {
                val has = state.route.trains.any { train ->
                    runCatching {
                        train.timeFollowingSingleLocomotive(
                            state.route.basicData.timeStartWork,
                            state.route.basicData.timeEndWork,
                        )
                    }.getOrDefault(0L) > 0L
                }
                ok = ok && has
            }
            if (filters.contains(RouteFilter.ONE_PERSON)) {
                ok = ok && state.route.basicData.isOnePersonOperation
            }
            if (filters.contains(RouteFilter.OVER_12_HOURS)) {
                val start = state.route.basicData.timeStartWork ?: 0L
                val end = state.route.basicData.timeEndWork ?: 0L
                ok = ok && (end > start && (end - start) > over12hMillis)
            }
            if (filters.contains(RouteFilter.HAS_BREAK)) {
                ok = ok && (state.route.getBreakDuration() > 0L)
            }
            if (filters.contains(RouteFilter.PUSHER)) {
                ok = ok && state.route.trains.any { it.pusher != null }
            }
            if (filters.contains(RouteFilter.DOUBLE_TRACTION)) {
                ok = ok && state.route.trains.any { it.doubleTraction != null }
            }
            if (filters.contains(RouteFilter.DOUBLED_TRAIN)) {
                ok = ok && state.route.trains.any { it.doubledTrain != null }
            }
            ok
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Публичное состояние (watch* для Swift)
    // ═══════════════════════════════════════════════════════════════════════

    fun watchUi(callback: (AllRoutesUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        viewModelScope.launch { _messages.collect { callback(it) } }
    }

    /** Полезная нагрузка share-sheet (text + subject). */
    fun watchShare(callback: (SharePayloadUi) -> Unit) {
        viewModelScope.launch { _share.collect { callback(it) } }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Месяц
    // ═══════════════════════════════════════════════════════════════════════

    fun setCurrentMonth(year: Int, month: Int) {
        val found = monthYearList.find { it.first == year && it.second == month } ?: return
        applyMonth(found.first, found.second)
    }

    fun prevMonth() {
        val us = userSettings ?: return
        val cur = us.selectMonthOfYear
        val index = monthYearList.indexOfFirst { it.first == cur.year && it.second == cur.month }
        val target = monthYearList.getOrNull(index - 1) ?: return
        applyMonth(target.first, target.second)
    }

    fun nextMonth() {
        val us = userSettings ?: return
        val cur = us.selectMonthOfYear
        val index = monthYearList.indexOfFirst { it.first == cur.year && it.second == cur.month }
        val target = monthYearList.getOrNull(index + 1) ?: return
        applyMonth(target.first, target.second)
    }

    private fun applyMonth(year: Int, month: Int) {
        // Выделение относится к маршрутам текущего месяца — сбрасываем режим
        // выбора (§9.2).
        exitSelectionMode()
        viewModelScope.launch {
            val list = calendarUseCase.loadFlowMonthOfYearListState().first()
            val found = list.find { it.year == year && it.month == month } ?: return@launch
            settingsUseCase.setCurrentMonthOfYear(found).collect { }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Фильтры / сортировка / вид
    // ═══════════════════════════════════════════════════════════════════════

    fun toggleFilter(name: String) {
        val filter = runCatching { RouteFilter.valueOf(name) }.getOrNull() ?: return
        val current = _ui.value.selectedFilters.mapNotNull {
            runCatching { RouteFilter.valueOf(it) }.getOrNull()
        }.toMutableSet()
        if (filter == RouteFilter.ALL) {
            current.clear()
            current.add(RouteFilter.ALL)
        } else {
            current.remove(RouteFilter.ALL)
            if (current.contains(filter)) current.remove(filter) else current.add(filter)
            if (current.isEmpty()) current.add(RouteFilter.ALL)
        }
        _ui.value = _ui.value.copy(selectedFilters = current.map { it.name })
        sharedPrefs.setSelectedFilters(current.map { it.name }.toSet())
        rebuild()
    }

    fun setSort(name: String) {
        val option = runCatching { SortOption.valueOf(name) }.getOrNull() ?: return
        _ui.value = _ui.value.copy(sortOption = option)
        sharedPrefs.setSortOption(option.name)
        rebuild()
    }

    fun toggleExpandedView() {
        val newValue = !_ui.value.isExpandedView
        _ui.value = _ui.value.copy(isExpandedView = newValue)
        sharedPrefs.setIsExpandedView(newValue)
        rebuild()
    }

    /** @return новое значение (true — трей «отдых в ПО» показывается). */
    fun toggleShowTurnaroundRest(): Boolean {
        val newValue = !_ui.value.showTurnaroundRest
        _ui.value = _ui.value.copy(showTurnaroundRest = newValue)
        sharedPrefs.setShowTurnaroundRest(newValue)
        rebuild()
        return newValue
    }

    fun reload() {
        val us = userSettings ?: return
        loadRoutesJob?.cancel()
        loadRoutesJob = loadRoutes(us)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Множественный выбор (§9.2)
    // ═══════════════════════════════════════════════════════════════════════

    private val selectedRouteIds = mutableSetOf<String>()

    fun enterSelectionMode() {
        selectedRouteIds.clear()
        _ui.value = _ui.value.copy(isSelectionMode = true, selectedCount = 0)
        rebuild()
    }

    fun exitSelectionMode() {
        selectedRouteIds.clear()
        _ui.value = _ui.value.copy(isSelectionMode = false, selectedCount = 0)
        rebuild()
    }

    fun toggleRouteSelection(routeId: String) {
        if (selectedRouteIds.contains(routeId)) selectedRouteIds.remove(routeId)
        else selectedRouteIds.add(routeId)
        rebuild()
    }

    /** Выделить/снять все видимые (после фильтров) маршруты. */
    fun toggleSelectAll() {
        val visibleIds = currentFiltered().map { it.route.basicData.id }.toSet()
        if (visibleIds.isNotEmpty() && selectedRouteIds.containsAll(visibleIds)) {
            selectedRouteIds.clear()
        } else {
            selectedRouteIds.clear()
            selectedRouteIds.addAll(visibleIds)
        }
        rebuild()
    }

    private fun selectedRoutes(): List<Route> {
        val ids = selectedRouteIds.toSet()
        return latestRawRoutes.value
            .filter { ids.contains(it.route.basicData.id) }
            .map { it.route }
    }

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
            _messages.tryEmit(
                when {
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

    /** Массовый soft-delete в корзину (§9.2). */
    fun deleteSelectedRoutes() {
        val routes = selectedRoutes()
        if (routes.isEmpty()) return
        removeRouteJob?.cancel()
        removeRouteJob = viewModelScope.launch {
            var deleted = 0
            var failed = 0
            routes.forEach { route ->
                when (routeUseCase.markAsRemoved(route).first { it !is ResultState.Loading }) {
                    is ResultState.Success -> deleted++
                    else -> failed++
                }
            }
            _messages.tryEmit(
                when {
                    failed == 0 && deleted == 1 -> "Маршрут перемещён в корзину"
                    failed == 0 -> "Маршруты перемещены в корзину: $deleted"
                    deleted == 0 -> "Не удалось удалить маршруты"
                    else -> "Перемещено в корзину: $deleted, не удалось: $failed"
                }
            )
            exitSelectionMode()
        }
    }

    fun shareSelectedRoutes() {
        val routes = selectedRoutes()
        if (routes.isEmpty()) return
        viewModelScope.launch {
            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (rawToken.isNullOrBlank()) {
                _messages.tryEmit("Неавторизованный пользователь")
                return@launch
            }
            val bearerToken = "Bearer $rawToken"
            val links = mutableListOf<Pair<Route, String>>()
            var errorMessage: String? = null
            routes.forEach { route ->
                val result = shareRouteManager.createShareLink(route, bearerToken)
                    .first { it !is ResultState.Loading }
                if (result is ResultState.Success) {
                    links.add(route to result.data)
                } else if (result is ResultState.Error) {
                    errorMessage = result.entity.message
                        ?: result.entity.throwable?.message
                        ?: "Не удалось создать ссылку"
                }
            }
            if (links.isEmpty()) {
                _messages.tryEmit(errorMessage ?: "Не удалось создать ссылку")
                return@launch
            }
            _share.tryEmit(buildSharePayload(links))
            if (links.size < routes.size) {
                _messages.tryEmit("Ссылки созданы не для всех маршрутов")
            }
            exitSelectionMode()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Действия по маршруту (§9.0.1)
    // ═══════════════════════════════════════════════════════════════════════

    fun setFavoriteRoute(routeId: String) {
        val route = findRoute(routeId) ?: return
        viewModelScope.launch {
            val result = routeUseCase
                .setFavoriteRoute(route.basicData.id, !route.basicData.isFavorite)
                .first { it !is ResultState.Loading }
            when (result) {
                is ResultState.Success -> _messages.tryEmit(
                    if (result.data) "Маршрут добавлен в избранное"
                    else "Маршрут удален из избранного"
                )

                is ResultState.Error -> _messages.tryEmit(
                    result.entity.message ?: "Ошибка"
                )

                else -> Unit
            }
        }
    }

    /** Soft-delete: маршрут уходит в корзину (§9.4). */
    fun deleteRoute(routeId: String) {
        val route = findRoute(routeId) ?: return
        removeRouteJob?.cancel()
        removeRouteJob = viewModelScope.launch {
            routeUseCase.markAsRemoved(route).collect { result ->
                when (result) {
                    is ResultState.Success ->
                        _messages.tryEmit("Маршрут перемещён в корзину")

                    is ResultState.Error ->
                        _messages.tryEmit(result.entity.message ?: "Ошибка")

                    else -> Unit
                }
            }
        }
    }

    fun shareRoute(routeId: String) {
        val route = findRoute(routeId) ?: return
        viewModelScope.launch {
            val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (rawToken.isNullOrBlank()) {
                _messages.tryEmit("Неавторизованный пользователь")
                return@launch
            }
            val result = shareRouteManager.createShareLink(route, "Bearer $rawToken")
                .first { it !is ResultState.Loading }
            when (result) {
                is ResultState.Success ->
                    _share.tryEmit(buildSharePayload(listOf(route to result.data)))

                is ResultState.Error -> _messages.tryEmit(
                    result.entity.message
                        ?: result.entity.throwable?.message
                        ?: "Не удалось создать ссылку"
                )

                else -> Unit
            }
        }
    }

    fun syncRoute(routeId: String) {
        val route = findRoute(routeId) ?: return
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _messages.tryEmit("Войдите в аккаунт, чтобы синхронизировать маршрут")
                return@launch
            }
            if (!hasActiveSubscriptionNow()) {
                _messages.tryEmit("Синхронизация доступна по подписке")
                return@launch
            }
            val label = SyncManager.routeLabel(route)
            routesManager.saveRouteInRemote(route, "Bearer $token").collect { resultState ->
                when (resultState) {
                    is ResultState.Success -> {
                        val warnings = resultState.data.warnings
                        routeUseCase.setSynchronizedRoute(route.basicData.id).first()
                        if (warnings.isNotEmpty()) {
                            _messages.tryEmit(
                                "$label сохранен с предупреждениями:\n${warnings.joinToString("\n")}"
                            )
                        } else {
                            _messages.tryEmit("Маршрут сохранен в облаке")
                        }
                    }

                    is ResultState.Error -> {
                        val errorMsg = resultState.entity.message
                            ?: resultState.entity.throwable?.message
                            ?: "Ошибка синхронизации"
                        _messages.tryEmit("$label: $errorMsg")
                    }

                    else -> Unit
                }
            }
        }
    }

    /** Шторка «Создать копию маршрута?» → гейт подписки → форма копии. */
    fun onCopyRoute(routeId: String, callback: (AllRoutesFormDecision) -> Unit) {
        viewModelScope.launch {
            callback(newRouteDecision(basicId = routeId, isMakeCopy = true))
        }
    }

    /** FAB «Добавить маршрут»: гейт подписки (§9.0.1). */
    fun onNewRouteClick(callback: (AllRoutesFormDecision) -> Unit) {
        viewModelScope.launch {
            callback(newRouteDecision(basicId = null, isMakeCopy = false))
        }
    }

    private suspend fun newRouteDecision(
        basicId: String?,
        isMakeCopy: Boolean,
    ): AllRoutesFormDecision {
        val freeRoutesLimit = NewRouteIosViewModel.FREE_ROUTES_LIMIT
        return try {
            val now = nowMs()
            val setting = settingsUseCase.getUserSettingFlow().first()
            val time = setting.subscriptionPeriod
            val gracePeriod = 24 * 3_600_000L
            val subscriptionActive = time != 0L && time + gracePeriod >= now
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val isAuthorized = !token.isNullOrBlank()
            val routesSize = withContext(Dispatchers.Default) {
                routeUseCase.listRouteWithDeleting().size
            }
            when {
                subscriptionActive -> AllRoutesFormDecision(
                    type = "open", basicId = basicId, isMakeCopy = isMakeCopy,
                    freeRoutesLeft = 0, isAuthorized = isAuthorized,
                )

                routesSize >= freeRoutesLimit -> AllRoutesFormDecision(
                    type = "limit", basicId = basicId, isMakeCopy = isMakeCopy,
                    freeRoutesLeft = 0, isAuthorized = isAuthorized,
                )

                else -> AllRoutesFormDecision(
                    type = "trial", basicId = basicId, isMakeCopy = isMakeCopy,
                    freeRoutesLeft = (freeRoutesLimit - routesSize).coerceAtLeast(0),
                    isAuthorized = isAuthorized,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            AllRoutesFormDecision(
                type = "error", basicId = basicId, isMakeCopy = isMakeCopy,
                freeRoutesLeft = 0, isAuthorized = false,
            )
        }
    }

    /** Есть ли записанные треки по поездам маршрута (строка «Будут удалены и записанные треки.»). */
    fun hasRecordedTracks(routeId: String): Boolean {
        val repo = trackRepository ?: return false
        val route = findRoute(routeId) ?: return false
        return runCatching {
            route.trains.any { train -> repo.tracksForTrain(train.trainId).isNotEmpty() }
        }.getOrDefault(false)
    }

    private fun findRoute(routeId: String): Route? =
        latestRawRoutes.value.firstOrNull { it.route.basicData.id == routeId }?.route

    // ═══════════════════════════════════════════════════════════════════════
    // Синхронизация (§9.0)
    // ═══════════════════════════════════════════════════════════════════════

    /** Тихая двусторонняя синхронизация при открытии экрана (cooldown 5 минут). */
    fun syncOnScreenOpen() {
        if (backgroundSyncJob?.isActive == true) return
        if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return
        backgroundSyncJob = viewModelScope.launch(Dispatchers.Default) {
            // Сначала показываем локальный список и завершаем его расчёты.
            kotlinx.coroutines.delay(SyncManager.BACKGROUND_SYNC_START_DELAY_MILLIS)
            if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return@launch
            if (!hasActiveSubscriptionNow()) return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch
            try {
                var failureMessage: String? = null
                var connectivityFailed = false
                var pendingDeletionCount = 0
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> {
                            failureMessage = NetworkErrorMapper.syncFailureMessage(
                                state.entity.message, state.entity.throwable
                            )
                            if (NetworkErrorMapper.isConnectivityError(state.entity.throwable) ||
                                NetworkErrorMapper.isConnectivityMessage(failureMessage)
                            ) connectivityFailed = true
                        }

                        is ResultState.Success -> {
                            pendingDeletionCount = state.data.pendingDeletionRouteIds.size
                        }

                        else -> Unit
                    }
                }
                // Нет сети — тихий красный индикатор вместо тоста (§9.0).
                _ui.value = _ui.value.copy(isOffline = connectivityFailed)
                val message = if (connectivityFailed) null
                else failureMessage ?: if (pendingDeletionCount > 0) {
                    // BUG-10 (паритет с Android): текст ведёт в «Настройки», хотя
                    // корзина находится в Профиле (§9.4).
                    "На сервере пропало маршрутов: $pendingDeletionCount. Проверьте корзину в Настройках."
                } else null
                message?.let { _messages.tryEmit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (NetworkErrorMapper.isConnectivityError(e)) {
                    _ui.value = _ui.value.copy(isOffline = true)
                } else {
                    _messages.tryEmit(
                        NetworkErrorMapper.syncFailureMessage(e.message, e)
                    )
                }
            }
        }
    }

    fun stopScreenSync() {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = null
        _ui.value = _ui.value.copy(isBackgroundSyncing = false)
    }

    /** Pull-to-refresh (§9.0): сообщения через watchMessages. */
    fun pullToRefresh() {
        viewModelScope.launch {
            if (!hasActiveSubscriptionNow()) {
                _messages.tryEmit("Синхронизация доступна по подписке")
                return@launch
            }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                _messages.tryEmit("Необходимо войти в профиль")
                return@launch
            }
            try {
                var failureMessage: String? = null
                var pendingDeletionCount = 0
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> failureMessage = NetworkErrorMapper.syncFailureMessage(
                            state.entity.message, state.entity.throwable
                        )

                        is ResultState.Success ->
                            pendingDeletionCount = state.data.pendingDeletionRouteIds.size

                        else -> Unit
                    }
                }
                _ui.value = _ui.value.copy(isOffline = false)
                val message = when {
                    failureMessage != null -> failureMessage!!
                    pendingDeletionCount > 0 ->
                        // BUG-10 (паритет с Android): см. syncOnScreenOpen.
                        "На сервере пропало маршрутов: $pendingDeletionCount. Проверьте корзину в Настройках."

                    else -> "Синхронизация завершена"
                }
                _messages.tryEmit(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (NetworkErrorMapper.isConnectivityError(e)) {
                    _ui.value = _ui.value.copy(isOffline = true)
                }
                _messages.tryEmit(
                    NetworkErrorMapper.syncFailureMessage(e.message, e)
                        .ifBlank { "Синхронизация не выполнена: сервер не завершил обработку данных" }
                )
            }
        }
    }

    private suspend fun hasActiveSubscriptionNow(): Boolean =
        settingsUseCase.getUserSettingFlow().first().subscriptionPeriod > nowMs()

    /** Освобождение при закрытии экрана (Swift вызывает в deinit wrapper'а). */
    fun dispose() {
        loadRoutesJob?.cancel()
        backgroundSyncJob?.cancel()
        removeRouteJob?.cancel()
    }

    // Временное соответствие …
    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun displayTz(us: UserSettings): TimeZone =
        runCatching { TimeZone.of(us.displayTimeZone()) }.getOrElse { TimeZone.of("GMT+3") }

    private companion object {
        val MONTH_NAMES = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )
    }
}
