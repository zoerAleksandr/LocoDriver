package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.domain.entities.MonthOfYear
import java.util.Calendar
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.filterByConsiderFutureRoute
import com.z_company.domain.entities.route.UtilsForEntities.getNightTime
import com.z_company.domain.entities.route.UtilsForEntities.getPassengerTime
import com.z_company.domain.entities.route.UtilsForEntities.getTravelTime
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.util.currencySymbol
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * ViewModel экрана «Статистика». Считает реальные метрики из маршрутов,
 * переиспользуя доменные утилиты (UtilsForEntities) и SalaryCalculationHelper —
 * те же расчёты, что и на Главном.
 */
@OptIn(kotlin.time.ExperimentalTime::class, FlowPreview::class)
class StatisticsViewModel : ViewModel(), KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val salarySettingUseCase: SalarySettingUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val normaUseCase: com.z_company.domain.use_cases.NormaUseCase by inject()

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    private var allRoutes: List<Route> = emptyList()
    private var settings: UserSettings? = null

    // Символ валюты по стране из настроек (₽ / ₸ / Br) для денежных метрик статистики.
    private fun moneyCurrency(): String = currencySymbol(settings?.country)
    private var salarySetting: SalarySetting? = null
    private var calendarMonths: List<MonthOfYear> = emptyList()

    private var tab: StatTab = StatTab.MONTH
    private var selYear: Int = 0
    private var selMonth: Int = 0            // 0-based
    private var compareId: String = "prev"   // prev | yearago | none

    private var recomputeJob: Job? = null

    // Тяжёлые расчёты (зарплата/ночные по многим месяцам) — на диспетчере с
    // ограничением в 1 поток. Это гарантирует, что Статистика не займёт все ядра
    // CPU и не задушит другие экраны (симптом был: Календарь висел на лоадере).
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val computeDispatcher = Dispatchers.Default.limitedParallelism(1)

    override fun onCleared() {
        super.onCleared() // отменяет viewModelScope (и все расчёты)
        // Освобождаем крупные структуры и результаты детализации, чтобы мусор от
        // тяжёлого расчёта зарплаты собрался при уходе с экрана, а не тормозил
        // следующий экран (например Календарь).
        recomputeJob?.cancel()
        recomputeJob = null
        allRoutes = emptyList()
        calendarMonths = emptyList()
        detailSeries = emptyList()
        detailState = null
        _uiState.value = StatisticsUiState()
    }

    init {
        viewModelScope.launch {
            combine(
                routeUseCase.getListRoutesAsFlow(),
                settingsUseCase.getUserSettingFlow(),
                calendarUseCase.loadFlowMonthOfYearListState(),
            ) { routes, userSettings, months -> Triple(routes, userSettings, months) }
                // Три источника отдают начальные значения не одновременно → combine
                // эмитит несколько раз подряд. debounce схлопывает эту пачку в один
                // пересчёт (иначе тяжёлый расчёт запускался 3+ раза при открытии).
                .debounce(150)
                .collect { (routes, userSettings, months) ->
                    settings = userSettings
                    // Уважаем настройку «Учитывать будущие маршруты» на уровне
                    // источника: тогда и расчёты, и диапазоны/пикеры периодов
                    // согласованы — при выключенной настройке будущие (ещё не
                    // начавшиеся) маршруты не участвуют нигде.
                    allRoutes = routes
                        .filter { it.basicData.timeStartWork != null }
                        .filterByConsiderFutureRoute(
                            userSettings.isConsiderFutureRoute,
                            System.currentTimeMillis(),
                        )
                    salarySetting = salarySettingUseCase.getSalarySetting()
                    calendarMonths = months
                    historyYearRaws = null // данные изменились — пересчитаем историю заново
                    if (selYear == 0) {
                        val sel = userSettings.selectMonthOfYear
                        selYear = sel.year
                        selMonth = sel.month
                    }
                    recompute()
                }
        }
    }

    fun setTab(newTab: StatTab) {
        if (tab == newTab) return
        tab = newTab
        // Мгновенно переключаем вкладку и показываем лоадер — не ждём тяжёлого
        // пересчёта. Данные предыдущей вкладки очищаем, чтобы экран показал загрузку.
        _uiState.update {
            it.copy(
                tab = newTab,
                loading = true,
                empty = false,
                metrics = emptyList(),
                topDirections = emptyList(),
                yearBars = emptyList(),
                historyRows = emptyList(),
            )
        }
        recompute()
    }

    fun prevPeriod() {
        when (tab) {
            StatTab.YEAR -> selYear -= 1
            else -> { if (selMonth == 0) { selMonth = 11; selYear -= 1 } else selMonth -= 1 }
        }
        recompute()
    }

    fun nextPeriod() {
        when (tab) {
            StatTab.YEAR -> selYear += 1
            else -> { if (selMonth == 11) { selMonth = 0; selYear += 1 } else selMonth += 1 }
        }
        recompute()
    }

    fun setCompare(id: String) {
        compareId = id
        recompute()
    }

    fun setCompareCustomMonth(year: Int, month0: Int) {
        compareId = "custom"
        customMonth = year to month0
        recompute()
    }

    fun setCompareCustomYear(year: Int) {
        compareId = "custom"
        customYear = year
        recompute()
    }

    private var customMonth: Pair<Int, Int>? = null
    private var customYear: Int? = null

    // ── Детализация метрики (тап по плашке) ──────────────────────────
    private data class DetailPoint(
        val bar: Float, val valueStr: String, val unit: String, val short: String, val full: String,
        val breakdown: List<StatDonutSeg>,
    )

    private var detailState: StatDetailState? = null
    private var detailKey: String? = null
    private var detailSeries: List<DetailPoint> = emptyList()

    fun openDetail(key: String) {
        if (settings == null) return
        detailKey = key
        viewModelScope.launch(computeDispatcher) {
            // Окно из 12 месяцев. Для «Года» — Янв..Дек выбранного года.
            // Для «Месяца» — центрируем выбранный месяц: показываем и более поздние
            // месяцы (для сравнения), но не дальше последнего месяца с данными и не
            // сдвигая выбранный левее середины (индекс 6). Если позже данных нет —
            // выбранный остаётся крайним справа.
            val end: Int          // year*12+month самого правого столбца
            val focusIndex: Int   // позиция выбранного месяца в окне [0..11]
            if (tab == StatTab.YEAR) {
                end = selYear * 12 + 11
                focusIndex = 11
            } else {
                val focus = selYear * 12 + selMonth
                val latest = latestFilledYM() ?: focus
                end = maxOf(focus, minOf(latest, focus + 5))
                focusIndex = (focus - (end - 11)).coerceIn(0, 11)
            }
            val series = (0..11).map { i ->
                val ym = end - 11 + i
                val y = Math.floorDiv(ym, 12)
                val m = Math.floorMod(ym, 12)
                val (bar, v, u) = detailValue(y, m, key)
                val point = DetailPoint(bar, v, u, MONTH_SHORT[m], "${MONTH_FULL[m]} $y", detailBreakdown(y, m, key))
                yield()
                point
            }
            detailSeries = series
            applyDetail(focusIndex)
        }
    }

    /** Последний месяц (year*12+month), в котором есть маршруты. */
    private fun latestFilledYM(): Int? = availableMonthsYm().maxOrNull()

    /** Множество месяцев (year*12+month), в которых есть маршруты. Учитываем и месяц
     *  начала, и месяц конца (переходные маршруты принадлежат обоим — как в routesOfMonth). */
    private fun availableMonthsYm(): Set<Int> {
        val tz = TimeCalculationContext.from(settings!!).crossMonthTZ
        val set = HashSet<Int>()
        allRoutes.forEach { r ->
            val start = r.basicData.timeStartWork ?: return@forEach
            val s = Instant.fromEpochMilliseconds(start).toLocalDateTime(tz)
            set.add(s.year * 12 + (s.monthNumber - 1))
            r.basicData.timeEndWork?.let { end ->
                val e = Instant.fromEpochMilliseconds(end).toLocalDateTime(tz)
                set.add(e.year * 12 + (e.monthNumber - 1))
            }
        }
        return set
    }

    fun selectDetailMonth(index: Int) {
        if (index in detailSeries.indices) applyDetail(index)
    }

    fun closeDetail() {
        detailState = null
        detailKey = null
        detailSeries = emptyList()
        _uiState.update { it.copy(detail = null) }
    }

    private fun applyDetail(index: Int) {
        val key = detailKey ?: return
        val series = detailSeries
        if (index !in series.indices) return
        val p = series[index]
        val prevPoint = if (index > 0) series[index - 1] else null
        // Сравнение показываем только когда у обоих месяцев есть данные («—» = нет данных).
        val bothHaveData = prevPoint != null && p.valueStr != "—" && prevPoint.valueStr != "—"
        val delta = if (bothHaveData) infoDelta(p.bar.toDouble(), prevPoint!!.bar.toDouble()) else null
        val prevCaption = if (bothHaveData) "${prevPoint!!.short} ${prevPoint.valueStr}" else ""
        detailState = StatDetailState(
            key = key,
            title = METRIC_SHORT[key] ?: "Метрика",
            caption = "${METRIC_CAPTION[key] ?: ""} · ${p.full.uppercase()}",
            heroValue = p.valueStr,
            heroUnit = p.unit,
            heroDelta = delta,
            heroPrevCaption = prevCaption,
            bars = series.map { StatDetailBar(it.short, it.bar, it.full) },
            selectedIndex = index,
            breakdown = p.breakdown,
            breakdownCenter = p.valueStr,
            breakdownCenterSub = p.unit,
        )
        _uiState.update { it.copy(detail = detailState) }
    }

    // Разбивка метрики за месяц по направлениям (плечам) — для метрик на основе поездов.
    private fun detailBreakdown(year: Int, month0: Int, key: String): List<StatDonutSeg> {
        // Разбивка по направлениям — метрика по поездам, значит по месяцу явки.
        val trains = trainRoutesOfMonth(year, month0).flatMap { it.trains }
        val valueOf: (com.z_company.domain.entities.route.Train) -> Double = when (key) {
            "distance" -> { t -> t.distance?.toDoubleOrNull() ?: 0.0 }
            "tkm" -> { t -> (t.distance?.toDoubleOrNull() ?: 0.0) * (t.weight?.toDoubleOrNull() ?: 0.0) }
            "transit" -> { t -> (t.getTravelTime() ?: 0L).toDouble() }
            else -> return emptyList()
        }
        val sub: (Double) -> String = when (key) {
            "distance" -> { v -> val (s, u) = StatFormat.distance(v); "$s $u" }
            "tkm" -> { v -> "${StatFormat.dec(v / 1_000_000, 2)} млн ткм" }
            else -> { v -> "${StatFormat.hm(v.toLong())} ч" }
        }
        val agg = LinkedHashMap<String, Double>()
        trains.forEach { t ->
            val first = t.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
            val last = t.stations.lastOrNull { it.stationName?.isNotBlank() == true }?.stationName
            if (first != null && last != null && t.stations.size > 1) {
                val v = valueOf(t)
                if (v > 0.0) {
                    val dir = "$first → $last"
                    agg[dir] = (agg[dir] ?: 0.0) + v
                }
            }
        }
        if (agg.isEmpty()) return emptyList()
        val sorted = agg.entries.sortedByDescending { it.value }
        val top = sorted.take(4)
        val restSum = sorted.drop(4).sumOf { it.value }
        val segs = top.map { StatDonutSeg(it.key, it.value.toFloat(), sub(it.value)) }.toMutableList()
        if (restSum > 0.0) segs.add(StatDonutSeg("Прочие", restSum.toFloat(), sub(restSum)))
        return segs
    }

    // Значение метрики за конкретный месяц: (столбец, строка, единица).
    private suspend fun detailValue(year: Int, month0: Int, key: String): Triple<Float, String, String> {
        val s = settings!!
        val month = monthOfYearFor(year, month0)
        val ctx = TimeCalculationContext.from(s)
        val routes = routesOfMonth(year, month0)
        // Метрики по поездам — по месяцу явки (переходные не задваиваем); время/зарплата
        // и счётчик маршрутов — по членству (routes).
        val trains = trainRoutesOfMonth(year, month0).flatMap { it.trains }
        return when (key) {
            "worked" -> routes.getWorkTime(month, ctx).let { Triple(it / 3_600_000f, StatFormat.hm(it), "") }
            "overtime" ->
                // Переработка = отработано − норма. Считаем только за месяцы, где есть
                // маршруты; без работы переработки нет (иначе пустой месяц показывал бы
                // −норму — «столбец в норму»).
                if (routes.isEmpty()) {
                    Triple(0f, "—", "")
                } else {
                    (routes.getWorkTime(month, ctx) - month.getPersonalNormaHours() * 3_600_000L)
                        .let { Triple(kotlin.math.abs(it / 3_600_000f), StatFormat.hm(it, signed = true), "") }
                }
            "night" -> (try { routes.getNightTime(s) } catch (_: Exception) { 0L })
                .let { Triple(it / 3_600_000f, StatFormat.hm(it), "") }
            "transit" -> trains.sumOf { it.getTravelTime() ?: 0L }
                .let { Triple(it / 3_600_000f, StatFormat.hm(it), "") }
            "distance" -> trains.sumOf { it.distance?.toDoubleOrNull() ?: 0.0 }
                .let { km -> val (v, u) = StatFormat.distance(km); Triple(km.toFloat(), v, u) }
            "tkm" -> trains.sumOf { (it.distance?.toDoubleOrNull() ?: 0.0) * (it.weight?.toDoubleOrNull() ?: 0.0) }
                .let { t -> Triple(t.toFloat(), StatFormat.dec(t / 1_000_000, 2), "млн ткм") }
            "speed" -> {
                val dist = trains.sumOf { it.distance?.toDoubleOrNull() ?: 0.0 }
                val tr = trains.sumOf { it.getTravelTime() ?: 0L }
                val sp = if (tr > 0) dist / (tr / 3_600_000.0) else 0.0
                Triple(sp.toFloat(), StatFormat.dec(sp, 1), "км/ч")
            }
            "earnings" -> {
                val effectiveNorma = effectiveNormaForUnderwork(month)
                // Без маршрутов доход есть только если положена оплата недоработки.
                val rub = if (routes.isEmpty() && (effectiveNorma <= 0 || salarySetting!!.averagePaymentHour <= 0.0)) {
                    0.0
                } else try {
                    SalaryCalculationHelper(
                        s.copy(selectMonthOfYear = month), salarySetting!!, routes, effectiveNorma
                    ).getMoneyToBeCredited().first()
                } catch (_: Exception) { 0.0 }
                val (v, u) = StatFormat.money(rub, moneyCurrency()); Triple(rub.toFloat(), v, u)
            }
            else -> routes.size.let { Triple(it.toFloat(), StatFormat.int(it), "") }
        }
    }

    // ── Пересчёт ─────────────────────────────────────────────────────
    private fun recompute() {
        val s = settings ?: return
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch(computeDispatcher) {
            // Показан ли сейчас полноэкранный лоадер (данные вкладки пусты) — только
            // тогда держим минимальную длительность, чтобы текст успел прочитаться.
            val loaderVisible = _uiState.value.let { st ->
                when (tab) {
                    StatTab.HISTORY -> st.historyRows.isEmpty()
                    else -> st.metrics.isEmpty()
                }
            }
            val startMs = System.currentTimeMillis()
            _uiState.update { it.copy(loading = true) }
            val state = when (tab) {
                StatTab.MONTH -> buildMonthState(s)
                StatTab.YEAR -> buildYearState(s)
                StatTab.HISTORY -> buildHistoryState()
            }
            if (loaderVisible) {
                val elapsed = System.currentTimeMillis() - startMs
                if (elapsed < MIN_LOADER_MS) delay(MIN_LOADER_MS - elapsed)
            }
            _uiState.update { state.copy(detail = detailState) }
        }
    }

    // ── Границы месяца (мск/крос-месячная TZ) + membership-фильтр ─────
    private fun monthBounds(year: Int, month0: Int): Pair<Long, Long> {
        val tz = TimeCalculationContext.from(settings!!).crossMonthTZ
        val startDate = LocalDate(year, month0 + 1, 1)
        val start = startDate.atStartOfDayIn(tz).toEpochMilliseconds()
        val maxDay = startDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
        val end = LocalDateTime(year, month0 + 1, maxDay, 23, 59, 0, 0)
            .toInstant(tz).toEpochMilliseconds()
        return start to end
    }

    // Маршруты месяца. Будущие маршруты уже отфильтрованы в источнике (allRoutes)
    // согласно настройке «Учитывать будущие маршруты».
    private fun routesOfMonth(year: Int, month0: Int): List<Route> {
        val (start, end) = monthBounds(year, month0)
        return allRoutes.filter { r ->
            val st = r.basicData.timeStartWork ?: return@filter false
            val en = r.basicData.timeEndWork
            if (en == null) {
                // Открытый маршрут (сдача не проставлена) принадлежит ТОЛЬКО месяцу
                // своей явки. Иначе «висящий» маршрут из прошлого месяца утекал бы во
                // все последующие месяцы и завышал бы расстояние/скорость/грузооборот.
                // На Главном/ЗП такого не бывает: там месяц грузится SQL-запросом
                // getByPeriod, который открытые маршруты берёт только по timeStartWork
                // внутри окна месяца; здесь фильтруем весь список в памяти сами.
                st in start..end
            } else {
                // Завершённый маршрут — по пересечению с месяцем (переходные попадают
                // и в месяц явки, и в месяц сдачи, как в routeListByMonthFlow).
                st < end && en >= start
            }
        }
    }

    // Маршруты, чей ПРОБЕГ относится к этому месяцу — по месяцу явки (timeStartWork).
    // Переходный маршрут (явка и сдача в разных месяцах) для метрик по поездам
    // (расстояние/грузооборот/время в пути/скорость/топ направлений) учитывается
    // ТОЛЬКО в месяце явки: неделимый пробег поезда нельзя резать по полуночи, а
    // учтённый целиком в оба месяца он задваивал бы годовой/исторический итог.
    // Метрики по времени (отработанное/ночные/пассажиром) и счётчик «смен», наоборот,
    // остаются по членству (routesOfMonth) — время клипается, смена как на Главном.
    private fun trainRoutesOfMonth(year: Int, month0: Int): List<Route> {
        val (start, end) = monthBounds(year, month0)
        return routesOfMonth(year, month0).filter {
            (it.basicData.timeStartWork ?: return@filter false) in start..end
        }
    }

    private fun monthOfYearFor(year: Int, month0: Int): MonthOfYear =
        calendarMonths.find { it.year == year && it.month == month0 }
            ?: MonthOfYear(year = year, month = month0)

    // Сырые метрики за календарный месяц (как на Главном: клип к месяцу).
    private data class Raw(
        val workedMs: Long, val normaHours: Int, val earnings: Double,
        val nightMs: Long, val passengerMs: Long, val routes: Int,
        val distanceKm: Double, val tkm: Double, val transitMs: Long, val speed: Double,
    )

    private suspend fun rawMonth(year: Int, month0: Int): Raw {
        val s = settings!!
        val month = monthOfYearFor(year, month0)
        val norma = month.getPersonalNormaHours()
        // Норма для оплаты недоработки — та же логика, что на экране ЗП.
        val effectiveNorma = effectiveNormaForUnderwork(month)
        val routes = routesOfMonth(year, month0)
        // Нет маршрутов — не запускаем тяжёлый расчёт зарплаты/ночных/пассажира.
        // Норма берётся из календаря и нужна для агрегатов (переработка за год).
        if (routes.isEmpty()) {
            // Единственная возможная выплата без маршрутов — оплата недоработки.
            val earnings = if (effectiveNorma > 0 && salarySetting!!.averagePaymentHour > 0.0) {
                try {
                    SalaryCalculationHelper(
                        userSettings = s.copy(selectMonthOfYear = month),
                        salarySetting = salarySetting!!,
                        allRoutes = emptyList(),
                        effectiveNormaHoursForUnderwork = effectiveNorma,
                    ).getMoneyToBeCredited().first()
                } catch (_: Exception) { 0.0 }
            } else 0.0
            return Raw(0L, norma, earnings, 0L, 0L, 0, 0.0, 0.0, 0L, 0.0)
        }
        val ctx = TimeCalculationContext.from(s)
        val worked = routes.getWorkTime(month, ctx)
        val night = try { routes.getNightTime(s) } catch (_: Exception) { 0L }
        val passenger = routes.getPassengerTime(month, ctx)
        // Пробег/грузооборот/время в пути — по месяцу явки (переходные не задваиваем).
        val trains = trainRoutesOfMonth(year, month0).flatMap { it.trains }
        val distance = trains.sumOf { it.distance?.toDoubleOrNull() ?: 0.0 }
        val tkm = trains.sumOf { (it.distance?.toDoubleOrNull() ?: 0.0) * (it.weight?.toDoubleOrNull() ?: 0.0) }
        val transit = trains.sumOf { it.getTravelTime() ?: 0L }
        val speed = if (transit > 0) distance / (transit / 3_600_000.0) else 0.0
        val earnings = try {
            SalaryCalculationHelper(
                userSettings = s.copy(selectMonthOfYear = month),
                salarySetting = salarySetting!!,
                allRoutes = routes,
                effectiveNormaHoursForUnderwork = effectiveNorma,
            ).getMoneyToBeCredited().first()
        } catch (_: Exception) { 0.0 }
        return Raw(worked, norma, earnings, night, passenger,
            routes.size, distance, tkm, transit, speed)
    }

    // Норма для расчёта недоработки: текущий месяц → на сегодня; завершённый →
    // полная; будущий → 0. Такая же, как на экране ЗП (единый источник — NormaUseCase).
    private suspend fun effectiveNormaForUnderwork(month: MonthOfYear): Int {
        val calendar = Calendar.getInstance()
        val nowYm = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
        val selectedYm = month.year * 12 + month.month
        return when {
            selectedYm > nowYm -> 0
            selectedYm < nowYm -> normaUseCase.normaHoursFlow(month.year, month.month).first()
            else -> normaUseCase.normaHoursToDateFlow(
                month.year, month.month, calendar.get(Calendar.DAY_OF_MONTH)
            ).first()
        }
    }

    private suspend fun rawYear(year: Int): Raw {
        var worked = 0L; var norma = 0; var earnings = 0.0; var night = 0L; var passenger = 0L
        var routes = 0; var distance = 0.0; var tkm = 0.0; var transit = 0L
        for (m in 0..11) {
            val r = rawMonth(year, m)
            worked += r.workedMs; norma += r.normaHours; earnings += r.earnings
            night += r.nightMs; passenger += r.passengerMs; routes += r.routes
            distance += r.distanceKm; tkm += r.tkm; transit += r.transitMs
            yield() // не монополизируем CPU — даём другим корутинам (экраны) работать
        }
        val speed = if (transit > 0) distance / (transit / 3_600_000.0) else 0.0
        return Raw(worked, norma, earnings, night, passenger, routes, distance, tkm, transit, speed)
    }

    // ── Сборка состояния «Месяц» ─────────────────────────────────────
    private suspend fun buildMonthState(s: UserSettings): StatisticsUiState {
        val cur = rawMonth(selYear, selMonth)
        val compare = compareId != "none"
        val baseline: Pair<Int, Int>? = when (compareId) {
            "yearago" -> (selYear - 1) to selMonth
            "custom" -> customMonth
            "none" -> null
            else -> if (selMonth == 0) (selYear - 1) to 11 else selYear to (selMonth - 1)
        }
        // Если у эталонного периода нет данных — не показываем дельты «+100% было 0».
        val prev = (if (compare && baseline != null) rawMonth(baseline.first, baseline.second) else null)
            ?.takeIf { it.routes > 0 }
        val compareNoData = compare && baseline != null && prev == null

        val metrics = buildMetrics(cur, prev, compare, MONTH_ORDER)
        val (minYM, maxYM) = dataRangeMonths()
        val curYM = selYear * 12 + selMonth
        val label = "${MONTH_FULL[selMonth]} $selYear"
        val avail = availableMonthsYm()
        return StatisticsUiState(
            loading = false,
            tab = StatTab.MONTH,
            empty = cur.routes == 0,
            currency = moneyCurrency(),
            periodLabel = label,
            periodSub = "МЕСЯЦ",
            canGoPrev = curYM > minYM,
            canGoNext = curYM < maxYM,
            compareEnabled = compare,
            compareTitle = when (compareId) {
                "yearago" -> "${MONTH_FULL[selMonth]} ${selYear - 1}"
                "custom" -> customMonth?.let { "${MONTH_FULL[it.second]} ${it.first}" } ?: ""
                else -> baseline?.let { "${MONTH_FULL[it.second]} ${it.first}" } ?: ""
            },
            compareOptions = monthCompareOptions(),
            compareSelectedId = compareId,
            compareNoData = compareNoData,
            // Доступны для сравнения любые месяцы С МАРШРУТАМИ (будущие — только
            // если включена настройка «Учитывать будущие маршруты»), кроме текущего.
            pickYears = avail.map { it / 12 }.distinct().sortedDescending(),
            pickCurYm = curYM,
            availableMonthsYm = avail,
            metrics = metrics,
            wideFirst = true,
            // Топ направлений — по поездам, значит по месяцу явки (переходные не задваиваем).
            topDirections = topDirections(trainRoutesOfMonth(selYear, selMonth)),
        )
    }

    // ── Сборка состояния «Год» ───────────────────────────────────────
    private suspend fun buildYearState(s: UserSettings): StatisticsUiState {
        val cur = rawYear(selYear)
        val compare = compareId != "none"
        val baselineYear: Int? = when (compareId) {
            "custom" -> customYear
            "none" -> null
            else -> selYear - 1
        }
        val prev = (if (compare && baselineYear != null) rawYear(baselineYear) else null)?.takeIf { it.routes > 0 }
        val showPrev = prev != null
        val compareNoData = compare && baselineYear != null && prev == null

        val metrics = buildMetrics(cur, prev, compare, YEAR_ORDER)
        val bars = (0..11).map { m ->
            val c = routesOfMonth(selYear, m).getWorkTime(monthOfYearFor(selYear, m), TimeCalculationContext.from(s))
            val p = if (showPrev && baselineYear != null) routesOfMonth(baselineYear, m).getWorkTime(monthOfYearFor(baselineYear, m), TimeCalculationContext.from(s)) else 0L
            yield()
            StatMonthBar(MONTH_SHORT[m], (c / 3_600_000.0).toFloat(), (p / 3_600_000.0).toFloat())
        }
        val yearRoutes = allRoutes.filter { startYearOf(it) == selYear }
        val (minY, maxY) = dataRangeYears()
        return StatisticsUiState(
            loading = false,
            tab = StatTab.YEAR,
            empty = cur.routes == 0,
            currency = moneyCurrency(),
            periodLabel = "$selYear год",
            periodSub = "ГОД",
            canGoPrev = selYear > minY,
            canGoNext = selYear < maxY,
            compareEnabled = compare,
            compareTitle = when (compareId) {
                "custom" -> customYear?.let { "$it год" } ?: ""
                else -> "${selYear - 1} год"
            },
            compareOptions = yearCompareOptions(),
            compareSelectedId = compareId,
            compareNoData = compareNoData,
            // Годы с маршрутами (будущие — только если включена настройка
            // «Учитывать будущие маршруты»), кроме текущего.
            pickYearOptions = availableMonthsYm().map { it / 12 }.distinct().filter { it != selYear }.sortedDescending(),
            metrics = metrics,
            wideFirst = false,
            yearHeroValue = StatFormat.int((cur.workedMs / 3_600_000L).toInt()),
            yearHeroDelta = if (showPrev) infoDelta((cur.workedMs / 3_600_000.0), (prev!!.workedMs / 3_600_000.0)) else null,
            yearBars = bars,
            topDirections = topDirections(yearRoutes),
        )
    }

    // ── Сборка состояния «История» ───────────────────────────────────
    private var historyYearRaws: Map<Int, Raw>? = null
    private var historySelectedKey = "worked"

    fun selectHistoryMetric(key: String) {
        if (historySelectedKey == key) return
        historySelectedKey = key
        val raws = historyYearRaws
        if (raws != null && raws.isNotEmpty()) {
            _uiState.update { buildHistoryUi(raws.keys.sorted(), raws).copy(detail = detailState) }
        } else recompute()
    }

    private suspend fun buildHistoryState(): StatisticsUiState {
        val years = allRoutes.mapNotNull { startYearOf(it) }.distinct().sorted()
        if (years.isEmpty()) return StatisticsUiState(loading = false, tab = StatTab.HISTORY, empty = true)
        // Полные метрики по каждому году считаем ОДИН раз и кешируем (тяжело из-за
        // зарплаты, но пустые месяцы пропускаются). Переключение метрики — без пересчёта.
        val raws = historyYearRaws ?: years.associateWith { rawYear(it) }.also { historyYearRaws = it }
        return buildHistoryUi(years, raws)
    }

    private fun buildHistoryUi(years: List<Int>, raws: Map<Int, Raw>): StatisticsUiState {
        val nowYear = settings?.selectMonthOfYear?.year ?: (years.maxOrNull() ?: 0)
        val minYear = years.minOrNull()
        val key = historySelectedKey

        val gridMetrics = HISTORY_METRIC_KEYS.map { k ->
            val sum = years.sumOf { historyRawValue(raws.getValue(it), k) }
            val (v, u) = historyFormat(k, sum)
            StatHistoryMetric(k, HISTORY_LABEL.getValue(k), v, u)
        }
        val total = years.sumOf { historyRawValue(raws.getValue(it), key) }
        val (totalStr, totalUnit) = historyFormat(key, total)
        val maxV = years.maxOf { historyRawValue(raws.getValue(it), key) }.coerceAtLeast(1e-9)
        val rows = years.map { y ->
            val value = historyRawValue(raws.getValue(y), key)
            val (vStr, u) = historyFormat(key, value)
            val note = when {
                y == nowYear -> currentMonthNote() ?: if (y == minYear) firstMonthNote(y) else null
                y == minYear -> firstMonthNote(y)
                else -> null
            }
            StatYearRow(y, note, vStr, u, (value / maxV).toFloat().coerceIn(0f, 1f), partial = note != null)
        }
        val caption = HISTORY_CAPTION.getValue(key) + if (totalUnit.isNotEmpty()) " · ${totalUnit.uppercase()}" else ""
        return StatisticsUiState(
            loading = false,
            tab = StatTab.HISTORY,
            empty = false,
            currency = moneyCurrency(),
            historyMetrics = gridMetrics,
            historySelected = key,
            historyTotal = totalStr,
            historyTotalUnit = totalUnit,
            historyTotalCaption = caption,
            historyYearsCount = years.size,
            historyRoutesTotal = years.sumOf { raws.getValue(it).routes },
            historyRows = rows,
        )
    }

    // Значение метрики за год в «естественных» единицах (для итога и полос).
    private fun historyRawValue(raw: Raw, key: String): Double = when (key) {
        "worked" -> raw.workedMs / 3_600_000.0
        "earnings" -> raw.earnings
        "distance" -> raw.distanceKm
        "tkm" -> raw.tkm
        "transit" -> raw.transitMs / 3_600_000.0
        "night" -> raw.nightMs / 3_600_000.0
        else -> raw.routes.toDouble()
    }

    // (значение, единица) для метрики истории.
    private fun historyFormat(key: String, value: Double): Pair<String, String> = when (key) {
        "worked", "night", "transit" -> StatFormat.int(value.roundToInt()) to "ч"
        "earnings" -> StatFormat.money(value, moneyCurrency())
        "distance" -> StatFormat.distance(value)
        "tkm" -> StatFormat.dec(value / 1_000_000, 1) to "млн ткм"
        else -> StatFormat.int(value.roundToInt()) to "смен"
    }

    // ── Построение метрик из сырых значений ──────────────────────────
    private fun buildMetrics(cur: Raw, prev: Raw?, compare: Boolean, order: List<String>): List<StatMetric> {
        fun d(cv: Double, pv: Double?): StatDelta? = if (compare && pv != null) infoDelta(cv, pv) else null
        return order.map { key ->
            when (key) {
                "worked" -> StatMetric("worked", "Отработанное время", StatFormat.hm(cur.workedMs), "",
                    d(cur.workedMs.toDouble(), prev?.workedMs?.toDouble()),
                    prev?.let { StatFormat.hm(it.workedMs) } ?: "",
                    (prev?.workedMs ?: 0L) / 3_600_000f, cur.workedMs / 3_600_000f)
                "overtime" -> {
                    val curOt = cur.workedMs - cur.normaHours * 3_600_000L
                    val prevOt = prev?.let { it.workedMs - it.normaHours * 3_600_000L }
                    // Сравниваем переработку с предыдущим периодом (как остальные метрики),
                    // а не с нормой: если за период-базу нет данных — сравнения нет.
                    StatMetric("overtime", "Переработка", StatFormat.hm(curOt, signed = true), "",
                        d(curOt.toDouble(), prevOt?.toDouble()),
                        prevOt?.let { StatFormat.hm(it, signed = true) } ?: "",
                        abs((prevOt ?: 0L) / 3_600_000f), abs(curOt / 3_600_000f))
                }
                "earnings" -> {
                    val (v, u) = StatFormat.money(cur.earnings, moneyCurrency())
                    StatMetric("earnings", "Заработано", v, u,
                        d(cur.earnings, prev?.earnings),
                        prev?.let { val (pv, pu) = StatFormat.money(it.earnings, moneyCurrency()); "$pv $pu" } ?: "",
                        (prev?.earnings ?: 0.0).toFloat(), cur.earnings.toFloat())
                }
                "speed" -> StatMetric("speed", "Средняя скорость", StatFormat.dec(cur.speed, 1), "км/ч",
                    d(cur.speed, prev?.speed),
                    prev?.let { "${StatFormat.dec(it.speed, 1)} км/ч" } ?: "",
                    (prev?.speed ?: 0.0).toFloat(), cur.speed.toFloat())
                "transit" -> StatMetric("transit", "Время в пути", StatFormat.hm(cur.transitMs), "",
                    d(cur.transitMs.toDouble(), prev?.transitMs?.toDouble()),
                    prev?.let { StatFormat.hm(it.transitMs) } ?: "",
                    (prev?.transitMs ?: 0L) / 3_600_000f, cur.transitMs / 3_600_000f)
                "distance" -> {
                    val (v, u) = StatFormat.distance(cur.distanceKm)
                    StatMetric("distance", "Расстояние", v, u,
                        d(cur.distanceKm, prev?.distanceKm),
                        prev?.let { val (pv, pu) = StatFormat.distance(it.distanceKm); "$pv $pu" } ?: "",
                        (prev?.distanceKm ?: 0.0).toFloat(), cur.distanceKm.toFloat())
                }
                "tkm" -> StatMetric("tkm", "Грузооборот", StatFormat.dec(cur.tkm / 1_000_000, 2), "млн ткм",
                    d(cur.tkm, prev?.tkm),
                    prev?.let { "${StatFormat.dec(it.tkm / 1_000_000, 2)} млн ткм" } ?: "",
                    (prev?.tkm ?: 0.0).toFloat(), cur.tkm.toFloat())
                "night" -> StatMetric("night", "Ночные часы", StatFormat.hm(cur.nightMs), "",
                    d(cur.nightMs.toDouble(), prev?.nightMs?.toDouble()),
                    prev?.let { StatFormat.hm(it.nightMs) } ?: "",
                    (prev?.nightMs ?: 0L) / 3_600_000f, cur.nightMs / 3_600_000f)
                else -> StatMetric("routes", "Маршрутов (смен)", StatFormat.int(cur.routes), "",
                    d(cur.routes.toDouble(), prev?.routes?.toDouble()),
                    prev?.let { StatFormat.int(it.routes) } ?: "",
                    (prev?.routes ?: 0).toFloat(), cur.routes.toFloat())
            }
        }.map { it.copy(hasPrev = prev != null) }
    }

    private fun infoDelta(cur: Double, prev: Double): StatDelta {
        val dir = when {
            abs(cur - prev) < 1e-6 -> "flat"
            cur > prev -> "up"
            else -> "down"
        }
        val pctV = if (prev != 0.0) (cur - prev) / prev * 100 else if (cur == 0.0) 0.0 else 100.0
        return StatDelta("info", dir = dir, pct = pctStr(pctV))
    }

    private fun pctStr(v: Double): String {
        val r = v.roundToInt()
        return (if (r > 0) "+" else if (r < 0) "−" else "") + abs(r) + "%"
    }

    // ── Топ направлений (плечи обслуживания) ─────────────────────────
    private fun topDirections(routes: List<Route>): List<StatTopDirection> {
        data class Agg(var trips: Int = 0, var ms: Long = 0L)
        val map = LinkedHashMap<Pair<String, String>, Agg>()
        routes.forEach { r ->
            r.trains.forEach { t ->
                val first = t.stations.firstOrNull()?.stationName?.takeIf { it.isNotBlank() }
                val last = t.stations.lastOrNull { it.stationName?.isNotBlank() == true }?.stationName
                if (first != null && last != null && t.stations.size > 1) {
                    val key = first to last
                    val agg = map.getOrPut(key) { Agg() }
                    agg.trips += 1
                    agg.ms += t.getTravelTime() ?: 0L
                }
            }
        }
        return map.entries
            .sortedByDescending { it.value.trips }
            .take(4)
            .map { StatTopDirection(it.key.first, it.key.second, it.value.trips, StatFormat.hm(it.value.ms)) }
    }

    // ── Диапазоны данных / служебное ─────────────────────────────────
    private fun startYearOf(r: Route): Int? {
        val ms = r.basicData.timeStartWork ?: return null
        val tz = TimeCalculationContext.from(settings!!).crossMonthTZ
        return Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).year
    }

    private fun dataRangeMonths(): Pair<Int, Int> {
        val tz = TimeCalculationContext.from(settings!!).crossMonthTZ
        val yms = allRoutes.mapNotNull { r ->
            r.basicData.timeStartWork?.let {
                val dt = Instant.fromEpochMilliseconds(it).toLocalDateTime(tz)
                dt.year * 12 + (dt.monthNumber - 1)
            }
        }
        val cur = (settings?.selectMonthOfYear?.let { it.year * 12 + it.month }) ?: (selYear * 12 + selMonth)
        val min = yms.minOrNull() ?: cur
        val max = maxOf(yms.maxOrNull() ?: cur, cur)
        return min to max
    }

    private fun dataRangeYears(): Pair<Int, Int> {
        val years = allRoutes.mapNotNull { startYearOf(it) }
        val cur = settings?.selectMonthOfYear?.year ?: selYear
        return (years.minOrNull() ?: cur) to maxOf(years.maxOrNull() ?: cur, cur)
    }

    private fun monthCompareOptions(): List<StatCompareOption> {
        val prevY = if (selMonth == 0) selYear - 1 else selYear
        val prevM = if (selMonth == 0) 11 else selMonth - 1
        return listOf(
            StatCompareOption("prev", "${MONTH_FULL[prevM]} $prevY", "предыдущий месяц"),
            StatCompareOption("yearago", "${MONTH_FULL[selMonth]} ${selYear - 1}", "этот месяц год назад"),
            StatCompareOption("pick", "Выбрать месяц…", "любой из истории", action = true),
            StatCompareOption("none", "Не сравнивать", "показать только выбранный период", off = true),
        )
    }

    private fun yearCompareOptions(): List<StatCompareOption> = listOf(
        StatCompareOption("prev", "${selYear - 1} год", "предыдущий год"),
        StatCompareOption("pick", "Выбрать год…", "любой из истории", action = true),
        StatCompareOption("none", "Не сравнивать", "показать только выбранный год", off = true),
    )

    private fun firstMonthNote(year: Int): String? {
        val tz = TimeCalculationContext.from(settings!!).crossMonthTZ
        val firstMonth = allRoutes.mapNotNull { r ->
            r.basicData.timeStartWork?.let {
                val dt = Instant.fromEpochMilliseconds(it).toLocalDateTime(tz)
                if (dt.year == year) dt.monthNumber else null
            }
        }.minOrNull() ?: return null
        // «с» требует родительный: «с июля».
        return if (firstMonth > 1) "с ${MONTH_GEN[firstMonth - 1]}" else null
    }

    private fun currentMonthNote(): String? {
        val sel = settings?.selectMonthOfYear ?: return null
        // «по» — именительный, без склонения: «по июль».
        return if (sel.month < 11) "по ${MONTH_FULL[sel.month].lowercase()}" else null
    }

    companion object {
        // Минимальная длительность показа лоадера — чтобы текст успевал прочитаться
        // даже когда расчёт занимает доли секунды.
        private const val MIN_LOADER_MS = 550L
        private val HISTORY_METRIC_KEYS = listOf("worked", "earnings", "distance", "tkm", "transit", "night", "routes")
        private val HISTORY_LABEL = mapOf(
            "worked" to "Отработано", "earnings" to "Заработок", "distance" to "Расстояние",
            "tkm" to "Грузооборот", "transit" to "Время в пути", "night" to "Ночные", "routes" to "Смены",
        )
        private val HISTORY_CAPTION = mapOf(
            "worked" to "ОТРАБОТАНО ВСЕГО", "earnings" to "ЗАРАБОТАНО ВСЕГО", "distance" to "ПРОЙДЕНО ВСЕГО",
            "tkm" to "ГРУЗООБОРОТ ВСЕГО", "transit" to "В ПУТИ ВСЕГО", "night" to "НОЧНЫЕ ВСЕГО", "routes" to "СМЕН ВСЕГО",
        )
        private val MONTH_ORDER = listOf("worked", "overtime", "earnings", "speed", "transit", "distance", "tkm", "night", "routes")
        private val YEAR_ORDER = listOf("overtime", "earnings", "speed", "transit", "distance", "tkm", "night", "routes")
        private val MONTH_FULL = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")
        private val MONTH_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
        private val MONTH_SHORT = listOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
        private val METRIC_SHORT = mapOf(
            "worked" to "Время", "overtime" to "Переработка", "earnings" to "Заработок",
            "speed" to "Скорость", "transit" to "В пути", "distance" to "Путь",
            "tkm" to "Грузооборот", "night" to "Ночные", "routes" to "Маршруты",
        )
        private val METRIC_CAPTION = mapOf(
            "worked" to "ОТРАБОТАНО", "overtime" to "ПЕРЕРАБОТКА", "earnings" to "ЗАРАБОТАНО",
            "speed" to "СРЕДНЯЯ СКОРОСТЬ", "transit" to "ВРЕМЯ В ПУТИ", "distance" to "РАССТОЯНИЕ",
            "tkm" to "ГРУЗООБОРОТ", "night" to "НОЧНЫЕ ЧАСЫ", "routes" to "МАРШРУТОВ",
        )
    }
}
