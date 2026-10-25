package com.z_company.repository.remote_rest.tracks

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

class SectionSealTest {
    @Test
    fun sha256KnownAnswers() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", SectionCrypto.sha256(ByteArray(0)).toHex())
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", SectionCrypto.sha256("abc".encodeToByteArray()).toHex())
        // 56 байт: граница, на которой длина уходит в следующий блок
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            SectionCrypto.sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".encodeToByteArray()).toHex())
    }

    @Test
    fun hmacAndHkdfKnownAnswers() {
        // RFC 4231, case 1
        assertEquals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7",
            SectionCrypto.hmacSha256(ByteArray(20) { 0x0b }, "Hi There".encodeToByteArray()).toHex())
        // RFC 5869, test case 1 (первые 32 байта OKM)
        val okm = SectionCrypto.hkdfSha256(ByteArray(22) { 0x0b }, hex("000102030405060708090a0b0c"), hex("f0f1f2f3f4f5f6f7f8f9"))
        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf", okm.toHex())
    }

    @Test
    fun chacha20Poly1305Rfc8439() {
        val key = ByteArray(32) { (0x80 + it).toByte() }
        val nonce = hex("070000004041424344454647")
        val aad = hex("50515253c0c1c2c3c4c5c6c7")
        val plain = ("Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.").encodeToByteArray()
        val sealed = SectionCrypto.chacha20Poly1305Seal(key, nonce, aad, plain)
        assertEquals("d31a8d34648e60db7b86afbc53ef7ec2", sealed.copyOfRange(0, 16).toHex())
        assertEquals("1ae10b594f09e26a7e902ecbd0600691", sealed.copyOfRange(sealed.size - 16, sealed.size).toHex())
        assertContentEquals(plain, SectionCrypto.chacha20Poly1305Open(key, nonce, aad, sealed))
        val bad = sealed.copyOf().also { it[3] = (it[3].toInt() xor 1).toByte() }
        assertNull(SectionCrypto.chacha20Poly1305Open(key, nonce, aad, bad))
        assertNull(SectionCrypto.chacha20Poly1305Open(key, nonce, aad + 1, sealed))
    }

    // Конверт собран серверной реализацией (python cryptography): ключ 00..1f, nonce 64..6f, версия 7.
    private val serverBlob = hex(
        "4c443201000000076465666768696a6b6c6d6e6fd684ad061dec3b07c017aefa180d4c82bebdb62f5200758daea790540ec3e4aaa281c95f6ec455447a981e7a16b74deca1d244bf8b",
    )
    private val testKeys: (Int) -> ByteArray? = { if (it == 1) ByteArray(32) { i -> i.toByte() } else null }
    private val sectionId = "11111111-2222-3333-4444-555555555555"

    @Test
    fun opensEnvelopeBuiltByServer() {
        assertEquals(7, SectionSeal.versionOf(serverBlob))
        assertEquals("{\"id\":\"x\",\"name\":\"Инзер\",\"n\":42}", SectionSeal.open(serverBlob, sectionId, testKeys))
    }

    @Test
    fun rejectsWrongSectionTamperingUnknownKeyAndGarbage() {
        assertFailsWith<SectionSeal.SealException> { SectionSeal.open(serverBlob, "other-id", testKeys) }
        val bad = serverBlob.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.open(bad, sectionId, testKeys) }
        val badVersion = serverBlob.copyOf().also { it[7] = 8 }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.open(badVersion, sectionId, testKeys) }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.open(serverBlob, sectionId) { null } }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.open("{\"plain\":true}".encodeToByteArray(), sectionId, testKeys) }
        assertNull(SectionSeal.versionOf(ByteArray(5)))
    }

    @Test
    fun productionKeyIsPresentAndNotTheTestKey() {
        val k = SectionSealKeys.forKid(1)!!
        assertEquals(32, k.size)
        assertEquals(false, k.contentEquals(ByteArray(32) { it.toByte() }))
        assertNull(SectionSealKeys.forKid(2))
    }
}
