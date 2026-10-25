package com.z_company.loco_driver

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.z_company.repository.remote_rest.tracks.AndroidDeviceKeys
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/** Ключ устройства в настоящем Keystore: цепочка аттестации с нашим challenge и расшифровка конверта, завёрнутого как на сервере. */
@RunWith(AndroidJUnit4::class)
class AndroidDeviceKeysTest {
    private val keys = AndroidDeviceKeys(InstrumentationRegistry.getInstrumentation().targetContext)

    private fun leaf(der: ByteArray) = CertificateFactory.getInstance("X.509").generateCertificate(der.inputStream()) as X509Certificate

    @Test
    fun attestationChainCarriesChallengeAndKeyUnwrapsServerStyleEnvelope() = runBlocking {
        val account = "instrumented-" + System.nanoTime()
        val challenge = ByteArray(16) { (it * 7 + 1).toByte() }
        val chain = keys.attest(account, challenge, forceNew = true)
        assertNotNull("устройство должно уметь аттестацию", chain)
        assertTrue(chain!!.size >= 2)
        val cert = leaf(chain[0])
        val ext = cert.getExtensionValue("1.3.6.1.4.1.11129.2.1.17")
        assertNotNull("в листе нет расширения аттестации", ext)
        // challenge лежит в расширении открытым текстом
        assertTrue(String(ext!!, Charsets.ISO_8859_1).contains(String(challenge, Charsets.ISO_8859_1)))

        // Заворачиваем 32 байта так же, как сервер (RSA-OAEP SHA-256, MGF1-SHA-1), и открываем закрытым ключом из Keystore.
        val secret = ByteArray(32) { (it + 3).toByte() }
        val wrapped = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").run {
            init(Cipher.ENCRYPT_MODE, cert.publicKey, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT))
            doFinal(secret)
        }
        assertArrayEquals(secret, keys.unwrap(account, wrapped))
        // чужой аккаунт открыть не может
        assertNull(keys.unwrap("other-" + account, wrapped))
        // токен устройства хранится по аккаунту
        keys.saveToken(account, "LDT1.x.y", 123L)
        assertEquals("LDT1.x.y" to 123L, keys.loadToken(account))
        keys.clearToken(account)
        assertNull(keys.loadToken(account))
    }
}
