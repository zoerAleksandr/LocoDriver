package com.z_company.domain.track

/*
 * Живое положение на профиле участка: выбор участка (ближайший, сверка с плечом), направление, километраж, окно показа.
 * Чистая логика KMP, координаты не логируются.
 */

/** Подбор участка по плечу поезда (поле «Плечо» раздела «Поезд»): названия станций плеча против станций оси. */
object LegMatch {
    /**
     * Участки, на оси которых есть и первая, и последняя станции плеча ([stopNames] по порядку отправление -> прибытие,
     * промежуточные не обязательны). Пусто, если плеча нет (меньше двух названий).
     */
    fun sections(sections: Collection<SectionProfile>, stopNames: List<String>): List<SectionProfile> {
        val from = stopNames.firstOrNull() ?: return emptyList()
        val to = stopNames.lastOrNull()?.takeIf { stopNames.size >= 2 } ?: return emptyList()
        return sections.filter { s -> kmOf(s, from) != null && kmOf(s, to) != null && kmOf(s, from) != kmOf(s, to) }
    }

    /** Подходит ли участок плечу: true / false; null - плеча нет, сверять не с чем. */
    fun fits(section: SectionProfile, stopNames: List<String>): Boolean? {
        if (stopNames.size < 2) return null
        return sections(listOf(section), stopNames).isNotEmpty()
    }

    /**
     * Направление поезда по плечу: false - от начала оси к концу, true - от конца к началу ([SectionProfile] хранит ось в одном
     * направлении, график разворачивается). null - плеча нет или оно не на оси.
     */
    fun reversed(section: SectionProfile, stopNames: List<String>): Boolean? {
        if (stopNames.size < 2) return null
        val a = kmOf(section, stopNames.first()) ?: return null
        val b = kmOf(section, stopNames.last()) ?: return null
        if (a == b) return null
        return a > b
    }

    private fun kmOf(section: SectionProfile, name: String): Double? =
        section.stations.firstOrNull { SectionPicker.sameStation(it.name, name) }?.km
}

/** Кадр для рисования: что под колёсами сейчас. [kmAxis] - позиция на оси участка, км. */
data class LivePosition(
    val kmAxis: Double,
    /** Километровая координата («142 км пк 5 +30 м») по отметкам участка; null - у участка нет отметок. */
    val kmText: String?,
    val element: ProfileElement?,
    val offRoute: Boolean,
    /** Уверенная привязка есть (иначе показывается последнее известное). */
    val locked: Boolean,
)

/** Окно показа вдоль профиля в координатах вида (0 = начало вида; вид развёрнут при [reversed]). */
data class ProfileWindow(val fromX: Double, val toX: Double)

class LiveProfile(val section: SectionProfile, val reversed: Boolean) {
    private val tracker = AxisTracker(section.axis.locator)
    private var last: LivePosition? = null

    val position: LivePosition? get() = last

    /** Длина участка, км (по оси). */
    val lengthKm: Double get() = section.axis.locator.lengthKm.takeIf { it > 0 } ?: section.lengthKm

    /** Ось -> вид: при развороте идём от конца к началу. */
    fun toView(kmAxis: Double): Double = if (reversed) lengthKm - kmAxis else kmAxis

    fun toAxis(viewX: Double): Double = if (reversed) lengthKm - viewX else viewX

    /** Фикс из GPS; возвращает обновлённое положение (или прежнее, если фикс отброшен фильтрами). */
    fun onFix(timeMs: Long, lat: Double, lon: Double, speedKmh: Double?, accuracyM: Float?): LivePosition? {
        val r = tracker.onFix(timeMs, lat, lon, speedKmh, accuracyM)
        val km = r.displayKmM?.div(1000.0) ?: r.kmM?.div(1000.0)
        if (km == null) {
            last = last?.copy(locked = tracker.isLocked)
            return last
        }
        last = LivePosition(
            kmAxis = km,
            kmText = KmMarks.text(section.kmMarks, km),
            element = section.elements.at(km),
            offRoute = r.offRoute,
            locked = tracker.isLocked,
        )
        return last
    }

    /**
     * Окно показа: [behindKm] позади и [aheadKm] впереди по ходу движения вокруг положения (в координатах вида),
     * прижато к краям участка, ширина сохраняется. Без положения - начало участка.
     */
    fun window(behindKm: Double, aheadKm: Double): ProfileWindow {
        val width = (behindKm + aheadKm).coerceAtMost(lengthKm)
        val center = last?.let { toView(it.kmAxis) } ?: behindKm
        val from = (center - behindKm).coerceIn(0.0, (lengthKm - width).coerceAtLeast(0.0))
        return ProfileWindow(from, from + width)
    }

    /** Уклон в виде: при развороте подъём становится спуском. */
    fun gradeInView(e: ProfileElement): Double = if (reversed) -e.gradePermille else e.gradePermille
}
