package com.z_company.route.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.sendToSentry
import com.z_company.core.ui.snackbar.ISnackbarManager
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate as JLocalDate
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel экрана «Новое отвлечение» — выбор диапазона дат + тип отвлечения,
 * сохранение как личных release-дней. Соответствует дизайну AbsenceFlowScreen.
 */
class AbsenceViewModel : ViewModel(), KoinComponent {
    private val settingsUseCase: SettingsUseCase by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val snackbarManager: ISnackbarManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()
    private val syncManager: SyncManager by inject()

    private val _uiState = MutableStateFlow(AbsenceUiState())
    val uiState: StateFlow<AbsenceUiState> = _uiState.asStateFlow()

    // Все месяцы производственного календаря, известные приложению, по (год, месяц-0based).
    // Позволяет листать месяцы шторки независимо от выбранного диапазона дат
    // (диапазон может начинаться в одном месяце и заканчиваться в другом).
    private var monthsCache: Map<Pair<Int, Int>, MonthOfYear> = emptyMap()

    /** Типы отвлечений для выбора (без «Выходного» — он добавляется отдельным пунктом меню). */
    val types: List<ReleaseType> = listOf(
        ReleaseType.Vacation, ReleaseType.SickLeave, ReleaseType.Courses,
        ReleaseType.Donor, ReleaseType.ChildCare, ReleaseType.BusinessTrip,
        ReleaseType.Other,
    )

    fun prepareScreen() {
        viewModelScope.launch {
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
                t.sendToSentry("AbsenceViewModel", "prepareScreen")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Отобразить в шторке другой месяц, не трогая уже выбранный диапазон дат. */
    private fun applyMonth(found: MonthOfYear) {
        val daysInMonth = JLocalDate.of(found.year, found.month + 1, 1).lengthOfMonth()
        val existing = found.days
            .filter { it.isReleaseDay && it.releaseType != null }
            .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
        _uiState.update {
            it.copy(
                isLoading = false,
                year = found.year,
                month = found.month,
                monthName = monthName(found.month),
                daysInMonth = daysInMonth,
                existingAbsences = existing,
            )
        }
    }

    /** Переключить показанный в шторке месяц (delta = -1 / +1). Диапазон дат не сбрасывается —
     * позволяет начать диапазон в одном месяце и закончить в другом. */
    fun shiftMonth(delta: Int) {
        val s = _uiState.value
        var y = s.year
        var m = s.month + delta
        if (m < 0) { m = 11; y -= 1 }
        if (m > 11) { m = 0; y += 1 }
        val found = monthsCache[y to m]
        if (found == null) {
            snackbarManager.show("Нет данных календаря за этот месяц")
            return
        }
        applyMonth(found)
    }

    /** Тап по дню (в текущем показанном месяце шторки):
     * 1-й — начало (одиночный), 2-й — конец диапазона, 3-й — сброс. */
    fun onDayTap(day: Int) = _uiState.update { s ->
        val tapped = LocalDate(s.year, s.month + 1, day)
        val start = s.rangeStart
        val end = s.rangeEnd
        val next = when {
            start == null -> s.copy(rangeStart = tapped, rangeEnd = tapped)
            start == end -> s.copy(rangeStart = minOf(start, tapped), rangeEnd = maxOf(start, tapped))
            else -> s.copy(rangeStart = tapped, rangeEnd = tapped)
        }
        next.copy(rangeHours = computeHours(next.rangeStart, next.rangeEnd, next.selectedType))
    }

    fun setType(type: ReleaseType) = _uiState.update {
        it.copy(
            selectedType = type,
            typePickerOpen = false,
            rangeHours = computeHours(it.rangeStart, it.rangeEnd, type),
        )
    }

    /** Тег дня (рабочий/выходной/праздник) по любой дате — по кэшу всех известных месяцев. */
    private fun tagFor(date: LocalDate): TagForDay =
        monthsCache[date.year to date.monthNumber - 1]?.days
            ?.find { it.dayOfMonth == date.dayOfMonth }?.tag
            ?: TagForDay.WORKING_DAY

    /** Норма-часы за диапазон (может охватывать несколько месяцев):
     * только рабочие дни (кроме «по уходу» — все дни). */
    private fun computeHours(start: LocalDate?, end: LocalDate?, type: ReleaseType): Int {
        if (start == null || end == null) return 0
        val lo = minOf(start, end)
        val hi = maxOf(start, end)
        var total = 0
        var d = lo
        while (d <= hi) {
            total += hoursForDay(tagFor(d), type)
            d = d.plus(1, DateTimeUnit.DAY)
        }
        return total
    }

    private fun hoursForDay(tag: TagForDay, type: ReleaseType): Int =
        // В карточке командировки показываем часы по рабочим дням
        // производственного календаря. Это справочное значение:
        // оплата командировочных маршрутов идёт отдельно по среднему часу.
        if (type == ReleaseType.TechnicalStudy) 0
        else if (type == ReleaseType.ChildCare) when (tag) {
            TagForDay.WORKING_DAY -> 8
            TagForDay.SHORTENED_DAY -> 7
            TagForDay.NON_WORKING_DAY -> 8
            TagForDay.HOLIDAY -> 8
        } else when (tag) {
            TagForDay.WORKING_DAY -> 8
            TagForDay.SHORTENED_DAY -> 7
            TagForDay.NON_WORKING_DAY -> 0
            TagForDay.HOLIDAY -> 0
        }

    fun toggleTypePicker() = _uiState.update { it.copy(typePickerOpen = !it.typePickerOpen) }

    fun save() {
        val s = _uiState.value
        val start = s.rangeStart ?: run { snackbarManager.show("Выберите даты"); return }
        val end = s.rangeEnd ?: start
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val lo = minOf(start, end)
                val hi = maxOf(start, end)
                val days = generateSequence(lo) { it.plus(1, DateTimeUnit.DAY) }
                    .takeWhile { it <= hi }
                    .toList()
                val saveResult = releaseDayUseCase.savePeriod(
                    ReleasePeriod(days = days, type = s.selectedType)
                ).first { it is com.z_company.core.ResultState.Success || it is com.z_company.core.ResultState.Error }
                if (saveResult is com.z_company.core.ResultState.Success) autoPushSettings()
                // Синхронизируем объединённые месяцы в настройки — иначе зарплата/норма
                // не увидят новое отвлечение (см. addDayOff в CalendarViewModel). Диапазон
                // может охватывать несколько месяцев — обновляем каждый из затронутых.
                runCatching { calendarUseCase.loadFlowMonthOfYearListState().first() }
                    .getOrNull()?.let { allMonths ->
                        val affected = days.map { it.year to (it.monthNumber - 1) }.distinct()
                        affected.forEach { (y, m) ->
                            allMonths.find { it.year == y && it.month == m }?.let { merged ->
                                settingsUseCase.setCurrentMonthOfYear(merged).first()
                            }
                        }
                    }
                _uiState.update { it.copy(isSaving = false, done = true) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("AbsenceViewModel", "save")
                snackbarManager.show("Не удалось сохранить отвлечение")
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun autoPushSettings() {
        sharedPrefs.setSettingsSyncPending(true)
        viewModelScope.launch(Dispatchers.IO) {
            val token = secureTokenStorage.getAuthBearerTokenFlow().first() ?: return@launch
            syncManager.autoPushSettings("Bearer $token").collect {}
        }
    }

    private fun monthName(index: Int): String = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    ).getOrElse(index) { "" }
}

data class AbsenceUiState(
    val isLoading: Boolean = true,
    val year: Int = 0,
    val month: Int = 0,
    val monthName: String = "",
    val daysInMonth: Int = 0,
    // Даты выбранного диапазона (могут лежать в разных месяцах — не обязательно
    // совпадают с показанным в шторке `year`/`month`).
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
    val rangeHours: Int = 0,
    val selectedType: ReleaseType = ReleaseType.Vacation,
    val typePickerOpen: Boolean = false,
    val existingAbsences: Map<Int, ReleaseType> = emptyMap(),
    val isSaving: Boolean = false,
    val done: Boolean = false,
)
