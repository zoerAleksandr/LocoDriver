package com.z_company.domain.track

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

/*
 * Согласие №2: обезличенный вклад «километр оси + высота» (SCREEN_SPECS §6.9, §17.7;
 * сервер - POST /v1/advisor/altitude-contrib, docs/TRAIN_TRACKS_SERVER_DESIGN.md).
 * Чистая логика без платформы: фильтр качества, привязка к оси участка, ячейки, кодирование.
 * Координаты нигде не логируются; в тело вклада они не попадают (только км, высоты и боковые отклонения).
 */

/** Точка геометрии оси участка: [km] - километры вдоль оси от её начала (как `geometry[].km` на сервере). */
data class AxisPoint(val km: Double, val lat: Double, val lon: Double)

/** Ось опубликованного участка советника и её хэш ([axisHash] - 32 hex, меняется только при смене геометрии). */
class SectionAxis(val sectionId: String, val axisHash: String, val points: List<AxisPoint>) {
    val minLat: Double = points.minOfOrNull { it.lat } ?: 0.0
    val maxLat: Double = points.maxOfOrNull { it.lat } ?: 0.0
    val minLon: Double = points.minOfOrNull { it.lon } ?: 0.0
    val maxLon: Double = points.maxOfOrNull { it.lon } ?: 0.0
}

/** Точка буфера вклада: только то, что нужно для привязки к оси (без времени). */
data class AltitudeSample(
    val lat: Double,
    val lon: Double,
    val altM: Double?,
    val baroM: Double?,
    val accM: Float?,
)

/** Результат привязки точки к оси: километр оси (м) и боковое отклонение (м). */
data class AxisProjection(val kmM: Double, val offsetM: Double)

object AxisProjector {
    private const val M_PER_DEG = 111_320.0

    /** Ближайший отрезок оси; null - ось короче двух точек. */
    fun project(axis: SectionAxis, lat: Double, lon: Double): AxisProjection? {
        val pts = axis.points
        if (pts.size < 2) return null
        val kx = cos(lat * PI_OVER_180) * M_PER_DEG
        var best = Double.MAX_VALUE
        var bestKm = 0.0
        for (i in 0 until pts.size - 1) {
            val a = pts[i]
            val b = pts[i + 1]
            val ax = (a.lon - lon) * kx
            val ay = (a.lat - lat) * M_PER_DEG
            val bx = (b.lon - lon) * kx
            val by = (b.lat - lat) * M_PER_DEG
            // Быстрый отказ: отрезок целиком дальше текущего лучшего по одной из осей.
            if (min(ay, by) > best || max(ay, by) < -best || min(ax, bx) > best || max(ax, bx) < -best) continue
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 <= 0.0) 0.0 else ((-ax * dx - ay * dy) / len2).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            val d = sqrt(px * px + py * py)
            if (d < best) {
                best = d
                bestKm = (a.km + t * (b.km - a.km)) * 1000.0
            }
        }
        return if (best == Double.MAX_VALUE) null else AxisProjection(bestKm, best)
    }

    private const val PI_OVER_180 = 0.017453292519943295
}

/** Один вклад: фрагмент оси `[kmFromM, kmFromM + n*stepM)`, высоты в дм (null - нет данных) и отклонения по ячейкам. */
data class ContribFragment(
    val sectionId: String,
    val axisHash: String,
    val kmFromM: Int,
    val stepM: Int,
    val altsDm: List<Long?>,
    val offsetM: List<Double?>,
    /** `gnss` | `baro` - один источник на весь фрагмент. */
    val source: String,
    val meanAccM: Double?,
)

object AltitudeContribBuilder {
    const val STEP_M = 25
    const val MAX_OFFSET_M = 30.0
    const val MAX_ACCURACY_M = 20f
    const val MAX_CELLS = 4000

    /** Разрыв между известными ячейками больше этого (3 км при шаге 25 м) - новый фрагмент (туннель, потеря GPS). */
    const val GAP_SPLIT_CELLS = 120

    /** Фрагменты короче (по числу ячеек с высотой) не отправляем. */
    const val MIN_KNOWN_CELLS = 5

    /** Окно сглаживания по ячейкам (медиана известных соседей в пределах ±2 ячеек). */
    private const val SMOOTH_RADIUS = 2

    /** Фильтр качества: точность ≤ 20 м, скорость известна, есть правдоподобная высота. Иначе null. */
    fun accept(p: TrackPoint): AltitudeSample? {
        val acc = p.accuracyM ?: return null
        if (!acc.isFinite() || acc > MAX_ACCURACY_M) return null
        if (p.speedKmh == null) return null
        val alt = AltitudeMath.sanitize(p.altitudeM)
        val baro = AltitudeMath.sanitize(p.baroAltitudeM)
        if (alt == null && baro == null) return null
        return AltitudeSample(p.latitude, p.longitude, alt, baro, acc)
    }

    private class Obs(val cell: Int, val alt: Double, val off: Double, val acc: Float?)

    /**
     * Фрагменты вклада для одной оси. Ячейка `i` - позиция `i*STEP_M` по оси (ближайший узел сетки), `kmFromM` - начало
     * первой ячейки. Источник высоты - барометр, если он есть хотя бы у половины точек на оси, иначе GPS
     * (смешивать нельзя: у барометра иной нуль). В ячейке - медиана высот, затем сглаживание медианой по соседям.
     */
    fun build(axis: SectionAxis, samples: List<AltitudeSample>): List<ContribFragment> {
        if (axis.points.size < 2 || samples.isEmpty()) return emptyList()
        val margin = 0.0005 // ~55 м: грубый отсев по рамке оси
        val onAxis = ArrayList<Pair<AltitudeSample, AxisProjection>>()
        for (s in samples) {
            if (s.lat < axis.minLat - margin || s.lat > axis.maxLat + margin ||
                s.lon < axis.minLon - margin || s.lon > axis.maxLon + margin
            ) continue
            val pr = AxisProjector.project(axis, s.lat, s.lon) ?: continue
            if (pr.offsetM <= MAX_OFFSET_M) onAxis += s to pr
        }
        if (onAxis.isEmpty()) return emptyList()
        val baroCount = onAxis.count { it.first.baroM != null }
        val useBaro = baroCount * 2 >= onAxis.size
        val obs = ArrayList<Obs>()
        for ((s, pr) in onAxis) {
            val alt = (if (useBaro) s.baroM else s.altM) ?: continue
            obs += Obs((pr.kmM / STEP_M).roundToInt().coerceAtLeast(0), alt, pr.offsetM, s.accM)
        }
        if (obs.isEmpty()) return emptyList()

        val byCell = obs.groupBy { it.cell }.toSortedMap()
        val cells = byCell.keys.toList()
        val raw = HashMap<Int, Double>()
        for ((c, list) in byCell) raw[c] = median(list.map { it.alt })
        val smooth = HashMap<Int, Double>()
        for (c in cells) {
            val near = ArrayList<Double>()
            for (d in -SMOOTH_RADIUS..SMOOTH_RADIUS) raw[c + d]?.let { near += it }
            smooth[c] = median(near)
        }

        val result = ArrayList<ContribFragment>()
        var start = 0
        while (start < cells.size) {
            var end = start
            while (end + 1 < cells.size &&
                cells[end + 1] - cells[end] <= GAP_SPLIT_CELLS &&
                cells[end + 1] - cells[start] < MAX_CELLS
            ) end++
            if (end - start + 1 >= MIN_KNOWN_CELLS) {
                val first = cells[start]
                val n = cells[end] - first + 1
                val alts = arrayOfNulls<Long>(n)
                val offs = arrayOfNulls<Double>(n)
                var accSum = 0.0
                var accN = 0
                for (k in start..end) {
                    val c = cells[k]
                    val list = byCell.getValue(c)
                    alts[c - first] = ((smooth.getValue(c)) * 10).roundToLong()
                        .coerceIn(TrackWireCodec.ALT_MIN_DM, TrackWireCodec.ALT_MAX_DM)
                    offs[c - first] = list.sumOf { abs(it.off) } / list.size
                    list.forEach { o -> o.acc?.let { accSum += it; accN++ } }
                }
                result += ContribFragment(
                    sectionId = axis.sectionId,
                    axisHash = axis.axisHash,
                    kmFromM = first * STEP_M,
                    stepM = STEP_M,
                    altsDm = alts.toList(),
                    offsetM = offs.toList(),
                    source = if (useBaro) "baro" else "gnss",
                    meanAccM = if (accN > 0) accSum / accN else null,
                )
            }
            start = end + 1
        }
        return result
    }

    private fun median(values: List<Double>): Double {
        val s = values.sorted()
        val n = s.size
        return if (n % 2 == 1) s[n / 2] else (s[n / 2 - 1] + s[n / 2]) / 2.0
    }
}

/** Тело вклада: `data` = zlib(токены varint), 0 - нет данных, иначе `1 + zigzag(дельта в дм от предыдущей известной)`. */
object AltitudeContribCodec {
    fun encodeRaw(altsDm: List<Long?>): ByteArray {
        val out = ArrayList<Byte>()
        var last = 0L
        for (a in altsDm) {
            if (a == null) {
                putVar(out, 0L)
            } else {
                putVar(out, 1L + zigzag(a - last))
                last = a
            }
        }
        return out.toByteArray()
    }

    fun encode(altsDm: List<Long?>, tools: TrackBlobTools): ByteArray = tools.deflate(encodeRaw(altsDm), 6)

    /** Обратное преобразование (для тестов и самопроверки). */
    fun decodeRaw(raw: ByteArray): List<Long?> {
        val res = ArrayList<Long?>()
        var i = 0
        var last = 0L
        while (i < raw.size) {
            var shift = 0
            var v = 0L
            while (true) {
                val b = raw[i++].toInt() and 0xFF
                v = v or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) break
                shift += 7
            }
            if (v == 0L) res += null else {
                last += unzigzag(v - 1)
                res += last
            }
        }
        return res
    }

    private fun zigzag(v: Long): Long = (v shl 1) xor (v shr 63)
    private fun unzigzag(u: Long): Long = (u ushr 1) xor -(u and 1)

    private fun putVar(out: MutableList<Byte>, value: Long) {
        var v = value
        while (v >= 0x80) {
            out += ((v and 0x7F) or 0x80).toByte()
            v = v ushr 7
        }
        out += v.toByte()
    }

    /**
     * Идентификатор вклада для идемпотентности: детерминирован от (сеанс, участок, хэш оси, начало), поэтому повтор после
     * сбоя шлёт тот же `contribId` (сервер отвечает `duplicate`). Сеанс - случайный локальный UUID, с аккаунтом не связан.
     */
    fun contribId(sessionId: String, f: ContribFragment, tools: TrackBlobTools): String {
        val h = tools.sha256Hex("$sessionId|${f.sectionId}|${f.axisHash}|${f.kmFromM}".encodeToByteArray())
        val variant = "89ab"[h[16].digitToInt(16) and 3]
        return "${h.substring(0, 8)}-${h.substring(8, 12)}-4${h.substring(13, 16)}-$variant${h.substring(17, 20)}-${h.substring(20, 32)}"
    }
}
