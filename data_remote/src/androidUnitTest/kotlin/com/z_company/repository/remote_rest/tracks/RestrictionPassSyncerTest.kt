package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.InMemoryRestrictionJournalStore
import com.z_company.domain.track.RestrictionKind
import com.z_company.domain.track.RestrictionPass
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class RpCursor : RestrictionSyncCursor {
    var v = 0L
    override fun lastPulledMs() = v
    override fun setLastPulledMs(value: Long) { v = value }
}

private class RpApi : RestrictionPassApi {
    val server = LinkedHashMap<Pair<String, String>, RestrictionPass>()
    var offline = false
    var rejectRoutes = emptySet<String>()
    var puts = 0
    var lastSince: Long? = null
    var failWith: RestrictionPassApiException? = null

    override suspend fun put(token: String, passes: List<RestrictionPass>): RestrictionPassBatchResponse {
        if (offline) throw IOException("no network")
        failWith?.let { throw it }
        puts++
        var ok = 0
        val skipped = mutableListOf<RestrictionPassSkipped>()
        for (p in passes) {
            if (p.routeId in rejectRoutes) { skipped += RestrictionPassSkipped(p.trainId, p.warningId, "route_unavailable"); continue }
            val cur = server[p.trainId to p.warningId]
            if (cur != null && cur.updatedAt >= p.updatedAt) { skipped += RestrictionPassSkipped(p.trainId, p.warningId, "stale"); continue }
            server[p.trainId to p.warningId] = p
            ok++
        }
        return RestrictionPassBatchResponse(ok, skipped)
    }

    override suspend fun list(token: String, cursor: String?, sinceMs: Long?, limit: Int): RestrictionPassListResponse {
        if (offline) throw IOException("no network")
        lastSince = sinceMs
        val all = server.values.filter { sinceMs == null || it.updatedAt >= sinceMs }.sortedBy { it.updatedAt }
        val from = cursor?.toInt() ?: 0
        val page = all.drop(from).take(limit)
        val next = if (from + limit < all.size) (from + limit).toString() else null
        return RestrictionPassListResponse(page, next)
    }
}

class RestrictionPassSyncerTest {
    private fun pass(w: String, at: Long = 100, route: String = "r1", train: String = "t1") = RestrictionPass(
        trainId = train, warningId = w, routeId = route, sectionId = "s", kind = RestrictionKind.SPEED, title = "огр", limitKmh = 40, updatedAt = at,
    )

    private fun syncer(api: RpApi, store: InMemoryRestrictionJournalStore, cursor: RpCursor = RpCursor(), sub: Boolean = true) =
        RestrictionPassSyncer(api, store, cursor, isSubscribed = { sub })

    @Test
    fun push_then_pull_merges_and_marks_uploaded() = runBlocking {
        val api = RpApi()
        val store = InMemoryRestrictionJournalStore()
        store.upsert(pass("a", 100))
        api.server["t2" to "z"] = pass("z", 300, train = "t2")
        val r = syncer(api, store).run("Bearer x")
        assertEquals(RestrictionPassSyncer.Outcome.DONE, r.outcome)
        assertEquals(1, r.sent)
        assertEquals(0, store.pendingUpload(10).size)
        assertTrue(store.get("t2", "z") != null)
        assertTrue(api.server.containsKey("t1" to "a"))
    }

    @Test
    fun consent_is_not_required_everything_is_sent() = runBlocking {
        val api = RpApi()
        val store = InMemoryRestrictionJournalStore()
        store.upsert(pass("a", 100))
        store.upsert(pass("b", 100))
        store.remove("t1", "b", 200)
        val r = syncer(api, store).run("Bearer x")
        assertEquals(RestrictionPassSyncer.Outcome.DONE, r.outcome)
        assertEquals(setOf("t1" to "a", "t1" to "b"), api.server.keys)
        assertEquals(0, store.pendingUpload(10).size)
    }

    @Test
    fun no_subscription_sends_nothing() = runBlocking {
        val api = RpApi()
        val store = InMemoryRestrictionJournalStore().also { it.upsert(pass("a")) }
        assertEquals(RestrictionPassSyncer.Outcome.NO_SUBSCRIPTION, syncer(api, store, sub = false).run("Bearer x").outcome)
        assertEquals(0, api.puts)
        assertEquals(1, store.pendingUpload(10).size)
    }

    @Test
    fun route_not_synced_yet_keeps_record_dirty_and_asks_retry() = runBlocking {
        val api = RpApi().also { it.rejectRoutes = setOf("r-new") }
        val store = InMemoryRestrictionJournalStore()
        val fresh = System.currentTimeMillis()
        store.upsert(pass("a", at = fresh, route = "r-new"))
        store.upsert(pass("b", at = fresh, route = "r1"))
        val r = syncer(api, store).run("Bearer x")
        assertEquals(RestrictionPassSyncer.Outcome.RETRY_LATER, r.outcome)
        assertEquals(listOf("a"), store.pendingUpload(10).map { it.warningId })
        // рейс доехал - уходит
        api.rejectRoutes = emptySet()
        assertEquals(RestrictionPassSyncer.Outcome.DONE, syncer(api, store).run("Bearer x").outcome)
        assertEquals(0, store.pendingUpload(10).size)
    }

    @Test
    fun network_and_http_errors_map_to_outcomes_and_keep_local_data() = runBlocking {
        val api = RpApi()
        val store = InMemoryRestrictionJournalStore().also { it.upsert(pass("a")) }
        api.offline = true
        assertEquals(RestrictionPassSyncer.Outcome.RETRY_LATER, syncer(api, store).run("Bearer x").outcome)
        api.offline = false
        api.failWith = RestrictionPassApiException(401, null)
        assertEquals(RestrictionPassSyncer.Outcome.UNAUTHORIZED, syncer(api, store).run("Bearer x").outcome)
        api.failWith = RestrictionPassApiException(429, "rate_limited")
        assertEquals(RestrictionPassSyncer.Outcome.RETRY_LATER, syncer(api, store).run("Bearer x").outcome)
        assertEquals(1, store.pendingUpload(10).size)
    }

    @Test
    fun pull_uses_cursor_with_one_day_margin_and_paginates() = runBlocking {
        val api = RpApi()
        val day = 24L * 3600_000L
        repeat(230) { api.server["t9" to "w$it"] = pass("w$it", at = 10 * day + it, train = "t9") }
        val cursor = RpCursor().also { it.v = 5 * day }
        val store = InMemoryRestrictionJournalStore()
        val r = syncer(api, store, cursor = cursor).run("Bearer x")
        assertEquals(230, r.pulled)
        assertEquals(4 * day, api.lastSince)
        assertEquals(10 * day + 229, cursor.v)
    }

    @Test
    fun older_server_version_does_not_overwrite_newer_local() = runBlocking {
        val api = RpApi()
        api.server["t1" to "a"] = pass("a", 100)
        val store = InMemoryRestrictionJournalStore()
        store.upsert(pass("a", 500).copy(maxSpeedKmh = 47.0))
        syncer(api, store).run("Bearer x")
        assertEquals(47.0, store.get("t1", "a")!!.maxSpeedKmh)
        assertEquals(500L, api.server["t1" to "a"]!!.updatedAt)
    }

    @Test
    fun stale_unroutable_record_is_dropped_after_two_weeks() = runBlocking {
        val api = RpApi().also { it.rejectRoutes = setOf("r-other") }
        val store = InMemoryRestrictionJournalStore()
        val now = 100L * 24 * 3600_000
        store.upsert(pass("old", at = now - 15L * 24 * 3600_000, route = "r-other"))
        val r = RestrictionPassSyncer(api, store, RpCursor(), { true }, { now }).run("Bearer x")
        assertEquals(RestrictionPassSyncer.Outcome.DONE, r.outcome)
        assertEquals(0, store.pendingUpload(10).size)
    }
}
