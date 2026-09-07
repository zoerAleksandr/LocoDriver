package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CalculationEnergyTotalTest {
    @Test
    fun includesEnabledHeatingAndAuxiliaryConsumption() {
        val result = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 100.0,
            heatingAccepted = 10.0,
            heatingDelivery = 15.0,
            auxiliaryAccepted = 20.0,
            auxiliaryDelivery = 27.0,
            considerHeating = true,
            considerAuxiliary = true,
        )

        assertEquals(112.0, result)
    }

    @Test
    fun excludesDisabledCountersAndIgnoresIncompletePair() {
        val result = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 100.0,
            heatingAccepted = 10.0,
            heatingDelivery = 15.0,
            auxiliaryAccepted = 20.0,
            auxiliaryDelivery = null,
            considerHeating = false,
            considerAuxiliary = true,
        )

        assertEquals(100.0, result)
    }
}
