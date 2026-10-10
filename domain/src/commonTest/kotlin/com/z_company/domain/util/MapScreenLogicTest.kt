package com.z_company.domain.util

import com.z_company.domain.track.TrackPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapScreenLogicTest {
    // ── Маска времени ──
    @Test fun maskInsertsColonAfterTwoHourDigits() {
        assertEquals("", TimeMask.format(""))
        assertEquals("1", TimeMask.format("1"))
        assertEquals("18:", TimeMask.format("18", ""))
        assertEquals("18:3", TimeMask.format("183"))
        assertEquals("18:30", TimeMask.format("1830"))
        assertEquals("09:05", TimeMask.format("0905"))
    }

    @Test fun maskTreatsFirstDigitAbove2AsSingleDigitHour() {
        assertEquals("9:", TimeMask.format("9"))
        assertEquals("9:05", TimeMask.format("905"))
        assertEquals("9:05", TimeMask.format("0905".drop(1)))
    }

    @Test fun maskKeepsOnlyDigitsAndCapsLength() {
        assertEquals("12:34", TimeMask.format("12:34"))
        assertEquals("12:34", TimeMask.format("12a3b4c5"))
        assertEquals("12:34", TimeMask.format("123456"))
    }

    @Test fun maskDoesNotReAddColonWhileDeleting() {
        assertEquals("18", TimeMask.format("18", previous = "18:"))
        assertEquals("1", TimeMask.format("1", previous = "18"))
    }

    @Test fun maskedTextParsesAsBefore() {
        assertEquals(9 to 5, ArrivalPlan.parseTime(TimeMask.format("905")))
        assertEquals(9 to 5, ArrivalPlan.parseTime(TimeMask.format("0905")))
        assertEquals(18 to 30, ArrivalPlan.parseTime(TimeMask.format("1830")))
    }

    // ── Возврат в слежение ──
    @Test fun followReturnsOnlyAfterTenSecondsWithoutTouch() {
        val t0 = 1_000_000L
        assertFalse(FollowReturn.shouldReturn(false, false, false, false, t0 + 9_999, t0))
        assertTrue(FollowReturn.shouldReturn(false, false, false, false, t0 + 10_000, t0))
    }

    @Test fun followDoesNotReturnWhenBlockedFingerDownViewOnlyOrAlreadyFollowing() {
        val t0 = 0L
        val late = 60_000L
        assertFalse(FollowReturn.shouldReturn(false, false, true, false, late, t0))
        assertFalse(FollowReturn.shouldReturn(false, false, false, true, late, t0))
        assertFalse(FollowReturn.shouldReturn(false, true, false, false, late, t0))
        assertFalse(FollowReturn.shouldReturn(true, false, false, false, late, t0))
    }

    @Test fun touchResetsTimer() {
        assertEquals(FollowReturn.RETURN_DELAY_MS, FollowReturn.remainingMs(5_000, 5_000))
        assertEquals(4_000L, FollowReturn.remainingMs(11_000, 5_000))
        assertEquals(0L, FollowReturn.remainingMs(99_000, 5_000))
    }

    // ── Дубли подписей ──
    @Test fun sameNameIgnoresCaseYoAndSuffix() {
        assertTrue(LabelDedup.sameName("Лужская", "лужская"))
        assertTrue(LabelDedup.sameName("Берёзовая", "Березовая"))
        assertTrue(LabelDedup.sameName("Луга", "Луга I"))
        assertFalse(LabelDedup.sameName("Луга", "Лужская"))
        assertFalse(LabelDedup.sameName("Ли", "Липа"))
        assertFalse(LabelDedup.sameName("", "Луга"))
    }

    @Test fun appLabelDroppedOnlyWhenSameNameNearby() {
        val app = listOf(AppMapLabel(100.0, 100.0, "Лужская"), AppMapLabel(400.0, 100.0, "Толмачёво"), AppMapLabel(100.0, 300.0, "Сиверская"))
        val base = listOf(
            BaseMapLabel(120.0, 110.0, "Лужская"), // рядом, то же имя -> дубль
            BaseMapLabel(405.0, 100.0, "Другая"), // рядом, другое имя -> нет
            BaseMapLabel(900.0, 300.0, "Сиверская"), // то же имя, но далеко -> нет
        )
        assertEquals(setOf(0), LabelDedup.duplicates(app, base, radiusPx = 40.0 * 2.5))
    }

    @Test fun noBaseLabelsMeansNoDuplicates() {
        assertTrue(LabelDedup.duplicates(listOf(AppMapLabel(0.0, 0.0, "А")), emptyList(), 100.0).isEmpty())
    }

    // ── Пройденный путь ──
    private fun pt(t: Long) = TrackPoint(t, 55.0, 37.0, 10.0, 5f)

    @Test fun failedOrShrunkLoadKeepsPreviousPath() {
        val prev = listOf(pt(1), pt(2), pt(3))
        assertSame(prev, PathKeeper.keep(prev, null) { it.size })
        assertSame(prev, PathKeeper.keep(prev, emptyList()) { it.size })
        assertSame(prev, PathKeeper.keep(prev, listOf(pt(1)), { it.size }))
        val grown = prev + pt(4)
        assertSame(grown, PathKeeper.keep(prev, grown) { it.size })
        assertSame(grown, PathKeeper.keep(null, grown) { it.size })
    }

    @Test fun livePointsExtendLoadedPathWithoutDuplicates() {
        val base = listOf(pt(1), pt(2))
        val live = listOf(pt(2), pt(3), pt(4))
        assertEquals(listOf(1L, 2L, 3L, 4L), PathKeeper.withLive(base, live).map { it.timeMs })
        assertEquals(listOf(3L, 4L), PathKeeper.withLive(emptyList(), listOf(pt(3), pt(4))).map { it.timeMs })
        assertSame(base, PathKeeper.withLive(base, emptyList()))
    }

    @Test fun liveBufferIsBoundedAndMonotonic() {
        var live = emptyList<TrackPoint>()
        for (i in 1..10) live = PathKeeper.appendLive(live, pt(i.toLong()), max = 4)
        assertEquals(listOf(7L, 8L, 9L, 10L), live.map { it.timeMs })
        assertEquals(4, PathKeeper.appendLive(live, pt(5), max = 4).size) // старее последней - игнорируется
    }

    // ── Сеть и рельсовая линия ──
    @Test fun railRefetchOnlyOnReturnOfNetworkWithCachedOrFailedLine() {
        assertTrue(RailRefetch.shouldRefetch(wasOnline = false, online = true, lineIsCachedOrFailed = true))
        assertFalse(RailRefetch.shouldRefetch(false, true, false))
        assertFalse(RailRefetch.shouldRefetch(true, true, true))
        assertFalse(RailRefetch.shouldRefetch(true, false, true))
    }

    @Test fun offlineCaptionHidesWhenOnline() {
        assertTrue(RailRefetch.showOfflineCaption(false))
        assertFalse(RailRefetch.showOfflineCaption(true))
    }
}
