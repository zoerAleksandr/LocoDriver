package com.z_company.domain.use_cases

import com.z_company.domain.entities.Day
import com.z_company.domain.entities.MonthOfYear
import com.z_company.domain.entities.ReleaseDay
import com.z_company.domain.entities.ReleaseType
import com.z_company.domain.entities.TagForDay
import com.z_company.domain.entities.WorkScheduleMode
import com.z_company.domain.entities.WorkScheduleProfile
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Норма по датам режима «Своя настройка» (календарь нормы) — ноябрь 2026, как на бумажном графике. */
class MonthNormCustomDaysTest {
    // 1 ноября 2026 — воскресенье. 4 ноября — праздник (День народного единства).
    private val november = MonthOfYear(
        year = 2026,
        month = 10,
        days = (1..30).map { d ->
            val dow = LocalDate(2026, 11, d).dayOfWeek.ordinal // 0 = Пн
            Day(
                d,
                when {
                    d == 4 -> TagForDay.HOLIDAY
                    dow >= 5 -> TagForDay.NON_WORKING_DAY
                    else -> TagForDay.WORKING_DAY
                },
            )
        },
    )

    private fun norm(profile: WorkScheduleProfile, releaseDays: List<ReleaseDay> = emptyList()) =
        buildMonthNorm(listOf(november), releaseDays, 2026, 10, profile)

    private fun custom() = WorkScheduleProfile.standard().copy(mode = WorkScheduleMode.CUSTOM)

    @Test
    fun withoutOverridesCustomFollowsPreviousWeek() {
        // 19 рабочих дней по 8 ч (праздничный вторник 4-го не считается): 20 пн-пт − 1 праздник.
        val standardTotal = norm(WorkScheduleProfile.standard()).sumOf { it.countedHours }
        assertEquals(standardTotal, norm(custom()).sumOf { it.countedHours })
    }

    @Test
    fun overridesReplaceDayHoursAndAreSummed() {
        val profile = custom()
            .withCustomDay(LocalDate(2026, 11, 2), 7)
            .withCustomDay(LocalDate(2026, 11, 3), 6)
            .withCustomDay(LocalDate(2026, 11, 7), 5) // суббота стала рабочей

        val days = norm(profile).associateBy { it.day }
        assertEquals(7, days.getValue(2).hours)
        assertEquals(6, days.getValue(3).hours)
        assertEquals(5, days.getValue(7).hours)
        assertEquals(true, days.getValue(7).isCustom)
        assertEquals(false, days.getValue(5).isCustom)
        assertEquals(days.values.sumOf { it.hours }, norm(profile).sumOf { it.countedHours })
    }

    @Test
    fun holidayIgnoresOverride() {
        val profile = custom().withCustomDay(LocalDate(2026, 11, 4), 8)
        val day = norm(profile).first { it.day == 4 }
        assertEquals(0, day.hours)
        assertEquals(false, day.isCustom)
    }

    @Test
    fun dayOffHasNoNormInCustomModeButKeepsNormInOtherModes() {
        val dayOff = listOf(
            ReleaseDay(year = 2026, month = 10, dayOfMonth = 9, releaseType = ReleaseType.DayOff),
        )
        assertEquals(0, norm(custom(), dayOff).first { it.day == 9 }.countedHours)
        assertEquals(true, norm(custom(), dayOff).first { it.day == 9 }.isDayOff)
        // Стандартная неделя: «Выходной» — перенос, норму не уменьшает.
        assertEquals(8, norm(WorkScheduleProfile.standard(), dayOff).first { it.day == 9 }.countedHours)
    }

    @Test
    fun absenceDayDoesNotCountInNorm() {
        val vacation = listOf(
            ReleaseDay(year = 2026, month = 10, dayOfMonth = 10, releaseType = ReleaseType.Vacation),
        )
        val day = norm(custom(), vacation).first { it.day == 10 }
        assertEquals(8, day.hours)
        assertEquals(0, day.countedHours)
    }

    @Test
    fun overridesApplyForAnyWeekAndResetOfMonthKeepsOthers() {
        val profile = custom()
            .withCustomDay(LocalDate(2026, 11, 2), 3)
            .withCustomDay(LocalDate(2026, 12, 7), 5)
            .copy(mode = WorkScheduleMode.STANDARD)
        assertEquals(3, norm(profile).first { it.day == 2 }.hours)

        val reset = profile.withoutCustomDaysOfMonth(2026, 11)
        assertEquals(8, norm(reset).first { it.day == 2 }.hours)
        assertEquals(5, reset.customHoursFor(LocalDate(2026, 12, 7)))
    }

    @Test
    fun resettingDayRemovesOverrideAndHoursAreClamped() {
        val profile = custom().withCustomDay(LocalDate(2026, 11, 2), 99)
        assertEquals(WorkScheduleProfile.MAX_CUSTOM_DAY_HOURS, profile.customHoursFor(LocalDate(2026, 11, 2)))
        assertNull(profile.withCustomDay(LocalDate(2026, 11, 2), null).customHoursFor(LocalDate(2026, 11, 2)))
    }

    @Test
    fun serverProfileDoesNotCarryCustomDays() {
        val profile = custom().withCustomDay(LocalDate(2026, 11, 2), 7)
        assertEquals(emptyMap(), profile.withoutCustomDays().customDays)
    }
}
