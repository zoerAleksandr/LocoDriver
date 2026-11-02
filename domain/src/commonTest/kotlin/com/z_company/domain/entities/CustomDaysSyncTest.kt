package com.z_company.domain.entities

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomDaysSyncTest {

    private fun month(key: String, days: Map<String, Int>, at: Long) = WorkScheduleMonth(key, days, at)

    @Test
    fun stampChangedMonths_stampsOnlyEditedMonth() {
        val before = WorkScheduleProfile()
            .withCustomDay(LocalDate(2026, 11, 5), 6)
            .copy(customMonthStamps = mapOf("2026-11" to 100L))
        val after = before.withCustomDay(LocalDate(2026, 12, 1), 4).stampChangedMonths(before, 500L)

        assertEquals(mapOf("2026-11" to 100L, "2026-12" to 500L), after.customMonthStamps)
    }

    @Test
    fun stampChangedMonths_resetKeepsMonthWithStamp() {
        val before = WorkScheduleProfile().withCustomDay(LocalDate(2026, 11, 5), 6)
        val after = before.withoutCustomDaysOfMonth(2026, 11).stampChangedMonths(before, 700L)

        assertTrue(after.customDays.isEmpty())
        assertEquals(listOf(month("2026-11", emptyMap(), 700L)), after.customMonths())
    }

    @Test
    fun stampChangedMonths_noChangeNoStamp() {
        val p = WorkScheduleProfile().withCustomDay(LocalDate(2026, 11, 5), 6)

        assertEquals(p, p.stampChangedMonths(p, 900L))
    }

    @Test
    fun legacyDaysWithoutStampGetLegacyStamp() {
        val p = WorkScheduleProfile().withCustomDay(LocalDate(2026, 11, 5), 6)

        assertEquals(WorkScheduleProfile.LEGACY_MONTH_STAMP, p.customMonths().single().updatedAt)
    }

    @Test
    fun plan_pushesLocalOnlyAndNewerMonths() {
        val local = WorkScheduleProfile(
            customDays = mapOf("2026-11-05" to 6, "2026-12-01" to 4),
            customMonthStamps = mapOf("2026-11" to 300L, "2026-12" to 100L),
        )
        val remote = listOf(month("2026-12", mapOf("2026-12-01" to 2), 200L))

        val plan = planCustomDaysSync(local, remote)

        assertEquals(listOf("2026-11"), plan.toPush.map { it.monthKey })
        assertEquals(listOf("2026-12"), plan.toAdopt.map { it.monthKey })
    }

    @Test
    fun plan_adoptsMonthMissingLocally() {
        val plan = planCustomDaysSync(
            WorkScheduleProfile(),
            listOf(month("2026-11", mapOf("2026-11-05" to 6), 300L)),
        )

        assertTrue(plan.toPush.isEmpty())
        assertEquals(1, plan.toAdopt.size)
    }

    @Test
    fun plan_equalStampsNothingToDo() {
        val local = WorkScheduleProfile(
            customDays = mapOf("2026-11-05" to 6),
            customMonthStamps = mapOf("2026-11" to 300L),
        )
        val plan = planCustomDaysSync(local, listOf(month("2026-11", mapOf("2026-11-05" to 6), 300L)))

        assertTrue(plan.toPush.isEmpty())
        assertTrue(plan.toAdopt.isEmpty())
    }

    @Test
    fun plan_remoteResetWinsOverOlderLocalDays() {
        val local = WorkScheduleProfile(
            customDays = mapOf("2026-11-05" to 6),
            customMonthStamps = mapOf("2026-11" to 100L),
        )
        val plan = planCustomDaysSync(local, listOf(month("2026-11", emptyMap(), 200L)))
        val adopted = local.adoptMonths(plan.toAdopt)

        assertTrue(adopted.customDays.isEmpty())
        assertEquals(200L, adopted.customMonthStamps["2026-11"])
    }

    @Test
    fun adoptMonths_replacesOnlyThoseMonths() {
        val local = WorkScheduleProfile(
            customDays = mapOf("2026-11-05" to 6, "2026-12-01" to 4),
            customMonthStamps = mapOf("2026-11" to 100L, "2026-12" to 100L),
        )
        val adopted = local.adoptMonths(listOf(month("2026-11", mapOf("2026-11-07" to 3), 200L)))

        assertEquals(mapOf("2026-12-01" to 4, "2026-11-07" to 3), adopted.customDays)
        assertEquals(100L, adopted.customMonthStamps["2026-12"])
    }

    @Test
    fun withoutCustomDays_stripsLocalOnlyData() {
        val p = WorkScheduleProfile(
            customDays = mapOf("2026-11-05" to 6),
            customMonthStamps = mapOf("2026-11" to 100L),
        )

        assertEquals(WorkScheduleProfile(), p.withoutCustomDays())
    }
}
