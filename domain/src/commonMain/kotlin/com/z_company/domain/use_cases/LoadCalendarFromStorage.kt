package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.CalendarStorage
import kotlinx.coroutines.flow.Flow

class LoadCalendarFromStorage(private val repositories: CalendarStorage) {
    fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return repositories.getMonthOfYearList()
    }
}