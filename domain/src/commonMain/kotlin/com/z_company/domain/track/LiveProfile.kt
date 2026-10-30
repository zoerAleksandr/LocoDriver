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

    /**
     * Участки-кандидаты для плеча [from] - [to] из [sections] (уже подгруженные оси). Сначала строгое правило [fits] (на оси есть обе
     * крайние станции плеча по названию). Нет таких - мягкое: ось проходит рядом (не дальше [RELAXED_M]) и с отправлением, и с прибытием
     * ([fromPoint]/[toPoint], если координаты известны): так же, как карта, которая подбирает участок по положению и показывает ближайший,
     * даже когда названия станций на оси записаны иначе (Луга I / Луга-1 и т.п.).
     */
    fun candidates(
        sections: Collection<SectionProfile>,
        from: String,
        to: String,
        fromPoint: Pair<Double, Double>?,
        toPoint: Pair<Double, Double>?,
    ): List<SectionProfile> {
        val names = listOf(from, to)
        val strict = sections.filter { fits(it, names) == true }
        if (strict.isNotEmpty() || fromPoint == null || toPoint == null) return strict
        fun near(s: SectionProfile, p: Pair<Double, Double>) =
            SectionPicker.nearest(listOf(s.axis), p.first, p.second, maxDistanceM = RELAXED_M, limit = 1).isNotEmpty()
        return sections.filter { near(it, fromPoint) && near(it, toPoint) }
    }

    const val RELAXED_M = 2_000.0

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
    /** Километровая координата («142 км 5 пк 30 м») по отметкам участка; null - у участка нет отметок. */
    val kmText: String?,
    val element: ProfileElement?,
    val offRoute: Boolean,
    /** Уверенная привязка есть (иначе показывается последнее известное). */
    val locked: Boolean,
)

/** Результат подбора поправки по введённому километру и пикету. */
sealed interface Calibration {
    /** [shiftKm] - новая поправка по оси; [diffKm] - на сколько введённое место отличается от показываемого сейчас. */
    data class Ok(val shiftKm: Double, val diffKm: Double) : Calibration
    /** Нет положения по GPS - не от чего считать поправку. */
    data object NoPosition : Calibration
    /** Такого километра нет на этом участке. */
    data object NotOnSection : Calibration
}

/** Окно показа вдоль профиля в координатах вида (0 = начало вида; вид развёрнут при [reversed]). */
data class ProfileWindow(val fromX: Double, val toX: Double)

class LiveProfile(val section: SectionProfile, val reversed: Boolean) {
    private val tracker = AxisTracker(section.axis.locator)
    private var last: LivePosition? = null
    private var rawKm: Double? = null

    /**
     * Поправка по оси, км: показываемое положение = положение по GPS + поправка. Нужна, когда карта нарисована со сдвигом или
     * километраж не совпадает с реальными столбами; задаётся ручной привязкой ([applyCalibration]).
     */
    var calibrationKm: Double = 0.0
        private set

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
        rawKm = km
        last = positionAt(km, r.offRoute)
        return last
    }

    private fun positionAt(raw: Double, offRoute: Boolean): LivePosition {
        val km = (raw + calibrationKm).coerceIn(0.0, lengthKm)
        return LivePosition(
            kmAxis = km,
            kmText = KmMarks.textToPicket(section.kmMarks, km),
            element = section.elements.at(km),
            offRoute = offRoute,
            locked = tracker.isLocked,
        )
    }

    /** Поправка для места «я на [absKm]» (км + пикет/100): ближайшее к показываемому положению место с такой координатой. */
    fun calibrationFor(absKm: Double): Calibration {
        val raw = rawKm ?: return Calibration.NoPosition
        val shown = (raw + calibrationKm).coerceIn(0.0, lengthKm)
        val target = KmMarks.candidates(section.kmMarks, absKm)
            .filter { it >= -0.05 && it <= lengthKm + 0.05 }
            .minByOrNull { kotlin.math.abs(it - shown) } ?: return Calibration.NotOnSection
        return Calibration.Ok(shiftKm = target - raw, diffKm = kotlin.math.abs(target - shown))
    }

    /** Применить поправку сразу к текущему положению; 0 - сбросить. */
    fun applyCalibration(shiftKm: Double) {
        calibrationKm = shiftKm
        val raw = rawKm ?: return
        last = positionAt(raw, last?.offRoute ?: false)
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

    /**
     * Линия поезда вдоль оси для географической карты: от головы назад по ходу на [lengthKm], точки `(lat, lon)` от хвоста к голове.
     * Голова - положение по GPS БЕЗ ручной поправки (поправка двигает положение на профиле, а на карте поезд там, где GPS и значок).
     * У начала/конца участка линия обрезается по участку. Пусто - положения нет, вне оси или длина не больше нуля.
     */
    fun trainPath(lengthKm: Double, tailAhead: Boolean = false): List<Pair<Double, Double>> {
        if (last?.offRoute != false || lengthKm <= 0.0) return emptyList()
        val head = rawKm ?: return emptyList()
        val tail = if (tailAhead) toAxis((toView(head) + lengthKm).coerceAtMost(this.lengthKm))
        else toAxis((toView(head) - lengthKm).coerceAtLeast(0.0))
        val path = AxisSlice.smooth(section.axis.points, tail, head)
        // порядок «хвост -> голова»: при развороте хвост на большем км оси
        return if (tail <= head) path else path.asReversed()
    }

    /**
     * Железнодорожный километр в точке [lat], [lon] по картам участка: проекция на ось + ручная поправка + отметки километража.
     * null - точка дальше [maxOffsetM] от оси или у участка нет отметок километража.
     */
    fun absKmAt(lat: Double, lon: Double, maxOffsetM: Double = 300.0): Double? {
        val hit = section.axis.locator.project(lat, lon) ?: return null
        if (kotlin.math.abs(hit.offsetM) > maxOffsetM) return null
        val km = (hit.kmM / 1000.0 + calibrationKm).coerceIn(0.0, lengthKm)
        return KmMarks.toAbs(section.kmMarks, km)
    }

    /** Уклон в виде: при развороте подъём становится спуском. */
    fun gradeInView(e: ProfileElement): Double = if (reversed) -e.gradePermille else e.gradePermille
}

/** Срез ломаной оси по диапазону километров с интерполяцией концов. */
object AxisSlice {
    /** Точки `(lat, lon)` оси между [fromKm] и [toKm] (в любом порядке) по возрастанию км; пусто - диапазон вырожден или ось короче двух точек. */
    fun between(points: List<AxisPoint>, fromKm: Double, toKm: Double): List<Pair<Double, Double>> {
        if (points.size < 2) return emptyList()
        val lo = minOf(fromKm, toKm).coerceAtLeast(points.first().km)
        val hi = maxOf(fromKm, toKm).coerceAtMost(points.last().km)
        if (hi - lo < 1e-6) return emptyList()
        val out = ArrayList<Pair<Double, Double>>()
        out += at(points, lo)
        for (p in points) if (p.km > lo + 1e-9 && p.km < hi - 1e-9) out += p.lat to p.lon
        out += at(points, hi)
        return out
    }

    /**
     * Сглаженный срез: кривая Катмулла-Рома через точки оси (точки лежат на пути, хорды между ними - нет), так линия повторяет изгиб
     * пути, а не ломается в вершинах. Точки `(lat, lon)` по возрастанию км, шаг не больше [stepKm]; концы - ровно в [fromKm]/[toKm].
     */
    fun smooth(points: List<AxisPoint>, fromKm: Double, toKm: Double, stepKm: Double = 0.02): List<Pair<Double, Double>> {
        if (points.size < 2) return emptyList()
        val lo = minOf(fromKm, toKm).coerceAtLeast(points.first().km)
        val hi = maxOf(fromKm, toKm).coerceAtMost(points.last().km)
        if (hi - lo < 1e-6) return emptyList()
        val n = kotlin.math.ceil((hi - lo) / stepKm).toInt().coerceAtLeast(2)
        return List(n + 1) { i -> splineAt(points, lo + (hi - lo) * i / n) }
    }

    private fun splineAt(points: List<AxisPoint>, km: Double): Pair<Double, Double> {
        var l = 0
        var h = points.size - 1
        while (h - l > 1) {
            val m = (l + h) / 2
            if (points[m].km <= km) l = m else h = m
        }
        val p1 = points[l]
        val p2 = points[h]
        val p0 = points[(l - 1).coerceAtLeast(0)]
        val p3 = points[(h + 1).coerceAtMost(points.size - 1)]
        val t = if (p2.km - p1.km < 1e-9) 0.0 else ((km - p1.km) / (p2.km - p1.km)).coerceIn(0.0, 1.0)
        val t2 = t * t
        val t3 = t2 * t
        fun cr(a: Double, b: Double, c: Double, d: Double) =
            0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
        return cr(p0.lat, p1.lat, p2.lat, p3.lat) to cr(p0.lon, p1.lon, p2.lon, p3.lon)
    }

    private fun at(points: List<AxisPoint>, km: Double): Pair<Double, Double> {
        var l = 0
        var h = points.size - 1
        while (h - l > 1) {
            val m = (l + h) / 2
            if (points[m].km <= km) l = m else h = m
        }
        val a = points[l]
        val b = points[h]
        val t = if (b.km - a.km < 1e-9) 0.0 else ((km - a.km) / (b.km - a.km)).coerceIn(0.0, 1.0)
        return (a.lat + (b.lat - a.lat) * t) to (a.lon + (b.lon - a.lon) * t)
    }
}
