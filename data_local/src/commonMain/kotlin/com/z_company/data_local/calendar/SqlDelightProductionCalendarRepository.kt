package com.z_company.data_local.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.domain.entities.ProductionCalendarDay
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.repositories.ProductionCalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightProductionCalendarRepository : ProductionCalendarRepository, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getByYearMonthFlow(country: String, year: Int, month: Int): Flow<List<ProductionCalendarDay>> =
        db.productionCalendarDayQueries.getByYearMonth(country, year.toLong(), month.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getByYearMonth(country: String, year: Int, month: Int): List<ProductionCalendarDay> =
        db.productionCalendarDayQueries.getByYearMonth(country, year.toLong(), month.toLong())
            .executeAsList()
            .map { it.toDomain() }

    override fun saveAll(days: List<ProductionCalendarDay>): Flow<ResultState<Unit>> = flowRequest {
        days.forEach { day ->
            db.productionCalendarDayQueries.insertOrReplace(
                country = day.country,
                year = day.year.toLong(),
                month = day.month.toLong(),
                dayOfMonth = day.dayOfMonth.toLong(),
                tag = day.tag.name
            )
        }
    }

    override fun clearAll(): Flow<ResultState<Unit>> = flowRequest {
        db.productionCalendarDayQueries.clearAll()
    }

    override fun hasDataForYear(country: String, year: Int): Boolean =
        db.productionCalendarDayQueries.hasDataForYear(country, year.toLong())
            .executeAsOne() > 0

    private fun com.z_company.data_local.setting.db.ProductionCalendarDay.toDomain(): ProductionCalendarDay =
        ProductionCalendarDay(
            country = country,
            year = year.toInt(),
            month = month.toInt(),
            dayOfMonth = dayOfMonth.toInt(),
            tag = runCatching { TagForDay.valueOf(tag) }.getOrElse { TagForDay.WORKING_DAY }
        )
}
