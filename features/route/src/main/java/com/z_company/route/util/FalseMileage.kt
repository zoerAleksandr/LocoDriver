package com.z_company.route.util

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Защита от «ложного километража» (SCREEN_SPECS §6.6): неверные координаты станции давали крюк на сотни км
 * (710 км вместо 165). Результат замера/автоподбора нельзя записывать молча, если путь больше 1,5× введённого
 * расстояния (введённое > 0) либо больше 3× суммы прямых между станциями плеча.
 */
object FalseMileage {
    const val ENTERED_FACTOR = 1.5
    const val STRAIGHT_FACTOR = 3.0

    /** Подозрительный ли результат: [lengthKm] — длина пути, [enteredKm] — введённое расстояние (0 — не введено). */
    fun isSuspicious(lengthKm: Double, enteredKm: Int, straightSumKm: Double): Boolean {
        if (lengthKm <= 0.0) return false
        if (enteredKm > 0 && lengthKm > ENTERED_FACTOR * enteredKm) return true
        return straightSumKm > 0.0 && lengthKm > STRAIGHT_FACTOR * straightSumKm
    }

    /** Сумма прямых (км) между соседними точками (lat, lon) по порядку. */
    fun straightSumKm(points: List<Pair<Double, Double>>): Double =
        points.zipWithNext().sumOf { (a, b) -> haversineKm(a.first, a.second, b.first, b.second) }

    /** «Было введено» для текста: введённое расстояние, а если его нет — округлённая сумма прямых. */
    fun expectedKm(enteredKm: Int, straightSumKm: Double): Int =
        if (enteredKm > 0) enteredKm else round(straightSumKm).toInt()

    fun message(lengthKm: Double, enteredKm: Int, straightSumKm: Double): String =
        "Расстояние намного больше введённого: ${round(lengthKm).toInt()} км вместо ${expectedKm(enteredKm, straightSumKm)} км. " +
            "Возможно, неверно указана одна из станций — проверьте станции."

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dp = p2 - p1
        val dl = (lon2 - lon1) * PI / 180
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * r * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
