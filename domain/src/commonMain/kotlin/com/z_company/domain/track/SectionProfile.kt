package com.z_company.domain.track

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * Продольный профиль участка советника и километраж по серверным отметкам (backend/src/services/advisor_km.py).
 * Чистая логика KMP. Единицы: ось и элементы - километры от начала оси, высоты - метры, уклон - промилле.
 */

/** Элемент профиля: участок оси `[startKm; endKm]` с постоянным уклоном (+ подъём, − спуск). */
data class ProfileElement(
    val startKm: Double,
    val endKm: Double,
    val gradePermille: Double = 0.0,
    val curveRadiusM: Double = 0.0,
    val speedLimitKmh: Int = 0,
    /** Уклон на этом месте неизвестен (данные собираются): в профиле серая зона, высота по нему не меняется. */
    val noData: Boolean = false,
)

/**
 * Отметка километража: в месте [km] оси километровая координата равна [kmAbs] (142.43 = 142 км 5 пк 30 м),
 * дальше по оси нумерация растёт ([dir] = +1) или убывает (−1) до следующей отметки.
 */
data class KmMark(val km: Double, val kmAbs: Double, val dir: Int = 1, val note: String = "")

/** Станция на оси участка. */
data class SectionStation(val km: Double, val name: String, val lat: Double? = null, val lon: Double? = null)

/** Железнодорожный переезд на оси участка: [label] - подпись («134 км 2 пк»), [kind] - вид (пока crossing). */
data class SectionCrossing(val km: Double, val label: String = "", val kind: String = "crossing")

/** Участок с осью, профилем, километражем и станциями. */
data class SectionProfile(
    val id: String,
    val name: String,
    val fromStation: String,
    val toStation: String,
    val lengthKm: Double,
    val axisHash: String,
    val axis: SectionAxis,
    val elements: List<ProfileElement>,
    val kmMarks: List<KmMark>,
    val stations: List<SectionStation>,
    /** Светофоры на оси; пусто - данных нет (старый сервер или участок ещё не оцифрован). */
    val signals: List<TrackSignal> = emptyList(),
    /** Переезды на оси; пусто - данных нет. */
    val crossings: List<SectionCrossing> = emptyList(),
) {
    /** Есть ли что рисовать: хотя бы один элемент профиля. Иначе - «данные профиля собираются». */
    val hasProfile: Boolean get() = elements.any { !it.noData }
}

/** Километр и пикет: «142 км 5 пк 30 м». */
data class KmPosition(val km: Int, val picket: Int, val plusM: Int) {
    fun text(): String = "$km км $picket пк $plusM м"

    /** «142 км 5 пк» - без метров (подпись места ограничения: оно задаётся с точностью до пикета). */
    fun textToPicket(): String = "$km км $picket пк"
}

object KmMarks {
    /** Километровая координата в месте [relKm] оси; null - отметок нет. Совпадает с `to_abs_km` сервера. */
    fun toAbs(marks: List<KmMark>, relKm: Double): Double? {
        if (marks.isEmpty()) return null
        val sorted = marks.sortedBy { it.km }
        var current = sorted[0]
        for (m in sorted) {
            if (m.km <= relKm + 1e-9) current = m else break
        }
        return current.kmAbs + (if (current.dir == 0) 1 else current.dir) * (relKm - current.km)
    }

    /** Место на оси по километровой координате (первое совпадение по ходу участка); null - не найдено. Как `to_rel_km`. */
    fun toRel(marks: List<KmMark>, absKm: Double): Double? {
        if (marks.isEmpty()) return null
        val sorted = marks.sortedBy { it.km }
        for (allowBeforeStart in listOf(false, true)) {
            for ((i, m) in sorted.withIndex()) {
                val dir = if (m.dir == 0) 1 else m.dir
                val rel = m.km + (absKm - m.kmAbs) / dir
                val lo = if (i > 0) m.km else if (allowBeforeStart) Double.NEGATIVE_INFINITY else minOf(0.0, m.km)
                val hi = if (i + 1 < sorted.size) sorted[i + 1].km else Double.POSITIVE_INFINITY
                if (rel >= lo - 1e-9 && rel < hi - 1e-9) return rel
            }
        }
        return null
    }

    /** Все места оси с километровой координатой [absKm] (нумерация может повторяться после смены); пусто - такой координаты нет. */
    fun candidates(marks: List<KmMark>, absKm: Double): List<Double> {
        if (marks.isEmpty()) return emptyList()
        val sorted = marks.sortedBy { it.km }
        val out = ArrayList<Double>()
        for ((i, m) in sorted.withIndex()) {
            val dir = if (m.dir == 0) 1 else m.dir
            val rel = m.km + (absKm - m.kmAbs) / dir
            val lo = if (i > 0) m.km else Double.NEGATIVE_INFINITY
            val hi = if (i + 1 < sorted.size) sorted[i + 1].km else Double.POSITIVE_INFINITY
            if (rel >= lo - 1e-9 && rel < hi - 1e-9) out += rel
        }
        return out
    }

    /** 142.43 -> 142 км 5 пк 30 м (пк 1 - первые 100 м после столба). Как `km_pk_text` сервера. */
    fun position(absKm: Double): KmPosition {
        val v = (absKm * 1000.0).roundToInt() / 1000.0
        var km = floor(v + 1e-9).toInt()
        var meters = ((v - km) * 1000.0).roundToInt()
        if (meters >= 1000) { km += 1; meters -= 1000 }
        return KmPosition(km, meters / 100 + 1, meters % 100)
    }

    fun text(marks: List<KmMark>, relKm: Double): String? = toAbs(marks, relKm)?.let { position(it).text() }

    /** Положение на экранах профиля пути: только километр и пикет («142 км 5 пк»), метры не показываются. */
    fun textToPicket(marks: List<KmMark>, relKm: Double): String? = toAbs(marks, relKm)?.let { position(it).textToPicket() }
}

/** Профиль высот: ломаная `(km, heightM)`, собранная интегрированием уклонов; высота отсчитывается от начала оси (0 м). */
class ElevationLine(val points: List<Pair<Double, Double>>) {
    val minHeight: Double = points.minOfOrNull { it.second } ?: 0.0
    val maxHeight: Double = points.maxOfOrNull { it.second } ?: 0.0

    fun heightAt(km: Double): Double {
        if (points.isEmpty()) return 0.0
        if (km <= points.first().first) return points.first().second
        if (km >= points.last().first) return points.last().second
        var lo = 0
        var hi = points.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (points[mid].first <= km) lo = mid else hi = mid
        }
        val (x1, y1) = points[lo]
        val (x2, y2) = points[hi]
        return if (x2 - x1 < 1e-9) y1 else y1 + (km - x1) / (x2 - x1) * (y2 - y1)
    }

    companion object {
        /** Разрывы между элементами заполняются нулевым уклоном (высота сохраняется). */
        fun fromElements(elements: List<ProfileElement>): ElevationLine {
            val sorted = elements.sortedBy { it.startKm }
            if (sorted.isEmpty()) return ElevationLine(emptyList())
            val out = ArrayList<Pair<Double, Double>>()
            var h = 0.0
            var x = sorted.first().startKm
            out += x to h
            for (e in sorted) {
                if (e.startKm > x + 1e-9) { x = e.startKm; out += x to h }
                val len = (e.endKm - maxOf(e.startKm, x)).coerceAtLeast(0.0)
                if (!e.noData) h += e.gradePermille * len // промилле * км = метры; в зоне «нет данных» высота не меняется
                x = maxOf(x, e.endKm)
                out += x to h
            }
            return ElevationLine(out)
        }
    }
}

/** Элемент профиля в месте [km] оси; null - вне элементов. */
fun List<ProfileElement>.at(km: Double): ProfileElement? =
    firstOrNull { km >= it.startKm - 1e-9 && km < it.endKm + 1e-9 }

/** Участок, подобранный по положению, с расстоянием до его оси. */
data class SectionMatch(val sectionId: String, val distanceM: Double, val kmOnAxis: Double)

object SectionPicker {
    /** Радиус, дальше которого участок считается «не здесь» (ось + запас на погрешность GPS). */
    const val MAX_DISTANCE_M = 2_000.0

    /**
     * Ближайшие к точке участки по возрастанию расстояния до оси. Быстрый отсев по габариту оси (+ запас), затем проекция.
     */
    fun nearest(
        axes: Collection<SectionAxis>,
        lat: Double,
        lon: Double,
        maxDistanceM: Double = MAX_DISTANCE_M,
        limit: Int = 10,
    ): List<SectionMatch> {
        val padLat = maxDistanceM / FlatMetric.M_PER_DEG_LAT
        val padLon = maxDistanceM / FlatMetric.kx(lat)
        return axes.asSequence()
            .filter { lat in (it.minLat - padLat)..(it.maxLat + padLat) && lon in (it.minLon - padLon)..(it.maxLon + padLon) }
            .mapNotNull { axis ->
                val hit = axis.locator.project(lat, lon) ?: return@mapNotNull null
                if (abs(hit.offsetM) > maxDistanceM) null else SectionMatch(axis.sectionId, abs(hit.offsetM), hit.kmM / 1000.0)
            }
            .sortedBy { it.distanceM }
            .take(limit)
            .toList()
    }

    /** Одно ли название станции (без регистра, ё=е, пробелов, кавычек и знаков) - для сверки с полем «Плечо». */
    fun sameStation(a: String, b: String): Boolean = norm(a).let { it.isNotEmpty() && it == norm(b) }

    /** Упоминается ли [name] в тексте [text] (без регистра, ё=е, пробелов и знаков): «Белогорск — Завитая — Архара» упоминает «Завитая». */
    fun mentions(text: String, name: String): Boolean = norm(name).let { it.isNotEmpty() && norm(text).contains(it) }

    private fun norm(s: String) = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
}

/** Габарит оси участка (прямоугольник, в который она вписана); приходит в списке участков, ось для него не нужна. */
data class SectionBox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double) {
    /** Расстояние от точки до прямоугольника, м (0 - внутри). Плоская метрика, для сотен километров достаточно для отсева. */
    fun distanceM(lat: Double, lon: Double): Double {
        val dLat = when {
            lat < minLat -> minLat - lat
            lat > maxLat -> lat - maxLat
            else -> 0.0
        }
        val dLon = when {
            lon < minLon -> minLon - lon
            lon > maxLon -> lon - maxLon
            else -> 0.0
        }
        val dy = dLat * FlatMetric.M_PER_DEG_LAT
        val dx = dLon * FlatMetric.kx(lat)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    companion object {
        /** `[minLat, minLon, maxLat, maxLon]` из ответа сервера; null - нет габарита или он некорректен. */
        fun of(values: List<Double>?): SectionBox? {
            if (values == null || values.size != 4 || values.any { !it.isFinite() }) return null
            val (a, b, c, d) = values
            if (a > c || b > d || a !in -90.0..90.0 || c !in -90.0..90.0 || b !in -180.0..180.0 || d !in -180.0..180.0) return null
            return SectionBox(a, b, c, d)
        }
    }
}

/** Строка списка участков (без оси и профиля): для выбора вручную и поиска. [box] null - сервер габарит не отдал (старый). */
data class SectionSummary(
    val id: String,
    val name: String,
    val fromStation: String,
    val toStation: String,
    val lengthKm: Double,
    val hasProfile: Boolean,
    val updatedAt: Long,
    val box: SectionBox? = null,
)

/** Граница пикета на оси: [axisKm] - место, [absKm] - километровая координата, [isPost] - это столб км (граница целого км). */
data class KmTick(val axisKm: Double, val absKm: Double, val isPost: Boolean)

/** Ячейка пикета: от [fromAxisKm] до [toAxisKm] оси, пикет [picket] 1..10 в километре [km]. */
data class PicketCell(val fromAxisKm: Double, val toAxisKm: Double, val km: Int, val picket: Int)

object KmGrid {
    private const val EPS = 1e-9

    /**
     * Границы пикетов (каждые 100 м) в окне оси [fromAxis; toAxis] по отметкам километража. Внутри отрезка отметки километр идёт
     * по пути (1 км оси = 1 км); на смене нумерации ряд начинается заново. Без отметок - пусто.
     */
    fun ticks(marks: List<KmMark>, fromAxis: Double, toAxis: Double): List<KmTick> {
        if (marks.isEmpty() || toAxis <= fromAxis) return emptyList()
        val sorted = marks.sortedBy { it.km }
        val out = ArrayList<KmTick>()
        for ((i, m) in sorted.withIndex()) {
            val segFrom = if (i == 0) Double.NEGATIVE_INFINITY else m.km
            val segTo = if (i + 1 < sorted.size) sorted[i + 1].km else Double.POSITIVE_INFINITY
            val a = maxOf(fromAxis, segFrom)
            val b = minOf(toAxis, segTo)
            if (b < a) continue
            val dir = if (m.dir == 0) 1 else m.dir
            val absA = m.kmAbs + dir * (a - m.km)
            val absB = m.kmAbs + dir * (b - m.km)
            val lo = minOf(absA, absB)
            val hi = maxOf(absA, absB)
            var n = floor(lo * 10 - EPS).toLong() + 1
            while (n / 10.0 <= hi + EPS) {
                val abs = n / 10.0
                val x = m.km + (abs - m.kmAbs) / dir
                val inSeg = x >= a - EPS && x <= b + EPS && (i + 1 >= sorted.size || x < segTo - EPS)
                if (inSeg && x >= fromAxis - EPS && x <= toAxis + EPS) out += KmTick(x, abs, n % 10 == 0L)
                n++
            }
        }
        return out.sortedBy { it.axisKm }
    }

    /** Ячейки пикетов между соседними границами; километр и пикет по середине ячейки (пк 1 - первые 100 м после столба). */
    fun cells(marks: List<KmMark>, fromAxis: Double, toAxis: Double): List<PicketCell> {
        // На 100 м шире окна: у краёв ячейка видна частично, её границы лежат за окном.
        val ticks = ticks(marks, fromAxis - 0.1, toAxis + 0.1)
        if (ticks.size < 2) return emptyList()
        return ticks.zipWithNext().filter { (a, b) -> b.axisKm > fromAxis && a.axisKm < toAxis }.mapNotNull { (a, b) ->
            if (b.axisKm - a.axisKm > 0.25) return@mapNotNull null // разрыв нумерации: между границами смена отметки
            val mid = KmMarks.toAbs(marks, (a.axisKm + b.axisKm) / 2) ?: return@mapNotNull null
            val km = floor(mid + EPS).toInt()
            val pk = floor((mid - floor(mid)) * 10 + EPS).toInt().coerceIn(0, 9) + 1
            PicketCell(a.axisKm, b.axisKm, km, pk)
        }
    }
}
