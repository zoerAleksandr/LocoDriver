package com.z_company.data_local.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.z_company.core.ResultState
import com.z_company.core.ResultState.Companion.flowRequest
import com.z_company.data_local.setting.db.SettingsDatabase
import com.z_company.data_local.setting.mapping.MonthOfYearMapper
import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.repositories.CalendarRepositories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SqlDelightCalendarRepository : CalendarRepositories, KoinComponent {
    private val db: SettingsDatabase by inject()

    override fun getFlowMonthOfYearListState(): Flow<List<MonthOfYear>> {
        val monthsFlow = db.monthOfYearQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { MonthOfYearMapper.toData(it) } }

        val releaseFlow = db.releaseDayQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)

        return combine(monthsFlow, releaseFlow) { months, releaseRows ->
            val releaseByMonthKey = releaseRows.groupBy { it.year to it.month }
            months.map { month ->
                val key = month.year.toLong() to month.month.toLong()
                val releaseDaysForMonth = releaseByMonthKey[key]?.map { row ->
                    ReleaseDay(
                        id = row.id,
                        year = row.year.toInt(),
                        month = row.month.toInt(),
                        dayOfMonth = row.dayOfMonth.toInt(),
                        releaseType = releaseTypeFromText(row.releaseType)
                    )
                } ?: emptyList()

                if (releaseDaysForMonth.isEmpty()) {
                    // Ещё не мигрировано — старые days из MonthOfYear содержат isReleaseDay
                    month
                } else {
                    // Мигрировано — мержим теги из MonthOfYear.days и флаги из ReleaseDay
                    mergeReleaseDays(month, releaseDaysForMonth)
                }
            }
        }
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

    /**
     * Мержит теги из MonthOfYear.days (производственный календарь)
     * с личными отвлечениями из таблицы ReleaseDay.
     * Производственный календарь остаётся нетронутым, только isReleaseDay/releaseType обновляются.
     */
    private fun mergeReleaseDays(month: MonthOfYear, releaseDays: List<ReleaseDay>): MonthOfYear {
        val releaseByDayOfMonth = releaseDays.associateBy { it.dayOfMonth }
        val mergedDays = month.days.map { day ->
            val releaseDay = releaseByDayOfMonth[day.dayOfMonth]
            day.copy(
                isReleaseDay = releaseDay != null,
                releaseType = releaseDay?.releaseType
            )
        }
        return month.copy(days = mergedDays)
    }

    private fun releaseTypeFromText(text: String): ReleaseType = when (text) {
        "Отпуск" -> ReleaseType.Vacation
        "Больничный" -> ReleaseType.SickLeave
        "Курсы" -> ReleaseType.Courses
        "Донорские" -> ReleaseType.Donor
        "По уходу за ребенком-инвалидом" -> ReleaseType.ChildCare
        else -> ReleaseType.Other
    }
}
