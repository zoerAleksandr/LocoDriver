package com.z_company.domain.track

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RestrictionJournalTest {
    private val latPerKm = 1.0 / 111.195
    private val lon = 30.0

    /** Прямая ось 20 км вдоль меридиана, без километровых отметок (зоны по км оси). */
    private val section = SectionProfile(
        id = "s1", name = "А — Б", fromStation = "А", toStation = "Б", lengthKm = 20.0, axisHash = "h",
        axis = SectionAxis("s1", "h", (0..20).map { AxisPoint(it.toDouble(), 55.0 + it * latPerKm, lon) }),
        elements = emptyList(), kmMarks = emptyList(), stations = emptyList(),
    )

    private fun warning(kind: String = RestrictionKind.SPEED, from: Double = 10.0, to: Double = 11.0, limit: Int? = 40) = RestrictionPass(
        trainId = "t1", warningId = "w1", routeId = "r1", sectionId = "s1", kind = kind, title = "ограничение", limitKmh = limit,
        startRelKm = from, endRelKm = to, updatedAt = 1L,
    )

    private fun sample(t: Long, km: Double, v: Double? = 60.0) = RestrictionSample(t, 55.0 + km * latPerKm, lon, v, 5f)

    /** Едем от [fromKm] к [toKm] со скоростью v км/ч, фиксы раз в 5 с; возвращает последнее время. */
    private fun drive(rec: RestrictionJournalRecorder, fromKm: Double, toKm: Double, speed: (Double) -> Double, lengthM: Int?, t0: Long = 1_000_000L): Long {
        var t = t0
        var km = fromKm
        val dir = if (toKm >= fromKm) 1 else -1
        while ((toKm - km) * dir >= 0) {
            val v = speed(km)
            runBlocking { rec.onPoint("t1", sample(t, km, v), lengthM?.let { it / 1000.0 }, lengthM) }
            val step = v / 3.6 * 5 / 1000.0 // за 5 с
            km += dir * step
            t += 5_000
        }
        return t
    }

    @Test
    fun speed_limit_records_head_enter_tail_exit_max_and_exceeded() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        // 50 км/ч до и после, внутри зоны один фикс на 47 км/ч (выше 40+1 допуска)
        drive(r, 8.0, 14.0, { km -> if (km in 10.0..11.2) 47.0 else 50.0 }, lengthM = 800)
        val e = assertNotNull(store.get("t1", "w1"))
        assertEquals(RestrictionStatus.DONE, e.status)
        assertTrue(e.enterAt!! > 0 && e.exitAt!! > e.enterAt!!)
        assertFalse(e.enterApprox)
        assertFalse(e.exitByHead)
        // хвост выходит позже головы: голова должна уйти на 0,8 км за конец зоны (11.8), это ~ 57 с при 50 км/ч от входа в 10.0 + 1.8 км
        val spanSec = (e.exitAt!! - e.enterAt!!) / 1000.0
        assertTrue(spanSec in 120.0..160.0, "span=$spanSec")
        assertTrue(e.maxSpeedKmh!! >= 47.0)
        assertTrue(e.exceeded)
        assertEquals(1, e.dir)
        r.invalidate()
    }

    @Test
    fun without_train_length_exit_is_by_head() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        drive(r, 8.0, 13.0, { 36.0 }, lengthM = null)
        val e = assertNotNull(store.get("t1", "w1"))
        assertEquals(RestrictionStatus.DONE, e.status)
        assertTrue(e.exitByHead)
        assertFalse(e.exceeded)
    }

    @Test
    fun other_kinds_only_enter() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(warning(kind = RestrictionKind.PANTOGRAPH, limit = null)) }
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        drive(r, 8.0, 10.6, { 60.0 }, lengthM = 500)
        val e = assertNotNull(store.get("t1", "w1"))
        assertEquals(RestrictionStatus.DONE, e.status)
        assertNull(e.exitAt)
        assertNotNull(e.enterSpeedKmh)
    }

    @Test
    fun reverse_direction_is_mirrored() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        drive(r, 14.0, 8.0, { 40.0 }, lengthM = 400)
        val e = assertNotNull(store.get("t1", "w1"))
        assertEquals(RestrictionStatus.DONE, e.status)
        assertEquals(-1, e.dir)
    }

    @Test
    fun first_fix_inside_zone_is_approximate_and_after_zone_is_ignored() {
        val inside = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var c1 = 0L
        val r1 = RestrictionJournalRecorder(inside, { section }, nowMs = { c1 += 5_000; c1 })
        drive(r1, 10.4, 12.5, { 50.0 }, lengthM = 300)
        assertTrue(inside.get("t1", "w1")!!.enterApprox)

        val after = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var c2 = 0L
        val r2 = RestrictionJournalRecorder(after, { section }, nowMs = { c2 += 5_000; c2 })
        drive(r2, 12.0, 14.0, { 50.0 }, lengthM = 300)
        assertEquals(RestrictionStatus.PLANNED, after.get("t1", "w1")!!.status)
    }

    @Test
    fun state_survives_process_restart() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(warning()) }
        var clock = 0L
        val first = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        drive(first, 8.0, 10.5, { 50.0 }, lengthM = 500, t0 = 1_000_000L)
        assertEquals(RestrictionStatus.IN_ZONE, store.get("t1", "w1")!!.status)
        // «убили процесс»: новый рекордер без памяти, продолжаем с места
        val second = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        drive(second, 10.6, 13.0, { 50.0 }, lengthM = 500, t0 = 2_000_000L)
        assertEquals(RestrictionStatus.DONE, store.get("t1", "w1")!!.status)
    }

    @Test
    fun interpolation_and_exceed_tolerance() {
        val prev = HeadSample(9.9, 1000, 50.0)
        val (t, v) = RestrictionPassMath.interpolate(prev, 10.1, 3000, 60.0, 10.0)
        assertEquals(2000L, t)
        assertEquals(55.0, v!!, 1e-6)
        assertFalse(RestrictionPass.exceeded(41.0, 40))
        assertTrue(RestrictionPass.exceeded(41.5, 40))
        assertFalse(RestrictionPass.exceeded(null, 40))
    }

    @Test
    fun store_lww_merge_pending_and_tombstones() {
        val s = InMemoryRestrictionJournalStore()
        val a = warning().copy(updatedAt = 100)
        s.upsert(a)
        assertEquals(1, s.pendingUpload(10).size)
        s.markUploaded(listOf(a))
        assertEquals(0, s.pendingUpload(10).size)
        // более старая серверная запись не затирает
        s.mergeRemote(listOf(a.copy(updatedAt = 50, title = "старое")))
        assertEquals("ограничение", s.get("t1", "w1")!!.title)
        // более новая заменяет и не считается неотправленной
        s.mergeRemote(listOf(a.copy(updatedAt = 200, title = "новое")))
        assertEquals("новое", s.get("t1", "w1")!!.title)
        assertEquals(0, s.pendingUpload(10).size)
        // снятие с поезда - надгробие, уходящее на сервер
        s.remove("t1", "w1", 300)
        assertNull(s.get("t1", "w1"))
        assertEquals(1, s.pendingUpload(10).size)
        assertTrue(s.pendingUpload(10).single().isDeleted)
        s.markUploaded(s.pendingUpload(10))
        s.upsert(a.copy(updatedAt = 400))
        s.deleteByTrain("t1")
        assertNull(s.get("t1", "w1"))
        assertTrue(s.pendingUpload(10).single().isDeleted)
    }

    @Test
    fun all_done_report_condition() {
        val done = warning().copy(enterAt = 1, exitAt = 2)
        val planned = warning().copy(warningId = "w2")
        assertEquals(RestrictionStatus.DONE, done.status)
        assertEquals(RestrictionStatus.PLANNED, planned.status)
        assertTrue(RestrictionReport.isReady(listOf(done)))
        assertFalse(RestrictionReport.isReady(listOf(done, planned)))
        assertFalse(RestrictionReport.isReady(emptyList()))
    }
}
