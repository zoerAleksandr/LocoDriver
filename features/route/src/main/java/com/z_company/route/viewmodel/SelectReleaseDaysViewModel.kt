package com.z_company.route.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.ReleaseDayUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.z_company.core.sendToSentry
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SelectReleaseDaysViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
    private val releaseDayUseCase: ReleaseDayUseCase by inject()
    private val settingsUseCase: SettingsUseCase by inject()

    private val _uiState = MutableStateFlow(SelectReleaseDaysUIState())
    val uiState = _uiState.asStateFlow()

    private var saveCurrentMonthJob: Job? = null
    private var setCalendarJob: Job? = null
    private var loadCalendarJob: Job? = null

    private var releasePeriodListState: SnapshotStateList<ReleasePeriod>
        get() {
            return uiState.value.releaseDaysPeriodState ?: mutableStateListOf()
        }
        set(value) {
            _uiState.update {
                it.copy(
                    releaseDaysPeriodState = value
                )
            }
        }

    var currentMonthOfYear: MonthOfYear?
        get() {
            return _uiState.value.currentMonthOfYearState.let {
                if (it is ResultState.Success) it.data else null
            }
        }
        private set(value) {
            _uiState.update {
                it.copy(currentMonthOfYearState = ResultState.Success(value))
            }
        }

    var allMonthOfYear: List<MonthOfYear> = listOf()
    private var newMonthList: MutableList<MonthOfYear> = mutableListOf()

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            result.find {
                it.year == yearAndMonth.first && it.month == yearAndMonth.second
            }?.let { selectMonthOfYear ->
                currentMonthOfYear = selectMonthOfYear
                saveCurrentMonthInLocal(selectMonthOfYear)
                setReleasePeriodState(selectMonthOfYear)
            }
        }.launchIn(viewModelScope)
    }

    private suspend fun saveCurrentMonthInLocal(monthOfYear: MonthOfYear) {
        saveCurrentMonthJob?.cancel()
        saveCurrentMonthJob =
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).onEach {
                if (it is ResultState.Success) {
                    saveCurrentMonthJob?.cancel()
                }
            }.launchIn(viewModelScope)
        saveCurrentMonthJob?.join()
    }

    /** Добавить период отвлечения — сохраняет в ReleaseDayRepository. */
    fun addReleasePeriod(period: ReleasePeriod) {
        viewModelScope.launch {
            try {
                releaseDayUseCase.savePeriod(period).collect { state ->
                    _uiState.update { it.copy(saveReleaseDaysState = state) }
                }
            } catch (e: Exception) {
                e.sendToSentry("SelectReleaseDaysViewModel", "addReleasePeriod")
            }
        }
    }

    /** Удалить период отвлечения — удаляет из ReleaseDayRepository. */
    fun deleteReleasePeriod(period: ReleasePeriod) {
        viewModelScope.launch {
            try {
                releaseDayUseCase.deletePeriod(period).collect { state ->
                    _uiState.update { it.copy(saveReleaseDaysState = state) }
                }
            } catch (e: Exception) {
                e.sendToSentry("SelectReleaseDaysViewModel", "deleteReleasePeriod")
            }
        }
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
            allMonthOfYear = result
            _uiState.update { state ->
                state.copy(
                    monthList = result.map { it.month }.distinct().sorted(),
                    yearList = result.map { it.year }.distinct().sorted()
                )
            }
            newMonthList = result.toMutableList()
        }.launchIn(viewModelScope)
    }

    /** Сохранить ставку тарифа и прочие поля MonthOfYear (не касается отвлечений). */
    fun saveNormaHours() {
        viewModelScope.launch {
            try {
                currentMonthOfYear?.let { monthOfYear ->
                    newMonthList.forEach { month ->
                        if (month.month == monthOfYear.month && month.year == monthOfYear.year) {
                            saveCurrentMonthInLocal(month)
                        }
                        saveCurrentMonthJob?.cancel()
                        saveCurrentMonthJob =
                            calendarUseCase.updateMonthOfYear(month).onEach { resultState ->
                                _uiState.update {
                                    it.copy(saveReleaseDaysState = resultState)
                                }
                                if (resultState is ResultState.Success) {
                                    saveCurrentMonthJob?.cancel()
                                }
                            }.launchIn(viewModelScope)
                        saveCurrentMonthJob?.join()
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SelectReleaseDaysViewModel", "saveNormaHours")
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                settingsUseCase.getFlowCurrentSettingsState().collect { result ->
                    if (result is ResultState.Success) {
                        result.data?.let { setting ->
                            _uiState.update {
                                it.copy(
                                    currentMonthOfYearState = ResultState.Success(setting.selectMonthOfYear),
                                    dateAndTimeConverter = DateAndTimeConverter(setting)
                                )
                            }
                            setReleasePeriodState(setting.selectMonthOfYear)
                        }
                    }
                }
            } catch (e: Exception) {
                e.sendToSentry("SelectReleaseDaysViewModel", "loadSettings")
            }
        }
    }

    /**
     * Строит список ReleasePeriod из дней MonthOfYear.
     * Дни уже обогащены release-флагами через SqlDelightCalendarRepository.combine().
     */
    private fun setReleasePeriodState(monthOfYear: MonthOfYear) {
        val releaseDays = monthOfYear.days.filter { it.isReleaseDay && it.releaseType != null }
            .map { day ->
                com.z_company.domain.entities.ReleaseDay(
                    year = monthOfYear.year,
                    month = monthOfYear.month,
                    dayOfMonth = day.dayOfMonth,
                    releaseType = day.releaseType!!
                )
            }
        val periods = releaseDayUseCase.toReleasePeriods(releaseDays)
        val newList = mutableStateListOf<ReleasePeriod>()
        newList.addAll(periods)
        releasePeriodListState = newList
    }

    init {
        loadMonthList()
        loadSettings()
    }
}
