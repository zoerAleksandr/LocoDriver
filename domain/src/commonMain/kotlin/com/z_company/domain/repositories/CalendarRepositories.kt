package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.calendar.RegionalHoliday
import kotlinx.coroutines.flow.Flow

interface CalendarRepositories {
    fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>>
    fun getFlowMonthOfYearListState(): Flow<List<MonthOfYear>>
    fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>>
    suspend fun getMonthOfYearById(id: String): MonthOfYear

    fun clearCalendar(): Flow<ResultState<Unit>>
    fun getMonthOfYearList(): List<MonthOfYear>

    // Локальный кеш названий региональных праздников (заполняется при выборе региона).
    suspend fun saveRegionalHolidays(holidays: List<RegionalHoliday>)
    suspend fun getRegionalHolidays(region: String, year: Int): List<RegionalHoliday>
}