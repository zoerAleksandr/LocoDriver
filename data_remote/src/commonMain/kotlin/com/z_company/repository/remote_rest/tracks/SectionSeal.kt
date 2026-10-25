package com.z_company.repository.remote_rest.tracks

/**
 * Конверт участка советника `LD2`, который отдаёт сервер (backend/src/services/advisor_seal.py):
 *
 *     "LD2" | kid (1) | version (4, big-endian) | nonce (12) | ChaCha20-Poly1305(JSON участка) + тег (16)
 *
 * Ключ на ответ: HKDF-SHA256(ключ приложения[kid], salt = nonce, info = "locodriver/section/v2|<id>|<version>"),
 * AAD = "LD2" + kid + "<id>|<version>". Без ключа приложения конверт - случайные байты. Сервер добавляет в геометрию
 * водяной знак на аккаунт, поэтому расшифрованные данные нельзя тиражировать анонимно.
 *
 * Тот же формат читают iOS и PWA (docs/ADVISOR_SEAL_PROTOCOL.md).
 */
object SectionSeal {
    private const val MAGIC_LEN = 3
    private const val HEADER_LEN = MAGIC_LEN + 1 + 4 + 12
    private val MAGIC = byteArrayOf('L'.code.toByte(), 'D'.code.toByte(), '2'.code.toByte())
    private const val INFO_PREFIX = "locodriver/section/v2|"

    class SealException(message: String) : Exception(message)

    /** Версия участка из заголовка (без расшифровки), либо null, если это не конверт LD2. */
    fun versionOf(blob: ByteArray): Int? =
        if (blob.size >= HEADER_LEN && blob[0] == MAGIC[0] && blob[1] == MAGIC[1] && blob[2] == MAGIC[2]) {
            ((blob[4].toInt() and 0xff) shl 24) or ((blob[5].toInt() and 0xff) shl 16) or ((blob[6].toInt() and 0xff) shl 8) or (blob[7].toInt() and 0xff)
        } else null

    /** Расшифровать конверт участка [sectionId] в JSON-текст. Любая ошибка формата/ключа/тега - [SealException]. */
    fun open(blob: ByteArray, sectionId: String, keys: (Int) -> ByteArray? = SectionSealKeys::forKid): String {
        val version = versionOf(blob) ?: throw SealException("not an LD2 envelope")
        if (blob.size < HEADER_LEN + 16) throw SealException("truncated")
        val kid = blob[3].toInt() and 0xff
        val appKey = keys(kid) ?: throw SealException("unknown key id")
        val nonce = blob.copyOfRange(8, 8 + 12)
        val tail = "$sectionId|$version".encodeToByteArray()
        val key = SectionCrypto.hkdfSha256(appKey, nonce, (INFO_PREFIX.encodeToByteArray()) + tail)
        val aad = MAGIC + byteArrayOf(kid.toByte()) + tail
        val plain = SectionCrypto.chacha20Poly1305Open(key, nonce, aad, blob.copyOfRange(HEADER_LEN, blob.size))
            ?: throw SealException("authentication failed")
        return plain.decodeToString()
    }
}
