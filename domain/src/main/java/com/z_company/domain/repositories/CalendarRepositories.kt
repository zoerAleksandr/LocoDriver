package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import kotlinx.coroutines.flow.Flow

interface CalendarRepositories {
    fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>>
    fun getFlowMonthOfYearListState(): Flow<ResultState<List<MonthOfYear>>>
    fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>>
    fun getMonthOfYearById(id: String): MonthOfYear

    fun clearCalendar(): Flow<ResultState<Unit>>
    fun getMonthOfYearList(): List<MonthOfYear>
}