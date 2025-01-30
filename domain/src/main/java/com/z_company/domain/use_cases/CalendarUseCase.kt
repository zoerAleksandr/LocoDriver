package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow

class CalendarUseCase(private val repositories: CalendarRepositories) {
    fun loadFlowMonthOfYearListState(): Flow<ResultState<List<MonthOfYear>>> {
        return repositories.getFlowMonthOfYearListState()
    }

    fun loadMonthOfYearList(): List<MonthOfYear> = repositories.getMonthOfYearList()

    /* For save Calendar in local storage after loading */
    fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>> {
        return repositories.saveCalendar(calendar)
    }

    fun clearCalendar(): Flow<ResultState<Unit>> {
        return repositories.clearCalendar()
    }

    fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return repositories.updateMonthOfYear(monthOfYear)
    }

    suspend fun loadMonthOfYearById(monthOfYearId: String): MonthOfYear {
        return repositories.getMonthOfYearById(monthOfYearId)
    }
}