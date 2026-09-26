package com.z_company.route.session

import com.z_company.route.subscription.SubscriptionPeriodTracker
import com.z_company.route.subscription.SubscriptionSeenStore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AccountSwitcherTest {

    private val day = 86_400_000L
    private val now = 1_790_000_000_000L

    /** Устройство после выхода из аккаунта A: его срок и id остались локально. */
    private inner class FakeGateway(
        var serverPeriod: Long? = 0L,
        var serverUserId: String? = "B",
    ) : AccountSwitchGateway {
        var userId = "A"
        var token = ""
        var vkId = ""
        var localPeriod = now + 300 * day
        val events = mutableListOf<String>()
        /** Счётчик трекера в момент каждого шага. */
        val updatesInProgress = mutableListOf<Int>()
        var failOnToken = false

        private fun step(event: String) {
            events += event
            updatesInProgress += tracker.periodUpdatesInProgress.value
        }

        override suspend fun saveUserId(userId: String) { this.userId = userId; step("userId=$userId") }
        override suspend fun saveAuthToken(token: String) {
            if (failOnToken) error("storage failure")
            this.token = token; step("token=$token")
        }
        override suspend fun saveVkId(vkId: String) { this.vkId = vkId; step("vkId=$vkId") }
        override suspend fun fetchSubscriptionPeriod(token: String): Long? = serverPeriod
        override suspend fun setLocalSubscriptionPeriod(period: Long) { localPeriod = period; step("period=$period") }
        override suspend fun fetchUserId(token: String): String? = serverUserId
    }

    private val noStore = object : SubscriptionSeenStore {
        override fun getLastSeenReferralAwardedDays(userId: String) = -1
        override fun setLastSeenReferralAwardedDays(userId: String, value: Int) = Unit
        override fun getLastSeenSubscriptionPeriod(userId: String) = -1L
        override fun setLastSeenSubscriptionPeriod(userId: String, value: Long) = Unit
        override fun getSubscriptionPeriodReferralDays(userId: String) = -1
        override fun setSubscriptionPeriodReferralDays(userId: String, value: Int) = Unit
    }

    private lateinit var gateway: FakeGateway
    private val tracker = SubscriptionPeriodTracker(currentUserId = { gateway.userId }, store = noStore)

    private fun switcher(gw: FakeGateway): AccountSwitcher {
        gateway = gw
        return AccountSwitcher(gw, tracker)
    }

    @Test
    fun `account without subscription does not inherit the previous account's period`() = runTest {
        val gw = FakeGateway(serverPeriod = 0L)

        val loaded = switcher(gw).switchTo("tokenB")

        assertTrue(loaded)
        assertEquals(0L, gw.localPeriod)
        assertEquals("B", gw.userId)
    }

    @Test
    fun `server period replaces a longer local one`() = runTest {
        val gw = FakeGateway(serverPeriod = now + 20 * day)

        switcher(gw).switchTo("tokenB")

        assertEquals(now + 20 * day, gw.localPeriod)
    }

    @Test
    fun `previous userId is cleared before the new token and set only after the new period`() = runTest {
        val gw = FakeGateway(serverPeriod = now + 20 * day)

        switcher(gw).switchTo("tokenB", vkId = "vk42")

        assertEquals(
            listOf("userId=", "token=tokenB", "vkId=vk42", "period=${now + 20 * day}", "userId=B"),
            gw.events,
        )
    }

    @Test
    fun `registration steps run after the period reset and before the new userId`() = runTest {
        val gw = FakeGateway(serverPeriod = 0L)
        val legacyPeriod = now + 50 * day

        switcher(gw).switchTo("tokenB") {
            assertEquals("", gw.userId)
            gw.setLocalSubscriptionPeriod(legacyPeriod)
        }

        assertEquals(legacyPeriod, gw.localPeriod)
        assertEquals("userId=B", gw.events.last())
    }

    @Test
    fun `global notices are paused for the whole switch`() = runTest {
        val gw = FakeGateway()

        switcher(gw).switchTo("tokenB")

        assertTrue(gw.updatesInProgress.all { it == 1 })
        assertEquals(0, tracker.periodUpdatesInProgress.value)
    }

    @Test
    fun `server unavailable resets the local period and reports failure`() = runTest {
        val gw = FakeGateway(serverPeriod = null)

        val loaded = switcher(gw).switchTo("tokenB")

        assertFalse(loaded)
        assertEquals(0L, gw.localPeriod)
    }

    @Test
    fun `profile unavailable leaves userId empty instead of the previous one`() = runTest {
        val gw = FakeGateway(serverUserId = null)

        switcher(gw).switchTo("tokenB")

        assertEquals("", gw.userId)
    }

    @Test
    fun `failure inside the switch releases the tracker`() = runTest {
        val gw = FakeGateway().apply { failOnToken = true }

        assertFailsWith<IllegalStateException> { switcher(gw).switchTo("tokenB") }

        assertEquals(0, tracker.periodUpdatesInProgress.value)
    }
}
