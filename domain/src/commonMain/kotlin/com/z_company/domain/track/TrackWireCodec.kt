package com.z_company.domain.track

import kotlin.math.roundToLong

/**
 * Платформенные примитивы для формата чанка: zlib (RFC 1950, как `zlib.compress` на сервере) и SHA-256.
 * В `commonMain` их нет без новой зависимости: Android/JVM — `java.util.zip`/`MessageDigest`,
 * iOS — пока заглушка (см. `IosTrackBlobTools`).
 */
interface TrackBlobTools {
    fun deflate(data: ByteArray, level: Int = 6): ByteArray

    /** Распаковать; больше [maxOutput] байт — бросить [TrackCodecException]. */
    fun inflate(data: ByteArray, maxOutput: Int): ByteArray

    /** SHA-256 в нижнем регистре hex (64 символа). */
    fun sha256Hex(data: ByteArray): String
}

/** Ошибка формата чанка. Текст без значений (координаты не попадают в сообщения). */
class TrackCodecException(message: String) : Exception(message)

/**
 * Формат чанка `ld-track-v1` (зеркало `backend/src/services/track_codec.py` сервера; менять только вместе с ним).
 *
 * До zlib, всё varint (LEB128, беззнаковый) / zigzag для знаковых:
 * `"LT" | ver(u8=1) | flags(u8: бит0 = есть барометр) | n | t0(мс) | lat0 | lon0 (1e-6°, zigzag)`,
 * затем колонки по n значений: `dt` (мс от предыдущей, первая 0), `dLat`, `dLon` (zigzag, 1e-6°, первая 0),
 * `speed` (0,1 км/ч), `alt` (маска присутствия ceil(n/8) байт, бит i%8 байта i/8, затем zigzag-дельты в дм
 * только по присутствующим), `baro` (то же, только если flags&1), `acc` (дм). Далее zlib level 6.
 * Шифрование — серверное, клиент шлёт этот блоб как есть (base64 в JSON-обёртке `{"enc":"ld-track-v1","data":...}`).
 */
object TrackWireCodec {
    const val ENC_NAME = "ld-track-v1"
    const val MAX_CHUNK_POINTS = 2000
    const val MAX_CHUNK_COMPRESSED = 256 * 1024
    const val MAX_CHUNK_RAW = 2 * 1024 * 1024
    const val MAX_SPEED_DKMH = 5000L
    const val ALT_MIN_DM = -5000L
    const val ALT_MAX_DM = 90000L

    /** Точка в единицах формата. */
    data class WirePoint(
        val tMs: Long,
        val latE6: Long,
        val lonE6: Long,
        val speedDkmh: Long,
        val altDm: Long?,
        val baroDm: Long?,
        val accDm: Long,
    )

    fun toWire(p: TrackPoint): WirePoint = WirePoint(
        tMs = p.timeMs,
        latE6 = (p.latitude * 1e6).roundToLong(),
        lonE6 = (p.longitude * 1e6).roundToLong(),
        // Скорость неизвестна -> 0 (в формате нет «нет значения»).
        speedDkmh = ((p.speedKmh ?: 0.0) * 10).roundToLong().coerceIn(0L, MAX_SPEED_DKMH),
        altDm = p.altitudeM?.let { (it * 10).roundToLong().coerceIn(ALT_MIN_DM, ALT_MAX_DM) },
        baroDm = null,
        accDm = ((p.accuracyM ?: 0f) * 10).roundToLong().coerceAtLeast(0L),
    )

    fun fromWire(w: WirePoint): TrackPoint = TrackPoint(
        timeMs = w.tMs,
        latitude = w.latE6 / 1e6,
        longitude = w.lonE6 / 1e6,
        speedKmh = w.speedDkmh / 10.0,
        accuracyM = (w.accDm / 10.0).toFloat(),
        altitudeM = w.altDm?.let { it / 10.0 },
    )

    /** Блоб для отправки: raw -> zlib. [points] — по возрастанию времени, 1..[MAX_CHUNK_POINTS]. */
    fun encodeChunk(points: List<TrackPoint>, tools: TrackBlobTools): ByteArray =
        tools.deflate(encodeRaw(points.map(::toWire), withBaro = false), 6)

    fun decodeChunk(blob: ByteArray, tools: TrackBlobTools): List<TrackPoint> =
        decodeRaw(tools.inflate(blob, MAX_CHUNK_RAW)).map(::fromWire)

    /** Несжатое тело чанка (для теста-вектора против сервера). */
    fun encodeRaw(points: List<WirePoint>, withBaro: Boolean): ByteArray {
        if (points.isEmpty()) throw TrackCodecException("пустой чанк")
        if (points.size > MAX_CHUNK_POINTS) throw TrackCodecException("слишком много точек в чанке")
        val n = points.size
        val out = ByteSink()
        out.byte('L'.code)
        out.byte('T'.code)
        out.byte(1)
        out.byte(if (withBaro) 1 else 0)
        out.put(n.toLong())
        out.put(points[0].tMs)
        out.put(zz(points[0].latE6))
        out.put(zz(points[0].lonE6))
        for (i in 0 until n) out.put(if (i == 0) 0L else points[i].tMs - points[i - 1].tMs)
        for (i in 0 until n) out.put(if (i == 0) 0L else zz(points[i].latE6 - points[i - 1].latE6))
        for (i in 0 until n) out.put(if (i == 0) 0L else zz(points[i].lonE6 - points[i - 1].lonE6))
        for (p in points) out.put(p.speedDkmh)
        optColumn(out, points.map { it.altDm })
        if (withBaro) optColumn(out, points.map { it.baroDm })
        for (p in points) out.put(p.accDm)
        return out.toByteArray()
    }

    fun decodeRaw(raw: ByteArray): List<WirePoint> {
        val r = ByteSource(raw)
        if (r.byte() != 'L'.code || r.byte() != 'T'.code || r.byte() != 1) {
            throw TrackCodecException("неизвестный формат чанка")
        }
        val flags = r.byte()
        val nL = r.varint()
        if (nL < 1 || nL > MAX_CHUNK_POINTS) throw TrackCodecException("недопустимое число точек")
        val n = nL.toInt()
        val t0 = r.varint()
        val lat0 = unzz(r.varint())
        val lon0 = unzz(r.varint())
        val ts = LongArray(n)
        var t = t0
        for (i in 0 until n) {
            val d = r.varint()
            if (i == 0 && d != 0L) throw TrackCodecException("первая дельта времени должна быть 0")
            t += d
            ts[i] = t
        }
        val lats = LongArray(n)
        var v = lat0
        for (i in 0 until n) {
            val d = unzz(r.varint())
            if (i == 0 && d != 0L) throw TrackCodecException("первая дельта координаты должна быть 0")
            v += d
            lats[i] = v
        }
        val lons = LongArray(n)
        v = lon0
        for (i in 0 until n) {
            val d = unzz(r.varint())
            if (i == 0 && d != 0L) throw TrackCodecException("первая дельта координаты должна быть 0")
            v += d
            lons[i] = v
        }
        val speeds = LongArray(n) { r.varint() }
        val alts = readOptColumn(r, n)
        val baros: List<Long?> = if (flags and 1 != 0) readOptColumn(r, n) else List(n) { null }
        val accs = LongArray(n) { r.varint() }
        if (!r.done()) throw TrackCodecException("лишние данные в конце чанка")
        return List(n) { i ->
            if (lats[i] !in -90_000_000L..90_000_000L || lons[i] !in -180_000_000L..180_000_000L) {
                throw TrackCodecException("координата вне диапазона")
            }
            if (speeds[i] > MAX_SPEED_DKMH) throw TrackCodecException("скорость вне диапазона")
            WirePoint(ts[i], lats[i], lons[i], speeds[i], alts[i], baros[i], accs[i])
        }
    }

    private fun optColumn(out: ByteSink, values: List<Long?>) {
        val mask = IntArray((values.size + 7) / 8)
        values.forEachIndexed { i, x -> if (x != null) mask[i / 8] = mask[i / 8] or (1 shl (i % 8)) }
        mask.forEach { out.byte(it) }
        var last = 0L
        for (x in values) if (x != null) {
            out.put(zz(x - last))
            last = x
        }
    }

    private fun readOptColumn(r: ByteSource, n: Int): List<Long?> {
        val mask = r.take((n + 7) / 8)
        var last = 0L
        return List(n) { i ->
            if ((mask[i / 8].toInt() shr (i % 8)) and 1 == 1) {
                last += unzz(r.varint())
                if (last !in ALT_MIN_DM..ALT_MAX_DM) throw TrackCodecException("высота вне диапазона")
                last
            } else {
                null
            }
        }
    }

    private fun zz(v: Long): Long = (v shl 1) xor (v shr 63)
    private fun unzz(u: Long): Long = (u ushr 1) xor -(u and 1)

    private class ByteSink {
        private var buf = ByteArray(256)
        private var size = 0

        fun byte(b: Int) {
            if (size == buf.size) buf = buf.copyOf(size * 2)
            buf[size++] = b.toByte()
        }

        /** Беззнаковый varint; отрицательное значение недопустимо. */
        fun put(value: Long) {
            if (value < 0) throw TrackCodecException("отрицательное значение в беззнаковом поле")
            var v = value
            while (v >= 0x80) {
                byte(((v and 0x7F) or 0x80).toInt())
                v = v ushr 7
            }
            byte(v.toInt())
        }

        fun toByteArray(): ByteArray = buf.copyOf(size)
    }

    private class ByteSource(private val d: ByteArray) {
        private var i = 0

        fun byte(): Int {
            if (i >= d.size) throw TrackCodecException("неожиданный конец данных")
            return d[i++].toInt() and 0xFF
        }

        fun varint(): Long {
            var shift = 0
            var v = 0L
            while (true) {
                val b = byte()
                v = v or ((b and 0x7F).toLong() shl shift)
                if (b and 0x80 == 0) return v
                shift += 7
                if (shift > 63) throw TrackCodecException("слишком длинное число")
            }
        }

        fun take(k: Int): ByteArray {
            if (i + k > d.size) throw TrackCodecException("неожиданный конец данных")
            return d.copyOfRange(i, i + k).also { i += k }
        }

        fun done(): Boolean = i == d.size
    }
}
