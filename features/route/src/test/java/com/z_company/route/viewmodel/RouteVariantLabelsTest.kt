package com.z_company.route.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteVariantLabelsTest {
    private fun via(name: String, km: Double, osm: Long? = null, point: Pair<Double, Double>? = 59.0 to 30.0) =
        RouteVia(name, km, osm, null, point?.first, point?.second)

    private fun variant(id: String, vararg v: RouteVia, line: List<Pair<Double, Double>> = listOf(59.0 to 30.0, 60.0 to 31.0)) =
        RouteVariant(id, 100.0, line, v.toList())

    @Test
    fun labelIsFirstStationThatOtherVariantsLack() {
        val a = variant("r1", via("Кикерино", 20.0, 1, null), via("Молосковицы", 40.0, 2, null))
        val b = variant("r2", via("Кикерино", 20.0, 1, null), via("Гатчина", 60.0, 3, null))
        assertEquals(listOf("Молосковицы", "Гатчина"), variantViaLabels(listOf(a, b)))
    }

    @Test
    fun withoutDifferencesLabelIsNeutralAndEmptyIsNull() {
        val a = variant("r1", via("Кикерино", 20.0, 1))
        val b = variant("r2", via("Кикерино", 20.0, 1))
        val c = variant("r3")
        assertEquals(listOf(null, null, null), variantViaLabels(listOf(a, b, c)))
    }

    @Test
    fun luzhskayaToSaintPetersburgUsesFirstStationsAfterRealBranchPattern() {
        val coast = variant("r1", via("Веймарн", 20.0, 1, 59.0 to 30.0), via("Котлы", 70.0, 2, 59.0 to 30.03), line = listOf(59.0 to 30.0, 59.0 to 30.03))
        val orange = variant("r2", via("ВЕЙМАРН ", 20.0, 11, 59.0 to 30.0), via("Молосковицы", 70.0, 3, 59.03 to 30.0), via("Гатчина-Варшавская", 110.0, 4, 59.06 to 30.03), line = listOf(59.0 to 30.0, 59.03 to 30.0, 59.06 to 30.03))
        val purple = variant("r3", via("Веймарн", 20.0, 12, 59.0 to 30.0), via("Молосковицы", 70.0, 13, 59.03 to 30.0), via("Гатчина-Балтийская Пассажирская", 120.0, 5, 59.06 to 29.97), line = listOf(59.0 to 30.0, 59.03 to 30.0, 59.06 to 29.97))
        assertEquals(
            listOf("Котлы", "Гатчина-Варшавская", "Гатчина-Балтийская Пассажирская"),
            variantViaLabels(listOf(coast, orange, purple)),
        )
    }

    @Test
    fun stationsWithoutOsmIdAreMatchedByNameIgnoringCase() {
        val a = variant("r1", via("Гатчина", 10.0, point = null), via("Луга", 90.0, point = null))
        val b = variant("r2", via("ГАТЧИНА ", 10.0, point = null))
        assertEquals("Луга", variantViaLabels(listOf(a, b))[0])
    }

    @Test
    fun addableStationsNeedCoordinatesAndAreOrderedByKm() {
        val v = variant("r1", via("B", 30.0), via("A", 10.0), via("Без точки", 20.0, point = null))
        assertEquals(listOf("A", "B"), v.addableStations().map { it.name })
    }
}
