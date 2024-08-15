package com.z_company.settings.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
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
    private var saveOtherMonthJob: Job? = null
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

    private var otherMonthOfYear: MutableList<MonthOfYear> = mutableListOf()
    private var allMonthOfYear: List<MonthOfYear> = listOf()

    fun setCurrentMonth(yearAndMonth: Pair<Int, Int>) {
        setCalendarJob?.cancel()
        setCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                result.data.find {
                    it.year == yearAndMonth.first && it.month == yearAndMonth.second
                }?.let { selectMonthOfYear ->
                    currentMonthOfYear = selectMonthOfYear
                    saveCurrentMonthInLocal(selectMonthOfYear)
                    setReleasePeriodState(selectMonthOfYear)
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun saveCurrentMonthInLocal(monthOfYear: MonthOfYear) {
        saveCurrentMonthJob?.cancel()
        saveCurrentMonthJob =
            settingsUseCase.setCurrentMonthOfYear(monthOfYear).launchIn(viewModelScope)
    }

    fun addReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.add(period)
        changeReleasePeriod()
    }

    fun deleteReleasePeriod(period: ReleasePeriod) {
        releasePeriodListState.remove(period)
        changeReleasePeriod()
    }

    private fun loadMonthList() {
        loadCalendarJob?.cancel()
        loadCalendarJob = calendarUseCase.loadMonthOfYearList().onEach { result ->
            if (result is ResultState.Success) {
                allMonthOfYear = result.data
                _uiState.update { state ->
                    state.copy(
                        monthList = result.data.map { it.month }.distinct().sorted(),
                        yearList = result.data.map { it.year }.distinct().sorted()
                    )
                }
            }
        }
            .launchIn(viewModelScope)
    }

    private fun changeReleasePeriod() {
        currentMonthOfYear?.let { monthOfYear ->
            val newDays: MutableList<Day> = monthOfYear.days.toMutableList()
            val changingDateList = mutableListOf<Day>()
            uiState.value.releaseDaysPeriodState?.let { periodList ->
                if (periodList.isEmpty()) {
                    newDays.forEachIndexed { index, day ->
                        val newDay = day.copy(isReleaseDay = false)
                        newDays[index] = newDay
                    }
                } else {
                    periodList.forEach { releasePeriod ->
                        val releaseCalendarList: MutableList<Calendar> = mutableListOf()
                        val firsDay = getInstance().also {
                            it.timeInMillis = releasePeriod.days.first().timeInMillis
                        }
                        if (releasePeriod.days.size > 1) {
                            val day = getInstance().also {
                                it.timeInMillis = firsDay.timeInMillis
                            }

                            while (!day.after(releasePeriod.days.last())) {
                                val nextDay = getInstance().also {
                                    it.timeInMillis = day.timeInMillis
                                }
                                releaseCalendarList.add(nextDay)
                                day.add(Calendar.DATE, 1)
                            }
                        } else {
                            releaseCalendarList.add(firsDay)
                        }
                        newDays.forEachIndexed { index, day ->
                            if (!changingDateList.contains(day)) {
                                val foundDay = releaseCalendarList.find { calendar ->
                                    calendar.get(DAY_OF_MONTH) == day.dayOfMonth
                                }
                                if (foundDay == null) {
                                    val newDay = day.copy(isReleaseDay = false)
                                    newDays[index] = newDay
                                } else {
                                    if (foundDay.get(MONTH) == monthOfYear.month) {
                                        val newDay = day.copy(isReleaseDay = true)
                                        newDays[index] = newDay
                                        changingDateList.add(newDay)
                                    } else {
                                        setOtherMonthOfYear(foundDay)
                                    }
                                }
                            }
                        }
                    }
                }
                currentMonthOfYear = monthOfYear.copy(
                    days = newDays
                )
            }
        }
    }

    private fun setOtherMonthOfYear(date: Calendar) {
        val foundMonthOfYear = otherMonthOfYear.find {
            it.year == date.get(YEAR) && it.month == date.get(MONTH)
        }
        if (foundMonthOfYear == null) {
            val monthOfYear = allMonthOfYear.find {
                it.year == date.get(YEAR) && it.month == date.get(MONTH)
            }
            monthOfYear?.let { month ->
                val days: MutableList<Day> = month.days.toMutableList()
                val foundDay = days.find {
                    it.dayOfMonth == date.get(DAY_OF_MONTH)
                }
                foundDay?.let {
                    val newDay = it.copy(
                        isReleaseDay = true
                    )
                    days[date.get(DAY_OF_MONTH) - 1] = newDay
                }
                val newMonthOfYear = month.copy(
                    days = days
                )
                otherMonthOfYear.add(newMonthOfYear)
            }
        } else {
            val days: MutableList<Day> = foundMonthOfYear.days.toMutableList()
            val foundDay = days.find {
                it.dayOfMonth == date.get(DAY_OF_MONTH)
            }
            foundDay?.let {
                val newDay = it.copy(
                    isReleaseDay = true
                )
                days[date.get(DAY_OF_MONTH) - 1] = newDay
            }
            val newMonthOfYear = foundMonthOfYear.copy(
                days = days
            )
            val index = otherMonthOfYear.indexOf(foundMonthOfYear)
            otherMonthOfYear[index] = newMonthOfYear
        }
    }

    private suspend fun saveNormaOtherMonth() {
        otherMonthOfYear.forEach { monthOfYear ->
            saveOtherMonthJob?.cancel()
            saveOtherMonthJob = calendarUseCase.updateMonthOfYear(monthOfYear).onEach {
                if (it is ResultState.Success) {
                    saveOtherMonthJob?.cancel()
                }
            }.launchIn(viewModelScope)
            saveOtherMonthJob?.join()
        }
    }

    fun saveNormaHours() {
        currentMonthOfYear?.let { monthOfYar ->
            saveCurrentMonthJob?.cancel()
            saveCurrentMonthJob =
                calendarUseCase.updateMonthOfYear(monthOfYar).onEach { resultState ->
                    _uiState.update {
                        it.copy(saveReleaseDaysState = resultState)
                    }
                }.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            saveNormaOtherMonth()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsUseCase.getCurrentSettings().collect { result ->
                if (result is ResultState.Success) {
                    _uiState.update {
                        it.copy(
                            currentMonthOfYearState = ResultState.Success(result.data?.selectMonthOfYear)
                        )
                    }
                    result.data?.selectMonthOfYear?.let {
                        setReleasePeriodState(it)
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