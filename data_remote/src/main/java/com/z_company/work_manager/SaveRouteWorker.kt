package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.parse.ParseObject
import com.parse.ParseRelation
import com.parse.ParseUser
import com.parse.coroutines.suspendSave
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.type_converter.RouteJSONConverter
import com.z_company.work_manager.RouteFieldName.USER_FIELD_NAME
import com.z_company.work_manager.RouteFieldName.DATA_FIELD_NAME
import com.z_company.work_manager.RouteFieldName.ROUTE_CLASS_NAME_REMOTE
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

const val ROUTE_DATA_INPUT_KEY = "ROUTE_DATA_INPUT_KEY"
const val ROUTE_DATA_OBJECT_ID_KEY = "ROUTE_DATA_OBJECT_ID_KEY"

class SaveRouteWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val currentUser = ParseUser.getCurrentUser()
            val value = inputData.getString(ROUTE_DATA_INPUT_KEY)
            val route = RouteJSONConverter.fromString(value!!)
            val routeObject = ParseObject(ROUTE_CLASS_NAME_REMOTE)
            if (!route.basicData.remoteObjectId.isNullOrEmpty()) {
                routeObject.objectId = route.basicData.remoteObjectId
            }
            routeObject.put(DATA_FIELD_NAME, value)

            val relation: ParseRelation<ParseUser> = routeObject.getRelation(USER_FIELD_NAME)
            relation.add(currentUser)

            routeObject.suspendSave()
            Log.d("ZZZ", "objectId ${routeObject.objectId}")

            routeUseCase.setRemoteObjectIdBasicData(route.basicData.id, routeObject.objectId)
                .launchIn(this)

            val data = workDataOf(ROUTE_DATA_OBJECT_ID_KEY to routeObject.objectId)
            return@coroutineScope Result.success(data)
        } catch (e: Exception) {
            Log.d("ZZZ", "failure ${e.message}")
            return@coroutineScope Result.failure()
        }
    }
}