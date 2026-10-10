package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrackSessionResumeTest {
    private val min = 60_000L
    private var seq = 0
    private fun writer(repo: TrackRepository) = TrackWriter(repo) { "t${++seq}" }
    private fun p(t: Long) = TrackPoint(t, 55.0, 37.0, 60.0, 5f)

    @Test
    fun restartWithOneMinuteGapContinuesSameTrack() {
        val repo = InMemoryTrackRepository()
        val w1 = writer(repo)
        (0L..4L).forEach { w1.onPoint("train", p(it * 5_000), true) }
        // перезапуск процесса: новый писатель, память пуста
        val w2 = writer(repo)
        assertTrue(w2.onPoint("train", p(20_000 + min), true))
        assertEquals(1, repo.tracks().size)
        assertEquals(6L, repo.pointCount("t1"))
    }

    @Test
    fun resumedWriterKeepsThinningFromLastPoint() {
        val repo = InMemoryTrackRepository()
        writer(repo).onPoint("train", p(0), true)
        val w2 = writer(repo)
        assertFalse(w2.onPoint("train", p(2_000), true)) // < 5 с после последней записанной
        assertTrue(w2.onPoint("train", p(6_000), true))
    }

    @Test
    fun resetThenSameTrainContinues() {
        val repo = InMemoryTrackRepository()
        val w = writer(repo)
        w.onPoint("train", p(0), true)
        w.reset()
        w.onPoint("train", p(10 * min), true)
        assertEquals(1, repo.tracks().size)
    }

    @Test
    fun gapOf30MinSameTrainContinuesTrack() {
        val repo = InMemoryTrackRepository()
        writer(repo).onPoint("train", p(0), true)
        writer(repo).onPoint("train", p(30 * min), true)
        assertEquals(1, repo.tracks().size)
    }

    @Test
    fun gapOver12HoursOpensNewTrack() {
        val repo = InMemoryTrackRepository()
        writer(repo).onPoint("train", p(0), true)
        writer(repo).onPoint("train", p(13 * 60 * min), true)
        assertEquals(2, repo.tracks().size)
    }

    @Test
    fun otherTrainOpensNewTrack() {
        val repo = InMemoryTrackRepository()
        writer(repo).onPoint("a", p(0), true)
        writer(repo).onPoint("b", p(min), true)
        assertEquals(2, repo.tracks().size)
    }

    private fun seed(repo: InMemoryTrackRepository, id: String, train: String, from: Long, to: Long) {
        var t = from
        while (t <= to) {
            repo.append(id, train, p(t))
            t += 5_000
        }
    }

    @Test
    fun mergeJoinsAdjacentTracksOfSameTrain() {
        val repo = InMemoryTrackRepository()
        seed(repo, "a", "train", 0, 60_000)
        seed(repo, "b", "train", 60_000 + min, 120_000 + min)
        seed(repo, "c", "train", 13 * 3_600_000L, 13 * 3_600_000L + 60_000) // > 12 ч - отдельный
        seed(repo, "d", "other", 90_000, 100_000)
        repo.setFavorite("b", true)
        repo.setUploadState("a", TrackUploadState.UPLOADED, 13)
        assertEquals(1, TrackMerger.mergeAdjacent(repo))
        val a = repo.track("a")!!
        assertEquals(0L, a.startedAt)
        assertEquals(120_000 + min, a.endedAt)
        assertTrue(a.isFavorite && a.favoriteDirty)
        assertEquals(0L, a.uploadedPoints)
        assertEquals(TrackUploadState.UPLOADED, a.uploadState)
        assertEquals(13L + 13L, repo.pointCount("a"))
        assertEquals(null, repo.track("b"))
        assertEquals(listOf("b"), repo.pendingRemoteDeletes())
        assertEquals(3, repo.tracks().size)
    }

    @Test
    fun mergeIsIdempotent() {
        val repo = InMemoryTrackRepository()
        seed(repo, "a", "train", 0, 60_000)
        seed(repo, "b", "train", 60_000 + min, 120_000 + min)
        TrackMerger.mergeAdjacent(repo)
        val before = repo.tracks()
        assertEquals(0, TrackMerger.mergeAdjacent(repo))
        assertEquals(before, repo.tracks())
    }

    @Test
    fun mergeSkipsLiveTracks() {
        val repo = InMemoryTrackRepository()
        seed(repo, "a", "train", 0, 60_000)
        seed(repo, "b", "train", 60_000 + min, 120_000 + min)
        assertEquals(0, TrackMerger.mergeAdjacent(repo, liveFromMs = 100_000))
        assertEquals(2, repo.tracks().size)
    }
}
