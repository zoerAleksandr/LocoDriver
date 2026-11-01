package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.sendToSentry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleMode
import com.z_company.domain.entities.WorkScheduleProfile
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.buildMonthNorm
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Ячейка календаря нормы («Своя настройка», Настройки → Норма/Регион). */
data class NormMonthCell(
    val day: Int,
    /** День недели: 0 = понедельник … 6 = воскресенье. */
    val weekdayIndex: Int,
    /** Норма на этот день. */
    val hours: Int,
    /** Норма с начала месяца по этот день включительно; null — у дня нормы нет (ВЫХ, праздник, отвлечение). */
    val cumulative: Int?,
    val isHoliday: Boolean,
    val isDayOff: Boolean,
    /** Норма задана пользователем, а не взята из шаблона недели. */
    val isCustom: Boolean,
    /** Сокращённый (предпраздничный) день: норма на 1 ч меньше. */
    val isShortened: Boolean,
    /** Отвлечение (отпуск, больничный…) на этом дне; null — нет или это «Выходной». */
    val absence: ReleaseType?,
    /** Норма по шаблону недели без правок пользователя — значение для «Сбросить». */
    val defaultHours: Int,
    /** Название праздника (региональное или федеральное); null — названия нет. */
    val holidayName: String? = null,
    /** Название отвлечения («Отпуск»…) и его непрерывный период в этом месяце. */
    val absenceLabel: String? = null,
    val absencePeriodStart: Int? = null,
    val absencePeriodEnd: Int? = null,
)

/** День недели в шаблоне «По неделям»: норма в часах или «Выходной». */
data class WeekPatternDay(val hours: Int, val dayOff: Boolean = false)

data class NormMonthUiState(
    val year: Int = 0,
    /** 0-based, как в MonthOfYear. */
    val month: Int = 0,
    /** false — месяца нет в производственном календаре (нет данных). */
    val hasData: Boolean = false,
    val cells: List<NormMonthCell> = emptyList(),
    /** Норма месяца — совпадает с «Нормой часов» в Настройках и расчётом зарплаты. */
    val total: Int = 0,
    /** Выбранная рабочая неделя: стандартная, шестидневная или своя (после правок по дням). */
    val mode: WorkScheduleMode = WorkScheduleMode.STANDARD,
    val canGoPrev: Boolean = false,
    val canGoNext: Boolean = false,
)

/**
 * Календарь нормы режима «Своя настройка». Норма дня хранится в
 * [WorkScheduleProfile.customDays] (локально), «Выходной» — в таблице ReleaseDay, ровно как в
 * Календаре: оба экрана читают и пишут одни и те же данные.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NormMonthViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val syncManager: SyncManager by inject()
    private val snackbarManager: ISnackbarManager by inject()

    private val selected = MutableStateFlow<Pair<Int, Int>?>(null)

    // Шаблон недели для «По неделям» (Пн…Вс); применяется к месяцу кнопкой «Применить».
    private val _weekPattern = MutableStateFlow(
        DayOfWeek.entries.map { WeekPatternDay(sharedPrefs.getWorkScheduleProfile().hoursFor(it)) }
    )
    val weekPattern: StateFlow<List<WeekPatternDay>> = _weekPattern.asStateFlow()
    private val _uiState = MutableStateFlow(NormMonthUiState())
    val uiState: StateFlow<NormMonthUiState> = _uiState.asStateFlow()

    init {
        selected
            .flatMapLatest { ym ->
                if (ym == null) kotlinx.coroutines.flow.emptyFlow()
                else kotlinx.coroutines.flow.flow {
                    val names = loadHolidayNames(ym.first, ym.second)
                    emitAll(
                        combine(
                            calendarUseCase.loadFlowMonthOfYearListState(),
                            releaseDayUseCase.getByYearMonthFlow(ym.first, ym.second),
                            sharedPrefs.getWorkScheduleProfileFlow(),
                        ) { months, releaseDays, profile ->
                            buildState(ym.first, ym.second, months, releaseDays, profile, names)
                        }
                    )
                }
            }
            .onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    /** Открыть месяц (0-based), только если ещё не выбран другой. */
    fun open(year: Int, month: Int) {
        if (selected.value == null) selected.value = year to month
    }

    fun shiftMonth(delta: Int) {
        val (y, m) = selected.value ?: return
        val index = y * 12 + m + delta
        val target = index / 12 to index % 12
        val exists = calendarUseCase.loadMonthOfYearList().any { it.year == target.first && it.month == target.second }
        if (exists) selected.value = target
    }

    /** Норма на день. Праздник и «Выходной» нормы не имеют — игнорируем. */
    fun setHours(day: Int, hours: Int) {
        val cell = _uiState.value.cells.firstOrNull { it.day == day } ?: return
        if (cell.isHoliday || cell.isDayOff) return
        val (y, m) = selected.value ?: return
        val date = LocalDate(y, m + 1, day)
        val value = hours.coerceIn(0, WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS)
        // Равно шаблону недели — не храним правку, чтобы день следовал шаблону.
        saveProfile(sharedPrefs.getWorkScheduleProfile().withCustomDay(date, value.takeIf { it != cell.defaultHours }))
    }

    /**
     * Стандартная или шестидневная неделя. Сбрасывает правки по дням **показанного месяца** (другие месяцы не
     * затрагиваются; «Выходные» не удаляются — они общие с Календарём) и возвращает шаблон «По неделям»
     * к значениям выбранной недели.
     */
    fun selectPreset(mode: WorkScheduleMode) {
        val (y, m) = selected.value ?: return
        val template = if (mode == WorkScheduleMode.SIX_DAY_7_5) {
            WorkScheduleProfile.sixDaySevenFive()
        } else {
            WorkScheduleProfile.standard()
        }
        _weekPattern.value = DayOfWeek.entries.map { WeekPatternDay(template.hoursFor(it)) }
        val current = sharedPrefs.getWorkScheduleProfile()
        saveProfile(
            template.copy(customDays = current.customDays).withoutCustomDaysOfMonth(y, m + 1)
        )
    }

    fun setPatternHours(dayIndex: Int, hours: Int) {
        updatePattern(dayIndex) { it.copy(hours = hours.coerceIn(0, WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS)) }
    }

    fun setPatternDayOff(dayIndex: Int, dayOff: Boolean) {
        updatePattern(dayIndex) { it.copy(dayOff = dayOff) }
    }

    private fun updatePattern(dayIndex: Int, change: (WeekPatternDay) -> WeekPatternDay) {
        _weekPattern.value = _weekPattern.value.toMutableList().also { it[dayIndex] = change(it[dayIndex]) }
    }

    /**
     * Перенести шаблон недели на показанный месяц: норма каждого дня становится значением его дня недели
     * (сокращённый день — на 1 ч меньше), а дни недели с «Выходным» получают ВЫХ (общий с Календарём).
     * Праздники и дни с отвлечением не затрагиваются; уже стоящие ВЫХ не снимаются.
     */
    fun applyPattern() {
        val (y, m) = selected.value ?: return
        val pattern = _weekPattern.value
        var profile = sharedPrefs.getWorkScheduleProfile().copy(mode = WorkScheduleMode.CUSTOM)
        val dayOffDates = mutableListOf<LocalDate>()
        _uiState.value.cells
            .filter { !it.isHoliday && it.absence == null }
            .forEach { cell ->
                val day = pattern[cell.weekdayIndex]
                val date = LocalDate(y, m + 1, cell.day)
                if (day.dayOff) {
                    if (!cell.isDayOff) dayOffDates += date
                } else if (!cell.isDayOff) {
                    // Сокращённый день — на час меньше, как и в значениях по умолчанию.
                    val value = (day.hours - if (cell.isShortened) 1 else 0).coerceAtLeast(0)
                    profile = profile.withCustomDay(date, value.takeIf { it != cell.defaultHours })
                }
            }
        saveProfile(profile)
        if (dayOffDates.isNotEmpty()) saveDaysOff(y, m, dayOffDates)
    }

    private fun saveDaysOff(year: Int, month: Int, dates: List<LocalDate>) {
        viewModelScope.launch {
            try {
                val result = releaseDayUseCase.savePeriod(ReleasePeriod(days = dates, type = ReleaseType.DayOff))
                    .first { it is ResultState.Success || it is ResultState.Error }
                if (result is ResultState.Success) autoPushSettings()
                runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == year && it.month == month }
                }.getOrNull()?.let { settingsUseCase.setCurrentMonthOfYear(it).first() }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("NormMonthViewModel", "saveDaysOff")
                snackbarManager.show("Не удалось поставить выходные")
            }
        }
    }

    /** Названия праздников месяца: региональные из БД, затем федеральные РФ (как в Календаре). */
    private suspend fun loadHolidayNames(year: Int, month: Int): Map<Int, String> {
        val region = runCatching { settingsUseCase.getUserSettingFlow().first().region }.getOrNull()
        val regional = region?.let { code ->
            runCatching {
                calendarUseCase.getRegionalHolidays(code, year)
                    .filter { it.month == month }
                    .associate { it.dayOfMonth to it.name }
            }.getOrDefault(emptyMap())
        } ?: emptyMap()
        val country = region?.substringBefore("-")?.uppercase() ?: "RU"
        return buildMap {
            for (d in 1..31) {
                val name = regional[d] ?: if (country == "RU") RU_FEDERAL_HOLIDAY_NAMES[month to d] else null
                if (name != null) put(d, name)
            }
        }
    }

    /** Вернуть норму дня по шаблону недели. */
    fun resetHours(day: Int) {
        val (y, m) = selected.value ?: return
        saveProfile(sharedPrefs.getWorkScheduleProfile().withCustomDay(LocalDate(y, m + 1, day), null))
    }

    private fun saveProfile(profile: WorkScheduleProfile) {
        sharedPrefs.setWorkScheduleProfile(profile.copy(updatedAt = System.currentTimeMillis()))
        autoPushSettings()
    }

    /** Поставить/снять «Выходной» — то же, что «Выходной» в Календаре (ReleaseDay, тип DayOff). */
    fun setDayOff(day: Int, dayOff: Boolean) {
        val (y, m) = selected.value ?: return
        val cell = _uiState.value.cells.firstOrNull { it.day == day } ?: return
        if (cell.isHoliday || cell.absence != null) return
        // В стандартной и шестидневной неделе «Выходной» норму не меняет; в календаре нормы он — день без нормы,
        // поэтому постановка выходного переводит неделю в «свою» (правки по дням).
        val profile = sharedPrefs.getWorkScheduleProfile()
        if (dayOff && profile.mode != WorkScheduleMode.CUSTOM) saveProfile(profile.copy(mode = WorkScheduleMode.CUSTOM))
        viewModelScope.launch {
            try {
                val date = LocalDate(y, m + 1, day)
                val result = if (dayOff) {
                    releaseDayUseCase.savePeriod(ReleasePeriod(days = listOf(date), type = ReleaseType.DayOff))
                } else {
                    releaseDayUseCase.deletePeriod(ReleasePeriod(days = listOf(date), type = null))
                }.first { it is ResultState.Success || it is ResultState.Error }
                if (result is ResultState.Success) autoPushSettings()
                // Как в CalendarViewModel: объединённый месяц — в settings.selectMonthOfYear,
                // оттуда расчёты зарплаты и нормы читают дни.
                runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == y && it.month == m }
                }.getOrNull()?.let { settingsUseCase.setCurrentMonthOfYear(it).first() }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("NormMonthViewModel", "setDayOff")
                snackbarManager.show("Не удалось изменить выходной")
            }
        }
    }

    private fun buildState(
        year: Int,
        month: Int,
        months: List<MonthOfYear>,
        releaseDays: List<ReleaseDay>,
        profile: WorkScheduleProfile,
        holidayNames: Map<Int, String>,
    ): NormMonthUiState {
        val norm = buildMonthNorm(months, releaseDays, year, month, profile)
        val absenceByDay = releaseDays
            .filter { it.releaseType != ReleaseType.DayOff }
            .associate { it.dayOfMonth to it.releaseType }
        val defaultProfile = profile.copy(customDays = emptyMap())
        // Непрерывный период отвлечения одного типа, в который входит день.
        fun periodOf(day: Int): IntRange? {
            val type = absenceByDay[day] ?: return null
            var a = day
            var b = day
            while (absenceByDay[a - 1] == type) a--
            while (absenceByDay[b + 1] == type) b++
            return a..b
        }
        var running = 0
        val cells = norm.map { d ->
            running += d.countedHours
            val absence = absenceByDay[d.day]
            val hasNorm = absence == null && d.tag != TagForDay.HOLIDAY && d.countedHours > 0
            NormMonthCell(
                day = d.day,
                weekdayIndex = d.date.dayOfWeek.ordinal,
                hours = d.hours,
                cumulative = running.takeIf { hasNorm && !d.isNormReducing },
                isHoliday = d.tag == TagForDay.HOLIDAY,
                isDayOff = d.isDayOff,
                isCustom = d.isCustom,
                isShortened = d.tag == TagForDay.SHORTENED_DAY,
                absence = absence,
                defaultHours = defaultProfile.effectiveHours(d.date, d.tag),
                holidayName = holidayNames[d.day].takeIf { d.tag == TagForDay.HOLIDAY },
                absenceLabel = absence?.text,
                absencePeriodStart = periodOf(d.day)?.first,
                absencePeriodEnd = periodOf(d.day)?.last,
            )
        }
        fun has(y: Int, m: Int) = months.any { it.year == y && it.month == m }
        val index = year * 12 + month
        return NormMonthUiState(
            year = year,
            month = month,
            mode = profile.mode,
            hasData = cells.isNotEmpty(),
            cells = cells,
            total = running,
            canGoPrev = has((index - 1) / 12, (index - 1) % 12),
            canGoNext = has((index + 1) / 12, (index + 1) % 12),
        )
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }
}
