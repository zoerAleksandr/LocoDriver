package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.ConverterLongToTime
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.calculateWorkTimeWithSettings
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.datetime.LocalDate
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel экрана «Календарь» — объединяет поездки (маршруты) и отвлечения
 * (личные дни: отпуск/больничный/…) в одном месяце.
 *
 * Данные грузятся так же, как в старом «Графике» (WorkScheduleViewModel):
 *   • маршруты месяца → [CalendarUiState.tripsByDay] (по дню явки)
 *   • дни-отвлечения из [MonthOfYear.days] → [CalendarUiState.absenceByDay]
 *   • суммарное отработанное время → [CalendarUiState.totalWorkedText]
 */
class CalendarViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // Режим планирования маршрутов: выбор времени явки + мультивыбор дней.
    private val _routePlan = MutableStateFlow(RoutePlanState())
    val routePlan: StateFlow<RoutePlanState> = _routePlan.asStateFlow()

    private var userSettings: UserSettings? = null
    private var converter: DateAndTimeConverter? = null
    private var currentMonth: MonthOfYear? = null

    fun formatWorkTime(millis: Long?): String =
        if (userSettings?.isDecimalTime == true) ConverterLongToTime.getTimeInStringDecimalFormat(millis)
        else ConverterLongToTime.getTimeInStringFormat(millis)

    /** Удалить маршрут (soft-delete + синхронизация), затем обновить месяц. */
    fun deleteRoute(route: Route) {
        viewModelScope.launch {
            try {
                routeUseCase.markAsRemoved(route)
                    .first { it is ResultState.Success || it is ResultState.Error }
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "deleteRoute")
                snackbarManager.show("Не удалось удалить маршрут")
            }
        }
    }

    /** Перечитать текущий месяц (маршруты + отвлечения). Вызывается при возврате
     *  с дочерних экранов, чтобы новые события отобразились. */
    fun reload() {
        if (currentMonth == null) return
        viewModelScope.launch { runCatching { loadMonth() } }
    }

    fun prepareScreen() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                userSettings = setting
                converter = DateAndTimeConverter(setting)
                currentMonth = setting.selectMonthOfYear
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "prepareScreen")
                snackbarManager.show("Ошибка загрузки: ${t.message ?: t::class.simpleName}")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Переключить месяц на соседний (delta = -1 / +1), если он есть в производственном календаре. */
    fun shiftMonth(delta: Int) {
        val month = currentMonth ?: return
        var y = month.year
        var m = month.month + delta
        if (m < 0) { m = 11; y -= 1 }
        if (m > 11) { m = 0; y += 1 }
        setMonth(y, m)
    }

    private fun setMonth(year: Int, monthIndex: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val list = calendarUseCase.loadFlowMonthOfYearListState().first()
                val found = list.find { it.year == year && it.month == monthIndex }
                if (found == null) {
                    snackbarManager.show("Нет данных календаря за этот месяц")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                settingsUseCase.setCurrentMonthOfYear(found).first()
                currentMonth = found
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "setMonth")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Планирование маршрутов ───────────────────────────────────
    /** Войти в режим планирования: время явки из настроек + пустой выбор дней. */
    fun enterRoutePlan() {
        val times = userSettings?.standardTimesStartWork.orEmpty()
        _routePlan.value = RoutePlanState(
            active = true,
            timeOptions = times.map { TimeOption(it, ConverterLongToTime.getTimeInStringFormat(it).trim()) },
            activeTime = times.firstOrNull(),
            plannedDays = emptySet(),
        )
    }

    fun exitRoutePlan() {
        _routePlan.value = RoutePlanState()
    }

    // Смена активного времени — явка меняется сразу на всех отмеченных днях
    // (они рисуют activeTime), отдельно перезаписывать нечего.
    fun pickPlanTime(millis: Long) = _routePlan.update { it.copy(activeTime = millis) }

    /** Добавить своё время явки в список чипов, выбрать его и сохранить в настройки. */
    fun addCustomPlanTime(hour: Int, minute: Int) {
        val millis = hour * 3_600_000L + minute * 60_000L
        _routePlan.update { s ->
            val opts = if (s.timeOptions.any { it.millis == millis }) s.timeOptions
            else (s.timeOptions + TimeOption(millis, ConverterLongToTime.getTimeInStringFormat(millis).trim()))
                .sortedBy { it.millis }
            s.copy(timeOptions = opts, activeTime = millis)
        }
        viewModelScope.launch {
            runCatching {
                val current = settingsUseCase.getUserSettingFlow().first()
                if (!current.standardTimesStartWork.contains(millis)) {
                    val updated = current.copy(
                        standardTimesStartWork = (current.standardTimesStartWork + millis).sorted()
                    )
                    settingsUseCase.saveSetting(updated).first()
                    userSettings = updated
                }
            }
        }
    }

    /** Тап по дню в режиме планирования: отметить/снять день. */
    fun toggleDayPlan(day: Int) {
        val s = _routePlan.value
        if (s.activeTime == null) return
        // На отвлечение (отпуск/больничный/…) маршрут ставить нельзя. Но «Выходной» —
        // это перенос выходного дня, работа в него допустима (оплата ×2), поэтому явку
        // в такой день разрешаем.
        val absence = _uiState.value.absenceByDay[day]
        if (absence != null && absence.type != ReleaseType.DayOff) return
        val days = s.plannedDays.toMutableSet()
        if (!days.add(day)) days.remove(day)
        _routePlan.value = s.copy(plannedDays = days)
    }

    /** Создать черновики маршрутов по запланированным дням и остаться на календаре. */
    fun createPlannedRoutes() {
        val plan = _routePlan.value
        val m = currentMonth ?: return
        val conv = converter ?: return
        val time = plan.activeTime
        if (plan.plannedDays.isEmpty() || time == null) {
            exitRoutePlan()
            return
        }
        viewModelScope.launch {
            try {
                when (routeHelper.newRouteClick()) {
                    is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                        snackbarManager.show("Создание маршрутов доступно по подписке")
                        return@launch
                    }
                    is RouteActionsHelper.NewRouteResult.Error -> {
                        snackbarManager.show("Не удалось проверить подписку")
                        return@launch
                    }
                    else -> { /* можно создавать */ }
                }
                val hour = ConverterLongToTime.getHour(time)
                val minute = ConverterLongToTime.getRemainingMinuteFromHour(time)
                var created = 0
                for (day in plan.plannedDays.sorted()) {
                    val start = conv.toEpochMillis(m.year, m.month, day, hour, minute)
                    val route = Route(basicData = BasicData(timeStartWork = start))
                    val res = routeUseCase.saveRoute(route)
                        .first { it is ResultState.Success || it is ResultState.Error }
                    if (res is ResultState.Success) created++
                }
                snackbarManager.show("Создано черновиков маршрутов: $created")
                exitRoutePlan()
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "createPlannedRoutes")
                snackbarManager.show("Ошибка создания маршрутов")
            }
        }
    }

    /** Удалить отвлечение только за один день. */
    fun deleteAbsenceDay(day: Int) = deleteAbsenceDates(listOf(day))

    /** Удалить весь период отвлечения (диапазон дней одного типа). */
    fun deleteAbsencePeriod(startDay: Int, endDay: Int) =
        deleteAbsenceDates((minOf(startDay, endDay)..maxOf(startDay, endDay)).toList())

    private fun deleteAbsenceDates(days: List<Int>) {
        val m = currentMonth ?: return
        if (days.isEmpty()) return
        viewModelScope.launch {
            try {
                val dates = days.map { LocalDate(m.year, m.month + 1, it) }
                releaseDayUseCase.deletePeriod(ReleasePeriod(days = dates, type = null)).collect {}
                // Синхронизируем месяц в настройки, чтобы зарплата/норма увидели удаление.
                runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == m.year && it.month == m.month }
                }.getOrNull()?.let { merged -> settingsUseCase.setCurrentMonthOfYear(merged).first() }
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "deleteAbsenceDates")
                snackbarManager.show("Не удалось удалить отвлечение")
            }
        }
    }

    /** Добавить «Выходной» на выбранный день (личный день отдыха, ×2 тариф). */
    fun addDayOff(day: Int) {
        val m = currentMonth ?: return
        viewModelScope.launch {
            try {
                val date = LocalDate(m.year, m.month + 1, day)
                releaseDayUseCase.savePeriod(
                    ReleasePeriod(days = listOf(date), type = ReleaseType.DayOff)
                ).collect {}
                // Синхронизируем объединённый месяц (с отвлечениями) в settings.selectMonthOfYear —
                // именно оттуда расчёт зарплаты/нормы читает дни, поэтому без этого ×2 за DayOff
                // не применится. Так же делает SelectReleaseDaysViewModel.
                runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == m.year && it.month == m.month }
                }.getOrNull()?.let { merged ->
                    settingsUseCase.setCurrentMonthOfYear(merged).first()
                }
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("CalendarViewModel", "addDayOff")
                snackbarManager.show("Не удалось добавить выходной")
            }
        }
    }

    private suspend fun loadMonth() {
        val baseMonth = currentMonth ?: return
        val setting = userSettings ?: return
        val conv = converter ?: return
        val context = TimeCalculationContext.from(setting)

        // Берём месяц из реактивного календаря (там объединены производственный
        // календарь + личные отвлечения из ReleaseDay), чтобы отвлечения
        // отображались и обновлялись после добавления.
        val month = runCatching {
            calendarUseCase.loadFlowMonthOfYearListState().first()
                .find { it.year == baseMonth.year && it.month == baseMonth.month }
        }.getOrNull() ?: baseMonth

        withContext(Dispatchers.IO) {
            val result = routeUseCase.listRoutesByMonth(month, context)
                .first { it is ResultState.Success || it is ResultState.Error }

            val routes: List<Route> = when (result) {
                is ResultState.Success -> result.data
                else -> emptyList()
            }

            // Поездки по дню явки (только в текущем месяце по TZ пользователя).
            val tripsByDay = mutableMapOf<Int, MutableList<TripInfo>>()
            val routesByDay = mutableMapOf<Int, MutableList<Route>>()
            routes.forEach { route ->
                val start = route.basicData.timeStartWork ?: return@forEach
                if (conv.getMonthIndex(start) != month.month) return@forEach
                val day = conv.getDayOfMonth(start)
                routesByDay.getOrPut(day) { mutableListOf() }.add(route)
                val hours = route.getWorkTime()
                tripsByDay.getOrPut(day) { mutableListOf() }.add(
                    TripInfo(
                        routeId = route.basicData.id,
                        startTime = start,
                        timeText = conv.getTime(start),
                        dateText = "${two(day)}.${two(month.month + 1)}",
                        number = route.basicData.number?.let { "Поездка №$it" } ?: "Поездка",
                        hoursText = ConverterLongToTime.getTimeInStringFormat(hours).trim(),
                        isFavorite = route.basicData.isFavorite,
                        isSynchronized = route.basicData.isSynchronized,
                    )
                )
            }
            tripsByDay.values.forEach { list -> list.sortBy { it.startTime } }
            routesByDay.values.forEach { list -> list.sortBy { it.basicData.timeStartWork } }

            // Отвлечения по дню — с группировкой в периоды (подряд идущие дни
            // одного типа). Для каждого дня храним и его период (диапазон, всего
            // дней/часов), чтобы показать «всё отвлечение» и удалить целиком.
            val absenceByDay = mutableMapOf<Int, AbsenceInfo>()
            val tagByDay = month.days.associate { it.dayOfMonth to it.tag }
            val relByDay = month.days
                .filter { it.isReleaseDay && it.releaseType != null }
                .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
            val sortedRel = relByDay.keys.sorted()
            var ri = 0
            while (ri < sortedRel.size) {
                val startDay = sortedRel[ri]
                val type = relByDay.getValue(startDay)
                var rj = ri
                while (rj + 1 < sortedRel.size &&
                    sortedRel[rj + 1] == sortedRel[rj] + 1 &&
                    relByDay[sortedRel[rj + 1]] == type
                ) rj++
                val endDay = sortedRel[rj]
                val periodHours = (startDay..endDay).sumOf { d ->
                    releaseHoursForDay(type, tagByDay[d] ?: TagForDay.WORKING_DAY)
                }
                for (d in startDay..endDay) {
                    absenceByDay[d] = AbsenceInfo(
                        type = type,
                        label = type.text,
                        hoursPerDay = releaseHoursForDay(type, tagByDay[d] ?: TagForDay.WORKING_DAY),
                        periodStart = startDay,
                        periodEnd = endDay,
                        periodDays = endDay - startDay + 1,
                        periodHours = periodHours,
                    )
                }
                ri = rj + 1
            }

            // ВАЖНО: считаем за отображаемый месяц (month), а не за settings.selectMonthOfYear —
            // иначе при листании месяцев часы берутся за исходный месяц и дают 00:00.
            val totalWorked = routes.calculateWorkTimeWithSettings(
                monthOfYear = month,
                userSettings = setting,
                currentTimeInMillis = System.currentTimeMillis(),
            )

            // Праздничные дни (тег HOLIDAY производственного календаря) — для выделения
            // и подписи. Названия — из региональных праздников (если задан регион).
            val holidayDays = month.days
                .filter { it.tag == TagForDay.HOLIDAY }
                .map { it.dayOfMonth }
                .toSet()
            // Названия: сначала региональные (из БД, специфичны для региона),
            // затем федеральные фиксированные (ст.112 ТК РФ) — зашиты на клиенте,
            // т.к. на сервере у производственного календаря имён нет, только теги.
            // Названия региональных праздников — из ЛОКАЛЬНОЙ БД (заполняется при
            // выборе региона в настройках). Без сети → без задержки загрузки месяца.
            val regionalNames: Map<Int, String> = setting.region?.let { region ->
                runCatching {
                    calendarUseCase.getRegionalHolidays(region, month.year)
                        .filter { it.month == month.month }
                        .associate { it.dayOfMonth to it.name }
                }.getOrDefault(emptyMap())
            } ?: emptyMap()
            val country = setting.region?.substringBefore("-")?.uppercase() ?: "RU"
            val holidayNames: Map<Int, String> = buildMap {
                holidayDays.forEach { day ->
                    val name = regionalNames[day]
                        ?: if (country == "RU") RU_FEDERAL_HOLIDAY_NAMES[month.month to day] else null
                    if (name != null) put(day, name)
                }
            }

            val today = todayIfCurrentMonth(month, conv)
            val tripCount = tripsByDay.values.sumOf { it.size }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    year = month.year,
                    month = month.month,
                    monthLabelUpper = monthLabel(month.month, month.year).uppercase(),
                    monthLabelNormal = monthLabel(month.month, month.year),
                    totalWorkedText = ConverterLongToTime.getTimeInStringFormat(totalWorked).trim(),
                    summaryText = buildSummary(tripCount, absenceByDay),
                    tripsByDay = tripsByDay,
                    routesByDay = routesByDay,
                    absenceByDay = absenceByDay,
                    holidayDays = holidayDays,
                    holidayNames = holidayNames,
                    monthData = month,
                    offsetInMoscow = setting.timeZone,
                    dateAndTimeConverter = conv,
                    timeContext = context,
                    today = today,
                )
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────
    private fun two(v: Int): String = if (v < 10) "0$v" else v.toString()

    private fun releaseHoursForDay(type: ReleaseType, tag: TagForDay): Int =
        // «Выходной» — не норма-часы, а ×2 тариф при работе; часы не показываем.
        // «Командировка» — оплата по фактическим маршрутам (средний час), норма-
        // часы отдыха не показываем.
        if (type == ReleaseType.DayOff || type == ReleaseType.BusinessTrip) {
            0
        } else if (type == ReleaseType.ChildCare) {
            when (tag) {
                TagForDay.WORKING_DAY -> 8
                TagForDay.SHORTENED_DAY -> 7
                TagForDay.NON_WORKING_DAY -> 8
                TagForDay.HOLIDAY -> 8
            }
        } else {
            when (tag) {
                TagForDay.WORKING_DAY -> 8
                TagForDay.SHORTENED_DAY -> 7
                TagForDay.NON_WORKING_DAY -> 0
                TagForDay.HOLIDAY -> 0
            }
        }

    private fun todayIfCurrentMonth(month: MonthOfYear, conv: DateAndTimeConverter): Int? {
        val now = System.currentTimeMillis()
        return if (conv.getMonthIndex(now) == month.month) conv.getDayOfMonth(now) else null
    }

    private fun monthLabel(monthIndex: Int, year: Int): String {
        val names = listOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val name = names.getOrElse(monthIndex) { "" }
        return "$name $year"
    }

    private fun buildSummary(tripCount: Int, absence: Map<Int, AbsenceInfo>): String {
        val parts = mutableListOf<String>()
        parts.add("$tripCount ${plural(tripCount, "поездка", "поездки", "поездок")}")
        val vacation = absence.values.count { it.type == ReleaseType.Vacation }
        val sick = absence.values.count { it.type == ReleaseType.SickLeave }
        if (vacation > 0) parts.add("$vacation ${plural(vacation, "день", "дня", "дней")} отпуска")
        if (sick > 0) parts.add("$sick ${plural(sick, "больничный", "больничных", "больничных")}")
        return parts.joinToString(" · ")
    }

    private fun plural(n: Int, one: String, few: String, many: String): String {
        val m10 = n % 10
        val m100 = n % 100
        return when {
            m100 in 11..14 -> many
            m10 == 1 -> one
            m10 in 2..4 -> few
            else -> many
        }
    }
}

/** Полное состояние экрана «Календарь». */
data class CalendarUiState(
    val isLoading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,                 // 0-based
    val monthLabelUpper: String = "",   // "МАЙ 2026"
    val monthLabelNormal: String = "",  // "Май 2026"
    val totalWorkedText: String = "00:00",
    val summaryText: String = "",
    val tripsByDay: Map<Int, List<TripInfo>> = emptyMap(),
    val routesByDay: Map<Int, List<Route>> = emptyMap(), // полные маршруты (для item как на главном)
    val absenceByDay: Map<Int, AbsenceInfo> = emptyMap(),
    val holidayDays: Set<Int> = emptySet(),        // дни с тегом HOLIDAY
    val holidayNames: Map<Int, String> = emptyMap(), // день → название праздника (если есть)
    val monthData: MonthOfYear? = null,
    val offsetInMoscow: Long = 0L,
    val dateAndTimeConverter: DateAndTimeConverter? = null,
    val timeContext: TimeCalculationContext? = null,
    val today: Int? = null,
)

/** Поездка (маршрут) для ячейки/детализации дня. */
data class TripInfo(
    val routeId: String,
    val startTime: Long,
    val timeText: String,   // "07:45"
    val dateText: String,   // "15.05"
    val number: String?,    // "Поездка №115"
    val hoursText: String,  // "04:35"
    val isFavorite: Boolean,
    val isSynchronized: Boolean,
)

/** Отвлечение (личный день) для ячейки/детализации дня. */
data class AbsenceInfo(
    val type: ReleaseType,
    val label: String,       // "Отпуск"
    val hoursPerDay: Int,    // норма-часы за этот день (0 в выходной/праздник)
    val periodStart: Int,    // начало непрерывного периода этого типа
    val periodEnd: Int,      // конец периода
    val periodDays: Int,     // всего дней в периоде
    val periodHours: Int,    // всего норма-часов за период
)

/**
 * Названия федеральных праздников РФ (ст.112 ТК РФ) по фиксированным датам.
 * Ключ — (месяц 0-based, число). Переносы выходных сюда не входят — у них
 * нет собственного названия (покажется «Праздничный день»).
 */
private val RU_FEDERAL_HOLIDAY_NAMES: Map<Pair<Int, Int>, String> = buildMap {
    // Январь: новогодние каникулы 1–8, Рождество 7-го
    for (d in 1..8) put(0 to d, "Новогодние каникулы")
    put(0 to 7, "Рождество Христово")
    put(1 to 23, "День защитника Отечества")
    put(2 to 8, "Международный женский день")
    put(4 to 1, "Праздник Весны и Труда")
    put(4 to 9, "День Победы")
    put(5 to 12, "День России")
    put(10 to 4, "День народного единства")
}

/** Вариант времени явки в режиме планирования (чип). */
data class TimeOption(val millis: Long, val label: String)

/** Состояние режима планирования маршрутов. */
data class RoutePlanState(
    val active: Boolean = false,
    val timeOptions: List<TimeOption> = emptyList(),
    val activeTime: Long? = null,
    // Отмеченные дни. Явка у всех — [activeTime], поэтому смена времени
    // мгновенно меняет её на всех выбранных днях.
    val plannedDays: Set<Int> = emptySet(),
)
