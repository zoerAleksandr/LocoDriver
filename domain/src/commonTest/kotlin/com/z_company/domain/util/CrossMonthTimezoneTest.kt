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

    private fun utc(year: Int = 2025, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime(year, month, day, hour, minute)
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

    @Test
    fun positiveWholeOffsetClipsAtItsOwnLocalMidnight() {
        // Екатеринбург: UTC+5, в настройке хранится +2 часа относительно Москвы.
        val yekaterinburg = TimeCalculationContext.from(UserSettings(
            timeZone = 2 * hour,
            crossMonthTimezone = CrossMonthTimezone.LOCAL,
        ))
        val route = Route(basicData = BasicData(
            timeStartWork = utc(month = 1, day = 31, hour = 18),
            timeEndWork = utc(month = 1, day = 31, hour = 20),
        ))

        val january = route.clipToMonth(MonthOfYear(year = 2025, month = 0), yekaterinburg)!!
        val february = route.clipToMonth(MonthOfYear(year = 2025, month = 1), yekaterinburg)!!
        assertEquals(hour, january.second - january.first)
        assertEquals(hour, february.second - february.first)
    }

    @Test
    fun negativeWholeOffsetClipsAtItsOwnLocalMidnight() {
        // UTC-5: в настройке хранится -8 часов относительно Москвы.
        val utcMinusFive = TimeCalculationContext.from(UserSettings(
            timeZone = -8 * hour,
            crossMonthTimezone = CrossMonthTimezone.LOCAL,
        ))
        val route = Route(basicData = BasicData(
            timeStartWork = utc(month = 2, day = 1, hour = 4),
            timeEndWork = utc(month = 2, day = 1, hour = 6),
        ))

        val january = route.clipToMonth(MonthOfYear(year = 2025, month = 0), utcMinusFive)!!
        val february = route.clipToMonth(MonthOfYear(year = 2025, month = 1), utcMinusFive)!!
        assertEquals(hour, january.second - january.first)
        assertEquals(hour, february.second - february.first)
    }

    @Test
    fun extremeAndFractionalFixedOffsetsClipWithoutLosingTime() {
        // Настройка хранит разницу относительно Москвы (UTC+3):
        // UTC-12 = -15 ч, UTC+14 = +11 ч; также проверяем +05:30 и +05:45.
        val offsetsFromMoscow = listOf(
            -15 * hour,
            11 * hour,
            2 * hour + 30 * 60_000L,
            2 * hour + 45 * 60_000L,
        )

        offsetsFromMoscow.forEach { offsetFromMoscow ->
            val context = TimeCalculationContext.from(UserSettings(
                timeZone = offsetFromMoscow,
                crossMonthTimezone = CrossMonthTimezone.LOCAL,
            ))
            val utcOffset = 3 * hour + offsetFromMoscow
            val localMidnightUtc = utc(2025, 2, 1, 0) - utcOffset
            val route = Route(basicData = BasicData(
                timeStartWork = localMidnightUtc - hour,
                timeEndWork = localMidnightUtc + hour,
            ))

            val january = route.clipToMonth(MonthOfYear(year = 2025, month = 0), context)!!
            val february = route.clipToMonth(MonthOfYear(year = 2025, month = 1), context)!!
            assertEquals(hour, january.second - january.first, "offset=$offsetFromMoscow")
            assertEquals(hour, february.second - february.first, "offset=$offsetFromMoscow")
        }
    }
}
