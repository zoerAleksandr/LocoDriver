package com.example.data_local.calendar

import com.example.core.ResultState
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

class CalendarRepositoryImpl : CalendarRepositories {

    private val monthOfYearList = listOf(
        MonthOfYear(0, year = 2023, month = 0, normaHours = 180),
        MonthOfYear(1, year = 2023, month = 1, normaHours = 180),
        MonthOfYear(2, year = 2023, month = 2, normaHours = 180),
        MonthOfYear(3, year = 2023, month = 3, normaHours = 180),
        MonthOfYear(4, year = 2023, month = 4, normaHours = 180),
        MonthOfYear(5, year = 2023, month = 5, normaHours = 180),
        MonthOfYear(6, year = 2023, month = 6, normaHours = 180),
        MonthOfYear(7, year = 2023, month = 7, normaHours = 180),
        MonthOfYear(8, year = 2023, month = 8, normaHours = 180),
        MonthOfYear(9, year = 2023, month = 9, normaHours = 180),
        MonthOfYear(10, year = 2023, month = 10, normaHours = 180),
        MonthOfYear(11, year = 2023, month = 11, normaHours = 180),
        MonthOfYear(12, year = 2024, month = 0, normaHours = 144),
        MonthOfYear(13, year = 2024, month = 1, normaHours = 159),
        MonthOfYear(14, year = 2024, month = 2, normaHours = 159),
        MonthOfYear(15, year = 2024, month = 3, normaHours = 175),
        MonthOfYear(16, year = 2024, month = 4, normaHours = 143),
        MonthOfYear(17, year = 2024, month = 5, normaHours = 151),
        MonthOfYear(18, year = 2024, month = 6, normaHours = 184),
        MonthOfYear(19, year = 2024, month = 7, normaHours = 176),
        MonthOfYear(20, year = 2024, month = 8, normaHours = 168),
        MonthOfYear(21, year = 2024, month = 9, normaHours = 184),
        MonthOfYear(22, year = 2024, month = 10, normaHours = 160),
        MonthOfYear(23, year = 2024, month = 11, normaHours = 175)
    ).asFlow()

    override fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return flow {
            emit(ResultState.Success(monthOfYearList.toList()))
        }
    }
}