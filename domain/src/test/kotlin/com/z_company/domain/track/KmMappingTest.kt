package com.z_company.domain.track

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.math.abs
import kotlin.math.cos
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KmMappingTest {
    private val mLat = FlatMetric.M_PER_DEG_LAT

    // --- Образцы участков стороннего приложения (внешние данные, в репозиторий не копируются; нет файла - тест пропускается) ---
    private val dataDir = File("/Users/zoer/Downloads/atlix-app/track_profile/uchastki")

    private fun loadJson(name: String): JsonObject? {
        val f = File(dataDir, "$name.json")
        if (!f.exists()) return null
        return Json.parseToJsonElement(f.readText()).jsonObject
    }

    private fun axisOf(o: JsonObject): List<AxisPoint> = o.getValue("route").jsonArray.map {
        val a = it.jsonArray
        AxisPoint(a[2].jsonPrimitive.doubleOrNull!!, a[0].jsonPrimitive.doubleOrNull!!, a[1].jsonPrimitive.doubleOrNull!!)
    }

    /** Синтетическая ось: прямая на север с шагом [stepM]. */
    private fun lineAxis(n: Int, stepM: Double = 55.0) =
        (0 until n).map { AxisPoint(it * stepM / 1000.0, 55.0 + it * stepM / mLat, 37.0) }

    /** Точка на оси по позиции [kmM] (линейно по полилинии) со сдвигом на восток [eastM] и шумом. */
    private fun pointAt(axis: List<AxisPoint>, kmM: Double, eastM: Double = 0.0): Pair<Double, Double> {
        val x = kmM / 1000.0
        var i = axis.indexOfLast { it.km <= x }.coerceIn(0, axis.size - 2)
        val a = axis[i]; val b = axis[i + 1]
        val t = ((x - a.km) / (b.km - a.km)).coerceIn(0.0, 1.0)
        val lat = a.lat + t * (b.lat - a.lat)
        val lon = a.lon + t * (b.lon - a.lon) + eastM / FlatMetric.kx(lat)
        return lat to lon
    }

    @Test
    fun projectionInterpolatesWithinSegment() {
        val loc = AxisLocator(lineAxis(100))
        val (lat, lon) = pointAt(lineAxis(100), 1234.0, eastM = 7.0)
        val h = assertNotNull(loc.project(lat, lon))
        assertEquals(1234.0, h.kmM, 0.5)
        assertEquals(7.0, h.offsetM, 0.2)
    }

    @Test
    fun windowKeepsSegmentAndFallsBackToFullSearch() {
        // Ось: 600 вершин на север, затем возврат в 100 м западнее (параллельный путь, индексы далеко).
        val out = (0 until 600).map { AxisPoint(it * 55.0 / 1000.0, 55.0 + it * 55.0 / mLat, 37.0) }
        val lon2 = 37.0 - 100.0 / FlatMetric.kx(55.2)
        val back = (0 until 600).map {
            AxisPoint(33.0 + 33.0 + it * 55.0 / 1000.0, 55.0 + (599 - it) * 55.0 / mLat, lon2)
        }
        val axis = out + back
        val loc = AxisLocator(axis)
        // Точка ближе к обратному пути (20 м от него, 80 м от прямого): с подсказкой в первом куске остаётся на нём (d < 200).
        val lat = 55.0 + 300 * 55.0 / mLat
        val lon = 37.0 - 80.0 / FlatMetric.kx(lat)
        val hinted = assertNotNull(loc.project(lat, lon, hint = 300))
        assertTrue(hinted.vertex < 600, "окно удержало прямой путь")
        val full = assertNotNull(loc.project(lat, lon, hint = null))
        assertTrue(full.vertex >= 600, "полный перебор выбирает ближайший (обратный)")
        // Обратный путь в 400 м: фикс на нём при подсказке в первом куске -> d > 200 м -> аварийный полный перебор.
        val lon3 = 37.0 - 400.0 / FlatMetric.kx(55.2)
        val axis3 = out + back.map { it.copy(lon = lon3) }
        val loc3 = AxisLocator(axis3)
        val far = assertNotNull(loc3.project(lat, lon3, hint = 300))
        assertTrue(far.vertex >= 600 && far.offsetM < 5.0, "vertex=${far.vertex} off=${far.offsetM}")
    }

    @Test
    fun picketBoundariesAndVariableCount() {
        assertEquals(Picket.KmPicket(5642, 7), Picket.of(5641.6))
        assertEquals(Picket.KmPicket(5, 1), Picket.of(4.0))
        // 4.3 - 4 = 0.2999999999999998: без эпсилона получился бы пикет 3
        assertEquals(Picket.KmPicket(5, 4), Picket.of(4.3))
        assertEquals(Picket.KmPicket(5, 10), Picket.of(4.9999999))
        assertEquals(Picket.KmPicket(1, 1), Picket.of(0.0))
        // нестандартный километр на 12 пикетов: пикет 7 начинается с 7/12 км
        assertEquals(Picket.KmPicket(3, 7), Picket.of(2.0 + 6.0 / 12 + 0.0001, perKm = 12))
        assertEquals(Picket.KmPicket(3, 12), Picket.of(2.9999999, perKm = 12))
    }

    @Test
    fun numberingAnchorsDecreasingBreaksReverseAndCalibration() {
        val m = RailKmMap(listOf(0.0 to 10.0, 10.0 to 20.0))
        assertEquals(15.0, m.at(5.0), 1e-6)
        assertEquals(25.0, m.at(15.0), 1e-6) // экстраполяция наклоном крайнего отрезка
        val dec = RailKmMap(listOf(0.0 to 47.45, 39.6 to 7.85))
        assertEquals(27.65, dec.at(19.8), 1e-3)
        // breaks: разрыв нумерации 7.85 -> 1632.2, куски независимы
        val br = RailKmMap(listOf(0.0 to 47.45, 39.5 to 7.95, 39.6 to 1632.2, 50.0 to 1621.8), breaks = listOf(39.6))
        assertEquals(1632.2, br.at(39.6), 1e-3)
        assertEquals(1626.99, br.at(44.81), 0.05)
        assertEquals(7.95, br.at(39.5), 1e-3)
        // reversed: км растёт от другого конца куска
        val rev = RailKmMap(listOf(0.0 to 0.0, 10.0 to 10.0), reversed = true)
        assertEquals(7.0, rev.at(3.0), 1e-6)
        // калибровка аддитивна, константна за крайними якорями
        val cal = KmCalibration(m, listOf(2.0 to 13.0, 8.0 to 19.0)) // базовые 12 и 18 -> Δ = +1
        assertEquals(1.0, cal.delta(0.0), 1e-6)
        assertEquals(1.0, cal.delta(100.0), 1e-6)
        assertEquals(16.0, cal.at(5.0), 1e-6)
        val cal2 = KmCalibration(m, listOf(2.0 to 12.0, 8.0 to 20.0)) // Δ 0 -> 2
        assertEquals(1.0, cal2.delta(5.0), 1e-6)
    }

    // --- Трекер на синтетических треках ---

    private fun drive(
        axis: List<AxisPoint>, tracker: AxisTracker, fromKm: Double, toKm: Double, startMs: Long,
        speedKmh: Double = 80.0, stepS: Int = 8, noiseM: Double = 4.0, rnd: Random = Random(1),
    ): Triple<Long, Double, List<Pair<Double, AxisTracker.FixResult>>> {
        val out = ArrayList<Pair<Double, AxisTracker.FixResult>>()
        var t = startMs
        var km = fromKm * 1000
        while (km < toKm * 1000) {
            val (lat, lon) = pointAt(axis, km, eastM = rnd.nextDouble(-noiseM, noiseM))
            out += km to tracker.onFix(t, lat, lon, speedKmh, 8f)
            t += stepS * 1000L
            km += speedKmh / 3.6 * stepS
        }
        return Triple(t, km, out)
    }

    private fun tracker(axis: List<AxisPoint>) = AxisTracker(AxisLocator(axis))

    private fun checkAgainstRealAxis(axis: List<AxisPoint>) {
        val tr = tracker(axis)
        val (_, _, res) = drive(axis, tr, 0.0, axis.last().km * 0.5, 1_000L)
        var maxErr = 0.0
        for ((truth, r) in res) {
            assertEquals(AxisTracker.Status.ACCEPTED, r.status)
            assertFalse(r.offRoute)
            maxErr = maxOf(maxErr, abs(r.kmM!! - truth))
        }
        assertTrue(maxErr < 40.0, "ошибка км $maxErr м")
    }

    @Test
    fun trackerOnKorshunihaAxis() {
        val o = loadJson("kor_usti") ?: return
        checkAgainstRealAxis(axisOf(o))
    }

    @Test
    fun trackerOnHrebtovayaAxis() {
        val o = loadJson("U1_hrebtovaya_ustilimsk") ?: return
        checkAgainstRealAxis(axisOf(o))
    }

    @Test
    fun u1AnchorsAgreeWithStations() {
        val o = loadJson("U1_hrebtovaya_ustilimsk") ?: return
        val km = o.getValue("km").jsonObject
        val anchors = km.getValue("anchors").jsonArray.map { it.jsonArray[0].jsonPrimitive.doubleOrNull!! to it.jsonArray[1].jsonPrimitive.doubleOrNull!! }
        val map = RailKmMap(anchors)
        val stations = o.getValue("stations").jsonArray
        // Начальный (geo 19.76) -> 19.7 по независимой разметке kor_usti; Усть-Илимск -> 214.5
        val nach = stations.first { it.jsonArray[1].jsonPrimitive.content == "Начальный" }.jsonArray[0].jsonPrimitive.doubleOrNull!!
        assertEquals(19.7, map.at(nach), 0.3)
        assertEquals(214.5, map.at(o.getValue("total").jsonPrimitive.doubleOrNull!!), 0.01)
        // Пикет с записанной станции: берём ось из данных
        assertTrue(Picket.of(map.at(nach)).picket in 1..10)
    }

    @Test
    fun spikeIsRejectedAndRealTeleportResyncsAfterTen() {
        val axis = lineAxis(2000)
        val tr = tracker(axis)
        var (t, km, _) = drive(axis, tr, 0.0, 5.0, 1_000L)
        // одиночный скачок на 3 км за 8 с -> GLITCH
        val (lat, lon) = pointAt(axis, km + 3000)
        assertEquals(AxisTracker.Status.GLITCH, tr.onFix(t, lat, lon, 80.0, 8f).status)
        // следующий нормальный фикс принимается (без залипания)
        val (lat2, lon2) = pointAt(axis, km + 80.0 / 3.6 * 16)
        val ok = tr.onFix(t + 16_000, lat2, lon2, 80.0, 8f)
        assertEquals(AxisTracker.Status.ACCEPTED, ok.status)
        // реальный телепорт: 10 согласованных фиксов подряд -> принять (9 отброшены, 10-й ресинхронизирует)
        val tr2 = tracker(axis)
        val (t2, km2, _) = drive(axis, tr2, 0.0, 5.0, 1_000L)
        val statuses = (1..10).map { i ->
            val (la, lo) = pointAt(axis, km2 + 20_000 + i * 20.0)
            tr2.onFix(t2 + i * 8_000L, la, lo, 80.0, 8f).status
        }
        assertEquals(List(9) { AxisTracker.Status.GLITCH } + AxisTracker.Status.ACCEPTED, statuses)
    }

    @Test
    fun accuracyThresholdsBeforeAndAfterLock() {
        val axis = lineAxis(500)
        val tr = tracker(axis)
        val (la, lo) = pointAt(axis, 1000.0)
        assertEquals(AxisTracker.Status.WEAK_SIGNAL, tr.onFix(1000, la, lo, 50.0, 100f).status)
        assertFalse(tr.isLocked)
        assertEquals(AxisTracker.Status.ACCEPTED, tr.onFix(2000, la, lo, 50.0, 79f).status)
        assertTrue(tr.isLocked)
        assertEquals(AxisTracker.Status.ACCEPTED, tr.onFix(10_000, la, lo, 0.0, 120f).status)
        assertEquals(AxisTracker.Status.COARSE_FIX, tr.onFix(20_000, la, lo, 0.0, 160f).status)
    }

    @Test
    fun offRouteNeedsThreeInARowAndBlocksEvents() {
        val axis = lineAxis(2000)
        val tr = tracker(axis)
        var (t, km, _) = drive(axis, tr, 0.0, 3.0, 1_000L)
        fun off(i: Int): AxisTracker.FixResult {
            val (la, lo) = pointAt(axis, km, eastM = 500.0)
            return tr.onFix(t + i * 8_000L, la, lo, 5.0, 8f)
        }
        val a = off(1); val b = off(2)
        assertFalse(a.offRoute); assertTrue(a.eventsAllowed)
        assertFalse(b.offRoute)
        val c = off(3)
        assertTrue(c.offRoute); assertFalse(c.eventsAllowed)
        // одиночный выброс не включает флаг: возврат на ось сбрасывает счётчик
        val (la, lo) = pointAt(axis, km)
        val back = tr.onFix(t + 4 * 8_000L, la, lo, 5.0, 8f)
        assertFalse(back.offRoute); assertTrue(back.eventsAllowed)
        val d = off(5)
        assertFalse(d.offRoute)
    }

    @Test
    fun standHysteresisKeepsDisplayedKm() {
        val axis = lineAxis(500)
        val tr = tracker(axis)
        val (la0, lo0) = pointAt(axis, 5000.0)
        val first = tr.onFix(1000, la0, lo0, 0.0, 8f)
        val shown = first.displayKmM
        val rnd = Random(7)
        for (i in 1..20) {
            val (la, lo) = pointAt(axis, 5000.0 + rnd.nextDouble(-15.0, 15.0))
            val r = tr.onFix(1000L + i * 8000L, la, lo, 0.2, 8f)
            assertEquals(shown, r.displayKmM)
        }
        // тронулись: км отображается свободно
        val (la, lo) = pointAt(axis, 5100.0)
        assertEquals(5100.0, tr.onFix(300_000, la, lo, 15.0, 8f).displayKmM!!, 1.0)
    }

    @Test
    fun altitudeContribProjectionWindowedMatchesFullSearch() {
        val axis = lineAxis(800)
        val sec = SectionAxis("s", "h".repeat(32), axis)
        val rnd = Random(3)
        var hint: Int? = null
        var km = 0.0
        while (km < 40_000) {
            val (la, lo) = pointAt(axis, km, eastM = rnd.nextDouble(-6.0, 6.0))
            val w = sec.locator.project(la, lo, hint)!!
            val f = sec.locator.project(la, lo, null)!!
            assertEquals(f.kmM, w.kmM, 1e-6)
            hint = w.vertex
            km += 40.0
        }
    }
}
