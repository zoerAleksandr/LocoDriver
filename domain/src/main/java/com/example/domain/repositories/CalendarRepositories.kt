package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import kotlinx.coroutines.flow.Flow

interface CalendarRepositories {
    fun getCurrentMonth(): Flow<ResultState<MonthOfYear>>
    fun getMonthList(): Flow<ResultState<List<Int>>>
    fun getYearList(): Flow<ResultState<List<Int>>>
}