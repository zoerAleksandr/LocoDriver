package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarStorage
import kotlinx.coroutines.flow.Flow

class LoadCalendarFromStorage(private val repositories: CalendarStorage) {
    fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return repositories.getMonthOfYearList()
    }
}