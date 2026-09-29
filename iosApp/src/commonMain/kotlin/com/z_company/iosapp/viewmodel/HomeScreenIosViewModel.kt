@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.UtilForMonthOfYear.getNormaHoursInDate
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Locomotive
import com.z_company.domain.entities.route.OverRestRoutes.adjacentRoutesOfMonth
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.filterByConsiderFutureRoute
import com.z_company.domain.entities.route.UtilsForEntities.findCurrentRoute
import com.z_company.domain.entities.route.UtilsForEntities.findNextFutureRoute
import com.z_company.domain.entities.route.UtilsForEntities.getBreakDuration
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getSingleLocomotiveTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeInMonth
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTimeWithoutHoliday
import com.z_company.domain.entities.route.UtilsForEntities.getWorkingTimeOnAHoliday
import com.z_company.domain.entities.route.UtilsForEntities.isExtendedServicePhaseTrains
import com.z_company.domain.entities.route.UtilsForEntities.isFuture
import com.z_company.domain.entities.route.UtilsForEntities.isHeavyTrains
import com.z_company.domain.entities.route.UtilsForEntities.isHolidayTimeInRoute
import com.z_company.domain.entities.route.UtilsForEntities.isLongCompositionTrain
import com.z_company.domain.entities.route.UtilsForEntities.isTimeWorkValid
import com.z_company.domain.entities.route.UtilsForEntities.isTransition
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.salary.SalaryCalculationHelper
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.toMoneyString
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.NetworkErrorMapper
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * iOS-ViewModel Главного экрана — SCREEN_SPECS §4 (перенос Android `HomeViewModel`,
 * `PullToSyncViewModel`, расчётов отдыха из `RouteActionsHelper`).
 *
 * Все расчёты — через общий domain (`UtilsForEntities`, `SalaryCalculationHelper`,
 * `NormaUseCase`), форматирование — здесь же (пояс отображения §0.1), чтобы Swift
 * только показывал готовые строки.
 *
 * Swift подписывается через `watch*`-колбэки (шаблон AGENTS.md).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeScreenIosViewModel(
    private val routeUseCase: RouteUseCase,
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val normaUseCase: NormaUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val syncManager: SyncManager,
    private val secureTokenStorage: SecureTokenStorage,
) : ViewModel() {

    // ── Исходные данные ─────────────────────────────────────────────────────
    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null
    /** Маршруты выбранного месяца (null — ещё не загружены). */
    private var monthRoutes: List<Route>? = null
    /** Все маршруты всех месяцев — для живых блоков, счётчиков и соседей месяца. */
    private var allRoutes: List<Route> = emptyList()
    private var monthYearList: List<Pair<Int, Int>> = emptyList()
    private var monthList: List<Int> = emptyList()
    private var yearList: List<Int> = emptyList()
    private var normaHours: Int? = null
    private var metrics = Metrics()
    private var unsyncedRoutesCount = 0
    private var freeRoutesUsedCount = 0
    private var isAuthorized = false
    private var isBackgroundSyncing = false
    private var isRoutesError = false
    /** Ключи скрытых карточек-уведомлений (§4.4 п.5). */
    private val dismissedNotices = mutableSetOf<String>()
    private var lastAdjacentRouteIds: List<String> = emptyList()

    // ── Живые блоки (§4.3) ──────────────────────────────────────────────────
    private var restParams: RestParams? = null
    private var lastCurrentRouteId: String? = null
    private var lastPreviousRouteId: String? = null
    private var restJob: Job? = null
    private var tickerJob: Job? = null

    // ── Jobs ────────────────────────────────────────────────────────────────
    private var calcJob: Job? = null
    private var backgroundSyncJob: Job? = null
    private var manualSyncJob: Job? = null
    private var isPullRefreshing = false

    // ── Выходы для Swift ───────────────────────────────────────────────────
    private val _ui = MutableStateFlow(HomeIosScreenUi())
    private val _live = MutableStateFlow(HomeIosLive())
    private val _syncDialog = MutableStateFlow(HomeIosSyncDialog())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)

    /** Ключи этапов выгрузки и их состояние: null — идёт, "" — успех, иначе текст ошибки. */
    private var syncSteps: LinkedHashMap<String, StepState> = LinkedHashMap()
    private var syncRouteErrors: List<String> = emptyList()
    private var syncRoutesTotalAttempted = 0
    private var syncRoutesSavedCount = 0
    private var syncReportUserId: String? = null

    private val routeParams =
        MutableStateFlow<Pair<MonthOfYear, TimeCalculationContext>?>(null)
    private val selectedMonthFlow = MutableStateFlow<MonthOfYear?>(null)

    init {
        loadMonthList()
        observeNorma()
        initListStationAndLocomotiveSeries()
        observeSettings()
        observeMonthRoutes()
        observeAllRoutes()
        refreshAuthorization()
        startTicker()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Загрузка
    // ═══════════════════════════════════════════════════════════════════════

    private fun observeSettings() {
        viewModelScope.launch {
            combine(
                salarySettingUseCase.salarySettingFlow().map { it as SalarySetting? }.onStart { emit(null) },
                settingsUseCase.getUserSettingFlow().map { it as UserSettings? }.onStart { emit(null) },
            ) { ss, us -> ss to us }
                .collectLatest { (ss, us) ->
                    salarySetting = ss
                    if (us == null || ss == null) {
                        routeParams.value = null
                        return@collectLatest
                    }
                    userSettings = us
                    selectedMonthFlow.value = us.selectMonthOfYear
                    val newParams = us.selectMonthOfYear to TimeCalculationContext.from(us)
                    val isMonthOrTzChanged = routeParams.value != newParams
                    routeParams.value = newParams
                    // Настройки могли прийти позже первого списка всех маршрутов —
                    // досчитываем живые блоки (идемпотентно, §4.3).
                    recomputeRestParams()
                    publishLive()
                    rebuild()
                    // Реактивный пересчёт при изменении настроек доплат/учёта будущих,
                    // когда месяц/пояс не менялись (debounce 150 мс, без перезагрузки из БД).
                    if (!isMonthOrTzChanged && monthRoutes != null) {
                        delay(150)
                        runAllCalculations()
                    }
                }
        }
    }

    private fun observeMonthRoutes() {
        viewModelScope.launch {
            routeParams
                .filterNotNull()
                .debounce(300)
                .flatMapLatest { (month, context) ->
                    routeUseCase.routeListByMonthFlow(month, context)
                        .map<List<Route>, ResultState<List<Route>>> { ResultState.Success(it) }
                        .onStart { emit(ResultState.Loading()) }
                }
                .collect { result ->
                    when (result) {
                        // Не очищаем список — старые данные видны, пока грузятся новые (§4.1).
                        is ResultState.Loading -> Unit
                        is ResultState.Success -> {
                            isRoutesError = false
                            monthRoutes = result.data
                                .sortedByDescending { it.basicData.timeStartWork ?: Long.MIN_VALUE }
                            rebuild()
                            runAllCalculations()
                        }
                        is ResultState.Error -> {
                            isRoutesError = true
                            rebuild()
                        }
                    }
                }
        }
    }

    private fun observeAllRoutes() {
        viewModelScope.launch {
            routeUseCase.getListRoutesAsFlow().collect { routes ->
                allRoutes = routes
                unsyncedRoutesCount = routes.count { !it.basicData.isSynchronized }
                // С учётом корзины — та же логика, что у гейта «+» (§3.4, §4.4 п.5).
                freeRoutesUsedCount = withContext(Dispatchers.Default) {
                    runCatching { routeUseCase.listRouteWithDeleting().size }.getOrDefault(freeRoutesUsedCount)
                }
                recomputeRestParams()
                publishLive()
                rebuild()
                recalcIfAdjacentRoutesChanged()
            }
        }
    }

    private fun loadMonthList() {
        viewModelScope.launch {
            calendarUseCase.loadFlowMonthOfYearListState().collect { list ->
                monthList = list.map { it.month }.distinct().sorted()
                yearList = list.map { it.year }.distinct().sorted()
                monthYearList = list
                    .map { it.year to it.month }
                    .distinct()
                    .sortedWith(compareBy({ it.first }, { it.second }))
                rebuild()
            }
        }
    }

    private fun observeNorma() {
        viewModelScope.launch {
            selectedMonthFlow
                .map { it?.let { m -> m.year to m.month } }
                .distinctUntilChanged()
                .flatMapLatest { pair ->
                    if (pair == null) flowOf(null)
                    else normaUseCase.normaHoursFlow(pair.first, pair.second).map { it as Int? }
                }
                .collect { hours ->
                    normaHours = hours
                    rebuild()
                }
        }
    }

    /** Однократно собирает серии и станции из маршрутов в списки автодополнения (§4.1). */
    private fun initListStationAndLocomotiveSeries() {
        if (sharedPrefs.tokenIsLoadStationAndLocomotiveSeries()) return
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val routes = routeUseCase.getListRoutes()
                val series = routes.flatMap { r -> r.locomotives.mapNotNull { it.series } }
                val stations = routes.flatMap { r -> r.trains.flatMap { t -> t.stations.mapNotNull { it.stationName } } }
                settingsUseCase.setLocomotiveSeriesList(series)
                settingsUseCase.setStations(stations)
                sharedPrefs.setTokenIsLoadStationAndLocomotiveSeries(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
            }
        }
    }

    private fun refreshAuthorization() {
        viewModelScope.launch {
            val token = runCatching { secureTokenStorage.getAuthBearerTokenFlow().first() }.getOrNull()
            isAuthorized = !token.isNullOrBlank()
            rebuild()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Расчёты метрик месяца (§4.2)
    // ═══════════════════════════════════════════════════════════════════════

    private fun runAllCalculations() {
        val us = userSettings ?: return
        val ss = salarySetting ?: return
        val routes = monthRoutes ?: return
        val all = allRoutes
        calcJob?.cancel()
        calcJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { computeMetrics(us, ss, routes, all, nowMs()) }
            metrics = result.first
            lastAdjacentRouteIds = result.second
            rebuild()
        }
    }

    /** Соседи месяца пришли позже расчёта — пересчёт переотдыха на стыке месяцев. */
    private fun recalcIfAdjacentRoutesChanged() {
        val us = userSettings ?: return
        if (salarySetting == null || monthRoutes == null) return
        val ids = allRoutes
            .filterByConsiderFutureRoute(us.isConsiderFutureRoute, nowMs())
            .adjacentRoutesOfMonth(us.selectMonthOfYear, TimeCalculationContext.from(us))
            .map { it.basicData.id }
        if (ids == lastAdjacentRouteIds) return
        runAllCalculations()
    }

    private suspend fun computeMetrics(
        us: UserSettings,
        ss: SalarySetting,
        fullRouteList: List<Route>,
        all: List<Route>,
        now: Long,
    ): Pair<Metrics, List<String>> = coroutineScope {
        val month = us.selectMonthOfYear
        val ctx = TimeCalculationContext.from(us)
        val filtered = fullRouteList.filterByConsiderFutureRoute(us.isConsiderFutureRoute, now)
        val adjacent = all
            .filterByConsiderFutureRoute(us.isConsiderFutureRoute, now)
            .adjacentRoutesOfMonth(month, ctx)
        val helper = SalaryCalculationHelper(
            userSettings = us,
            salarySetting = ss,
            allRoutes = filtered,
            workScheduleProfile = sharedPrefs.getWorkScheduleProfile(),
            adjacentRoutes = adjacent,
        )

        val extended = async { calc { helper.getTotalTimeSurchargeServicePhaseFlow().first() } }
        val longTrains = async { calc { helper.getTotalTimeLongTrainsFlow().first() } }
        val heavy = async { calc { helper.getTotalTimeHeavyTrainsFlow().first() } }
        val night = async { calc { filtered.getNightTime(us) } }
        val single = async { calc { filtered.getSingleLocomotiveTime() } }
        val holiday = async { calc { filtered.getWorkingTimeOnAHoliday(month, ctx).first() } }
        val money = async { calc { helper.getMoneyToBeCredited().first() } }
        val total = calc { filtered.getWorkTime(month, ctx) }
        val withoutHoliday = calc { filtered.getWorkTimeWithoutHoliday(month, ctx) }
        val passenger = calc { filtered.getPassengerTime(month, ctx) }
        val today = if (us.isConsiderFutureRoute) {
            calc {
                fullRouteList
                    .filter { r -> r.basicData.timeEndWork?.let { it <= now } ?: false }
                    .getWorkTime(month, ctx)
            }.value ?: 0L
        } else 0L

        val nightResult = night.await()
        val holidayResult = holiday.await()
        val m = Metrics(
            totalTimeWithHoliday = total,
            timeWithoutHoliday = withoutHoliday.value ?: 0L,
            todayWorkTime = today,
            // BUG-11 (паритет с Android): ошибка расчёта праздничных часов ставит
            // в состояние ошибки поле НОЧНЫХ часов.
            night = if (holidayResult.isError) Calc(null, true) else nightResult,
            passenger = passenger,
            single = single.await(),
            extended = extended.await(),
            longTrains = longTrains.await(),
            heavy = heavy.await(),
            toBeCredited = money.await(),
        )
        m to adjacent.map { it.basicData.id }
    }

    private suspend fun <T> calc(block: suspend () -> T): Calc<T> = try {
        Calc(block(), false)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        Calc(null, true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Сборка состояния экрана
    // ═══════════════════════════════════════════════════════════════════════

    private fun rebuild() {
        val us = userSettings
        val ss = salarySetting
        if (us == null || ss == null) {
            _ui.value = _ui.value.copy(isBackgroundSyncing = isBackgroundSyncing, isAuthorized = isAuthorized)
            return
        }
        val now = nowMs()
        val month = us.selectMonthOfYear
        val isDecimal = us.isDecimalTime
        val displayTz = displayTz(us)

        val currentIndex = monthYearList.indexOfFirst { it.first == month.year && it.second == month.month }

        // ── «ОТРАБОТАНО» ──
        val totalTime = metrics.timeWithoutHoliday
        val totalWithHoliday = metrics.totalTimeWithHoliday
        val normaMonth = normaHours ?: month.getPersonalNormaHours()
        val chip = if (normaMonth > 0 && totalWithHoliday.value != null) {
            val diff = totalTime - normaMonth.toLong() * HOUR
            val text = fmtDuration(abs(diff), isDecimal)
            if (diff >= 0) "сверх $text" else "еще $text"
        } else null
        val breakdown = totalWithHoliday.value?.let { t ->
            if (totalTime != t) " (${fmtDuration(totalTime, isDecimal)} + ${fmtDuration(t - totalTime, isDecimal)})" else null
        }
        val money = metrics.toBeCredited
        val moneyState = when {
            money.isError -> 2
            money.value != null -> 1
            else -> 0
        }
        val moneyText = when (moneyState) {
            1 -> money.value.toMoneyString(currencySymbol(us.country))
            2 -> "—"
            else -> "считаем деньги"
        }

        _ui.value = HomeIosScreenUi(
            isReady = true,
            isError = isRoutesError,
            monthTitle = MONTH_NAMES.getOrElse(month.month) { "" },
            yearTitle = month.year.toString(),
            selectedMonth = month.month,
            selectedYear = month.year,
            hasPrevMonth = currentIndex > 0,
            hasNextMonth = currentIndex in 0 until monthYearList.lastIndex,
            monthOptions = monthList,
            yearOptions = yearList,
            moneyText = moneyText,
            moneyState = moneyState,
            totalTimeText = totalWithHoliday.value?.let { fmtDuration(it, isDecimal) },
            isTotalTimeError = totalWithHoliday.isError,
            breakdownText = breakdown,
            normaChipText = chip,
            metricPages = buildMetricPages(us, month, normaMonth, now, displayTz),
            notices = buildNotices(us, now, displayTz),
            lastRoutes = buildLastRoutes(us, ss, month, now, displayTz),
            monthRoutesCount = monthRoutes?.size ?: 0,
            isBackgroundSyncing = isBackgroundSyncing,
            isAuthorized = isAuthorized,
        )
    }

    private fun buildMetricPages(
        us: UserSettings,
        month: MonthOfYear,
        normaMonth: Int,
        now: Long,
        displayTz: TimeZone,
    ): List<List<HomeIosMetricRow>> {
        val isDecimal = us.isDecimalTime
        val totalTime = metrics.timeWithoutHoliday
        val normaMonthMs = normaMonth.toLong() * HOUR
        val normaToday = month.getNormaHoursInDate(now)
        val normaTodayMs = normaToday.toLong() * HOUR
        val todayText = fmtDate(now, displayTz)

        val third = if (us.isConsiderFutureRoute) {
            HomeIosMetricRow(
                label = "Отработано на $todayText",
                value = fmtDuration(metrics.todayWorkTime, isDecimal),
                isError = false,
                progress = ratio(metrics.todayWorkTime, normaTodayMs),
            )
        } else {
            val difference = abs(normaMonthMs - totalTime)
            HomeIosMetricRow(
                label = if (totalTime > normaMonthMs) "Сверх нормы" else "Осталось до нормы",
                value = fmtDuration(difference, isDecimal),
                isError = false,
                progress = ratio(difference, normaMonthMs),
            )
        }
        val page1 = listOf(
            HomeIosMetricRow("Норма на месяц", "$normaMonth ч.", false, ratio(totalTime, normaMonthMs)),
            HomeIosMetricRow("Норма на $todayText", "$normaToday ч.", false, ratio(totalTime, normaTodayMs)),
            third,
        )
        val base = metrics.totalTimeWithHoliday.value ?: 0L
        fun row(label: String, c: Calc<Long>) = HomeIosMetricRow(
            label = label,
            value = c.value?.let { fmtDuration(it, isDecimal) },
            isError = c.isError,
            progress = ratio(c.value ?: 0L, base),
        )
        val page2 = listOf(
            row("Ночные", metrics.night),
            row("Пассажиром", metrics.passenger),
            row("Резервом", metrics.single),
        )
        val page3 = listOf(
            row("Удл. плечи обслуживания", metrics.extended),
            row("Длинносоставные", metrics.longTrains),
            row("Тяжелые", metrics.heavy),
        )
        return listOf(page1, page2, page3)
    }

    private fun buildNotices(us: UserSettings, now: Long, displayTz: TimeZone): List<HomeIosNotice> {
        val result = mutableListOf<HomeIosNotice>()
        val subscriptionEndTime = us.subscriptionPeriod
        val hasActiveSubscription = subscriptionEndTime > now
        val dateText = fmtDate(subscriptionEndTime, displayTz)

        // Подписка (была когда-либо).
        if (subscriptionEndTime != 0L) {
            if (subscriptionEndTime > now) {
                val daysLeft = ceil((subscriptionEndTime - now).toDouble() / DAY).toLong()
                if (daysLeft <= SUBSCRIPTION_EXPIRING_SOON_DAYS) {
                    result += HomeIosNotice(
                        dismissKey = "subscription:soon:$daysLeft",
                        tone = "warning",
                        icon = "schedule",
                        title = "Подписка заканчивается",
                        message = if (daysLeft <= 0L) "Заканчивается сегодня, $dateText"
                        else "Осталось $daysLeft ${daysWord(daysLeft)} — до $dateText",
                        hint = "Продлите заранее — новый срок прибавится к текущему, дни не сгорят.",
                        buttonText = "Продлить",
                        action = "purchases",
                        progress = -1f,
                    )
                }
            } else {
                result += HomeIosNotice(
                    dismissKey = "subscription:expired",
                    tone = "danger",
                    icon = "alert",
                    title = "Подписка закончилась",
                    message = "Закончилась $dateText",
                    hint = "Маршруты и история сохранены. Но добавлять новые и пользоваться синхронизацией нельзя, пока подписка не возобновлена.",
                    buttonText = "Возобновить",
                    action = "purchases",
                    progress = -1f,
                )
            }
        } else {
            // Бесплатный период — подписки никогда не было.
            val limit = NewRouteIosViewModel.FREE_ROUTES_LIMIT
            val used = freeRoutesUsedCount.coerceIn(0, limit)
            val remaining = (limit - used).coerceAtLeast(0)
            val progress = used.toFloat() / limit.toFloat()
            val key = "free:$freeRoutesUsedCount"
            result += when {
                used >= limit -> HomeIosNotice(
                    key, "danger", "alert",
                    "Бесплатный лимит исчерпан",
                    "Использовано $limit из $limit бесплатных маршрутов",
                    "Маршруты и история сохранены. Чтобы добавлять новые и пользоваться синхронизацией — оформите подписку.",
                    "Оформить подписку", "purchases", progress,
                )
                remaining <= FREE_TRIAL_RUNNING_LOW -> HomeIosNotice(
                    key, "warning", "schedule",
                    "Бесплатный период",
                    "Осталось $remaining из $limit бесплатных маршрутов",
                    "Оформите подписку заранее, чтобы не потерять возможность добавлять маршруты.",
                    "Оформить подписку", "purchases", progress,
                )
                else -> HomeIosNotice(
                    key, "neutral", "crown",
                    "Бесплатный период",
                    "Использовано $used из $limit бесплатных маршрутов",
                    "Оформите подписку в любой момент — снимет лимит и откроет синхронизацию.",
                    "Оформить подписку", "purchases", progress,
                )
            }
        }

        if (hasActiveSubscription && unsyncedRoutesCount > 2) {
            result += HomeIosNotice(
                dismissKey = "sync:$unsyncedRoutesCount",
                tone = "neutral",
                icon = "none",
                title = "Внимание!",
                message = "Не синхронизировано маршрутов: $unsyncedRoutesCount",
                hint = "Проверьте подключение к интернету и выполните синхронизацию.",
                buttonText = "Синхронизировать",
                action = "sync",
                progress = -1f,
            )
        }
        return result.filter { it.dismissKey !in dismissedNotices }
    }

    private fun buildLastRoutes(
        us: UserSettings,
        ss: SalarySetting,
        month: MonthOfYear,
        now: Long,
        displayTz: TimeZone,
    ): List<HomeIosRouteCard> {
        val routes = monthRoutes ?: return emptyList()
        val count = routes.size
        val ctx = TimeCalculationContext.from(us)
        val offsetMs = us.timeZone
        return routes.take(2).mapIndexed { index, route ->
            val start = route.basicData.timeStartWork
            val end = route.basicData.timeEndWork
            val isDifferenceDate = start != null && end != null &&
                localDate(start, displayTz) != localDate(end, displayTz)
            val endText = when {
                end == null -> ""
                isDifferenceDate || start == null -> fmtDateMiniTime(end, displayTz)
                else -> fmtTime(end, displayTz)
            }
            val workInMonth = runCatching { route.getWorkTimeInMonth(month, ctx) }.getOrNull()
            val workTime = route.getWorkTime()
            val icons = buildList {
                if (runCatching { isHolidayTimeInRoute(month, us, route) }.getOrDefault(false)) add("holiday")
                if (route.getBreakDuration() > 0L) add("break")
                if (runCatching { isLongCompositionTrain(ss, route) }.getOrDefault(false)) add("length")
                if (runCatching { isHeavyTrains(ss, route) }.getOrDefault(false)) add("weight")
                if (runCatching { isExtendedServicePhaseTrains(ss, route) }.getOrDefault(false)) add("shoulder")
                if (route.basicData.isOnePersonOperation) add("solo")
                if ((route.getPassengerTime() ?: 0L) > 0L) add("passenger")
                if (workTime != null && workTime > 12 * HOUR) add("over12")
                if (route.trains.any { it.pusher != null }) add("pusher")
                if (route.trains.any { it.doubleTraction != null }) add("double")
                if (route.trains.any { it.doubledTrain != null }) add("coupled")
            }
            HomeIosRouteCard(
                basicId = route.basicData.id,
                startText = start?.let { fmtDateMiniTime(it, displayTz) } ?: "",
                endText = endText,
                endTextWithDate = end?.let { fmtDateMiniTime(it, displayTz) } ?: "",
                durationText = fmtDuration(workInMonth, us.isDecimalTime),
                summary = routeSummary(route),
                numberText = "#${count - index}",
                // BUG-16 (паритет с Android): isFuture прибавляет к now смещение пояса
                // пользователя от Москвы (UserSettings.timeZone).
                isFuture = route.isFuture(offsetMs),
                isTransition = route.isTransition(offsetMs),
                icons = icons,
                isSynchronized = route.basicData.isSynchronized,
                isFavorite = route.basicData.isFavorite,
            )
        }
    }

    /** Свёрнутая строка карточки: поезд с самым поздним отправлением с первой станции, иначе первая прочая работа. */
    private fun routeSummary(route: Route): String {
        val sortedTrains = route.trains
            .filter { it.stations.firstOrNull()?.timeDeparture != null }
            .sortedByDescending { it.stations.firstOrNull()?.timeDeparture } +
            route.trains.filter { it.stations.firstOrNull()?.timeDeparture == null }
        val train = sortedTrains.firstOrNull()
        if (train != null) {
            val tn = if (!train.number.isNullOrBlank()) "№${train.number} " else ""
            val first = train.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = if (train.stations.size > 1) train.stations.last().stationName?.takeIf { it.isNotBlank() } else null
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
    // Живые блоки (§4.3)
    // ═══════════════════════════════════════════════════════════════════════

    /** Тикер: на границе каждой минуты, а пока виден блок отдыха — каждую секунду. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val now = nowMs()
                onTick(now)
                val isRest = _live.value.kind.startsWith("rest")
                val step = if (isRest) 1_000L else 60_000L
                delay((step - now % step).coerceAtLeast(50L))
            }
        }
    }

    private fun onTick(now: Long) {
        val us = userSettings ?: return
        val currentId = allRoutes.findCurrentRoute(now, us)?.basicData?.id
        val previousId = previousFinishedRoute(allRoutes, now)?.basicData?.id
        val boundaryPassed = restParams?.let { now >= it.boundary } ?: false
        if (currentId != lastCurrentRouteId || previousId != lastPreviousRouteId || boundaryPassed) {
            val wasCurrent = lastCurrentRouteId
            lastCurrentRouteId = currentId
            recomputeRestParams()
            // Маршрут завершился / начался — пересчёт «Отработано на сегодня» (§4.3).
            if (wasCurrent != currentId) runAllCalculations()
        }
        publishLive()
    }

    private fun recomputeRestParams() {
        val us = userSettings ?: return
        val all = allRoutes
        restJob?.cancel()
        restJob = viewModelScope.launch {
            val now = nowMs()
            val previous = previousFinishedRoute(all, now)
            lastPreviousRouteId = previous?.basicData?.id
            restParams = try {
                previous?.let { computeRestParams(it, all, us, now) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            }
            publishLive()
        }
    }

    /** Последний завершённый маршрут: `сдача < now`, максимальная сдача. */
    private fun previousFinishedRoute(all: List<Route>, now: Long): Route? = all
        .filter {
            val end = it.basicData.timeEndWork
            it.basicData.timeStartWork != null && end != null && end < now
        }
        .maxByOrNull { it.basicData.timeEndWork ?: 0L }

    private suspend fun computeRestParams(previous: Route, all: List<Route>, us: UserSettings, now: Long): RestParams? {
        val restStart = previous.basicData.timeEndWork ?: return null
        return if (previous.basicData.restPointOfTurnover) {
            val shortEnd = shortRest(previous, all, us)?.second
            val fullEnd = fullRest(previous, all, us)?.second
            // Окно — до явки следующего (обратного) маршрута, иначе до конца полного отдыха.
            val nextStart = all.findNextFutureRoute(now)?.basicData?.timeStartWork
            val boundary = nextStart ?: fullEnd ?: return null
            if (now >= boundary) return null
            RestParams(
                isTurnover = true,
                restStart = restStart,
                shortEnd = shortEnd,
                fullEnd = fullEnd ?: boundary,
                minEnd = null,
                boundary = boundary,
            )
        } else {
            val home = homeRest(previous, us) ?: return null
            if (now >= home.first) return null
            RestParams(
                isTurnover = false,
                restStart = restStart,
                shortEnd = null,
                fullEnd = home.first,
                minEnd = home.second,
                boundary = home.first,
            )
        }
    }

    /** Второй отдых в ПО подряд — минимум `minTimeRestPointOfTurnoverSecond`. */
    private fun effectiveTurnaroundMinimum(route: Route, routes: List<Route>, us: UserSettings): Long {
        val start = route.basicData.timeStartWork
        val isSecond = route.basicData.restPointOfTurnover && start != null && routes.asSequence()
            .filter { it.basicData.id != route.basicData.id }
            .filter { !it.basicData.isDeleted }
            .filter { (it.basicData.timeStartWork ?: Long.MAX_VALUE) < start }
            .maxByOrNull { it.basicData.timeStartWork ?: Long.MIN_VALUE }
            ?.basicData?.restPointOfTurnover == true
        return if (isSecond) us.minTimeRestPointOfTurnoverSecond else us.minTimeRestPointOfTurnover
    }

    /** Короткий отдых в ПО: (длительность, окончание) — как `RouteActionsHelper.calculateShortRest`. */
    private fun shortRest(route: Route, routes: List<Route>, us: UserSettings): Pair<Long, Long>? {
        val start = route.basicData.timeStartWork ?: return null
        val end = route.basicData.timeEndWork ?: return null
        if (!route.isTimeWorkValid()) return null
        val minimum = effectiveTurnaroundMinimum(route, routes, us)
        val time = route.getWorkTime() ?: (end - start)
        var halfRest = time / 2
        // BUG-13 (паритет с Android): «округление вверх до минуты» прибавляет целую
        // минуту к некратной половине (окончание может получиться с :30 секунд).
        if (halfRest % 60_000L != 0L) halfRest += 60_000L
        val effective = if (halfRest > minimum) halfRest else minimum
        return effective to (end + effective)
    }

    /** Полный отдых в ПО — как `RouteActionsHelper.calculateFullRest`. */
    private fun fullRest(route: Route, routes: List<Route>, us: UserSettings): Pair<Long, Long>? {
        val start = route.basicData.timeStartWork ?: return null
        val end = route.basicData.timeEndWork ?: return null
        if (!route.isTimeWorkValid()) return null
        val minimum = effectiveTurnaroundMinimum(route, routes, us)
        val time = route.getWorkTime() ?: (end - start)
        val effective = if (time > minimum) time else minimum
        return effective to (end + effective)
    }

    /**
     * Домашний отдых — как `RouteActionsHelper.calculationHomeRest`: (окончание полного, окончание минимального).
     * BUG-14 (паритет с Android): цепочка берётся из маршрутов ВЫБРАННОГО в UI месяца и
     * предыдущего, а не месяца самого маршрута; суммируется `сдача − явка`, а не getWorkTime.
     */
    private suspend fun homeRest(route: Route, us: UserSettings): Pair<Long, Long>? {
        val routeEnd = route.basicData.timeEndWork ?: return null
        if (route.basicData.timeStartWork == null) return null
        val current = us.selectMonthOfYear
        val previousMonth = if (current.month > 0) {
            current.copy(month = current.month - 1, days = emptyList())
        } else {
            current.copy(year = current.year - 1, month = 11, days = emptyList())
        }
        val tz = us.timeZone
        val (currentResult, prevResult) = coroutineScope {
            val c = async(Dispatchers.Default) {
                routeUseCase.listRoutesByMonth(current, tz).first { it is ResultState.Success || it is ResultState.Error }
            }
            val p = async(Dispatchers.Default) {
                routeUseCase.listRoutesByMonth(previousMonth, tz).first { it is ResultState.Success || it is ResultState.Error }
            }
            c.await() to p.await()
        }
        val currentList = (currentResult as? ResultState.Success)?.data ?: return null
        val prevList = (prevResult as? ResultState.Success)?.data ?: return null
        val sorted = (currentList + prevList)
            .sortedByDescending { it.basicData.timeStartWork ?: 0L }
            .toMutableList()
        val existingIndex = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (existingIndex != -1) {
            sorted[existingIndex] = route
        } else {
            val insertIndex = sorted.indexOfFirst {
                (it.basicData.timeStartWork ?: 0L) < (route.basicData.timeStartWork ?: 0L)
            }
            if (insertIndex == -1) sorted.add(route) else sorted.add(insertIndex, route)
        }
        val index = sorted.indexOfFirst { it.basicData.id == route.basicData.id }
        if (index == -1) return null
        val chain = mutableListOf(sorted[index])
        var nextIdx = index + 1
        while (nextIdx < sorted.size && sorted[nextIdx].basicData.restPointOfTurnover) {
            chain.add(sorted[nextIdx])
            nextIdx++
        }
        if (chain.any { it.basicData.timeStartWork == null || it.basicData.timeEndWork == null }) return null
        val sumWork = chain.sumOf { (it.basicData.timeEndWork ?: 0L) - (it.basicData.timeStartWork ?: 0L) }
        var sumRest = 0L
        for (i in 1 until chain.size) {
            sumRest += (chain[i - 1].basicData.timeStartWork ?: 0L) - (chain[i].basicData.timeEndWork ?: 0L)
        }
        val rawDuration = (sumWork.toDouble() * 2.6).toLong()
        val duration = maxOf(rawDuration - sumRest, us.minTimeHomeRest)
        return (routeEnd + duration) to (routeEnd + us.minTimeHomeRest)
    }

    private fun publishLive() {
        val us = userSettings ?: run {
            _live.value = HomeIosLive()
            return
        }
        val now = nowMs()
        val displayTz = displayTz(us)
        val all = allRoutes

        val current = all.findCurrentRoute(now, us)
        if (current != null) {
            val start = current.basicData.timeStartWork ?: now
            val workMs = (now - start).coerceAtLeast(0L)
            val hours = workMs / 3_600_000f
            _live.value = HomeIosLive(
                kind = "current",
                basicId = current.basicData.id,
                workText = fmtDuration(workMs, false),
                workProgress = (hours / 12f).coerceIn(0f, 1f),
                isOver12 = hours > 12f,
                loco = locoTile(current),
                train = trainTile(current),
                passenger = passengerTile(current),
                defaultOrder = defaultTileOrder(current),
            )
            return
        }

        val rest = restParams?.takeIf { now < it.boundary }
        // Приоритет: Отдых в ПО > Следующий маршрут > Домашний отдых (§4.3).
        if (rest != null && rest.isTurnover) {
            _live.value = HomeIosLive(kind = "restTurnover", rest = restUi(rest, now, displayTz))
            return
        }
        val next = all.findNextFutureRoute(now)
        val nextStart = next?.basicData?.timeStartWork
        if (next != null && nextStart != null) {
            _live.value = HomeIosLive(
                kind = "next",
                basicId = next.basicData.id,
                countdownText = fmtDuration((nextStart - now).coerceAtLeast(0L), false),
                appearanceText = fmtDateMiniTime(nextStart, displayTz),
            )
            return
        }
        if (rest != null) {
            _live.value = HomeIosLive(kind = "restHome", rest = restUi(rest, now, displayTz))
            return
        }
        _live.value = HomeIosLive()
    }

    private fun restUi(p: RestParams, now: Long, tz: TimeZone): HomeIosRest {
        val total = (p.fullEnd - p.restStart).toFloat().coerceAtLeast(1f)
        val progress = ((now - p.restStart) / total).coerceIn(0f, 1f)
        val marker = if (p.isTurnover) p.shortEnd else p.minEnd
        val markerFraction = marker?.let { ((it - p.restStart) / total).coerceIn(0f, 1f) } ?: -1f
        val rows = mutableListOf<HomeIosRestRow>()
        if (p.isTurnover) {
            p.shortEnd?.let { rows += restRow("Короткий отдых", it, now, tz) }
        } else {
            p.minEnd?.let { rows += restRow("Минимальный отдых", it, now, tz) }
        }
        rows += restRow("Полный отдых", p.fullEnd, now, tz)
        return HomeIosRest(
            title = if (p.isTurnover) "ОТДЫХ В ПУНКТЕ ОБОРОТА" else "ДОМАШНИЙ ОТДЫХ",
            elapsedText = fmtDuration((now - p.restStart).coerceAtLeast(0L), false),
            startedText = "начало отдыха ${fmtDateMiniTime(p.restStart, tz)}",
            progress = progress,
            markerFraction = markerFraction,
            markerIsSuccess = p.isTurnover,
            startLabel = fmtTime(p.restStart, tz),
            markerLabel = if (p.isTurnover) p.shortEnd?.let { fmtTime(it, tz) } ?: "" else "",
            endLabel = fmtTime(p.fullEnd, tz),
            rows = rows,
        )
    }

    private fun restRow(title: String, until: Long, now: Long, tz: TimeZone) = HomeIosRestRow(
        title = title,
        untilText = "до ${fmtDateMiniTime(until, tz)}",
        leftText = fmtDuration((until - now).coerceAtLeast(0L), false),
    )

    private fun defaultTileOrder(route: Route): List<String> {
        val units = listOf("loco", "train", "passenger").sortedBy { type ->
            val isEmpty = when (type) {
                "loco" -> route.locomotives.isEmpty()
                "train" -> route.trains.isEmpty()
                else -> route.passengers.isEmpty()
            }
            if (isEmpty) 1 else 0
        }
        return listOf("work") + units
    }

    private fun locoTile(route: Route): HomeIosUnitTile {
        val locos = route.locomotives
        return HomeIosUnitTile(
            type = "loco",
            count = locos.size,
            title = locos.lastOrNull()?.let { locomotiveName(it, locos.size) },
            subtitle = null,
            items = locos.mapIndexed { i, l -> HomeIosUnitItem(l.locoId, locomotiveName(l, i + 1)) },
        )
    }

    private fun trainTile(route: Route): HomeIosUnitTile {
        val trains = route.trains
        fun parts(t: com.z_company.domain.entities.route.Train): Triple<String, String?, String> {
            val first = t.stations.firstOrNull()?.stationName
            val last = if (t.stations.size > 1) t.stations.last().stationName else null
            val title = unitTitle(t.number, t.servicePhase, first, last)
            val sub = unitSubtitle(t.number, t.servicePhase, first, last)
            return Triple(title, sub, if (sub != null) "$title $sub" else title)
        }
        val lastParts = trains.lastOrNull()?.let { parts(it) }
        return HomeIosUnitTile(
            type = "train",
            count = trains.size,
            title = lastParts?.first,
            subtitle = lastParts?.second,
            items = trains.map { HomeIosUnitItem(it.trainId, parts(it).third) },
        )
    }

    private fun passengerTile(route: Route): HomeIosUnitTile {
        val passengers = route.passengers
        fun full(p: com.z_company.domain.entities.route.Passenger): String {
            val title = unitTitle(p.trainNumber, null, p.stationDeparture, p.stationArrival)
            val sub = unitSubtitle(p.trainNumber, null, p.stationDeparture, p.stationArrival)
            return if (sub != null) "$title $sub" else title
        }
        val last = passengers.lastOrNull()
        return HomeIosUnitTile(
            type = "passenger",
            count = passengers.size,
            title = last?.let { unitTitle(it.trainNumber, null, it.stationDeparture, it.stationArrival) },
            subtitle = last?.let { unitSubtitle(it.trainNumber, null, it.stationDeparture, it.stationArrival) },
            items = passengers.map { HomeIosUnitItem(it.passengerId, full(it)) },
        )
    }

    /** `серия-номер` / `серия б/н` / `Тяга номер` / `Тяга N`. */
    private fun locomotiveName(loco: Locomotive, ordinal: Int): String {
        val s = loco.series?.takeIf { it.isNotBlank() }
        val n = loco.number?.takeIf { it.isNotBlank() }
        return when {
            s != null && n != null -> "$s-$n"
            s != null -> "$s б/н"
            n != null -> "${loco.type.text} $n"
            else -> "${loco.type.text} $ordinal"
        }
    }

    private fun stationsLine(first: String?, last: String?): String? {
        val f = first?.takeIf { it.isNotBlank() }
        val l = last?.takeIf { it.isNotBlank() }
        return when {
            f != null && l != null -> "$f — $l"
            f != null -> "$f — "
            l != null -> " — $l"
            else -> null
        }
    }

    private fun unitTitle(number: String?, shoulder: ServicePhase?, first: String?, last: String?): String {
        val num = number?.takeIf { it.isNotBlank() }
        return when {
            num != null -> "№$num"
            shoulder != null -> "${shoulder.departureStation} — ${shoulder.arrivalStation}"
            else -> stationsLine(first, last) ?: "б/н"
        }
    }

    private fun unitSubtitle(number: String?, shoulder: ServicePhase?, first: String?, last: String?): String? {
        if (number.isNullOrBlank()) return null
        return shoulder?.let { "${it.departureStation} — ${it.arrivalStation}" } ?: stationsLine(first, last)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Действия экрана
    // ═══════════════════════════════════════════════════════════════════════

    /** Вызывается при каждом появлении экрана: авторизация, живые блоки, фоновая синхронизация. */
    fun onScreenAppear() {
        refreshAuthorization()
        startTicker()
        syncOnScreenOpen()
    }

    /** Уход с экрана отменяет фоновую синхронизацию (§4.1). */
    fun onScreenDisappear() {
        backgroundSyncJob?.cancel()
        backgroundSyncJob = null
        if (isBackgroundSyncing) {
            isBackgroundSyncing = false
            rebuild()
        }
    }

    /** Стрелки ‹ › и шторка месяца: применить пару (год, месяц) из календаря. */
    fun setCurrentMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val list = runCatching { calendarUseCase.loadFlowMonthOfYearListState().first() }.getOrNull() ?: return@launch
            // BUG-15 (паритет с Android): пары нет в календаре — ничего не происходит.
            val selected = list.find { it.year == year && it.month == month } ?: return@launch
            settingsUseCase.setCurrentMonthOfYear(selected).first { it !is ResultState.Loading }
        }
    }

    fun previousMonth() {
        val us = userSettings ?: return
        val index = monthYearList.indexOfFirst { it.first == us.selectMonthOfYear.year && it.second == us.selectMonthOfYear.month }
        monthYearList.getOrNull(index - 1)?.takeIf { index > 0 }?.let { setCurrentMonth(it.first, it.second) }
    }

    fun nextMonth() {
        val us = userSettings ?: return
        val index = monthYearList.indexOfFirst { it.first == us.selectMonthOfYear.year && it.second == us.selectMonthOfYear.month }
        if (index < 0) return
        monthYearList.getOrNull(index + 1)?.let { setCurrentMonth(it.first, it.second) }
    }

    fun dismissNotice(dismissKey: String) {
        dismissedNotices += dismissKey
        rebuild()
    }

    /** Свайп → «Да, удалить»: soft-delete в корзину. */
    fun removeRoute(basicId: String) {
        viewModelScope.launch {
            val route = monthRoutes?.firstOrNull { it.basicData.id == basicId }
                ?: allRoutes.firstOrNull { it.basicData.id == basicId }
                ?: return@launch
            val result = routeUseCase.markAsRemoved(route).first { it !is ResultState.Loading }
            when (result) {
                is ResultState.Success -> _messages.tryEmit("Маршрут перемещён в корзину")
                is ResultState.Error -> _messages.tryEmit(
                    result.entity.message ?: result.entity.throwable?.message ?: "Ошибка"
                )
                else -> Unit
            }
        }
    }

    /** Текст подтверждения удаления: «от dd.MM HH:mm». */
    fun removeRouteSubtitle(basicId: String): String {
        val us = userSettings ?: return ""
        val route = monthRoutes?.firstOrNull { it.basicData.id == basicId } ?: return ""
        val start = route.basicData.timeStartWork ?: return ""
        return "от ${fmtDateMiniTime(start, displayTz(us))}"
    }

    // ── Синхронизация (§4.1, §4.6) ──────────────────────────────────────────

    private fun hasActiveSubscription(): Boolean =
        (userSettings?.subscriptionPeriod ?: 0L) > nowMs()

    /** Фоновая синхронизация при открытии экрана. */
    private fun syncOnScreenOpen() {
        if (backgroundSyncJob?.isActive == true || manualSyncJob?.isActive == true) return
        if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return
        backgroundSyncJob = viewModelScope.launch {
            delay(SyncManager.BACKGROUND_SYNC_START_DELAY_MILLIS)
            if (syncManager.isSyncInProgress() || !syncManager.shouldRunAutomaticSync()) return@launch
            val subscriptionEnd = settingsUseCase.getUserSettingFlow().first().subscriptionPeriod
            if (subscriptionEnd <= nowMs()) return@launch
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) return@launch
            isBackgroundSyncing = true
            rebuild()
            try {
                var failureMessage: String? = null
                var sessionExpired = false
                var pendingDeletionCount = 0
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> {
                            if (NetworkErrorMapper.isSessionExpiredMessage(state.entity.message)) sessionExpired = true
                            failureMessage = NetworkErrorMapper.syncFailureMessage(state.entity.message, state.entity.throwable)
                        }
                        is ResultState.Success -> pendingDeletionCount = state.data.pendingDeletionRouteIds.size
                        else -> Unit
                    }
                }
                if (sessionExpired) closeSession()
                // BUG-10 (паритет с Android): текст ведёт в «Настройки», хотя корзина — в Профиле.
                val message = failureMessage ?: if (pendingDeletionCount > 0) {
                    "На сервере пропало маршрутов: $pendingDeletionCount. Проверьте корзину в Настройках."
                } else null
                message?.let { _messages.tryEmit(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _messages.tryEmit(NetworkErrorMapper.syncFailureMessage(e.message, e))
            } finally {
                isBackgroundSyncing = false
                rebuild()
            }
        }
    }

    /**
     * Pull-to-refresh (`PullToSyncViewModel.refresh`, без cooldown). Итоговое сообщение
     * передаётся в [onFinished] (показывается snackbar'ом экрана). Повторный жест во
     * время синхронизации игнорируется — [onFinished] получает null.
     */
    fun pullToSync(onFinished: (String?) -> Unit) {
        if (isPullRefreshing) {
            onFinished(null)
            return
        }
        isPullRefreshing = true
        viewModelScope.launch {
            var message: String? = null
            try {
                if (!hasActiveSubscriptionNow()) {
                    message = "Синхронизация доступна по подписке"
                    return@launch
                }
                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) {
                    message = "Необходимо войти в профиль"
                    return@launch
                }
                var completed = false
                var failureMessage: String? = null
                var sessionExpired = false
                var pendingDeletionCount = 0
                syncManager.syncBidirectional("Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Error -> {
                            if (NetworkErrorMapper.isSessionExpiredMessage(state.entity.message)) sessionExpired = true
                            failureMessage = NetworkErrorMapper.syncFailureMessage(state.entity.message, state.entity.throwable)
                        }
                        is ResultState.Success -> {
                            completed = state.data.routesDone
                            pendingDeletionCount = state.data.pendingDeletionRouteIds.size
                        }
                        is ResultState.Loading -> Unit
                    }
                }
                if (sessionExpired) closeSession()
                message = failureMessage ?: if (!completed) {
                    "Синхронизация не выполнена: сервер не завершил обработку данных"
                } else if (pendingDeletionCount > 0) {
                    // BUG-10 (паритет с Android).
                    "На сервере пропало маршрутов: $pendingDeletionCount. Проверьте корзину в Настройках."
                } else {
                    "Синхронизация завершена"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                message = NetworkErrorMapper.syncFailureMessage(e.message, e)
            } finally {
                isPullRefreshing = false
                onFinished(message)
            }
        }
    }

    private suspend fun hasActiveSubscriptionNow(): Boolean =
        settingsUseCase.getUserSettingFlow().first().subscriptionPeriod > nowMs()

    /** «Синхронизировать» в карточке несинхронизированных — выгрузка на сервер (§4.6). */
    fun manualSync() {
        manualSyncJob?.cancel()
        manualSyncJob = viewModelScope.launch {
            if (!hasActiveSubscriptionNow()) {
                _messages.tryEmit("Синхронизация доступна по подписке")
                return@launch
            }
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            val userId = secureTokenStorage.getUserIdFlow().first()
            if (token.isNullOrBlank()) {
                syncSteps = linkedMapOf(
                    STEP_USER to StepState.error("Неавторизованный пользователь"),
                    STEP_SALARY to StepState.error(""),
                    STEP_RELEASE to StepState.error(""),
                    STEP_ROUTES to StepState.error(""),
                )
                publishSyncDialog(isComplete = true)
                return@launch
            }
            syncSteps = linkedMapOf(
                STEP_USER to StepState.loading(),
                STEP_SALARY to StepState.loading(),
                STEP_RELEASE to StepState.loading(),
                STEP_ROUTES to StepState.loading(),
            )
            syncRouteErrors = emptyList()
            syncRoutesTotalAttempted = 0
            syncRoutesSavedCount = 0
            syncReportUserId = userId
            publishSyncDialog()
            var stopped = false
            try {
                syncManager.syncToRemote("Bearer $token").collect { state ->
                    if (stopped) return@collect
                    when (state) {
                        is ResultState.Loading -> Unit
                        is ResultState.Success -> {
                            val r = state.data
                            if (r.userSettingsSaved) syncSteps[STEP_USER] = StepState.success()
                            if (r.salarySettingsSaved) syncSteps[STEP_SALARY] = StepState.success()
                            if (r.releaseDaysSaved) syncSteps[STEP_RELEASE] = StepState.success()
                            if (r.routesSavedCount >= 0) {
                                syncSteps[STEP_ROUTES] = if (r.routeErrors.isNotEmpty()) {
                                    StepState.error("синхронизировано ${r.routesSavedCount} из ${r.routeErrors.size + r.routesSavedCount}")
                                } else StepState.success()
                            }
                            syncRouteErrors = r.routeErrors
                            syncRoutesTotalAttempted = r.routeErrors.size + r.routesSavedCount
                            syncRoutesSavedCount = r.routesSavedCount
                            val isFullSuccess = r.timestamp != null && r.routeErrors.isEmpty()
                            r.timestamp?.let { sharedPrefs.setLastSyncTimestamp(it) }
                            publishSyncDialog(isComplete = !isFullSuccess, isSuccess = isFullSuccess)
                        }
                        is ResultState.Error -> {
                            val msg = state.entity.message ?: ""
                            val clean = cleanSyncError(msg)
                            if (NetworkErrorMapper.isConnectivityMessage(clean)) {
                                stopped = true
                                publishSyncDialog(isComplete = true, isNetworkError = true)
                                return@collect
                            }
                            if (NetworkErrorMapper.isSessionExpiredMessage(clean)) {
                                stopped = true
                                closeSession()
                                publishSyncDialog(isComplete = true, isSessionExpired = true)
                                return@collect
                            }
                            val stepKey = parseSyncStep(msg)
                            if (stepKey != null) {
                                syncSteps[stepKey] = StepState.error(clean)
                                publishSyncDialog()
                            } else {
                                syncSteps.keys.toList().forEach { key ->
                                    if (syncSteps[key]?.isLoading == true) syncSteps[key] = StepState.error(msg)
                                }
                                publishSyncDialog(isComplete = true)
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                syncSteps.keys.toList().forEach { key ->
                    if (syncSteps[key]?.isLoading == true) {
                        syncSteps[key] = StepState.error(e.message ?: "Ошибка синхронизации")
                    }
                }
                publishSyncDialog(isComplete = true)
            }
        }
    }

    /** «Понятно» / закрытие диалога — сброс состояния (сама операция продолжается). */
    fun resetSyncState() {
        manualSyncJob?.cancel()
        syncSteps = LinkedHashMap()
        syncRouteErrors = emptyList()
        syncRoutesTotalAttempted = 0
        syncRoutesSavedCount = 0
        syncReportUserId = null
        _syncDialog.value = HomeIosSyncDialog()
    }

    private fun publishSyncDialog(
        isComplete: Boolean = false,
        isSuccess: Boolean = false,
        isNetworkError: Boolean = false,
        isSessionExpired: Boolean = false,
    ) {
        val total = syncSteps.size.coerceAtLeast(1)
        val completed = syncSteps.values.count { !it.isLoading }
        val progress = completed.toFloat() / total.toFloat()
        val currentStep = syncSteps.entries.firstOrNull { it.value.isLoading }?.key
        val stepTitle = currentStep?.let { stepName(it) }
            ?: if (completed == total) "Завершено" else "Подготовка"
        val firstError = syncSteps.values.firstOrNull { !it.isLoading && it.error != null && it.error.isNotEmpty() }?.error
        val hasErrors = syncSteps.values.any { it.error != null }
        _syncDialog.value = HomeIosSyncDialog(
            // Полный успех: прогресс закрывается, показывается «Выгрузка завершена!».
            isVisible = true,
            stepTitle = stepTitle,
            progress = progress,
            percentText = "${(progress * 100).toInt()}%",
            errorText = firstError,
            isComplete = isComplete,
            isSuccess = isSuccess,
            isNetworkError = isNetworkError,
            isSessionExpired = isSessionExpired,
            canSendReport = isComplete && hasErrors && !isNetworkError && !isSessionExpired,
            reportText = buildReport(),
        )
    }

    private fun buildReport(): String = buildString {
        appendLine("Отчет об ошибках синхронизации")
        appendLine("Тип: Выгрузка на сервер")
        syncReportUserId?.takeIf { it.isNotBlank() }?.let { appendLine("ID пользователя: $it") }
        appendLine("=".repeat(40))
        syncSteps.forEach { (step, state) ->
            val error = state.error ?: return@forEach
            val name = stepName(step)
            if (step == STEP_ROUTES && syncRoutesTotalAttempted > 0) {
                appendLine("$name: синхронизировано $syncRoutesSavedCount из $syncRoutesTotalAttempted")
                syncRouteErrors.forEach { appendLine("  - $it") }
            } else if (error.isNotEmpty()) {
                appendLine("$name: $error")
            }
        }
    }

    private fun stepName(key: String): String = when (key) {
        STEP_USER -> "Настройки пользователя"
        STEP_SALARY -> "Настройки зарплаты"
        STEP_RELEASE -> "Отвлечения"
        STEP_ROUTES -> "Маршруты"
        else -> key
    }

    private fun parseSyncStep(message: String): String? = when {
        message.contains("UserSettings") -> STEP_USER
        message.contains("SalarySetting") -> STEP_SALARY
        message.contains("отвлечений") -> STEP_RELEASE
        else -> null
    }

    private fun cleanSyncError(message: String): String {
        val prefixes = listOf(
            "Ошибка сохранения UserSettings: ",
            "Ошибка сохранения SalarySetting: ",
            "Ошибка сохранения дней отвлечений: ",
        )
        for (prefix in prefixes) {
            if (message.startsWith(prefix)) return message.removePrefix(prefix)
        }
        return message
    }

    /** 401 — сессия закрывается как logOut (§18.5): токен, vk id, курсор дельта-синхронизации. */
    private suspend fun closeSession() {
        runCatching {
            secureTokenStorage.saveAuthToken("")
            secureTokenStorage.saveVkId("")
            sharedPrefs.setRouteSyncCursor(null)
        }
        isAuthorized = false
        rebuild()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // watch* для Swift
    // ═══════════════════════════════════════════════════════════════════════

    fun watchUi(callback: (HomeIosScreenUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    fun watchLive(callback: (HomeIosLive) -> Unit) {
        viewModelScope.launch { _live.collect { callback(it) } }
    }

    fun watchSyncDialog(callback: (HomeIosSyncDialog) -> Unit) {
        viewModelScope.launch { _syncDialog.collect { callback(it) } }
    }

    fun watchMessages(callback: (String) -> Unit) {
        viewModelScope.launch { _messages.collect { callback(it) } }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Вспомогательное
    // ═══════════════════════════════════════════════════════════════════════

    private class Calc<T>(val value: T?, val isError: Boolean)

    private class StepState private constructor(val isLoading: Boolean, val error: String?) {
        companion object {
            fun loading() = StepState(true, null)
            fun success() = StepState(false, null)
            fun error(message: String) = StepState(false, message)
        }
    }

    private class Metrics(
        val totalTimeWithHoliday: Calc<Long> = Calc(null, false),
        val timeWithoutHoliday: Long = 0L,
        val todayWorkTime: Long = 0L,
        val night: Calc<Long> = Calc(null, false),
        val passenger: Calc<Long> = Calc(null, false),
        val single: Calc<Long> = Calc(null, false),
        val extended: Calc<Long> = Calc(null, false),
        val longTrains: Calc<Long> = Calc(null, false),
        val heavy: Calc<Long> = Calc(null, false),
        val toBeCredited: Calc<Double> = Calc(null, false),
    )

    private class RestParams(
        val isTurnover: Boolean,
        val restStart: Long,
        val shortEnd: Long?,
        val fullEnd: Long,
        val minEnd: Long?,
        val boundary: Long,
    )

    private fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

    private fun displayTz(us: UserSettings): TimeZone =
        runCatching { TimeZone.of(us.displayTimeZone()) }.getOrElse { TimeZone.of("GMT+3") }

    private fun ratio(value: Long, base: Long): Float =
        (value.toFloat() / base.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    private fun localDate(ms: Long, tz: TimeZone) =
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).date

    private fun two(n: Int): String = if (n < 10) "0$n" else n.toString()

    /** `dd.MM HH:mm`. */
    private fun fmtDateMiniTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)} ${two(t.hour)}:${two(t.minute)}"
    }

    /** `HH:mm`. */
    private fun fmtTime(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.hour)}:${two(t.minute)}"
    }

    /** `dd.MM.yy`. */
    private fun fmtDate(ms: Long, tz: TimeZone): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz)
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${two(t.year % 100)}"
    }

    /**
     * Длительность как `ConverterLongToTime` (§0.1): `ЧЧ:ММ` (часы не ограничены 24,
     * отрицательное → `00:00`, null → пробелы) или десятичные часы `Ч,ММ`.
     */
    private fun fmtDuration(ms: Long?, isDecimal: Boolean): String {
        if (ms == null) return if (isDecimal) "" else "          "
        if (isDecimal) {
            val totalMinutes = ms / 60_000L
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            // "%.2f" от minutes/60 с округлением половины вверх.
            val hundredths = ((minutes * 100 + 30) / 60).toInt()
            return "$hours,${two(hundredths)}"
        }
        if (ms < 0) return "00:00"
        val totalMinutes = ms / 60_000L
        val hours = totalMinutes / 60
        val minutes = (totalMinutes % 60).toInt()
        val hourText = if (hours < 10) "0$hours" else hours.toString()
        return "$hourText:${two(minutes)}"
    }

    private fun daysWord(n: Long): String {
        val nn = n % 100
        val n1 = n % 10
        return when {
            nn in 11..14 -> "дней"
            n1 == 1L -> "день"
            n1 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24L * HOUR
        const val SUBSCRIPTION_EXPIRING_SOON_DAYS = 7L
        const val FREE_TRIAL_RUNNING_LOW = 5
        const val STEP_USER = "UserSettings"
        const val STEP_SALARY = "SalarySettings"
        const val STEP_RELEASE = "ReleaseDays"
        const val STEP_ROUTES = "Routes"
        val MONTH_NAMES = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
        )
    }
}
