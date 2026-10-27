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

    /** Что делать/что за предупреждение - для озвучки (без расстояния). */
    private val instruction: String
        get() = when (type) {
            WarningType.SPEED -> "ограничение скорости ${speedKmh ?: 0} километров в час"
            WarningType.PANTOGRAPH -> "опустите токоприёмники"
            WarningType.POWER_OFF -> "выключите ток"
            WarningType.OTHER -> note.ifBlank { "предупреждение" }
        }

    private val noteSuffix: String get() = if (type != WarningType.OTHER && note.isNotBlank()) ". $note" else ""

    /** Текст для озвучки; [meters] - расстояние до начала. */
    fun spoken(meters: Int): String = "Через $meters ${meterWord(meters)} $instruction$noteSuffix"

    /**
     * Озвучка при заходе головы поезда в зону: у ограничения скорости - «Начало ограничения скорости 40 километров в час»
     * (выход озвучивается отдельно, [spokenExit]); у остальных видов - сама инструкция («Опустите токоприёмники»).
     */
    fun spokenEnter(): String =
        if (type == WarningType.SPEED) "Начало ограничения скорости ${speedKmh ?: 0} километров в час$noteSuffix" else "${instruction.replaceFirstChar { it.uppercase() }}$noteSuffix"

    /** Озвучка выхода: только у ограничения скорости, когда зону проследовал весь поезд; у остальных видов выхода нет. */
    fun spokenExit(): String? =
        if (type == WarningType.SPEED) "Конец ограничения скорости ${speedKmh ?: 0} километров в час" else null

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

/** Событие проезда зоны предупреждения. */
enum class WarningEvent { ENTER, EXIT }

/**
 * Следит за проездом предупреждений: «зашли головой» ([WarningEvent.ENTER]) и «проследовали всей длиной» ([WarningEvent.EXIT],
 * только у ограничения скорости; у остальных видов событие одно - заход). Состояние на предупреждение: не видели - до зоны -
 * в зоне (ждём выход хвоста) - закончено. Если первый раз головы уже внутри - заход озвучивается; уже за зоной - молчим.
 * Скачок через короткую зону между двумя фиксами даёт оба события сразу. Смена участка или направления - [reset].
 */
class WarningPassTracker {
    private enum class State { BEFORE, INSIDE, DONE }

    private val states = HashMap<String, State>()

    fun reset() = states.clear()

    fun forget(id: String) { states.remove(id) }

    /**
     * [headKm] - голова на оси участка, [dir] = +1 к росту км оси, [lengthKm] - длина поезда (null - 0, выход по голове).
     * События возвращаются в порядке ENTER, EXIT.
     */
    fun update(r: ResolvedWarning, headKm: Double, dir: Int, lengthKm: Double?): List<WarningEvent> {
        val id = r.warning.id
        val head = headSide(r, headKm, dir)
        val speed = r.warning.type == WarningType.SPEED
        val out = ArrayList<WarningEvent>(2)
        var st = states[id]
        if (st == null) {
            if (head == Side.AFTER) { states[id] = State.DONE; return out }
            st = State.BEFORE
        }
        if (st == State.BEFORE && head != Side.BEFORE) {
            out += WarningEvent.ENTER
            st = if (speed) State.INSIDE else State.DONE
        }
        if (st == State.INSIDE && head == Side.AFTER && tailClear(r, headKm, dir, lengthKm ?: 0.0)) {
            out += WarningEvent.EXIT
            st = State.DONE
        }
        states[id] = st
        return out
    }

    private enum class Side { BEFORE, INSIDE, AFTER }

    private fun headSide(r: ResolvedWarning, p: Double, dir: Int): Side =
        if (dir >= 0) when { p < r.fromKm -> Side.BEFORE; p > r.toKm -> Side.AFTER; else -> Side.INSIDE }
        else when { p > r.toKm -> Side.BEFORE; p < r.fromKm -> Side.AFTER; else -> Side.INSIDE }

    /** Хвост (позади головы на длину поезда) вышел за дальний край зоны. */
    private fun tailClear(r: ResolvedWarning, p: Double, dir: Int, lengthKm: Double): Boolean =
        if (dir >= 0) p - lengthKm >= r.toKm else p + lengthKm <= r.fromKm
}
