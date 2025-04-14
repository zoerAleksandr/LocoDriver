package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.parse.ParseUser
import com.z_company.type_converter.RouteJSONConverter
import com.z_company.work_manager.RouteFieldName.DATA_FIELD_NAME
import com.z_company.work_manager.RouteFieldName.ROUTE_CLASS_NAME_REMOTE
import com.z_company.work_manager.RouteFieldName.USER_EMAIL_FIELD_NAME
import kotlinx.coroutines.coroutineScope
import org.koin.core.component.KoinComponent
import ru.ok.tracer.crash.report.TracerCrashReport
import java.io.NotActiveException
import com.z_company.ParseHelper.saveOrUpdateObjectAsync
import com.z_company.core.ResultState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

const val ROUTE_DATA_INPUT_KEY = "ROUTE_DATA_INPUT_KEY"
const val ROUTE_DATA_OBJECT_ID_KEY = "ROUTE_DATA_OBJECT_ID_KEY"

class SaveRouteWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            var result: Result = Result.failure()
            val currentUser = ParseUser.getCurrentUser()
            val value = inputData.getString(ROUTE_DATA_INPUT_KEY)
            val route = RouteJSONConverter.fromString(value!!)
            Log.d("ZZZ", "in SaveWorker $route")
            this.launch {
                saveOrUpdateObjectAsync(
                    className = ROUTE_CLASS_NAME_REMOTE,
                    uniqueKey = "objectId",
                    uniqueValue = route.basicData.remoteRouteId,
                    fieldsToUpdate = mapOf(
                        Pair(DATA_FIELD_NAME, value),
                        Pair(USER_EMAIL_FIELD_NAME, currentUser.email)
                    )
                ).collect { saveResult ->
                    if (saveResult is ResultState.Success) {
                        val data = workDataOf(ROUTE_DATA_OBJECT_ID_KEY to saveResult.data)
                        result = Result.success(data)
                        Log.d("ZZZ", "success ${saveResult.data}")
                        this.cancel()
                    }
                    if (saveResult is ResultState.Error) {
                        result = Result.failure()
                        Log.d("ZZZ", "failure ${saveResult}")
                        this.cancel()
                    }
                }
            }.join()

            return@coroutineScope result
        } catch (e: Exception) {
            Log.d("ZZZ", "failure ${e.message}")
            val value = inputData.getString(ROUTE_DATA_INPUT_KEY)
            val route = RouteJSONConverter.fromString(value!!)
            TracerCrashReport.report(NotActiveException("$e \n${e.message} \n$route"))
            return@coroutineScope Result.failure()
        }
    }
}