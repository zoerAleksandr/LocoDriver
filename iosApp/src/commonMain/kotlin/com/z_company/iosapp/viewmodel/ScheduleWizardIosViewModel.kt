@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SchedulePattern
import com.z_company.domain.entities.WorkShiftType
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.PartnerUseCase
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.util.TimeCalculationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** Служебный id «плитки-конструктора» — свой цикл (порт `CUSTOM_PATTERN_ID`). */
const val WIZARD_CUSTOM_PATTERN_ID = "custom"

/**
 * KMP ViewModel мастера «Заполнить месяц» (SCREEN_SPECS §15) — порт
 * `ScheduleWizardViewModel`. Паттерны хранятся локально (`SharedPreferencesRepositories`),
 * `last_schedule_month` пишется в формате Android (§15.1).
 */
class ScheduleWizardIosViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val routeUseCase: RouteUseCase,
    private val partnerUseCase: PartnerUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val prefs: SharedPreferencesRepositories,
) : ViewModel() {

    private val gate = IosRouteGate(routeUseCase, settingsUseCase)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _ui = MutableStateFlow(WizardIosUi())
    private var month: MonthOfYear? = null
    private var lastSchedule: LastScheduleRecord? = null

    private data class LastScheduleRecord(
        val year: Int,
        val month: Int,
        val patternId: String,
        val firstDay: Int,
        val nextPhase: Int,
        val dayStart: String?,
        val dayEnd: String?,
        val nightStart: String?,
        val nightEnd: String?,
    )

    fun watchUi(callback: (WizardIosUi) -> Unit) {
        scope.launch { _ui.collect { callback(it) } }
    }

    fun prepareScreen() {
        scope.launch {
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val m = setting.selectMonthOfYear
                month = m
                val daysInMonth = iosDaysInMonth(m.year, m.month)
                val patterns = prefs.getSchedulePatterns() ?: SchedulePattern.defaults().also {
                    prefs.setSchedulePatterns(it)
                }
                val patternUis = patterns.map { p -> p.toUi() }
                val selectedId = patternUis.firstOrNull()?.id ?: WIZARD_CUSTOM_PATTERN_ID
                val customCycle = _ui.value.customCycle
                val record = parseLastSchedule(patternUis, customCycle)
                lastSchedule = record
                val canContinue = canContinue(record, m, patternUis, customCycle)
                _ui.update {
                    it.copy(
                        year = m.year,
                        month = m.month,
                        monthName = iosMonthName(m.month),
                        daysInMonth = daysInMonth,
                        patterns = patternUis,
                        selectedId = selectedId,
                        isCustom = selectedId == WIZARD_CUSTOM_PATTERN_ID,
                        pickerIndex = -1,
                        canContinuePrevious = canContinue,
                        showContinuePreviousSheet = canContinue,
                        previousMonthName = record?.let { r -> iosMonthName(r.month) } ?: "",
                        continuePrevious = false,
                        phaseOffset = 0,
                        preview = emptyList(),
                    )
                }
                rebuildPreview(resetPhase = true)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    // ── Шаг 1: паттерны и «свой» цикл ───────────────────────────────────────

    fun selectPattern(id: String) {
        // Ручной выбор паттерна выходит из режима продолжения (§15.1).
        _ui.update {
            it.copy(
                selectedId = id,
                isCustom = id == WIZARD_CUSTOM_PATTERN_ID,
                pickerIndex = -1,
                continuePrevious = false,
                phaseOffset = 0,
            )
        }
        rebuildPreview(resetPhase = true)
    }

    fun deletePattern(id: String) {
        if (id == WIZARD_CUSTOM_PATTERN_ID) return
        _ui.update { s ->
            val patterns = s.patterns.filterNot { it.id == id }
            prefs.setSchedulePatterns(patterns.map { it.toDomain() })
            val newSelected = if (s.selectedId == id) {
                patterns.firstOrNull()?.id ?: WIZARD_CUSTOM_PATTERN_ID
            } else s.selectedId
            s.copy(
                patterns = patterns,
                selectedId = newSelected,
                isCustom = newSelected == WIZARD_CUSTOM_PATTERN_ID,
                pickerIndex = -1,
            )
        }
        rebuildPreview(resetPhase = true)
    }

    fun openTypePicker(index: Int) = _ui.update {
        it.copy(pickerIndex = if (it.pickerIndex == index) -1 else index)
    }

    fun closeTypePicker() = _ui.update { it.copy(pickerIndex = -1) }

    /** kind: "day" | "night" | "off". */
    fun setCycleDayType(index: Int, kind: String) {
        _ui.update { s ->
            val cycle = s.customCycle.toMutableList()
            if (index in cycle.indices) cycle[index] = kind
            s.copy(customCycle = cycle, pickerIndex = -1, continuePrevious = false, phaseOffset = 0)
        }
        rebuildPreview(resetPhase = true)
    }

    fun addCycleDay() {
        _ui.update { s ->
            val cycle = s.customCycle + "day"
            s.copy(
                customCycle = cycle,
                pickerIndex = cycle.lastIndex,
                continuePrevious = false,
                phaseOffset = 0,
            )
        }
        rebuildPreview(resetPhase = true)
    }

    fun removeCycleDay(index: Int) {
        _ui.update { s ->
            if (s.customCycle.size <= 1) return@update s
            val cycle = s.customCycle.toMutableList().apply { removeAt(index) }
            s.copy(customCycle = cycle, pickerIndex = -1, continuePrevious = false, phaseOffset = 0)
        }
        rebuildPreview(resetPhase = true)
    }

    fun setDayStart(hour: Int, minute: Int) = _ui.update { it.copy(dayStartText = fmt(hour, minute)) }
    fun setDayEnd(hour: Int, minute: Int) = _ui.update { it.copy(dayEndText = fmt(hour, minute)) }
    fun setNightStart(hour: Int, minute: Int) = _ui.update { it.copy(nightStartText = fmt(hour, minute)) }
    fun setNightEnd(hour: Int, minute: Int) = _ui.update { it.copy(nightEndText = fmt(hour, minute)) }

    fun setFirstDay(day: Int) {
        _ui.update { it.copy(firstDay = day, continuePrevious = false, phaseOffset = 0) }
        rebuildPreview(resetPhase = true)
    }

    fun setExtendToNextMonth(enabled: Boolean) = _ui.update { it.copy(extendToNextMonth = enabled) }

    fun goToStep(step: Int) = _ui.update { it.copy(step = step) }

    fun dismissSubscriptionLimit() = _ui.update { it.copy(showLimitDialog = false) }

    // ── §15.1 Продолжение графика прошлого месяца ───────────────────────────

    fun continuePreviousSchedule() {
        val rec = lastSchedule ?: return
        _ui.update { s ->
            val size = cycleSizeOf(rec.patternId, s.patterns, s.customCycle) ?: return@update s
            val phase = rec.nextPhase % size
            s.copy(
                selectedId = rec.patternId,
                isCustom = rec.patternId == WIZARD_CUSTOM_PATTERN_ID,
                dayStartText = rec.dayStart ?: s.dayStartText,
                dayEndText = rec.dayEnd ?: s.dayEndText,
                nightStartText = rec.nightStart ?: s.nightStartText,
                nightEndText = rec.nightEnd ?: s.nightEndText,
                continuePrevious = true,
                showContinuePreviousSheet = false,
                firstDay = 1,
                phaseOffset = phase,
                step = 2,
                pickerIndex = -1,
            )
        }
        rebuildPreview(resetPhase = false)
    }

    /** Гасит только шторку предложения — кнопка продолжения остаётся (§15.1). */
    fun declineContinuePrevious() = _ui.update { it.copy(showContinuePreviousSheet = false) }

    fun shiftMonth(delta: Int) {
        scope.launch {
            val current = month ?: return@launch
            val months = calendarUseCase.loadFlowMonthOfYearListState().first()
            val index = months.indexOfFirst { it.year == current.year && it.month == current.month }
            val next = months.getOrNull(index + delta) ?: return@launch
            month = next
            val days = iosDaysInMonth(next.year, next.month)
            _ui.update { s ->
                val canContinue = canContinue(lastSchedule, next, s.patterns, s.customCycle)
                val firstDay = s.firstDay.coerceAtMost(days)
                s.copy(
                    year = next.year,
                    month = next.month,
                    monthName = iosMonthName(next.month),
                    daysInMonth = days,
                    firstDay = firstDay,
                    canContinuePrevious = canContinue,
                    continuePrevious = false,
                    phaseOffset = 0,
                )
            }
            rebuildPreview(resetPhase = true)
        }
    }

    // ── apply() (§15.2) ─────────────────────────────────────────────────────

    fun apply() {
        val m = month ?: return
        val state = _ui.value
        val cycle = selectedCycle(state).mapNotNull { it.toShiftKind() }
        if (cycle.none { it != ShiftKind.OFF }) {
            _ui.update { it.copy(applyMessage = "В паттерне нет рабочих дней") }
            return
        }
        val (dsh, dsm) = parse(state.dayStartText)
        val dayDur = durationMin(state.dayStartText, state.dayEndText)
        val (nsh, nsm) = parse(state.nightStartText)
        val nightDur = durationMin(state.nightStartText, state.nightEndText)

        _ui.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val planned = mutableListOf<Route>()
                fun appendMonth(year: Int, monthIndex: Int, daysInMonth: Int, firstDay: Int, phaseOffset: Int) {
                    for (day in 1..daysInMonth) {
                        if (day < firstDay) continue
                        val kind = cycle[(phaseOffset + day - firstDay) % cycle.size]
                        val (sh, sm, dur) = when (kind) {
                            ShiftKind.DAY -> Triple(dsh, dsm, dayDur)
                            ShiftKind.NIGHT -> Triple(nsh, nsm, nightDur)
                            ShiftKind.OFF -> continue
                        }
                        val startMillis = iosToEpochMillis(year, monthIndex, day, sh, sm, setting)
                        planned += Route(
                            basicData = BasicData(
                                timeStartWork = startMillis,
                                timeEndWork = startMillis + dur * 60_000L,
                            )
                        )
                    }
                }
                appendMonth(m.year, m.month, state.daysInMonth, state.firstDay, state.phaseOffset)
                val currentConsumed = (state.daysInMonth - state.firstDay + 1).coerceAtLeast(0)
                val nextPhaseForExtension = (state.phaseOffset + currentConsumed) % cycle.size
                val nextYear = if (m.month == 11) m.year + 1 else m.year
                val nextMonth = if (m.month == 11) 0 else m.month + 1
                val nextDays = iosDaysInMonth(nextYear, nextMonth)
                if (state.extendToNextMonth) {
                    appendMonth(nextYear, nextMonth, nextDays, firstDay = 1, nextPhaseForExtension)
                }

                val g = gate.canCreateRoutes(planned.size)
                when (g.type) {
                    "limit" -> {
                        _ui.update {
                            it.copy(
                                isSaving = false,
                                showLimitDialog = true,
                                limitRequested = g.requested,
                                limitRemaining = g.remaining,
                                limitMessage = wizardLimitMessage(g.requested, g.remaining),
                            )
                        }
                        return@launch
                    }
                    "error" -> {
                        _ui.update {
                            it.copy(isSaving = false, applyMessage = "Не удалось проверить подписку")
                        }
                        return@launch
                    }
                }

                var created = 0
                val permanentPartner = partnerUseCase.getPermanent()
                for (route in planned) {
                    val res = routeUseCase.saveRoute(PartnerUseCase.withPermanentPartner(route, permanentPartner))
                        .first { it is ResultState.Success || it is ResultState.Error }
                    if (res is ResultState.Success) created++
                }

                val patternId = if (state.selectedId == WIZARD_CUSTOM_PATTERN_ID) {
                    saveCustomAsPattern(state.customCycle.mapNotNull { it.toShiftKind() })
                } else state.selectedId

                val nextPhase = if (state.extendToNextMonth) {
                    (nextPhaseForExtension + nextDays) % cycle.size
                } else nextPhaseForExtension
                val savedYear = if (state.extendToNextMonth) nextYear else m.year
                val savedMonth = if (state.extendToNextMonth) nextMonth else m.month
                prefs.setLastScheduleMonth(
                    listOf(
                        "$savedYear-$savedMonth",
                        patternId,
                        state.firstDay,
                        nextPhase,
                        state.dayStartText,
                        state.dayEndText,
                        state.nightStartText,
                        state.nightEndText,
                    ).joinToString("|")
                )

                val left = gate.freeRoutesLeft()
                _ui.update {
                    it.copy(
                        isSaving = false,
                        done = true,
                        applyMessage = when {
                            created == 0 -> "Не создано ни одного маршрута"
                            left == null -> "Создано черновиков маршрутов: $created"
                            else -> "Создано черновиков маршрутов: $created. " +
                                "Осталось бесплатных: $left из ${IosRouteGate.FREE_ROUTES_LIMIT}"
                        },
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ui.update {
                    it.copy(isSaving = false, applyMessage = "Ошибка: ${t.message ?: "неизвестная ошибка"}")
                }
            }
        }
    }

    /** Сбросить одноразовые поля (done / applyMessage) после показа. */
    fun consumeApplyMessage() = _ui.update { it.copy(applyMessage = "", done = false) }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun wizardLimitMessage(requested: Int, remaining: Int): String =
        if (remaining == 0) {
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

    private fun SchedulePattern.toUi() = WizardIosPattern(
        id = id,
        title = title,
        subtitle = subtitle,
        cycle = cycle.map { it.toKind() },
    )

    private fun WizardIosPattern.toDomain() = SchedulePattern(
        id = id,
        title = title,
        subtitle = subtitle,
        cycle = cycle.mapNotNull { it.toShiftKind() }.map { it.toWorkShiftType() },
    )

    private fun WorkShiftType.toKind(): String = when (this) {
        WorkShiftType.DAY -> "day"
        WorkShiftType.NIGHT -> "night"
        WorkShiftType.OFF -> "off"
    }

    private fun String.toShiftKind(): ShiftKind? = when (this) {
        "day" -> ShiftKind.DAY
        "night" -> ShiftKind.NIGHT
        "off" -> ShiftKind.OFF
        else -> null
    }

    private fun ShiftKind.toWorkShiftType(): WorkShiftType = when (this) {
        ShiftKind.DAY -> WorkShiftType.DAY
        ShiftKind.NIGHT -> WorkShiftType.NIGHT
        ShiftKind.OFF -> WorkShiftType.OFF
    }

    private fun cycleSizeOf(
        patternId: String,
        patterns: List<WizardIosPattern>,
        customCycle: List<String>,
    ): Int? = if (patternId == WIZARD_CUSTOM_PATTERN_ID) customCycle.size.takeIf { it > 0 }
    else patterns.find { it.id == patternId }?.cycle?.size?.takeIf { it > 0 }

    private fun parseLastSchedule(
        patterns: List<WizardIosPattern>,
        customCycle: List<String>,
    ): LastScheduleRecord? {
        val raw = prefs.getLastScheduleMonth()?.split('|') ?: return null
        val ym = raw.getOrNull(0)?.split('-') ?: return null
        val year = ym.getOrNull(0)?.toIntOrNull() ?: return null
        val monthIndex = ym.getOrNull(1)?.toIntOrNull() ?: return null
        val patternId = raw.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        val firstDay = raw.getOrNull(2)?.toIntOrNull() ?: 1
        val size = if (patternId == WIZARD_CUSTOM_PATTERN_ID) customCycle.size.takeIf { it > 0 }
        else patterns.find { it.id == patternId }?.cycle?.size?.takeIf { it > 0 }
        val nextPhase = raw.getOrNull(3)?.toIntOrNull() ?: run {
            if (size == null) return@run 0
            val days = iosDaysInMonth(year, monthIndex)
            (days - firstDay + 1).coerceAtLeast(0) % size
        }
        return LastScheduleRecord(
            year = year,
            month = monthIndex,
            patternId = patternId,
            firstDay = firstDay,
            nextPhase = if (size != null) nextPhase % size else nextPhase,
            dayStart = raw.getOrNull(4)?.takeIf { it.isNotBlank() },
            dayEnd = raw.getOrNull(5)?.takeIf { it.isNotBlank() },
            nightStart = raw.getOrNull(6)?.takeIf { it.isNotBlank() },
            nightEnd = raw.getOrNull(7)?.takeIf { it.isNotBlank() },
        )
    }

    /** Продолжение доступно только для ПРЕДЫДУЩЕГО месяца и существующего паттерна (§15.1). */
    private fun canContinue(
        record: LastScheduleRecord?,
        target: MonthOfYear,
        patterns: List<WizardIosPattern>,
        customCycle: List<String>,
    ): Boolean {
        val rec = record ?: return false
        val prevYear = if (target.month > 0) target.year else target.year - 1
        val prevMonth = if (target.month > 0) target.month - 1 else 11
        if (rec.year != prevYear || rec.month != prevMonth) return false
        return cycleSizeOf(rec.patternId, patterns, customCycle) != null
    }

    private fun selectedCycle(state: WizardIosUi): List<String> =
        if (state.selectedId == WIZARD_CUSTOM_PATTERN_ID) state.customCycle
        else state.patterns.find { it.id == state.selectedId }?.cycle ?: state.customCycle

    private fun rebuildPreview(resetPhase: Boolean) {
        _ui.update { s ->
            val phase = if (resetPhase) 0 else s.phaseOffset
            val cycle = selectedCycle(s).mapNotNull { it.toShiftKind() }
            val preview = if (s.daysInMonth == 0) emptyList()
            else if (cycle.isEmpty()) List(s.daysInMonth) { "off" }
            else (1..s.daysInMonth).map { day ->
                if (day < s.firstDay) "off"
                else cycle[(phase + day - s.firstDay) % cycle.size].toKindStr()
            }
            s.copy(
                phaseOffset = phase,
                preview = preview,
                previewShiftCount = preview.count { it == "day" || it == "night" },
                cycleCaption = cycleCaption(s),
                hasDayShift = cycle.contains(ShiftKind.DAY),
                hasNightShift = cycle.contains(ShiftKind.NIGHT),
            )
        }
    }

    private fun ShiftKind.toKindStr(): String = when (this) {
        ShiftKind.DAY -> "day"
        ShiftKind.NIGHT -> "night"
        ShiftKind.OFF -> "off"
    }

    private fun cycleCaption(state: WizardIosUi): String {
        val cycle = selectedCycle(state).mapNotNull { it.toShiftKind() }
        val d = cycle.count { it == ShiftKind.DAY }
        val n = cycle.count { it == ShiftKind.NIGHT }
        val o = cycle.count { it == ShiftKind.OFF }
        return "Цикл: $d ${iosPlural(d, "дневной", "дневных", "дневных")}, " +
            "$n ${iosPlural(n, "ночной", "ночных", "ночных")}, " +
            "$o ${iosPlural(o, "выходной", "выходных", "выходных")}. " +
            "Нажмите день, чтобы изменить тип или удалить."
    }

    /** «Свой» цикл → паттерн «{рабочих}/{выходных}»; id уже существующего с тем же циклом. */
    private fun saveCustomAsPattern(customCycle: List<ShiftKind>): String {
        val cycleTypes = customCycle.map { it.toWorkShiftType() }
        _ui.value.patterns.firstOrNull { p ->
            p.cycle.mapNotNull { it.toShiftKind() }.map { it.toWorkShiftType() } == cycleTypes
        }?.let { return it.id }
        val work = customCycle.count { it != ShiftKind.OFF }
        val off = customCycle.count { it == ShiftKind.OFF }
        val pattern = SchedulePattern(
            title = "$work/$off",
            subtitle = customCycle.joinToString(" · ") { shiftWord(it) },
            cycle = cycleTypes,
        )
        _ui.update { s ->
            val patterns = s.patterns + pattern.toUi()
            prefs.setSchedulePatterns(patterns.map { it.toDomain() })
            s.copy(patterns = patterns)
        }
        return pattern.id
    }

    private fun shiftWord(k: ShiftKind): String = when (k) {
        ShiftKind.DAY -> "день"
        ShiftKind.NIGHT -> "ночь"
        ShiftKind.OFF -> "вых"
    }

    private fun parse(text: String): Pair<Int, Int> {
        val parts = text.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    /** Длительность смены в минутах; ≤0 (через полночь) → +24 ч. */
    private fun durationMin(startText: String, endText: String): Int {
        val (sh, sm) = parse(startText)
        val (eh, em) = parse(endText)
        var d = (eh * 60 + em) - (sh * 60 + sm)
        if (d <= 0) d += 24 * 60
        return d
    }

    private fun fmt(hour: Int, minute: Int): String = "${iosTwo(hour)}:${iosTwo(minute)}"
}

/** Тип дня цикла (порт `ShiftKind`). */
enum class ShiftKind { DAY, NIGHT, OFF }
