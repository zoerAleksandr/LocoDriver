package com.example.locodriver.viewmodel

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.use_cases.LoadCalendarFromStorage
import com.example.domain.use_cases.CalendarUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val TAG = "MainViewModel_TAG"
class MainViewModel : ViewModel(), KoinComponent, DefaultLifecycleObserver {
    private val loadCalendarFromStorage: LoadCalendarFromStorage by inject()
    private val calendarUseCase: CalendarUseCase by inject()
    private var loadCalendarJob: Job? = null
    private var saveCalendarInLocalJob: Job? = null

    private fun loadCalendar() {
        loadCalendarJob?.cancel()
        loadCalendarJob = loadCalendarFromStorage.getMonthOfYearList().onEach { resultState ->
            if (resultState is ResultState.Success) {
                saveCalendarInLocal(resultState.data)
            }
        }.launchIn(viewModelScope)
    }

    private fun saveCalendarInLocal(calendar: List<MonthOfYear>) {
        saveCalendarInLocalJob?.cancel()
        saveCalendarInLocalJob = calendarUseCase.saveCalendar(calendar).onEach { resultState ->
            if (resultState is ResultState.Success) {
                Log.i(TAG, "production calendar is loading")
            }
        }.launchIn(viewModelScope)
    }

    init {
        loadCalendar()
    }
}