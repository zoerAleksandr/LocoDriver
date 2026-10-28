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
            runBlocking { rec.onPoint("t1", sample(t, km, v)) { lengthM } }
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

    @Test
    fun interim_report_is_allowed_and_marks_unfinished() {
        val done = warning().copy(enterAt = 1, exitAt = 2)
        val inZone = warning().copy(warningId = "w2", enterAt = 3, maxSpeedKmh = 41.0)
        val planned = warning().copy(warningId = "w3")
        val all = listOf(done, inZone, planned)
        assertFalse(RestrictionReport.canGenerate(emptyList()))
        assertTrue(RestrictionReport.canGenerate(listOf(planned)))
        val p = RestrictionReport.progress(all)
        assertEquals(RestrictionReport.Progress(total = 3, passed = 1, inZone = 1, waiting = 1, untracked = 0), p)
        assertFalse(p.complete)
        assertTrue(RestrictionReport.progress(listOf(done)).complete)
        val rows = RestrictionReport.rows(all, "UTC")
        assertEquals(setOf("ПРОСЛЕДОВАНО", "В ЗОНЕ", "ЕЩЁ НЕ ПРОСЛЕДОВАНО"), rows.map { it.statusLabel }.toSet())
        assertTrue(rows.first { it.statusLabel == "В ЗОНЕ" }.maxSpeed.endsWith("(пока)"))
    }

    @Test
    fun report_rows_are_formatted_in_zone() {
        val e = warning().copy(
            title = "ограничение 40 км/ч", place = "А — Б", startAbsKm = 138.0, endAbsKm = 139.4,
            enterAt = 1_761_660_309_000, enterSpeedKmh = 41.6, exitAt = 1_761_660_429_000, exitSpeedKmh = 38.2,
            maxSpeedKmh = 44.0, exceeded = true, exitByHead = true,
        )
        val rows = RestrictionReport.rows(listOf(e, warning().copy(warningId = "w2", enterAt = null)), "GMT+3")
        // запись с данными - первой, непроследованная - последней
        assertEquals("Ограничение 40 км/ч", rows[0].title)
        assertEquals("28.10 17:05:09 · 41 км/ч", rows[0].enter)
        assertTrue(rows[0].exit.startsWith("28.10 17:07:09 · 38 км/ч (по голове"))
        assertEquals("44 км/ч", rows[0].maxSpeed)
        assertTrue(rows[0].exceeded)
        assertEquals("138 км 1 пк 0 м — 139 км 5 пк 0 м", rows[0].kmText)
        assertEquals("не проследовано", rows[1].enter)
        assertEquals("28.10 14:05:09", RestrictionReport.formatTime(1_761_660_309_000, "UTC"))
        assertEquals("28.10 14:05:09", RestrictionReport.formatTime(1_761_660_309_000, "bad zone"))
    }

    @Test
    fun switching_auto_target_moves_recording_and_closes_dangling_zone() {
        val store = InMemoryRestrictionJournalStore()
        store.upsert(warning().copy(trainId = "t1", warningId = "a"))
        store.upsert(warning().copy(trainId = "t2", warningId = "b"))
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { clock += 5_000; clock })
        // «АВТО» на t1: заходим в зону и едем внутри
        var t = 1_000_000L
        var km = 9.5
        while (km < 10.5) { runBlocking { r.onPoint("t1", sample(t, km, 50.0)) { 800 } }; km += 0.07; t += 5_000 }
        assertEquals(RestrictionStatus.IN_ZONE, store.get("t1", "a")!!.status)
        assertEquals(RestrictionStatus.PLANNED, store.get("t2", "b")!!.status)
        val lastT1 = t - 5_000
        // цель сменилась на t2 посреди зоны: t1 закрыта по последнему фиксу, дальше пишется t2 (тот же участок)
        while (km < 13.0) { runBlocking { r.onPoint("t2", sample(t, km, 50.0)) { 800 } }; km += 0.07; t += 5_000 }
        val a = store.get("t1", "a")!!
        assertEquals(RestrictionStatus.DONE, a.status)
        assertEquals(lastT1, a.exitAt)
        assertTrue(a.exitByHead)
        // t1 больше не двигается, а у t2 зона зашла «по первому фиксу внутри» и тоже завершена
        val b = store.get("t2", "b")!!
        assertEquals(RestrictionStatus.DONE, b.status)
        assertTrue(b.enterApprox)
        // возврат цели на t1 запись не воскрешает (выход уже есть)
        runBlocking { r.onPoint("t1", sample(t, 10.2, 50.0)) { 800 } }
        assertEquals(lastT1, store.get("t1", "a")!!.exitAt)
    }

    @Test
    fun auto_off_on_other_train_closes_old_train_zone_even_after_restart() {
        val store = InMemoryRestrictionJournalStore()
        store.upsert(warning().copy(trainId = "t1", warningId = "a", enterAt = 1_000, enterSpeedKmh = 40.0, updatedAt = 7_000))
        // новый рекордер без памяти (процесс убит), АВТО включили на другом поезде
        val r = RestrictionJournalRecorder(store, { section }, nowMs = { 9_000_000L })
        runBlocking { r.onPoint("t2", sample(9_000_000L, 3.0, 50.0)) { null } }
        val a = store.get("t1", "a")!!
        assertEquals(RestrictionStatus.DONE, a.status)
        assertEquals(7_000L, a.exitAt) // нет фикса в памяти - время последнего обновления записи
    }

    // --- Подбор участка по плечу и ограничения без участка (28.10.2026) ---

    private fun sectionWithStations(stations: List<SectionStation>) = section.copy(stations = stations)

    @Test
    fun leg_candidates_strict_by_station_names() {
        val s = sectionWithStations(listOf(SectionStation(0.0, "Лужская"), SectionStation(20.0, "Луга")))
        val other = sectionWithStations(listOf(SectionStation(0.0, "Мга"), SectionStation(20.0, "Ульяновка"))).copy(id = "other")
        val r = LegMatch.candidates(listOf(s, other), "Лужская", "Луга", null, null)
        assertEquals(listOf("s1"), r.map { it.id })
    }

    @Test
    fun leg_candidates_fall_back_to_axis_near_both_ends_when_names_differ() {
        // на оси станции записаны иначе («Луга I»): строго не находится, как «ближайший» на карте - находится по близости оси
        val s = sectionWithStations(listOf(SectionStation(0.0, "Лужская-1"), SectionStation(20.0, "Луга I")))
        val far = sectionWithStations(emptyList()).copy(id = "far", axis = SectionAxis("far", "h", (0..20).map { AxisPoint(it.toDouble(), 57.0 + it * latPerKm, 31.0) }))
        val from = 55.0 + 1 * latPerKm to lon
        val to = 55.0 + 19 * latPerKm to lon
        assertTrue(LegMatch.candidates(listOf(s, far), "Лужская", "Луга", null, null).isEmpty())
        assertEquals(listOf("s1"), LegMatch.candidates(listOf(s, far), "Лужская", "Луга", from, to).map { it.id })
        // строгое совпадение имеет приоритет и мягкий поиск не подмешивается
        val strict = sectionWithStations(listOf(SectionStation(0.0, "Лужская"), SectionStation(20.0, "Луга"))).copy(id = "strict")
        assertEquals(listOf("strict"), LegMatch.candidates(listOf(s, strict), "Лужская", "Луга", from, to).map { it.id })
    }

    private fun untracked(at: Long? = null) = warning().copy(
        warningId = "u1", sectionId = RestrictionPass.NO_SECTION, startAbsKm = 138.0, endAbsKm = 139.4, enterAt = at, updatedAt = 5L,
    )

    @Test
    fun untracked_restriction_is_skipped_by_detector_and_does_not_block_report() {
        val store = InMemoryRestrictionJournalStore().also { it.upsert(untracked()) }
        var clock = 0L
        val r = RestrictionJournalRecorder(store, { error("участок без профиля не запрашивается") }, nowMs = { clock += 5_000; clock })
        drive(r, 8.0, 13.0, { 50.0 }, lengthM = 500)
        val e = store.get("t1", "u1")!!
        assertEquals(RestrictionStatus.PLANNED, e.status)
        assertEquals(null, e.enterAt)
        assertFalse(e.tracked)
        // отчёт: непроследованное без участка не блокирует, но показывается в строках
        val tracked = warning().copy(warningId = "w2", enterAt = 1, exitAt = 2)
        assertTrue(RestrictionReport.isReady(listOf(tracked, untracked())))
        assertEquals(0, RestrictionReport.remaining(listOf(tracked, untracked())))
        assertTrue(RestrictionReport.isReady(listOf(untracked())))
        assertFalse(RestrictionReport.isReady(listOf(warning().copy(warningId = "w3"), untracked())))
        val rows = RestrictionReport.rows(listOf(untracked()), "UTC")
        assertTrue(rows.single().untracked)
        assertTrue(rows.single().enter.startsWith("не отслеживается автоматически"))
        assertEquals("—", rows.single().exit)
    }
}
