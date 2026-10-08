package com.z_company.domain.track

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Курс маркера положения на карте поезда (SCREEN_SPECS §6.9, этап 4в), градусы по часовой от севера, 0..360.
 *
 * Правила: курс провайдера ([bearing] из `Location.bearing`) берётся, если он есть; иначе вычисляется по двум точкам
 * (опорная точка и текущая, сдвиг не меньше [MIN_MOVE_M]); при скорости ниже [MIN_SPEED_KMH] сохраняется последний
 * курс; новый курс применяется плавно (сглаживание по кратчайшей дуге, [ALPHA]).
 */
class HeadingEstimator {
    private var shown: Double? = null
    private var refLat = Double.NaN
    private var refLon = Double.NaN

    /** Текущий курс; null — ещё не известен. */
    val heading: Double? get() = shown

    /** Сброс (например, при смене источника положения). */
    fun reset() {
        shown = null
        refLat = Double.NaN
        refLon = Double.NaN
    }

    /** Новая точка положения; возвращает текущий (сглаженный) курс или null, если он пока неизвестен. */
    fun update(lat: Double, lon: Double, speedKmh: Double?, bearing: Float?): Double? {
        val slow = speedKmh != null && speedKmh < MIN_SPEED_KMH
        if (slow) {
            // Стоим: курс сохраняем, опорную точку двигаем, чтобы дрейф GPS на месте не дал «курс».
            refLat = lat
            refLon = lon
            return shown
        }
        val raw: Double? = if (bearing != null) {
            refLat = lat
            refLon = lon
            normalize(bearing.toDouble())
        } else if (refLat.isNaN()) {
            refLat = lat
            refLon = lon
            null
        } else if (TrackAnalysis.distanceM(refLat, refLon, lat, lon) >= MIN_MOVE_M) {
            val b = bearingBetween(refLat, refLon, lat, lon)
            refLat = lat
            refLon = lon
            b
        } else {
            null
        }
        if (raw != null) {
            val cur = shown
            shown = if (cur == null) raw else smooth(cur, raw, ALPHA)
        }
        return shown
    }

    companion object {
        const val MIN_SPEED_KMH = 2.0
        const val MIN_MOVE_M = 5.0
        const val ALPHA = 0.5

        fun normalize(deg: Double): Double = ((deg % 360.0) + 360.0) % 360.0

        /** Начальный азимут от точки 1 к точке 2 (градусы по часовой от севера). */
        fun bearingBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val p1 = lat1 * PI / 180.0
            val p2 = lat2 * PI / 180.0
            val dl = (lon2 - lon1) * PI / 180.0
            val y = sin(dl) * cos(p2)
            val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
            return normalize(atan2(y, x) * 180.0 / PI)
        }

        /** Сдвиг [from] к [to] на долю [alpha] по кратчайшей дуге (учитывает переход через 0/360). */
        fun smooth(from: Double, to: Double, alpha: Double): Double {
            var d = normalize(to) - normalize(from)
            if (d > 180.0) d -= 360.0
            if (d < -180.0) d += 360.0
            return normalize(from + d * alpha)
        }
    }
}
