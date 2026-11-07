package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SectionGraphTest {
    /* Линейный граф: A —(e1 0..10)— B —(e2 0..10)— C; ответвление D —(e3 0..4)— B. */
    private val nA = GraphNode("A", 55.0, 30.0, "Андреево")
    private val nB = GraphNode("B", 55.1, 30.0, "Борисово")
    private val nC = GraphNode("C", 55.2, 30.0, "")
    private val nD = GraphNode("D", 55.1, 30.3, "Данилово")
    private val e1 = GraphEdge("e1", "s1", 0.0, 10.0, "A", "B", 10.0)
    private val e2 = GraphEdge("e2", "s2", 0.0, 10.0, "B", "C", 10.0)
    private val e3 = GraphEdge("e3", "s3", 0.0, 4.0, "D", "B", 4.0)
    private val graph = SectionGraph("v1", listOf(nA, nB, nC, nD), listOf(e1, e2, e3))

    private fun section(id: String, lengthKm: Double, stations: List<SectionStation>) = SectionProfile(
        id = id, name = "Уч $id", fromStation = "", toStation = "", lengthKm = lengthKm,
        axisHash = "h$id",
        axis = SectionAxis(id, "h$id", listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(lengthKm, 55.2, 30.0))),
        elements = listOf(ProfileElement(0.0, lengthKm, gradePermille = 2.0)),
        kmMarks = listOf(KmMark(0.0, 100.0, 1)),
        stations = stations,
    )

    private val sections = mapOf(
        "s1" to section("s1", 10.0, listOf(SectionStation(0.0, "Андреево"), SectionStation(5.0, "Междуречье"), SectionStation(10.0, "Борисово"))),
        "s2" to section("s2", 10.0, listOf(SectionStation(0.0, "Борисово"), SectionStation(10.0, "Ветлужская"))),
        "s3" to section("s3", 4.0, listOf(SectionStation(0.0, "Данилово"), SectionStation(4.0, "Борисово"))),
    )

    @Test
    fun path_between_nodes_across_two_edges() {
        val p = assertNotNull(graph.path(GraphPoint.AtNode("A"), GraphPoint.AtNode("C")))
        assertEquals(listOf("e1", "e2"), p.map { it.edge.id })
        assertEquals(20.0, p.sumOf { it.lengthKm }, 1e-9)
        assertTrue(p.none { it.reverse })
    }

    @Test
    fun path_picks_shorter_branch() {
        val p = assertNotNull(graph.path(GraphPoint.AtNode("D"), GraphPoint.AtNode("C")))
        assertEquals(listOf("e3", "e2"), p.map { it.edge.id })
    }

    @Test
    fun path_reversed_direction_flips_traversal() {
        val p = assertNotNull(graph.path(GraphPoint.AtNode("C"), GraphPoint.AtNode("A")))
        assertEquals(listOf("e2", "e1"), p.map { it.edge.id })
        assertTrue(p.all { it.reverse })
        assertEquals(listOf(0.0, 0.0), p.map { it.endKm })  // придём в начало каждого ребра
    }

    @Test
    fun path_between_mid_edge_points_on_same_edge_is_direct() {
        val p = assertNotNull(graph.path(GraphPoint.OnEdge("e1", 2.0), GraphPoint.OnEdge("e1", 8.0)))
        assertEquals(1, p.size)
        assertEquals(2.0, p[0].startKm)
        assertEquals(8.0, p[0].endKm)
    }

    @Test
    fun path_mid_edge_to_mid_edge_uses_tails() {
        val p = assertNotNull(graph.path(GraphPoint.OnEdge("e1", 8.0), GraphPoint.OnEdge("e2", 2.0)))
        assertEquals(listOf("e1", "e2"), p.map { it.edge.id })
        // хвосты: 8 -> конец e1 (2 км), 0 e2 -> 2 (2 км), между узлами B пусто; всего 4 км
        assertEquals(4.0, p.sumOf { it.lengthKm }, 1e-9)
        assertEquals(8.0, p.first().startKm)
        assertEquals(2.0, p.last().endKm)
    }

    @Test
    fun graph_leg_finds_stations_and_path() {
        val p = assertNotNull(GraphLeg.pathForStops(graph, sections, "Андреево", "Ветлужская"))
        assertEquals(listOf("e1", "e2"), p.map { it.edge.id })
    }

    @Test
    fun graph_leg_station_without_graph_returns_null() {
        assertNull(GraphLeg.pathForStops(graph, sections, "Андреево", "Нет такой"))
    }

    @Test
    fun graph_leg_mid_edge_station() {
        val pts = GraphLeg.pointsForStation(graph, sections, "Междуречье")
        assertEquals(listOf<GraphPoint>(GraphPoint.OnEdge("e1", 5.0)), pts)
    }
}
