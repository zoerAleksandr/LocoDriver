package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.repository.Back4AppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SynchronizedWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val back4AppManager: Back4AppManager by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("ZZZ", "doWork")
        try {
            var result: Result = Result.failure()
            this.launch {
                back4AppManager.synchronizedStorage().collect { resultSync ->
                    if (resultSync is ResultState.Success){
                        result = Result.success()
                        this.cancel()
                    }
                    if (resultSync is ResultState.Error){
                        this.cancel()
                    }
                }
            }.join()
            return@withContext result
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.retry()
        }
    }
}