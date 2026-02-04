package com.z_company.work_manager
// SyncWorker.kt
// Описание: Класс Worker для выполнения фоновой синхронизации данных. Вызывается периодически WorkManager.
// Использует CoroutineWorker для асинхронного выполнения. Инжектирует SyncManager и SharedPreferencesRepositories через Koin.
// Перед синхронизацией проверяет наличие токена авторизации (bearerToken) из SecureDataStore.
// Если токен есть, выполняет syncToRemote и syncFromRemote последовательно.
// При успешной полной синхронизации (оба метода успешны) обновляет timestamp в SharedPreferences.
// Если токена нет или ошибка - retry.

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.domain.repositories.SharedPreferencesRepositories
import com.z_company.repository.SecureDataStore
import com.z_company.repository.remote_rest.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncWorker(
    val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val syncManager: SyncManager by inject()
    private val sharedPrefs: SharedPreferencesRepositories by inject()

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val application = appContext.applicationContext as Application

                // Добавлено: Проверка наличия токена авторизации
                // Для чего: Чтобы убедиться, что пользователь авторизован перед синхронизацией. Если токена нет - прерываем с retry
                val token = SecureDataStore.getAuthBearerTokenFlow(application).first()
                if (token.isNullOrBlank()) {
                    // Нет токена - пользователь не авторизован, повторяем позже
                    return@withContext Result.retry()
                }
                val bearerToken = "Bearer $token"

                // Выполняем upload (sync to remote)
                val uploadResult = syncManager.syncToRemote(bearerToken).first { it !is ResultState.Loading } // Ждем завершения (Success или Error)
                Log.d("zzz", "uploadResult $uploadResult")
                if (uploadResult is ResultState.Error) {
                    return@withContext Result.retry()
                }

                // обновляем timestamp
                if (uploadResult is ResultState.Success) {
                    sharedPrefs.setLastSyncTimestamp(System.currentTimeMillis())
                    Result.success()
                } else {
                    Result.retry()
                }
            } catch (e: Exception) {
                // Обработка исключений - повторить
                Result.retry()
            }
        }
    }
}