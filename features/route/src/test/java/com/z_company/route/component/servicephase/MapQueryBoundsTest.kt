package com.z_company.route.component.servicephase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MapQueryBoundsTest {
    @Test
    fun queryBoundsKeepEveryCandidateMarginAndClipToViewport() {
        val bounds = mapQueryBoundsAround(
            points = listOf(MapScreenPoint(10f, 20f), MapScreenPoint(80f, 60f)),
            paddingPx = 15f,
            viewportWidth = 100f,
            viewportHeight = 100f,
        )

        assertEquals(MapQueryBounds(0f, 5f, 95f, 75f), bounds)
    }

    @Test
    fun queryBoundsReturnNullWhenAllCandidatesAreOutsideViewport() {
        val bounds = mapQueryBoundsAround(
            points = listOf(MapScreenPoint(-80f, 30f)),
            paddingPx = 10f,
            viewportWidth = 100f,
            viewportHeight = 100f,
        )

        assertNull(bounds)
    }
}
