package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

class DateInputTest {
    private fun type(vararg s: String): String { var cur = ""; for (x in s) cur = DateInput.mask(cur, x); return cur }

    @Test fun typingForward_addsDots() {
        assertEquals("2", type("2"))
        assertEquals("22.", type("2", "22"))
        assertEquals("22.1", type("2", "22", "22.1"))
        assertEquals("22.10.", type("2", "22", "22.1", "22.10"))
        assertEquals("22.10.2026", type("2", "22", "22.1", "22.10", "22.10.2", "22.10.20", "22.10.202", "22.10.2026"))
    }

    @Test fun lettersAndSymbolsAreDropped() {
        assertEquals("", DateInput.mask("", "э"))
        assertEquals("22.10.2026", DateInput.mask("22.10.202", "22.10.202a6"))
        assertEquals("1", DateInput.mask("", "1-"))
    }

    @Test fun impossibleDigitsAreRejected() {
        assertEquals("", DateInput.mask("", "4"))
        assertEquals("3", DateInput.mask("3", "39"))
        assertEquals("31.1", DateInput.mask("31.", "31.1"))
        assertEquals("31.1", DateInput.mask("31.1", "31.19"))
        assertEquals("12.0", DateInput.mask("12.", "12.0"))
    }

    @Test fun deleting_doesNotRestoreDot() {
        assertEquals("22", DateInput.mask("22.", "22"))
        assertEquals("22.10", DateInput.mask("22.10.", "22.10"))
        assertEquals("22.1", DateInput.mask("22.10", "22.1"))
    }

    @Test fun pastedFullDate_isFormatted() {
        assertEquals("22.10.2026", DateInput.mask("", "22102026"))
        assertEquals("22.10.2026", DateInput.mask("", "22.10.2026"))
        assertEquals("22.10.2026", DateInput.mask("22.10.2026", "22.10.20261"))
    }
}
