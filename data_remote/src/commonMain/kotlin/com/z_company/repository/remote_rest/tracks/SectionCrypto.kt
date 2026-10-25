package com.z_company.repository.remote_rest.tracks

/**
 * Криптопримитивы для конверта участков (`LD2`, см. [SectionSeal]) на чистом Kotlin: SHA-256, HMAC, HKDF, ChaCha20-Poly1305 (RFC 8439).
 * Свой код нужен потому, что в commonMain нет java.* / javax.crypto, а новые зависимости без согласия не добавляем; один и тот же
 * код работает на Android и iOS. Проверяется известными векторами и конвертом, собранным серверной реализацией (SectionSealTest).
 */
internal object SectionCrypto {
    private val K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0x0bf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )

    fun sha256(data: ByteArray): ByteArray {
        val h = intArrayOf(
            0x6a09e667, -0x4498517b, 0x3c6ef372, -0x5ab00ac6, 0x510e527f, -0x64fa9774, 0x1f83d9ab, 0x5be0cd19,
        )
        val bitLen = data.size.toLong() * 8
        val padLen = ((55 - data.size) % 64 + 64) % 64
        val msg = ByteArray(data.size + 1 + padLen + 8)
        data.copyInto(msg)
        msg[data.size] = 0x80.toByte()
        for (i in 0 until 8) msg[msg.size - 1 - i] = (bitLen ushr (8 * i)).toByte()
        val w = IntArray(64)
        var off = 0
        while (off < msg.size) {
            for (i in 0 until 16) {
                w[i] = ((msg[off + 4 * i].toInt() and 0xff) shl 24) or ((msg[off + 4 * i + 1].toInt() and 0xff) shl 16) or
                    ((msg[off + 4 * i + 2].toInt() and 0xff) shl 8) or (msg[off + 4 * i + 3].toInt() and 0xff)
            }
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotr(7) xor w[i - 15].rotr(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotr(17) xor w[i - 2].rotr(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (i in 0 until 64) {
                val s1 = e.rotr(6) xor e.rotr(11) xor e.rotr(25)
                val ch = (e and f) xor (e.inv() and g)
                val t1 = hh + s1 + ch + K[i] + w[i]
                val s0 = a.rotr(2) xor a.rotr(13) xor a.rotr(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val t2 = s0 + maj
                hh = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d; h[4] += e; h[5] += f; h[6] += g; h[7] += hh
            off += 64
        }
        val out = ByteArray(32)
        for (i in 0 until 8) {
            out[4 * i] = (h[i] ushr 24).toByte(); out[4 * i + 1] = (h[i] ushr 16).toByte()
            out[4 * i + 2] = (h[i] ushr 8).toByte(); out[4 * i + 3] = h[i].toByte()
        }
        return out
    }

    private fun Int.rotr(n: Int): Int = (this ushr n) or (this shl (32 - n))
    private fun Int.rotl(n: Int): Int = (this shl n) or (this ushr (32 - n))

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val k = if (key.size > 64) sha256(key) else key
        val ipad = ByteArray(64) { (if (it < k.size) k[it].toInt() else 0).xor(0x36).toByte() }
        val opad = ByteArray(64) { (if (it < k.size) k[it].toInt() else 0).xor(0x5c).toByte() }
        return sha256(opad + sha256(ipad + data))
    }

    /** HKDF-SHA256 (RFC 5869), [length] до 32 байт хватает для ключа ChaCha20. */
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int = 32): ByteArray {
        require(length in 1..32)
        val prk = hmacSha256(if (salt.isEmpty()) ByteArray(32) else salt, ikm)
        return hmacSha256(prk, info + byteArrayOf(1)).copyOf(length)
    }

    private fun le32(b: ByteArray, o: Int): Int =
        (b[o].toInt() and 0xff) or ((b[o + 1].toInt() and 0xff) shl 8) or ((b[o + 2].toInt() and 0xff) shl 16) or ((b[o + 3].toInt() and 0xff) shl 24)

    private fun chachaBlock(key: ByteArray, counter: Int, nonce: ByteArray): ByteArray {
        val s = IntArray(16)
        s[0] = 0x61707865; s[1] = 0x3320646e; s[2] = 0x79622d32; s[3] = 0x6b206574
        for (i in 0 until 8) s[4 + i] = le32(key, 4 * i)
        s[12] = counter
        for (i in 0 until 3) s[13 + i] = le32(nonce, 4 * i)
        val x = s.copyOf()
        fun qr(a: Int, b: Int, c: Int, d: Int) {
            x[a] += x[b]; x[d] = (x[d] xor x[a]).rotl(16)
            x[c] += x[d]; x[b] = (x[b] xor x[c]).rotl(12)
            x[a] += x[b]; x[d] = (x[d] xor x[a]).rotl(8)
            x[c] += x[d]; x[b] = (x[b] xor x[c]).rotl(7)
        }
        repeat(10) {
            qr(0, 4, 8, 12); qr(1, 5, 9, 13); qr(2, 6, 10, 14); qr(3, 7, 11, 15)
            qr(0, 5, 10, 15); qr(1, 6, 11, 12); qr(2, 7, 8, 13); qr(3, 4, 9, 14)
        }
        val out = ByteArray(64)
        for (i in 0 until 16) {
            val v = x[i] + s[i]
            out[4 * i] = v.toByte(); out[4 * i + 1] = (v ushr 8).toByte(); out[4 * i + 2] = (v ushr 16).toByte(); out[4 * i + 3] = (v ushr 24).toByte()
        }
        return out
    }

    private fun chachaXor(key: ByteArray, nonce: ByteArray, data: ByteArray): ByteArray {
        val out = ByteArray(data.size)
        var counter = 1
        var pos = 0
        while (pos < data.size) {
            val block = chachaBlock(key, counter++, nonce)
            val n = minOf(64, data.size - pos)
            for (i in 0 until n) out[pos + i] = (data[pos + i].toInt() xor block[i].toInt()).toByte()
            pos += n
        }
        return out
    }

    /** Poly1305 (поблочно по 26 бит, как poly1305-donna-32). */
    private fun poly1305(key: ByteArray, msg: ByteArray): ByteArray {
        val m26 = 0x3ffffffL
        fun u(b: ByteArray, o: Int): Long = le32(b, o).toLong() and 0xffffffffL
        val r0 = u(key, 0) and 0x3ffffff
        val r1 = (u(key, 3) ushr 2) and 0x3ffff03
        val r2 = (u(key, 6) ushr 4) and 0x3ffc0ff
        val r3 = (u(key, 9) ushr 6) and 0x3f03fff
        val r4 = (u(key, 12) ushr 8) and 0x00fffff
        val s1 = r1 * 5; val s2 = r2 * 5; val s3 = r3 * 5; val s4 = r4 * 5
        var h0 = 0L; var h1 = 0L; var h2 = 0L; var h3 = 0L; var h4 = 0L
        var pos = 0
        while (pos < msg.size) {
            val n = minOf(16, msg.size - pos)
            val blk = ByteArray(17)
            msg.copyInto(blk, 0, pos, pos + n)
            val hibit: Long
            if (n < 16) { blk[n] = 1; hibit = 0L } else hibit = 1L shl 24
            h0 += u(blk, 0) and m26
            h1 += (u(blk, 3) ushr 2) and m26
            h2 += (u(blk, 6) ushr 4) and m26
            h3 += (u(blk, 9) ushr 6) and m26
            h4 += (u(blk, 12) ushr 8) or hibit
            var d0 = h0 * r0 + h1 * s4 + h2 * s3 + h3 * s2 + h4 * s1
            var d1 = h0 * r1 + h1 * r0 + h2 * s4 + h3 * s3 + h4 * s2
            var d2 = h0 * r2 + h1 * r1 + h2 * r0 + h3 * s4 + h4 * s3
            var d3 = h0 * r3 + h1 * r2 + h2 * r1 + h3 * r0 + h4 * s4
            var d4 = h0 * r4 + h1 * r3 + h2 * r2 + h3 * r1 + h4 * r0
            var c = d0 ushr 26; h0 = d0 and m26; d1 += c
            c = d1 ushr 26; h1 = d1 and m26; d2 += c
            c = d2 ushr 26; h2 = d2 and m26; d3 += c
            c = d3 ushr 26; h3 = d3 and m26; d4 += c
            c = d4 ushr 26; h4 = d4 and m26
            h0 += c * 5; c = h0 ushr 26; h0 = h0 and m26; h1 += c
            pos += n
        }
        var c = h1 ushr 26; h1 = h1 and m26; h2 += c
        c = h2 ushr 26; h2 = h2 and m26; h3 += c
        c = h3 ushr 26; h3 = h3 and m26; h4 += c
        c = h4 ushr 26; h4 = h4 and m26; h0 += c * 5
        c = h0 ushr 26; h0 = h0 and m26; h1 += c
        var g0 = h0 + 5; c = g0 ushr 26; g0 = g0 and m26
        var g1 = h1 + c; c = g1 ushr 26; g1 = g1 and m26
        var g2 = h2 + c; c = g2 ushr 26; g2 = g2 and m26
        var g3 = h3 + c; c = g3 ushr 26; g3 = g3 and m26
        val g4 = h4 + c - (1L shl 26)
        if (g4 >= 0) { h0 = g0; h1 = g1; h2 = g2; h3 = g3; h4 = g4 and m26 }
        val w0 = (h0 or (h1 shl 26)) and 0xffffffffL
        val w1 = ((h1 ushr 6) or (h2 shl 20)) and 0xffffffffL
        val w2 = ((h2 ushr 12) or (h3 shl 14)) and 0xffffffffL
        val w3 = ((h3 ushr 18) or (h4 shl 8)) and 0xffffffffL
        var f = w0 + u(key, 16); val t0 = f and 0xffffffffL
        f = w1 + u(key, 20) + (f ushr 32); val t1 = f and 0xffffffffL
        f = w2 + u(key, 24) + (f ushr 32); val t2 = f and 0xffffffffL
        f = w3 + u(key, 28) + (f ushr 32); val t3 = f and 0xffffffffL
        val out = ByteArray(16)
        for ((i, t) in longArrayOf(t0, t1, t2, t3).withIndex()) {
            out[4 * i] = t.toByte(); out[4 * i + 1] = (t ushr 8).toByte(); out[4 * i + 2] = (t ushr 16).toByte(); out[4 * i + 3] = (t ushr 24).toByte()
        }
        return out
    }

    private fun pad16(n: Int): ByteArray = ByteArray((16 - n % 16) % 16)

    private fun le64(v: Long): ByteArray = ByteArray(8) { (v ushr (8 * it)).toByte() }

    /** Расшифровка ChaCha20-Poly1305 (RFC 8439): [cipherAndTag] = шифртекст + 16 байт тега. null - тег не сошёлся. */
    fun chacha20Poly1305Open(key: ByteArray, nonce: ByteArray, aad: ByteArray, cipherAndTag: ByteArray): ByteArray? {
        if (key.size != 32 || nonce.size != 12 || cipherAndTag.size < 16) return null
        val ct = cipherAndTag.copyOfRange(0, cipherAndTag.size - 16)
        val tag = cipherAndTag.copyOfRange(cipherAndTag.size - 16, cipherAndTag.size)
        val polyKey = chachaBlock(key, 0, nonce).copyOf(32)
        val mac = poly1305(polyKey, aad + pad16(aad.size) + ct + pad16(ct.size) + le64(aad.size.toLong()) + le64(ct.size.toLong()))
        var diff = 0
        for (i in 0 until 16) diff = diff or (mac[i].toInt() xor tag[i].toInt())
        if (diff != 0) return null
        return chachaXor(key, nonce, ct)
    }

    /** Только для тестов: шифрование той же схемой. */
    internal fun chacha20Poly1305Seal(key: ByteArray, nonce: ByteArray, aad: ByteArray, plain: ByteArray): ByteArray {
        val ct = chachaXor(key, nonce, plain)
        val polyKey = chachaBlock(key, 0, nonce).copyOf(32)
        val mac = poly1305(polyKey, aad + pad16(aad.size) + ct + pad16(ct.size) + le64(aad.size.toLong()) + le64(ct.size.toLong()))
        return ct + mac
    }
}
