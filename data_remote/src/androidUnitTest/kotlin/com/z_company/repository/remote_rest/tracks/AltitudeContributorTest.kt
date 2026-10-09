@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.AltitudeContribCodec
import com.z_company.domain.track.AltitudeContribSettings
import com.z_company.domain.track.AltitudeContribStore
import com.z_company.domain.track.AltitudeSample
import com.z_company.domain.track.AxisCache
import com.z_company.domain.track.AxisPoint
import com.z_company.domain.track.PendingContribSession
import com.z_company.domain.track.SectionAxis
import com.z_company.repository.remote_rest.RemoteRestClient
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import java.io.IOException
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val M_PER_DEG = 111_320.0
private const val HASH1 = "11111111111111111111111111111111"
private const val HASH2 = "22222222222222222222222222222222"

private fun geometry() = (0..10).map { AdvisorGeoPointDto(it.toDouble(), 55.0 + it * 1000 / M_PER_DEG, 37.0) }

private class FakeAdvisorApi : AdvisorApi {
    var summaries = mutableListOf(AdvisorSectionSummaryDto("sec-1", true, HASH1))
    var detailHash = HASH1
    var serverHash = HASH1
    val posted = mutableListOf<AltitudeContribRequest>()
    val seenIds = mutableSetOf<String>()
    var sectionCalls = 0
    var failPost: Exception? = null
    var failSections: Exception? = null

    override suspend fun sections(): List<AdvisorSectionSummaryDto> {
        failSections?.let { throw it }
        return summaries
    }

    override suspend fun section(id: String): AdvisorSectionDto {
        sectionCalls++
        // После перекачки сервер отдаёт актуальный хэш.
        return AdvisorSectionDto(id, detailHash, geometry())
    }

    override suspend fun postAltitudeContrib(token: String, body: AltitudeContribRequest): AltitudeContribResponse {
        failPost?.let { throw it }
        if (body.axisHash != serverHash) throw AdvisorApiException(409, "axis_changed")
        posted += body
        return AltitudeContribResponse(true, if (seenIds.add(body.contribId)) "stored" else "duplicate")
    }
}

private class MemStore : AltitudeContribStore {
    val items = linkedMapOf<String, PendingContribSession>()
    override fun save(session: PendingContribSession) { items[session.id] = session }
    override fun pendingIds() = items.keys.toList()
    override fun load(id: String) = items[id]
    override fun delete(id: String) { items.remove(id) }
    override fun clear() = items.clear()
}

private class MemAxisCache : AxisCache {
    val map = mutableMapOf<String, SectionAxis>()
    override fun load(sectionId: String) = map[sectionId]
    override fun save(axis: SectionAxis) { map[axis.sectionId] = axis }
    override fun retain(keepIds: Set<String>) { map.keys.retainAll(keepIds) }
}

class AltitudeContributorTest {
    private val tools = AndroidTrackBlobTools()
    private val api = FakeAdvisorApi()
    private val store = MemStore()
    private val cache = MemAxisCache()
    private var consent = true
    private var now = 1_000_000L

    private fun contributor() = AltitudeContributor(api, store, cache, AltitudeContribSettings { consent }, tools, nowMs = { now })

    private fun samples(fromM: Double = 1000.0, count: Int = 12): List<AltitudeSample> =
        (0 until count).map {
            AltitudeSample(55.0 + (fromM + it * 50) / M_PER_DEG, 37.0, 100.0 + it, null, 5f)
        }

    private fun session(id: String = "s1") = PendingContribSession(id, now, samples()).also { store.save(it) }

    private fun run(): AltitudeContributor.Report = runBlocking { contributor().run("Bearer t") }

    @Test
    fun sendsFragmentAndDeletesSession() {
        session()
        val r = run()
        assertEquals(AltitudeContributor.Outcome.DONE, r.outcome)
        assertTrue(store.items.isEmpty())
        val body = api.posted.single()
        assertEquals("sec-1", body.sectionId)
        assertEquals(HASH1, body.axisHash)
        assertEquals(25, body.stepM)
        assertEquals("gnss", body.source)
        val alts = AltitudeContribCodec.decodeRaw(tools.inflate(Base64.decode(body.data), 100_000))
        assertEquals(body.offsetM.size, alts.size)
        assertEquals(alts.map { it == null }, body.offsetM.map { it == null })
        assertTrue(alts.any { it != null })
    }

    @Test
    fun bodyHasNoIdentityTimeOrCoordinates() {
        session()
        run()
        val json = RemoteRestClient.appJson.encodeToString(api.posted.single())
        val keys = setOf("contribId", "sectionId", "axisHash", "kmFromM", "stepM", "source", "meanAccM", "data", "offsetM")
        val parsed = RemoteRestClient.appJson.parseToJsonElement(json) as kotlinx.serialization.json.JsonObject
        assertTrue(parsed.keys.all { it in keys }, parsed.keys.toString())
        assertTrue("offsetM" in parsed.keys)
    }

    @Test
    fun axisIsCachedAndNotRefetchedWhileHashSame() {
        session("s1")
        run()
        session("s2")
        run()
        assertEquals(1, api.sectionCalls)
        assertNotNull(cache.load("sec-1"))
    }

    @Test
    fun axisChangedRefetchesAndRebuildsUnderNewHash() {
        cache.save(SectionAxis("sec-1", HASH1, geometry().map { AxisPoint(it.km, it.lat, it.lon) }))
        // Сервер уже сменил ось, но список ещё отдаёт старый хэш (кэш считается актуальным) -> 409.
        api.serverHash = HASH2
        api.detailHash = HASH2
        session()
        val r = run()
        assertEquals(AltitudeContributor.Outcome.DONE, r.outcome)
        assertEquals(1, api.sectionCalls)
        assertEquals(HASH2, api.posted.single().axisHash)
        assertEquals(HASH2, cache.load("sec-1")!!.axisHash)
        assertTrue(store.items.isEmpty())
    }

    @Test
    fun axisChangedWithSameHashAfterRefetchSkipsContribution() {
        api.serverHash = HASH2 // сервер ждёт другой хэш, а detail всё ещё отдаёт старый
        session()
        val r = run()
        assertEquals(AltitudeContributor.Outcome.DONE, r.outcome)
        assertTrue(api.posted.isEmpty())
        assertEquals(1, r.skipped)
    }

    @Test
    fun networkFailureKeepsSessionAndRetryDoesNotDuplicate() {
        session()
        api.failPost = IOException("offline")
        assertEquals(AltitudeContributor.Outcome.RETRY_LATER, run().outcome)
        assertEquals(1, store.items.size)
        api.failPost = null
        assertEquals(AltitudeContributor.Outcome.DONE, run().outcome)
        assertTrue(store.items.isEmpty())
        // Повтор сеанса с тем же id даёт тот же contribId -> сервер ответит duplicate.
        session()
        val r = run()
        assertEquals(1, r.duplicates)
    }

    @Test
    fun serverErrorsMapToRetryOrSkip() {
        session()
        api.failPost = AdvisorApiException(429, "rate_limited")
        assertEquals(AltitudeContributor.Outcome.RETRY_LATER, run().outcome)
        api.failPost = AdvisorApiException(503, "tracks_disabled")
        assertEquals(AltitudeContributor.Outcome.RETRY_LATER, run().outcome)
        assertEquals(1, store.items.size)
        api.failPost = AdvisorApiException(401, null)
        assertEquals(AltitudeContributor.Outcome.UNAUTHORIZED, run().outcome)
        assertEquals(1, store.items.size)
        api.failPost = AdvisorApiException(422, "bad_payload")
        assertEquals(AltitudeContributor.Outcome.DONE, run().outcome)
        assertTrue(store.items.isEmpty())
    }

    @Test
    fun sectionsFailureKeepsSession() {
        session()
        api.failSections = IOException("offline")
        assertEquals(AltitudeContributor.Outcome.RETRY_LATER, run().outcome)
        assertEquals(1, store.items.size)
    }

    @Test
    fun withoutConsentNothingIsSentAndBufferCleared() {
        session()
        consent = false
        val r = run()
        assertEquals(AltitudeContributor.Outcome.NO_CONSENT, r.outcome)
        assertTrue(store.items.isEmpty())
        assertTrue(api.posted.isEmpty())
        assertEquals(0, api.sectionCalls)
    }

    @Test
    fun staleSessionIsDroppedAndTrackOffAxisSendsNothing() {
        store.save(PendingContribSession("old", now - AltitudeContributor.MAX_AGE_MS - 1, samples()))
        assertEquals(AltitudeContributor.Outcome.DONE, run().outcome)
        assertTrue(store.items.isEmpty())
        assertEquals(0, api.sectionCalls)
        // Трек далеко от оси: сеанс обработан, вкладов нет.
        store.save(PendingContribSession("far", now, samples().map { it.copy(lon = 38.0) }))
        assertEquals(AltitudeContributor.Outcome.DONE, run().outcome)
        assertTrue(api.posted.isEmpty())
        assertFalse(store.items.isNotEmpty())
    }
}
