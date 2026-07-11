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
import com.z_company.domain.entities.calendar.RegionalHoliday
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

        // Производственный календарь: реактивный источник тегов (страна хранится в таблице).
        // Это позволяет теги дней обновляться сразу при смене страны — без записи в MonthOfYear.days.
        val productionFlow = db.productionCalendarDayQueries.getAll()
            .asFlow()
            .mapToList(Dispatchers.Default)

        return combine(monthsFlow, releaseFlow, productionFlow) { months, releaseRows, prodRows ->
            val releaseByMonthKey = releaseRows.groupBy { it.year to it.month }

            // Группируем производственный календарь по (year, month).
            // Если в месяце данные ровно одной страны — применяем её теги.
            // Если нескольких (переходный момент при смене страны) — используем теги из MonthOfYear.
            val prodByMonthKey = prodRows.groupBy { it.year to it.month }

            // Дедублируем MonthOfYear по (year, month): берём с наибольшим числом дней.
            // Сортируем по (year, month) для стабильного порядка на каждом emission.
            val deduplicatedMonths = months
                .groupBy { it.year to it.month }
                .map { (_, dupes) -> dupes.maxByOrNull { it.days.size } ?: dupes.first() }
                .sortedWith(compareBy({ it.year }, { it.month }))

            deduplicatedMonths.map { month ->
                val key = month.year.toLong() to month.month.toLong()

                // Применяем теги производственного календаря
                val prodForMonth = prodByMonthKey[month.year.toLong() to month.month.toLong()]
                val monthWithProdTags = if (prodForMonth != null) {
                    val countries = prodForMonth.map { it.country }.toSet()
                    if (countries.size == 1) {
                        // Одна страна → теги актуальны, применяем.
                        // ВАЖНО: HOLIDAY-теги из MonthOfYear (региональные праздники,
                        // выставленные через applyRegionalHolidays) имеют приоритет
                        // над стандартными тегами производственного календаря —
                        // иначе регион "теряется" при каждом чтении флоу.
                        val tagByDay = prodForMonth.associateBy { it.dayOfMonth }
                        val updatedDays = month.days.map { day ->
                            if (day.tag == TagForDay.HOLIDAY) {
                                // Региональный праздник — оставляем как есть
                                day
                            } else {
                                val prodTag = tagByDay[day.dayOfMonth.toLong()]?.tag
                                    ?.let { runCatching { TagForDay.valueOf(it) }.getOrNull() }
                                if (prodTag != null) day.copy(tag = prodTag) else day
                            }
                        }
                        month.copy(days = updatedDays)
                    } else {
                        // Несколько стран — идёт переход, используем теги из MonthOfYear
                        month
                    }
                } else {
                    // Нет данных производственного календаря — используем теги из MonthOfYear
                    month
                }

                // Применяем флаги отвлечений (isReleaseDay) из таблицы ReleaseDay
                val releaseDaysForMonth = releaseByMonthKey[key]?.map { row ->
                    ReleaseDay(
                        id = row.id,
                        year = row.year.toInt(),
                        month = row.month.toInt(),
                        dayOfMonth = row.dayOfMonth.toInt(),
                        releaseType = releaseTypeFromText(row.releaseType)
                    )
                } ?: emptyList()

                mergeReleaseDays(monthWithProdTags, releaseDaysForMonth)
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

    // ── Локальный кеш названий региональных праздников ───────────────────
    override suspend fun saveRegionalHolidays(holidays: List<RegionalHoliday>) {
        db.regionalHolidayQueries.transaction {
            holidays.forEach { h ->
                db.regionalHolidayQueries.insertOrReplace(
                    region = h.region,
                    year = h.year.toLong(),
                    month = h.month.toLong(),
                    dayOfMonth = h.dayOfMonth.toLong(),
                    name = h.name,
                )
            }
        }
    }

    override suspend fun getRegionalHolidays(region: String, year: Int): List<RegionalHoliday> {
        return db.regionalHolidayQueries.getByRegionYear(region, year.toLong()).executeAsList().map { row ->
            RegionalHoliday(
                region = row.region,
                year = row.year.toInt(),
                month = row.month.toInt(),
                dayOfMonth = row.dayOfMonth.toInt(),
                name = row.name,
            )
        }
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
        "Выходной" -> ReleaseType.DayOff
        "Командировка" -> ReleaseType.BusinessTrip
        else -> ReleaseType.Other
    }
}
