@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.z_company.work_manager
// SyncWorker.kt
// Описание: Класс Worker для выполнения фоновой синхронизации данных. Вызывается периодически WorkManager.
// Использует CoroutineWorker для асинхронного выполнения. Инжектирует SyncManager и SharedPreferencesRepositories через Koin.
// Перед синхронизацией проверяет наличие токена авторизации (bearerToken) из SecureTokenStorage.
// Если токен есть, выполняет syncToRemote и syncFromRemote последовательно.
// При успешной полной синхронизации (оба метода успешны) обновляет timestamp в SharedPreferences.
// Если токена нет или ошибка - retry.

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.SecureTokenStorage
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

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
                if (userSettings.subscriptionPeriod < Clock.System.now().toEpochMilliseconds()) {
                    return@withContext Result.success()
                }

                val token = secureTokenStorage.getAuthBearerTokenFlow().first()
                if (token.isNullOrBlank()) {
                    return@withContext Result.retry()
                }
                val bearerToken = "Bearer $token"

                // Загружаем все данные на сервер.
                // ВАЖНО: собираем ВЕСЬ flow через lastOrNull() — syncToRemote эмитит
                // промежуточные Success после каждого шага (UserSettings, SalarySetting,
                // маршруты и т.д.). .first() забирал только первый Success и отменял
                // остальные шаги, из-за чего синхронизировался только UserSettings.
                val uploadResult = syncManager.syncToRemote(bearerToken)
                    .filter { it !is ResultState.Loading }
                    .lastOrNull()

                if (uploadResult !is ResultState.Success) {
                    return@withContext Result.retry()
                }

                // Загружаем данные с сервера (обновляем локальную БД).
                // Best-effort: если download не удался — не делаем retry, upload уже выполнен.
                try {
                    syncManager.syncFromRemote(bearerToken)
                        .filter { it !is ResultState.Loading }
                        .lastOrNull()
                } catch (_: Exception) { /* best-effort */ }

                sharedPrefs.setLastSyncTimestamp(Clock.System.now().toEpochMilliseconds())
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
