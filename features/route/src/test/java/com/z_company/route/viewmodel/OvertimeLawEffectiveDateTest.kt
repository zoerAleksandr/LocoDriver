package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class OvertimeLawEffectiveDateTest {
    @Test
    fun law144DoesNotApplyToAugust2026() {
        assertEquals(false, isFederalLaw144Effective(year = 2026, month = 7))
    }

    @Test
    fun law144AppliesFromSeptember2026() {
        assertEquals(true, isFederalLaw144Effective(year = 2026, month = 8))
    }

    @Test
    fun expandedOvertimeBaseStartsInSeptember2024() {
        assertEquals(false, isExpandedOvertimeBaseEffective(year = 2024, month = 7))
        assertEquals(true, isExpandedOvertimeBaseEffective(year = 2024, month = 8))
        assertEquals(true, isExpandedOvertimeBaseEffective(year = 2026, month = 8))
    }

    @Test
    fun firstTwoHoursAreLimitedByShiftCount() {
        val result = calculateHalfRateOvertime(
            overtime = 70 * HOUR,
            shiftCount = 20,
            year = 2026,
            month = 8,
        )

        assertEquals(40 * HOUR, result)
    }

    @Test
    fun september2026UsesRemainingPartOfAnnual120Hours() {
        val result = calculateHalfRateOvertime(
            overtime = 30 * HOUR,
            shiftCount = 20,
            year = 2026,
            month = 8,
            annualOvertimeBeforePeriod = 110 * HOUR,
        )

        assertEquals(10 * HOUR, result)
    }

    @Test
    fun afterAnnual120HoursAllCurrentOvertimeIsDoubleRate() {
        val result = calculateHalfRateOvertime(
            overtime = 30 * HOUR,
            shiftCount = 20,
            year = 2026,
            month = 8,
            annualOvertimeBeforePeriod = 120 * HOUR,
        )

        assertEquals(0L, result)
    }

    private companion object { const val HOUR = 3_600_000L }
}
