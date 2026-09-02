package com.z_company.route.viewmodel

import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class SalarySettingListValidationTest {

    @Test
    fun `valid lists are copied for persistence`() {
        val heavy = SurchargeHeavyTrains(weight = "6 000,5", percentSurcharge = "5")
        val long = SurchargeLongTrains(conditionalLength = "71", percentSurcharge = "7,5")
        val extended = SurchargeExtendedServicePhase(distance = "250", percentSurcharge = "10")

        val validatedHeavy = validatedHeavyTrainSettings(listOf(heavy))!!
        val validatedLong = validatedLongTrainSettings(listOf(long))!!
        val validatedExtended = validatedExtendedServiceSettings(listOf(extended))!!

        assertEquals(listOf(heavy), validatedHeavy)
        assertEquals(listOf(long), validatedLong)
        assertEquals(listOf(extended), validatedExtended)
        assertNotSame(heavy, validatedHeavy.single())
        assertNotSame(long, validatedLong.single())
        assertNotSame(extended, validatedExtended.single())
    }

    @Test
    fun `one invalid row rejects entire edited list`() {
        assertNull(validatedHeavyTrainSettings(listOf(
            SurchargeHeavyTrains(weight = "6000", percentSurcharge = "5"),
            SurchargeHeavyTrains(weight = "weight", percentSurcharge = "7"),
        )))
        assertNull(validatedLongTrainSettings(listOf(
            SurchargeLongTrains(conditionalLength = "0", percentSurcharge = "7"),
        )))
        assertNull(validatedExtendedServiceSettings(listOf(
            SurchargeExtendedServicePhase(distance = "250", percentSurcharge = "NaN"),
        )))
    }

    @Test
    fun `negative infinity and blank values cannot be persisted`() {
        assertNull(validatedHeavyTrainSettings(listOf(
            SurchargeHeavyTrains(weight = "-1", percentSurcharge = "5"),
        )))
        assertNull(validatedLongTrainSettings(listOf(
            SurchargeLongTrains(conditionalLength = "71", percentSurcharge = "Infinity"),
        )))
        assertNull(validatedExtendedServiceSettings(listOf(
            SurchargeExtendedServicePhase(distance = "", percentSurcharge = "10"),
        )))
    }

    @Test
    fun `empty list remains valid to allow deletion of all tiers`() {
        assertEquals(emptyList(), validatedHeavyTrainSettings(emptyList()))
        assertEquals(emptyList(), validatedLongTrainSettings(emptyList()))
        assertEquals(emptyList(), validatedExtendedServiceSettings(emptyList()))
    }
}
