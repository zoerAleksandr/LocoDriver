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
import kotlinx.datetime.LocalDate
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

    private var month: MonthOfYear? = null
    // Теги дней (рабочий/выходной/праздник) — для подсчёта часов отвлечения:
    // норма-часы начисляются только за рабочие дни (кроме «по уходу» — за все).
    private var dayTags: Map<Int, TagForDay> = emptyMap()

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
                val merged = runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == base.year && it.month == base.month }
                }.getOrNull() ?: base
                month = merged
                dayTags = merged.days.associate { it.dayOfMonth to it.tag }
                val daysInMonth = JLocalDate.of(merged.year, merged.month + 1, 1).lengthOfMonth()
                val existing = merged.days
                    .filter { it.isReleaseDay && it.releaseType != null }
                    .associate { it.dayOfMonth to (it.releaseType ?: ReleaseType.Other) }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        year = merged.year,
                        month = merged.month,
                        monthName = monthName(merged.month),
                        daysInMonth = daysInMonth,
                        existingAbsences = existing,
                    )
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                t.sendToSentry("AbsenceViewModel", "prepareScreen")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Тап по дню: 1-й — начало (одиночный), 2-й — конец диапазона, 3-й — сброс. */
    fun onDayTap(day: Int) = _uiState.update { s ->
        val start = s.rangeStart
        val end = s.rangeEnd
        val next = when {
            start == null -> s.copy(rangeStart = day, rangeEnd = day)
            start == end -> s.copy(rangeStart = minOf(start, day), rangeEnd = maxOf(start, day))
            else -> s.copy(rangeStart = day, rangeEnd = day)
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

    /** Норма-часы за диапазон: только рабочие дни (кроме «по уходу» — все дни). */
    private fun computeHours(start: Int?, end: Int?, type: ReleaseType): Int {
        if (start == null || end == null) return 0
        return (minOf(start, end)..maxOf(start, end)).sumOf { d ->
            hoursForDay(dayTags[d] ?: TagForDay.WORKING_DAY, type)
        }
    }

    private fun hoursForDay(tag: TagForDay, type: ReleaseType): Int =
        // Командировка — рабочий период, часы норма-отдыха не начисляем (оплата
        // идёт по фактическим маршрутам через средний час).
        if (type == ReleaseType.BusinessTrip || type == ReleaseType.TechnicalStudy) 0
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
        val m = month ?: return
        val start = s.rangeStart ?: run { snackbarManager.show("Выберите даты"); return }
        val end = s.rangeEnd ?: start
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val days = (minOf(start, end)..maxOf(start, end)).map { d ->
                    LocalDate(m.year, m.month + 1, d)
                }
                val saveResult = releaseDayUseCase.savePeriod(
                    ReleasePeriod(days = days, type = s.selectedType)
                ).first { it is com.z_company.core.ResultState.Success || it is com.z_company.core.ResultState.Error }
                if (saveResult is com.z_company.core.ResultState.Success) autoPushSettings()
                // Синхронизируем объединённый месяц в настройки — иначе зарплата/норма
                // не увидят новое отвлечение (см. addDayOff в CalendarViewModel).
                runCatching {
                    calendarUseCase.loadFlowMonthOfYearListState().first()
                        .find { it.year == m.year && it.month == m.month }
                }.getOrNull()?.let { merged ->
                    settingsUseCase.setCurrentMonthOfYear(merged).first()
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
    val rangeStart: Int? = null,
    val rangeEnd: Int? = null,
    val rangeHours: Int = 0,
    val selectedType: ReleaseType = ReleaseType.Vacation,
    val typePickerOpen: Boolean = false,
    val existingAbsences: Map<Int, ReleaseType> = emptyMap(),
    val isSaving: Boolean = false,
    val done: Boolean = false,
)
