package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.data_local.SharedPreferenceStorage
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.Back4AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SynchronizedWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val back4AppManager: Back4AppManager by inject()
    private val settingsUseCase: SettingsUseCase by inject()
    private val sharedPreferenceStorage: SharedPreferenceStorage by inject()
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            var result: Result = Result.failure()
            this.launch {
                val maxEndTime = sharedPreferenceStorage.getSubscriptionExpiration()
                val currentTimeInMillis = Calendar.getInstance().timeInMillis
                if (maxEndTime >= currentTimeInMillis) {
                    back4AppManager.synchronizedStorage().collect { resultSync ->
                        if (resultSync is ResultState.Success) {
                            result = Result.success()
                            val timestamp = Calendar.getInstance().timeInMillis
                            settingsUseCase.setUpdateAt(timestamp).collect {}
                            this.cancel()
                        }
                        if (resultSync is ResultState.Error) {
                            this.cancel()
                        }
                    }
                }
                else { this.cancel() }

            }.join()
            return@withContext result
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.retry()
        }
    }
}