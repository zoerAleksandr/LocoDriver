package com.z_company.domain.util_test

import com.z_company.domain.autorecord.stationNameForStop
import com.z_company.domain.entities.route.Station
import com.z_company.domain.entities.route.Train
import com.z_company.domain.entities.setting.ServicePhase
import com.z_company.domain.entities.setting.ServicePhasePoint
import com.z_company.domain.entities.setting.ServicePhaseStation
import com.z_company.domain.util.RailCandidate
import com.z_company.domain.util.RailLookupResult
import com.z_company.domain.util.RailStation
import com.z_company.domain.util.RailStationLookup
import com.z_company.domain.util.ServicePhaseStationPicker
import com.z_company.domain.util.StopStationNamer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StopStationNamerTest {
    private val phase = ServicePhase(
        departureStation = "Начало",
        arrivalStation = "Конец",
        distance = 50,
        departurePoint = ServicePhasePoint(60.0, 29.0),
        arrivalPoint = ServicePhasePoint(60.0, 30.5),
        intermediateStations = listOf(
            ServicePhaseStation(name = "Узел-1", latitude = 60.0, longitude = 30.000),
            ServicePhaseStation(name = "Узел-2", latitude = 60.0, longitude = 30.005),
        ),
    )
    private val points = mapOf("справочная" to (60.0 to 30.002))
    private val names = mapOf("справочная" to "Справочная")

    private var now = 1_000_000L
    private var calls = 0
    private var sent: List<RailCandidate> = emptyList()
    private var answer: RailLookupResult = RailLookupResult.NoStation("not_found")
    private val lookup = RailStationLookup { _, _, _, c -> calls++; sent = c; answer }
    private val namer = StopStationNamer(lookup, { now })

    private fun rail(name: String) = RailLookupResult.Found(RailStation(1, name, "station", null, 60.0, 30.0, 400, 5))

    private fun name(p: ServicePhase?, written: List<String?>, lon: Double, n: StopStationNamer = namer) =
        runBlocking { n.name(p, written, 60.0, lon, points, names) }

    @Test
    fun `GO, AUTO and map use one function - same result on same data`() {
        for (written in listOf(emptyList(), listOf("Начало"), listOf("Начало", "Узел-1", "Конец"))) {
            for (lon in listOf(30.0, 30.0045, 30.003, 31.5)) {
                val train = Train(
                    servicePhase = phase,
                    stations = written.map { Station(stationName = it) }.toMutableList(),
                )
                val auto = stationNameForStop(train, 60.0, lon, points, names)
                val go = name(phase, ServicePhaseStationPicker.writtenNames(written, true), lon)
                assertEquals(auto, go, "written=$written lon=$lon")
            }
        }
        assertEquals(0, calls) // с плечом сервер не спрашиваем
    }

    @Test
    fun `dense node with phase - by order, one km radius`() {
        assertEquals("Узел-1", name(phase, listOf("Начало"), 30.0045))
        assertEquals("Узел-2", name(phase, listOf("Начало", "Узел-1", "Узел-2"), 30.0))
        // 1 км (~0.018°) - ещё «здесь», старые 500 м уже не ограничивают
        assertEquals("Узел-1", name(phase, emptyList(), 30.012))
    }

    @Test
    fun `last form station is the leg arrival and is not counted as written`() {
        assertEquals("Узел-1", name(phase, ServicePhaseStationPicker.writtenNames(listOf("Начало", "Конец"), true), 30.0))
    }

    @Test
    fun `no phase - server answer wins over the directory`() {
        answer = rail("Рельсовая")
        assertEquals("Рельсовая", name(null, emptyList(), 30.003))
        assertEquals(1, calls)
    }

    @Test
    fun `phase without coordinates is treated as no phase`() {
        answer = rail("Рельсовая")
        val bare = ServicePhase(departureStation = "А", arrivalStation = "Б", distance = 1)
        assertEquals("Рельсовая", name(bare, emptyList(), 30.003))
    }

    @Test
    fun `off_rails, not_found, no_rail_data - directory fallback`() {
        for (reason in listOf("off_rails", "not_found", "no_rail_data", null)) {
            answer = RailLookupResult.NoStation(reason)
            assertEquals("Справочная", name(null, emptyList(), 30.003, StopStationNamer(lookup, { now })), "reason=$reason")
            assertNull(name(null, emptyList(), 31.5, StopStationNamer(lookup, { now })))
        }
    }

    @Test
    fun `no network or 503 - directory fallback and backoff`() {
        answer = RailLookupResult.Unavailable(retryAfterMs = 30_000)
        assertEquals("Справочная", name(null, emptyList(), 30.003))
        assertEquals(1, calls)
        // в паузе сервер не спрашиваем
        name(null, emptyList(), 30.9)
        assertEquals(1, calls)
        now += 30_001
        answer = rail("Рельсовая")
        assertEquals("Рельсовая", name(null, emptyList(), 30.9))
        assertEquals(2, calls)
    }

    @Test
    fun `lookup exception is a fallback, not a crash`() {
        val n = StopStationNamer({ _, _, _, _ -> error("boom") }, { now })
        assertEquals("Справочная", name(null, emptyList(), 30.003, n))
        val noLookup = StopStationNamer(null, { now })
        assertEquals("Справочная", name(null, emptyList(), 30.003, noLookup))
    }

    @Test
    fun `cache - within 150 m and 2 min the server is not asked again`() {
        answer = rail("Рельсовая")
        name(null, emptyList(), 30.0)
        name(null, emptyList(), 30.001) // ~55 м
        assertEquals(1, calls)
        name(null, emptyList(), 30.004) // ~220 м - снова спрашиваем
        assertEquals(2, calls)
        now += 119_000
        name(null, emptyList(), 30.004)
        assertEquals(2, calls)
        now += 2_000 // прошло > 2 мин
        name(null, emptyList(), 30.004)
        assertEquals(3, calls)
    }

    @Test
    fun `discovered station is reported for the directory`() {
        answer = rail("Новая")
        var got: String? = null
        runBlocking { namer.name(null, emptyList(), 60.0, 30.0, points, names) { got = it.name } }
        assertEquals("Новая", got)
    }

    @Test
    fun `server picks a directory candidate - directory name, no discovery`() {
        answer = RailLookupResult.Found(RailStation(null, "Чужое", "station", null, 60.0, 30.002, 300, 4, candidateKey = "справочная"))
        var discovered = false
        val r = runBlocking { namer.name(null, emptyList(), 60.0, 30.0, points, names) { discovered = true } }
        assertEquals("Справочная", r)
        assertEquals(false, discovered)
        assertEquals(listOf("справочная"), sent.map { it.key })
    }

    @Test
    fun `server picks osm - osm name and discovery`() {
        answer = RailLookupResult.Found(RailStation(5, "Осм", "station", null, 60.0, 30.0, 300, 4))
        var got: String? = null
        val r = runBlocking { namer.name(null, emptyList(), 60.0, 30.0, points, names) { got = it.name } }
        assertEquals("Осм", r)
        assertEquals("Осм", got)
    }

    @Test
    fun `candidates - at most 40, nearest first, within 2 km, only with coordinates`() {
        val pts = (0 until 60).associate { "k$it" to (60.0 to 30.0 + it * 0.0002) } + ("far" to (60.0 to 31.0)) + ("noname" to (60.0 to 30.0))
        val nms = (0 until 60).associate { "k$it" to "С$it" } + ("far" to "Далеко") + ("nocoords" to "Без координат")
        val c = StopStationNamer.candidatesFor(60.0, 30.0, pts, nms)
        assertEquals(40, c.size)
        assertEquals((0 until 40).map { "k$it" }, c.map { it.key })
        assertEquals(false, c.any { it.key == "far" || it.key == "noname" || it.key == "nocoords" })
        val few = StopStationNamer.candidatesFor(60.0, 30.0, mapOf("a" to (60.0 to 31.0)), mapOf("a" to "A"))
        assertEquals(0, few.size)
    }

    @Test
    fun `candidates not sent when phase is selected`() {
        runBlocking { namer.name(phase, emptyList(), 60.0, 30.0, points, names) }
        assertEquals(0, calls)
    }
}
