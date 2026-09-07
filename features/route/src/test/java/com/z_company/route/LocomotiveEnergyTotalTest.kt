package com.z_company.route

import com.z_company.domain.util.CalculationEnergy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocomotiveEnergyTotalTest {
    @Test
    fun `enabled counters are added to section consumption`() {
        val total = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 150.0,
            heatingAccepted = 10.0,
            heatingDelivery = 14.5,
            auxiliaryAccepted = 20.0,
            auxiliaryDelivery = 26.5,
            considerHeating = true,
            considerAuxiliary = true,
        )

        assertEquals(161.0, total)
    }

    @Test
    fun `each counter can be excluded independently`() {
        val heatingExcluded = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 100.0,
            heatingAccepted = 10.0,
            heatingDelivery = 15.0,
            auxiliaryAccepted = 20.0,
            auxiliaryDelivery = 27.0,
            considerHeating = false,
            considerAuxiliary = true,
        )
        val auxiliaryExcluded = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 100.0,
            heatingAccepted = 10.0,
            heatingDelivery = 15.0,
            auxiliaryAccepted = 20.0,
            auxiliaryDelivery = 27.0,
            considerHeating = true,
            considerAuxiliary = false,
        )

        assertEquals(107.0, heatingExcluded)
        assertEquals(105.0, auxiliaryExcluded)
    }

    @Test
    fun `incomplete pairs do not affect total`() {
        val total = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = 100.0,
            heatingAccepted = 10.0,
            heatingDelivery = null,
            auxiliaryAccepted = null,
            auxiliaryDelivery = 27.0,
            considerHeating = true,
            considerAuxiliary = true,
        )

        assertEquals(100.0, total)
    }

    @Test
    fun `empty input has no artificial zero total`() {
        val total = CalculationEnergy.getTotalEnergyConsumption(
            sectionConsumption = null,
            heatingAccepted = null,
            heatingDelivery = null,
            auxiliaryAccepted = null,
            auxiliaryDelivery = null,
            considerHeating = true,
            considerAuxiliary = true,
        )

        assertNull(total)
    }
}
