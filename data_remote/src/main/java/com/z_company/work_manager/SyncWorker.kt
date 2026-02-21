package com.z_company.work_manager
// SyncWorker.kt
// Описание: Класс Worker для выполнения фоновой синхронизации данных. Вызывается периодически WorkManager.
// Использует CoroutineWorker для асинхронного выполнения. Инжектирует SyncManager и SharedPreferencesRepositories через Koin.
// Перед синхронизацией проверяет наличие токена авторизации (bearerToken) из SecureTokenStorage.
// Если токен есть, выполняет syncToRemote и syncFromRemote последовательно.
// При успешной полной синхронизации (оба метода успешны) обновляет timestamp в SharedPreferences.
// Если токена нет или ошибка - retry.

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SyncWorker(
    val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val syncManager: SyncManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val secureTokenStorage: SecureTokenStorage by inject()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val userSettings = settingsUseCase.getUserSettingFlow().first()
                if (userSettings.subscriptionPeriod < Calendar.getInstance().timeInMillis) {
                    return@withContext Result.success()
                }

                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) {
                    return@withContext Result.retry()
                }
                val bearerToken = "Bearer $token"

                val uploadResult = syncManager.syncToRemote(bearerToken).first { it !is ResultState.Loading }
                if (uploadResult is ResultState.Success) {
                    sharedPrefs.setLastSyncTimestamp(System.currentTimeMillis())
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}