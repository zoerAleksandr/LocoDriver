package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.core.util.DateAndTimeConverter
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleasePeriod
import com.z_company.domain.use_cases.CalendarUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.MONTH
import java.util.Calendar.YEAR
import java.util.Calendar.getInstance

class SelectReleaseDaysViewModel : ViewModel(), KoinComponent {
    private val calendarUseCase: CalendarUseCase by inject()
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

    private var allMonthOfYear: List<MonthOfYear> = listOf()
    private var newMonthList: MutableList<MonthOfYear> = mutableListOf()

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
//            if (result is ResultState.Success) {
                result.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    currentMonthOfYear = selectMonthOfYear
                    saveCurrentMonthInLocal(selectMonthOfYear)
                    setReleasePeriodState(selectMonthOfYear)
                }
//            }
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

    fun addReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.add(period)
        testAddPeriodInMonthOfYear(period)
    }

    fun deleteReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.remove(period)
        testRemovePeriodInMonthOfYear(period)
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadFlowMonthOfYearListState().onEach { result ->
//            if (result is ResultState.Success) {
                allMonthOfYear = result
                _uiState.update { state ->
                    state.copy(
                        monthList = result.map { it.month }.distinct().sorted(),
                        yearList = result.map { it.year }.distinct().sorted()
                    )
                }
                newMonthList = result.toMutableList()

//            }
        }.launchIn(viewModelScope)
    }

    private fun testAddPeriodInMonthOfYear(period: ReleasePeriod) {
        period.days.forEach { releaseDay ->
            val searchMonth = newMonthList.find {
                it.month == releaseDay.get(MONTH) && it.year == releaseDay.get(YEAR)
            }
            searchMonth?.let {
                val indexMonthList = newMonthList.indexOf(searchMonth)
                if (indexMonthList != -1) {
                    val day = newMonthList[indexMonthList].days.find { d ->
                        d.dayOfMonth == releaseDay.get(DAY_OF_MONTH)
                    }
                    day?.let {
                        val indexDay = newMonthList[indexMonthList].days.indexOf(it)
                        val days = newMonthList[indexMonthList].days.toMutableList()
                        days[indexDay] = day.copy(isReleaseDay = true)
                        val newMonth = newMonthList[indexMonthList].copy(
                            days = days
                        )
                        newMonthList[indexMonthList] = newMonth
                        if (newMonth.month == currentMonthOfYear?.month && newMonth.year == currentMonthOfYear?.year) {
                            currentMonthOfYear = newMonth
                        }
                    }
                }
            }
        }
    }

    private fun testRemovePeriodInMonthOfYear(period: ReleasePeriod) {
        period.days.forEach { releaseDay ->
            val searchMonth = newMonthList.find {
                it.month == releaseDay.get(MONTH) && it.year == releaseDay.get(YEAR)
            }
            searchMonth?.let {
                val indexMonthList = newMonthList.indexOf(searchMonth)
                if (indexMonthList != -1) {
                    val day = newMonthList[indexMonthList].days.find { d ->
                        d.dayOfMonth == releaseDay.get(DAY_OF_MONTH)
                    }
                    day?.let {
                        val indexDay = newMonthList[indexMonthList].days.indexOf(it)
                        val days = newMonthList[indexMonthList].days.toMutableList()
                        days[indexDay] = day.copy(isReleaseDay = false)
                        val newMonth = newMonthList[indexMonthList].copy(
                            days = days
                        )
                        newMonthList[indexMonthList] = newMonth
                        if (newMonth.month == currentMonthOfYear?.month && newMonth.year == currentMonthOfYear?.year) {
                            currentMonthOfYear = newMonth
                        }
                    }
                }
            }
        }
    }

    fun saveNormaHours() {
        viewModelScope.launch {
            currentMonthOfYear?.let { monthOfYear ->
                newMonthList.forEach {
                    if (it.month == monthOfYear.month && it.year == monthOfYear.year) {
                        saveCurrentMonthInLocal(it)
                    }
                    saveCurrentMonthJob?.cancel()
                    saveCurrentMonthJob =
                        calendarUseCase.updateMonthOfYear(it).onEach { resultState ->
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
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
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
        }
    }

    private fun setReleasePeriodState(monthOfYear: MonthOfYear) {
        releasePeriodListState.clear()
        val listReleasePeriod = mutableListOf<Calendar>()
        var isBegunCounting = false
        monthOfYear.days.forEachIndexed { index, day ->
            if (day.isReleaseDay) {
                if (!isBegunCounting) {
                    isBegunCounting = true
                }
                listReleasePeriod.add(
                    getInstance().also {
                        it.set(DAY_OF_MONTH, day.dayOfMonth)
                        it.set(MONTH, monthOfYear.month)
                    }
                )
                if ((index + 1 == monthOfYear.days.size)) {
                    isBegunCounting = false
                    val copyList = mutableListOf<Calendar>()
                    copyList.addAll(listReleasePeriod)
                    releasePeriodListState.add(
                        ReleasePeriod(
                            days = copyList
                        )
                    )
                    listReleasePeriod.clear()
                }

            } else {
                if (isBegunCounting) {
                    isBegunCounting = false
                    val copyList = mutableListOf<Calendar>()
                    copyList.addAll(listReleasePeriod)
                    releasePeriodListState.add(
                        ReleasePeriod(
                            days = copyList
                        )
                    )
                    listReleasePeriod.clear()
                }
            }
        }
    }

    init {
        loadMonthList()
        loadSettings()
    }
}