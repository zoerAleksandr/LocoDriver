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
    private const val TAG = "SecureDataStore"  // Изменено: Добавил константу для тега логирования.
    // Для чего: Чтобы унифицировать логи и упростить отладку во всех методах.

    private const val DATASTORE_NAME = "secure_datastore"
    private const val KEYSET_NAME = "secure_keyset"
    private const val MASTER_KEY_ALIAS = "master_key"  // Изменено: Изменил MASTER_KEY_URI на MASTER_KEY_ALIAS, вынес в const.
    // Для чего: Чтобы использовать в deleteMasterKey и избежать хардкода. URI теперь строится динамически.

    // Ключи для хранения (пример для токена)
    private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")

    // Чтобы хранить зашифрованный VK ID пользователя для определения, привязан ли VK аккаунт, и для получения данных пользователя из VKID SDK.
    private val VK_ID_KEY = stringPreferencesKey("vk_id")

    // Добавляем ключ для userId (комментарий на русском: Новый ключ для хранения userId как строки)
    private val USER_ID_KEY = stringPreferencesKey("user_id")

    // Изменено: Добавил новый метод getAeadWithRetry с обработкой InvalidKeyException.
    // Для чего: Чтобы централизованно обрабатывать случаи, когда ключ повреждён или отсутствует — удаляем его и retry генерацию. Это решает InvalidKeyException во всех методах, избегая дублирования кода. Возвращает Aead? (nullable), чтобы caller мог обработать null (например, emit null в Flow).
    private fun getAeadWithRetry(context: Context): Aead? {
        return try {
            AeadConfig.register()  // Регистрация конфигурации Tink
            AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, DATASTORE_NAME)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri("android-keystore://$MASTER_KEY_ALIAS")
                .build()
                .keysetHandle
                .getPrimitive(Aead::class.java)
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "InvalidKeyException: Keystore cannot load master_key. Deleting and retrying.", e)
            deleteMasterKey()  // Удаляем повреждённый ключ
            // Retry: Пытаемся заново получить Aead после удаления
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
                Log.e(TAG, "Retry failed after deleting master key: ${retryException.message}", retryException)
                null  // Если retry провалился, возвращаем null
            }
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "GeneralSecurityException in getAeadWithRetry: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception in getAeadWithRetry: ${e.message}", e)
            null
        }
    }

    // Изменено: Добавил новый метод deleteMasterKey().
    // Для чего: Чтобы удалить повреждённый master_key из Android Keystore при InvalidKeyException. Это позволяет заново сгенерировать ключ и избежать краша. Стандартный workaround из Tink issues (#535) и Stripe (#444).
    private fun deleteMasterKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(MASTER_KEY_ALIAS)
            Log.d(TAG, "Deleted corrupted master key: $MASTER_KEY_ALIAS")
        } catch (e: KeyStoreException) {
            Log.e(TAG, "KeyStoreException while deleting master key: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete master key: ${e.message}", e)
        }
    }

    // Расширение для DataStore
    private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)

    // Метод для сохранения (комментарий на русском: Сохраняем userId зашифрованно, аналогично token)
    suspend fun saveUserId(context: Context, userId: String) {
        val aead = getAeadWithRetry(context) ?: return  // Изменено: Заменил getAead на getAeadWithRetry, обработка null (не сохраняем, если ключ недоступен).
        // Для чего: Чтобы метод не крашился на InvalidKeyException — если retry failed, просто выходим (или можно добавить log/error handling).
        val encryptedUserId = aead.encrypt(userId.toByteArray(), null)
        context.secureDataStore.edit { preferences ->
            preferences[USER_ID_KEY] = encryptedUserId.toString(Charsets.ISO_8859_1)
        }
    }

    // Метод для получения (комментарий на русском: Получаем userId как Flow<String?>)
    fun getUserIdFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context) ?: return@map null  // Изменено: Заменил getAead на getAeadWithRetry, emit null если ключ недоступен.
            // Для чего: Чтобы Flow не крашился — если InvalidKeyException и retry failed, возвращаем null, что соответствует случаю "данные не доступны".
            preferences[USER_ID_KEY]?.let { encrypted ->
                try {
                    val decryptedBytes = aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed in getUserIdFlow: ${e.message}", e)
                    null  // Обработка ошибок дешифрования
                }
            }
        }
    }

    // Для чего: Аналогично saveAuthToken, но для VK ID, чтобы securely хранить индикатор привязки VK.
    suspend fun saveVkId(context: Context, vkId: String) {
        val aead = getAeadWithRetry(context) ?: return  // Изменено: Заменил getAead на getAeadWithRetry, обработка null.
        // Для чего: Аналогично saveUserId — предотвращаем краш, выходим если ключ недоступен.
        val encryptedVkId = aead.encrypt(vkId.toByteArray(), null) // Шифрование
        context.secureDataStore.edit { preferences ->
            preferences[VK_ID_KEY] = encryptedVkId.toString(Charsets.ISO_8859_1) // Хранение как строки
        }
    }

    // Для чего: Аналогично getAuthTokenFlow, чтобы отслеживать наличие VK ID и загружать данные пользователя из VKID.
    fun getVkIdFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context) ?: return@map null  // Изменено: Заменил getAead на getAeadWithRetry, emit null если ключ недоступен.
            // Для чего: Аналогично getUserIdFlow — безопасная обработка без краша.
            preferences[VK_ID_KEY]?.let { encrypted ->
                try {
                    val decryptedBytes = aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed in getVkIdFlow: ${e.message}", e)
                    null // Обработка ошибок дешифрования
                }
            }
        }
    }

    /**
     * Сохраняет зашифрованный токен аутентификации.
     * @param context Контекст приложения.
     * @param token Значение токена.
     */
    suspend fun saveAuthToken(context: Context, token: String) {
        val aead = getAeadWithRetry(context) ?: return  // Изменено: Заменил getAead на getAeadWithRetry, обработка null.
        // Для чего: Предотвращаем краш, если ключ повреждён — не сохраняем, но приложение не падает.
        val encryptedToken = aead.encrypt(token.toByteArray(), null) // Шифрование (associated data = null)
        context.secureDataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = encryptedToken.toString(Charsets.ISO_8859_1) // Хранение как строки
        }
    }

    /**
     * Получает поток с дешифрованным токеном аутентификации.
     * @param context Контекст приложения.
     * @return Flow<String?> - токен или null, если не сохранён.
     */
    fun getAuthBearerTokenFlow(context: Context): Flow<String?> {
        return context.secureDataStore.data.map { preferences ->
            val aead = getAeadWithRetry(context) ?: return@map null  // Изменено: Заменил getAead на getAeadWithRetry, emit null если ключ недоступен.
            // Для чего: Основной метод, где крашился код — теперь безопасно возвращает null, если retry failed, что соответствует вашему вопросу (Flow эмитирует null при проблемах с ключом).
            preferences[AUTH_TOKEN_KEY]?.let { encrypted ->
                try {
                    val decryptedBytes = aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    Log.e(TAG, "Decryption failed in getAuthBearerTokenFlow: ${e.message}", e)
                    null // Обработка ошибок дешифрования
                }
            }
        }
    }
}