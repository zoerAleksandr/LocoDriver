package com.z_company.repository.remote_rest.tracks

import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(2 * it, 2 * it + 2).toInt(16).toByte() }

/** Конверт LD3 собран серверной реализацией (python cryptography): RSA-OAEP SHA-256/MGF1-SHA-1, ключ участка 20..3f, nonce 64..6f, версия 7. */
class SectionSealLd3Test {
    private val blob = hex("4c44330000000701002fd6b4b8917b937b3a6326ee8d0c56ba845981b69760eb27d34359c63d9706ef5bafd4010edb32c1d0b3872a715c778fed14808096cbd887843e9a0716e3febdf9eb9b348cd34a3a25d24d1e036c977e159839c8dbc27be3e67b46a0b8d2b93a2eb160bf56bb45e908406c856705ca9c34911dcee0ee3e4ebfcf327aeac803180fcd485047a9be1ba70cdfd5f359467b1b231adb90fe12977c9768f4ece1f3bb7e780e20379dbdc4f005c9bf85020db5e8c5cb0b8ac3720d3dbd3667f2fce83a7380eb1d1fc6e442ce62757ab32df9a97123f2bd4fb5a694b87f0d49bf9c327a016505c739c70a21e40dfe5529d29cba3af6acc93530ab47ef41f18b5a273b0d6465666768696a6b6c6d6e6fff89915be79c7af2e486c4345d871002cb5a4e02c84eb4f023de820059d21e7d71de8c23856fe9ef54854b7851d4deeed6211ed427")
    private val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(hex("308204bd020100300d06092a864886f70d0101010500048204a7308204a30201000282010100b48ccdf2542b079bd806cbbe61aba01cfaedcb65bf55e49fbbd4bfa40c406a0c852dd98ca3fd7ca990b47c61486bad33e6c9d76110943ad894b251a83cd54961a6a64e78039876dd9b672a23abd0961f8a2b61ad4e6d6ee44aafd908aaa5aee13bc6461068956ac8b99d52456618bbfb06eb0161ed484e209edc7a163da9f41853413f2fe4d6f7d020cfaa78d0e38675381543bfb00e3338935e1e59d99b8edd87bd677ecd08edd232bfe80f80d04cefa0104bc08c62bdef718343cd7daf99ae5f02c4462c723ad65141de8da3946325d626da944df58e5a93bffc47081f40c52d3de34e5be61fec70353e070223b7c60e6675b5b86de5ffbb51c712a61e8ddb0203010001028201003838a642c0e9db473098412689f46ef9c73b77fc397dee20bd550ccdf110b7d5293d734263a86202cd301e910e83951e15ce2156b4da6b8e989304716e6bfe1b3dd1089b3f8760f035dd84963200d45682e86a549498e810879f456678f8f9c16223261b22edf0200c088c9f56a0fa1c3b75bf6af67feef02277c63e99ba0beb67307608cf662df4d2bee579da2f1a03e7bc85cafbb0c33d795828b7a34938899f17c985b34f7400ffb5df5ca1a236bebce8229a49308f1a082893fb371b4157c75ef267a5be9ac2dcc9378d9cdc8404c7d8b286e443bd6a2f9e030fa4fb754bac895b4ba26835f8033a7aea7ff58275b3fc500eb6f4baf4c6d894d79168225102818100e0867fe62068dfd41262819515016a1474a417b84f3bcda6f5639a4eca5bfe0e125fe4186c16a8aeb8437ae4b919d2a5ca4c01fb6eca87eb9714890179e33cb0d57d9cbbd1fb47da1054ccac2a65de3fb4b04f5c56b87a6abf9bc8b499175ce8ac5edb111b30a78d94fa2a20221d2432219cda9ff6053081aa8dff0e4605bd8b02818100cddc2b42bfe2017e10663eb6b75fc59eeaf6d1da7025e643e4119bf965a180879bd577bd739779d9cd6e683b181ca274ca9c2efeb40c4172b946bae65e99f2a9de9cbf3d20cd15d3ef6fc409dfaafc7b2758bf05068a86595a11bd7880fe3d6d3fc8bfc86afaac4863095fff19944de0e11a2a5ab5a6ec8e5826874e0d451af102818029414d7af17593bb19edb3b5baf8598f8ca7221ca1a271753e1da11add1fee1c93cfbecfa5fcbbddeec9a7692381d52cd57105e437454b984458679c37d3433192e8d3c5c71d8ee4489fec43ae3ead89b31fba555331ee7c72dca3125c16e8e16fdab611fc97021287cf37b253d25af6432ad9190f5180ca329688c90ca0bd6502818100ab505d2408614192b27d1415a456ec33cb1faa16199c409b21125275e2d0db107f6bcf60f6f008f46b54452360767befaba8fc26f498fa6249b385d82ec3f8c0cac43115e2d529241d2f2aba78cb679fe9679e57c550c46b595c480c1f74f49395303001ce9d5b804602c84397fae6308dc3e3996770498e4fe788543910d851028180581a00c7e6f2e996ad8e0fff04832398dfb3ad6ca509aa25e99559fed653768bd65b0a502e822d23fb73079e15bac1e248d05e2237f7e682ee2fbf7c2a99fc00dc98f52160614be83123f4ae689cf27351f6877f7209028a07592ea0107d158ca1670a34921881212f60e7f62a6b78e89dccb74b5c284fc969ba40d1d3995944")))
    private val sectionId = "11111111-2222-3333-4444-555555555555"

    private fun unwrap(wrapped: ByteArray): ByteArray? = runCatching {
        Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run {
            init(Cipher.DECRYPT_MODE, privateKey, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT))
            doFinal(wrapped)
        }
    }.getOrNull()

    @Test
    fun opensEnvelopeWrappedByServer() {
        assertTrue(SectionSeal.isLd3(blob))
        assertEquals("{\"id\":\"x\",\"name\":\"Инзер\",\"n\":42}", SectionSeal.openLd3(blob, sectionId, ::unwrap))
    }

    @Test
    fun rejectsOtherSectionTamperingAndForeignKey() {
        assertFailsWith<SectionSeal.SealException> { SectionSeal.openLd3(blob, "other", ::unwrap) }
        val bad = blob.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 1).toByte() }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.openLd3(bad, sectionId, ::unwrap) }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.openLd3(blob, sectionId) { null } }
        val otherVersion = blob.copyOf().also { it[6] = 8 }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.openLd3(otherVersion, sectionId, ::unwrap) }
        assertFailsWith<SectionSeal.SealException> { SectionSeal.openLd3("LD2xxxxxxxxxxxxxxxxxxxxxxxxxxxxx".encodeToByteArray(), sectionId, ::unwrap) }
    }

    @Test
    fun errorCodeIsReadFromStringDetailToo() {
        // Эндпоинты советника отдают {"detail":"device_required"}; общий парсер читает только detail.code
        assertEquals("device_required", advisorStringDetail("{\"detail\":\"device_required\"}"))
        assertEquals("challenge_mismatch", advisorStringDetail("{\"detail\":\"challenge_mismatch\"}"))
        assertEquals(null, advisorStringDetail("{\"detail\":{\"code\":\"x\"}}"))
        assertEquals(null, advisorStringDetail("not json"))
        assertEquals(null, advisorStringDetail(null))
    }
}
