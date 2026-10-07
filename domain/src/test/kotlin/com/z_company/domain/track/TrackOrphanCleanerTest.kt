package com.z_company.domain.track

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TrackOrphanCleanerTest {
    private val now = 1_800_000_000_000L
    private val day = 24 * 60 * 60_000L

    private fun repoWith(vararg specs: Triple<String, String, Long>): InMemoryTrackRepository {
        val repo = InMemoryTrackRepository()
        specs.forEach { (trackId, trainId, endedAt) ->
            repo.append(trackId, trainId, TrackPoint(endedAt - 1000, 55.0, 37.0, 10.0, 5f))
            repo.append(trackId, trainId, TrackPoint(endedAt, 55.0, 37.0, 10.0, 5f))
        }
        return repo
    }

    @Test
    fun removesTracksOfTrainsThatNoLongerExistAndQueuesRemoteDelete() {
        val repo = repoWith(
            Triple("alive", "train-1", now - 3 * day),
            Triple("orphan", "train-gone", now - 3 * day),
        )
        val removed = TrackOrphanCleaner.clean(repo, setOf("train-1"), now)
        assertEquals(1, removed)
        assertNotNull(repo.track("alive"))
        assertNull(repo.track("orphan"))
        assertEquals(listOf("orphan"), repo.pendingRemoteDeletes())
        assertEquals(0L, repo.pointCount("orphan"))
    }

    @Test
    fun keepsFreshTracksBecauseTheTrainMayNotBeSavedYet() {
        val repo = repoWith(Triple("fresh", "train-new", now - 60_000))
        assertEquals(0, TrackOrphanCleaner.clean(repo, setOf("train-1"), now))
        assertNotNull(repo.track("fresh"))
    }

    @Test
    fun emptyTrainSetMeansUnreadableRoutesAndDeletesNothing() {
        val repo = repoWith(Triple("a", "train-1", now - 3 * day))
        assertEquals(0, TrackOrphanCleaner.clean(repo, emptySet(), now))
        assertNotNull(repo.track("a"))
    }

    @Test
    fun favoriteOrphanIsAlsoRemovedBecauseTrackDiesWithItsRoute() {
        val repo = repoWith(Triple("fav", "train-gone", now - 3 * day))
        repo.setFavorite("fav", true)
        assertEquals(1, TrackOrphanCleaner.clean(repo, setOf("train-1"), now))
    }
}
