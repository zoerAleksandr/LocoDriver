package com.z_company.data_local.calendar

import com.z_company.core.ResultState
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.TagForDay.NON_WORKING_DAY
import com.z_company.domain.entities.TagForDay.WORKING_DAY
import com.z_company.domain.entities.TagForDay.SHORTENED_DAY
import com.z_company.domain.repositories.CalendarStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList

class CalendarStorageLocalImpl : CalendarStorage {

    private val monthOfYearList = listOf(
        MonthOfYear( year = 2023, month = 0, days = listOf()),
        MonthOfYear( year = 2023, month = 1, days = listOf()),
        MonthOfYear( year = 2023, month = 2, days = listOf()),
        MonthOfYear( year = 2023, month = 5, days = listOf()),
        MonthOfYear( year = 2023, month = 3, days = listOf()),
        MonthOfYear( year = 2023, month = 4, days = listOf()),
        MonthOfYear( year = 2023, month = 5, days = listOf()),
        MonthOfYear( year = 2023, month = 6, days = listOf()),
        MonthOfYear( year = 2023, month = 7, days = listOf()),
        MonthOfYear( year = 2023, month = 8, days = listOf()),
        MonthOfYear( year = 2023, month = 9, days = listOf()),
        MonthOfYear(year = 2023, month = 10, days = listOf()),
        MonthOfYear(year = 2023, month = 11, days = listOf()),
        MonthOfYear(year = 2024, month = 0, days = listOf()),
        MonthOfYear(year = 2024, month = 1, days = listOf()),
        MonthOfYear(year = 2024, month = 2, days = listOf()),
        MonthOfYear(year = 2024, month = 3, days = listOf()),
        MonthOfYear(year = 2024, month = 4, days = listOf()),
        MonthOfYear(
            year = 2024,
            month = 5,
            days = listOf(
                Day(1, NON_WORKING_DAY), Day(2, NON_WORKING_DAY),
                Day(3, WORKING_DAY), Day(4, WORKING_DAY), Day(5, WORKING_DAY), Day(6, WORKING_DAY), Day(7, WORKING_DAY), Day(8, NON_WORKING_DAY), Day(9, NON_WORKING_DAY),
                Day(10, WORKING_DAY), Day(11, SHORTENED_DAY), Day(12, NON_WORKING_DAY), Day(13, WORKING_DAY), Day(14, WORKING_DAY), Day(15, NON_WORKING_DAY), Day(16, NON_WORKING_DAY),
                Day(17, WORKING_DAY), Day(18, WORKING_DAY), Day(19, WORKING_DAY), Day(20, WORKING_DAY), Day(21, WORKING_DAY), Day(22, NON_WORKING_DAY), Day(23, NON_WORKING_DAY),
                Day(24, WORKING_DAY), Day(25, WORKING_DAY), Day(26, WORKING_DAY), Day(27, WORKING_DAY), Day(28, WORKING_DAY), Day(29, NON_WORKING_DAY), Day(30, NON_WORKING_DAY),
            )
        ),
        MonthOfYear(year = 2024, month = 6, days = listOf()),
        MonthOfYear(year = 2024, month = 7, days = listOf()),
        MonthOfYear(year = 2024, month = 8, days = listOf()),
        MonthOfYear(year = 2024, month = 9, days = listOf()),
        MonthOfYear(year = 2024, month = 10, days = listOf()),
        MonthOfYear(year = 2024, month = 11, days = listOf())
    ).asFlow()

    override fun getMonthOfYearList(): Flow<ResultState<List<MonthOfYear>>> {
        return flow {
            emit(ResultState.Success(monthOfYearList.toList()))
        }
    }
}