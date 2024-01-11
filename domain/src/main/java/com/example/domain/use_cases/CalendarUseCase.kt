package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow

class CalendarUseCase(private val repositories: CalendarRepositories) {
    fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return repositories.getMonthOfYearList()
    }
}