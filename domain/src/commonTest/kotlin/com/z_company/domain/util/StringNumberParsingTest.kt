package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringNumberParsingTest {
    @Test
    fun finiteDoubleAcceptsSpacesAndDecimalComma() {
        assertEquals(6_000.5, "6 000,5".toFiniteDoubleOrNull())
        assertEquals(6_000.5, " 6\u00A0000,5 ".toFiniteDoubleOrNull())
    }

    @Test
    fun finiteDoubleRejectsNanInfinityAndMalformedValues() {
        listOf("NaN", "Infinity", "-Infinity", "text", "1,2.3").forEach { value ->
            assertNull(value.toFiniteDoubleOrNull(), value)
            assertEquals(0.0, value.toDoubleOrZero(), value)
        }
    }

    @Test
    fun salaryValueRejectsNegativeAndNonFiniteButTreatsBlankAsZero() {
        assertEquals(0.0, "".toNonNegativeFiniteDoubleOrNull())
        assertEquals(12.5, "12,5".toNonNegativeFiniteDoubleOrNull())
        listOf("-1", "NaN", "Infinity", "-Infinity", "text").forEach { value ->
            assertNull(value.toNonNegativeFiniteDoubleOrNull(), value)
        }
    }

    @Test
    fun exactIntegerAcceptsServerDecimalFormButRejectsFractionAndOverflow() {
        assertEquals(6_000, "6 000,0".toExactIntOrNull())
        assertEquals(57, "57.0".toExactIntOrNull())
        assertNull("6000,5".toExactIntOrNull())
        assertNull("999999999999999999999".toExactIntOrNull())
    }

    @Test
    fun legacyIntConversionStaysPredictableForFractionalServerValues() {
        assertEquals(57, "57,5".toIntOrZero())
        assertEquals(6_000, "6 000".toIntOrZero())
        assertEquals(0, "NaN".toIntOrZero())
        assertEquals(0, "Infinity".toIntOrZero())
    }
}
