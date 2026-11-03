package com.z_company.domain.track

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Согласие №2 на устройстве («Передавать обезличенные высоты для режимной карты»). Независимо от согласия №1. */
fun interface AltitudeContribSettings {
    fun enabled(): Boolean
}

/** Накопленные за сеанс «АВТО» точки для вклада высот, ожидающие отправки. */
class PendingContribSession(val id: String, val createdAtMs: Long, val samples: List<AltitudeSample>)

/**
 * Временное хранилище точек вклада (НЕ Track.db): живёт только до успешной отправки, затем удаляется.
 * Удаление записи = «вклад по сеансу отправлен».
 */
interface AltitudeContribStore {
    fun save(session: PendingContribSession)
    fun pendingIds(): List<String>
    fun load(id: String): PendingContribSession?
    fun delete(id: String)
    fun clear()
}

/** Кэш осей участков по `axisHash` (перекачивается только при смене хэша). */
interface AxisCache {
    fun load(sectionId: String): SectionAxis?
    fun save(axis: SectionAxis)

    /** Удалить оси участков, которых нет в [keepIds] (участок снят с публикации). */
    fun retain(keepIds: Set<String>)
}

/**
 * Буфер точек вклада за сеанс «АВТО»: прореживание как у трека (раз в ~5 с), фильтр качества, потолок размера.
 * Не потокобезопасен - вызывать под внешней блокировкой.
 */
class AltitudeSessionBuffer(private val maxSamples: Int = MAX_SAMPLES) {
    private var thinner = TrackThinner()
    private val samples = ArrayList<AltitudeSample>()

    val size: Int get() = samples.size

    fun add(p: TrackPoint): Boolean {
        if (samples.size >= maxSamples) return false
        val s = AltitudeContribBuilder.accept(p) ?: return false
        if (!thinner.accept(p)) return false
        samples += s
        return true
    }

    /** Забрать накопленное и начать заново. */
    fun drain(): List<AltitudeSample> {
        val copy = samples.toList()
        samples.clear()
        thinner = TrackThinner()
        return copy
    }

    fun clear() {
        drain()
    }

    companion object {
        /** ~14 часов при точке раз в 5 с. */
        const val MAX_SAMPLES = 10_000
    }
}

@OptIn(ExperimentalUuidApi::class)
fun newContribSessionId(): String = Uuid.random().toString()

/** Текстовый формат файла сеанса и кэша оси (строки через `\n`; пустое поле - null). */
object AltitudeContribText {
    fun encodeSession(s: PendingContribSession): String = buildString {
        append("S1,").append(s.id).append(',').append(s.createdAtMs).append('\n')
        for (p in s.samples) {
            append(p.lat).append(',').append(p.lon).append(',')
            append(p.altM ?: "").append(',').append(p.baroM ?: "").append(',').append(p.accM ?: "").append('\n')
        }
    }

    fun decodeSession(text: String): PendingContribSession? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return null
        val head = lines.next().split(',')
        if (head.size != 3 || head[0] != "S1") return null
        val created = head[2].toLongOrNull() ?: return null
        val list = ArrayList<AltitudeSample>()
        while (lines.hasNext()) {
            val f = lines.next().split(',')
            if (f.size != 5) return null
            list += AltitudeSample(
                lat = f[0].toDoubleOrNull() ?: return null,
                lon = f[1].toDoubleOrNull() ?: return null,
                altM = f[2].takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                baroM = f[3].takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
                accM = f[4].takeIf { it.isNotEmpty() }?.toFloatOrNull(),
            )
        }
        return PendingContribSession(head[1], created, list)
    }

    fun encodeAxis(a: SectionAxis): String = buildString {
        append("A1,").append(a.sectionId).append(',').append(a.axisHash).append('\n')
        for (p in a.points) append(p.km).append(',').append(p.lat).append(',').append(p.lon).append('\n')
    }

    fun decodeAxis(text: String): SectionAxis? {
        val lines = text.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return null
        val head = lines.next().split(',')
        if (head.size != 3 || head[0] != "A1") return null
        val pts = ArrayList<AxisPoint>()
        while (lines.hasNext()) {
            val f = lines.next().split(',')
            if (f.size != 3) return null
            pts += AxisPoint(
                f[0].toDoubleOrNull() ?: return null,
                f[1].toDoubleOrNull() ?: return null,
                f[2].toDoubleOrNull() ?: return null,
            )
        }
        return SectionAxis(head[1], head[2], pts)
    }
}
