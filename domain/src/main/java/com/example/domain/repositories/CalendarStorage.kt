package com.example.domain.repositories

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import kotlinx.coroutines.flow.Flow

interface CalendarStorage {
    fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>>
}