package com.example.data_local.calendar

import com.example.core.ResultState
import com.example.core.ResultState.Companion.flowMap
import com.example.domain.entities.MonthOfYear
import com.example.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class CalendarRepositoryImpl : CalendarRepositories {

    private val monthOfYearList = listOf(
        MonthOfYear(1, 2024),
        MonthOfYear(2, 2024),
        MonthOfYear(3, 2024),
        MonthOfYear(4, 2024),
        MonthOfYear(5, 2024),
        MonthOfYear(6, 2024),
        MonthOfYear(7, 2024),
        MonthOfYear(8, 2024),
        MonthOfYear(9, 2024),
        MonthOfYear(10, 2024),
        MonthOfYear(11, 2024),
        MonthOfYear(12, 2024),

        MonthOfYear(1, 2023),
        MonthOfYear(2, 2023),
        MonthOfYear(3, 2023),
        MonthOfYear(4, 2023),
    )

    private val monthList = monthOfYearList.map {
        it.month
    }

    private val yearList = monthOfYearList.map {
        it.year
    }

    override fun getCurrentMonth(): Flow<ResultState<MonthOfYear>> {
        return flowMap {
            flow {
                monthOfYearList.find {
                    it.month == Calendar.getInstance().get(Calendar.MONTH)
                }
            }
        }
    }

    override fun getMonthList(): Flow<ResultState<List<Int>>> {
        return flowMap {
            flow {
                monthList
            }
        }
    }

    override fun getYearList(): Flow<ResultState<List<Int>>> {
        return flowMap {
            flow {
                yearList
            }
        }
    }


}