package com.z_company.domain.use_cases

import com.z_company.core.ResultState
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.repositories.ProductionCalendarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ProductionCalendarUseCase(private val repository: ProductionCalendarRepository) {

    fun getByYearMonthFlow(country: String, year: Int, month: Int): Flow<List<ProductionCalendarDay>> =
        repository.getByYearMonthFlow(country, year, month)

    fun getByYearMonth(country: String, year: Int, month: Int): List<ProductionCalendarDay> =
        repository.getByYearMonth(country, year, month)

    /** Сохранить данные производственного календаря (после загрузки с сервера) */
    fun saveCalendar(days: List<ProductionCalendarDay>): Flow<ResultState<Unit>> =
        repository.saveAll(days)

    /** Нужно ли загружать с сервера (нет данных за год) */
    fun needsFetch(country: String, year: Int): Boolean =
        !repository.hasDataForYear(country, year)

    fun clearAll(): Flow<ResultState<Unit>> = repository.clearAll()

    /**
     * Возвращает год следующего года для загрузки (если сейчас декабрь — возвращает следующий год,
     * иначе null).
     */
    fun nextYearToFetchIfDecember(currentMonth: Int, currentYear: Int): Int? =
        if (currentMonth == 12) currentYear + 1 else null
}
