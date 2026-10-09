package com.z_company.domain.util_test

import com.z_company.domain.util.PointStationPicker
import com.z_company.domain.util.PointStationSource
import com.z_company.domain.util.RailCandidate
import com.z_company.domain.util.RailLookupResult
import com.z_company.domain.util.RailStation
import com.z_company.domain.util.RailStationLookup
import com.z_company.domain.util.RenderedStation
import com.z_company.domain.util.StopStationNamer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Станция по точке на карте редактора плеча (SCREEN_SPECS §6.6): справочник / пользовательские / OSM / сети нет. */
class PointStationPickerTest {
    // Тап в (60.0; 30.0). 0.001 градуса долготы на 60 с.ш. ~ 56 м.
    private val tapLat = 60.0
    private val tapLon = 30.0
    private val points = mapOf(
        "ближняя" to (60.0 to 30.002),   // ~111 м
        "дальняя" to (60.0 to 30.010),   // ~557 м
        "очень далёкая" to (60.0 to 30.05), // ~2,8 км
    )
    private val names = mapOf("ближняя" to "Ближняя", "дальняя" to "Дальняя", "очень далёкая" to "Очень далёкая")

    private var sent: List<RailCandidate> = emptyList()
    private var calls = 0
    private var answer: RailLookupResult = RailLookupResult.Unavailable()
    private val lookup = RailStationLookup { _, _, _, c -> calls++; sent = c; answer }
    private val namer = StopStationNamer(lookup, { 0L })

    private fun at(rendered: RenderedStation? = null, n: StopStationNamer = namer) =
        runBlocking { n.stationAtPoint(tapLat, tapLon, points, names, rendered) }

    private fun osm(name: String, lat: Double = 60.0, lon: Double = 30.004) =
        RailLookupResult.Found(RailStation(77, name, "station", "123456", lat, lon, 300, 5))

    @Test
    fun `no network - nearest directory station by straight line within 1 km`() {
        answer = RailLookupResult.Unavailable()
        val r = at()
        assertTrue(r.offline)
        val st = assertNotNull(r.station)
        assertEquals("Ближняя", st.name)
        assertEquals(PointStationSource.DIRECTORY, st.source)
        assertEquals(60.0, st.latitude)
        assertEquals(30.002, st.longitude)
        assertTrue(st.distanceM in 100..125, "distance=${st.distanceM}")
    }

    @Test
    fun `no lookup at all behaves like no network`() {
        val r = runBlocking { StopStationNamer(null, { 0L }).stationAtPoint(tapLat, tapLon, points, names) }
        assertTrue(r.offline)
        assertEquals("Ближняя", r.station?.name)
    }

    @Test
    fun `directory station farther than 1 km is not taken`() {
        answer = RailLookupResult.Unavailable()
        val far = mapOf("x" to (60.0 to 30.05))
        val r = runBlocking { namer.stationAtPoint(tapLat, tapLon, far, mapOf("x" to "Икс")) }
        assertNull(r.station)
        assertTrue(r.offline)
    }

    @Test
    fun `server candidate wins - directory name and coordinates, no osm id required`() {
        answer = RailLookupResult.Found(RailStation(null, "Ближняя", "station", null, 60.0, 30.002, 150, 3, candidateKey = "ближняя"))
        val r = at()
        assertFalse(r.offline)
        val st = assertNotNull(r.station)
        assertEquals("Ближняя", st.name)
        assertEquals(PointStationSource.DIRECTORY, st.source)
        // кандидаты ушли на сервер: только в радиусе 2 км
        assertEquals(setOf("ближняя", "дальняя"), sent.map { it.key }.toSet())
    }

    @Test
    fun `server osm station when directory has no such name`() {
        answer = osm("Ультрамар")
        val st = assertNotNull(at().station)
        assertEquals("Ультрамар", st.name)
        assertEquals(PointStationSource.OSM, st.source)
        assertEquals(77L, st.osmId)
        assertEquals("123456", st.esr)
        assertEquals(30.004, st.longitude)
    }

    @Test
    fun `osm station with name already in directory keeps directory coordinates - no overwrite`() {
        // OSM нашла «Дальняя» в другой точке, чем в справочнике пользователя
        answer = osm("Дальняя", lat = 60.0, lon = 30.011)
        val st = assertNotNull(at().station)
        assertEquals(PointStationSource.DIRECTORY, st.source)
        assertEquals("Дальняя", st.name)
        assertEquals(30.010, st.longitude)
    }

    @Test
    fun `off rails - falls back to directory then to rendered osm station`() {
        answer = RailLookupResult.NoStation("off_rails")
        assertEquals("Ближняя", at().station?.name)
        val empty = runBlocking {
            namer.stationAtPoint(tapLat, tapLon, emptyMap(), emptyMap(), RenderedStation("Ультрамар", 60.0, 30.003))
        }
        val st = assertNotNull(empty.station)
        assertEquals("Ультрамар", st.name)
        assertEquals(PointStationSource.OSM, st.source)
        assertFalse(empty.offline)
    }

    @Test
    fun `rendered osm station farther than 1 km is ignored`() {
        answer = RailLookupResult.Unavailable()
        val r = runBlocking {
            namer.stationAtPoint(tapLat, tapLon, emptyMap(), emptyMap(), RenderedStation("Далёкая", 60.0, 30.05))
        }
        assertNull(r.station)
    }

    @Test
    fun `nothing nearby - no station`() {
        answer = RailLookupResult.NoStation("not_found")
        val r = runBlocking { namer.stationAtPoint(tapLat, tapLon, emptyMap(), emptyMap()) }
        assertNull(r.station)
        assertFalse(r.offline)
    }

    @Test
    fun `lookup exception is treated as no network`() {
        val boom = StopStationNamer({ _, _, _, _ -> error("boom") }, { 0L })
        val r = runBlocking { boom.stationAtPoint(tapLat, tapLon, points, names) }
        assertTrue(r.offline)
        assertEquals("Ближняя", r.station?.name)
    }

    @Test
    fun `every tap asks the server - no cache between close points`() {
        answer = osm("А")
        at()
        at()
        assertEquals(2, calls)
    }

    @Test
    fun `nearestDirectory skips entries without a name`() {
        val st = PointStationPicker.nearestDirectory(tapLat, tapLon, mapOf("a" to (60.0 to 30.001)), emptyMap())
        assertNull(st)
    }
}
