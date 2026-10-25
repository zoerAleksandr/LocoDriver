package com.z_company.domain.track

import kotlin.math.roundToInt

/*
 * Предупреждения на участке (ограничение скорости, опускание токоприёмников, выключение тока и прочее), которые машинист
 * вносит вручную. Хранятся на устройстве по id участка. Чистая логика KMP.
 */

enum class WarningType(val title: String) {
    SPEED("Ограничение"),
    PANTOGRAPH("Опускание"),
    POWER_OFF("Выключение тока"),
    OTHER("Прочее"),
}

/**
 * Предупреждение. Место задано километрами и пикетами ([startAbsKm], [endAbsKm] - железнодорожные км, 142.4 = 142 км 5 пк);
 * у участка без отметок км - километрами от начала оси ([startRelKm], [endRelKm]).
 */
data class TrackWarning(
    val id: String,
    val sectionId: String,
    val startAbsKm: Double?,
    val endAbsKm: Double?,
    val startRelKm: Double,
    val endRelKm: Double,
    val type: WarningType,
    val speedKmh: Int?,
    val note: String,
    /** Станция или перегон, из которого создано (для списка). */
    val place: String,
) {
    /** Короткая подпись на графике и карте. */
    val label: String
        get() = when (type) {
            WarningType.SPEED -> "ограничение ${speedKmh ?: "?"} км/ч"
            WarningType.PANTOGRAPH -> "опустить токоприёмники"
            WarningType.POWER_OFF -> "выключить ток"
            WarningType.OTHER -> note.ifBlank { "предупреждение" }
        }

    /** Заголовок карточки на карте: «Ограничение 40 км/ч». */
    val title: String get() = label.replaceFirstChar { it.uppercase() }

    /** Текст для озвучки; [meters] - расстояние до начала. */
    fun spoken(meters: Int): String {
        val what = when (type) {
            WarningType.SPEED -> "ограничение скорости ${speedKmh ?: 0} километров в час"
            WarningType.PANTOGRAPH -> "опустите токоприёмники"
            WarningType.POWER_OFF -> "выключите ток"
            WarningType.OTHER -> note.ifBlank { "предупреждение" }
        }
        val extra = if (type != WarningType.OTHER && note.isNotBlank()) ". $note" else ""
        return "Через $meters ${meterWord(meters)} $what$extra"
    }

    private fun meterWord(n: Int): String {
        val a = n % 100
        val b = n % 10
        return if (a in 11..14) "метров" else when (b) { 1 -> "метр"; 2, 3, 4 -> "метра"; else -> "метров" }
    }
}

/** Предупреждение, привязанное к оси выбранного участка: [fromKm] <= [toKm] - км оси. */
data class ResolvedWarning(val warning: TrackWarning, val fromKm: Double, val toKm: Double)

object TrackWarnings {
    /** Км и пикет (1..10) -> железнодорожный км. */
    fun absKm(km: Int, picket: Int): Double = km + (picket - 1) / 10.0

    /** Железнодорожный км -> (км, пикет) для полей ввода. */
    fun split(absKm: Double): Pair<Int, Int> {
        val p = KmMarks.position(absKm)
        return p.km to p.picket
    }

    /** Место на оси участка; null - такого километра на участке нет. */
    fun resolve(section: SectionProfile, w: TrackWarning): ResolvedWarning? {
        if (w.sectionId != section.id) return null
        val a: Double
        val b: Double
        if (w.startAbsKm != null && w.endAbsKm != null && section.kmMarks.isNotEmpty()) {
            a = KmMarks.toRel(section.kmMarks, w.startAbsKm) ?: return null
            b = KmMarks.toRel(section.kmMarks, w.endAbsKm) ?: return null
        } else {
            a = w.startRelKm
            b = w.endRelKm
        }
        return ResolvedWarning(w, minOf(a, b), maxOf(a, b))
    }

    /** Расстояние до ближнего края зоны впереди по ходу, м; null - зона позади или поезд уже внутри. [dir] = +1 к росту км оси. */
    fun distanceAheadM(r: ResolvedWarning, posKm: Double, dir: Int): Double? {
        val d = if (dir >= 0) r.fromKm - posKm else posKm - r.toKm
        return if (d >= 0) d * 1000.0 else null
    }

    fun roundedMeters(m: Double): Int = ((m / 50.0).roundToInt() * 50).coerceAtLeast(50)
}
