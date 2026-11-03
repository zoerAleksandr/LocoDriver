package com.z_company.route.ui.settings

import com.z_company.route.viewmodel.TrackHistoryItem
import com.z_company.route.viewmodel.TrackHistoryMessages
import com.z_company.route.viewmodel.TrackHistoryStatus
import com.z_company.route.viewmodel.mergeTrackItems
import com.z_company.route.viewmodel.remoteOnlyCards
import com.z_company.route.ui.settings.TrackHistoryActions.Action as Act
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackHistoryFormatTest {
    @Test
    fun durationReadsLikeHumans() {
        assertEquals("3 ч 20 мин", TrackHistoryFormat.duration((3 * 60 + 20) * 60_000L))
        assertEquals("2 ч", TrackHistoryFormat.duration(2 * 3_600_000L))
        assertEquals("45 мин", TrackHistoryFormat.duration(45 * 60_000L))
        assertEquals("1 мин", TrackHistoryFormat.duration(5_000L))
    }

    @Test
    fun distanceKeepsDecimalOnlyForShortTracks() {
        assertEquals("5,4 км", TrackHistoryFormat.distance(5.43))
        assertEquals("132 км", TrackHistoryFormat.distance(132.7))
    }

    @Test
    fun dateUsesDisplayZone() {
        // 2026-10-12 05:15 UTC = 08:15 МСК
        val ms = 1_791_782_100_000L
        assertTrue(TrackHistoryFormat.dateTime(ms, "GMT+3").endsWith("08:15"))
        assertTrue(TrackHistoryFormat.dateTime(ms, "GMT+5").endsWith("10:15"))
    }

    @Test
    fun summaryShowsDistanceAndTravelTimeWithoutSpeed() {
        val text = TrackHistoryFormat.summary(132.7, (3 * 60 + 20) * 60_000L)
        assertEquals("132 км · в пути 3 ч 20 мин", text)
        assertTrue("км/ч" !in text)
        assertEquals("в пути 45 мин", TrackHistoryFormat.summary(null, 45 * 60_000L))
    }

    @Test
    fun workStartHasPrefixAndZone() {
        val ms = 1_791_782_100_000L
        assertEquals("Явка: " + TrackHistoryFormat.dateTime(ms, "GMT+3"), TrackHistoryFormat.workStart(ms, "GMT+3"))
    }

    @Test
    fun serverOnlyLabelIsGoneAndEveryStatusHasDescription() {
        TrackHistoryStatus.entries.forEach {
            val d = TrackHistoryFormat.syncDescription(it, false)
            assertTrue(d.isNotBlank())
            assertTrue("Только на сервере" !in d)
        }
        assertEquals("Синхронизирован", TrackHistoryFormat.syncDescription(TrackHistoryStatus.SERVER_ONLY, false))
        assertEquals("Отправляется на сервер", TrackHistoryFormat.syncDescription(TrackHistoryStatus.PENDING, true))
        assertTrue(TrackHistoryFormat.status(TrackHistoryStatus.REJECTED).isNotBlank())
    }

    private fun item(id: String, status: TrackHistoryStatus, fav: Boolean = false, started: Long = 1L) = TrackHistoryItem(
        id = id, title = "t", startedAt = started, endedAt = started + 1, distanceKm = null, preview = emptyList(),
        isFavorite = fav, status = status,
    )

    @Test
    fun contextMenuOffersSyncOnlyForUnsyncedLocalTracks() {
        assertEquals(listOf(Act.FAVORITE, Act.SYNC, Act.DELETE), TrackHistoryActions.menu(item("1", TrackHistoryStatus.PENDING)))
        assertEquals(listOf(Act.FAVORITE, Act.SYNC, Act.DELETE), TrackHistoryActions.menu(item("1", TrackHistoryStatus.DEVICE_ONLY)))
        assertEquals(listOf(Act.FAVORITE, Act.DELETE), TrackHistoryActions.menu(item("1", TrackHistoryStatus.SENT)))
        assertEquals(listOf(Act.FAVORITE, Act.DELETE), TrackHistoryActions.menu(item("1", TrackHistoryStatus.SERVER_ONLY)))
        assertEquals("Убрать из избранного", TrackHistoryActions.label(Act.FAVORITE, item("1", TrackHistoryStatus.SENT, fav = true)))
        assertEquals("В избранное", TrackHistoryActions.label(Act.FAVORITE, item("1", TrackHistoryStatus.SENT)))
    }

    @Test
    fun syncedFlagFollowsServerPresence() {
        assertTrue(item("1", TrackHistoryStatus.SENT).isSynced)
        assertTrue(item("1", TrackHistoryStatus.SERVER_ONLY).isSynced)
        assertTrue(!item("1", TrackHistoryStatus.PENDING).isSynced)
        assertTrue(!item("1", TrackHistoryStatus.REJECTED).isSynced)
    }

    @Test
    fun sameTrackOnDeviceAndServerIsOneCard() {
        val local = listOf(item("a", TrackHistoryStatus.SENT, started = 10), item("b", TrackHistoryStatus.PENDING, started = 30))
        val remote = listOf(item("a", TrackHistoryStatus.SERVER_ONLY, started = 10), item("c", TrackHistoryStatus.SERVER_ONLY, started = 20))
        val merged = mergeTrackItems(local, remote)
        assertEquals(listOf("b", "c", "a"), merged.map { it.id })
        assertEquals(TrackHistoryStatus.SENT, merged.first { it.id == "a" }.status)
    }

    @Test
    fun remoteCardsSkipLocalAndPendingDeleteIds() {
        val cards = listOf("a", "b", "c", "d")
        assertEquals(listOf("c"), remoteOnlyCards(cards, { it }, setOf("a"), setOf("b", "d")))
    }

    @Test
    fun favoriteMessages() {
        assertEquals("Трек добавлен в избранное", TrackHistoryMessages.favorite(true, 1))
        assertEquals("Трек убран из избранного", TrackHistoryMessages.favorite(false, 1))
        assertEquals("Треки добавлены в избранное", TrackHistoryMessages.favorite(true, 3))
        assertEquals("Треки убраны из избранного", TrackHistoryMessages.favorite(false, 3))
        assertEquals("Ничего не изменилось", TrackHistoryMessages.favorite(true, 0))
    }
}
