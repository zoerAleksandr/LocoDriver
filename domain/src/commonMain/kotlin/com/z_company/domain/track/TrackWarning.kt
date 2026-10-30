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

    /** Подпись на профиле пути: у ограничения скорости - только число («40»), у остальных видов - текст предупреждения. */
    val chartLabel: String
        get() = if (type == WarningType.SPEED) "${speedKmh ?: "?"}" else label

    /** Заголовок карточки на карте: «Ограничение 40 км/ч». */
    val title: String get() = label.replaceFirstChar { it.uppercase() }

    /** Что делать/что за предупреждение - для озвучки (без расстояния). */
    private val instruction: String
        get() = when (type) {
            WarningType.SPEED -> "ограничение скорости на ${speedKmh ?: 0}"
            WarningType.PANTOGRAPH -> "опустите токоприёмники"
            WarningType.POWER_OFF -> "выключите ток"
            WarningType.OTHER -> note.ifBlank { "предупреждение" }
        }

    private val noteSuffix: String get() = if (type != WarningType.OTHER && note.isNotBlank()) ". $note" else ""

    /** Текст для озвучки; [meters] - расстояние до начала. */
    fun spoken(meters: Int): String = "Через $meters ${meterWord(meters)} $instruction$noteSuffix"

    /**
     * Озвучка при заходе головы поезда в зону: у ограничения скорости - «Ограничение скорости на 40» (выход озвучивается
     * отдельно, [spokenExit]); у остальных видов - сама инструкция («Опустите токоприёмники»).
     */
    fun spokenEnter(): String =
        if (type == WarningType.SPEED) "Ограничение скорости на ${speedKmh ?: 0}$noteSuffix" else "${instruction.replaceFirstChar { it.uppercase() }}$noteSuffix"

    /** Озвучка выхода: только у ограничения скорости, когда зону проследовал весь поезд; у остальных видов выхода нет. */
    fun spokenExit(): String? =
        if (type == WarningType.SPEED) "Конец ограничения скорости на ${speedKmh ?: 0}" else null

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

    /**
     * Место на оси участка; null - такого километра на участке нет. Если километровая координата на участке встречается несколько раз
     * (смена нумерации), выбирается вхождение, ближайшее к месту предупреждения ([placeRange]: станция или перегон); километр может
     * относиться к соседнему месту - берётся ближайшее, а не только лежащее внутри. Конец зоны - вхождение, ближайшее к началу. Место
     * неизвестно - первое по ходу участка, как раньше.
     */
    fun resolve(section: SectionProfile, w: TrackWarning, legStops: List<com.z_company.domain.util.TrainMapStop> = emptyList()): ResolvedWarning? {
        if (w.sectionId != section.id) return null
        val a: Double
        val b: Double
        if (w.startAbsKm != null && w.endAbsKm != null && section.kmMarks.isNotEmpty()) {
            val ac = KmMarks.candidates(section.kmMarks, w.startAbsKm)
            val bc = KmMarks.candidates(section.kmMarks, w.endAbsKm)
            val range = placeRange(section, w, legStops)
            a = if (ac.size > 1 && range != null) ac.minByOrNull { distanceTo(range, it) }!! else KmMarks.toRel(section.kmMarks, w.startAbsKm) ?: return null
            b = if (bc.size > 1) bc.minByOrNull { kotlin.math.abs(it - a) }!! else KmMarks.toRel(section.kmMarks, w.endAbsKm) ?: return null
        } else {
            a = w.startRelKm
            b = w.endRelKm
        }
        return ResolvedWarning(w, minOf(a, b), maxOf(a, b))
    }

    private fun distanceTo(range: Pair<Double, Double>, km: Double): Double = when {
        km < range.first -> range.first - km
        km > range.second -> km - range.second
        else -> 0.0
    }

    /** Сообщение, когда ограничение попадает туда, где километраж неизвестен. */
    const val UNKNOWN_KM_MESSAGE =
        "На этом участке режимная карта уточняется: километраж здесь неизвестен, поставить ограничение нельзя. Свяжитесь с поддержкой, чтобы уточнить информацию."

    /**
     * Можно ли ставить предупреждение в найденное место: не за пределами оси участка (километр «вытянут» за край продолжением нумерации) и не в
     * серой зоне профиля (`noData`), где километраж неизвестен. null - можно; иначе текст для пользователя.
     */
    fun placementIssue(section: SectionProfile, r: ResolvedWarning): String? {
        if (r.fromKm < -0.05 || r.toKm > section.lengthKm + 0.05) return UNKNOWN_KM_MESSAGE
        if (section.elements.any { it.noData && it.endKm > r.fromKm && it.startKm < r.toKm }) return UNKNOWN_KM_MESSAGE
        return null
    }

    /** Станция плеча дальше этого от оси участка на нём «не лежит» (м). */
    const val STATION_MAX_OFFSET_M = 500.0

    /**
     * Километр оси, где находится станция [name]: сначала по станциям плеча [legStops] (полный список, с координатами - проекция на ось не
     * зависит от написания названия), затем по станциям самого участка (на нём часть станций может отсутствовать). null - станции на оси нет.
     */
    fun stationKm(section: SectionProfile, name: String, legStops: List<com.z_company.domain.util.TrainMapStop>): Double? {
        legStops.firstOrNull { SectionPicker.sameStation(it.name, name) }?.let { stop ->
            val hit = section.axis.locator.project(stop.latitude, stop.longitude)
            if (hit != null && hit.offsetM <= STATION_MAX_OFFSET_M) return hit.kmM / 1000.0
        }
        return section.stations.firstOrNull { SectionPicker.sameStation(it.name, name) }?.km
    }

    /**
     * Диапазон оси, где находится место предупреждения: сохранённые границы места (создано на профиле), иначе по названию - станция
     * («Луга») или перегон («Лужская — Луга») по станциям плеча и участка ([stationKm]). null - место не найдено на участке.
     */
    fun placeRange(section: SectionProfile, w: TrackWarning, legStops: List<com.z_company.domain.util.TrainMapStop> = emptyList()): Pair<Double, Double>? {
        if (w.endRelKm > w.startRelKm + 1e-9) return w.startRelKm to w.endRelKm
        val names = w.place.split(" — ").map { it.trim() }.filter { it.isNotEmpty() }
        if (names.isEmpty() || names.size > 2) return null
        val kms = names.map { n -> stationKm(section, n, legStops) ?: return null }
        return kms.min() to kms.max()
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

    /** Предупреждение проследовано (заход для видов без выхода, выход всем составом - у ограничения скорости) или осталось позади. */
    fun isDone(id: String): Boolean = states[id] == State.DONE

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

/** Приоритеты голосовых сообщений ДУ-61: меньше число - раньше. */
enum class WarningSpeechPriority { ENTER, AHEAD, EXIT, NO_MORE }

/** Фраза, когда последнее предупреждение проследовано. */
const val WARNING_SPEECH_NO_MORE = "Больше предупреждений нет"

/**
 * Очередь голосовых сообщений: говорится сперва заход головы на место ограничения, потом лежащее впереди, потом выход всем
 * составом, потом «больше предупреждений нет»; внутри одного приоритета - в порядке поступления. Одинаковое сообщение,
 * уже стоящее в очереди, повторно не добавляется. Не потокобезопасна.
 */
class WarningSpeechQueue {
    private class Item(val seq: Long, val text: String, val priority: WarningSpeechPriority)

    private val items = ArrayList<Item>()
    private var seq = 0L

    fun add(text: String, priority: WarningSpeechPriority) {
        if (items.any { it.text == text && it.priority == priority }) return
        items += Item(seq++, text, priority)
    }

    /** Следующее сообщение или null. */
    fun poll(): String? {
        val next = items.minWithOrNull(compareBy<Item> { it.priority.ordinal }.thenBy { it.seq }) ?: return null
        items.remove(next)
        return next.text
    }

    fun clear() = items.clear()

    val isEmpty: Boolean get() = items.isEmpty()
}
