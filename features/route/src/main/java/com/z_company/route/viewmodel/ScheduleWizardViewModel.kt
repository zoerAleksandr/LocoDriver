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
    private val routeHelper: RouteActionsHelper by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val prefs: SharedPreferencesRepositories by inject()

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    private var converter: DateAndTimeConverter? = null
    private var month: MonthOfYear? = null

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

                _uiState.update {
                    it.copy(
                        year = m.year,
                        month = m.month,
                        monthName = monthName(m.month),
                        daysInMonth = daysInMonth,
                        patterns = patterns,
                        selectedId = selectedId,
                        preview = buildPreview(selectedId, patterns, it.firstDay, daysInMonth, it.customCycle),
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("ScheduleWizardViewModel", "prepareScreen")
            }
        }
    }

    fun selectPattern(id: String) = _uiState.update {
        it.copy(
            selectedId = id,
            pickerIndex = null,
            preview = buildPreview(id, it.patterns, it.firstDay, it.daysInMonth, it.customCycle),
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
            preview = buildPreview(newSelected, patterns, s.firstDay, s.daysInMonth, s.customCycle),
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
        s.copy(customCycle = cycle, preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle))
    }

    fun addCycleDay() = _uiState.update { s ->
        val cycle = s.customCycle + ShiftKind.DAY
        s.copy(customCycle = cycle, preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle))
    }

    fun removeCycleDay(index: Int) = _uiState.update { s ->
        if (s.customCycle.size <= 1) return@update s
        val cycle = s.customCycle.toMutableList().apply { removeAt(index) }
        s.copy(
            customCycle = cycle,
            pickerIndex = null,
            preview = buildPreview(s.selectedId, s.patterns, s.firstDay, s.daysInMonth, cycle),
        )
    }

    fun setDayStart(hour: Int, minute: Int) = _uiState.update { it.copy(dayStartText = fmt(hour, minute)) }
    fun setDayEnd(hour: Int, minute: Int) = _uiState.update { it.copy(dayEndText = fmt(hour, minute)) }
    fun setNightStart(hour: Int, minute: Int) = _uiState.update { it.copy(nightStartText = fmt(hour, minute)) }
    fun setNightEnd(hour: Int, minute: Int) = _uiState.update { it.copy(nightEndText = fmt(hour, minute)) }

    fun setFirstDay(day: Int) = _uiState.update {
        it.copy(firstDay = day, preview = buildPreview(it.selectedId, it.patterns, day, it.daysInMonth, it.customCycle))
    }

    fun goToStep(step: Int) = _uiState.update { it.copy(step = step) }

    fun resetNeedSubscription() = _uiState.update { it.copy(needSubscription = false) }

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
                // Гейт подписки — та же логика, что при создании маршрута вручную.
                when (routeHelper.newRouteClick()) {
                    is RouteActionsHelper.NewRouteResult.NeedSubscribeDialog -> {
                        snackbarManager.show("Заполнение месяца доступно по подписке")
                        _uiState.update { it.copy(isSaving = false, needSubscription = true) }
                        return@launch
                    }
                    is RouteActionsHelper.NewRouteResult.Error -> {
                        snackbarManager.show("Не удалось проверить подписку")
                        _uiState.update { it.copy(isSaving = false) }
                        return@launch
                    }
                    else -> { /* можно создавать */ }
                }

                var created = 0
                for (day in 1..state.daysInMonth) {
                    if (day < state.firstDay) continue
                    val kind = cycle[(day - state.firstDay) % cycle.size]
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
                    val startMillis = conv.toEpochMillis(m.year, m.month, day, sh, sm)
                    val endMillis = startMillis + dur * 60_000L
                    val route = Route(
                        basicData = BasicData(timeStartWork = startMillis, timeEndWork = endMillis)
                    )
                    val res = routeUseCase.saveRoute(route)
                        .first { it is ResultState.Success || it is ResultState.Error }
                    if (res is ResultState.Success) created++
                }

                // Свой цикл после применения — сохраняем паттерном (если ещё не сохранён).
                if (state.selectedId == CUSTOM_PATTERN_ID) {
                    saveCustomAsPattern(state.customCycle)
                }

                snackbarManager.show(
                    if (created > 0) "Создано черновиков маршрутов: $created"
                    else "Не создано ни одного маршрута"
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
    private fun saveCustomAsPattern(customCycle: List<ShiftKind>) {
        val cycleTypes = customCycle.map { it.toWorkShiftType() }
        _uiState.update { s ->
            // Не дублируем — если такой цикл уже есть, ничего не добавляем.
            if (s.patterns.any { it.cycle == cycleTypes }) return@update s
            val work = customCycle.count { it != ShiftKind.OFF }
            val off = customCycle.count { it == ShiftKind.OFF }
            val pattern = SchedulePattern(
                title = "$work/$off",
                subtitle = customCycle.joinToString(" · ") { shiftWord(it) },
                cycle = cycleTypes,
            )
            val patterns = s.patterns + pattern
            prefs.setSchedulePatterns(patterns)
            s.copy(patterns = patterns)
        }
    }

    private fun selectedCycle(state: WizardUiState): List<ShiftKind> =
        if (state.selectedId == CUSTOM_PATTERN_ID) state.customCycle
        else state.patterns.find { it.id == state.selectedId }?.cycle?.map { it.toShiftKind() }
            ?: state.customCycle

    private fun buildPreview(
        selectedId: String,
        patterns: List<SchedulePattern>,
        firstDay: Int,
        daysInMonth: Int,
        customCycle: List<ShiftKind>,
    ): List<ShiftKind> {
        if (daysInMonth == 0) return emptyList()
        val cycle = if (selectedId == CUSTOM_PATTERN_ID) customCycle
        else patterns.find { it.id == selectedId }?.cycle?.map { it.toShiftKind() } ?: customCycle
        if (cycle.isEmpty()) return List(daysInMonth) { ShiftKind.OFF }
        return (1..daysInMonth).map { day ->
            if (day < firstDay) ShiftKind.OFF
            else cycle[(day - firstDay) % cycle.size]
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
    val year: Int = 0,
    val month: Int = 0,
    val monthName: String = "",
    val daysInMonth: Int = 0,
    val preview: List<ShiftKind> = emptyList(),
    val customCycle: List<ShiftKind> = listOf(ShiftKind.DAY, ShiftKind.NIGHT, ShiftKind.OFF, ShiftKind.OFF),
    val isSaving: Boolean = false,
    val done: Boolean = false,
    val needSubscription: Boolean = false,
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
