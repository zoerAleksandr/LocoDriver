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

/**
 * Утилитный класс для безопасного хранения данных с использованием Jetpack DataStore и шифрования через Google Tink.
 * Описание: Инициализирует DataStore для хранения preferences. Использует Tink для шифрования/дешифрования значений с помощью Android Keystore.
 * Это альтернатива устаревшему EncryptedSharedPreferences.
 */
object SecureDataStore {

    private const val DATASTORE_NAME = "secure_datastore"
    private const val KEYSET_NAME = "secure_keyset"
    private const val MASTER_KEY_URI = "android-keystore://master_key"  // URI для Keystore

    // Ключи для хранения (пример для токена)
    private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    // Чтобы хранить зашифрованный VK ID пользователя для определения, привязан ли VK аккаунт, и для получения данных пользователя из VKID SDK.
    private val VK_ID_KEY = stringPreferencesKey("vk_id")

    // Инициализация Tink AEAD (Authenticated Encryption with Associated Data)
    private fun getAead(context: Context): Aead {
        AeadConfig.register()  // Регистрация конфигурации Tink

        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, DATASTORE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    // Расширение для DataStore
    private val Context.secureDataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)



    // Для чего: Аналогично saveAuthToken, но для VK ID, чтобы securely хранить индикатор привязки VK.
    suspend fun saveVkId(context: Context, vkId: String) {
        val aead = getAead(context)
        val encryptedVkId = aead.encrypt(vkId.toByteArray(), null)  // Шифрование

        context.secureDataStore.edit { preferences ->
            preferences[VK_ID_KEY] = encryptedVkId.toString(Charsets.ISO_8859_1)  // Хранение как строки
        }
    }

    // Для чего: Аналогично getAuthTokenFlow, чтобы отслеживать наличие VK ID и загружать данные пользователя из VKID.
    fun getVkIdFlow(context: Context): Flow<String?> {
        val aead = getAead(context)
        return context.secureDataStore.data.map { preferences ->
            preferences[VK_ID_KEY]?.let { encrypted ->
                try {
                    val decryptedBytes = aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    null  // Обработка ошибок дешифрования
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
        val aead = getAead(context)
        val encryptedToken = aead.encrypt(token.toByteArray(), null)  // Шифрование (associated data = null)

        context.secureDataStore.edit { preferences ->
            preferences[AUTH_TOKEN_KEY] = encryptedToken.toString(Charsets.ISO_8859_1)  // Хранение как строки
        }
    }

    /**
     * Получает поток с дешифрованным токеном аутентификации.
     * @param context Контекст приложения.
     * @return Flow<String?> - токен или null, если не сохранён.
     */
    fun getAuthTokenFlow(context: Context): Flow<String?> {
        val aead = getAead(context)
        return context.secureDataStore.data.map { preferences ->
            preferences[AUTH_TOKEN_KEY]?.let { encrypted ->
                try {
                    val decryptedBytes = aead.decrypt(encrypted.toByteArray(Charsets.ISO_8859_1), null)
                    String(decryptedBytes)
                } catch (e: Exception) {
                    null  // Обработка ошибок дешифрования
                }
            }
        }
    }
}