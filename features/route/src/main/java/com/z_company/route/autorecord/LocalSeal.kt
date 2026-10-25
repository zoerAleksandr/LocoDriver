package com.z_company.route.autorecord

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Шифрование файлов кэша участков на устройстве (AES-256-GCM). Ключ создаётся в Android Keystore и не покидает его, поэтому
 * скопированный из памяти приложения/бэкапа файл на другом устройстве (и без приложения) - случайные байты. Формат файла:
 * `"LC1" | iv (12) | ciphertext+tag`. Расшифровка не удалась (старый открытый файл, ключ потерян после сброса экрана
 * блокировки/переустановки) - [open] отдаёт null, кэш просто перекачивается.
 */
class LocalSeal(private val key: () -> SecretKey) {
    fun seal(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return MAGIC + cipher.iv + cipher.doFinal(plain)
    }

    fun open(blob: ByteArray): ByteArray? {
        if (blob.size < MAGIC.size + IV_LEN + 16 || !blob.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
        return runCatching {
            val iv = blob.copyOfRange(MAGIC.size, MAGIC.size + IV_LEN)
            val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
            cipher.doFinal(blob, MAGIC.size + IV_LEN, blob.size - MAGIC.size - IV_LEN)
        }.getOrNull()
    }

    companion object {
        private val MAGIC = byteArrayOf('L'.code.toByte(), 'C'.code.toByte(), '1'.code.toByte())
        private const val IV_LEN = 12
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val ALIAS = "locodriver_section_cache_v1"

        /** Ключ в Android Keystore (создаётся при первом обращении). */
        fun deviceBound(): LocalSeal = LocalSeal {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
                init(
                    KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        }
    }
}
