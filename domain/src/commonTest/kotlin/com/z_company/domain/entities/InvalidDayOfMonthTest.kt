package com.z_company.domain.entities

import com.z_company.domain.entities.UtilForMonthOfYear.getDayoffHoursIncludingWeekends
import com.z_company.domain.entities.UtilForMonthOfYear.getPersonalNormaHours
import com.z_company.domain.util.daysInMonth
import com.z_company.domain.util.validFor
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Испорченные данные календаря («31 сентября» в списке дней) не должны ронять
 * расчёт нормы: такой день просто не учитывается.
 */
class InvalidDayOfMonthTest {

    // сентябрь = month 8 (0-based), в нём 30 дней
    private val september = MonthOfYear(
        year = 2026,
        month = 8,
        days = listOf(
            Day(29, TagForDay.WORKING_DAY),
            Day(30, TagForDay.WORKING_DAY),
            Day(31, TagForDay.WORKING_DAY),
        ),
    )

    @Test
    fun nonExistentDayIsSkippedInPersonalNorma() {
        assertEquals(16, september.getPersonalNormaHours())
    }

    @Test
    fun nonExistentDayIsSkippedInDayoffHours() {
        val month = september.copy(
            days = september.days.map {
                it.copy(isReleaseDay = true, releaseType = ReleaseType.Vacation)
            }
        )
        assertEquals(16, month.getDayoffHoursIncludingWeekends())
    }

    @Test
    fun validForDropsNonExistentAndDuplicatedDays() {
        assertEquals(listOf(29, 30), september.days.validFor(2026, 8).map { it.dayOfMonth })
        assertEquals(30, daysInMonth(2026, 8))
        assertEquals(29, daysInMonth(2024, 1))
        assertEquals(0, daysInMonth(2026, 12))
    }
}
