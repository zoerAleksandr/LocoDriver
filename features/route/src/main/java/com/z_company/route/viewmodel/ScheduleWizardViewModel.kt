package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.SchedulePattern
import com.z_company.domain.entities.WorkShiftType
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException

/** Служебный id «плитки-конструктора» — свой цикл, ещё не сохранённый паттерном. */
const val CUSTOM_PATTERN_ID = "custom"

/**
 * Мастер «Заполнить месяц» — по паттерну графика раскладывает смены на месяц
 * и создаёт черновики маршрутов (Route с basicData.timeStartWork/timeEndWork)
 * на каждый рабочий день.
 *
 * Паттерны хранятся ЛОКАЛЬНО (SharedPreferences), список редактируемый:
 * стандартные (2/2, 4/4, 2/1) можно удалить, а свой цикл после применения
 * сохраняется новым паттерном. У дневной и ночной смены своё время.
 *
 * Соответствует дизайну schedule-wizard.jsx / schedule-wizard-step1.jsx.
 */
class ScheduleWizardViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val routeUseCase: RouteUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val routeHelper: RouteActionsHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val prefs: SharedPreferencesRepositories by inject()

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    private var converter: DateAndTimeConverter? = null
    private var month: MonthOfYear? = null

    /** Разобранная запись о последнем заполненном мастером месяце. */
    private var lastSchedule: LastScheduleRecord? = null

    /**
     * Что мастер запомнил о последнем заполненном месяце.
     *
     * [nextPhase] — индекс цикла, с которого должно начаться 1-е число
     * СЛЕДУЮЩЕГО месяца. Храним именно конечную фазу, а не «сколько дней
     * израсходовано»: так продолжение корректно склеивается в цепочку
     * (месяц, продолженный с фазы, сам отдаёт правильную фазу дальше).
     */
    private data class LastScheduleRecord(
        val year: Int,
        val month: Int,          // 0-based, как в MonthOfYear
        val patternId: String,
        val firstDay: Int,
        val nextPhase: Int,
        val dayStart: String?,
        val dayEnd: String?,
        val nightStart: String?,
        val nightEnd: String?,
    )

    private fun cycleSizeOf(
        patternId: String,
        patterns: List<SchedulePattern>,
        customCycle: List<ShiftKind>,
    ): Int? = if (patternId == CUSTOM_PATTERN_ID) customCycle.size.takeIf { it > 0 }
    else patterns.find { it.id == patternId }?.cycle?.size?.takeIf { it > 0 }

    /**
     * Формат записи: `year-month|patternId|firstDay|nextPhase|дн.начало|дн.конец|ноч.начало|ноч.конец`.
     *
     * Старые записи содержат только первые три поля — для них фазу
     * восстанавливаем из длины того месяца (как считал прежний код), а время
     * смен оставляем текущее.
     */
    private fun parseLastSchedule(
        patterns: List<SchedulePattern>,
        customCycle: List<ShiftKind>,
    ): LastScheduleRecord? {
        val raw = prefs.getLastScheduleMonth()?.split('|') ?: return null
        val ym = raw.getOrNull(0)?.split('-') ?: return null
        val year = ym.getOrNull(0)?.toIntOrNull() ?: return null
        val monthIndex = ym.getOrNull(1)?.toIntOrNull() ?: return null
        val patternId = raw.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
        val firstDay = raw.getOrNull(2)?.toIntOrNull() ?: 1
        val size = cycleSizeOf(patternId, patterns, customCycle)
        val nextPhase = raw.getOrNull(3)?.toIntOrNull() ?: run {
            if (size == null) return@run 0
            val days = LocalDate.of(year, monthIndex + 1, 1).lengthOfMonth()
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

    /**
     * Предложение продолжить график действует, только если мастером заполнен
     * именно ПРЕДЫДУЩИЙ месяц относительно выбранного (раньше подходил любой
     * ранее заполненный месяц — хоть годичной давности, хоть будущий) и если
     * тот паттерн ещё существует: без него фазу цикла продолжать не от чего.
     */
    private fun canContinue(
        record: LastScheduleRecord?,
        target: MonthOfYear,
        patterns: List<SchedulePattern>,
        customCycle: List<ShiftKind>,
    ): Boolean {
        val rec = record ?: return false
        val prevYear = if (target.month > 0) target.year else target.year - 1
        val prevMonth = if (target.month > 0) target.month - 1 else 11
        if (rec.year != prevYear || rec.month != prevMonth) return false
        return cycleSizeOf(rec.patternId, patterns, customCycle) != null
    }

    fun prepareScreen() {
        viewModelScope.launch {
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                converter = DateAndTimeConverter(setting)
                val m = setting.selectMonthOfYear
                month = m
                val daysInMonth = LocalDate.of(m.year, m.month + 1, 1).lengthOfMonth()

                // Загружаем сохранённые паттерны; при первом запуске сидим дефолты.
                val patterns = prefs.getSchedulePatterns() ?: SchedulePattern.defaults().also {
                    prefs.setSchedulePatterns(it)
                }
                val selectedId = patterns.firstOrNull()?.id ?: CUSTOM_PATTERN_ID
                val customCycle = _uiState.value.customCycle
                val record = parseLastSchedule(patterns, customCycle)
                lastSchedule = record
                val canContinue = canContinue(record, m, patterns, customCycle)

                _uiState.update {
                    it.copy(
                        year = m.year,
                        month = m.month,
                        monthName = monthName(m.month),
                        daysInMonth = daysInMonth,
                        patterns = patterns,
                        selectedId = selectedId,
                        canContinuePrevious = canContinue,
                        showContinuePreviousSheet = canContinue,
                        previousMonthName = record?.let { r -> monthName(r.month) }.orEmpty(),
                        continuePrevious = false,
                        phaseOffset = 0,
                        preview = buildPreview(selectedId, patterns, it.firstDay, daysInMonth, it.customCycle, 0),
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("ScheduleWizardViewModel", "prepareScreen")
            }
        }
    }

    fun selectPattern(id: String) = _uiState.update {
        // Выбор паттерна вручную выходит из режима продолжения: фаза считалась
        // для цикла прошлого месяца и к другому циклу неприменима.
        it.copy(
            selectedId = id,
            pickerIndex = null,
            continuePrevious = false,
            phaseOffset = 0,
            preview = buildPreview(id, it.patterns, it.firstDay, it.daysInMonth, it.customCycle, 0),
        )
    }

    /** Удалить паттерн из списка (и из хранилища). «Свой»-конструктор не удаляется. */
    fun deletePattern(id: String) = _uiState.update { s ->
        if (id == CUSTOM_PATTERN_ID) return@update s
        val patterns = s.patterns.filterNot { it.id == id }
        prefs.setSchedulePatterns(patterns)
        val newSelected = if (s.selectedId == id) {
            patterns.firstOrNull()?.id ?: CUSTOM_PATTERN_ID
        } else s.selectedId
        s.copy(
            patterns = patterns,
            selectedId = newSelected,
            pickerIndex = null,
            preview = buildPreview(newSelected, patterns, s.firstDay, s.daysInMonth, s.customCycle, 0),
        )
    }

    // ── Редактор «Свой» ───────────────────────────────────────────
    /** Тап по дню открывает пикер типа (единственный способ правки дня). */
    fun openTypePicker(index: Int) = _uiState.update {
        it.copy(pickerIndex = if (it.pickerIndex == index) null else index)
    }

    fun closeTypePicker() = _uiState.update { it.copy(pickerIndex = null) }

    /** Явно задать тип дня из пикера (без перебора). */
    fun setCycleDayType(index: Int, kind: ShiftKind) = _uiState.update { s ->
        val cycle = s.customCycle.toMutableList()
        if (index in cycle.indices) cycle[index] = kind
        s.copy(
            customCycle = cycle,
            continuePrevious = false,
            phaseOffset = 0,
            preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle, 0),
        )
    }

    fun addCycleDay() = _uiState.update { s ->
        val cycle = s.customCycle + ShiftKind.DAY
        // Сразу открываем пикер типа для нового дня — следующим тапом задаётся тип.
        s.copy(
            customCycle = cycle,
            pickerIndex = cycle.lastIndex,
            continuePrevious = false,
            phaseOffset = 0,
            preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle, 0),
        )
    }

    fun removeCycleDay(index: Int) = _uiState.update { s ->
        if (s.customCycle.size <= 1) return@update s
        val cycle = s.customCycle.toMutableList().apply { removeAt(index) }
        s.copy(
            customCycle = cycle,
            pickerIndex = null,
            continuePrevious = false,
            phaseOffset = 0,
            preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle, 0),
        )
    }

    fun setDayStart(hour: Int, minute: Int) = _uiState.update { it.copy(dayStartText = fmt(hour, minute)) }
    fun setDayEnd(hour: Int, minute: Int) = _uiState.update { it.copy(dayEndText = fmt(hour, minute)) }
    fun setNightStart(hour: Int, minute: Int) = _uiState.update { it.copy(nightStartText = fmt(hour, minute)) }
    fun setNightEnd(hour: Int, minute: Int) = _uiState.update { it.copy(nightEndText = fmt(hour, minute)) }

    fun setFirstDay(day: Int) = _uiState.update {
        // Ручной выбор первого дня — тоже выход из режима продолжения.
        it.copy(
            firstDay = day,
            continuePrevious = false,
            phaseOffset = 0,
            preview = buildPreview(it.selectedId, it.patterns, day, it.daysInMonth, it.customCycle, 0),
        )
    }

    fun setExtendToNextMonth(enabled: Boolean) = _uiState.update {
        it.copy(extendToNextMonth = enabled)
    }

    /**
     * Продолжить график прошлого месяца: берём тот же паттерн и то же время
     * смен, ставим первый день = 1 и сдвигаем цикл на сохранённую фазу, чтобы
     * он перетёк в новый месяц без разрыва, а не начинался заново.
     */
    fun continuePreviousSchedule() {
        val rec = lastSchedule ?: return
        _uiState.update { s ->
            val size = cycleSizeOf(rec.patternId, s.patterns, s.customCycle) ?: return@update s
            val phase = rec.nextPhase % size
            s.copy(
                selectedId = rec.patternId,
                dayStartText = rec.dayStart ?: s.dayStartText,
                dayEndText = rec.dayEnd ?: s.dayEndText,
                nightStartText = rec.nightStart ?: s.nightStartText,
                nightEndText = rec.nightEnd ?: s.nightEndText,
                continuePrevious = true,
                showContinuePreviousSheet = false,
                firstDay = 1,
                phaseOffset = phase,
                step = 2,
                pickerIndex = null,
                preview = buildPreview(rec.patternId, s.patterns, 1, s.daysInMonth, s.customCycle, phase),
            )
        }
    }

    /**
     * «Выбрать заново» / закрытие шторки «Продолжить график прошлого месяца?».
     *
     * Гасим только шторку: сама возможность продолжить остаётся кнопкой на шаге
     * выбора графика. Сбрасывать флаг обязательно — иначе шторка остаётся в
     * композиции, её scrim перехватывает нажатия и экран мастера зависает.
     */
    fun declineContinuePrevious() = _uiState.update { it.copy(showContinuePreviousSheet = false) }

    fun shiftMonth(delta: Int) {
        viewModelScope.launch {
            val current = month ?: return@launch
            val months = calendarMonths()
            val index = months.indexOfFirst { it.year == current.year && it.month == current.month }
            val next = months.getOrNull(index + delta) ?: return@launch
            month = next
            val days = LocalDate.of(next.year, next.month + 1, 1).lengthOfMonth()
            _uiState.update { s ->
                // Фаза считалась для прежнего месяца — при смене месяца режим
                // продолжения сбрасывается, а доступность пересчитывается заново.
                val canContinue = canContinue(lastSchedule, next, s.patterns, s.customCycle)
                val firstDay = s.firstDay.coerceAtMost(days)
                s.copy(
                    year = next.year,
                    month = next.month,
                    monthName = monthName(next.month),
                    daysInMonth = days,
                    firstDay = firstDay,
                    canContinuePrevious = canContinue,
                    continuePrevious = false,
                    phaseOffset = 0,
                    preview = buildPreview(s.selectedId, s.patterns, firstDay, days, s.customCycle, 0),
                )
            }
        }
    }

    private suspend fun calendarMonths(): List<MonthOfYear> =
        settingsUseCase.getUserSettingFlow().first().let { calendarUseCase.loadFlowMonthOfYearListState().first() }

    fun goToStep(step: Int) = _uiState.update { it.copy(step = step) }

    fun dismissSubscriptionLimit() = _uiState.update { it.copy(subscriptionLimit = null) }

    /** Раскладывает выбранный паттерн на месяц и создаёт черновики на рабочие дни. */
    fun apply() {
        val m = month ?: return
        val conv = converter ?: return
        val state = _uiState.value
        val cycle = selectedCycle(state)
        if (cycle.none { it != ShiftKind.OFF }) {
            snackbarManager.show("В паттерне нет рабочих дней")
            return
        }
        val (dsh, dsm) = parse(state.dayStartText)
        val dayDur = durationMin(state.dayStartText, state.dayEndText)
        val (nsh, nsm) = parse(state.nightStartText)
        val nightDur = durationMin(state.nightStartText, state.nightEndText)

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Сначала раскладываем месяц в список маршрутов, и только потом
                // проверяем лимит: гейт должен знать размер пачки. Со старой
                // проверкой «можно ли ещё один маршрут» мастер создавал без
                // подписки сколько угодно — лимит обходился целиком.
                val planned = mutableListOf<Route>()
                fun appendMonth(
                    year: Int,
                    monthIndex: Int,
                    daysInMonth: Int,
                    firstDay: Int,
                    phaseOffset: Int,
                ) {
                    for (day in 1..daysInMonth) {
                        if (day < firstDay) continue
                        val kind = cycle[(phaseOffset + day - firstDay) % cycle.size]
                    val (sh, sm, dur) = when (kind) {
                        ShiftKind.DAY -> Triple(dsh, dsm, dayDur)
                        ShiftKind.NIGHT -> Triple(nsh, nsm, nightDur)
                        ShiftKind.OFF -> continue
                    }
                    // ПРАВИЛО часовых поясов (см. CODEBASE.md «Часовые пояса»):
                    // время явки сохраняется по московскому (displayTimeZone) —
                    // тот же путь, что у ручной формы. Ночные/праздничные (местный
                    // ЧП) и переходные (настройка Норма/Регион) считаются ниже по
                    // стеку из сохранённого instant через TimeCalculationContext.
                    val startMillis = conv.toEpochMillis(year, monthIndex, day, sh, sm)
                    val endMillis = startMillis + dur * 60_000L
                    planned += Route(
                        basicData = BasicData(timeStartWork = startMillis, timeEndWork = endMillis)
                    )
                }
                }

                appendMonth(m.year, m.month, state.daysInMonth, state.firstDay, state.phaseOffset)
                val currentConsumed = (state.daysInMonth - state.firstDay + 1).coerceAtLeast(0)
                val nextPhaseForExtension = (state.phaseOffset + currentConsumed) % cycle.size
                val nextYear = if (m.month == 11) m.year + 1 else m.year
                val nextMonth = if (m.month == 11) 0 else m.month + 1
                val nextDays = LocalDate.of(nextYear, nextMonth + 1, 1).lengthOfMonth()
                if (state.extendToNextMonth) {
                    appendMonth(nextYear, nextMonth, nextDays, firstDay = 1, nextPhaseForExtension)
                }

                // Гейт на всю пачку — та же проверка, что в Календаре.
                when (val gate = routeHelper.canCreateRoutes(planned.size)) {
                    is RouteActionsHelper.BatchRoutesResult.LimitExceeded -> {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                subscriptionLimit = SubscriptionLimitState(
                                    requested = gate.requested,
                                    remaining = gate.remaining,
                                ),
                            )
                        }
                        return@launch
                    }
                    is RouteActionsHelper.BatchRoutesResult.Error -> {
                        snackbarManager.show("Не удалось проверить подписку")
                        _uiState.update { it.copy(isSaving = false) }
                        return@launch
                    }
                    is RouteActionsHelper.BatchRoutesResult.Allowed -> { /* можно создавать */ }
                }

                var created = 0
                for (route in planned) {
                    val res = routeUseCase.saveRoute(route)
                        .first { it is ResultState.Success || it is ResultState.Error }
                    if (res is ResultState.Success) created++
                }

                // Свой цикл после применения — сохраняем паттерном (если ещё не сохранён).
                val patternId = if (state.selectedId == CUSTOM_PATTERN_ID) {
                    saveCustomAsPattern(state.customCycle)
                } else state.selectedId

                // Запоминаем месяц вместе с фазой, на которой цикл закончился, и
                // временем смен — чтобы следующий месяц продолжился без разрыва.
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

                val left = routeHelper.freeRoutesLeft()
                snackbarManager.show(
                    when {
                        created == 0 -> "Не создано ни одного маршрута"
                        left == null -> "Создано черновиков маршрутов: $created"
                        else -> "Создано черновиков маршрутов: $created. " +
                            "Осталось бесплатных: $left из ${RouteActionsHelper.FREE_ROUTES_LIMIT}"
                    }
                )
                _uiState.update { it.copy(isSaving = false, done = true) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("ScheduleWizardViewModel", "apply")
                snackbarManager.show("Ошибка: ${t.message ?: t::class.simpleName}")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    // ── helpers ──────────────────────────────────────────────────
    /** Возвращает id паттерна — сохранённого только что либо уже существующего
     *  с таким же циклом. Нужен, чтобы записать в «последний месяц» реальный id,
     *  а не служебный [CUSTOM_PATTERN_ID], иначе продолжить график не выйдет. */
    private fun saveCustomAsPattern(customCycle: List<ShiftKind>): String {
        val cycleTypes = customCycle.map { it.toWorkShiftType() }
        _uiState.value.patterns.firstOrNull { it.cycle == cycleTypes }?.let { return it.id }
        val work = customCycle.count { it != ShiftKind.OFF }
        val off = customCycle.count { it == ShiftKind.OFF }
        val pattern = SchedulePattern(
            title = "$work/$off",
            subtitle = customCycle.joinToString(" · ") { shiftWord(it) },
            cycle = cycleTypes,
        )
        _uiState.update { s ->
            val patterns = s.patterns + pattern
            prefs.setSchedulePatterns(patterns)
            s.copy(patterns = patterns)
        }
        return pattern.id
    }

    private fun selectedCycle(state: WizardUiState): List<ShiftKind> =
        if (state.selectedId == CUSTOM_PATTERN_ID) state.customCycle
        else state.patterns.find { it.id == state.selectedId }?.cycle?.map { it.toShiftKind() }
            ?: state.customCycle

    /**
     * [phaseOffset] — с какого индекса цикла начинается [firstDay]. Ненулевой
     * только при продолжении графика прошлого месяца. Без него предпросмотр
     * рисовал цикл заново с первого числа, а `apply()` раскладывал со сдвигом —
     * то есть показывал не то, что создавал.
     */
    private fun buildPreview(
        selectedId: String,
        patterns: List<SchedulePattern>,
        firstDay: Int,
        daysInMonth: Int,
        customCycle: List<ShiftKind>,
        phaseOffset: Int,
    ): List<ShiftKind> {
        if (daysInMonth == 0) return emptyList()
        val cycle = if (selectedId == CUSTOM_PATTERN_ID) customCycle
        else patterns.find { it.id == selectedId }?.cycle?.map { it.toShiftKind() } ?: customCycle
        if (cycle.isEmpty()) return List(daysInMonth) { ShiftKind.OFF }
        return (1..daysInMonth).map { day ->
            if (day < firstDay) ShiftKind.OFF
            else cycle[(phaseOffset + day - firstDay) % cycle.size]
        }
    }

    private fun parse(text: String): Pair<Int, Int> {
        val parts = text.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    /** Длительность смены в минутах; отрицательная (через полночь) → +24ч. */
    private fun durationMin(startText: String, endText: String): Int {
        val (sh, sm) = parse(startText)
        val (eh, em) = parse(endText)
        var d = (eh * 60 + em) - (sh * 60 + sm)
        if (d <= 0) d += 24 * 60
        return d
    }

    private fun fmt(hour: Int, minute: Int): String =
        "${if (hour < 10) "0$hour" else hour}:${if (minute < 10) "0$minute" else minute}"

    private fun shiftWord(k: ShiftKind): String = when (k) {
        ShiftKind.DAY -> "день"
        ShiftKind.NIGHT -> "ночь"
        ShiftKind.OFF -> "вых"
    }

    private fun monthName(index: Int): String = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    ).getOrElse(index) { "" }
}

enum class ShiftKind { DAY, NIGHT, OFF }

private fun ShiftKind.toWorkShiftType(): WorkShiftType = when (this) {
    ShiftKind.DAY -> WorkShiftType.DAY
    ShiftKind.NIGHT -> WorkShiftType.NIGHT
    ShiftKind.OFF -> WorkShiftType.OFF
}

private fun WorkShiftType.toShiftKind(): ShiftKind = when (this) {
    WorkShiftType.DAY -> ShiftKind.DAY
    WorkShiftType.NIGHT -> ShiftKind.NIGHT
    WorkShiftType.OFF -> ShiftKind.OFF
}

data class WizardUiState(
    val step: Int = 1,                 // 1 | 2
    val patterns: List<SchedulePattern> = emptyList(),
    val selectedId: String = CUSTOM_PATTERN_ID,
    val pickerIndex: Int? = null,      // индекс дня цикла с открытым пикером типа
    val dayStartText: String = "08:00",
    val dayEndText: String = "20:00",
    val nightStartText: String = "20:00",
    val nightEndText: String = "08:00",
    val firstDay: Int = 1,
    /** Создать маршруты следующего месяца, не сбрасывая фазу выбранного цикла. */
    val extendToNextMonth: Boolean = false,
    /** Прошлый месяц заполнен мастером — продолжение доступно (кнопка на шаге 1). */
    val canContinuePrevious: Boolean = false,
    /** Показывать шторку с предложением; гаснет после выбора, кнопка остаётся. */
    val showContinuePreviousSheet: Boolean = false,
    /** Название прошлого месяца — для подписи предложения. */
    val previousMonthName: String = "",
    val continuePrevious: Boolean = false,
    /** Индекс цикла для [firstDay]; ненулевой только в режиме продолжения. */
    val phaseOffset: Int = 0,
    val year: Int = 0,
    val month: Int = 0,
    val monthName: String = "",
    val daysInMonth: Int = 0,
    val preview: List<ShiftKind> = emptyList(),
    val customCycle: List<ShiftKind> = listOf(ShiftKind.DAY, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF),
    val isSaving: Boolean = false,
    val done: Boolean = false,
    /** Пачка не помещается в бесплатный лимит — показать диалог о подписке. */
    val subscriptionLimit: SubscriptionLimitState? = null,
) {
    /** Типы смен выбранного паттерна (для показа карточек времени и предпросмотра). */
    val selectedCycleKinds: List<ShiftKind>
        get() = if (selectedId == CUSTOM_PATTERN_ID) customCycle
        else patterns.find { it.id == selectedId }?.cycle?.map {
            when (it) {
                WorkShiftType.DAY -> ShiftKind.DAY
                WorkShiftType.NIGHT -> ShiftKind.NIGHT
                WorkShiftType.OFF -> ShiftKind.OFF
            }
        } ?: emptyList()

    val isCustom: Boolean get() = selectedId == CUSTOM_PATTERN_ID
    val hasDayShift: Boolean get() = selectedCycleKinds.contains(ShiftKind.DAY)
    val hasNightShift: Boolean get() = selectedCycleKinds.contains(ShiftKind.NIGHT)
}
