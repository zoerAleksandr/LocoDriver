package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.parse.ParseObject
import com.parse.ParseQuery
import com.parse.ParseUser
import com.z_company.core.ResultState
import com.z_company.data_remote.B4ARouteRepository
import com.z_company.domain.entities.route.BasicData
import com.z_company.domain.repositories.RemoteRouteRepository
import com.z_company.domain.use_cases.RemoteRouteUseCase
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SyncBasicDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params), KoinComponent {
    val routeUseCase: RouteUseCase by inject()
    val remoteRemoteRouteUseCase: RemoteRouteUseCase by inject()
    override suspend fun doWork(): Result {
        try {
            val parseQuery: ParseQuery<ParseObject> = ParseQuery("BasicData")
            parseQuery.whereEqualTo("user", ParseUser.getCurrentUser())
            parseQuery.orderByDescending("updatedAt")
            val lastElement = parseQuery.find()[0]
            Log.d("ZZZ", "v2 lastElement in remote update = ${lastElement.updatedAt}")

            routeUseCase.listRoutes().onEach { resultState ->
                Log.d("ZZZ", "resultState = $resultState")
                if (resultState is ResultState.Success) {
                    val sortedList = resultState.data.sortedByDescending { route ->
                        route.basicData.updateAt
                    }
                    sortedList.forEach {
                        Log.d("ZZZ", "lastElement in local update =${it.basicData.updateAt}")
                        if (it.basicData.updateAt.after(lastElement.updatedAt)) {
                            Log.d("ZZZ", "basicData update = ${lastElement.updatedAt}")
                            remoteRemoteRouteUseCase.saveBasicData(it.basicData)
                        } else {
                            Log.d("ZZZ", "db synchronized")
                            return@onEach
                        }
                    }
                }
            }.launchIn(CoroutineScope(Dispatchers.IO))
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return Result.retry()
        }
        return Result.success()
    }
}