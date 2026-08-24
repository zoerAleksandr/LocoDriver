package com.z_company.domain.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class WorkScheduleProfileTest {
    private val profile = WorkScheduleProfile.sixDaySevenFive()

    @Test
    fun sixDayProfileUsesSevenHoursOnWeekdayAndFiveOnSaturday() {
        assertEquals(7, profile.effectiveHours(LocalDate(2026, 8, 24), TagForDay.WORKING_DAY))
        assertEquals(5, profile.effectiveHours(LocalDate(2026, 8, 29), TagForDay.NON_WORKING_DAY))
        assertEquals(0, profile.effectiveHours(LocalDate(2026, 8, 30), TagForDay.NON_WORKING_DAY))
    }

    @Test
    fun holidayAlwaysHasZeroHours() {
        assertEquals(0, profile.effectiveHours(LocalDate(2026, 8, 29), TagForDay.HOLIDAY))
    }

    @Test
    fun shortenedDayIsOneHourShorterThanConfiguredDay() {
        assertEquals(6, profile.effectiveHours(LocalDate(2026, 8, 24), TagForDay.SHORTENED_DAY))
        assertEquals(4, profile.effectiveHours(LocalDate(2026, 8, 29), TagForDay.SHORTENED_DAY))
    }

    @Test
    fun customHoursAreClampedToValidRange() {
        val low = profile.withHours(DayOfWeek.MONDAY, -1)
        val high = profile.withHours(DayOfWeek.SUNDAY, 30)

        assertEquals(0, low.mondayHours)
        assertEquals(24, high.sundayHours)
        assertEquals(WorkScheduleMode.CUSTOM, low.mode)
    }
}
