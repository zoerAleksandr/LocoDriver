package com.example.domain.use_cases

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow

class CalendarUseCase(private val repositories: CalendarRepositories) {
    fun monthList(): Flow<ResultState<List<Int>>> =
        repositories.getMonthList()

    fun yearList(): Flow<ResultState<List<Int>>> =
        repositories.getYearList()

    fun getCurrentMonthOfYear(): Flow<ResultState<MonthOfYear>> =
        repositories.getCurrentMonth()
}