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

    private fun utc(month: Int, day: Int, hour: Int): Long =
        LocalDateTime(2025, month, day, hour, 0)
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
}
