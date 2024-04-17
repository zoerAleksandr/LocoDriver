package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.z_company.work_manager.BasicDataFieldName.BASIC_DATA_CLASS_NAME_REMOTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val REMOVE_OBJECT_ID_KEY = "REMOVE_OBJECT_ID_KEY"
class RemoveBasicDataWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val basicDataObject = ParseObject(BASIC_DATA_CLASS_NAME_REMOTE)
            val data = inputData.getString(REMOVE_OBJECT_ID_KEY)
            basicDataObject.objectId = data
            basicDataObject.deleteInBackground()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.retry()
        }

    }
}