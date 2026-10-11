package com.z_company.domain.gnss

import com.z_company.domain.track.TrackPoint
import com.z_company.domain.track.TrackThinner
import com.z_company.domain.util.GnssPlausibility
import kotlin.math.floor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/*
 * Общая карта зон без GPS (SCREEN_SPECS §6.9, «Зоны без GPS»; план - docs/GNSS_DEAD_ZONES_PLAN.md §5, §12).
 * Вклад: ячейки сетки [GnssGrid] + маска видов, без времени и порядка, под согласием №2. Чистая логика без платформы.
 * Координаты, ячейки и tag не логировать.
 */

/** Битовая маска видов для API: GAP=1, POOR=2, JUMP=4. */
object GnssKindMask {
    const val GAP = 1
    const val POOR = 2
    const val JUMP = 4

    fun of(kind: GnssBadKind): Int = when (kind) {
        GnssBadKind.GAP -> GAP
        GnssBadKind.POOR_ACCURACY -> POOR
        GnssBadKind.SPEED_JUMP -> JUMP
    }

    fun toMask(kinds: Set<GnssBadKind>): Int = kinds.fold(0) { acc, k -> acc or of(k) }

    fun fromMask(mask: Int): Set<GnssBadKind> =
        GnssBadKind.entries.filterTo(LinkedHashSet()) { mask and of(it) != 0 }
}

/** Ячейка вклада: [kinds] - маска 1..7, [count] - сколько участков её накрыло (1..100, информативно). */
data class GnssContribCell(val cell: GnssCell, val kinds: Int, val count: Int)

/** Неотправленный вклад одного сеанса «АВТО». [id] - UUID сеанса, он же `contribId` (повтор = duplicate). */
class GnssContribSession(val id: String, val createdAtMs: Long, val cells: List<GnssContribCell>)

object GnssContribBuilder {
    /** Потолок ячеек в одном вкладе (лимит сервера). */
    const val MAX_CELLS = 1000
    const val MAX_COUNT = 100

    /** Свести плохие участки в ячейки: маска - объединение видов, count - число накрывших участков. */
    fun fromSpans(spans: List<GnssBadSpan>): List<GnssContribCell> {
        val masks = LinkedHashMap<GnssCell, Int>()
        val counts = HashMap<GnssCell, Int>()
        for (s in spans) {
            for (c in GnssDeadZones.cellsOf(s)) {
                masks[c] = (masks[c] ?: 0) or GnssKindMask.of(s.kind)
                counts[c] = (counts[c] ?: 0) + 1
            }
        }
        return masks.entries
            .sortedWith(compareBy({ it.key.iy }, { it.key.ix }))
            .take(MAX_CELLS)
            .map { (c, m) -> GnssContribCell(c, m, (counts[c] ?: 1).coerceIn(1, MAX_COUNT)) }
    }

    /** Записанные за сеанс точки -> очистка от подмены -> детектор -> ячейки. Пусто, если плохих участков нет. */
    fun fromPoints(points: List<TrackPoint>): List<GnssContribCell> =
        fromSpans(GnssDeadZoneDetector.detect(GnssPlausibility.sanitize(points)))

    /** UTC-день сеанса `yyyy-MM-dd` (сервер принимает [сегодня-7 .. сегодня+1]). */
    fun utcDay(ms: Long): String {
        val days = floor(ms / 86_400_000.0).toLong()
        val d = kotlinx.datetime.LocalDate.fromEpochDays(days.toInt())
        fun two(n: Int) = n.toString().padStart(2, '0')
        return "${d.year}-${two(d.monthNumber)}-${two(d.dayOfMonth)}"
    }
}

/**
 * Буфер точек сеанса «АВТО» для вклада зон (в памяти, не Track.db): прореживание как у трека, потолок размера.
 * Не потокобезопасен - вызывать под внешней блокировкой.
 */
class GnssSessionBuffer(private val maxPoints: Int = MAX_POINTS) {
    private var thinner = TrackThinner()
    private val points = ArrayList<TrackPoint>()

    val size: Int get() = points.size

    fun add(p: TrackPoint): Boolean {
        if (points.size >= maxPoints) return false
        if (!thinner.accept(p)) return false
        points += p
        return true
    }

    fun drain(): List<TrackPoint> {
        val copy = points.toList()
        points.clear()
        thinner = TrackThinner()
        return copy
    }

    fun clear() {
        drain()
    }

    companion object {
        /** ~14 часов при точке раз в 5 с. */
        const val MAX_POINTS = 10_000
    }
}

/** Временное хранилище неотправленных вкладов зон: удаление записи = «отправлено». */
interface GnssContribStore {
    fun save(session: GnssContribSession)
    fun pendingIds(): List<String>
    fun load(id: String): GnssContribSession?
    fun delete(id: String)
    fun clear()
}

/** Согласие №2 в редакции, включающей места потери GPS. Без него вклад зон не копится и не отправляется. */
fun interface GnssContribSettings {
    fun enabled(): Boolean
}

/** Текстовый формат файла вклада: `G1,<id>,<createdAtMs>` и по строке `iy,ix,kinds,count`. */
object GnssContribText {
    fun encode(s: GnssContribSession): String = buildString {
        append("G1,").append(s.id).append(',').append(s.createdAtMs).append('\n')
        for (c in s.cells) {
            append(c.cell.iy).append(',').append(c.cell.ix).append(',').append(c.kinds).append(',').append(c.count).append('\n')
        }
    }

    fun decode(text: String): GnssContribSession? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return null
        val head = lines.next().split(',')
        if (head.size != 3 || head[0] != "G1") return null
        val created = head[2].toLongOrNull() ?: return null
        val cells = ArrayList<GnssContribCell>()
        while (lines.hasNext()) {
            val f = lines.next().split(',')
            if (f.size != 4) return null
            val kinds = f[2].toIntOrNull() ?: return null
            if (kinds !in 1..7) return null
            cells += GnssContribCell(
                GnssCell(f[0].toIntOrNull() ?: return null, f[1].toIntOrNull() ?: return null),
                kinds,
                (f[3].toIntOrNull() ?: return null).coerceIn(1, GnssContribBuilder.MAX_COUNT),
            )
        }
        return GnssContribSession(head[1], created, cells)
    }
}

/** Где хранится случайный tag вклада (SharedPreferences на Android). */
interface GnssTagStorage {
    fun tag(): String?
    fun createdAtMs(): Long
    fun save(tag: String, createdAtMs: Long)
    fun clear()
}

/**
 * Случайный tag устройства для порога «разные устройства» на сервере. Не связан с аккаунтом, ротируется раз в 30 дней
 * (после ротации старые вклады не связываются с новыми). Значение не логировать.
 */
class GnssContribTag(
    private val storage: GnssTagStorage,
    private val newTag: () -> String = ::randomTag,
) {
    fun current(nowMs: Long): String {
        val t = storage.tag()
        val at = storage.createdAtMs()
        // Часы назад (at > now) - тоже ротация: иначе tag мог бы жить бесконечно.
        if (t != null && at > 0 && nowMs - at in 0 until ROTATE_MS) return t
        val fresh = newTag()
        storage.save(fresh, nowMs)
        return fresh
    }

    fun reset() = storage.clear()

    companion object {
        const val ROTATE_MS = 30L * 24 * 60 * 60 * 1000

        @OptIn(ExperimentalUuidApi::class)
        fun randomTag(): String = Uuid.random().toString()
    }
}

/** Ячейка общей карты от сервера: [level] 1..3 (слабо/средне/сильно), [kinds] - маска. Точного числа машинистов нет. */
data class GnssServerCell(val cell: GnssCell, val level: Int, val kinds: Int)

/** Кэш ответа по плитке 0,5°: ETag для `If-None-Match` и время последней сверки. */
data class GnssTileCache(val etag: String?, val fetchedAtMs: Long, val cells: List<GnssServerCell>)

object GnssTileText {
    fun encode(t: GnssTileCache): String = buildString {
        append("T1,").append(t.etag.orEmpty()).append(',').append(t.fetchedAtMs).append('\n')
        for (c in t.cells) append(c.cell.iy).append(',').append(c.cell.ix).append(',').append(c.level).append(',').append(c.kinds).append('\n')
    }

    fun decode(text: String): GnssTileCache? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return null
        val head = lines.next().split(',')
        if (head.size != 3 || head[0] != "T1") return null
        val at = head[2].toLongOrNull() ?: return null
        val cells = ArrayList<GnssServerCell>()
        while (lines.hasNext()) {
            val f = lines.next().split(',')
            if (f.size != 4) return null
            cells += GnssServerCell(
                GnssCell(f[0].toIntOrNull() ?: return null, f[1].toIntOrNull() ?: return null),
                (f[2].toIntOrNull() ?: return null).coerceIn(1, 3),
                f[3].toIntOrNull() ?: return null,
            )
        }
        return GnssTileCache(head[1].ifEmpty { null }, at, cells)
    }
}

/** Ячейка на карте: локальные данные ([local]) и/или общие ([serverLevel] 0 - общих нет). */
data class GnssMapZone(
    val cell: GnssCell,
    val latitude: Double,
    val longitude: Double,
    val local: GnssZone?,
    val serverLevel: Int,
    val serverKinds: Set<GnssBadKind>,
) {
    /** Густота цвета 0..1: локально 1 поездка - бледно, 4+ - насыщенно; общий уровень 1/2/3 - 0,5/0,75/1. */
    val weight: Double
        get() {
            val l = local?.let { (it.tracks / 4.0).coerceIn(0.0, 1.0) } ?: 0.0
            val s = when (serverLevel) {
                1 -> 0.5
                2 -> 0.75
                3 -> 1.0
                else -> 0.0
            }
            return maxOf(l, s)
        }

    val isShared: Boolean get() = serverLevel > 0
}

object GnssZoneMerge {
    /** Один слой: ячейка из любого источника; при совпадении - оба набора данных, цвет по максимуму. */
    fun merge(local: List<GnssZone>, server: List<GnssServerCell>): List<GnssMapZone> {
        val byCell = LinkedHashMap<GnssCell, GnssMapZone>()
        for (z in local) {
            byCell[z.cell] = GnssMapZone(z.cell, z.latitude, z.longitude, z, 0, emptySet())
        }
        // Соседние плитки могут вернуть одну ячейку дважды: берём больший уровень, виды объединяем.
        val serverBest = HashMap<GnssCell, GnssServerCell>()
        for (s in server) {
            val prev = serverBest[s.cell]
            serverBest[s.cell] = if (prev == null) s else GnssServerCell(s.cell, maxOf(prev.level, s.level), prev.kinds or s.kinds)
        }
        for (s in serverBest.values) {
            if (s.level !in 1..3) continue
            val kinds = GnssKindMask.fromMask(s.kinds)
            val existing = byCell[s.cell]
            byCell[s.cell] = if (existing != null) {
                existing.copy(serverLevel = s.level, serverKinds = kinds)
            } else {
                val pos = GnssGrid.center(s.cell)
                GnssMapZone(s.cell, pos.latitude, pos.longitude, null, s.level, kinds)
            }
        }
        return byCell.values.sortedWith(compareBy({ it.cell.iy }, { it.cell.ix }))
    }
}

/** Тексты карточки общей зоны. */
object GnssSharedZoneText {
    const val TITLE = "Здесь у машинистов пропадал GPS"
    const val NOTE = "Данные обезличены и собраны по поездкам других машинистов. Причина неизвестна: глушение, тоннель, выемка или сбой телефона."

    /** Уровень словами; точное число машинистов не раскрывается. */
    fun level(level: Int): String = when (level) {
        1 -> "изредка"
        2 -> "нередко"
        else -> "часто"
    }

    fun levelLine(level: Int): String = "Случается: " + level(level)
}
