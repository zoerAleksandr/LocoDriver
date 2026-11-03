package com.z_company.domain.gnss

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Зоны без GPS / с глушением (SCREEN_SPECS §6.9, «Зоны без GPS»; план - docs/GNSS_DEAD_ZONES_PLAN.md).
 * Чистая логика без платформенных зависимостей: один и тот же детектор и одна и та же сетка ячеек
 * используются локальным слоем карты и (позже) вкладом в общую карту. Координаты не логировать.
 */

/** Вид признака плохого сигнала. */
enum class GnssBadKind {
    /** Разрыв записи: между точками трека слишком долго нет данных. */
    GAP,

    /** Точность fix хуже порога [GnssQualityConfig.DETECTOR_MAX_ACCURACY_M] на протяжении участка. */
    POOR_ACCURACY,

    /** Нереальный скачок положения или скорости (спуфинг, помехи). */
    SPEED_JUMP,
}

/** Положение (градусы). */
data class GnssPos(val latitude: Double, val longitude: Double)

/**
 * Участок трека, где GPS недостоверен: от последней достоверной точки [fromMs] до первой достоверной после
 * него [toMs] (если достоверной после нет - до последней недостоверной точки).
 * [anchors] - достоверные опорные точки: до участка и (если есть) после; сами координаты недостоверных точек
 * не используются (при спуфинге они ложные).
 */
data class GnssBadSpan(
    val kind: GnssBadKind,
    val fromMs: Long,
    val toMs: Long,
    val anchors: List<GnssPos>,
) {
    val durationMs: Long get() = toMs - fromMs
}

/** Пороги детектора - в одном месте. */
object GnssDeadZoneConfig {
    /** Разрыв в движении (или скорость неизвестна) больше этого - GAP. Запись в движении идёт раз в ~5 с. */
    const val GAP_MOVING_MS = 30_000L

    /** Разрыв на стоянке больше этого - GAP. Запись на стоянке идёт раз в 60 с, поэтому порог ровно как у «нет данных» на карте. */
    const val GAP_STANDING_MS = TrackAnalysis.DATA_GAP_MS

    /** Разрыв больше этого - не фиксируем: скорее другой сеанс записи/выключенный телефон, а не потеря сигнала. */
    const val GAP_MAX_MS = 20 * 60_000L

    /** Серия точек с плохой точностью короче этого (от последней хорошей до следующей хорошей) - единичный сбой, не фиксируем. */
    const val POOR_MIN_DURATION_MS = 15_000L

    /** Скорость, с которой поезд двигаться не может, км/ч (по смещению между точками и по значению скорости). */
    const val MAX_SPEED_KMH = 300.0
    const val MAX_REPORTED_SPEED_KMH = 250.0

    /** «Телепорт»: смещение не меньше этого и при этом выше [MAX_SPEED_KMH]. */
    const val JUMP_MIN_DISTANCE_M = 300.0

    /** Скачок скорости: изменение не меньше [ACCEL_MIN_DV_KMH] и быстрее [ACCEL_LIMIT_KMH_PER_S] (≈3 м/с²). */
    const val ACCEL_MIN_DV_KMH = 40.0
    const val ACCEL_LIMIT_KMH_PER_S = 11.0

    /** Сравнение с последней достоверной точкой - не дальше этого по времени (потом считаем, что состояние сменилось). */
    const val JUMP_WINDOW_MS = 120_000L

    /** Плохая точность до этого значения ещё годится для оценки положения. */
    const val USABLE_POS_ACCURACY_M = 500f
}

/** Детектор плохих участков трека: чистая функция «точки → участки». */
object GnssDeadZoneDetector {
    private const val OK = 0
    private const val POOR = 1
    private const val JUMP = 2

    /**
     * [points] - записанный (прореженный) трек по времени. Точки с откатом времени игнорируются.
     * Участок без достоверной точки ПЕРЕД ним (трек начался с плохого fix: холодный старт) не фиксируется.
     */
    fun detect(points: List<TrackPoint>): List<GnssBadSpan> {
        val pts = monotonic(points)
        val n = pts.size
        if (n < 2) return emptyList()
        val cls = classify(pts)
        val prevOk = IntArray(n)
        val nextOk = IntArray(n)
        var last = -1
        for (i in 0 until n) {
            if (cls[i] == OK) last = i
            prevOk[i] = last
        }
        var nxt = -1
        for (i in n - 1 downTo 0) {
            if (cls[i] == OK) nxt = i
            nextOk[i] = nxt
        }
        fun pos(i: Int) = GnssPos(pts[i].latitude, pts[i].longitude)
        val out = ArrayList<GnssBadSpan>()

        // Серии недостоверных точек.
        var i = 0
        while (i < n) {
            if (cls[i] == OK) {
                i++
                continue
            }
            var j = i
            while (j + 1 < n && cls[j + 1] == cls[i]) j++
            val ref = if (i > 0) prevOk[i - 1] else -1
            if (ref >= 0) {
                val next = if (j + 1 < n) nextOk[j + 1] else -1
                val endMs = if (next >= 0) pts[next].timeMs else pts[j].timeMs
                val anchors = listOfNotNull(pos(ref), if (next >= 0) pos(next) else null)
                if (cls[i] == POOR) {
                    if (endMs - pts[ref].timeMs >= GnssDeadZoneConfig.POOR_MIN_DURATION_MS) {
                        out += GnssBadSpan(GnssBadKind.POOR_ACCURACY, pts[ref].timeMs, endMs, anchors)
                    }
                } else {
                    out += GnssBadSpan(GnssBadKind.SPEED_JUMP, pts[ref].timeMs, endMs, anchors)
                }
            }
            i = j + 1
        }

        // Разрывы между соседними записанными точками.
        for (k in 1 until n) {
            val a = pts[k - 1]
            val b = pts[k]
            val dt = b.timeMs - a.timeMs
            val standing = a.speedKmh?.let { it < TrackAnalysis.STOP_SPEED_KMH } == true
            val threshold = if (standing) GnssDeadZoneConfig.GAP_STANDING_MS else GnssDeadZoneConfig.GAP_MOVING_MS
            if (dt <= threshold || dt > GnssDeadZoneConfig.GAP_MAX_MS) continue
            val ref = prevOk[k - 1]
            if (ref < 0) continue
            val next = nextOk[k]
            val anchors = listOfNotNull(pos(ref), if (next >= 0) pos(next) else null)
            out += GnssBadSpan(GnssBadKind.GAP, a.timeMs, b.timeMs, anchors)
        }
        return out.sortedBy { it.fromMs }
    }

    private fun monotonic(points: List<TrackPoint>): List<TrackPoint> {
        val out = ArrayList<TrackPoint>(points.size)
        var lastMs = Long.MIN_VALUE
        for (p in points) {
            if (p.timeMs < lastMs) continue
            out += p
            lastMs = p.timeMs
        }
        return out
    }

    private fun classify(pts: List<TrackPoint>): IntArray {
        val cls = IntArray(pts.size)
        var ref: TrackPoint? = null
        for (i in pts.indices) {
            val p = pts[i]
            val acc = p.accuracyM
            if (acc != null && acc > GnssQualityConfig.DETECTOR_MAX_ACCURACY_M) {
                cls[i] = POOR
                continue
            }
            val r = ref
            if (r != null && isJump(r, p)) {
                cls[i] = JUMP
                continue
            }
            cls[i] = OK
            ref = p
        }
        return cls
    }

    private fun isJump(r: TrackPoint, p: TrackPoint): Boolean {
        val dtMs = p.timeMs - r.timeMs
        if (dtMs <= 0 || dtMs > GnssDeadZoneConfig.JUMP_WINDOW_MS) return false
        val dtS = dtMs / 1000.0
        val dist = TrackAnalysis.distanceM(r.latitude, r.longitude, p.latitude, p.longitude)
        if (dist >= GnssDeadZoneConfig.JUMP_MIN_DISTANCE_M && dist / dtS * 3.6 > GnssDeadZoneConfig.MAX_SPEED_KMH) return true
        val v = p.speedKmh ?: return false
        if (v > GnssDeadZoneConfig.MAX_REPORTED_SPEED_KMH) return true
        val rv = r.speedKmh ?: return false
        val dv = abs(v - rv)
        return dv >= GnssDeadZoneConfig.ACCEL_MIN_DV_KMH && dv / dtS > GnssDeadZoneConfig.ACCEL_LIMIT_KMH_PER_S
    }
}

/** Ячейка сетки: строка [iy] по широте, столбец [ix] по долготе. */
data class GnssCell(val iy: Int, val ix: Int)

/**
 * Фиксированная всемирная сетка ячеек ≈250 м (одинаковая для клиента, сервера, PWA и iOS).
 * Строка: floor((lat + 90) / [LAT_STEP_DEG]). Шаг по долготе в строке - целые микроградусы
 * round(2250 / cos(широта центра строки)); столбец: floor((lon + 180) * 1e6 / шаг).
 */
object GnssGrid {
    const val LAT_STEP_DEG = 0.00225
    private const val STEP_MICRO_AT_EQUATOR = 2250.0
    private const val DEG_TO_RAD = PI / 180.0

    fun cellOf(latitude: Double, longitude: Double): GnssCell {
        val lat = latitude.coerceIn(-89.999, 89.999)
        val iy = floor((lat + 90.0) / LAT_STEP_DEG).toInt()
        val lonMicro = floor((longitude.coerceIn(-180.0, 179.999999) + 180.0) * 1e6).toLong()
        return GnssCell(iy, lonMicro.floorDiv(lonStepMicro(iy)).toInt())
    }

    fun center(cell: GnssCell): GnssPos {
        val lat = (cell.iy + 0.5) * LAT_STEP_DEG - 90.0
        val lon = (cell.ix + 0.5) * lonStepMicro(cell.iy) / 1e6 - 180.0
        return GnssPos(lat, lon)
    }

    private fun lonStepMicro(iy: Int): Long {
        val centerLat = (iy + 0.5) * LAT_STEP_DEG - 90.0
        return (STEP_MICRO_AT_EQUATOR / cos(centerLat * DEG_TO_RAD)).roundToLong()
    }
}

/** Агрегат по ячейке для карты. */
data class GnssZone(
    val cell: GnssCell,
    val latitude: Double,
    val longitude: Double,
    /** В скольких РАЗНЫХ поездках (треках) ячейка попала в плохой участок. */
    val tracks: Int,
    val kinds: Set<GnssBadKind>,
    /** Конец последнего плохого участка, мс; для общей карты не используется. */
    val lastMs: Long,
    /** Самый длинный плохой участок, мс. */
    val maxDurationMs: Long,
)

/** Плохие участки одного трека. */
data class GnssTrackSpans(val trackId: String, val spans: List<GnssBadSpan>)

object GnssDeadZones {
    /** Хорда длиннее этого - ячейки только на концах (путь между ними неизвестен). */
    const val CHORD_MAX_M = 3_000.0
    private const val SAMPLE_STEP_M = 100.0

    /** Ячейки, которые накрывает участок: по хорде между опорными точками шагом 100 м. */
    fun cellsOf(span: GnssBadSpan): Set<GnssCell> {
        val a = span.anchors
        if (a.isEmpty()) return emptySet()
        val p = a[0]
        val cells = LinkedHashSet<GnssCell>()
        cells += GnssGrid.cellOf(p.latitude, p.longitude)
        if (a.size < 2) return cells
        val q = a[1]
        cells += GnssGrid.cellOf(q.latitude, q.longitude)
        val d = TrackAnalysis.distanceM(p.latitude, p.longitude, q.latitude, q.longitude)
        if (d > CHORD_MAX_M) return cells
        val steps = ceil(d / SAMPLE_STEP_M).toInt().coerceAtLeast(1)
        for (s in 1 until steps) {
            val t = s.toDouble() / steps
            cells += GnssGrid.cellOf(p.latitude + (q.latitude - p.latitude) * t, p.longitude + (q.longitude - p.longitude) * t)
        }
        return cells
    }

    /**
     * Свести участки всех треков в зоны. Один трек даёт ячейке максимум 1 в [GnssZone.tracks]
     * (повторные срабатывания внутри трека не копятся). [minTracks] - минимум разных поездок.
     */
    fun aggregate(tracks: List<GnssTrackSpans>, minTracks: Int = 1): List<GnssZone> {
        class Acc(var tracks: Int = 0, val kinds: MutableSet<GnssBadKind> = mutableSetOf(), var lastMs: Long = 0L, var maxDur: Long = 0L)
        val acc = HashMap<GnssCell, Acc>()
        for (t in tracks) {
            val seen = HashSet<GnssCell>()
            for (s in t.spans) {
                for (c in cellsOf(s)) {
                    val a = acc.getOrPut(c) { Acc() }
                    if (seen.add(c)) a.tracks++
                    a.kinds += s.kind
                    if (s.toMs > a.lastMs) a.lastMs = s.toMs
                    if (s.durationMs > a.maxDur) a.maxDur = s.durationMs
                }
            }
        }
        return acc.filterValues { it.tracks >= minTracks }.map { (c, a) ->
            val pos = GnssGrid.center(c)
            GnssZone(c, pos.latitude, pos.longitude, a.tracks, a.kinds.toSet(), a.lastMs, a.maxDur)
        }.sortedWith(compareBy({ it.cell.iy }, { it.cell.ix }))
    }
}

/** Тексты карточки зоны (SCREEN_SPECS §6.9, «Зоны без GPS»). */
object GnssDeadZoneText {
    const val TOGGLE = "Зоны без GPS"
    const val TITLE = "Здесь пропадал GPS"
    const val NOTE = "По вашим записанным поездкам. Причина неизвестна: глушение, тоннель, выемка или сбой телефона."
    const val EMPTY = "В ваших поездках нет мест с потерей GPS."

    fun trips(n: Int): String {
        val m100 = n % 100
        val m10 = n % 10
        val word = when {
            m100 in 11..14 -> "поездках"
            m10 == 1 -> "поездке"
            else -> "поездках"
        }
        return "Замечено в $n $word"
    }

    fun kind(k: GnssBadKind): String = when (k) {
        GnssBadKind.GAP -> "нет данных GPS"
        GnssBadKind.POOR_ACCURACY -> "низкая точность"
        GnssBadKind.SPEED_JUMP -> "скачки положения"
    }

    fun kinds(kinds: Set<GnssBadKind>): String =
        GnssBadKind.entries.filter { it in kinds }.joinToString(", ") { kind(it) }.replaceFirstChar { it.uppercase() }

    /** Длительность: «до 4 мин» / «до 45 с». */
    fun duration(ms: Long): String {
        val s = (ms / 1000L).coerceAtLeast(1L)
        return if (s < 90) "до $s с" else "до ${(s + 30) / 60} мин"
    }

    fun date(ms: Long, zone: String): String {
        val t = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.of(zone))
        fun two(n: Int) = n.toString().padStart(2, '0')
        return "${two(t.dayOfMonth)}.${two(t.monthNumber)}.${t.year}"
    }
}
