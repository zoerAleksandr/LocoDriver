package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.repositories.CalendarRepositories
import kotlinx.coroutines.flow.Flow

class CalendarUseCase(private val repositories: CalendarRepositories) {
    fun loadFlowMonthOfYearListState(): Flow<List<MonthOfYear>> {
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

    /**
     * Применяет данные производственного календаря к MonthOfYear.
     * Для каждого месяца обновляет теги дней. Создаёт MonthOfYear если не существует.
     */
    fun applyProductionCalendar(days: List<ProductionCalendarDay>): Flow<ResultState<Unit>> = flowRequest {
        val byMonth = days.groupBy { it.year to it.month }
        val allMonths = repositories.getMonthOfYearList()
        val monthMap = allMonths.associateBy { it.year to it.month }

        byMonth.forEach { (yearMonth, calDays) ->
            val (year, month) = yearMonth
            val existing = monthMap[yearMonth]
            val tagByDay = calDays.associateBy { it.dayOfMonth }

            val updatedDays = if (existing != null && existing.days.isNotEmpty()) {
                existing.days.map { day ->
                    day.copy(tag = tagByDay[day.dayOfMonth]?.tag ?: day.tag)
                }
            } else {
                calDays.sortedBy { it.dayOfMonth }.map { pd ->
                    Day(dayOfMonth = pd.dayOfMonth, tag = pd.tag)
                }
            }

            val updatedMonth = existing?.copy(days = updatedDays) ?: MonthOfYear(
                year = year,
                month = month,
                days = updatedDays
            )
            repositories.updateMonthOfYear(updatedMonth).collect {}
        }
    }
}