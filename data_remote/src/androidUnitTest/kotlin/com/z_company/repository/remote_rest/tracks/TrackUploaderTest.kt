@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.InMemoryTrackRepository
import com.z_company.domain.track.TrackPacking
import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackSyncSettings
import com.z_company.domain.track.TrackTrainResolution
import com.z_company.domain.track.TrackUploadState
import com.z_company.domain.track.TrackWireCodec
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Сервер в памяти с теми же правилами идемпотентности, что у `/v1/tracks` (дубликат чанка, конфликт sha256). */
private class FakeTracksApi : TracksApi {
    val calls = mutableListOf<String>()
    var serverConsent = true
    val tracks = mutableMapOf<String, TrackMetaRequest>()
    val chunks = mutableMapOf<Pair<String, Int>, TrackChunkRequest>()
    val deleted = mutableListOf<String>()
    val patches = mutableListOf<Pair<String, TrackPatchRequest>>()

    /** Сбои по одному на вызов: ключ — "метод" или "метод:n". */
    val failures = ArrayDeque<Pair<String, Exception>>()
    var completeMissingOnce: List<Int>? = null
    var consentPuts = mutableListOf<Boolean>()

    private fun maybeFail(key: String) {
        val idx = failures.indexOfFirst { it.first == key }
        if (idx >= 0) throw failures.removeAt(idx).second
    }

    override suspend fun getConsent(token: String) = TrackConsentResponse(serverConsent)

    override suspend fun putConsent(token: String, body: TrackConsentRequest): TrackConsentResponse {
        calls += "putConsent"
        maybeFail("putConsent")
        serverConsent = body.storageGranted
        consentPuts += body.storageGranted
        var n = 0
        if (!body.storageGranted) {
            n = tracks.size
            tracks.clear()
            chunks.clear()
        }
        return TrackConsentResponse(body.storageGranted, deletedTracks = n)
    }

    override suspend fun putTrack(token: String, trackId: String, body: TrackMetaRequest): TrackCardResponse {
        calls += "putTrack"
        maybeFail("putTrack")
        if (!serverConsent) throw TracksApiException(403, "consent_required")
        tracks[trackId] = body
        return TrackCardResponse(id = trackId)
    }

    override suspend fun putChunk(token: String, trackId: String, chunkNo: Int, body: TrackChunkRequest): TrackChunkResponse {
        calls += "putChunk:$chunkNo"
        maybeFail("putChunk")
        maybeFail("putChunk:$chunkNo")
        val key = trackId to chunkNo
        val old = chunks[key]
        if (old != null && old.sha256 != body.sha256) throw TracksApiException(409, "chunk_conflict")
        chunks[key] = body
        return TrackChunkResponse(chunkNo, if (old != null) "duplicate" else "stored")
    }

    override suspend fun complete(token: String, trackId: String, body: TrackCompleteRequest): TrackCompleteResponse {
        calls += "complete"
        maybeFail("complete")
        completeMissingOnce?.let {
            completeMissingOnce = null
            // Сервер «потерял» эти чанки — клиент обязан дослать.
            it.forEach { n -> chunks.remove(trackId to n) }
            return TrackCompleteResponse("uploading", it)
        }
        val missing = (0 until body.chunks).filter { (trackId to it) !in chunks }
        return TrackCompleteResponse(if (missing.isEmpty()) "complete" else "uploading", missing)
    }

    override suspend fun list(token: String, limit: Int, cursor: String?, favorite: Boolean?) = TrackListResponse()
    override suspend fun getTrack(token: String, trackId: String) = TrackCardResponse(id = trackId)
    override suspend fun getChunk(token: String, trackId: String, chunkNo: Int) = TrackChunkPayload("", "")

    override suspend fun patch(token: String, trackId: String, body: TrackPatchRequest): TrackCardResponse {
        calls += "patch"
        maybeFail("patch")
        patches += trackId to body
        return TrackCardResponse(id = trackId)
    }

    override suspend fun delete(token: String, trackId: String) {
        calls += "delete"
        maybeFail("delete")
        deleted += trackId
        tracks.remove(trackId)
        chunks.keys.removeAll { it.first == trackId }
    }

    override suspend fun deleteAll(token: String) = TrackDeleteAllResponse(0)
}

private class FakeSettings(var local: Boolean, var server: Boolean?) : TrackSyncSettings {
    override fun localConsent() = local
    override fun serverConsent() = server
    override fun setServerConsent(value: Boolean?) {
        server = value
    }
}

class TrackUploaderTest {
    private val tools = AndroidTrackBlobTools()
    private val now = 1_800_000_000_000L
    private val api = FakeTracksApi()
    private val repo = InMemoryTrackRepository()
    private var resolution: TrackTrainResolution = TrackTrainResolution.Found("route-1", "№ 2301 · А — Б")
    private val settings = FakeSettings(local = true, server = true)

    private fun uploader() = TrackUploader(api, repo, tools, { resolution }, settings, nowMs = { now })

    private fun seed(trackId: String, count: Int, endedAgoMs: Long = 60 * 60_000L, trainId: String = "train-1") {
        val end = now - endedAgoMs
        val start = end - (count - 1) * 5_000L
        for (i in 0 until count) {
            repo.append(
                trackId, trainId,
                TrackPoint(start + i * 5_000L, 55.0 + i * 1e-4, 37.0 + i * 1e-4, 40.0 + i % 30, 5f, 120.0 + i % 7),
            )
        }
    }

    private fun run() = runBlocking { uploader().run("Bearer t") }

    @Test
    fun uploadsMetaChunksAndMarksUploaded() {
        seed("t1", 4500)
        val r = run()
        assertEquals(TrackUploader.Outcome.DONE, r.outcome)
        assertEquals(1, r.uploaded)
        assertEquals(TrackUploadState.UPLOADED, repo.track("t1")!!.uploadState)
        assertEquals(4500L, repo.track("t1")!!.uploadedPoints)

        val meta = api.tracks.getValue("t1")
        assertEquals("route-1", meta.routeId)
        assertEquals("№ 2301 · А — Б", meta.title)
        assertEquals(4500, meta.pointsCount)
        assertEquals(3, meta.chunksExpected)
        val preview = TrackWireCodec.decodeChunk(Base64.decode(meta.preview!!.data), tools)
        assertTrue(preview.size <= 100)

        // Чанки на сервере декодируются в исходные точки в исходном порядке, sha256 совпадает с блобом.
        val back = (0 until 3).flatMap { n ->
            val c = api.chunks.getValue("t1" to n)
            val blob = Base64.decode(c.data)
            assertEquals(tools.sha256Hex(blob), c.sha256)
            assertEquals(TrackWireCodec.ENC_NAME, c.enc)
            TrackWireCodec.decodeChunk(blob, tools)
        }
        val src = repo.points("t1")
        assertEquals(src.size, back.size)
        assertEquals(src.map { it.timeMs }, back.map { it.timeMs })
    }

    @Test
    fun secondRunDoesNothingAndCoordinatesStayOutOfCalls() {
        seed("t1", 100)
        run()
        api.calls.clear()
        val r = run()
        assertEquals(TrackUploader.Outcome.DONE, r.outcome)
        assertEquals(0, r.uploaded)
        assertTrue(api.calls.isEmpty(), "повторный прогон не должен ходить в сеть: ${api.calls}")
    }

    @Test
    fun retryAfterMidUploadFailureIsIdempotent() {
        seed("t1", 4500)
        api.failures += "putChunk:1" to IOException("offline")
        val first = run()
        assertEquals(TrackUploader.Outcome.RETRY_LATER, first.outcome)
        assertEquals(TrackUploadState.PENDING, repo.track("t1")!!.uploadState)
        assertTrue(("t1" to 0) in api.chunks)

        val second = run()
        assertEquals(TrackUploader.Outcome.DONE, second.outcome)
        assertEquals(TrackUploadState.UPLOADED, repo.track("t1")!!.uploadState)
        assertEquals(3, api.chunks.keys.count { it.first == "t1" })
    }

    @Test
    fun completeMissingTriggersResend() {
        seed("t1", 2500)
        api.completeMissingOnce = listOf(1)
        val r = run()
        assertEquals(TrackUploader.Outcome.DONE, r.outcome)
        assertTrue(("t1" to 1) in api.chunks)
        assertEquals(2, api.calls.count { it == "complete" })
        assertEquals(2, api.calls.count { it == "putChunk:1" })
    }

    @Test
    fun trackStillBeingRecordedWaits() {
        seed("t1", 50, endedAgoMs = 60_000L)
        val r = run()
        assertTrue(r.waitingForQuiet)
        assertEquals(0, r.uploaded)
        assertFalse(api.calls.contains("putTrack"))
    }

    @Test
    fun routeNotSyncedDefersWithoutLosingTrack() {
        seed("t1", 50)
        api.failures += "putTrack" to TracksApiException(409, "route_not_synced")
        val r = run()
        assertEquals(TrackUploader.Outcome.RETRY_LATER, r.outcome)
        assertEquals(TrackUploadState.PENDING, repo.track("t1")!!.uploadState)
        assertEquals(TrackUploader.Outcome.DONE, run().outcome)
    }

    @Test
    fun deletedRouteRemovesLocalTrack() {
        seed("t1", 50)
        api.failures += "putTrack" to TracksApiException(410, "route_deleted")
        run()
        assertEquals(null, repo.track("t1"))
        assertTrue(repo.pendingRemoteDeletes().isEmpty())
    }

    @Test
    fun noConsentNeverTouchesTheNetworkForTracks() {
        settings.local = false
        settings.server = null
        seed("t1", 50)
        val r = run()
        assertEquals(TrackUploader.Outcome.NO_CONSENT, r.outcome)
        assertTrue(api.calls.isEmpty())
        assertEquals(false, settings.server)
    }

    @Test
    fun revokeSendsConsentFalseDeliversDeletesAndResetsMarks() {
        seed("t1", 50)
        run()
        assertEquals(TrackUploadState.UPLOADED, repo.track("t1")!!.uploadState)
        // Пользователь отозвал согласие офлайн: локально включён, на сервере ещё нет.
        settings.local = false
        val r = run()
        assertEquals(TrackUploader.Outcome.NO_CONSENT, r.outcome)
        assertEquals(listOf(false), api.consentPuts.takeLast(1))
        assertTrue(api.tracks.isEmpty())
        assertEquals(false, settings.server)
        assertEquals(TrackUploadState.PENDING, repo.track("t1")!!.uploadState)
    }

    @Test
    fun enablingConsentPutsItOnceThenUploads() {
        settings.server = null
        api.serverConsent = false
        seed("t1", 50)
        val r = run()
        assertEquals(TrackUploader.Outcome.DONE, r.outcome)
        assertEquals(listOf(true), api.consentPuts)
        assertEquals(1, r.uploaded)
    }

    @Test
    fun pendingRemoteDeletesAreDeliveredAndKeptOnFailure() {
        seed("a", 10)
        seed("b", 10)
        repo.deleteTrack("a", queueRemoteDelete = true)
        repo.deleteTrack("b", queueRemoteDelete = true)
        api.failures += "delete" to IOException("offline")
        assertEquals(TrackUploader.Outcome.RETRY_LATER, run().outcome)
        assertEquals(listOf("a", "b"), repo.pendingRemoteDeletes())

        val r = run()
        assertEquals(2, r.remoteDeleted)
        assertTrue(repo.pendingRemoteDeletes().isEmpty())
        assertEquals(listOf("a", "b"), api.deleted)
    }

    @Test
    fun grownTrackIsReplacedNotConflicted() {
        seed("t1", 100)
        run()
        val oldChunk = api.chunks.getValue("t1" to 0).sha256
        // Тот же сеанс продолжился после паузы: добавились точки.
        for (i in 100 until 150) {
            repo.append("t1", "train-1", TrackPoint(now - 3_600_000L + i * 5_000L, 56.0, 38.0, 30.0, 5f))
        }
        val r = run()
        assertEquals(1, r.uploaded)
        assertTrue("t1" in api.deleted)
        assertEquals(150L, repo.track("t1")!!.uploadedPoints)
        assertTrue(api.chunks.getValue("t1" to 0).sha256 != oldChunk)
    }

    @Test
    fun dirtyFavoriteIsDeliveredAfterUpload() {
        seed("t1", 50)
        run()
        repo.setFavorite("t1", true)
        run()
        assertEquals(listOf("t1" to TrackPatchRequest(isFavorite = true)), api.patches)
        assertFalse(repo.track("t1")!!.favoriteDirty)
        api.patches.clear()
        run()
        assertTrue(api.patches.isEmpty())
    }

    @Test
    fun favoriteTrackGetsStarOnFirstUpload() {
        seed("t1", 50)
        repo.setFavorite("t1", true)
        run()
        assertEquals(1, api.patches.size)
        assertFalse(repo.track("t1")!!.favoriteDirty)
    }

    @Test
    fun serverWithoutConsentTriggersConsentResyncNotRejection() {
        seed("t1", 50)
        api.serverConsent = false // сервер «забыл» согласие
        val r = run()
        assertEquals(TrackUploader.Outcome.RETRY_LATER, r.outcome)
        assertEquals(null, settings.server)
        assertEquals(TrackUploadState.PENDING, repo.track("t1")!!.uploadState)
        assertEquals(TrackUploader.Outcome.DONE, run().outcome)
        assertEquals(TrackUploadState.UPLOADED, repo.track("t1")!!.uploadState)
    }

    @Test
    fun quotaStopsTheRunAndRejectsTrack() {
        seed("t1", 50)
        seed("t2", 50)
        api.failures += "putTrack" to TracksApiException(413, "quota_exceeded")
        val r = run()
        assertEquals(TrackUploader.Outcome.QUOTA_EXCEEDED, r.outcome)
        assertEquals(1, repo.tracks().count { it.uploadState == TrackUploadState.REJECTED })
        assertEquals(1, api.calls.count { it == "putTrack" })
    }

    @Test
    fun unauthorizedIsReported() {
        seed("t1", 50)
        api.failures += "putTrack" to TracksApiException(401, "unauthorized")
        assertEquals(TrackUploader.Outcome.UNAUTHORIZED, run().outcome)
    }

    @Test
    fun orphanTrainIsNotUploaded() {
        seed("t1", 50)
        resolution = TrackTrainResolution.Missing
        val r = run()
        assertEquals(0, r.uploaded)
        assertFalse(api.calls.contains("putTrack"))
    }

    @Test
    fun titleAndStatsComeFromTrack() {
        seed("t1", 3000)
        run()
        val meta = api.tracks.getValue("t1")
        val stats = TrackPacking.stats(repo.points("t1"))
        assertEquals(stats.distanceKm, meta.distanceKm, 1e-9)
        assertEquals(repo.points("t1").first().timeMs, meta.startedAt)
        assertEquals(repo.points("t1").last().timeMs, meta.endedAt)
    }

    @Test
    fun errorCodeParsedFromServerBody() {
        assertEquals("route_not_synced", KtorTracksApi.parseErrorCode("""{"detail":{"code":"route_not_synced"}}"""))
        assertEquals(null, KtorTracksApi.parseErrorCode("""{"detail":"Not found"}"""))
        assertEquals(null, KtorTracksApi.parseErrorCode(""))
    }
}
