package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecimalInputTest {
    @Test
    fun parse() {
        assertEquals(8.5, "8,5".toFiniteDoubleOrNull())
        assertEquals(8.5, "8.5".toFiniteDoubleOrNull())
        assertEquals(8.0, "8,".toFiniteDoubleOrNull())
        assertEquals(0.5, ",5".toFiniteDoubleOrNull())
        assertNull("8,5,5".toFiniteDoubleOrNull())
        assertNull("abc".toFiniteDoubleOrNull())
        assertNull("".toFiniteDoubleOrNull())
        assertEquals(8.5, " 8 , 5 ".toFiniteDoubleOrNull())
        assertEquals(3040.5, "3040,5".toFiniteDoubleOrNull())
    }

    @Test
    fun sanitizeKeepsPartialInput() {
        assertEquals("8,", "8,".sanitizeDecimalInput())
        assertEquals("8,", "8.".sanitizeDecimalInput())
        assertEquals(",5", ",5".sanitizeDecimalInput())
        assertEquals("8,5", "8.5".sanitizeDecimalInput())
        assertEquals("8,55", "8,5,5".sanitizeDecimalInput())
        assertEquals("85", "a8b5c".sanitizeDecimalInput())
        assertEquals("8,5", " 8 , 5 ".sanitizeDecimalInput())
        assertEquals("", "abc".sanitizeDecimalInput())
        assertEquals("1234567890", "12345678901234".sanitizeDecimalInput())
    }

    @Test
    fun sanitizedValueNeverBreaksCalculation() {
        for (raw in listOf("8,", ",5", "8,5,5", "abc", "", "..", ",,", "8..5")) {
            val s = raw.sanitizeDecimalInput()
            // после очистки разбор либо успешен, либо поле пустое (расчёт игнорирует)
            if (s.isNotEmpty() && s != ",") assertEquals(true, s.toFiniteDoubleOrNull() != null, raw)
        }
    }

    @Test
    fun integerInput() {
        assertEquals("128", "1a2,8".sanitizeIntegerInput())
        assertEquals("", "abc".sanitizeIntegerInput())
    }
}
