package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.z_company.work_manager.LocomotiveFieldName.LOCOMOTIVE_CLASS_NAME_REMOTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val REMOVE_LOCOMOTIVE_OBJECT_ID_KEY = "REMOVE_LOCOMOTIVE_OBJECT_ID_KEY"

class RemoveLocomotiveWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val locomotiveObject = ParseObject(LOCOMOTIVE_CLASS_NAME_REMOTE)
            val data = inputData.getString(REMOVE_LOCOMOTIVE_OBJECT_ID_KEY)
            locomotiveObject.objectId = data
            locomotiveObject.deleteInBackground()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "EX REMOVE_LOCOMOTIVE = ${e.message}")
            return@withContext Result.retry()
        }

    }
}