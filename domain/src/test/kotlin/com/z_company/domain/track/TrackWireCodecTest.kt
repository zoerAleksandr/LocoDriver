package com.z_company.domain.track

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** JVM-реализация примитивов для тестов (на Android — `AndroidTrackBlobTools` в data_remote). */
object JvmTrackBlobTools : TrackBlobTools {
    override fun deflate(data: ByteArray, level: Int): ByteArray {
        val d = Deflater(level)
        d.setInput(data)
        d.finish()
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return out.toByteArray()
    }

    override fun inflate(data: ByteArray, maxOutput: Int): ByteArray {
        val inf = Inflater()
        inf.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        try {
            while (!inf.finished()) {
                val n = inf.inflate(buf)
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) throw TrackCodecException("повреждённые сжатые данные")
                out.write(buf, 0, n)
                if (out.size() > maxOutput) throw TrackCodecException("распакованные данные слишком велики")
            }
        } catch (e: java.util.zip.DataFormatException) {
            throw TrackCodecException("повреждённые сжатые данные")
        } finally {
            inf.end()
        }
        return out.toByteArray()
    }

    override fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}

class TrackWireCodecTest {
    private val tools = JvmTrackBlobTools

    // Вектор получен из серверного track_codec.encode_chunk (backend, ветка feature/user-tracks):
    // те же четыре точки; RAW — тело до zlib, Z — полный блоб сервера.
    private val vectorPoints = listOf(
        TrackWireCodec.WirePoint(1760000000000, 55751244, 37618423, 0, 1234, null, 50),
        TrackWireCodec.WirePoint(1760000005000, 55751300, 37618500, 421, 1236, null, 30),
        TrackWireCodec.WirePoint(1760000010000, 55751100, 37618300, 805, null, null, 45),
        TrackWireCodec.WirePoint(1760000015000, -33868820, 151209296, 1203, -50, null, 120),
    )
    private val serverRawHex =
        "4c540100048080b3c19c3398c99535ee8bf0230088278827882700708f039ff7bb55009a018f03a88baa6c00a503a506b3090ba413048b14321e2d78"
    private val serverZlibHex =
        "789cf3096164606968d87c708ef18c93534ddf757f5066e850074186827ee6f9df778732cc62ec675ed1bd2a876129f352b6cd9cdc4b8459ba458ce4742b00d63b1634"

    private fun hex(s: String) = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    @Test
    fun encodeRawMatchesServerTestVector() {
        assertEquals(serverRawHex, TrackWireCodec.encodeRaw(vectorPoints, withBaro = false).toHex())
    }

    @Test
    fun decodesBlobProducedByServerCodec() {
        val decoded = TrackWireCodec.decodeRaw(tools.inflate(hex(serverZlibHex), TrackWireCodec.MAX_CHUNK_RAW))
        assertEquals(vectorPoints, decoded)
    }

    @Test
    fun encodedChunkInflatesToServerRaw() {
        val blob = TrackWireCodec.encodeRaw(vectorPoints, false).let { tools.deflate(it, 6) }
        assertContentEquals(hex(serverRawHex), tools.inflate(blob, TrackWireCodec.MAX_CHUNK_RAW))
    }

    @Test
    fun roundTripKeepsPointsWithinFormatPrecision() {
        val src = (0 until 500).map { i ->
            TrackPoint(
                timeMs = 1_760_000_000_000L + i * 5_000L,
                latitude = 55.751244 + i * 0.0001,
                longitude = 37.618423 - i * 0.00007,
                speedKmh = if (i % 7 == 0) null else (i % 120).toDouble() + 0.34,
                accuracyM = (i % 30).toFloat(),
                altitudeM = if (i % 3 == 0) null else 120.0 + i * 0.25,
            )
        }
        val back = TrackWireCodec.decodeChunk(TrackWireCodec.encodeChunk(src, tools), tools)
        assertEquals(src.size, back.size)
        src.zip(back).forEach { (a, b) ->
            assertEquals(a.timeMs, b.timeMs)
            assertEquals(a.latitude, b.latitude, 1e-6)
            assertEquals(a.longitude, b.longitude, 1e-6)
            assertEquals(a.speedKmh ?: 0.0, b.speedKmh!!, 0.051)
            assertEquals(a.accuracyM ?: 0f, b.accuracyM!!, 0.051f)
            if (a.altitudeM == null) assertNull(b.altitudeM) else assertEquals(a.altitudeM!!, b.altitudeM!!, 0.051)
        }
    }

    @Test
    fun extremeCoordinatesAndSinglePoint() {
        val p = listOf(TrackPoint(1L, -90.0, 180.0, 0.0, 0f, null))
        val back = TrackWireCodec.decodeChunk(TrackWireCodec.encodeChunk(p, tools), tools)
        assertEquals(-90.0, back[0].latitude)
        assertEquals(180.0, back[0].longitude)
    }

    @Test
    fun compressesMuchBetterThanJson() {
        val src = (0 until 2000).map { i ->
            TrackPoint(1_760_000_000_000L + i * 5_000L, 55.0 + i * 0.0002, 37.0 + i * 0.0002, 60.0 + (i % 5), 10f, null)
        }
        val blob = TrackWireCodec.encodeChunk(src, tools)
        assertTrue(blob.size < 2000 * 12, "блоб ${blob.size} Б слишком велик")
    }

    @Test
    fun rejectsEmptyOversizeCorruptAndOutOfRange() {
        assertFailsWith<TrackCodecException> { TrackWireCodec.encodeChunk(emptyList(), tools) }
        val big = List(2001) { TrackPoint(it.toLong(), 1.0, 1.0, 1.0, 1f, null) }
        assertFailsWith<TrackCodecException> { TrackWireCodec.encodeChunk(big, tools) }
        assertFailsWith<TrackCodecException> { TrackWireCodec.decodeChunk(byteArrayOf(1, 2, 3), tools) }
        // Хвост после тела чанка недопустим.
        val raw = hex(serverRawHex) + byteArrayOf(0)
        assertFailsWith<TrackCodecException> { TrackWireCodec.decodeRaw(raw) }
        // Широта 91 градус.
        val bad = TrackWireCodec.encodeRaw(listOf(vectorPoints[0].copy(latE6 = 91_000_000)), false)
        assertFailsWith<TrackCodecException> { TrackWireCodec.decodeRaw(bad) }
    }

    @Test
    fun errorMessagesCarryNoCoordinates() {
        val bad = TrackWireCodec.encodeRaw(listOf(vectorPoints[0].copy(latE6 = 91_123_456)), false)
        val e = assertFailsWith<TrackCodecException> { TrackWireCodec.decodeRaw(bad) }
        assertTrue(e.message!!.none { it.isDigit() })
    }

    @Test
    fun sha256IsHexOfBlob() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            tools.sha256Hex(ByteArray(0)),
        )
    }

    @Test
    fun packingIsDeterministicAndBounded() {
        val pts = (0 until 4500).map { TrackPoint(it * 5000L, 55.0 + it * 1e-5, 37.0, 50.0, 5f, null) }
        val a = TrackPacking.chunks(pts.shuffled())
        val b = TrackPacking.chunks(pts)
        assertEquals(listOf(2000, 2000, 500), a.map { it.size })
        assertEquals(a, b)
        val prev = TrackPacking.preview(pts)
        assertEquals(100, prev.size)
        assertEquals(pts.first(), prev.first())
        assertEquals(pts.last(), prev.last())
        assertEquals(pts, TrackPacking.preview(pts, max = 5000))
    }

    @Test
    fun statsIgnoreSignalGaps() {
        val pts = listOf(
            TrackPoint(0, 55.0, 37.0, 10.0, null),
            TrackPoint(60_000, 55.01, 37.0, 80.0, null),
            // 10 минут без сигнала и скачок на 100 км не считаются пройденным путём.
            TrackPoint(660_000, 56.0, 37.0, 700.0, null),
        )
        val s = TrackPacking.stats(pts)
        assertEquals(1.11, s.distanceKm, 0.05)
        assertEquals(500.0, s.maxSpeedKmh)
    }
}
