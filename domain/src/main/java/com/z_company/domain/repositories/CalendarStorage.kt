package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import kotlinx.coroutines.flow.Flow

interface CalendarStorage {
    fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>>
}