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
)

/**
 * Отметка километража: в месте [km] оси километровая координата равна [kmAbs] (142.43 = 142 км пк 5 +30 м),
 * дальше по оси нумерация растёт ([dir] = +1) или убывает (−1) до следующей отметки.
 */
data class KmMark(val km: Double, val kmAbs: Double, val dir: Int = 1, val note: String = "")

/** Станция на оси участка. */
data class SectionStation(val km: Double, val name: String, val lat: Double? = null, val lon: Double? = null)

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
) {
    /** Есть ли что рисовать: хотя бы один элемент профиля. Иначе - «данные профиля собираются». */
    val hasProfile: Boolean get() = elements.isNotEmpty()
}

/** Километр и пикет: «142 км пк 5 +30 м». */
data class KmPosition(val km: Int, val picket: Int, val plusM: Int) {
    fun text(): String = "$km км пк $picket" + if (plusM > 0) " +$plusM м" else ""
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

    /** 142.43 -> 142 км пк 5 +30 м (пк 1 - первые 100 м после столба). Как `km_pk_text` сервера. */
    fun position(absKm: Double): KmPosition {
        val v = (absKm * 1000.0).roundToInt() / 1000.0
        var km = floor(v + 1e-9).toInt()
        var meters = ((v - km) * 1000.0).roundToInt()
        if (meters >= 1000) { km += 1; meters -= 1000 }
        return KmPosition(km, meters / 100 + 1, meters % 100)
    }

    fun text(marks: List<KmMark>, relKm: Double): String? = toAbs(marks, relKm)?.let { position(it).text() }
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
                h += e.gradePermille * len // промилле * км = метры
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

    private fun norm(s: String) = s.lowercase().replace('ё', 'е').filter { it.isLetterOrDigit() }
}

/** Строка списка участков (без оси и профиля): для выбора вручную и поиска. */
data class SectionSummary(
    val id: String,
    val name: String,
    val fromStation: String,
    val toStation: String,
    val lengthKm: Double,
    val hasProfile: Boolean,
    val updatedAt: Long,
)
