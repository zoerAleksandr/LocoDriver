package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.data_remote.RemoteRouteRepository
import com.z_company.domain.use_cases.RouteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SynchronizedWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val remoteRepository: RemoteRouteRepository by inject()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val list = routeUseCase.listRouteWithDeleting()

            val isDeletedList = list.filter { route ->
                route.basicData.isDeleted
            }
            isDeletedList.forEach { route ->
                routeUseCase.removeRoute(route).launchIn(this)
                route.basicData.remoteObjectId?.let { remoteId ->
                    remoteRepository.removeBasicData(remoteId)
                }
                route.locomotives.forEach { locomotive ->
                    locomotive.remoteObjectId?.let { remoteId ->
                        remoteRepository.removeLocomotive(remoteId)
                    }
                }
                route.trains.forEach { train ->
                    train.remoteObjectId?.let { remoteId ->
                        remoteRepository.removeTrain(remoteId)
                    }
                }
            }
            val notSynchronizedList = list.filter { route ->
                !route.basicData.isSynchronized
            }
            notSynchronizedList.forEach { route ->
                remoteRepository.saveRoute(route)
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.retry()
        }
    }
}