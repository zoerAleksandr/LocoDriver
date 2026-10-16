package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

class BrakeAutoblockTest {
    private val freight = BrakeNorms.categories.first { it.isLoadedFreight }

    private fun speed(grade: GradeClass, ab: Boolean, cat: BrakeCategory = freight): Int? {
        val r = BrakeCertificateCalculator.calculate(
            BrakeCertificateInput(cat, 1000.0, listOf(AxleRow(5.0, 100)), grade = grade, autoblock3 = ab),
        )
        return (r.speed as? SpeedLimit.Allowed)?.kmh
    }

    @Test fun off_giveMainSpeed80() = assertEquals(80, speed(GradeClass.UP_TO_006, false))

    @Test fun on_flat_gives90() = assertEquals(90, speed(GradeClass.UP_TO_006, true))

    @Test fun on_upTo010_gives90() = assertEquals(90, speed(GradeClass.OVER_006_TO_010, true))

    @Test fun on_steeper_than_010_doesNotChange() {
        assertEquals(speed(GradeClass.OVER_010_TO_015, false), speed(GradeClass.OVER_010_TO_015, true))
    }

    @Test fun noBracketValue_doesNotChange() {
        val cat = BrakeNorms.categories.first { it.vFlatAutoblock == null && it.vFlat != null }
        assertEquals(speed(GradeClass.UP_TO_006, false, cat), speed(GradeClass.UP_TO_006, true, cat))
    }
}
