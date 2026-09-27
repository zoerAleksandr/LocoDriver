package com.z_company.route.subscription

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PaymentReturnCheckerTest {

    private class FakePendingStore : PendingPaymentStore {
        var value: String? = null
        override fun get() = value
        override fun set(value: String?) { this.value = value }
    }

    private class FakeSeenStore : SubscriptionSeenStore {
        val period = mutableMapOf<String, Long>()
        override fun getLastSeenReferralAwardedDays(userId: String) = -1
        override fun setLastSeenReferralAwardedDays(userId: String, value: Int) = Unit
        override fun getLastSeenSubscriptionPeriod(userId: String) = period[userId] ?: -1L
        override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) { period[userId] = value }
        override fun getSubscriptionPeriodReferralDays(userId: String) = -1
        override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) = Unit
    }

    private val day = PaymentReturnChecker.DAY_MS
    private val start = 1_800_000_000_000L
    private val pendingStore = FakePendingStore()
    private val seenStore = FakeSeenStore()
    private var userId: String? = "A"
    private var serverPeriod: Long? = 0L
    private var refreshCalls = 0

    private fun TestScope.checker(): PaymentReturnChecker {
        val tracker = SubscriptionPeriodTracker(currentUserId = { userId }, store = seenStore)
        return PaymentReturnChecker(
            store = pendingStore,
            currentUserId = { userId },
            refreshPeriodFromServer = { refreshCalls++; serverPeriod },
            tracker = tracker,
            scope = this,
            now = { start + testScheduler.currentTime },
        )
    }

    @Test
    fun `free user paid, app was killed in the bank - dialog on next start`() = runTest {
        checker().markStarted(userId = "A", periodBefore = 0L, periodDays = 31)
        serverPeriod = start + 31 * day

        // Новый экземпляр — как после выгрузки процесса: запись читается с диска.
        val restarted = checker()
        assertTrue(restarted.hasPending.value)
        restarted.onAppResumed()
        advanceUntilIdle()

        assertEquals(start + 31 * day, restarted.paidUntil.value)
        assertFalse(restarted.hasPending.value)
        assertNull(pendingStore.value)
        assertEquals(start + 31 * day, seenStore.period["A"]) // «Подписка продлена» не дублирует
    }

    @Test
    fun `payment not confirmed - no dialog, pending kept for next return`() = runTest {
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = 0L, periodDays = 31)

        val result = checker.checkNow(attempts = 5)

        assertEquals(PaymentReturnChecker.Result.NOT_CONFIRMED, result)
        assertNull(checker.paidUntil.value)
        assertTrue(checker.hasPending.value)
        assertEquals(5, refreshCalls)
    }

    @Test
    fun `referral bonus alone is not a payment`() = runTest {
        val before = start + 10 * day
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = before, periodDays = 31)
        serverPeriod = before + 7 * day

        assertEquals(PaymentReturnChecker.Result.NOT_CONFIRMED, checker.checkNow(attempts = 1))
        assertNull(checker.paidUntil.value)
    }

    @Test
    fun `renewal of active subscription counts from previous end`() = runTest {
        val before = start + 10 * day
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = before, periodDays = 31)
        serverPeriod = before + 31 * day

        assertEquals(PaymentReturnChecker.Result.PAID, checker.checkNow(attempts = 1))
    }

    @Test
    fun `another account - pending dropped without dialog`() = runTest {
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = 0L, periodDays = 31)
        userId = "B"
        serverPeriod = start + 31 * day

        assertEquals(PaymentReturnChecker.Result.NO_PENDING, checker.checkNow(attempts = 1))
        assertNull(checker.paidUntil.value)
        assertNull(pendingStore.value)
    }

    @Test
    fun `expired pending is dropped`() = runTest {
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = 0L, periodDays = 31)
        testScheduler.advanceTimeBy(PaymentReturnChecker.TTL_MS + 1)
        serverPeriod = start + 40 * day

        assertEquals(PaymentReturnChecker.Result.NO_PENDING, checker.checkNow(attempts = 1))
        assertNull(pendingStore.value)
    }

    @Test
    fun `screen check joins resume check - one polling`() = runTest {
        val checker = checker()
        checker.markStarted(userId = "A", periodBefore = 0L, periodDays = 31)

        checker.onAppResumed()
        val fromScreen = async { checker.checkNow(attempts = 10) }
        advanceUntilIdle()

        assertEquals(PaymentReturnChecker.Result.NOT_CONFIRMED, fromScreen.await())
        assertEquals(PaymentReturnChecker.FRESH_ATTEMPTS, refreshCalls)
    }
}
