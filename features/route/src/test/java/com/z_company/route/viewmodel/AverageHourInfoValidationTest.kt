package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AverageHourInfoValidationTest {
    @Test
    fun missingOrInvalidAverageHourShowsInfoWhenUnderworkExists() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { value ->
            assertTrue(shouldShowAverageHourInfo(3_600_000L, value, alreadyDismissed = false))
        }
    }

    @Test
    fun infoIsHiddenWithoutUnderworkAfterValidSettingOrAfterDismissal() {
        assertFalse(shouldShowAverageHourInfo(0L, 0.0, alreadyDismissed = false))
        assertFalse(shouldShowAverageHourInfo(3_600_000L, 200.0, alreadyDismissed = false))
        assertFalse(shouldShowAverageHourInfo(3_600_000L, 0.0, alreadyDismissed = true))
    }
}
