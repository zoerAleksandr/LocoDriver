@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.calculateWorkTimeWithSettings
import com.z_company.domain.entities.route.UtilsForEntities.getWorkTime
import com.z_company.domain.entities.setting.SalarySetting
import com.z_company.domain.entities.setting.UserSettings
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SalarySettingUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import com.z_company.domain.util.displayTimeZone
import com.z_company.domain.util.currencySymbol
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.RoutesManager
import com.z_company.repository.remote_rest.ShareRouteManager
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * KMP ViewModel экрана «Календарь» (SCREEN_SPECS §14) — порт `CalendarViewModel`
 * (features/route). Логика здесь, SwiftUI только рисует (`watchX`-колбэки).
 *
 * BUG-45/BUG-46 перенесены как есть (паритет с Android):
 * - «Заменить» при дубле удаляет маршрут физически (`removeRoute`) — BUG-45;
 * - `addDayOff` не удаляет существующие отвлечения дня — BUG-46.
 */
class CalendarIosViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val salarySettingUseCase: SalarySettingUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val routeUseCase: RouteUseCase,
    private val partnerUseCase: PartnerUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val secureTokenStorage: SecureTokenStorage,
    private val syncManager: SyncManager,
    private val shareRouteManager: ShareRouteManager,
    private val routesManager: RoutesManager,
) : ViewModel() {

    private val gate = IosRouteGate(routeUseCase, settingsUseCase)

    private val _ui = MutableStateFlow(CalendarIosUi())
    private val _plan = MutableStateFlow(CalendarIosPlanUi())
    private val _limit = MutableStateFlow<CalendarIosLimit?>(null)
    private val _duplicate = MutableStateFlow<CalendarIosDuplicate?>(null)

    private var userSettings: UserSettings? = null
    private var salarySetting: SalarySetting? = null
    private var currentMonth: MonthOfYear? = null

    // Внутренние данные для дублей и шаринга (Swift получает только готовые строки).
    private var pendingDuplicateRoutes: List<Route> = emptyList()
    private var duplicateExistingRoute: Route? = null

    private var messageSink: ((String) -> Unit)? = null
    private val pendingMessages = mutableListOf<String>()
    private var shareSink: ((String) -> Unit)? = null
    private var openFormSink: ((String) -> Unit)? = null

    // ── watchX (паттерн AGENTS.md) ───────────────────────────────────────────

    fun watchUi(callback: (CalendarIosUi) -> Unit) {
        viewModelScope.launch { _ui.collect { callback(it) } }
    }

    fun watchPlan(callback: (CalendarIosPlanUi) -> Unit) {
        viewModelScope.launch { _plan.collect { callback(it) } }
    }

    fun watchLimit(callback: (CalendarIosLimit?) -> Unit) {
        viewModelScope.launch { _limit.collect { callback(it) } }
    }

    fun watchDuplicate(callback: (CalendarIosDuplicate?) -> Unit) {
        viewModelScope.launch { _duplicate.collect { callback(it) } }
    }

    fun watchMessage(callback: (String) -> Unit) {
        messageSink = callback
        pendingMessages.toList().also { pendingMessages.clear() }.forEach(callback)
    }

    fun watchShareLink(callback: (String) -> Unit) { shareSink = callback }

    /** Открыть форму маршрута-копии (после проверки подписки). */
    fun watchOpenRouteForm(callback: (String) -> Unit) { openFormSink = callback }

    // ── Загрузка месяца (§14.1) ─────────────────────────────────────────────

    fun prepareScreen() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true) }
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                userSettings = setting
                salarySetting = runCatching { salarySettingUseCase.salarySettingFlow().first() }.getOrNull()
                currentMonth = setting.selectMonthOfYear
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Ошибка загрузки: ${t.message ?: "неизвестная ошибка"}")
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun reload() {
        if (currentMonth == null) return
        viewModelScope.launch { runCatching { loadMonth() } }
    }

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
            _ui.update { it.copy(isLoading = true) }
            try {
                val list = calendarUseCase.loadFlowMonthOfYearListState().first()
                val found = list.find { it.year == year && it.month == monthIndex }
                if (found == null) {
                    emitMessage("Нет данных календаря за этот месяц")
                    _ui.update { it.copy(isLoading = false) }
                    return@launch
                }
                settingsUseCase.setCurrentMonthOfYear(found).first()
                currentMonth = found
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadMonth() {
        val baseMonth = currentMonth ?: return
        val setting = userSettings ?: return

        salarySetting = runCatching { salarySettingUseCase.salarySettingFlow().first() }
            .getOrNull() ?: salarySetting

        // Месяц из реактивного календаря — отвлечения из ReleaseDay уже объединены.
        val month = runCatching {
            calendarUseCase.loadFlowMonthOfYearListState().first()
                .find { it.year == baseMonth.year && it.month == baseMonth.month }
        }.getOrNull() ?: baseMonth

        val workResult = withContext(Dispatchers.Default) {
            // Дата явки в календаре — в поясе отображения (§14.1).
            val calendarContext = TimeCalculationContext.from(setting).copy(
                crossMonthTZ = iosDisplayTimeZone(setting)
            )
            val result = routeUseCase.listRoutesByMonth(month, calendarContext)
                .first { it is ResultState.Success || it is ResultState.Error }
            val routes = when (result) {
                is ResultState.Success -> result.data
                else -> emptyList()
            }
            val totalWorked = routes.calculateWorkTimeWithSettings(
                monthOfYear = month,
                userSettings = setting,
                currentTimeInMillis = Clock.System.now().toEpochMilliseconds(),
            )
            routes to totalWorked
        }
        val (routes, totalWorked) = workResult

        val tripsByDay = mutableMapOf<Int, MutableList<CalendarIosTrip>>()
        // Нумерация «#N» — как на Главном: список месяца по убыванию явки, #N = порядок от старых.
        val orderedRoutes = routes.sortedByDescending { it.basicData.timeStartWork ?: Long.MIN_VALUE }
        val monthRouteCount = orderedRoutes.size
        val cardSettings = salarySetting ?: SalarySetting()
        val displayTz = iosDisplayTimeZone(setting)
        routes.forEach { route ->
            val start = route.basicData.timeStartWork ?: return@forEach
            if (iosMonthIndex(start, setting) != month.month) return@forEach
            val day = iosDayOfMonth(start, setting)
            val dt = iosLocal(start, iosDisplayOffsetMs(setting))
            val indexInMonth = orderedRoutes.indexOfFirst { it.basicData.id == route.basicData.id }
            tripsByDay.getOrPut(day) { mutableListOf() }.add(
                CalendarIosTrip(
                    basicId = route.basicData.id,
                    timeText = "${iosTwo(dt.hour)}:${iosTwo(dt.minute)}",
                    numberText = route.basicData.number?.let { "Поездка №$it" } ?: "Поездка",
                    hoursText = iosFormatWorkTime(route.getWorkTime() ?: 0L, setting),
                    isFavorite = route.basicData.isFavorite,
                    isSynchronized = route.basicData.isSynchronized,
                    card = HomeRouteCardBuilder.build(
                        route, indexInMonth, monthRouteCount,
                        setting, cardSettings, month, displayTz,
                    ),
                )
            )
        }
        tripsByDay.values.forEach { list -> list.sortBy { it.timeText } }

        // Отвлечения: группировка подряд идущих дней одного типа в периоды.
        val profile = sharedPrefs.getWorkScheduleProfile()
        val tagByDay = month.days.associate { it.dayOfMonth to it.tag }
        val relByDay = month.days
            .filter { it.isReleaseDay && it.releaseType != null }
            .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
        val techHoursByDay = month.days
            .filter { it.isReleaseDay && it.releaseType == ReleaseType.TechnicalStudy }
            .associate { it.dayOfMonth to (it.hours ?: 0.0) }
        val absenceByDay = mutableMapOf<Int, CalendarIosAbsence>()
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
            val isTech = type == ReleaseType.TechnicalStudy
            val isDayOff = type == ReleaseType.DayOff
            val periodHours = (startDay..endDay).sumOf { d ->
                iosReleaseHoursForDay(
                    type,
                    LocalDate(month.year, month.month + 1, d),
                    tagByDay[d] ?: TagForDay.WORKING_DAY,
                    profile,
                )
            }
            val customPeriodHours = if (isTech) (startDay..endDay).sumOf { techHoursByDay[it] ?: 0.0 } else null
            for (d in startDay..endDay) {
                val perDay = iosReleaseHoursForDay(
                    type,
                    LocalDate(month.year, month.month + 1, d),
                    tagByDay[d] ?: TagForDay.WORKING_DAY,
                    profile,
                )
                val customPerDay = if (isTech) techHoursByDay[d] ?: 0.0 else null
                absenceByDay[d] = CalendarIosAbsence(
                    kind = type.iosKind(),
                    label = type.text,
                    badge = type.iosBadge(),
                    hoursPerDay = perDay,
                    periodStart = startDay,
                    periodEnd = endDay,
                    periodDays = endDay - startDay + 1,
                    periodHours = periodHours,
                    customHoursText = customPerDay?.let { iosHoursText(it) },
                    customPeriodHoursText = customPeriodHours?.let { iosHoursText(it) },
                    hoursValueText = when {
                        isDayOff -> "×2"
                        isTech -> customPerDay?.let { iosHoursText(it) } ?: ""
                        else -> "$perDay:00"
                    },
                    hoursCaptionText = when {
                        isDayOff -> "тариф"
                        isTech -> "часов"
                        else -> "всего"
                    },
                    isDayOff = isDayOff,
                    isTechnicalStudy = isTech,
                )
            }
            ri = rj + 1
        }

        val holidayDays = month.days.filter { it.tag == TagForDay.HOLIDAY }.map { it.dayOfMonth }.toSet()
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
                val name = iosHolidayName(month.month, day, regionalNames, country)
                if (name != null) put(day, name)
            }
        }

        val today = Clock.System.now().toEpochMilliseconds().let { now ->
            if (iosMonthIndex(now, setting) == month.month) iosDayOfMonth(now, setting) else null
        }

        val days = (1..iosDaysInMonth(month.year, month.month)).map { day ->
            val date = LocalDate(month.year, month.month + 1, day)
            val weekdayIndex = (date.dayOfWeek.ordinal + 6) % 7 // 0 = понедельник
            CalendarIosDay(
                day = day,
                weekdayIndex = weekdayIndex,
                isWeekend = weekdayIndex >= 5,
                isToday = day == today,
                isHoliday = day in holidayDays,
                holidayName = holidayNames[day],
                trips = tripsByDay[day].orEmpty(),
                absence = absenceByDay[day],
            )
        }

        val tripCount = tripsByDay.values.sumOf { it.size }
        _ui.update {
            it.copy(
                isLoading = false,
                year = month.year,
                month = month.month,
                monthLabelUpper = "${iosMonthName(month.month)} ${month.year}".uppercase(),
                monthLabelNormal = "${iosMonthName(month.month)} ${month.year}",
                totalWorkedText = iosFormatWorkTime(totalWorked, setting),
                summaryText = buildSummary(tripCount, absenceByDay),
                days = days,
                today = today,
                averagePaymentHour = salarySetting?.averagePaymentHour ?: 0.0,
                currencySymbol = currencySymbol(setting.country),
            )
        }
        refreshPlanLabels()
    }

    private fun iosHoursText(hours: Double): String =
        if (hours % 1.0 == 0.0) hours.toInt().toString()
        else hours.toString().replace('.', ',')

    private fun buildSummary(tripCount: Int, absence: Map<Int, CalendarIosAbsence>): String {
        val parts = mutableListOf<String>()
        parts.add("$tripCount ${iosPlural(tripCount, "поездка", "поездки", "поездок")}")
        val vacation = absence.values.count { it.kind == "vacation" }
        val sick = absence.values.count { it.kind == "sick" }
        if (vacation > 0) parts.add("$vacation ${iosPlural(vacation, "день", "дня", "дней")} отпуска")
        if (sick > 0) parts.add("$sick ${iosPlural(sick, "больничный", "больничных", "больничных")}")
        return parts.joinToString(" · ")
    }

    private fun emitMessage(text: String) {
        val sink = messageSink
        if (sink != null) sink(text) else pendingMessages.add(text)
    }

    // ── Режим планирования (§14.4) ──────────────────────────────────────────

    fun enterRoutePlan(initialDay: Int) {
        val times = userSettings?.standardTimesStartWork.orEmpty()
        _plan.value = CalendarIosPlanUi(
            active = true,
            timeOptions = times.map { CalendarIosTimeOption(it, iosFormatTimeOfDay(it)) },
            activeTimeText = null,
            plannedDays = emptyList(),
            initialDay = initialDay,
            hintStep = 0,
            bottomText = "Выберите время и отметьте дни",
            suggestedWorkDuration = userSettings?.defaultWorkTime ?: 43_200_000L,
        )
    }

    fun exitRoutePlan() {
        _plan.value = CalendarIosPlanUi()
    }

    fun pickPlanTime(millis: Long) {
        _plan.update { state ->
            val days = if (state.plannedDays.isEmpty() && state.initialDay != null) {
                listOf(CalendarIosPlanDay(state.initialDay, iosFormatTimeOfDay(millis)))
            } else state.plannedDays
            state.copy(
                activeTimeText = iosFormatTimeOfDay(millis),
                plannedDays = days,
                hintStep = if (days.isEmpty()) 1 else 2,
                bottomText = planBottomText(days.size),
            )
        }
    }

    /** Добавить своё время явки (§14.4 «+ Своё время») в чипы и в настройки. */
    fun addCustomPlanTime(hour: Int, minute: Int) {
        val millis = hour * 3_600_000L + minute * 60_000L
        _plan.update { s ->
            val opts = if (s.timeOptions.any { it.millis == millis }) s.timeOptions
            else (s.timeOptions + CalendarIosTimeOption(millis, iosFormatTimeOfDay(millis)))
                .sortedBy { it.millis }
            val days = if (s.plannedDays.isEmpty() && s.initialDay != null) {
                listOf(CalendarIosPlanDay(s.initialDay, iosFormatTimeOfDay(millis)))
            } else s.plannedDays
            s.copy(
                timeOptions = opts,
                activeTimeText = iosFormatTimeOfDay(millis),
                plannedDays = days,
                hintStep = if (days.isEmpty()) 1 else 2,
                bottomText = planBottomText(days.size),
            )
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

    /** Тап по дню в планировании: отметить/снять (день с отвлечением кроме «Выходного» недоступен). */
    fun toggleDayPlan(day: Int) {
        val s = _plan.value
        if (s.activeTimeText == null) return
        val absence = _ui.value.days.find { it.day == day }?.absence
        if (absence != null && !absence.isDayOff) return
        val time = s.activeTimeText ?: return
        val days = s.plannedDays.toMutableList()
        val existing = days.indexOfFirst { it.day == day }
        if (existing >= 0) days.removeAt(existing)
        else days.add(CalendarIosPlanDay(day, time))
        val sorted = days.sortedBy { it.day }
        _plan.value = s.copy(
            plannedDays = sorted,
            hintStep = if (sorted.isEmpty()) 1 else 2,
            bottomText = planBottomText(sorted.size),
        )
    }

    private fun planBottomText(count: Int): String =
        if (count == 0) "Выберите время и отметьте дни"
        else "$count ${iosPlural(count, "маршрут", "маршрута", "маршрутов")} к созданию"

    private fun refreshPlanLabels() {
        _plan.update { s ->
            if (!s.active) s
            else s.copy(
                hintStep = when {
                    s.activeTimeText == null -> 0
                    s.plannedDays.isEmpty() -> 1
                    else -> 2
                },
                bottomText = planBottomText(s.plannedDays.size),
            )
        }
    }

    /** Создать черновики по отмеченным дням ([workDurationMillis] = null — «Без окончания»). */
    fun createPlannedRoutes(workDurationMillis: Long?) {
        val plan = _plan.value
        val m = currentMonth ?: return
        val setting = userSettings ?: return
        if (plan.plannedDays.isEmpty()) {
            exitRoutePlan()
            return
        }
        viewModelScope.launch {
            try {
                val g = gate.canCreateRoutes(plan.plannedDays.size)
                when (g.type) {
                    "limit" -> {
                        _limit.value = buildLimitState(g.requested, g.remaining)
                        return@launch
                    }
                    "error" -> {
                        emitMessage("Не удалось проверить подписку")
                        return@launch
                    }
                }
                val routes = plan.plannedDays.map { p ->
                    val hour = p.timeText.substringBefore(":").toIntOrNull() ?: 0
                    val minute = p.timeText.substringAfter(":").toIntOrNull() ?: 0
                    val start = iosToEpochMillis(m.year, m.month, p.day, hour, minute, setting)
                    Route(basicData = BasicData(timeStartWork = start, timeEndWork = workDurationMillis?.let(start::plus)))
                }
                val existing = withContext(Dispatchers.Default) {
                    routeUseCase.getListRoutes().firstOrNull { old ->
                        routes.any { candidate ->
                            val a = old.basicData.timeStartWork
                            val b = candidate.basicData.timeStartWork
                            a != null && b != null && normalizeToMinute(a) == normalizeToMinute(b)
                        }
                    }
                }
                if (existing != null) {
                    duplicateExistingRoute = existing
                    pendingDuplicateRoutes = routes
                    _duplicate.value = CalendarIosDuplicate(
                        existingLabel = formatRouteLabel(existing, setting),
                    )
                    return@launch
                }
                savePlannedRoutes(routes)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Ошибка создания маршрутов")
            }
        }
    }

    fun dismissDuplicate() {
        _duplicate.value = null
        duplicateExistingRoute = null
        pendingDuplicateRoutes = emptyList()
    }

    /** «Заменить»: удаляет найденный маршрут физически и создаёт всю пачку. // BUG-45 (паритет с Android) */
    fun replaceDuplicateAndCreate() {
        val routes = pendingDuplicateRoutes
        val existing = duplicateExistingRoute
        dismissDuplicate()
        if (routes.isEmpty()) return
        viewModelScope.launch {
            if (existing != null) {
                routeUseCase.removeRoute(existing).first { it is ResultState.Success || it is ResultState.Error }
            }
            savePlannedRoutes(routes)
        }
    }

    /** «Оставить оба»: создаёт пачку без удаления найденного маршрута. */
    fun keepDuplicateAndCreate() {
        val routes = pendingDuplicateRoutes
        dismissDuplicate()
        if (routes.isEmpty()) return
        viewModelScope.launch { savePlannedRoutes(routes) }
    }

    private fun normalizeToMinute(timeMs: Long): Long = (timeMs / 60_000L) * 60_000L

    private fun formatRouteLabel(route: Route, settings: UserSettings): String {
        val start = route.basicData.timeStartWork ?: return ""
        val dt = iosLocal(start, iosDisplayOffsetMs(settings))
        return "${iosTwo(dt.dayOfMonth)}.${iosTwo(dt.monthNumber)} ${iosTwo(dt.hour)}:${iosTwo(dt.minute)}"
    }

    private suspend fun savePlannedRoutes(routes: List<Route>) {
        _ui.update { it.copy(operationMessage = "Создаём маршруты…\nЭто может занять несколько секунд") }
        try {
            var created = 0
            val permanentPartner = partnerUseCase.getPermanent()
            for (route in routes) {
                val res = routeUseCase.saveRoute(PartnerUseCase.withPermanentPartner(route, permanentPartner))
                    .first { it is ResultState.Success || it is ResultState.Error }
                if (res is ResultState.Success) created++
            }
            val left = gate.freeRoutesLeft()
            emitMessage(
                when {
                    created == 0 -> "Не создано ни одного маршрута"
                    left == null -> "Создано черновиков маршрутов: $created"
                    else -> "Создано черновиков маршрутов: $created. " +
                        "Осталось бесплатных: $left из ${IosRouteGate.FREE_ROUTES_LIMIT}"
                }
            )
            exitRoutePlan()
            loadMonth()
        } finally {
            _ui.update { it.copy(operationMessage = null) }
        }
    }

    private fun buildLimitState(requested: Int, remaining: Int): CalendarIosLimit {
        val message = if (remaining == 0) {
            "Бесплатный лимит исчерпан: использовано ${IosRouteGate.FREE_ROUTES_LIMIT} " +
                "из ${IosRouteGate.FREE_ROUTES_LIMIT}. Сейчас создаётся $requested " +
                "${iosPlural(requested, "маршрут", "маршрута", "маршрутов")}. " +
                "Оформите подписку — лимит снимется и включится синхронизация."
        } else {
            "В бесплатном периоде доступно ${IosRouteGate.FREE_ROUTES_LIMIT} маршрутов, " +
                "у вас осталось $remaining. Сейчас создаётся $requested " +
                "${iosPlural(requested, "маршрут", "маршрута", "маршрутов")}. " +
                "Оформите подписку — лимит снимется и включится синхронизация."
        }
        return CalendarIosLimit(requested = requested, remaining = remaining, isExhausted = remaining == 0, message = message)
    }

    fun dismissLimit() { _limit.value = null }

    // ── Отвлечения (§14.3, §14.5) ───────────────────────────────────────────

    /** «Выходной» на день. // BUG-46 (паритет с Android): существующие отвлечения дня не удаляются. */
    fun addDayOff(day: Int) {
        val m = currentMonth ?: return
        viewModelScope.launch {
            try {
                val date = LocalDate(m.year, m.month + 1, day)
                val saveResult = releaseDayUseCase.savePeriod(
                    ReleasePeriod(days = listOf(date), type = ReleaseType.DayOff)
                ).first { it is ResultState.Success || it is ResultState.Error }
                if (saveResult is ResultState.Success) autoPushSettings()
                syncMonthToSettings(m)
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось добавить выходной")
            }
        }
    }

    /** «Технические занятия» (§14.6): заменяет любое отвлечение дня. */
    fun addTechnicalStudy(day: Int, hours: Double) {
        val m = currentMonth ?: return
        if (hours <= 0.0) return
        viewModelScope.launch {
            try {
                val date = LocalDate(m.year, m.month + 1, day)
                releaseDayUseCase.deletePeriod(ReleasePeriod(days = listOf(date), type = null))
                    .first { it is ResultState.Success || it is ResultState.Error }
                val releaseDay = ReleaseDay(
                    year = m.year,
                    month = m.month,
                    dayOfMonth = day,
                    releaseType = ReleaseType.TechnicalStudy,
                    hours = hours,
                )
                val saveResult = releaseDayUseCase.saveAll(listOf(releaseDay))
                    .first { it is ResultState.Success || it is ResultState.Error }
                if (saveResult is ResultState.Success) autoPushSettings()
                syncMonthToSettings(m)
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось добавить технические занятия")
            }
        }
    }

    fun deleteAbsenceDay(day: Int) = deleteAbsenceDates(listOf(day))

    fun deleteAbsencePeriod(startDay: Int, endDay: Int) =
        deleteAbsenceDates((minOf(startDay, endDay)..maxOf(startDay, endDay)).toList())

    private fun deleteAbsenceDates(days: List<Int>) {
        val m = currentMonth ?: return
        if (days.isEmpty()) return
        viewModelScope.launch {
            try {
                val dates = days.map { LocalDate(m.year, m.month + 1, it) }
                val deleteResult = releaseDayUseCase
                    .deletePeriod(ReleasePeriod(days = dates, type = null))
                    .first { it is ResultState.Success || it is ResultState.Error }
                if (deleteResult is ResultState.Success) autoPushSettings()
                syncMonthToSettings(m)
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось удалить отвлечение")
            }
        }
    }

    /** Синхронизирует объединённый месяц в `settings.selectMonthOfYear` (иначе ЗП/норма не увидят изменения). */
    private suspend fun syncMonthToSettings(m: MonthOfYear) {
        runCatching {
            calendarUseCase.loadFlowMonthOfYearListState().first()
                .find { it.year == m.year && it.month == m.month }
        }.getOrNull()?.let { merged -> settingsUseCase.setCurrentMonthOfYear(merged).first() }
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch(Dispatchers.Default) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }

    // ── Маршруты: удаление и быстрый просмотр (§14.5, §14) ─────────────────

    fun deleteRoute(basicId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(operationMessage = "Удаляем маршрут…") }
            try {
                val route = findRoute(basicId) ?: return@launch
                routeUseCase.markAsRemoved(route)
                    .first { it is ResultState.Success || it is ResultState.Error }
                loadMonth()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось удалить маршрут")
            } finally {
                _ui.update { it.copy(operationMessage = null) }
            }
        }
    }

    fun setFavoriteRoute(basicId: String) {
        viewModelScope.launch {
            val route = findRoute(basicId) ?: return@launch
            try {
                val result = routeUseCase.setFavoriteRoute(route.basicData.id, !route.basicData.isFavorite)
                    .first { it is ResultState.Success || it is ResultState.Error }
                when (result) {
                    is ResultState.Success -> {
                        emitMessage(
                            if (result.data) "Маршрут добавлен в избранное"
                            else "Маршрут удален из избранного"
                        )
                        loadMonth()
                    }
                    is ResultState.Error -> emitMessage("Ошибка")
                    else -> Unit
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Ошибка")
            }
        }
    }

    fun syncRoute(basicId: String) {
        viewModelScope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first()
            if (token.isNullOrBlank()) {
                emitMessage("Войдите в аккаунт, чтобы синхронизировать маршрут")
                return@launch
            }
            if (!gate.hasActiveSubscription()) {
                emitMessage("Синхронизация доступна по подписке")
                return@launch
            }
            val route = findRoute(basicId) ?: return@launch
            try {
                routesManager.saveRouteInRemote(route, "Bearer $token").collect { state ->
                    when (state) {
                        is ResultState.Success -> {
                            routeUseCase.setSynchronizedRoute(route.basicData.id).first()
                            val warnings = state.data.warnings
                            if (warnings.isNotEmpty()) {
                                emitMessage("Маршрут сохранен с предупреждениями:\n${warnings.joinToString("\n")}")
                            } else {
                                emitMessage("Маршрут сохранен в облаке")
                            }
                            loadMonth()
                        }
                        is ResultState.Error -> emitMessage("Ошибка синхронизации")
                        else -> Unit
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Ошибка синхронизации")
            }
        }
    }

    /** Копия маршрута: проверка подписки → `watchOpenRouteForm(basicId)`. */
    fun makeCopyRoute(basicId: String) {
        viewModelScope.launch {
            val setting = settingsUseCase.getUserSettingFlow().first()
            val now = Clock.System.now().toEpochMilliseconds()
            val subscriptionActive = setting.subscriptionPeriod != 0L &&
                setting.subscriptionPeriod + 24L * 3_600_000L >= now
            if (!subscriptionActive) {
                emitMessage("Создание маршрутов доступно по подписке")
                return@launch
            }
            openFormSink?.invoke(basicId)
        }
    }

    /** Публичная ссылка → `watchShareLink(url)`. */
    fun shareRoute(basicId: String) {
        viewModelScope.launch {
            try {
                val rawToken = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (rawToken.isNullOrBlank()) {
                    emitMessage("Неавторизованный пользователь")
                    return@launch
                }
                val route = findRoute(basicId) ?: return@launch
                shareRouteManager.createShareLink(route, "Bearer $rawToken").collect { result ->
                    when (result) {
                        is ResultState.Success -> shareSink?.invoke(result.data)
                        is ResultState.Error -> emitMessage("Не удалось создать ссылку")
                        else -> Unit
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось создать ссылку")
            }
        }
    }

    /** Сумма «К начислению» за техзанятия (§14.6) или "" если средний час не задан. */
    fun techStudyPayText(hours: Double): String {
        val average = salarySetting?.averagePaymentHour ?: 0.0
        if (average <= 0.0) return ""
        return iosFormatMoney(average * hours)
    }

    private suspend fun findRoute(basicId: String): Route? {
        val m = currentMonth ?: return null
        val setting = userSettings ?: return null
        val context = TimeCalculationContext.from(setting).copy(crossMonthTZ = iosDisplayTimeZone(setting))
        return withContext(Dispatchers.Default) {
            val result = routeUseCase.listRoutesByMonth(m, context)
                .first { it is ResultState.Success || it is ResultState.Error }
            (result as? ResultState.Success)?.data?.find { it.basicData.id == basicId }
        }
    }
}
