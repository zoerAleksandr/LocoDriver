package com.z_company.work_manager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.z_company.core.ResultState
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.domain.use_cases.SettingsUseCase
import com.z_company.repository.RemoteRouteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Calendar

class SynchronizedOneTimeWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params),
    KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    private val remoteRepository: RemoteRouteRepository by inject()
    private val settingsUseCase: SettingsUseCase by inject()

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
                route.passengers.forEach { passenger ->
                    passenger.remoteObjectId?.let { remoteId ->
                        remoteRepository.removePassenger(remoteId)
                    }
                }
                route.photos.forEach { photo ->
                    photo.remoteObjectId?.let { remoteId ->
                        remoteRepository.removePhoto(remoteId)
                    }
                }
            }
            val notSynchronizedList = list.filter { route ->
                !route.basicData.isSynchronized
            }
            var timestamp: Long = 0
            var syncRouteCount = 0

            if (notSynchronizedList.isEmpty()) {
                timestamp = Calendar.getInstance().timeInMillis
                CoroutineScope(Dispatchers.IO).launch {
                    settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                }
            } else {
                notSynchronizedList.forEach { route ->
                    this.launch {
                        remoteRepository.saveRoute(route).collect { result ->
                            Log.d("ZZZ", "result in sync $result")
                            if (result is ResultState.Success) {
                                syncRouteCount += 1
                                this.cancel()
                            }
                            if (syncRouteCount == notSynchronizedList.size) {
                                timestamp = Calendar.getInstance().timeInMillis
                                CoroutineScope(Dispatchers.IO).launch {
                                    settingsUseCase.setUpdateAt(timestamp).launchIn(this)
                                }
                            }
                        }
                    }
                }
            }
            return@withContext Result.success()
        } catch (e: Exception) {
            Log.d("ZZZ", "ex sync = ${e.message}")
            return@withContext Result.failure()
        }
    }
}