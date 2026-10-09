package com.z_company.route.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StationSuggestionMatchTest {
    private val list = listOf("Лужская", "Усть-Луга", "Бернгардовка")

    @Test fun fullMatchIgnoringCaseAndSpaces() {
        assertTrue(isExactStationMatch("  лужская ", list))
        assertTrue(isExactStationMatch("УСТЬ-ЛУГА", list))
    }

    @Test fun yoEquivalentToYe() {
        assertTrue(isExactStationMatch("Елочная", listOf("Ёлочная")))
    }

    @Test fun partialOrEmptyDoesNotMatch() {
        assertFalse(isExactStationMatch("Луж", list))
        assertFalse(isExactStationMatch("", list))
        assertFalse(isExactStationMatch("   ", list))
    }
}
