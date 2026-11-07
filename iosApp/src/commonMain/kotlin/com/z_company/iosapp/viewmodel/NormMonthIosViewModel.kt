@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.iosapp.viewmodel

import androidx.lifecycle.ViewModel
import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleMode
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.NormaUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.use_cases.buildMonthNorm
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

/**
 * KMP ViewModel «Норма по месяцам» / «Своя настройка» (SCREEN_SPECS §16.3, §16.3.1).
 *
 * Профиль рабочей недели (`WorkScheduleProfile`) хранится локально в
 * `SharedPreferencesRepositories`; норма по дням — `buildMonthNorm` (единый расчёт
 * с `NormaUseCase`). ВЫХ — тот же `ReleaseDay(DayOff)`, что в Календаре (§14).
 */
class NormMonthIosViewModel(
    private val settingsUseCase: SettingsUseCase,
    private val calendarUseCase: CalendarUseCase,
    private val releaseDayUseCase: ReleaseDayUseCase,
    private val normaUseCase: NormaUseCase,
    private val sharedPrefs: SharedPreferencesRepositories,
    private val secureTokenStorage: SecureTokenStorage,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _ui = MutableStateFlow(NormMonthIosUi())

    private var year: Int = 0
    private var month: Int = 0
    private var monthsCache: Map<Pair<Int, Int>, MonthOfYear> = emptyMap()

    /** Шаблон «По неделям» живёт на экране и в профиль не пишется (§16.3.1). */
    private data class TemplateSlot(var hours: Int, var isDayOff: Boolean)
    private val weekTemplate = MutableList(7) { TemplateSlot(8, false) }

    fun watchUi(callback: (NormMonthIosUi) -> Unit) {
        scope.launch { _ui.collect { callback(it) } }
    }

    fun prepareScreen() {
        scope.launch {
            try {
                val setting = settingsUseCase.getUserSettingFlow().first()
                val base = setting.selectMonthOfYear
                year = base.year
                month = base.month
                val allMonths = runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                }.getOrNull().orEmpty()
                monthsCache = allMonths.associateBy { it.year to it.month }
                initTemplateFromProfile()
                rebuild()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }

    fun shiftMonth(delta: Int) {
        var y = year
        var m = month + delta
        if (m < 0) { m = 11; y -= 1 }
        if (m > 11) { m = 0; y += 1 }
        if (monthsCache[y to m] == null) {
            emitMessage("Нет данных календаря за этот месяц")
            return
        }
        year = y
        month = m
        scope.launch { rebuild() }
    }

    // ── Рабочая неделя (§16.3) ──────────────────────────────────────────────

    /** mode: "standard" | "sixDay" | "custom". Сброс правок только показанного месяца. */
    fun setWeekMode(mode: String) {
        scope.launch {
            val current = sharedPrefs.getWorkScheduleProfile()
            val base = when (mode) {
                "standard" -> WorkScheduleProfile.standard()
                "sixDay" -> WorkScheduleProfile.sixDaySevenFive()
                else -> current.copy(mode = WorkScheduleMode.CUSTOM)
            }
            // Правки по дням других месяцев и стоящие ВЫХ не затрагиваются (§16.3.1).
            val updated = base.copy(
                customDays = current.withoutCustomDaysOfMonth(year, month + 1).customDays
            )
            sharedPrefs.setWorkScheduleProfile(updated)
            if (mode != "custom") initTemplateFromProfile()
            rebuild()
        }
    }

    fun toggleWeekTemplate() = _ui.update {
        it.copy(isWeekTemplateExpanded = !it.isWeekTemplateExpanded)
    }

    /** Счётчик шаблона дня недели (0..12). Шаблон живёт до выхода (§16.3.1). */
    fun setWeekdayHours(index: Int, hours: Int) {
        if (index !in weekTemplate.indices) return
        weekTemplate[index].hours = hours.coerceIn(0, 12)
        weekTemplate[index].isDayOff = false
        rebuildLight()
    }

    /** Переключатель «Выходной (ВЫХ)» в шторке дня недели шаблона. */
    fun setWeekdayDayOff(index: Int, isDayOff: Boolean) {
        if (index !in weekTemplate.indices) return
        weekTemplate[index].isDayOff = isDayOff
        rebuildLight()
    }

    /** «Применить к месяцу»: шаблон → норма дней показанного месяца + ВЫХ (§16.3.1). */
    fun applyTemplateToMonth() {
        scope.launch {
            try {
                val profile = sharedPrefs.getWorkScheduleProfile()
                val monthData = monthsCache[year to month]
                val tagByDay = monthData?.days?.associate { it.dayOfMonth to it.tag }.orEmpty()
                val releaseDays = releaseDayUseCase.getAll()
                val dayOffDays = releaseDays
                    .filter { it.year == year && it.month == month && it.releaseType == ReleaseType.DayOff }
                    .map { it.dayOfMonth }
                    .toSet()
                val absenceDays = releaseDays
                    .filter {
                        it.year == year && it.month == month &&
                            it.releaseType != ReleaseType.DayOff &&
                            it.releaseType != ReleaseType.BusinessTrip &&
                            it.releaseType != ReleaseType.TechnicalStudy
                    }
                    .map { it.dayOfMonth }
                    .toSet()

                var updatedProfile = profile
                val newDayOffs = mutableListOf<LocalDate>()
                val daysInMonth = iosDaysInMonth(year, month)
                for (day in 1..daysInMonth) {
                    val tag = tagByDay[day] ?: TagForDay.WORKING_DAY
                    if (tag == TagForDay.HOLIDAY) continue
                    if (day in absenceDays) continue
                    val date = LocalDate(year, month + 1, day)
                    val slot = weekTemplate[(date.dayOfWeek.ordinal + 6) % 7]
                    if (slot.isDayOff) {
                        if (day !in dayOffDays) newDayOffs.add(date)
                        continue
                    }
                    // Сокращённый день — на 1 ч меньше (§16.3.1).
                    val hours = if (tag == TagForDay.SHORTENED_DAY) {
                        (slot.hours - 1).coerceAtLeast(0)
                    } else slot.hours
                    val defaultHours = updatedProfile.copy(customDays = emptyMap())
                        .effectiveHours(date, tag)
                    // Значение, равное умолчанию, правкой не хранится.
                    updatedProfile = updatedProfile.withCustomDay(date, if (hours == defaultHours) null else hours)
                }
                updatedProfile = updatedProfile.copy(mode = WorkScheduleMode.CUSTOM)
                sharedPrefs.setWorkScheduleProfile(updatedProfile)
                if (newDayOffs.isNotEmpty()) {
                    releaseDayUseCase.savePeriod(ReleasePeriod(days = newDayOffs, type = ReleaseType.DayOff))
                        .first { it is ResultState.Success || it is ResultState.Error }
                    autoPushSettings()
                }
                rebuild()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось применить шаблон")
            }
        }
    }

    // ── Правки дня (шторка дня) ─────────────────────────────────────────────

    /** Норма дня (0..12); сразу сохраняется, режим становится «своим». */
    fun setDayHours(day: Int, hours: Int) {
        scope.launch {
            val profile = sharedPrefs.getWorkScheduleProfile()
            val date = LocalDate(year, month + 1, day)
            val tag = tagFor(day)
            val safe = hours.coerceIn(0, WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS)
            val defaultHours = profile.copy(customDays = emptyMap()).effectiveHours(date, tag)
            val updated = profile.withCustomDay(date, if (safe == defaultHours) null else safe)
                .copy(mode = WorkScheduleMode.CUSTOM)
            sharedPrefs.setWorkScheduleProfile(updated)
            rebuild()
        }
    }

    /** «Вернуть по умолчанию (N ч)». */
    fun resetDayHours(day: Int) {
        scope.launch {
            val profile = sharedPrefs.getWorkScheduleProfile()
            val date = LocalDate(year, month + 1, day)
            sharedPrefs.setWorkScheduleProfile(profile.withCustomDay(date, null).copy(mode = WorkScheduleMode.CUSTOM))
            rebuild()
        }
    }

    /** «Выходной (ВЫХ)» — тот же ReleaseDay(DayOff), что в Календаре (§14). */
    fun toggleDayOff(day: Int) {
        scope.launch {
            try {
                val date = LocalDate(year, month + 1, day)
                val existing = releaseDayUseCase.getAll().find {
                    it.year == year && it.month == month && it.dayOfMonth == day &&
                        it.releaseType == ReleaseType.DayOff
                }
                if (existing != null) {
                    releaseDayUseCase.deletePeriod(ReleasePeriod(days = listOf(date), type = null))
                        .first { it is ResultState.Success || it is ResultState.Error }
                } else {
                    releaseDayUseCase.savePeriod(ReleasePeriod(days = listOf(date), type = ReleaseType.DayOff))
                        .first { it is ResultState.Success || it is ResultState.Error }
                }
                autoPushSettings()
                // Постановка ВЫХ переводит неделю в «свою» (§16.3.1).
                val profile = sharedPrefs.getWorkScheduleProfile()
                sharedPrefs.setWorkScheduleProfile(profile.copy(mode = WorkScheduleMode.CUSTOM))
                rebuild()
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                emitMessage("Не удалось изменить выходной")
            }
        }
    }

    fun consumeMessage() = _ui.update { it.copy(message = "") }

    // ── Пересборка ──────────────────────────────────────────────────────────

    private fun rebuildLight() {
        val profile = sharedPrefs.getWorkScheduleProfile()
        _ui.update {
            it.copy(weekTemplate = templateUi(profile))
        }
    }

    private suspend fun rebuild() {
        val profile = sharedPrefs.getWorkScheduleProfile()
        val allMonths = runCatching { calendarUseCase.loadFlowMonthOfYearListState().first() }
            .getOrNull().orEmpty()
        monthsCache = allMonths.associateBy { it.year to it.month }
        val releaseDays = releaseDayUseCase.getAll().filter { it.year == year && it.month == month }
        val normDays = buildMonthNorm(allMonths, releaseDays, year, month, profile)
        val normaMonth = normaUseCase.getNormaHours(year, month)

        val regionalNames: Map<Int, String> = runCatching {
            val setting = settingsUseCase.getUserSettingFlow().first()
            setting.region?.let { region ->
                calendarUseCase.getRegionalHolidays(region, year)
                    .filter { it.month == month }
                    .associate { it.dayOfMonth to it.name }
            }
        }.getOrNull() ?: emptyMap()

        val tagByDay = monthsCache[year to month]?.days?.associate { it.dayOfMonth to it.tag }.orEmpty()
        val absenceByDay = absencePeriods(releaseDays)

        var cumulative = 0
        val dayCells = normDays.map { nd ->
            cumulative += nd.countedHours
            val date = LocalDate(year, month + 1, nd.day)
            val weekdayIndex = (date.dayOfWeek.ordinal + 6) % 7
            val absence = absenceByDay[nd.day]
            val label = when {
                nd.isDayOff -> "ВЫХ"
                nd.tag == TagForDay.HOLIDAY -> "ПР"
                absence != null -> absence.first
                else -> null
            }
            val showNorm = label == null && nd.hours > 0
            val defaultHours = profile.copy(customDays = emptyMap())
                .effectiveHours(date, nd.tag)
            NormMonthIosDay(
                day = nd.day,
                weekdayIndex = weekdayIndex,
                isWeekend = weekdayIndex >= 5,
                isToday = isToday(nd.day),
                isHoliday = nd.tag == TagForDay.HOLIDAY,
                holidayName = if (nd.tag == TagForDay.HOLIDAY) {
                    iosHolidayName(month, nd.day, regionalNames, "RU")
                } else null,
                normText = if (showNorm) nd.hours.toString() else "",
                cumulativeText = if (showNorm) cumulative.toString() else "",
                label = label,
                labelIsHoliday = nd.tag == TagForDay.HOLIDAY,
                isDayOff = nd.isDayOff,
                isNormReducing = nd.isNormReducing,
                isCustom = nd.isCustom,
                defaultHoursText = "$defaultHours ч",
                absenceTitle = absence?.second,
                absencePeriodText = absence?.third,
                isEditable = nd.tag != TagForDay.HOLIDAY && absence == null,
            )
        }

        _ui.update {
            it.copy(
                isLoading = false,
                year = year,
                month = month,
                monthLabel = "${iosMonthName(month)} $year",
                normaMonthText = iosFormatHhMm(normaMonth.toLong() * 3_600_000L),
                weekMode = when (profile.mode) {
                    WorkScheduleMode.STANDARD -> "standard"
                    WorkScheduleMode.SIX_DAY_7_5 -> "sixDay"
                    WorkScheduleMode.CUSTOM -> "custom"
                },
                weekTemplate = templateUi(profile),
                days = dayCells,
            )
        }
    }

    private fun templateUi(profile: WorkScheduleProfile): List<NormMonthIosWeekday> {
        val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        return weekTemplate.mapIndexed { i, slot ->
            NormMonthIosWeekday(
                index = i,
                label = labels[i],
                hours = slot.hours,
                hoursText = if (slot.isDayOff) "ВЫХ" else "${slot.hours} ч",
                isDayOff = slot.isDayOff,
            )
        }
    }

    private fun initTemplateFromProfile() {
        val profile = sharedPrefs.getWorkScheduleProfile()
        for (i in 0..6) {
            val dow = kotlinx.datetime.DayOfWeek.entries[i]
            weekTemplate[i] = TemplateSlot(profile.hoursFor(dow).coerceIn(0, 12), false)
        }
    }

    private fun tagFor(day: Int): TagForDay =
        monthsCache[year to month]?.days?.find { it.dayOfMonth == day }?.tag ?: TagForDay.WORKING_DAY

    /** Периоды отвлечений: день → (badge, label, период-текст). */
    private fun absencePeriods(releaseDays: List<ReleaseDay>): Map<Int, Triple<String, String, String>> {
        // Triple: badge, label, «10–24 ноября (15 дн.)» — складываем в data-класс ниже.
        val rel = releaseDays
            .filter { it.releaseType != ReleaseType.DayOff && it.releaseType != ReleaseType.TechnicalStudy }
            .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
        val result = mutableMapOf<Int, Triple<String, String, String>>()
        val sorted = rel.keys.sorted()
        var i = 0
        while (i < sorted.size) {
            val start = sorted[i]
            val type = rel.getValue(start)
            var j = i
            while (j + 1 < sorted.size && sorted[j + 1] == sorted[j] + 1 && rel[sorted[j + 1]] == type) j++
            val end = sorted[j]
            val daysCount = end - start + 1
            val periodText = if (start == end) {
                "$start ${iosMonthGenitive(month)}"
            } else {
                "$start–$end ${iosMonthGenitive(month)} ($daysCount дн.)"
            }
            for (d in start..end) {
                result[d] = Triple(type.iosBadge(), type.text, periodText)
            }
            i = j + 1
        }
        return result
    }

    private fun isToday(day: Int): Boolean {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val todayDate = kotlinx.datetime.Instant.fromEpochMilliseconds(now)
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        return todayDate.year == year && todayDate.monthNumber - 1 == month && todayDate.dayOfMonth == day
    }

    private fun emitMessage(text: String) {
        _ui.update { it.copy(message = text) }
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        scope.launch {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }
}
