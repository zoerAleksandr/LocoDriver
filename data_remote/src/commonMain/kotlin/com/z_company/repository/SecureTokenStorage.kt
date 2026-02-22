package com.z_company.repository

import kotlinx.coroutines.flow.Flow

/**
 * Шаг 11 KMP-миграции: expect/actual для безопасного хранилища токенов.
 *
 * Android: DataStore + Google Tink (AES256_GCM, AndroidKeyStore)
 * iOS:     Keychain Services (platform.Security)
 *
 * Конструктор намеренно не объявлен в expect — каждая платформа использует
 * свою инициализацию (Android требует Context, iOS — нет).
 */
expect class SecureTokenStorage {
    suspend fun saveAuthToken(token: String)
    fun getAuthBearerTokenFlow(): Flow<String?>

    suspend fun saveVkId(vkId: String)
    fun getVkIdFlow(): Flow<String?>

    suspend fun saveUserId(userId: String)
    fun getUserIdFlow(): Flow<String?>
}
