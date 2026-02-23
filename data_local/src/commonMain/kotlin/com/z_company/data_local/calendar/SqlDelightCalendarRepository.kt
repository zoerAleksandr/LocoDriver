package com.z_company.data_local.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.mapping.MonthOfYearMapper
import com.z_company.data_local.setting.mapping.settingsJson
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.repositories.CalendarRepositories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightCalendarRepository : CalendarRepositories, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getFlowMonthOfYearListState(): Flow<List<MonthOfYear>> {
        return db.monthOfYearQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { MonthOfYearMapper.toData(it) } }
    }

    override fun updateMonthOfYear(monthOfYear: MonthOfYear): Flow<ResultState<Unit>> {
        return flowRequest {
            db.monthOfYearQueries.insertOrReplace(
                id = monthOfYear.id,
                year = monthOfYear.year.toLong(),
                month = monthOfYear.month.toLong(),
                days = MonthOfYearMapper.encodeDays(monthOfYear.days),
                tariffRate = monthOfYear.tariffRate,
                dateSetTariffRate = MonthOfYearMapper.encodeDateSetTariffRate(monthOfYear.dateSetTariffRate)
            )
        }
    }

    override suspend fun getMonthOfYearById(id: String): MonthOfYear {
        val row = db.monthOfYearQueries.getById(id).executeAsOneOrNull()
        return row?.let { MonthOfYearMapper.toData(it) } ?: MonthOfYear()
    }

    override fun clearCalendar(): Flow<ResultState<Unit>> {
        return flowRequest { db.monthOfYearQueries.deleteAll() }
    }

    override fun getMonthOfYearList(): List<MonthOfYear> {
        return db.monthOfYearQueries.getAll().executeAsList().map { MonthOfYearMapper.toData(it) }
    }

    override fun saveCalendar(calendar: List<MonthOfYear>): Flow<ResultState<Unit>> {
        return flowRequest {
            calendar.forEach { monthOfYear ->
                db.monthOfYearQueries.insertOrReplace(
                    id = monthOfYear.id,
                    year = monthOfYear.year.toLong(),
                    month = monthOfYear.month.toLong(),
                    days = MonthOfYearMapper.encodeDays(monthOfYear.days),
                    tariffRate = monthOfYear.tariffRate,
                    dateSetTariffRate = MonthOfYearMapper.encodeDateSetTariffRate(monthOfYear.dateSetTariffRate)
                )
            }
        }
    }
}
