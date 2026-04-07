package com.z_company.domain.repositories

import com.z_company.core.ResultState
import com.z_company.domain.entities.ProductionCalendarDay
import kotlinx.coroutines.flow.Flow

interface ProductionCalendarRepository {
    /** Поток дней производственного календаря за месяц */
    fun getByYearMonthFlow(country: String, year: Int, month: Int): Flow<List<ProductionCalendarDay>>

    /** Разовый запрос */
    fun getByYearMonth(country: String, year: Int, month: Int): List<ProductionCalendarDay>

    /** Сохранить календарь (после загрузки с сервера) */
    fun saveAll(days: List<ProductionCalendarDay>): Flow<ResultState<Unit>>

    /** Удалить все записи (при смене страны / полной перезагрузке) */
    fun clearAll(): Flow<ResultState<Unit>>

    /** Есть ли данные за год */
    fun hasDataForYear(country: String, year: Int): Boolean
}
