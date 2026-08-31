package com.z_company.route.viewmodel

import com.z_company.domain.entities.setting.SurchargeHeavyTrains
import com.z_company.domain.entities.setting.SurchargeLongTrains
import com.z_company.domain.entities.setting.SurchargeExtendedServicePhase
import kotlin.test.Test
import kotlin.test.assertEquals

class TrainSurchargeThresholdValidationTest {
    @Test
    fun invalidAndNegativePercentsAreExcludedFromAllTierLists() {
        assertEquals(
            emptyList(),
            validHeavyTrainSurcharges(listOf(
                SurchargeHeavyTrains(weight = "100", percentSurcharge = "-1"),
                SurchargeHeavyTrains(weight = "200", percentSurcharge = "NaN"),
            )),
        )
        assertEquals(
            emptyList(),
            validLongTrainSurcharges(listOf(
                SurchargeLongTrains(conditionalLength = "50", percentSurcharge = "Infinity"),
            )),
        )
        assertEquals(
            emptyList(),
            validExtendedServicePhaseSurcharges(listOf(
                SurchargeExtendedServicePhase(distance = "250", percentSurcharge = "text"),
            )),
        )
    }

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

    @Test
    fun formattedIntegerThresholdsAreAcceptedButFractionalAndNonFiniteAreExcluded() {
        val result = validHeavyTrainSurcharges(
            listOf(
                SurchargeHeavyTrains(weight = "6 000", percentSurcharge = "5"),
                SurchargeHeavyTrains(weight = "10000,0", percentSurcharge = "10"),
                SurchargeHeavyTrains(weight = "6000,5"),
                SurchargeHeavyTrains(weight = "NaN"),
                SurchargeHeavyTrains(weight = "Infinity"),
            ),
        )

        assertEquals(listOf("6 000", "10000,0"), result.map { it.weight })
    }

    @Test
    fun duplicateHeavyThresholdUsesLastConfiguredRangeOnce() {
        val result = validHeavyTrainSurcharges(
            listOf(
                SurchargeHeavyTrains(weight = "6000", percentSurcharge = "5"),
                SurchargeHeavyTrains(weight = "10000", percentSurcharge = "10"),
                SurchargeHeavyTrains(weight = "6000", percentSurcharge = "7"),
            ),
        )

        assertEquals(listOf("6000", "10000"), result.map { it.weight })
        assertEquals(listOf("7", "10"), result.map { it.percentSurcharge })
    }

    @Test
    fun duplicateLongThresholdUsesLastConfiguredRangeOnce() {
        val result = validLongTrainSurcharges(
            listOf(
                SurchargeLongTrains(conditionalLength = "80", percentSurcharge = "5"),
                SurchargeLongTrains(conditionalLength = "80", percentSurcharge = "8"),
            ),
        )

        assertEquals(1, result.size)
        assertEquals("8", result.single().percentSurcharge)
    }

    @Test
    fun servicePhaseThresholdsAreNumericAndDuplicateUsesLastConfiguredRange() {
        val result = validExtendedServicePhaseSurcharges(
            listOf(
                SurchargeExtendedServicePhase(distance = "1 000", percentSurcharge = "10"),
                SurchargeExtendedServicePhase(distance = "250", percentSurcharge = "5"),
                SurchargeExtendedServicePhase(distance = "250,0", percentSurcharge = "7"),
                SurchargeExtendedServicePhase(distance = "250,5", percentSurcharge = "99"),
                SurchargeExtendedServicePhase(distance = "NaN", percentSurcharge = "99"),
                SurchargeExtendedServicePhase(distance = "-1", percentSurcharge = "99"),
            ),
        )

        assertEquals(listOf("250,0", "1 000"), result.map { it.distance })
        assertEquals(listOf("7", "10"), result.map { it.percentSurcharge })
    }
}
