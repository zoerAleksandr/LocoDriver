package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** Точка плана прибытия на рельсовой линии: координаты уже привязаны к линии; [name] - станция плеча или null. */
data class PlanTarget(val latitude: Double, val longitude: Double, val name: String? = null)

/** План прибытия: точка и плановый момент (мс, UTC). Хранится локально, на сервер не уходит. */
data class ArrivalPlanData(val target: PlanTarget, val plannedMs: Long)

/**
 * Планирование прибытия на карте поезда (SCREEN_SPECS §6.9, состояние 7): расстояние вдоль рельсовой линии до
 * выбранной точки впереди, нужная средняя скорость, сравнение с текущей. Чистая логика без платформенных зависимостей.
 */
object ArrivalPlan {
    /** Ближе этого расстояния (м) точка считается достигнутой: план снимается; точки ближе плана не создают. */
    const val ARRIVED_M = 100.0

    /** План, плановое время которого прошло больше чем на это время, считается устаревшим и снимается (мс). */
    const val STALE_MS = 12 * 3_600_000L

    /** Допуск на скорость «опережаете»: текущая скорость не меньше нужной × этот множитель. */
    const val AHEAD_FACTOR = 1.15

    /** Допуск, дальше которого сохранённая точка плана не считается лежащей на линии, м. */
    const val TARGET_ON_LINE_M = 200.0

    /** Результат разбора тапа на карте. */
    sealed interface TapResult {
        /** Точка впереди по плечу; [distanceM] - вдоль линии от текущего положения. */
        data class Ahead(val target: PlanTarget, val distanceM: Double) : TapResult
        /** Точка позади или ближе [ARRIVED_M]. */
        data object NotAhead : TapResult
        /** Тап не на линии и не у станции плеча. */
        data object NotOnLine : TapResult
        /** Положение дальше 50 м от линии - расстояние по рельсам не определить. */
        data object OffRail : TapResult
        /** Положение неизвестно. */
        data object NoFix : TapResult
    }

    /**
     * Что означает тап ([tapLat], [tapLon]) на карте. Станция плеча в пределах [stationTolM] от касания имеет приоритет,
     * иначе точка берётся на рельсовой линии в пределах [lineTolM] (привязывается к линии).
     */
    fun resolveTap(
        stops: List<TrainMapStop>,
        line: List<Pair<Double, Double>>?,
        fix: GeoFix?,
        tapLat: Double,
        tapLon: Double,
        lineTolM: Double,
        stationTolM: Double,
    ): TapResult {
        if (line == null || line.size < 2) return TapResult.NotOnLine
        val cum = TrainMapRoute.cumulativeM(line)
        val station = stops
            .map { it to TrackAnalysis.distanceM(tapLat, tapLon, it.latitude, it.longitude) }
            .filter { it.second <= stationTolM }
            .minByOrNull { it.second }?.first
        val cand: TrainMapRoute.LineProjection
        val name: String?
        if (station != null) {
            cand = TrainMapRoute.project(line, cum, station.latitude, station.longitude) ?: return TapResult.NotOnLine
            name = station.name.takeIf { it.isNotBlank() }
        } else {
            cand = TrainMapRoute.project(line, cum, tapLat, tapLon) ?: return TapResult.NotOnLine
            if (cand.offsetM > lineTolM) return TapResult.NotOnLine
            name = null
        }
        if (fix == null) return TapResult.NoFix
        val me = TrainMapRoute.project(line, cum, fix.latitude, fix.longitude) ?: return TapResult.NoFix
        if (me.offsetM > TrainMapRoute.SNAP_TOLERANCE_M) return TapResult.OffRail
        val d = cand.alongM - me.alongM
        if (d <= ARRIVED_M) return TapResult.NotAhead
        return TapResult.Ahead(PlanTarget(cand.latitude, cand.longitude, name), d)
    }

    /** Расстояние до точки плана в данный момент. */
    sealed interface PlanDistance {
        data class Ahead(val meters: Double) : PlanDistance
        /** Точка достигнута или пройдена - план снимается. */
        data object Arrived : PlanDistance
        /** Положение дальше 50 м от линии. */
        data object OffRail : PlanDistance
        /** Точка плана не лежит на текущей линии (сменили плечо) - план снимается. */
        data object TargetNotOnLine : PlanDistance
        data object NoFix : PlanDistance
        data object NoLine : PlanDistance
    }

    fun distance(line: List<Pair<Double, Double>>?, fix: GeoFix?, target: PlanTarget): PlanDistance {
        if (line == null || line.size < 2) return PlanDistance.NoLine
        val cum = TrainMapRoute.cumulativeM(line)
        val t = TrainMapRoute.project(line, cum, target.latitude, target.longitude) ?: return PlanDistance.NoLine
        if (t.offsetM > TARGET_ON_LINE_M) return PlanDistance.TargetNotOnLine
        if (fix == null) return PlanDistance.NoFix
        val me = TrainMapRoute.project(line, cum, fix.latitude, fix.longitude) ?: return PlanDistance.NoFix
        if (me.offsetM > TrainMapRoute.SNAP_TOLERANCE_M) return PlanDistance.OffRail
        val d = t.alongM - me.alongM
        return if (d <= ARRIVED_M) PlanDistance.Arrived else PlanDistance.Ahead(d)
    }

    fun isStale(plan: ArrivalPlanData, nowMs: Long): Boolean = nowMs - plan.plannedMs > STALE_MS

    // ─── Время ───────────────────────────────────────────────────────────

    /** Разбор «чч:мм» (допустимо «ччмм»/«чмм» без двоеточия): часы 0-23, минуты 0-59; null - неверный ввод. */
    fun parseTime(text: String): Pair<Int, Int>? {
        val s = text.trim()
        val digits: String
        if (':' in s) {
            val parts = s.split(':')
            if (parts.size != 2 || parts[0].length !in 1..2 || parts[1].length != 2) return null
            digits = parts[0].padStart(2, '0') + parts[1]
        } else {
            if (s.length !in 3..4) return null
            digits = s.padStart(4, '0')
        }
        if (!digits.all { it in '0'..'9' }) return null
        val h = digits.substring(0, 2).toInt()
        val m = digits.substring(2, 4).toInt()
        return if (h in 0..23 && m in 0..59) h to m else null
    }

    /** Плановый момент. [earlier] - введённое время не позже текущего (тогда [ms] - те же часы следующих суток). */
    data class Planned(val ms: Long, val earlier: Boolean)

    /**
     * Момент «чч:мм» в поясе [zone] на сегодняшнюю дату (по [nowMs]). Если он не позже текущего, [Planned.earlier] = true,
     * а [Planned.ms] - тот же час на следующие сутки (применяется только после подтверждения пользователя).
     */
    fun planned(nowMs: Long, hour: Int, minute: Int, zone: String): Planned {
        val tz = TimeZone.of(zone)
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val todayMs = LocalDateTime(today.year, today.month, today.dayOfMonth, hour, minute).toInstant(tz).toEpochMilliseconds()
        if (todayMs > nowMs) return Planned(todayMs, false)
        val next = today.plus(DatePeriod(days = 1))
        val nextMs = LocalDateTime(next.year, next.month, next.dayOfMonth, hour, minute).toInstant(tz).toEpochMilliseconds()
        return Planned(nextMs, true)
    }

    /** На сколько суток [ms] позже сегодняшней даты [nowMs] в поясе [zone] (0 - сегодня). */
    fun dayOffset(nowMs: Long, ms: Long, zone: String): Int {
        val tz = TimeZone.of(zone)
        val a = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz).date
        val b = Instant.fromEpochMilliseconds(ms).toLocalDateTime(tz).date
        return (b.toEpochDays() - a.toEpochDays()).toInt()
    }

    // ─── Расчёт ──────────────────────────────────────────────────────────

    enum class Status {
        /** Текущая скорость неизвестна. */
        UNKNOWN,
        /** Текущая скорость ≥ нужной × [AHEAD_FACTOR]. */
        AHEAD,
        /** Нужная ≤ текущая < нужная × [AHEAD_FACTOR]. */
        ON_TIME,
        /** Текущая меньше нужной. */
        BEHIND,
        /** Плановое время уже прошло. */
        OVERDUE,
    }

    data class Evaluation(
        /** Нужная средняя скорость, км/ч; null - плановое время прошло. */
        val requiredKmh: Double?,
        val status: Status,
        /** На сколько км/ч нужно быстрее текущей (только [Status.BEHIND]), округлено вверх до целого. */
        val faster: Int?,
        /** Ожидаемое прибытие при текущей скорости; null - скорость неизвестна. */
        val expectedMs: Long?,
    )

    /**
     * Нужная скорость = расстояние / (плановое - сейчас); сравнение с [speedKmh] (живая или средняя, как у ETA ленты,
     * [TrainMapRoute.etaSpeedKmh]; null - неизвестна).
     */
    fun evaluate(nowMs: Long, distanceM: Double, plannedMs: Long, speedKmh: Double?): Evaluation {
        val expected = TrainMapRoute.Eta.arrivalMs(nowMs, distanceM, speedKmh)
        val leftMs = plannedMs - nowMs
        if (leftMs <= 0) return Evaluation(null, Status.OVERDUE, null, expected)
        val required = (distanceM / 1000.0) / (leftMs / 3_600_000.0)
        if (speedKmh == null || speedKmh <= 0.0) return Evaluation(required, Status.UNKNOWN, null, expected)
        return when {
            speedKmh >= required * AHEAD_FACTOR -> Evaluation(required, Status.AHEAD, null, expected)
            speedKmh >= required -> Evaluation(required, Status.ON_TIME, null, expected)
            else -> Evaluation(required, Status.BEHIND, kotlin.math.ceil(required - speedKmh).toInt().coerceAtLeast(1), expected)
        }
    }

    // ─── Хранение ────────────────────────────────────────────────────────

    private const val SEP = '\t'

    /** Строка для локального хранилища: «lat TAB lon TAB plannedMs TAB name». */
    fun encode(plan: ArrivalPlanData): String =
        listOf(plan.target.latitude, plan.target.longitude, plan.plannedMs, plan.target.name.orEmpty().replace(SEP, ' '))
            .joinToString(SEP.toString())

    fun decode(raw: String?): ArrivalPlanData? {
        if (raw.isNullOrEmpty()) return null
        val p = raw.split(SEP, limit = 4)
        if (p.size < 4) return null
        val lat = p[0].toDoubleOrNull() ?: return null
        val lon = p[1].toDoubleOrNull() ?: return null
        val ms = p[2].toLongOrNull() ?: return null
        return ArrivalPlanData(PlanTarget(lat, lon, p[3].takeIf { it.isNotEmpty() }), ms)
    }
}
