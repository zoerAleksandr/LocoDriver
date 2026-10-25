package com.z_company.domain.onboarding

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OnboardingRulesTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day + 12 * 60 * 60 * 1000

    // --- Подсказки на месте ---

    @Test
    fun hintShownOnlyOnce() {
        assertTrue(OnboardingHints.canShow(HintId.TRAIN_MAP, emptySet(), homeReached = true, recording = false))
        assertFalse(OnboardingHints.canShow(HintId.TRAIN_MAP, setOf("train_map"), homeReached = true, recording = false))
    }

    @Test
    fun notShownBeforeHomeScreen() {
        assertFalse(OnboardingHints.canShow(HintId.TRAIN_MAP, emptySet(), homeReached = false, recording = false))
    }

    @Test
    fun notShownWhileRecordingOrBlocked() {
        assertFalse(OnboardingHints.canShow(HintId.AUTO_CHIP, emptySet(), homeReached = true, recording = true))
        assertFalse(OnboardingHints.canShow(HintId.AUTO_CHIP, emptySet(), homeReached = true, recording = false, blocked = true))
    }

    @Test
    fun referralHintNeedsAuthorization() {
        assertFalse(
            OnboardingHints.canShow(HintId.PROFILE_REFERRAL, emptySet(), homeReached = true, recording = false, authorized = false),
        )
        assertTrue(
            OnboardingHints.canShow(HintId.PROFILE_REFERRAL, emptySet(), homeReached = true, recording = false, authorized = true),
        )
    }

    @Test
    fun trainScreenOrderIsMapThenAutoThenVu45() {
        // Одна подсказка за раз, по порядку: «Карта» -> «АВТО» -> «Справка ВУ-45»; когда показаны все три - null.
        assertEquals(HintId.TRAIN_MAP, OnboardingHints.nextOnTrainScreen(emptySet(), true, false, false))
        assertEquals(HintId.AUTO_CHIP, OnboardingHints.nextOnTrainScreen(setOf("train_map"), true, false, false))
        assertEquals(HintId.TRAIN_VU45, OnboardingHints.nextOnTrainScreen(setOf("train_map", "auto_chip"), true, false, false))
        assertNull(OnboardingHints.nextOnTrainScreen(setOf("train_map", "auto_chip", "train_vu45"), true, false, false))
        // Показанные в другом порядке подсказки не мешают: берётся первая из непоказанных.
        assertEquals(HintId.AUTO_CHIP, OnboardingHints.nextOnTrainScreen(setOf("train_map", "train_vu45"), true, false, false))
    }

    @Test
    fun trainScreenHintsWaitForHomeAndStayOutOfTheWay() {
        assertNull(OnboardingHints.nextOnTrainScreen(emptySet(), homeReached = false, recording = false, blocked = false))
        assertNull(OnboardingHints.nextOnTrainScreen(emptySet(), homeReached = true, recording = true, blocked = false))
        assertNull(OnboardingHints.nextOnTrainScreen(emptySet(), homeReached = true, recording = false, blocked = true))
    }

    // --- Напоминание про «АВТО» ---

    private fun trips(vararg daysAgo: Int) = daysAgo.map { now - it * day }

    @Test
    fun remindsAfterWeekOfTripsWithoutAuto() {
        assertTrue(AutoReminderRules.shouldRemind(now, 0, 0, trips(1, 3, 5), autoActive = false))
    }

    @Test
    fun noReminderWithFewTripDays() {
        assertFalse(AutoReminderRules.shouldRemind(now, 0, 0, trips(1, 3), autoActive = false))
        // Несколько рейсов в один день - это один день.
        assertFalse(AutoReminderRules.shouldRemind(now, 0, 0, listOf(now - 1000, now - 2000, now - 3000), autoActive = false))
    }

    @Test
    fun oldTripsOutsideWindowAreIgnored() {
        assertFalse(AutoReminderRules.shouldRemind(now, 0, 0, trips(8, 9, 10, 11), autoActive = false))
    }

    @Test
    fun thirtyDayLimit() {
        val t = trips(1, 3, 5)
        assertFalse(AutoReminderRules.shouldRemind(now, now - 29 * day, 0, t, autoActive = false))
        assertTrue(AutoReminderRules.shouldRemind(now, now - 30 * day, 0, t, autoActive = false))
    }

    @Test
    fun noReminderIfAutoUsedRecentlyOrActive() {
        val t = trips(1, 3, 5)
        assertFalse(AutoReminderRules.shouldRemind(now, 0, now - 10 * day, t, autoActive = false))
        assertTrue(AutoReminderRules.shouldRemind(now, 0, now - 40 * day, t, autoActive = false))
        assertFalse(AutoReminderRules.shouldRemind(now, 0, 0, t, autoActive = true))
    }

    @Test
    fun zoneOffsetSplitsDays() {
        // Две поездки в 23:00 и 01:00 UTC соседних суток: при смещении +3 ч это один день, при UTC - два.
        val base = 50 * day
        val a = base - 1 * 60 * 60 * 1000 // 23:00 (49-е сутки)
        val b = base + 1 * 60 * 60 * 1000 // 01:00 (50-е сутки)
        val c = base - 5 * day
        val localNow = base + 2 * day
        assertTrue(AutoReminderRules.shouldRemind(localNow, 0, 0, listOf(a, b, c), false, zoneOffsetMs = 0))
        assertFalse(AutoReminderRules.shouldRemind(localNow, 0, 0, listOf(a, b, c), false, zoneOffsetMs = 3 * 60 * 60 * 1000))
    }
}
