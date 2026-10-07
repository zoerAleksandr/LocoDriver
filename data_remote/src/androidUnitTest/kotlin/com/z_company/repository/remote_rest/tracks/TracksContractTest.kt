package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.TrackWireCodec
import com.z_company.repository.remote_rest.RemoteRestClient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Форма JSON `/v1/tracks` против схем сервера (backend/src/schemas/tracks.py, api/v1/tracks.py `_card`). */
class TracksContractTest {
    private val json = RemoteRestClient.appJson
    private val tools = AndroidTrackBlobTools()

    // Блоб из серверного encode_chunk (те же 4 точки, что в TrackWireCodecTest в :domain).
    private val serverZlib = (
        "789cf3096164606968d87c708ef18c93534ddf757f5066e850074186827ee6f9df778732cc62ec675ed1bd2a876129f352b6cd9cdc4b8459ba458ce4742b00d63b1634"
        )

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    @Test
    fun javaZlibReadsServerBlobAndWritesReadableOne() {
        val pts = TrackWireCodec.decodeChunk(hex(serverZlib), tools)
        assertEquals(4, pts.size)
        assertEquals(55.751244, pts[0].latitude, 1e-9)
        assertEquals(-33.86882, pts[3].latitude, 1e-9)
        val again = TrackWireCodec.decodeChunk(TrackWireCodec.encodeChunk(pts, tools), tools)
        assertEquals(pts.map { it.timeMs }, again.map { it.timeMs })
    }

    @Test
    fun sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", tools.sha256Hex("abc".encodeToByteArray()))
    }

    @Test
    fun metaRequestUsesServerFieldNamesAndOmitsNulls() {
        val body = TrackMetaRequest(
            routeId = "r1", title = "№ 1 · А — Б", startedAt = 1L, endedAt = 2L, distanceKm = 1.5,
            maxSpeedKmh = 80.0, avgSpeedKmh = 40.0, pointsCount = 10, chunksExpected = 1,
            preview = TrackEncPayload("ld-track-v1", "AAAA"),
        )
        val o = json.parseToJsonElement(json.encodeToString(body)).jsonObject
        assertEquals(
            setOf(
                "routeId", "title", "startedAt", "endedAt", "distanceKm", "maxSpeedKmh", "avgSpeedKmh",
                "pointsCount", "chunksExpected", "preview",
            ),
            o.keys,
        )
        assertEquals("ld-track-v1", (o["preview"] as JsonObject)["enc"]!!.toString().trim('"'))
        val noRoute = json.parseToJsonElement(json.encodeToString(body.copy(routeId = null, preview = null))).jsonObject
        assertFalse("routeId" in noRoute.keys)
        assertFalse("preview" in noRoute.keys)
    }

    @Test
    fun requestBodiesMatchServerSchemas() {
        assertEquals("""{"storageGranted":true,"policyVersion":"2026-10"}""", json.encodeToString(TrackConsentRequest(true, "2026-10")))
        assertEquals("""{"enc":"ld-track-v1","data":"QQ==","sha256":"ab"}""", json.encodeToString(TrackChunkRequest("ld-track-v1", "QQ==", "ab")))
        assertEquals("""{"chunks":3}""", json.encodeToString(TrackCompleteRequest(3)))
        assertEquals("""{"isFavorite":true}""", json.encodeToString(TrackPatchRequest(isFavorite = true)))
        assertEquals("""{"confirm":true}""", json.encodeToString(TrackDeleteAllRequest(true)))
    }

    @Test
    fun parsesServerCardAndList() {
        val list = json.decodeFromString<TrackListResponse>(
            """{"tracks":[{"id":"7b7f7c6e-0000-0000-0000-000000000001","title":"№ 5","routeId":null,"startedAt":1760000000000,
            "endedAt":1760003600000,"distanceKm":42.5,"durationS":3600,"avgSpeedKmh":42.5,"maxSpeedKmh":90.0,"pointsCount":720,
            "isFavorite":true,"status":"complete","expiresAt":null,"preview":{"enc":"ld-track-v1","data":"AA=="},"newField":1}],
            "nextCursor":"MTc2MDAwMDAwMDAwMDphYmM="}""",
        )
        val c = list.tracks.single()
        assertEquals(3600L, c.durationS)
        assertTrue(c.isFavorite)
        assertNull(c.expiresAt)
        assertNull(c.routeId)
        assertEquals("MTc2MDAwMDAwMDAwMDphYmM=", list.nextCursor)
        val done = json.decodeFromString<TrackCompleteResponse>("""{"status":"uploading","missing":[2,3]}""")
        assertEquals(listOf(2, 3), done.missing)
        val consent = json.decodeFromString<TrackConsentResponse>("""{"storageGranted":false,"deletedTracks":4}""")
        assertEquals(4, consent.deletedTracks)
        val chunks = json.decodeFromString<TrackCardResponse>(
            """{"id":"x","chunks":[{"n":0,"pointsCount":2000,"tFrom":1,"tTo":2}]}""",
        )
        assertEquals(2000, chunks.chunks.single().pointsCount)
    }
}
