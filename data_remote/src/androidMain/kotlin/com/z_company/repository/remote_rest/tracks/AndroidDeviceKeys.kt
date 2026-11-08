package com.z_company.repository.remote_rest.tracks

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * [DeviceKeys] на Android Keystore: RSA-2048 для расшифровки (OAEP SHA-256), закрытая часть в TEE/StrongBox и не извлекается.
 * Google Key Attestation намеренно не запрашивается: на части устройств в РФ она даёт ложные отказы.
 * Сертификат используется только для передачи серверу открытого RSA-ключа; закрытая часть остаётся в Keystore.
 * Псевдоним привязан к аккаунту: смена аккаунта на том же телефоне даёт отдельный ключ.
 */
class AndroidDeviceKeys(context: Context) : DeviceKeys {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences("advisor_device", Context.MODE_PRIVATE)

    private fun alias(account: String) = "locodriver_advisor_${account.filter { it.isLetterOrDigit() }.take(40)}_v1"

    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override suspend fun certificateChain(accountId: String, forceNew: Boolean): List<ByteArray>? = runCatching {
        val ks = store()
        val alias = alias(accountId)
        if (forceNew || !ks.containsAlias(alias)) {
            if (ks.containsAlias(alias)) ks.deleteEntry(alias)
            generate(alias, strongBox = true)
        }
        ks.getCertificateChain(alias)?.map { it.encoded }?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun generate(alias: String, strongBox: Boolean) {
        val useStrongBox = strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA1)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
        if (useStrongBox) builder.setIsStrongBoxBacked(true)
        try {
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                .apply { initialize(builder.build()) }
                .generateKeyPair()
        } catch (e: StrongBoxUnavailableException) {
            // Нет отдельного чипа StrongBox - обычное железо (TEE) тоже принимается сервером.
            if (useStrongBox) generate(alias, strongBox = false) else throw e
        }
    }

    override fun unwrap(accountId: String, wrapped: ByteArray): ByteArray? = runCatching {
        val key = store().getKey(alias(accountId), null) as? PrivateKey ?: return null
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT))
        cipher.doFinal(wrapped)
    }.getOrNull()

    override fun loadToken(accountId: String): Pair<String, Long>? {
        val token = prefs.getString("token_${alias(accountId)}", null) ?: return null
        return token to prefs.getLong("exp_${alias(accountId)}", 0L)
    }

    override fun saveToken(accountId: String, token: String, expiresAtMs: Long) {
        prefs.edit().putString("token_${alias(accountId)}", token).putLong("exp_${alias(accountId)}", expiresAtMs).apply()
    }

    override fun clearToken(accountId: String) {
        prefs.edit().remove("token_${alias(accountId)}").remove("exp_${alias(accountId)}").apply()
    }
}
