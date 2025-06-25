package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.repository.Back4AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class LoadRoutesWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
        val back4AppManager: Back4AppManager by inject()
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            back4AppManager.loadRouteListFromRemote().first {
                it is ResultState.Success
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "LoadRoutesWorker ex = ${e.message}")
            return@withContext Result.failure()
        }
    }
}
