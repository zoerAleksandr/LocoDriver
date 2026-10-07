package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabelPlacerTest {
    private fun item(x: Double, y: Double, prio: Int = 1, w: Double = 60.0) = MapLabelItem(x, y, w, 14.0, 5.0, prio)

    @Test
    fun farApartAllShown() {
        assertEquals(setOf(0, 1), LabelPlacer.visible(listOf(item(0.0, 0.0), item(200.0, 0.0))))
    }

    @Test
    fun overlappingLabelsKeepHigherPriority() {
        val v = LabelPlacer.visible(listOf(item(0.0, 0.0, prio = 1), item(30.0, 5.0, prio = 0)))
        assertTrue(1 in v)
        assertTrue(0 !in v)
    }

    @Test
    fun labelOverDotIsHidden() {
        // вторая точка прямо под подписью первой
        val v = LabelPlacer.visible(listOf(item(0.0, 0.0), item(5.0, 14.0, w = 0.0)))
        assertTrue(0 !in v)
    }

    @Test
    fun zoomInRevealsLabel() {
        val near = LabelPlacer.visible(listOf(item(0.0, 0.0), item(20.0, 0.0)))
        val far = LabelPlacer.visible(listOf(item(0.0, 0.0), item(300.0, 0.0)))
        assertEquals(1, near.size.coerceAtMost(1))
        assertEquals(2, far.size)
    }
}
