package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteVariantLabelsTest {
    private fun via(name: String, km: Double, osm: Long? = null, withPoint: Boolean = true) =
        RouteVia(name, km, osm, null, if (withPoint) 59.0 else null, if (withPoint) 30.0 else null)

    private fun variant(id: String, vararg v: RouteVia) =
        RouteVariant(id, 100.0, listOf(0.0 to 0.0, 1.0 to 1.0), v.toList())

    @Test
    fun labelIsFirstStationThatOtherVariantsLack() {
        val a = variant("r1", via("Кикерино", 20.0, 1), via("Молосковицы", 40.0, 2))
        val b = variant("r2", via("Кикерино", 20.0, 1), via("Гатчина", 60.0, 3))
        assertEquals(listOf("Молосковицы", "Гатчина"), variantViaLabels(listOf(a, b)))
    }

    @Test
    fun withoutDifferencesFirstStationIsUsedAndEmptyIsNull() {
        val a = variant("r1", via("Кикерино", 20.0, 1))
        val b = variant("r2", via("Кикерино", 20.0, 1))
        val c = variant("r3")
        assertEquals(listOf("Кикерино", "Кикерино", null), variantViaLabels(listOf(a, b, c)))
    }

    @Test
    fun stationsWithoutOsmIdAreMatchedByNameIgnoringCase() {
        val a = variant("r1", via("Гатчина", 10.0), via("Луга", 90.0))
        val b = variant("r2", via("ГАТЧИНА ", 10.0))
        assertEquals("Луга", variantViaLabels(listOf(a, b))[0])
    }

    @Test
    fun addableStationsNeedCoordinatesAndAreOrderedByKm() {
        val v = variant("r1", via("B", 30.0), via("A", 10.0), via("Без точки", 20.0, withPoint = false))
        assertEquals(listOf("A", "B"), v.addableStations().map { it.name })
    }
}
