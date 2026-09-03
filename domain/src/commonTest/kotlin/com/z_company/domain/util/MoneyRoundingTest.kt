package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyRoundingTest {
    @Test
    fun monetaryRowsRoundToKopecksBeforeSummation() {
        val rows = listOf(10.004, 10.005, 10.006)
        assertEquals(listOf(10.0, 10.01, 10.01), rows.map { it.roundMoneyToCents() })
        assertEquals(30.02, rows.sumOf { it.roundMoneyToCents() }, 0.0000001)
    }

    @Test
    fun negativeAndNonFiniteValuesAreSafe() {
        assertEquals(-10.01, (-10.006).roundMoneyToCents())
        assertEquals(0.0, Double.NaN.roundMoneyToCents())
        assertEquals(0.0, Double.POSITIVE_INFINITY.roundMoneyToCents())
    }
}
