package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackWriterTest {

    private class FakeRepo(base: com.z_company.domain.track.InMemoryTrackRepository = com.z_company.domain.track.InMemoryTrackRepository()) : TrackRepository by base {
        val appended = mutableListOf<Triple<String, String, TrackPoint>>()
        var deletedAll = 0
        override fun append(trackId: String, trainId: String, point: TrackPoint) {
            appended += Triple(trackId, trainId, point)
        }
        override fun points(trackId: String) = appended.filter { it.first == trackId }.map { it.third }
        override fun pointCount(trackId: String) = points(trackId).size.toLong()
        override fun tracks() = emptyList<TrackInfo>()
        override fun tracksForTrain(trainId: String) = emptyList<TrackInfo>()
        override fun setFavorite(trackId: String, favorite: Boolean) {}
        override fun deleteByTrain(trainId: String) {}
        override fun deleteAll() { deletedAll++ }
        override fun purgeExpired(nowMs: Long) = 0
    }

    private fun p(t: Long, speed: Double?) = TrackPoint(t, 55.0, 37.0, speed, 5f)

    @Test
    fun movingKeepsPointEveryFiveSeconds() {
        val thinner = TrackThinner()
        // фиксы раз в секунду, 20 секунд в движении
        val kept = (0L..20L).count { thinner.accept(p(it * 1000, 60.0)) }
        assertEquals(5, kept) // 0, 5, 10, 15, 20 с
    }

    @Test
    fun heldPointsAreThinnedAsStandEvenWithClaimedSpeed() {
        val thinner = TrackThinner()
        // якорь стоянки (DriftFilter) при заявленной скорости 5 км/ч не пишется раз в 5 с
        val kept = (0L..120L).count { thinner.accept(p(it * 1000, 5.0), held = true) }
        assertEquals(3, kept) // 0, 60, 120 с
        // подтверждённое движение (held=false) принимается сразу
        assertTrue(thinner.accept(p(121_000, 5.0), held = false))
    }

    @Test
    fun stoppedKeepsPointEveryMinute() {
        val thinner = TrackThinner()
        val kept = (0L..180L).count { thinner.accept(p(it * 1000, 0.0)) }
        assertEquals(4, kept) // 0, 60, 120, 180 с
    }

    @Test
    fun departureAfterLongStopIsKeptImmediately() {
        val thinner = TrackThinner()
        assertTrue(thinner.accept(p(0, 0.0)))
        assertFalse(thinner.accept(p(30_000, 0.0)))
        // тронулись через 30 с после последней принятой: в движении интервал 5 с
        assertTrue(thinner.accept(p(31_000, 10.0)))
    }

    @Test
    fun unknownSpeedCountsAsMoving() {
        val thinner = TrackThinner()
        assertTrue(thinner.accept(p(0, null)))
        assertTrue(thinner.accept(p(5_000, null)))
    }

    @Test
    fun noConsentWritesNothing() {
        val repo = FakeRepo()
        val w = TrackWriter(repo) { "t1" }
        assertFalse(w.onPoint("train", p(0, 50.0), consent = false))
        assertTrue(repo.appended.isEmpty())
    }

    @Test
    fun noTargetTrainWritesNothing() {
        val repo = FakeRepo()
        val w = TrackWriter(repo) { "t1" }
        assertFalse(w.onPoint(null, p(0, 50.0), consent = true))
        assertTrue(repo.appended.isEmpty())
    }

    @Test
    fun trackIsBoundToTargetTrainAndThinned() {
        val repo = FakeRepo()
        val w = TrackWriter(repo) { "t1" }
        (0L..10L).forEach { w.onPoint("train-A", p(it * 1000, 50.0), consent = true) }
        assertEquals(3, repo.appended.size) // 0, 5, 10 с
        assertTrue(repo.appended.all { it.first == "t1" && it.second == "train-A" })
    }

    @Test
    fun switchingTrainOrResetStartsNewTrack() {
        val repo = FakeRepo()
        var n = 0
        val w = TrackWriter(repo) { "t${++n}" }
        w.onPoint("A", p(0, 50.0), true)
        w.onPoint("B", p(1_000, 50.0), true)
        w.reset()
        w.onPoint("B", p(2_000, 50.0), true)
        assertEquals(listOf("t1", "t2", "t3"), repo.appended.map { it.first })
    }

    @Test
    fun consentRevokedMidwayStopsWriting() {
        val repo = FakeRepo()
        val w = TrackWriter(repo) { "t1" }
        assertTrue(w.onPoint("A", p(0, 50.0), true))
        assertFalse(w.onPoint("A", p(10_000, 50.0), false))
        assertEquals(1, repo.appended.size)
    }

    @Test
    fun retentionIs180DaysAndFavoritesAreKept() {
        val now = 1_000L * 24 * 3600 * 1000
        val day = 24L * 3600 * 1000
        assertEquals(180, TrackRetention.DAYS)
        assertTrue(TrackRetention.isExpired(now - 181 * day, isFavorite = false, nowMs = now))
        assertFalse(TrackRetention.isExpired(now - 179 * day, isFavorite = false, nowMs = now))
        assertFalse(TrackRetention.isExpired(now - 400 * day, isFavorite = true, nowMs = now))
    }

    @Test
    fun stopPointNotWrittenWithoutSignalButMovingPointIs() {
        val repo = FakeRepo()
        val w = TrackWriter(repo, newTrackId = { "t1" })
        assertFalse(w.onPoint("tr", p(0, 0.0), consent = true, signalOk = false)) // устаревший fix "стоим"
        assertFalse(w.onPoint("tr", p(1_000, null), consent = true, signalOk = false))
        assertTrue(w.onPoint("tr", p(2_000, 40.0), consent = true, signalOk = false))
        assertEquals(1, repo.appended.size)
    }

    @Test
    fun rejectedStopPointDoesNotShiftThinner() {
        val repo = FakeRepo()
        val w = TrackWriter(repo, newTrackId = { "t1" })
        assertTrue(w.onPoint("tr", p(0, 0.0), consent = true))
        assertFalse(w.onPoint("tr", p(59_000, 0.0), consent = true, signalOk = false))
        assertTrue(w.onPoint("tr", p(60_000, 0.0), consent = true))
    }
}
