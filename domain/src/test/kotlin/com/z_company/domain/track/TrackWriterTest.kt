package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackWriterTest {

    private class FakeRepo : TrackRepository {
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
}
