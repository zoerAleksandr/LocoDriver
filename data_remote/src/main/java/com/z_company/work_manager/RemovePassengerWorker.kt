package com.z_company.work_manager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.z_company.work_manager.PassengerFieldName.PASSENGER_CLASS_NAME_REMOTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val REMOVE_PASSENGER_OBJECT_ID_KEY = "REMOVE_PASSENGER_OBJECT_ID_KEY"


class RemovePassengerWorker (context: Context, parameters: WorkerParameters):
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val passengerObject = ParseObject(PASSENGER_CLASS_NAME_REMOTE)
            val data = inputData.getString(REMOVE_PASSENGER_OBJECT_ID_KEY)
            passengerObject.objectId = data
            passengerObject.deleteInBackground()
            return@withContext Result.success()
        } catch (e: Exception) {
            return@withContext Result.retry()
        }
    }

}