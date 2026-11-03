package com.z_company.domain.util

import com.z_company.domain.track.TrackAnalysis
import com.z_company.domain.track.TrackPoint
import kotlin.math.hypot

/**
 * Маска ввода «чч:мм» (SCREEN_SPECS §6.9, карточка планирования): вводятся только цифры, двоеточие ставится автоматически
 * после двух цифр часов. Если первая цифра 3-9, час однозначный и двоеточие идёт сразу за ней («905» -> «9:05»).
 * При стирании (текст стал короче) завершающее двоеточие не возвращается, иначе его нельзя удалить.
 */
object TimeMask {
    fun format(input: String, previous: String = ""): String {
        val digits = input.filter { it in '0'..'9' }.take(4)
        if (digits.isEmpty()) return ""
        val hourLen = if (digits[0] in '3'..'9') 1 else 2
        if (digits.length < hourLen) return digits
        val deleting = input.length < previous.length
        if (digits.length == hourLen) return if (deleting) digits else "$digits:"
        return digits.substring(0, hourLen) + ":" + digits.substring(hourLen)
    }
}

/**
 * Возврат карты в слежение после ручного перемещения (SCREEN_SPECS §6.9): через [RETURN_DELAY_MS] без касаний
 * камера снова ведётся за положением. Константа - в одном месте.
 */
object FollowReturn {
    const val RETURN_DELAY_MS = 10_000L

    /**
     * Пора ли вернуться в слежение. [blocked] - открыта карточка планирования/выбранной точки/график/панель значка;
     * [fingerDown] - палец на карте; [viewOnly] - просмотр записанного трека (не применяется).
     */
    fun shouldReturn(
        following: Boolean,
        viewOnly: Boolean,
        blocked: Boolean,
        fingerDown: Boolean,
        nowMs: Long,
        lastInteractionMs: Long,
    ): Boolean = !following && !viewOnly && !blocked && !fingerDown && nowMs - lastInteractionMs >= RETURN_DELAY_MS

    /** Сколько ещё ждать до возврата, мс (не меньше 0). */
    fun remainingMs(nowMs: Long, lastInteractionMs: Long): Long =
        (RETURN_DELAY_MS - (nowMs - lastInteractionMs)).coerceIn(0L, RETURN_DELAY_MS)
}

/** Подпись базовой карты в экранных координатах (px). */
data class BaseMapLabel(val x: Double, val y: Double, val name: String)

/** Подпись приложения, проверяемая на дубль с подписями базовой карты. */
data class AppMapLabel(val x: Double, val y: Double, val name: String)

/**
 * Дубли подписей станций: подпись приложения не рисуется, если рядом (≤ [radiusPx], 40 dp в пикселях)
 * есть подпись базовой карты с тем же названием (иначе тексты накладываются друг на друга).
 */
object LabelDedup {
    const val RADIUS_DP = 40.0

    /** Регулярка компилируется один раз (раньше - на каждый вызов, а вызовов O(n^2) на каждый пересчёт подписей). */
    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(name: String): String =
        name.lowercase().replace('ё', 'е').replace(NON_WORD, " ").trim()

    /** Одно название: совпадают после нормализации либо одно целиком входит в другое (от 4 символов: «Луга» / «Луга I»). */
    fun sameName(a: String, b: String): Boolean = sameNormalized(normalize(a), normalize(b))

    /** То же, что [sameName], для уже нормализованных ([normalize]) названий. */
    fun sameNormalized(x: String, y: String): Boolean {
        if (x.isEmpty() || y.isEmpty()) return false
        if (x == y) return true
        val (short, long) = if (x.length <= y.length) x to y else y to x
        return short.length >= 4 && (long.startsWith("$short ") || long.endsWith(" $short"))
    }

    /** Подпись в географических координатах для [sameNameDuplicates]; [priority] меньше = важнее (0 - крайняя станция). */
    data class GeoLabel(val lat: Double, val lon: Double, val name: String, val priority: Int = 1)

    /** Радиус, в котором одинаковые названия считаются одной подписью, м. */
    const val SAME_NAME_RADIUS_M = 300.0

    /**
     * Дубли подписей ОДНОГО названия в радиусе [radiusM] друг от друга (оранжевые «Будогощь» на разных путях парка):
     * из группы остаётся одна подпись - с меньшим [GeoLabel.priority], при равенстве ближайшая к ([refLat], [refLon]).
     * Возвращает индексы подписей, которые рисовать не надо (маркеры остаются).
     */
    fun sameNameDuplicates(
        labels: List<GeoLabel>,
        refLat: Double,
        refLon: Double,
        radiusM: Double = SAME_NAME_RADIUS_M,
    ): Set<Int> {
        val hidden = mutableSetOf<Int>()
        val norm = Array(labels.size) { normalize(labels[it].name) }
        val order = labels.indices.filter { labels[it].name.isNotBlank() }.sortedWith(
            compareBy<Int> { labels[it].priority }.thenBy {
                com.z_company.domain.track.TrackAnalysis.distanceM(labels[it].lat, labels[it].lon, refLat, refLon)
            }
        )
        val kept = ArrayList<Int>()
        for (i in order) {
            val l = labels[i]
            // Сначала дешёвое сравнение нормализованных названий (строки посчитаны один раз), потом расстояние.
            val dup = kept.any { k ->
                sameNormalized(norm[k], norm[i]) &&
                    com.z_company.domain.track.TrackAnalysis.distanceM(labels[k].lat, labels[k].lon, l.lat, l.lon) <= radiusM
            }
            if (dup) hidden += i else kept += i
        }
        return hidden
    }

    /** Индексы подписей приложения, дублирующих подписи базовой карты. */
    fun duplicates(app: List<AppMapLabel>, base: List<BaseMapLabel>, radiusPx: Double): Set<Int> {
        if (base.isEmpty()) return emptySet()
        val result = mutableSetOf<Int>()
        // Названия базовой карты нормализуются лениво и один раз (их сотни, а проверка по расстоянию отсекает почти все).
        val baseNorm = arrayOfNulls<String>(base.size)
        for ((i, a) in app.withIndex()) {
            if (a.name.isBlank()) continue
            val an = normalize(a.name)
            val dup = base.indices.any { j ->
                val b = base[j]
                hypot(a.x - b.x, a.y - b.y) <= radiusPx &&
                    sameNormalized(an, baseNorm[j] ?: normalize(b.name).also { baseNorm[j] = it })
            }
            if (dup) result += i
        }
        return result
    }
}

/**
 * Пройденный путь не должен пропадать (SCREEN_SPECS §6.9): ошибка/пустой/«усохший» результат загрузки из Track.db
 * не затирает прежний, а живые точки записи дорисовываются к загруженным. Не зависит от интернета и рельсовой линии.
 */
object PathKeeper {
    /** Что показывать после очередной загрузки: пустой результат или результат с меньшим числом точек - прежний. */
    fun <T : Any> keep(previous: T?, loaded: T?, count: (T) -> Int): T? {
        if (loaded == null) return previous
        if (previous == null) return loaded
        return if (count(loaded) == 0 || count(loaded) < count(previous)) previous else loaded
    }

    /** Точки пути: загруженные + живые точки новее последней загруженной (без дублей, по времени). */
    fun withLive(base: List<TrackPoint>, live: List<TrackPoint>): List<TrackPoint> {
        if (live.isEmpty()) return base
        val last = base.lastOrNull()?.timeMs ?: Long.MIN_VALUE
        val extra = live.filter { it.timeMs > last }.sortedBy { it.timeMs }
        return if (extra.isEmpty()) base else base + extra
    }

    /** Живой буфер ограничен: хранятся последние [max] точек. */
    fun appendLive(live: List<TrackPoint>, p: TrackPoint, max: Int = 5000): List<TrackPoint> {
        val out = if (live.isNotEmpty() && live.last().timeMs >= p.timeMs) live else live + p
        return if (out.size > max) out.takeLast(max) else out
    }
}

/** Рельсовая линия и сеть: автоперезапрос при возврате интернета (SCREEN_SPECS §6.9). */
object RailRefetch {
    /** Перезапросить линию: сеть вернулась (был офлайн, стал онлайн), а линия из кэша или не построена. */
    fun shouldRefetch(wasOnline: Boolean, online: Boolean, lineIsCachedOrFailed: Boolean): Boolean =
        !wasOnline && online && lineIsCachedOrFailed

    /** Подпись «Нет интернета» на плашке показывается только пока сети нет. */
    fun showOfflineCaption(online: Boolean): Boolean = !online
}

/**
 * Настоящие разрывы записи пути (между СОСЕДНИМИ записанными точками дольше [TrackAnalysis.DATA_GAP_MS], но не дольше
 * [TrackAnalysis.GAP_MS]). Линия на карте упрощается (прореживание + Дуглас-Пекер), и после упрощения соседние вершины
 * на прямом участке отстоят друг от друга на минуты - это не «нет данных»: серый пунктир рисуется только там, где разрыв
 * был в самой записи.
 */
object PathGaps {
    /** Разрывы записи в виде пар (время последней точки до разрыва, время первой точки после), мс. */
    fun of(points: List<TrackPoint>): List<Pair<Long, Long>> {
        val out = ArrayList<Pair<Long, Long>>()
        for (i in 1 until points.size) {
            val dt = points[i].timeMs - points[i - 1].timeMs
            if (dt > TrackAnalysis.DATA_GAP_MS && dt <= TrackAnalysis.GAP_MS) out += points[i - 1].timeMs to points[i].timeMs
        }
        return out
    }

    /** Содержит ли отрезок упрощённой линии от [fromMs] до [toMs] настоящий разрыв записи. */
    fun spans(gaps: List<Pair<Long, Long>>, fromMs: Long, toMs: Long): Boolean =
        gaps.any { (a, b) -> a >= fromMs && b <= toMs }
}
