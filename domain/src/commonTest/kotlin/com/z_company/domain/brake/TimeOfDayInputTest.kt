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

    /** Набор посимвольно, как с клавиатуры: каждый шаг - mask(предыдущее, предыдущее + символ). */
    private fun type(vararg chars: Char): String {
        var cur = ""
        for (c in chars) cur = TimeOfDayInput.mask(cur, cur + c)
        return cur
    }

    @Test fun maskTypingCharByChar() {
        assertEquals("12:45", type('1', '2', '4', '5'))
        assertEquals("", type('a', 'я', ':', '-')) // буквы и символы не проходят
        assertEquals("", type('9')) // первая цифра часов 0-2
        assertEquals("", type('9', '9'))
        assertEquals("2", type('2', '5')) // при первой 2 вторая не больше 3
        assertEquals("23:", type('2', '3'))
        assertEquals("12:", type('1', '2', '6', '7')) // десятки минут 0-5 (6 отброшена; 7 после неё тоже не принимается за десятки)
        assertEquals("12:59", type('1', '2', '5', '9'))
        assertEquals("12:59", type('1', '2', '5', '9', '1')) // не более 4 цифр
    }

    @Test fun maskPasteAndDelete() {
        assertEquals("", TimeOfDayInput.mask("", "99"))
        assertEquals("12:", TimeOfDayInput.mask("", "1260"))
        assertEquals("23:59", TimeOfDayInput.mask("", "2359"))
        assertEquals("2", TimeOfDayInput.mask("", "25"))
        // стирание: «12:» -> «12», «12:4» -> «12:», двоеточие обратно не навязывается при стирании
        assertEquals("12", TimeOfDayInput.mask("12:", "12"))
        assertEquals("12:", TimeOfDayInput.mask("12:4", "12:"))
        assertEquals("1", TimeOfDayInput.mask("12", "1"))
    }

    @Test fun committable() {
        assertEquals("", TimeOfDayInput.committable(""))
        assertEquals("12:45", TimeOfDayInput.committable("12:45"))
        assertNull(TimeOfDayInput.committable("12:4"))
        assertNull(TimeOfDayInput.committable("12:"))
    }
}
