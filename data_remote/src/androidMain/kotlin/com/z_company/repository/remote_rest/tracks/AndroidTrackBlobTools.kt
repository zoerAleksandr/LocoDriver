package com.z_company.repository.remote_rest.tracks

import com.z_company.domain.track.TrackBlobTools
import com.z_company.domain.track.TrackCodecException
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/** zlib (RFC 1950) и SHA-256 для формата чанка `ld-track-v1` на `java.util.zip` / `MessageDigest`. */
class AndroidTrackBlobTools : TrackBlobTools {
    override fun deflate(data: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level)
        try {
            deflater.setInput(data)
            deflater.finish()
            val out = ByteArrayOutputStream(data.size / 2 + 16)
            val buf = ByteArray(8192)
            while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf))
            return out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    override fun inflate(data: ByteArray, maxOutput: Int): ByteArray {
        val inflater = Inflater()
        try {
            inflater.setInput(data)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            while (!inflater.finished()) {
                val n = inflater.inflate(buf)
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw TrackCodecException("повреждённые сжатые данные")
                }
                out.write(buf, 0, n)
                if (out.size() > maxOutput) throw TrackCodecException("распакованные данные слишком велики")
            }
            return out.toByteArray()
        } catch (e: DataFormatException) {
            throw TrackCodecException("повреждённые сжатые данные")
        } finally {
            inflater.end()
        }
    }

    override fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
}
