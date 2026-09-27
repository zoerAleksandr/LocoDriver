package com.z_company.route.subscription

import com.z_company.route.viewmodel.SubscriptionPeriodChange
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class SubscriptionNoticesPolicyTest {

    private val day = 86_400_000L
    private val now = 1_790_000_000_000L

    /** Та же семантика, что у SharedPreferences: -1 — «не отслеживали». */
    private class FakeStore : SubscriptionSeenStore {
        val awarded = mutableMapOf<String, Int>()
        val period = mutableMapOf<String, Long>()
        val periodReferral = mutableMapOf<String, Int>()

        override fun getLastSeenReferralAwardedDays(userId: String) = awarded[userId] ?: -1
        override fun setLastSeenReferralAwardedDays(userId: String, value: Int) { awarded[userId] = value }
        override fun getLastSeenSubscriptionPeriod(userId: String) = period[userId] ?: -1L
        override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) { period[userId] = value }
        override fun getSubscriptionPeriodReferralDays(userId: String) = periodReferral[userId] ?: -1
        override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) { periodReferral[userId] = value }
    }

    private val store = FakeStore()
    private val policy = SubscriptionNoticesPolicy(store) { now }

    @Test
    fun `switch to another account records its base under its own key without dialogs`() {
        policy.check("A", period = now + 10 * day, awardedDays = 0)

        // Вход в B: срок и бонус больше, чем «увиденные» у A.
        val notices = policy.check("B", period = now + 100 * day, awardedDays = 30)

        assertNull(notices.bonusDays)
        assertNull(notices.periodChange)
        assertEquals(now + 100 * day, store.period["B"])
        assertEquals(30, store.awarded["B"])
        assertEquals(30, store.periodReferral["B"])
        // «Увиденное» A не тронуто.
        assertEquals(now + 10 * day, store.period["A"])
        assertEquals(0, store.awarded["A"])
    }

    @Test
    fun `extension of the same account is reported`() {
        policy.check("A", period = now + 10 * day, awardedDays = 0)

        val notices = policy.check("A", period = now + 40 * day, awardedDays = 0)

        assertEquals(SubscriptionPeriodChange(now + 10 * day, now + 40 * day), notices.periodChange)
        assertNull(notices.bonusDays)
    }

    @Test
    fun `growth explained by referral bonus shows only the bonus dialog`() {
        policy.check("A", period = now + 10 * day, awardedDays = 0)

        val notices = policy.check("A", period = now + 40 * day, awardedDays = 30)

        assertEquals(30, notices.bonusDays)
        assertNull(notices.periodChange)
    }

    @Test
    fun `growth to an already expired date is not an extension`() {
        policy.check("A", period = now - 40 * day, awardedDays = 0)

        val notices = policy.check("A", period = now - 5 * day, awardedDays = 0)

        assertNull(notices.periodChange)
        assertEquals(now - 5 * day, store.period["A"])
    }
}
