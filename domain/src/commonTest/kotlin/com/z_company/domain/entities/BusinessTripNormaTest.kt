package com.z_company.domain.entities

import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import kotlin.test.Test
import kotlin.test.assertEquals

class BusinessTripNormaTest {
    @Test
    fun businessTripDayDoesNotReduceMonthlyNorma() {
        val month = MonthOfYear(
            year = 2026,
            month = 7,
            days = listOf(
                Day(1, TagForDay.WORKING_DAY, isReleaseDay = true, releaseType = ReleaseType.BusinessTrip),
                Day(2, TagForDay.SHORTENED_DAY, isReleaseDay = true, releaseType = ReleaseType.BusinessTrip),
                Day(3, TagForDay.WORKING_DAY, isReleaseDay = true, releaseType = ReleaseType.Vacation),
            ),
        )

        assertEquals(15, month.getPersonalNormaHours())
    }
}
