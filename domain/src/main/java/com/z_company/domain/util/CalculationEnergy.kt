package com.z_company.domain.util

import kotlin.math.pow
import kotlin.math.roundToInt

object CalculationEnergy {
    private fun differenceBetweenDouble(value1: Double?, value2: Double?): Double? {
        val countAfterPoint1: Int = value1?.countCharsAfterDecimalPoint() ?: 0
        val countAfterPoint2: Int = value2?.countCharsAfterDecimalPoint() ?: 0
        val maxCount = if (countAfterPoint1 > countAfterPoint2) {
            countAfterPoint1
        } else {
            countAfterPoint2
        }
        val result = value2 - value1
        return result?.let {
            rounding(it, maxCount)
        }
    }

    private fun reverseDifferenceBetweenDouble(value1: Double?, value2: Double?): Double? {
        val countAfterPoint1: Int = value1?.countCharsAfterDecimalPoint() ?: 0
        val countAfterPoint2: Int = value2?.countCharsAfterDecimalPoint() ?: 0
        val maxCount = if (countAfterPoint1 > countAfterPoint2) {
            countAfterPoint1
        } else {
            countAfterPoint2
        }
        val result = value1 - value2
        return result?.let {
            rounding(it, maxCount)
        }
    }

    fun rounding(value: Double?, count: Int): Double? {
        return value?.let {
            (it * 10.0.pow(count)).roundToInt() / 10.0.pow(count)
        }
    }

    fun getTotalFuelConsumption(
        accepted: Double?,
        delivery: Double?,
        refuel: Double?
    ): Double? {
        return reverseDifferenceBetweenDouble(accepted, delivery).plusNullableValue(refuel)
    }

    fun getTotalFuelInKiloConsumption(
        consumption: Double?,
        coefficient: Double?
    ): Double? {
        return consumption * coefficient
    }

    fun getTotalEnergyConsumption(
        accepted: Int?,
        delivery: Int?
    ): Int? {
        return delivery - accepted
    }
}