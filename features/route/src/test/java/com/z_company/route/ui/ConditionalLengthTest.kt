package com.z_company.route.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionalLengthTest {

    @Test
    fun freightConditionalUnitEqualsFourteenMeters() {
        assertEquals(14, conditionalLengthToMeters("1", "2503", 24.5))
        assertEquals(246, conditionalLengthToMeters("17.6", "2503", 24.5))
    }

    @Test
    fun passengerValueUsesConfiguredWagonLength() {
        assertEquals(245, conditionalLengthToMeters("10", "100", 24.5))
        assertEquals(250, conditionalLengthToMeters("10", "100", 25.0))
    }

    @Test
    fun emptyOrZeroLengthHasNoHint() {
        assertNull(conditionalLengthToMeters(null, "100", 24.5))
        assertNull(conditionalLengthToMeters("", "100", 24.5))
        assertNull(conditionalLengthToMeters("0", "100", 24.5))
    }
}
