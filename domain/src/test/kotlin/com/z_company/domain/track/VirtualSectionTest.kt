package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VirtualSectionTest {
    private fun section(
        id: String,
        lengthKm: Double,
        grade: Double,
        stations: List<SectionStation> = emptyList(),
    ) = SectionProfile(
        id = id, name = "Уч $id", fromStation = "", toStation = "", lengthKm = lengthKm,
        axisHash = "h$id",
        axis = SectionAxis(
            id, "h$id",
            listOf(
                AxisPoint(0.0, 55.0, 30.0),
                AxisPoint(lengthKm / 2, 55.1, 30.1),
                AxisPoint(lengthKm, 55.2, 30.2),
            ),
        ),
        elements = listOf(
            ProfileElement(0.0, lengthKm / 2, gradePermille = grade),
            ProfileElement(lengthKm / 2, lengthKm, gradePermille = grade / 2),
        ),
        kmMarks = listOf(KmMark(0.0, 100.0, 1)),
        stations = stations,
        signals = listOf(TrackSignal(lengthKm / 2, "1", SignalTrack.ODD, SignalKind.PASS, facing = 1)),
    )

    private val s1 = section("s1", 10.0, 2.0, listOf(SectionStation(0.0, "Андреево"), SectionStation(5.0, "Междуречье"), SectionStation(10.0, "Борисово")))
    private val s2 = section("s2", 20.0, -4.0, listOf(SectionStation(0.0, "Борисово"), SectionStation(20.0, "Ветлужская")))
    private val byId = mapOf("s1" to s1, "s2" to s2)

    private val e1 = GraphEdge("e1", "s1", 0.0, 10.0, "A", "B", 10.0)
    private val e2 = GraphEdge("e2", "s2", 0.0, 20.0, "B", "C", 20.0)

    @Test
    fun single_edge_forward_slicing() {
        val v = assertNotNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 0.0, 10.0)), byId))
        assertEquals(10.0, v.lengthKm, 1e-9)
        assertEquals(listOf("Андреево", "Междуречье", "Борисово"), v.stations.map { it.name })
        assertEquals(listOf(0.0, 5.0, 10.0), v.stations.map { it.km })
        assertEquals(2.0, v.elements[0].gradePermille, 1e-9)
        assertEquals("Андреево", v.fromStation)
        assertEquals("Борисово", v.toStation)
    }

    @Test
    fun two_edges_accumulate_km() {
        val v = assertNotNull(VirtualSection.compose(
            listOf(EdgeTraversal(e1, 0.0, 10.0), EdgeTraversal(e2, 0.0, 20.0)), byId))
        assertEquals(30.0, v.lengthKm, 1e-9)
        // уклон второго ребра со сдвигом 10 км
        assertEquals(4, v.elements.size)
        assertEquals(10.0, v.elements[2].startKm, 1e-9)
        assertEquals(20.0, v.elements[2].endKm, 1e-9)
        assertEquals(-4.0, v.elements[2].gradePermille, 1e-9)
        // станции: стыковая «Борисово» одна; км накапливаются (10 + 20)
        assertEquals(listOf("Андреево", "Междуречье", "Борисово", "Ветлужская"), v.stations.map { it.name })
        assertEquals(30.0, v.stations.last().km, 1e-9)
    }

    @Test
    fun reverse_traversal_flips_grade_and_signal_facing() {
        val v = assertNotNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 10.0, 0.0)), byId))
        assertEquals(10.0, v.lengthKm, 1e-9)
        // профиль развернут: первый элемент бывший хвостовой (уклон со знаком минус)
        assertEquals(-1.0, v.elements[0].gradePermille, 1e-9)
        assertEquals(-2.0, v.elements[1].gradePermille, 1e-9)
        assertEquals(listOf("Борисово", "Междуречье", "Андреево"), v.stations.map { it.name })
        assertEquals(-1, v.signals.single().facing)
    }

    @Test
    fun partial_slice_keeps_only_inner_data() {
        val v = assertNotNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 5.0, 10.0)), byId))
        assertEquals(5.0, v.lengthKm, 1e-9)
        assertEquals(listOf("Междуречье", "Борисово"), v.stations.map { it.name })
        assertEquals(listOf(0.0, 5.0), v.stations.map { it.km })
        // элемент, кончающийся ровно на границе среза, не берётся; хвостовой срезан по 5 км
        assertEquals(1, v.elements.size)
        assertEquals(0.0, v.elements[0].startKm, 1e-9)
        assertEquals(5.0, v.elements[0].endKm, 1e-9)
    }

    @Test
    fun missing_owner_returns_null() {
        assertNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 0.0, 10.0)), emptyMap()))
    }

    @Test
    fun id_is_stable_and_unique_per_path() {
        val a = assertNotNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 0.0, 10.0)), byId))
        val b = assertNotNull(VirtualSection.compose(
            listOf(EdgeTraversal(e1, 0.0, 10.0), EdgeTraversal(e2, 0.0, 20.0)), byId))
        assertEquals(a.id, assertNotNull(VirtualSection.compose(listOf(EdgeTraversal(e1, 0.0, 10.0)), byId)).id)
        assertTrue(a.id != b.id)
    }
}
