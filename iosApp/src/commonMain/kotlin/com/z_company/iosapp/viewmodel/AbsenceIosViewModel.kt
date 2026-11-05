package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * KMP ViewModel экрана «Новое отвлечение» (SCREEN_SPECS §16.1, §16.4) — порт
 * `AbsenceViewModel`. Диапазон дат независим от показанного месяца шторки.
 *
 * BUG-46 / BUG-47 / BUG-48 перенесены как есть (паритет с Android):
 * - существующие записи на даты не удаляются (может быть дубль) — BUG-46;
 * - склонение «день/дня/дней» даёт «21 дней» — BUG-47;
 * - после сохранения выбранным месяцем становится последний затронутый — BUG-48.
 */
class AbsenceIosViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val secureTokenStorage: SecureTokenStorage,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _ui = MutableStateFlow(AbsenceIosUi())

    private var monthsCache: Map<Pair<Int, Int>, MonthOfYear> = emptyMap()
    private var rangeStart: LocalDate? = null
    private var rangeEnd: LocalDate? = null

    /** Типы для выбора (без «Выходного» и «Технических занятий» — они из Календаря, §14.3). */
    val typeOptions: List<AbsenceIosTypeOption> = listOf(
        AbsenceIosTypeOption("vacation", ReleaseType.Vacation.text),
        AbsenceIosTypeOption("sick", ReleaseType.SickLeave.text),
        AbsenceIosTypeOption("courses", ReleaseType.Courses.text),
        AbsenceIosTypeOption("donor", ReleaseType.Donor.text),
        AbsenceIosTypeOption("childCare", ReleaseType.ChildCare.text),
        AbsenceIosTypeOption("businessTrip", ReleaseType.BusinessTrip.text),
        AbsenceIosTypeOption("other", ReleaseType.Other.text),
    )

    fun watchUi(callback: (AbsenceIosUi) -> Unit) {
        scope.launch { _ui.collect { callback(it) } }
    }

    fun prepareScreen() {
        scope.launch {
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val base = setting.selectMonthOfYear
                val allMonths = runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                }.getOrNull().orEmpty()
                monthsCache = allMonths.associateBy { it.year to it.month }
                val merged = monthsCache[base.year to base.month] ?: base
                applyMonth(merged)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    private fun applyMonth(found: MonthOfYear) {
        val daysInMonth = iosDaysInMonth(found.year, found.month)
        val existing = found.days
            .filter { it.isReleaseDay && it.releaseType != null }
            .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
        _ui.update {
            it.copy(
                isLoading = false,
                year = found.year,
                month = found.month,
                monthLabel = "${iosMonthName(found.month)} ${found.year}",
                days = buildDays(found.year, found.month, daysInMonth, existing),
            ).withSelection(rangeStart, rangeEnd)
        }
    }

    /** Листание месяца шторки — диапазон дат не сбрасывается (§16.1). */
    fun shiftMonth(delta: Int) {
        val s = _ui.value
        var y = s.year
        var m = s.month + delta
        if (m < 0) { m = 11; y -= 1 }
        if (m > 11) { m = 0; y += 1 }
        val found = monthsCache[y to m]
        if (found == null) {
            _ui.update { it.copy(errorMessage = "Нет данных календаря за этот месяц") }
            return
        }
        applyMonth(found)
    }

    /** Тап по дню: 1-й — однодневный, 2-й — диапазон, 3-й — новый однодневный (§16.4). */
    fun onDayTap(day: Int) {
        val s = _ui.value
        val tapped = LocalDate(s.year, s.month + 1, day)
        val start = rangeStart
        val end = rangeEnd
        when {
            start == null -> { rangeStart = tapped; rangeEnd = tapped }
            start == end -> {
                rangeStart = minOf(start, tapped)
                rangeEnd = maxOf(start, tapped)
            }
            else -> { rangeStart = tapped; rangeEnd = tapped }
        }
        _ui.update { it.withSelection(rangeStart, rangeEnd) }
    }

    fun setType(kind: String) {
        _ui.update {
            it.copy(selectedKind = kind, typePickerOpen = false)
                .withSelection(rangeStart, rangeEnd)
        }
    }

    fun toggleTypePicker() = _ui.update { it.copy(typePickerOpen = !it.typePickerOpen) }

    fun save() {
        val start = rangeStart
        if (start == null) {
            _ui.update { it.copy(errorMessage = "Выберите даты") }
            return
        }
        val end = rangeEnd ?: start
        _ui.update { it.copy(isSaving = true) }
        scope.launch {
            try {
                val lo = minOf(start, end)
                val hi = maxOf(start, end)
                val days = generateSequence(lo) { it.plus(1, DateTimeUnit.DAY) }
                    .takeWhile { it <= hi }
                    .toList()
                val type = typeFromKind(_ui.value.selectedKind)
                val saveResult = releaseDayUseCase.savePeriod(
                    ReleasePeriod(days = days, type = type)
                ).first { it is ResultState.Success || it is ResultState.Error }
                if (saveResult is ResultState.Success) autoPushSettings()
                // Каждый затронутый месяц синхронизируем в settings.selectMonthOfYear.
                // BUG-48 (паритет с Android): выбранным становится последний затронутый месяц.
                runCatching { calendarUseCase.loadFlowMonthOfYearListState().first() }
                    .getOrNull()?.let { allMonths ->
                        val affected = days.map { it.year to (it.monthNumber - 1) }.distinct()
                        affected.forEach { (y, m) ->
                            allMonths.find { it.year == y && it.month == m }?.let { merged ->
                                settingsUseCase.setCurrentMonthOfYear(merged).first()
                            }
                        }
                    }
                _ui.update { it.copy(isSaving = false, done = true) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ui.update {
                    it.copy(isSaving = false, errorMessage = "Не удалось сохранить отвлечение")
                }
            }
        }
    }

    /** Одноразовое сообщение об ошибке — Swift показывает snackbar и вызывает consumeError(). */
    fun consumeError() = _ui.update { it.copy(errorMessage = "") }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun typeFromKind(kind: String): ReleaseType = when (kind) {
        "vacation" -> ReleaseType.Vacation
        "sick" -> ReleaseType.SickLeave
        "courses" -> ReleaseType.Courses
        "donor" -> ReleaseType.Donor
        "childCare" -> ReleaseType.ChildCare
        "businessTrip" -> ReleaseType.BusinessTrip
        else -> ReleaseType.Other
    }

    private fun kindOfType(type: ReleaseType): String = type.iosKind()

    private fun buildDays(
        year: Int,
        month0: Int,
        daysInMonth: Int,
        existing: Map<Int, ReleaseType>,
    ): List<AbsenceIosDay> = (1..daysInMonth).map { day ->
        val date = LocalDate(year, month0 + 1, day)
        val weekdayIndex = (date.dayOfWeek.ordinal + 6) % 7
        AbsenceIosDay(
            day = day,
            weekdayIndex = weekdayIndex,
            isWeekend = weekdayIndex >= 5,
            selection = null,
            existingBadge = existing[day]?.iosBadge(),
        )
    }

    private fun AbsenceIosUi.withSelection(start: LocalDate?, end: LocalDate?): AbsenceIosUi {
        val hours = computeHours(start, end, typeFromKind(selectedKind))
        val daysCount = if (start == null || end == null) 0
        else (end.toEpochDays() - start.toEpochDays()).toInt() + 1
        val selectionKind = when {
            start == null -> "none"
            start == end -> "single"
            else -> "range"
        }
        val mapped = days.map { d ->
            val date = LocalDate(year, month + 1, d.day)
            val sel = when {
                start == null -> null
                end == null -> null
                start == end && date == start -> "single"
                date == start -> "start"
                date == end -> "end"
                date > start && date < end -> "inside"
                else -> null
            }
            d.copy(selection = sel)
        }
        return copy(
            selectionKind = selectionKind,
            days = mapped,
            summaryRangeText = rangeText(start, end),
            summaryDays = daysCount,
            summaryHoursText = "$hours:00",
            summaryHoursCaption = if (selectedKind == "childCare") "по всем дням" else "по рабочим дням",
        )
    }

    private fun rangeText(start: LocalDate?, end: LocalDate?): String {
        if (start == null) return "Не выбрано"
        val e = end ?: start
        val sm = iosMonthName(start.monthNumber - 1)
        val em = iosMonthName(e.monthNumber - 1)
        return when {
            start == e -> "${start.dayOfMonth} $sm"
            start.monthNumber == e.monthNumber && start.year == e.year ->
                "${start.dayOfMonth}–${e.dayOfMonth} $sm"
            else -> {
                val sy = if (start.year != e.year) " ${start.year}" else ""
                val ey = if (start.year != e.year) " ${e.year}" else ""
                "${start.dayOfMonth} $sm$sy – ${e.dayOfMonth} $em$ey"
            }
        }
    }

    /** Норма-часы за диапазон: рабочие дни («по уходу» — все дни), §16.1. */
    private fun computeHours(start: LocalDate?, end: LocalDate?, type: ReleaseType): Int {
        if (start == null || end == null) return 0
        val lo = minOf(start, end)
        val hi = maxOf(start, end)
        var total = 0
        var d = lo
        while (d <= hi) {
            total += hoursForDay(d, tagFor(d), type)
            d = d.plus(1, DateTimeUnit.DAY)
        }
        return total
    }

    private fun tagFor(date: LocalDate): TagForDay =
        monthsCache[date.year to date.monthNumber - 1]?.days
            ?.find { it.dayOfMonth == date.dayOfMonth }?.tag
            ?: TagForDay.WORKING_DAY

    private fun hoursForDay(date: LocalDate, tag: TagForDay, type: ReleaseType): Int {
        if (type == ReleaseType.TechnicalStudy) return 0
        return iosReleaseHoursForDay(type, date, tag, sharedPrefs.getWorkScheduleProfile())
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }
}
