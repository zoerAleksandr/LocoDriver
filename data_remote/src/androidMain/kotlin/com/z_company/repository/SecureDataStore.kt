package com.z_company.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.z_company.core.sendToSentry
import com.z_company.core.sendMessageToSentry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import android.util.Log

/**
 * Утилитный класс для безопасного хранения данных с использованием Jetpack DataStore и шифрования через Google Tink.
 * Описание: Инициализирует DataStore для хранения preferences. Использует Tink для шифрования/дешифрования значений с помощью Android Keystore.
 * Это альтернатива устаревшему EncryptedSharedPreferences.
 */
object SecureDataStore {
    private const val TAG = "SecureDataStore"

    private const val DATASTORE_NAME = "secure_datastore"
    private const val KEYSET_NAME = "secure_keyset"
    private const val MASTER_KEY_ALIAS = "master_key"

    // Зашифрованные ключи (Tink)
    private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    private val VK_ID_KEY = stringPreferencesKey("vk_id")
    private val USER_ID_KEY = stringPreferencesKey("user_id")

    // Plaintext fallback-ключи — используются только когда Tink полностью недоступен.
    // Данные в app-private DataStore (недоступны другим приложениям без root).
    // Активируются при невосстановимом сбое Android Keystore на конкретном устройстве.
    private val AUTH_TOKEN_FALLBACK_KEY = stringPreferencesKey("auth_token_plain")
    private val VK_ID_FALLBACK_KEY = stringPreferencesKey("vk_id_plain")
    private val USER_ID_FALLBACK_KEY = stringPreferencesKey("user_id_plain")

    // Кеш Aead — создаётся один раз, переиспользуется для всех операций.
    // Исправляет: гонку при конкурентных вызовах, когда два coroutine независимо
    // удаляли и пересоздавали ключ, получая разные Aead и не могли расшифровать
    // данные друг друга.
    @Volatile
    private var aeadCache: Aead? = null

    // Thread-safe получение Aead с double-checked locking.
    // Кеш инвалидируется в deleteMasterKey перед ротацией ключа.
    private fun getAeadWithRetry(context: Context): Aead? {
        aeadCache?.let { return it }
        return synchronized(this) {
            aeadCache?.let { return it }
            val result = buildAead(context)
            aeadCache = result
            result
        }
    }

    private fun buildAead(context: Context): Aead? {
        return try {
            AeadConfig.register()
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, DATASTORE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        } catch (e: InvalidKeyException) {
            // Ожидаемо: бэкап/восстановление, смена биометрии, Android 16+.
            // Удаляем повреждённый ключ и пересоздаём — не засоряем Sentry.
            Log.e(TAG, "InvalidKeyException: Keystore cannot load master_key. Deleting and retrying.", e)
            deleteMasterKey(context)
            try {
                AeadConfig.register()
                AndroidKeysetManager.Builder()
                    .withSharedPref(context, KEYSET_NAME, DATASTORE_NAME)
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
                    .build()
                    .keysetHandle
                    .getPrimitive(Aead::class.java)
            } catch (retryException: Exception) {
                // Retry тоже не удался — переходим на plaintext fallback.
                Log.e(TAG, "Tink retry failed — will use plaintext fallback: ${retryException.message}", retryException)
                retryException.sendToSentry("SecureDataStore", "buildAead.retry")
                null
            }
        } catch (e: GeneralSecurityException) {
            // Ожидаемо при ротации ключа — не засоряем Sentry.
            Log.e(TAG, "GeneralSecurityException in buildAead: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in buildAead: ${e.message}", e)
            e.sendToSentry("SecureDataStore", "buildAead")
            null
        }
    }

    // Удаляет повреждённый master_key из Android Keystore и сбрасывает кеш Aead.
    // Это позволяет retry сгенерировать свежий keyset с новым master key.
    // Стандартный workaround из Tink issues (#535) и Stripe (#444).
    private fun deleteMasterKey(context: Context) {
        // Инвалидируем кеш — после удаления ключа старый Aead бесполезен
        aeadCache = null
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
            Log.d(TAG, "Deleted corrupted master key: $MASTER_KEY_ALIAS")
        } catch (e: KeyStoreException) {
            Log.e(TAG, "KeyStoreException while deleting master key: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete master key: ${e.message}", e)
        }
        // Старый keyset зашифрован удалённым ключом — расшифровать невозможно.
        // Удаляем его, чтобы retry создал свежий keyset с новым master key,
        // иначе retry тоже завершится ошибкой (GeneralSecurityException).
        try {
            context.getSharedPreferences(DATASTORE_NAME, android.content.Context.MODE_PRIVATE)
                .edit().remove(KEYSET_NAME).apply()
            Log.d(TAG, "Cleared corrupted keyset from SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear keyset from SharedPreferences: ${e.message}", e)
        }
    }

    // Расширение для DataStore
    private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)

    suspend fun saveAuthToken(context: Context, token: String) {
        val aead = getAeadWithRetry(context)
        if (aead == null) {
            Log.w(TAG, "Tink unavailable — saving auth token as plaintext fallback")
            sendMessageToSentry("SecureDataStore: Tink unavailable on saveAuthToken, using plaintext fallback")
            context.secureDataStore.edit { preferences ->
                preferences[AUTH_TOKEN_FALLBACK_KEY] = token
            }
            return
        }
        val encryptedToken = aead.encrypt(token.toByteArray(), null)
        context.secureDataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = encryptedToken.toString(Charsets.ISO_8859_1)
            // Очищаем fallback если Tink работает
            preferences.remove(AUTH_TOKEN_FALLBACK_KEY)
        }
    }

    fun getAuthBearerTokenFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context)
            if (aead != null) {
                preferences[AUTH_TOKEN_KEY]?.let { encrypted ->
                    try {
                        String(aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null))
                    } catch (e: Exception) {
                        // Ожидаемо после ротации ключа — старые данные недешифруемы новым ключом.
                        Log.e(TAG, "Decryption failed in getAuthBearerTokenFlow, checking fallback: ${e.message}")
                        preferences[AUTH_TOKEN_FALLBACK_KEY]
                    }
                } ?: preferences[AUTH_TOKEN_FALLBACK_KEY]
            } else {
                // Tink недоступен — читаем из plaintext fallback
                preferences[AUTH_TOKEN_FALLBACK_KEY]
            }
        }
    }

    suspend fun saveVkId(context: Context, vkId: String) {
        val aead = getAeadWithRetry(context)
        if (aead == null) {
            Log.w(TAG, "Tink unavailable — saving vkId as plaintext fallback")
            sendMessageToSentry("SecureDataStore: Tink unavailable on saveVkId, using plaintext fallback")
            context.secureDataStore.edit { preferences ->
                preferences[VK_ID_FALLBACK_KEY] = vkId
            }
            return
        }
        val encryptedVkId = aead.encrypt(vkId.toByteArray(), null)
        context.secureDataStore.edit { preferences ->
            preferences[VK_ID_KEY] = encryptedVkId.toString(Charsets.ISO_8859_1)
            preferences.remove(VK_ID_FALLBACK_KEY)
        }
    }

    fun getVkIdFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context)
            if (aead != null) {
                preferences[VK_ID_KEY]?.let { encrypted ->
                    try {
                        String(aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null))
                    } catch (e: Exception) {
                        Log.e(TAG, "Decryption failed in getVkIdFlow, checking fallback: ${e.message}")
                        preferences[VK_ID_FALLBACK_KEY]
                    }
                } ?: preferences[VK_ID_FALLBACK_KEY]
            } else {
                preferences[VK_ID_FALLBACK_KEY]
            }
        }
    }

    suspend fun saveUserId(context: Context, userId: String) {
        val aead = getAeadWithRetry(context)
        if (aead == null) {
            Log.w(TAG, "Tink unavailable — saving userId as plaintext fallback")
            sendMessageToSentry("SecureDataStore: Tink unavailable on saveUserId, using plaintext fallback")
            context.secureDataStore.edit { preferences ->
                preferences[USER_ID_FALLBACK_KEY] = userId
            }
            return
        }
        val encryptedUserId = aead.encrypt(userId.toByteArray(), null)
        context.secureDataStore.edit { preferences ->
            preferences[USER_ID_KEY] = encryptedUserId.toString(Charsets.ISO_8859_1)
            preferences.remove(USER_ID_FALLBACK_KEY)
        }
    }

    fun getUserIdFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context)
            if (aead != null) {
                preferences[USER_ID_KEY]?.let { encrypted ->
                    try {
                        String(aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null))
                    } catch (e: Exception) {
                        Log.e(TAG, "Decryption failed in getUserIdFlow, checking fallback: ${e.message}")
                        preferences[USER_ID_FALLBACK_KEY]
                    }
                } ?: preferences[USER_ID_FALLBACK_KEY]
            } else {
                preferences[USER_ID_FALLBACK_KEY]
            }
        }
    }
}
