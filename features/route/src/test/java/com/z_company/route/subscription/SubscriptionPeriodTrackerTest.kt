package com.z_company.route.subscription

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SubscriptionPeriodTrackerTest {

    private class FakeStore : SubscriptionSeenStore {
        val period = mutableMapOf<String, Long>()
        val periodReferral = mutableMapOf<String, Int>()

        override fun getLastSeenReferralAwardedDays(userId: String) = -1
        override fun setLastSeenReferralAwardedDays(userId: String, value: Int) = Unit
        override fun getLastSeenSubscriptionPeriod(userId: String) = period[userId] ?: -1L
        override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) { period[userId] = value }
        override fun getSubscriptionPeriodReferralDays(userId: String) = periodReferral[userId] ?: -1
        override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) { periodReferral[userId] = value }
    }

    private val store = FakeStore()
    private var userId: String? = "A"
    private val tracker = SubscriptionPeriodTracker(currentUserId = { userId }, store = store)

    @Test
    fun `first finished update does not resume checks while another is running`() = runTest {
        val finishFirst = CompletableDeferred<Unit>()
        val finishSecond = CompletableDeferred<Unit>()
        launch { tracker.runPeriodUpdate { finishFirst.await() } }
        launch { tracker.runPeriodUpdate { finishSecond.await() } }
        runCurrent()
        assertEquals(2, tracker.periodUpdatesInProgress.value)

        finishFirst.complete(Unit)
        runCurrent()
        assertEquals(1, tracker.periodUpdatesInProgress.value)

        finishSecond.complete(Unit)
        runCurrent()
        assertEquals(0, tracker.periodUpdatesInProgress.value)
    }

    @Test
    fun `acknowledge marks the period seen for the current account`() = runTest {
        store.periodReferral["A"] = 5

        tracker.acknowledge(1_800_000_000_000L)

        assertEquals(1_800_000_000_000L, store.period["A"])
        assertEquals(-1, store.periodReferral["A"])
    }

    @Test
    fun `acknowledge without a known account writes nothing`() = runTest {
        userId = ""

        tracker.acknowledge(1_800_000_000_000L)

        assertTrue(store.period.isEmpty())
    }
}
