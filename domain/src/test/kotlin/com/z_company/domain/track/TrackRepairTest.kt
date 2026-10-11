package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals

class TrackRepairTest {
    private val h = 3_600_000L
    private val day = 24 * h
    private val t0 = 1_760_000_000_000L // 10.10.2025-ish, произвольная опора

    private fun pt(t: Long, i: Int) = TrackPoint(t, 59.0 + i * 0.001, 32.0, 60.0, 5f)

    /** Регрессия: две части поезда склеены, а одна точка со спуфинговой меткой времени (+396 суток) раздувала «время в пути» до 9522 ч. */
    @Test
    fun mergedTrackWithSpoofedTimestampIsRepaired() {
        val repo = InMemoryTrackRepository()
        for (i in 0 until 10) repo.append("a", "train", pt(t0 + i * 20 * 60_000L, i)) // 14:59..18:33
        for (i in 0 until 10) repo.append("b", "train", pt(t0 + 3 * h + 35 * 60_000L + i * 20 * 60_000L, 10 + i))
        repo.append("b", "train", pt(t0 + 396 * day + 22 * h + 56 * 60_000L, 99)) // ложная метка
        TrackMerger.mergeAdjacent(repo)
        assertEquals(396 * day, repo.track("a")!!.endedAt - t0 - 22 * h - 56 * 60_000L) // испорчено до починки
        assertEquals(1, TrackRepair.fixBounds(repo))
        val info = repo.track("a")!!
        val points = repo.points("a")
        assertEquals(20, points.size)
        assertEquals(points.first().timeMs, info.startedAt)
        assertEquals(points.last().timeMs, info.endedAt)
        assertEquals(0L, info.uploadedPoints)
        assertEquals(true, info.endedAt - info.startedAt < 8 * h)
        assertEquals(0, TrackRepair.fixBounds(repo)) // идемпотентно
    }

    @Test
    fun healthyTrackIsUntouched() {
        val repo = InMemoryTrackRepository()
        for (i in 0 until 5) repo.append("a", "train", pt(t0 + i * 60_000L, i))
        repo.setUploadState("a", TrackUploadState.UPLOADED, 5)
        assertEquals(0, TrackRepair.fixBounds(repo))
        assertEquals(5L, repo.track("a")!!.uploadedPoints)
    }

    @Test
    fun staleBoundsOfCleanPointsAreRecomputed() {
        val repo = InMemoryTrackRepository()
        for (i in 0 until 5) repo.append("a", "train", pt(t0 + i * 60_000L, i))
        repo.trimTrack("a", t0 - 50 * day, t0 + 50 * day) // имитация испорченных границ при честных точках
        assertEquals(1, TrackRepair.fixBounds(repo))
        assertEquals(t0, repo.track("a")!!.startedAt)
        assertEquals(t0 + 4 * 60_000L, repo.track("a")!!.endedAt)
    }

    @Test
    fun statsIgnoreSpoofedTimestampAfterSanitize() {
        val pts = (0 until 10).map { pt(t0 + it * 60_000L, it) } + pt(t0 + 396 * day, 50)
        val clean = com.z_company.domain.util.GnssPlausibility.sanitize(pts)
        assertEquals(9 * 60_000L, TrackPacking.stats(clean).durationMs)
    }
}
