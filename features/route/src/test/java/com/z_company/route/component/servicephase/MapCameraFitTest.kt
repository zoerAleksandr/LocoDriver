package com.z_company.route.component.servicephase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MapCameraFitTest {
    @Test
    fun emptyIsNone() = assertEquals(CameraFitPlan.None, MapCameraFit.plan(emptyList()))

    @Test
    fun singlePointIsPoint() {
        val p = assertIs<CameraFitPlan.Point>(MapCameraFit.plan(listOf(55.0 to 37.0)))
        assertEquals(55.0, p.lat)
        assertEquals(37.0, p.lon)
    }

    @Test
    fun identicalPointsArePoint() {
        assertIs<CameraFitPlan.Point>(MapCameraFit.plan(listOf(55.0 to 37.0, 55.0 to 37.0, 55.0 to 37.0)))
    }

    @Test
    fun twoDistinctPointsAreBounds() {
        val b = assertIs<CameraFitPlan.Bounds>(MapCameraFit.plan(listOf(55.0 to 37.0, 55.5 to 37.8)))
        assertEquals(55.0, b.south)
        assertEquals(55.5, b.north)
        assertEquals(37.0, b.west)
        assertEquals(37.8, b.east)
    }

    @Test
    fun spreadOnOneAxisIsBounds() {
        assertIs<CameraFitPlan.Bounds>(MapCameraFit.plan(listOf(55.0 to 37.0, 55.0 to 37.5)))
    }
}
