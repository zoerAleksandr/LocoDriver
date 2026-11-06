package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineLegTest {
    private fun section(id: String, vararg stations: SectionStation) = SectionProfile(
        id = id, name = "Уч $id", fromStation = "", toStation = "", lengthKm = 100.0, axisHash = "h",
        axis = SectionAxis(id, "h", listOf(AxisPoint(0.0, 60.0, 30.0), AxisPoint(100.0, 61.0, 31.0))),
        elements = emptyList(), kmMarks = emptyList(), stations = stations.toList(),
    )

    private val s = section(
        "a",
        SectionStation(0.0, "Луга I", 58.7, 29.8), SectionStation(20.0, "Мшинская", 58.9, 29.9),
        SectionStation(55.0, "Вревская", null, null), SectionStation(100.0, "Батецкая", 58.5, 30.1),
    )

    @Test
    fun forward_leg_has_intermediates_in_order() {
        val r = assertNotNull(OfflineLeg.determine(listOf(s), "Луга I", "Батецкая"))
        assertEquals(100.0, r.lengthKm, 1e-9)
        assertEquals(listOf("Мшинская", "Вревская"), r.stations.map { it.name })
        assertEquals(listOf(20.0, 55.0), r.stations.map { it.kmFromStart })
    }

    @Test
    fun reverse_leg_reorders_and_measures_from_departure() {
        val r = assertNotNull(OfflineLeg.determine(listOf(s), "Батецкая", "Луга I"))
        assertEquals(listOf("Вревская", "Мшинская"), r.stations.map { it.name })
        assertEquals(listOf(45.0, 80.0), r.stations.map { it.kmFromStart })
        assertEquals("Батецкая", r.departure.name)
    }

    @Test
    fun inner_leg_gives_only_its_part() {
        val r = assertNotNull(OfflineLeg.determine(listOf(s), "Мшинская", "Батецкая"))
        assertEquals(80.0, r.lengthKm, 1e-9)
        assertEquals(listOf("Вревская"), r.stations.map { it.name })
    }

    @Test
    fun station_without_coordinates_gets_point_from_axis() {
        val r = assertNotNull(OfflineLeg.determine(listOf(s), "Луга I", "Батецкая"))
        val vrev = r.stations.first { it.name == "Вревская" }
        assertNotNull(vrev.lat); assertNotNull(vrev.lon)
        assertEquals(60.55, vrev.lat!!, 1e-6)
    }

    @Test
    fun shortest_section_wins_and_missing_station_gives_null() {
        val other = section("b", SectionStation(10.0, "Луга I", 1.0, 1.0), SectionStation(40.0, "Батецкая", 2.0, 2.0))
        assertEquals("b", OfflineLeg.determine(listOf(s, other), "Луга I", "Батецкая")?.sectionId)
        assertNull(OfflineLeg.determine(listOf(s), "Луга I", "Нарва"))
        assertNull(OfflineLeg.determine(listOf(s), "Луга I", "луга-i"))
        assertTrue(OfflineLeg.determine(emptyList(), "A", "B") == null)
    }
}
