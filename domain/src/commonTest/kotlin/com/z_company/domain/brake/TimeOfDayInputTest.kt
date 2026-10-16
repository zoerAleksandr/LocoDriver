package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeOfDayInputTest {
    @Test fun filterFormatsFourDigits() {
        assertEquals("12:45", TimeOfDayInput.filter("1245"))
        assertEquals("12", TimeOfDayInput.filter("12"))
        assertEquals("12:4", TimeOfDayInput.filter("12:4"))
        assertEquals("9:05", TimeOfDayInput.filter("9a:05"))
        assertEquals("1:2", TimeOfDayInput.filter(":1:2:"))
    }

    @Test fun normalizeRange() {
        assertEquals("12:45", TimeOfDayInput.normalize("1245"))
        assertEquals("09:05", TimeOfDayInput.normalize("9:05"))
        assertEquals("09:05", TimeOfDayInput.normalize("905"))
        assertEquals("23:59", TimeOfDayInput.normalize("23:59"))
        assertEquals("00:00", TimeOfDayInput.normalize("0000"))
        assertNull(TimeOfDayInput.normalize("24:00"))
        assertNull(TimeOfDayInput.normalize("12:60"))
        assertNull(TimeOfDayInput.normalize("12:4"))
        assertNull(TimeOfDayInput.normalize(""))
    }

    @Test fun displayOldValues() {
        assertEquals("12:45", TimeOfDayInput.display("1245"))
        assertEquals("12:45", TimeOfDayInput.display("12:45"))
        assertEquals("xyz", TimeOfDayInput.display("xyz"))
    }
}
