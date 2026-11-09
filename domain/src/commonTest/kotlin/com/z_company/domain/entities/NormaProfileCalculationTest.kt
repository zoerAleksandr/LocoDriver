package com.z_company.domain.entities

import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHoursInPeriod
import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursExcludingWeekends
import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals

class NormaProfileCalculationTest {
    private val month = MonthOfYear(
        year = 2025,
        month = 0,
        days = listOf(
            Day(6, TagForDay.WORKING_DAY),       // понедельник
            Day(7, TagForDay.SHORTENED_DAY),     // вторник
            Day(11, TagForDay.NON_WORKING_DAY),  // суббота
        ),
    )

    @Test
    fun standardAndSixDayProfilesUseTheirOwnDailyNorms() {
        assertEquals(15, month.getPersonalNormaHours(WorkScheduleProfile.standard()))
        assertEquals(18, month.getPersonalNormaHours(WorkScheduleProfile.sixDaySevenFive()))
    }

    @Test
    fun customProfileIsUsedForWholeMonthAndTariffPeriod() {
        val custom = WorkScheduleProfile.standard()
            .withHours(DayOfWeek.MONDAY, 4)
            .withHours(DayOfWeek.TUESDAY, 5)

        assertEquals(8, month.getPersonalNormaHours(custom))
        assertEquals(4, month.getPersonalNormaHoursInPeriod(7 to 11, custom))
    }

    @Test
    fun adjacentTariffPeriodsDoNotCountBoundaryDayTwice() {
        val fullMonth = month.getPersonalNormaHours(WorkScheduleProfile.standard())
        val oldPeriod = month.getPersonalNormaHoursInPeriod(
            1 to 6,
            WorkScheduleProfile.standard(),
        )
        val newPeriod = month.getPersonalNormaHoursInPeriod(
            7 to 31,
            WorkScheduleProfile.standard(),
        )

        assertEquals(fullMonth, oldPeriod + newPeriod)
    }

    @Test
    fun childCareHoursUsePersonalWorkSchedule() {
        val childCareMonth = MonthOfYear(
            year = 2025,
            month = 0,
            days = listOf(
                Day(6, TagForDay.WORKING_DAY, isReleaseDay = true, releaseType = ReleaseType.ChildCare),
                Day(11, TagForDay.NON_WORKING_DAY, isReleaseDay = true, releaseType = ReleaseType.ChildCare),
                Day(12, TagForDay.NON_WORKING_DAY, isReleaseDay = true, releaseType = ReleaseType.ChildCare),
            ),
        )

        assertEquals(8, childCareMonth.getDayoffHoursExcludingWeekends())
        assertEquals(
            12,
            childCareMonth.getDayoffHoursExcludingWeekends(WorkScheduleProfile.sixDaySevenFive()),
        )
    }
}
