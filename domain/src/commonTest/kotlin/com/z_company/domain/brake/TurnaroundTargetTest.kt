package com.z_company.domain.brake

import kotlin.test.Test
import kotlin.test.assertEquals

class TurnaroundTargetTest {
    private fun e(id: String, arr: Long?, dep: Long?, d: Double? = null) =
        TurnaroundEntry(id = id, stationName = id, stopClass = StopClass.OVER_5, arrivalMs = arr, departureMs = dep, densitySec = d)

    @Test
    fun explicitTargetWins() {
        val l = listOf(e("a", 0, 10), e("b", 100, 200))
        assertEquals(TurnaroundPick.Existing("a"), TurnaroundTarget.pick(l, "a", 150, true))
    }

    @Test
    fun noTarget_nearestWithoutDensity() {
        val l = listOf(e("a", 0, 10), e("b", 100, 200), e("c", 300, 400))
        assertEquals(TurnaroundPick.Existing("b"), TurnaroundTarget.pick(l, null, 150, true))
        // после последней стоянки - последняя прошедшая
        assertEquals(TurnaroundPick.Existing("c"), TurnaroundTarget.pick(l, null, 900, true))
    }

    @Test
    fun existingDensityIsNotOverwritten() {
        val l = listOf(e("a", 0, 10), e("b", 100, 200, d = 40.0))
        assertEquals(TurnaroundPick.Existing("a"), TurnaroundTarget.pick(l, "b", 150, true))
        assertEquals(TurnaroundPick.Existing("a"), TurnaroundTarget.pick(l, null, 150, true))
    }

    @Test
    fun noCandidates_createNew() {
        assertEquals(TurnaroundPick.CreateNew, TurnaroundTarget.pick(emptyList(), null, 1, true))
        assertEquals(TurnaroundPick.CreateNew, TurnaroundTarget.pick(listOf(e("a", 0, 10, 30.0)), null, 1, true))
    }

    @Test
    fun passenger_notApplicable() {
        assertEquals(TurnaroundPick.NotApplicable, TurnaroundTarget.pick(listOf(e("a", 0, 10)), "a", 1, false))
    }
}
