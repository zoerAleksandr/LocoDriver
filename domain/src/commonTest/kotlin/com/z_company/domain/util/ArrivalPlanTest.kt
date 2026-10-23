package com.z_company.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArrivalPlanTest {
    // Прямая линия по экватору: 0°..1° долготы (~111 км)
    private val line = listOf(0.0 to 0.0, 0.0 to 0.5, 0.0 to 1.0)
    private val stops = listOf(
        TrainMapStop("A", 0.0, 0.0),
        TrainMapStop("B", 0.0, 0.5),
        TrainMapStop("C", 0.0, 1.0),
    )
    private val km = 111.19

    @Test fun parseTime() {
        assertEquals(18 to 30, ArrivalPlan.parseTime("18:30"))
        assertEquals(9 to 5, ArrivalPlan.parseTime("9:05"))
        assertEquals(9 to 5, ArrivalPlan.parseTime("0905"))
        assertEquals(9 to 5, ArrivalPlan.parseTime("905"))
        assertNull(ArrivalPlan.parseTime("24:00"))
        assertNull(ArrivalPlan.parseTime("12:60"))
        assertNull(ArrivalPlan.parseTime("12:5"))
        assertNull(ArrivalPlan.parseTime(""))
        assertNull(ArrivalPlan.parseTime("ab:cd"))
        assertNull(ArrivalPlan.parseTime("1:2:3"))
    }

    @Test fun plannedLaterTodayAndEarlierNextDay() {
        // 2026-10-09 12:00 UTC = 15:00 МСК
        val now = 1_791_547_200_000L
        val later = ArrivalPlan.planned(now, 18, 30, "GMT+3")
        assertEquals(false, later.earlier)
        assertEquals(3 * 3_600_000L + 30 * 60_000L, later.ms - now)
        assertEquals(0, ArrivalPlan.dayOffset(now, later.ms, "GMT+3"))
        val earlier = ArrivalPlan.planned(now, 9, 0, "GMT+3")
        assertTrue(earlier.earlier)
        assertEquals(18 * 3_600_000L, earlier.ms - now)
        assertEquals(1, ArrivalPlan.dayOffset(now, earlier.ms, "GMT+3"))
        // то же время, что сейчас, - уже «раньше»
        assertTrue(ArrivalPlan.planned(now, 15, 0, "GMT+3").earlier)
    }

    @Test fun tapAheadOnLineSnapsAndMeasuresAlongLine() {
        val fix = GeoFix(0.0, 0.1)
        val r = ArrivalPlan.resolveTap(stops, line, fix, 0.001, 0.75, lineTolM = 500.0, stationTolM = 500.0)
        val a = r as ArrivalPlan.TapResult.Ahead
        assertNull(a.target.name)
        assertTrue(kotlin.math.abs(a.target.latitude) < 1e-9)
        assertTrue(kotlin.math.abs(a.distanceM - 0.65 * km * 1000) < 300.0)
    }

    @Test fun tapNearStationTakesNameAndSnaps() {
        val fix = GeoFix(0.0, 0.1)
        val r = ArrivalPlan.resolveTap(stops, line, fix, 0.003, 0.502, lineTolM = 10.0, stationTolM = 1000.0)
        val a = r as ArrivalPlan.TapResult.Ahead
        assertEquals("B", a.target.name)
        assertTrue(kotlin.math.abs(a.distanceM - 0.4 * km * 1000) < 300.0)
    }

    @Test fun tapBehindOrTooCloseIsNotAhead() {
        val fix = GeoFix(0.0, 0.5)
        assertEquals(ArrivalPlan.TapResult.NotAhead, ArrivalPlan.resolveTap(stops, line, fix, 0.0, 0.2, 500.0, 500.0))
        assertEquals(ArrivalPlan.TapResult.NotAhead, ArrivalPlan.resolveTap(stops, line, fix, 0.0, 0.5005, 500.0, 50.0))
    }

    @Test fun tapAwayFromLineAndNoLine() {
        val fix = GeoFix(0.0, 0.1)
        assertEquals(ArrivalPlan.TapResult.NotOnLine, ArrivalPlan.resolveTap(stops, line, fix, 0.1, 0.75, 500.0, 500.0))
        assertEquals(ArrivalPlan.TapResult.NotOnLine, ArrivalPlan.resolveTap(stops, null, fix, 0.0, 0.75, 500.0, 500.0))
    }

    @Test fun tapNeedsFixOnRail() {
        assertEquals(ArrivalPlan.TapResult.NoFix, ArrivalPlan.resolveTap(stops, line, null, 0.0, 0.75, 500.0, 500.0))
        // положение в ~1,1 км севернее линии - вне путей
        assertEquals(ArrivalPlan.TapResult.OffRail, ArrivalPlan.resolveTap(stops, line, GeoFix(0.01, 0.1), 0.0, 0.75, 500.0, 500.0))
    }

    @Test fun distanceCases() {
        val t = PlanTarget(0.0, 0.75)
        val ahead = ArrivalPlan.distance(line, GeoFix(0.0, 0.25), t) as ArrivalPlan.PlanDistance.Ahead
        assertTrue(kotlin.math.abs(ahead.meters - 0.5 * km * 1000) < 300.0)
        assertEquals(ArrivalPlan.PlanDistance.Arrived, ArrivalPlan.distance(line, GeoFix(0.0, 0.7499), t))
        assertEquals(ArrivalPlan.PlanDistance.Arrived, ArrivalPlan.distance(line, GeoFix(0.0, 0.9), t))
        assertEquals(ArrivalPlan.PlanDistance.OffRail, ArrivalPlan.distance(line, GeoFix(0.01, 0.25), t))
        assertEquals(ArrivalPlan.PlanDistance.NoFix, ArrivalPlan.distance(line, null, t))
        assertEquals(ArrivalPlan.PlanDistance.NoLine, ArrivalPlan.distance(null, GeoFix(0.0, 0.25), t))
        assertEquals(ArrivalPlan.PlanDistance.TargetNotOnLine, ArrivalPlan.distance(line, GeoFix(0.0, 0.25), PlanTarget(1.0, 0.75)))
    }

    @Test fun evaluateStatuses() {
        val now = 1_000_000L
        val hour = 3_600_000L
        // 60 км за 1 ч: нужно чуть больше 60 км/ч (потеря времени на разгон от 45 км/ч)
        val behind = ArrivalPlan.evaluate(now, 60_000.0, now + hour, 45.0)
        assertEquals(ArrivalPlan.Status.BEHIND, behind.status)
        assertEquals(60.05, behind.requiredKmh!!, 0.2)
        assertEquals(16, behind.faster)
        assertEquals(now + (hour * 60.0 / 45.0).toLong(), behind.expectedMs)
        assertEquals(ArrivalPlan.Status.ON_TIME, ArrivalPlan.evaluate(now, 60_000.0, now + hour, 65.0).status)
        assertEquals(ArrivalPlan.Status.ON_TIME, ArrivalPlan.evaluate(now, 60_000.0, now + hour, 61.0).status)
        assertEquals(ArrivalPlan.Status.AHEAD, ArrivalPlan.evaluate(now, 60_000.0, now + hour, 80.0).status)
        val unknown = ArrivalPlan.evaluate(now, 60_000.0, now + hour, null)
        assertEquals(ArrivalPlan.Status.UNKNOWN, unknown.status)
        assertNull(unknown.expectedMs)
        assertEquals(60.0, unknown.requiredKmh!!, 1e-6)
        val overdue = ArrivalPlan.evaluate(now, 60_000.0, now - 1, 80.0)
        assertEquals(ArrivalPlan.Status.OVERDUE, overdue.status)
        assertNull(overdue.requiredKmh)
        assertNotNull(overdue.expectedMs)
    }

    @Test fun evaluateAccelerationAndStop() {
        val now = 1_000_000L
        val hour = 3_600_000L
        // Уже едем быстрее нужного: разгон не нужен, v = d/T
        assertEquals(60.0, ArrivalPlan.evaluate(now, 60_000.0, now + hour, 80.0).requiredKmh!!, 1e-6)
        // С места: потеря времени на разгон поднимает нужную скорость выше простой d/T
        val fromStop = ArrivalPlan.evaluate(now, 10_000.0, now + hour / 4, 0.1)
        assertEquals(40.0 < fromStop.requiredKmh!!, true)
        // «На остановку»: торможение 3 мин - прибавка 1,5 мин к ходу, а не вычитание 3 мин; 4 мин до цели на 1,5 км при 5 мин торможения не «вышло»
        val noStop = ArrivalPlan.evaluate(now, 60_000.0, now + hour, 80.0).requiredKmh!!
        val stop = ArrivalPlan.evaluate(now, 60_000.0, now + hour, 80.0, stopMin = 3).requiredKmh!!
        assertEquals(true, stop > noStop && stop < 60.0 * 60.0 / 57.0 + 0.5)
        val near = ArrivalPlan.evaluate(now, 1_500.0, now + 4 * 60_000, 43.0, stopMin = 5)
        assertEquals(true, near.status != ArrivalPlan.Status.OVERDUE && near.status != ArrivalPlan.Status.IMPOSSIBLE)
        // Время вышло только когда плановый момент прошёл
        assertEquals(ArrivalPlan.Status.OVERDUE, ArrivalPlan.evaluate(now, 1_000.0, now - 1, 50.0, stopMin = 3).status)
        // Не успеть с разгоном: 5 км за 2 мин с места
        assertEquals(ArrivalPlan.Status.IMPOSSIBLE, ArrivalPlan.evaluate(now, 5_000.0, now + 120_000, 5.0).status)
    }

    @Test fun codecKeepsStopFieldsAndReadsOldFormat() {
        val plan = ArrivalPlanData(PlanTarget(1.5, 2.5, "Луга"), 7L, onStop = true, stopMin = 5, showOnMap = false)
        val back = ArrivalPlan.decode(ArrivalPlan.encode(plan))!!
        assertEquals(plan, back)
        val old = ArrivalPlan.decode("1.5\t2.5\t7\tЛуга")!!
        assertEquals(ArrivalPlanData(PlanTarget(1.5, 2.5, "Луга"), 7L), old)
    }

    @Test fun stopsAheadSkipsPassedAndNeedsFix() {
        val line = listOf(0.0 to 0.0, 0.0 to 1.0)
        val stops = listOf(TrainMapStop("A", 0.0, 0.0), TrainMapStop("B", 0.0, 0.5), TrainMapStop("C", 0.0, 1.0))
        val ahead = ArrivalPlan.stopsAhead(stops, line, GeoFix(0.0, 0.25))
        assertEquals(listOf("B", "C"), ahead.map { it.stop.name })
        assertEquals(true, ahead[0].distanceM < ahead[1].distanceM)
        assertEquals(emptyList(), ArrivalPlan.stopsAhead(stops, line, null))
    }

    @Test fun staleAndCodec() {
        val plan = ArrivalPlanData(PlanTarget(55.123456, 37.654321, "Станция\tX"), 5_000_000L)
        val back = ArrivalPlan.decode(ArrivalPlan.encode(plan))!!
        assertEquals(plan.target.latitude, back.target.latitude)
        assertEquals(plan.target.longitude, back.target.longitude)
        assertEquals("Станция X", back.target.name)
        assertEquals(plan.plannedMs, back.plannedMs)
        assertNull(ArrivalPlan.decode(ArrivalPlan.encode(ArrivalPlanData(PlanTarget(1.0, 2.0), 3L)))!!.target.name)
        assertNull(ArrivalPlan.decode(null))
        assertNull(ArrivalPlan.decode("garbage"))
        assertEquals(false, ArrivalPlan.isStale(plan, plan.plannedMs + ArrivalPlan.STALE_MS))
        assertEquals(true, ArrivalPlan.isStale(plan, plan.plannedMs + ArrivalPlan.STALE_MS + 1))
    }
}
