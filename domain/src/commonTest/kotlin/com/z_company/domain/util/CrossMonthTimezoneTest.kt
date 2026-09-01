@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.domain.util

import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.entities.route.Route
import com.z_company.domain.entities.route.UtilsForEntities.clipToMonth
import com.z_company.domain.entities.setting.CrossMonthTimezone
import com.z_company.domain.entities.setting.UserSettings
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CrossMonthTimezoneTest {
    private val hour = 3_600_000L

    private fun utc(year: Int = 2025, month: Int, day: Int, hour: Int): Long =
        LocalDateTime(year, month, day, hour, 0)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()

    private fun context(mode: CrossMonthTimezone): TimeCalculationContext =
        TimeCalculationContext.from(UserSettings(
            timeZone = 7 * hour,
            crossMonthTimezone = mode,
        ))

    @Test
    fun vladivostokRouteWorkedByMoscowTimeIsClippedBySelectedBusinessZone() {
        val route = Route(basicData = BasicData(
            timeStartWork = utc(month = 1, day = 31, hour = 20),
            timeEndWork = utc(month = 1, day = 31, hour = 22),
        ))
        val january = MonthOfYear(year = 2025, month = 0)
        val february = MonthOfYear(year = 2025, month = 1)

        val januaryMoscow = route.clipToMonth(january, context(CrossMonthTimezone.MOSCOW))
        assertEquals(hour, januaryMoscow!!.second - januaryMoscow.first)
        assertNull(route.clipToMonth(january, context(CrossMonthTimezone.LOCAL)))

        val februaryMoscow = route.clipToMonth(february, context(CrossMonthTimezone.MOSCOW))
        assertEquals(hour, februaryMoscow!!.second - februaryMoscow.first)
        val februaryLocal = route.clipToMonth(february, context(CrossMonthTimezone.LOCAL))
        assertEquals(2 * hour, februaryLocal!!.second - februaryLocal.first)
    }

    @Test
    fun moscowMonthBoundaryHandlesNewYearLeapAndCommonFebruary() {
        fun assertOneHourOnEachSide(route: Route, firstMonth: MonthOfYear, secondMonth: MonthOfYear) {
            val moscow = context(CrossMonthTimezone.MOSCOW)
            val first = route.clipToMonth(firstMonth, moscow)!!
            val second = route.clipToMonth(secondMonth, moscow)!!
            assertEquals(hour, first.second - first.first)
            assertEquals(hour, second.second - second.first)
        }

        assertOneHourOnEachSide(
            Route(basicData = BasicData(
                timeStartWork = utc(2024, 12, 31, 20),
                timeEndWork = utc(2024, 12, 31, 22),
            )),
            MonthOfYear(year = 2024, month = 11),
            MonthOfYear(year = 2025, month = 0),
        )
        assertOneHourOnEachSide(
            Route(basicData = BasicData(
                timeStartWork = utc(2024, 2, 29, 20),
                timeEndWork = utc(2024, 2, 29, 22),
            )),
            MonthOfYear(year = 2024, month = 1),
            MonthOfYear(year = 2024, month = 2),
        )
        assertOneHourOnEachSide(
            Route(basicData = BasicData(
                timeStartWork = utc(2025, 2, 28, 20),
                timeEndWork = utc(2025, 2, 28, 22),
            )),
            MonthOfYear(year = 2025, month = 1),
            MonthOfYear(year = 2025, month = 2),
        )
    }

    @Test
    fun utcLocalBusinessZoneClipsAtUtcMidnight() {
        val utcContext = TimeCalculationContext.from(UserSettings(
            timeZone = -3 * hour,
            crossMonthTimezone = CrossMonthTimezone.LOCAL,
        ))
        val route = Route(basicData = BasicData(
            timeStartWork = utc(month = 2, day = 28, hour = 23),
            timeEndWork = utc(month = 3, day = 1, hour = 1),
        ))

        val february = route.clipToMonth(MonthOfYear(year = 2025, month = 1), utcContext)!!
        val march = route.clipToMonth(MonthOfYear(year = 2025, month = 2), utcContext)!!
        assertEquals(hour, february.second - february.first)
        assertEquals(hour, march.second - march.first)
    }
}
