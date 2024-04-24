package com.z_company.use_case

import android.util.Log
import com.z_company.core.ResultState
import com.z_company.repository.RemoteRouteRepository
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.entity.Locomotive
import com.z_company.entity_converter.BasicDataConverter
import com.z_company.entity_converter.LocomotiveConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteRouteUseCase(private val repository: RemoteRouteRepository) : KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    suspend fun loadingRoutesFromRemote() {
        repository.getAllBasicData().collect { result ->
            if (result is ResultState.Success) {
                Log.d("ZZZ", "list basic data = ${result.data}")
                val basicDataList = result.data
                var route = Route()

                var job: Job? = null
                basicDataList?.forEach { basicData ->
                    job = CoroutineScope(Dispatchers.IO).launch {
                        Log.d("ZZZ", "1 - ${basicData.id}")
                        route = route.copy(basicData = BasicDataConverter.toData(basicData))
                        this.launch {
                            repository.loadLocomotiveFromRemote(basicData.id).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data?.let { locomotives ->
                                        route = route.copy(
                                            locomotives = LocomotiveConverter.toDataList(locomotives)
                                        )
                                    }
                                    Log.d("ZZZ", "2 - ${result.data}")
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            Log.d("ZZZ", "3 - $route")
                            routeUseCase.saveRoute(route).collect { resultSave ->
                                Log.d("ZZZ", "4 - $resultSave")
                            }
                        }.join()
                    }
                    job?.join()
                }
            }
        }
    }

    private suspend fun saveBasicData(route: Route) {
        repository.saveRoute(route)
    }

    fun syncBasicData() {
        repository.synchronizedRoute()
    }

    private suspend fun removeBasicData(remoteObjectId: String) {
        repository.removeBasicData(remoteObjectId)
    }

    suspend fun saveLocomotive(locomotive: Locomotive) {
        repository.saveLocomotive(locomotive)
    }
}