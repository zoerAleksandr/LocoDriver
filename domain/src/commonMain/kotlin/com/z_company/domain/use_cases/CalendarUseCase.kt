package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.calendar.RegionalHoliday
import com.z_company.domain.repositories.CalendarRepositories
import com.z_company.domain.repositories.RegionalHolidaysRepository
import kotlinx.coroutines.flow.Flow

class CalendarUseCase(
    private val repositories: CalendarRepositories,
    private val regionalHolidaysRepository: RegionalHolidaysRepository? = null,
) {
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

    /**
     * Применяет региональные праздники ПОВЕРХ уже загруженного стандартного
     * календаря. Региональные праздники получают приоритет — день помечается
     * как HOLIDAY независимо от того что было раньше (рабочий, сокращённый и т.д.).
     *
     * Должен вызываться ПОСЛЕ [applyProductionCalendar] для корректного результата.
     *
     * @param region код региона (ISO 3166-2, например "RU-TA"). null — снять
     *   региональные праздники (вернуть к чистому стандартному календарю).
     * @param year год для применения праздников.
     */
    fun applyRegionalHolidays(region: String?, year: Int): Flow<ResultState<Unit>> = flowRequest {
        if (region == null || regionalHolidaysRepository == null) {
            // Регион не выбран — ничего не делаем (стандартный календарь остаётся).
            return@flowRequest
        }
        val holidays = regionalHolidaysRepository.getHolidaysForRegionYear(region, year)
        if (holidays.isEmpty()) return@flowRequest

        val byMonth = holidays.groupBy { it.month }
        val allMonths = repositories.getMonthOfYearList()

        // ВАЖНО: в локальной БД может быть несколько записей MonthOfYear с одним
        // year+month (наследие старых импортов / синхронизаций). Если обновить
        // только ПЕРВЫЙ найденный, реактивный flow в SettingsViewModel может
        // подхватить ДРУГОЙ дубликат — без HOLIDAY-тегов, и норма "сбросится"
        // на стандартную через долю секунды. Поэтому обновляем ВСЕ найденные
        // дубли — тогда какой бы из них ни выбрал UI, праздник применится.
        byMonth.forEach { (month, monthHolidays) ->
            val matching = allMonths.filter { it.year == year && it.month == month }
            if (matching.isEmpty()) return@forEach
            val regionalDays = monthHolidays.map { it.dayOfMonth }.toSet()
            matching.forEach { existing ->
                val updatedDays = existing.days.map { day ->
                    if (day.dayOfMonth in regionalDays) {
                        day.copy(tag = TagForDay.HOLIDAY)
                    } else {
                        day
                    }
                }
                repositories.updateMonthOfYear(existing.copy(days = updatedDays)).collect {}
            }
        }
    }
}