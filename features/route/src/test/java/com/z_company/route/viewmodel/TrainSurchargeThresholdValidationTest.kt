package com.z_company.route.viewmodel

import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import kotlin.test.Test
import kotlin.test.assertEquals

class TrainSurchargeThresholdValidationTest {
    @Test
    fun heavyThresholdsAreSortedNumericallyAndInvalidValuesAreExcluded() {
        val result = validHeavyTrainSurcharges(
            listOf(
                SurchargeHeavyTrains(weight = "10000", percentSurcharge = "10"),
                SurchargeHeavyTrains(weight = "6000", percentSurcharge = "5"),
                SurchargeHeavyTrains(weight = "invalid"),
                SurchargeHeavyTrains(weight = "0"),
                SurchargeHeavyTrains(weight = "-1"),
            )
        )

        assertEquals(listOf("6000", "10000"), result.map { it.weight })
    }

    @Test
    fun longThresholdsAreSortedNumericallyAndWhitespaceIsAccepted() {
        val result = validLongTrainSurcharges(
            listOf(
                SurchargeLongTrains(conditionalLength = " 100 "),
                SurchargeLongTrains(conditionalLength = "80"),
                SurchargeLongTrains(conditionalLength = "NaN"),
                SurchargeLongTrains(conditionalLength = "0"),
            )
        )

        assertEquals(listOf("80", " 100 "), result.map { it.conditionalLength })
    }
}
