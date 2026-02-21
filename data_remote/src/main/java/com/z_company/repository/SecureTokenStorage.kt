package com.z_company.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Шаг 3 KMP-миграции: абстракция Context из бизнес-логики.
 *
 * Класс-обёртка над SecureDataStore, хранящий Context внутри себя.
 * Регистрируется в Koin с androidContext() — ViewModels и Worker
 * инжектируют этот класс и не нуждаются в Application/Context.
 *
 * В будущем (commonMain) заменяется на expect/actual:
 *   // commonMain
 *   interface TokenStorage {
 *       suspend fun getToken(): String?
 *       suspend fun saveToken(token: String)
 *   }
 *   // androidMain
 *   class AndroidTokenStorage(context: Context) : TokenStorage { ... }
 */
class SecureTokenStorage(private val context: Context) {

    suspend fun saveAuthToken(token: String) =
        SecureDataStore.saveAuthToken(context, token)

    fun getAuthBearerTokenFlow(): Flow<String?> =
        SecureDataStore.getAuthBearerTokenFlow(context)

    suspend fun saveVkId(vkId: String) =
        SecureDataStore.saveVkId(context, vkId)

    fun getVkIdFlow(): Flow<String?> =
        SecureDataStore.getVkIdFlow(context)

    suspend fun saveUserId(userId: String) =
        SecureDataStore.saveUserId(context, userId)

    fun getUserIdFlow(): Flow<String?> =
        SecureDataStore.getUserIdFlow(context)
}
