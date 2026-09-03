package com.z_company.domain.entities.setting

import kotlin.test.Test
import kotlin.test.assertEquals

class SalarySettingDefaultsTest {
    @Test
    fun harmfulnessDefaultsToFourPercent() {
        assertEquals(4.0, SalarySetting().harmfulnessPercent)
    }
}
