package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.z_company.work_manager.RouteFieldName.ROUTE_CLASS_NAME_REMOTE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val REMOVE_ROUTE_OBJECT_ID_KEY = "REMOVE_ROUTE_OBJECT_ID_KEY"

class RemoveRouteWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val routeObject = ParseObject(ROUTE_CLASS_NAME_REMOTE)
            val data = inputData.getString(REMOVE_ROUTE_OBJECT_ID_KEY)
            routeObject.objectId = data
            routeObject.deleteInBackground()
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex remove route = ${e.message}")
            return@withContext Result.failure()
        }
    }
}