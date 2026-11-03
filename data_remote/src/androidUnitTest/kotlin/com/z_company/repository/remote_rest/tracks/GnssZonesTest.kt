package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.gnss.GnssCell
import com.z_company.domain.gnss.GnssContribCell
import com.z_company.domain.gnss.GnssContribSession
import com.z_company.domain.gnss.GnssContribSettings
import com.z_company.domain.gnss.GnssContribStore
import com.z_company.domain.gnss.GnssContribTag
import com.z_company.domain.gnss.GnssTagStorage
import com.z_company.domain.gnss.GnssTileCache
import com.z_company.repository.remote_rest.RemoteRestClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeZonesApi : GnssZonesApi {
    val posted = mutableListOf<GnssZoneContribRequest>()
    val getCalls = mutableListOf<Pair<GnssBbox, String?>>()
    val seen = mutableSetOf<String>()
    var failPost: Exception? = null
    var failGet: Exception? = null
    var response = GnssZonesResponse(1, 5L, listOf(GnssZoneCellDto(10, 20, 2, 1)))
    var etag: String? = "\"e1\""
    var notModifiedFor: String? = null

    override suspend fun postContrib(token: String, body: GnssZoneContribRequest): GnssZoneContribResponse {
        failPost?.let { throw it }
        posted += body
        return GnssZoneContribResponse(true, !seen.add(body.contribId))
    }

    override suspend fun zones(token: String, bbox: GnssBbox, etag: String?): GnssZonesFetch {
        getCalls += bbox to etag
        failGet?.let { throw it }
        if (etag != null && etag == notModifiedFor) return GnssZonesFetch.NotModified
        return GnssZonesFetch.Data(this.etag, response)
    }
}

private class MemContribStore : GnssContribStore {
    val items = linkedMapOf<String, GnssContribSession>()
    override fun save(session: GnssContribSession) { items[session.id] = session }
    override fun pendingIds() = items.keys.toList()
    override fun load(id: String) = items[id]
    override fun delete(id: String) { items.remove(id) }
    override fun clear() = items.clear()
}

private class MemTagStorage : GnssTagStorage {
    var t: String? = null
    var at = 0L
    override fun tag() = t
    override fun createdAtMs() = at
    override fun save(tag: String, createdAtMs: Long) { t = tag; at = createdAtMs }
    override fun clear() { t = null; at = 0 }
}

private class MemTileStore : GnssTileStore {
    val map = mutableMapOf<String, GnssTileCache>()
    var cleared = 0
    override fun load(key: String) = map[key]
    override fun save(key: String, tile: GnssTileCache) { map[key] = tile }
    override fun clear() { map.clear(); cleared++ }
}

class GnssZoneContributorTest {
    private val api = FakeZonesApi()
    private val store = MemContribStore()
    private val tagStorage = MemTagStorage()
    private var now = 1_700_000_000_000L
    private var allowed = true
    private var n = 0
    private val tag = GnssContribTag(tagStorage) { "tag-${++n}" }

    private fun contributor() = GnssZoneContributor(api, store, GnssContribSettings { allowed }, tag) { now }
    private fun run() = runBlocking { contributor().run("Bearer t") }
    private fun session(id: String = "11111111-1111-1111-1111-111111111111", at: Long = now) =
        GnssContribSession(id, at, listOf(GnssContribCell(GnssCell(40133, 123456), 3, 2))).also { store.save(it) }

    @Test
    fun sendsAndDeletesSessionWithDeterministicContribId() {
        session()
        val r = run()
        assertEquals(GnssZoneContributor.Outcome.DONE, r.outcome)
        assertEquals(1, r.sent)
        assertTrue(store.items.isEmpty())
        val b = api.posted.single()
        assertEquals("11111111-1111-1111-1111-111111111111", b.contribId)
        assertEquals(1, b.step)
        assertEquals("2023-11-14", b.day)
        assertEquals(listOf(GnssContribCellDto(40133, 123456, 3, 2)), b.cells)
        // Повтор той же поездки - duplicate, без накрутки.
        session()
        assertEquals(1, run().duplicates)
    }

    @Test
    fun bodyContainsOnlyContractFields() {
        session()
        run()
        val json = RemoteRestClient.appJson.encodeToString(api.posted.single())
        val parsed = RemoteRestClient.appJson.parseToJsonElement(json) as JsonObject
        assertEquals(setOf("contribId", "tag", "step", "day", "cells"), parsed.keys)
        val cell = parsed["cells"]!!.jsonArray.single().jsonObject
        assertEquals(setOf("iy", "ix", "kinds", "count"), cell.keys)
        // Ответ разбирается camelCase, лишние поля игнорируются.
        val resp = RemoteRestClient.appJson.decodeFromString<GnssZonesResponse>(
            """{"step":1,"updatedAt":1760000000000,"cells":[{"iy":1,"ix":2,"level":3,"kinds":5,"extra":1}],"x":1}""",
        )
        assertEquals(GnssZoneCellDto(1, 2, 3, 5), resp.cells.single())
        assertEquals(1760000000000L, resp.updatedAt)
    }

    @Test
    fun tagIsStableWithinMonthRotatesAfterAndNotInAccount() {
        session("a1"); run()
        session("a2"); run()
        assertEquals(api.posted[0].tag, api.posted[1].tag)
        now += 31L * 24 * 60 * 60 * 1000
        session("a3", at = now); run()
        assertTrue(api.posted[2].tag != api.posted[0].tag)
    }

    @Test
    fun noConsentDeletesEverythingAndSendsNothing() {
        session()
        allowed = false
        val r = run()
        assertEquals(GnssZoneContributor.Outcome.NO_CONSENT, r.outcome)
        assertTrue(store.items.isEmpty())
        assertTrue(api.posted.isEmpty())
    }

    @Test
    fun emptyOrStaleSessionsAreNeverSent() {
        store.save(GnssContribSession("e1", now, emptyList()))
        store.items["e1"] = GnssContribSession("e1", now, emptyList())
        session("old", at = now - 7L * 24 * 60 * 60 * 1000)
        val r = run()
        assertEquals(GnssZoneContributor.Outcome.DONE, r.outcome)
        assertTrue(api.posted.isEmpty())
        assertTrue(store.items.isEmpty())
    }

    @Test
    fun serverNotDeployedNetworkAndRateLimitKeepSessionForRetry() {
        session()
        for (e in listOf<Exception>(GnssZonesApiException(404, null), GnssZonesApiException(503, "gnss_zones_disabled"), GnssZonesApiException(429, "rate_limited"), IOException("net"))) {
            api.failPost = e
            assertEquals(GnssZoneContributor.Outcome.RETRY_LATER, run().outcome, e.toString())
            assertEquals(1, store.items.size)
        }
        api.failPost = null
        assertEquals(GnssZoneContributor.Outcome.DONE, run().outcome)
        assertTrue(store.items.isEmpty())
    }

    @Test
    fun unauthorizedKeepsSessionRejectionDropsIt() {
        session()
        api.failPost = GnssZonesApiException(401, null)
        assertEquals(GnssZoneContributor.Outcome.UNAUTHORIZED, run().outcome)
        assertEquals(1, store.items.size)
        api.failPost = GnssZonesApiException(422, "bad_cell")
        val r = run()
        assertEquals(GnssZoneContributor.Outcome.DONE, r.outcome)
        assertEquals(1, r.dropped)
        assertTrue(store.items.isEmpty())
    }
}

class GnssZonesRepositoryTest {
    private val api = FakeZonesApi()
    private val store = MemTileStore()
    private var now = 1_000_000L
    private var token: String? = "Bearer t"
    private val repo = GnssZonesRepository(api, store, { token }) { now }

    // Небольшая область внутри одной плитки 0,5° (lat 59.5..60, lon 32..32.5).
    private val view = GnssBbox(59.6, 32.1, 59.7, 32.2)

    private fun load() = runBlocking { repo.load(view) }

    @Test
    fun fetchesCachesAndThrottlesFiveMinutes() {
        val cells = load()
        assertEquals(1, cells.size)
        assertEquals(2, cells.single().level)
        assertEquals(1, api.getCalls.size)
        // Границы запроса - плитка 0,5°, не больше 1° по каждой оси.
        val box = api.getCalls.single().first
        assertEquals(0.5, box.maxLat - box.minLat, 1e-9)
        assertEquals(59.5, box.minLat, 1e-9)
        assertEquals(32.0, box.minLon, 1e-9)
        now += 4 * 60_000
        assertEquals(1, load().size)
        assertEquals(1, api.getCalls.size)
        now += 2 * 60_000
        load()
        assertEquals(2, api.getCalls.size)
        // Повторный запрос идёт с If-None-Match.
        assertEquals("\"e1\"", api.getCalls[1].second)
    }

    @Test
    fun notModifiedKeepsCachedCells() {
        load()
        api.notModifiedFor = "\"e1\""
        api.response = GnssZonesResponse(1, 0, emptyList())
        now += 6 * 60_000
        assertEquals(1, load().size)
        assertEquals(now, store.map.values.single().fetchedAtMs)
    }

    @Test
    fun errorsAreSilentAndShowCache() {
        load()
        for (e in listOf<Exception>(GnssZonesApiException(404, null), GnssZonesApiException(503, null), IOException("x"))) {
            now += 6 * 60_000
            api.failGet = e
            assertEquals(1, load().size, e.toString())
        }
        // Без кэша и с ошибкой - просто пусто, без исключений.
        val fresh = GnssZonesRepository(api, MemTileStore(), { token }) { now }
        assertTrue(runBlocking { fresh.load(view) }.isEmpty())
    }

    @Test
    fun rateLimitPausesAllRequestsForAnHour() {
        api.failGet = GnssZonesApiException(429, "rate_limited")
        load()
        assertEquals(1, api.getCalls.size)
        api.failGet = null
        now += 10 * 60_000
        assertTrue(load().isEmpty())
        assertEquals(1, api.getCalls.size)
        now += 61 * 60_000
        assertEquals(1, load().size)
    }

    @Test
    fun signedOutGetsNothingAndCacheIsWiped() {
        load()
        token = null
        assertTrue(load().isEmpty())
        assertTrue(store.map.isEmpty())
        assertEquals(1, api.getCalls.size)
        load()
        assertEquals(1, store.cleared)
    }

    @Test
    fun tooManyCellsSplitsTileIntoFour() {
        var first = true
        val splitApi = object : GnssZonesApi {
            val boxes = mutableListOf<GnssBbox>()
            override suspend fun postContrib(token: String, body: GnssZoneContribRequest) = GnssZoneContribResponse()
            override suspend fun zones(token: String, bbox: GnssBbox, etag: String?): GnssZonesFetch {
                boxes += bbox
                if (first) {
                    first = false
                    throw GnssZonesApiException(422, "too_many_cells")
                }
                return GnssZonesFetch.Data(null, GnssZonesResponse(1, 1, listOf(GnssZoneCellDto(bbox.minLat.toInt(), (bbox.minLon * 10).toInt(), 1, 1))))
            }
        }
        val r = GnssZonesRepository(splitApi, MemTileStore(), { token }) { now }
        val cells = runBlocking { r.load(view) }
        assertEquals(4, cells.size)
        assertEquals(5, splitApi.boxes.size)
        assertTrue(splitApi.boxes.drop(1).all { it.maxLat - it.minLat <= 0.25 + 1e-9 })
    }

    @Test
    fun viewportTilesAreCappedAndNearestToCenter() {
        // Область 3°x3° = 36 плиток: берём не более 4, ближайшие к центру.
        val tiles = GnssZonesRepository.tilesOf(GnssBbox(58.0, 30.0, 61.0, 33.0))
        assertEquals(4, tiles.size)
        assertTrue(tiles.all { it.bbox.minLat in 58.5..60.0 && it.bbox.minLon in 30.5..32.5 })
        assertTrue(GnssZonesRepository.tilesOf(GnssBbox(60.0, 30.0, 59.0, 33.0)).isEmpty())
    }
}
