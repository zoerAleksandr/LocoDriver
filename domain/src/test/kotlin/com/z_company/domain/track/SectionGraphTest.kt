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

class FactRouteTest {
    /* A —(e1 0..10)— B —(e2 0..10)— C; D —(e3 0..4)— B: B - развилка (3 ребра), A/C/D - тупики (1). */
    private val nA = GraphNode("A", 55.0, 30.0, "Андреево")
    private val nB = GraphNode("B", 55.1, 30.0, "Борисово")
    private val nC = GraphNode("C", 55.2, 30.0, "")
    private val nD = GraphNode("D", 55.1, 30.3, "Данилово")
    private val e1 = GraphEdge("e1", "s1", 0.0, 10.0, "A", "B", 10.0)
    private val e2 = GraphEdge("e2", "s2", 0.0, 10.0, "B", "C", 10.0)
    private val e3 = GraphEdge("e3", "s3", 0.0, 4.0, "D", "B", 4.0)
    private val graph = SectionGraph("v1", listOf(nA, nB, nC, nD), listOf(e1, e2, e3))

    /* Участок s1: ось 0..10 км по широте 55.0 -> 55.1 (в т.ч. точка узла B в конце). */
    private fun sectionS1() = SectionProfile(
        id = "s1", name = "Уч", fromStation = "", toStation = "", lengthKm = 10.0, axisHash = "h",
        axis = SectionAxis(
            "s1", "h", listOf(
                AxisPoint(0.0, 55.0, 30.0),
                AxisPoint(5.0, 55.05, 30.0),
                AxisPoint(10.0, 55.1, 30.0),
            ),
        ),
        elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
    )

    @Test
    fun junction_ahead_is_found() {
        val km = FactRoute.nextJunctionKm(graph, sectionS1(), posKm = 3.0, reversed = false)
        assertNotNull(km)
        assertEquals(10.0, km!!, 0.05)  // узел B в конце оси s1
    }

    @Test
    fun junction_behind_is_skipped() {
        // едем от B к A (reverse): развилка B позади, впереди её нет
        assertNull(FactRoute.nextJunctionKm(graph, sectionS1(), posKm = 8.0, reversed = true))
    }

    @Test
    fun range_is_from_train_to_junction() {
        val r = FactRoute.axisRange(graph, sectionS1(), posKm = 3.0, reversed = false)
        assertNotNull(r)
        assertTrue(r!!.endInclusive >= 10.0 - 0.05)   // правая граница - развилка
        assertTrue(r.start <= 3.0)                    // поезд внутри диапазона
    }

    @Test
    fun no_junction_ahead_gives_null() {
        // участок без развилок (впереди только тупик)
        val s = SectionProfile(
            id = "x", name = "У", fromStation = "", toStation = "", lengthKm = 5.0, axisHash = "h",
            axis = SectionAxis("x", "h", listOf(AxisPoint(0.0, 56.0, 31.0), AxisPoint(5.0, 56.05, 31.0))),
            elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
        )
        assertNull(FactRoute.axisRange(graph, s, posKm = 1.0, reversed = false))
    }
}

class FactRouteNameTest {
    private val nA = GraphNode("A", 55.0, 30.0, "Андреево")
    private val nB = GraphNode("B", 55.1, 30.0, "Борисово")
    private val nC = GraphNode("C", 55.2, 30.0, "")
    private val nD = GraphNode("D", 55.1, 30.3, "Данилово")
    private val e1 = GraphEdge("e1", "s1", 0.0, 10.0, "A", "B", 10.0)
    private val e2 = GraphEdge("e2", "s2", 0.0, 10.0, "B", "C", 10.0)
    private val e3 = GraphEdge("e3", "s3", 0.0, 4.0, "D", "B", 4.0)
    private val graph = SectionGraph("v1", listOf(nA, nB, nC, nD), listOf(e1, e2, e3))

    @Test
    fun junction_ahead_carries_name() {
        val s = SectionProfile(
            id = "s1", name = "Уч", fromStation = "", toStation = "", lengthKm = 10.0, axisHash = "h",
            axis = SectionAxis("s1", "h", listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(5.0, 55.05, 30.0), AxisPoint(10.0, 55.1, 30.0))),
            elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
        )
        val ahead = assertNotNull(FactRoute.nextJunction(graph, s, posKm = 3.0, reversed = false))
        assertEquals("Борисово", ahead.name)
        assertEquals(10.0, ahead.km, 0.05)
    }
}

class BranchesTest {
    private val nA = GraphNode("A", 55.0, 30.0, "Андреево")
    private val nB = GraphNode("B", 55.1, 30.0, "Борисово")
    private val nC = GraphNode("C", 55.2, 30.0, "")
    private val nD = GraphNode("D", 55.1, 30.3, "Данилово")
    private val e1 = GraphEdge("e1", "s1", 0.0, 10.0, "A", "B", 10.0)
    private val e2 = GraphEdge("e2", "s2", 0.0, 10.0, "B", "C", 10.0)
    private val e2Parallel = GraphEdge("e2-parallel", "s2b", 0.0, 10.0, "B", "C", 10.0)
    private val e3 = GraphEdge("e3", "s3", 0.0, 4.0, "D", "B", 4.0)
    private val graph = SectionGraph("v1", listOf(nA, nB, nC, nD), listOf(e1, e2, e2Parallel, e3))

    @Test
    fun branches_exclude_incoming_edge_and_carry_titles() {
        val s = SectionProfile(
            id = "s1", name = "Уч", fromStation = "", toStation = "", lengthKm = 10.0, axisHash = "h",
            axis = SectionAxis("s1", "h", listOf(AxisPoint(0.0, 55.0, 30.0), AxisPoint(5.0, 55.05, 30.0), AxisPoint(10.0, 55.1, 30.0))),
            elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
        )
        val branches = Branches.ahead(graph, s, posKm = 3.0, reversed = false)
        assertEquals(setOf("e2", "e3"), branches.map { it.edgeId }.toSet())
        // входящее ребро e1 (узел A позади) в ветки не попадает
        assertTrue(branches.none { it.edgeId == "e1" })
        assertEquals(1, branches.count { it.title == "Ветка" })
        assertEquals("Данилово", branches.first { it.edgeId == "e3" }.title)
    }

    @Test
    fun branch_title_uses_first_station_after_junction_on_branch_axis() {
        val current = section("s1", 10.0, emptyList())
        val branch = section(
            "s2", 10.0,
            listOf(SectionStation(0.0, "Борисово"), SectionStation(3.0, "Первая после"), SectionStation(9.0, "Дальняя станция")),
        )
        val branches = Branches.ahead(
            graph, current, posKm = 3.0, reversed = false,
            sectionsById = mapOf("s2" to branch),
        )
        assertEquals("Первая после", branches.first { it.edgeId == "e2" }.title)
        assertEquals(3.0, branches.first { it.edgeId == "e2" }.distanceKm, 1e-9)
    }

    @Test
    fun no_branches_when_no_junction_ahead() {
        val s = SectionProfile(
            id = "x", name = "У", fromStation = "", toStation = "", lengthKm = 5.0, axisHash = "h",
            axis = SectionAxis("x", "h", listOf(AxisPoint(0.0, 56.0, 31.0), AxisPoint(5.0, 56.05, 31.0))),
            elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
        )
        assertTrue(Branches.ahead(graph, s, posKm = 1.0, reversed = false).isEmpty())
    }
}
