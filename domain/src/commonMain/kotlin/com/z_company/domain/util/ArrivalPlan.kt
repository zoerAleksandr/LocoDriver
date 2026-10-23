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

/**
 * План прибытия («Цель»): точка и плановый момент (мс, UTC). Хранится локально, на сервер не уходит.
 * [onStop] - прибытие на остановку: за [stopMin] минут до планового момента поезд должен подойти к точке (торможение);
 * [showOnMap] - показывать расчёт на экране карты.
 */
data class ArrivalPlanData(
    val target: PlanTarget,
    val plannedMs: Long,
    val onStop: Boolean = false,
    val stopMin: Int = ArrivalPlan.DEFAULT_STOP_MIN,
    val showOnMap: Boolean = true,
)

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

    /** Время на торможение и остановку по умолчанию, мин. */
    const val DEFAULT_STOP_MIN = 3

    /** Среднее ускорение разгона поезда, м/с² (поправка на разгон от начальной скорости до нужной). */
    const val ACCEL_MS2 = 0.2

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

    /** Станция плеча впереди и расстояние до неё вдоль линии. */
    data class StopAhead(val stop: TrainMapStop, val target: PlanTarget, val distanceM: Double)

    /** Станции плеча впереди текущего положения ([ARRIVED_M] и дальше) с расстоянием вдоль линии; пусто, если положение не на линии. */
    fun stopsAhead(stops: List<TrainMapStop>, line: List<Pair<Double, Double>>?, fix: GeoFix?): List<StopAhead> {
        if (line == null || line.size < 2 || fix == null) return emptyList()
        val cum = TrainMapRoute.cumulativeM(line)
        val me = TrainMapRoute.project(line, cum, fix.latitude, fix.longitude) ?: return emptyList()
        if (me.offsetM > TrainMapRoute.SNAP_TOLERANCE_M) return emptyList()
        return stops.mapNotNull { st ->
            val p = TrainMapRoute.project(line, cum, st.latitude, st.longitude) ?: return@mapNotNull null
            val d = p.alongM - me.alongM
            if (d <= ARRIVED_M) null
            else StopAhead(st, PlanTarget(p.latitude, p.longitude, st.name.takeIf { it.isNotBlank() }), d)
        }
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
        /** Плановое время (с учётом времени на остановку) уже прошло. */
        OVERDUE,
        /** Время ещё есть, но с разгоном от текущей скорости не успеть ни при какой скорости хода. */
        IMPOSSIBLE,
    }

    data class Evaluation(
        /** Нужная средняя скорость хода (после разгона), км/ч; null - время вышло. */
        val requiredKmh: Double?,
        val status: Status,
        /** На сколько км/ч нужно быстрее текущей (только [Status.BEHIND]), округлено вверх до целого. */
        val faster: Int?,
        /** Ожидаемое прибытие при текущей скорости; null - скорость неизвестна. */
        val expectedMs: Long?,
    )

    /**
     * Нужная скорость хода v: поезд со скоростью v0 ([speedKmh]) разгоняется до v с ускорением [ACCEL_MS2] и идёт дальше,
     * укладываясь в T = плановое - сейчас - [stopMin] минут: d/v + (v-v0)²/(2·a·v) = T (потеря времени на разгон).
     * Если нужная скорость не выше v0, разгон не нужен: v = d/T. v0 неизвестна - расчёт без разгона.
     * Сравнение с [speedKmh] (живая или средняя, как у ETA ленты, [TrainMapRoute.etaSpeedKmh]; null - неизвестна).
     */
    fun evaluate(nowMs: Long, distanceM: Double, plannedMs: Long, speedKmh: Double?, stopMin: Int = 0): Evaluation {
        val expected = TrainMapRoute.Eta.arrivalMs(nowMs, distanceM, speedKmh)
        val leftMs = plannedMs - nowMs - stopMin * 60_000L
        if (leftMs <= 0) return Evaluation(null, Status.OVERDUE, null, expected)
        val t = leftMs / 1000.0
        val plain = distanceM / t * 3.6
        if (speedKmh == null || speedKmh <= 0.0) return Evaluation(plain, Status.UNKNOWN, null, expected)
        val v0 = speedKmh / 3.6
        val required: Double
        if (plain <= speedKmh) {
            required = plain
        } else {
            val a = ACCEL_MS2
            val b = v0 + a * t
            val disc = b * b - v0 * v0 - 2 * a * distanceM
            if (disc < 0) return Evaluation(plain, Status.IMPOSSIBLE, null, expected)
            required = (b - kotlin.math.sqrt(disc)) * 3.6
        }
        return when {
            speedKmh >= required * AHEAD_FACTOR -> Evaluation(required, Status.AHEAD, null, expected)
            speedKmh >= required -> Evaluation(required, Status.ON_TIME, null, expected)
            else -> Evaluation(required, Status.BEHIND, kotlin.math.ceil(required - speedKmh).toInt().coerceAtLeast(1), expected)
        }
    }

    // ─── Хранение ────────────────────────────────────────────────────────

    private const val SEP = '\t'

    /** Строка для локального хранилища: «lat TAB lon TAB plannedMs TAB onStop TAB stopMin TAB showOnMap TAB name». */
    fun encode(plan: ArrivalPlanData): String =
        listOf(
            plan.target.latitude, plan.target.longitude, plan.plannedMs,
            if (plan.onStop) 1 else 0, plan.stopMin, if (plan.showOnMap) 1 else 0,
            plan.target.name.orEmpty().replace(SEP, ' '),
        ).joinToString(SEP.toString())

    /** Читает и прежний формат «lat TAB lon TAB plannedMs TAB name» (без остановки, расчёт показывается на карте). */
    fun decode(raw: String?): ArrivalPlanData? {
        if (raw.isNullOrEmpty()) return null
        val p = raw.split(SEP, limit = 7)
        if (p.size < 4) return null
        val lat = p[0].toDoubleOrNull() ?: return null
        val lon = p[1].toDoubleOrNull() ?: return null
        val ms = p[2].toLongOrNull() ?: return null
        if (p.size < 7) return ArrivalPlanData(PlanTarget(lat, lon, p[3].takeIf { it.isNotEmpty() }), ms)
        return ArrivalPlanData(
            PlanTarget(lat, lon, p[6].takeIf { it.isNotEmpty() }), ms,
            onStop = p[3] == "1",
            stopMin = p[4].toIntOrNull()?.coerceIn(0, 60) ?: DEFAULT_STOP_MIN,
            showOnMap = p[5] != "0",
        )
    }
}
