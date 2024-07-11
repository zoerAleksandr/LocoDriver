package com.z_company.use_case

import com.z_company.core.ResultState
import com.z_company.repository.RemoteRouteRepository
import com.z_company.domain.entities.route.Route
import com.z_company.domain.use_cases.RouteUseCase
import com.z_company.entity.Locomotive
import com.z_company.entity_converter.BasicDataConverter
import com.z_company.entity_converter.LocomotiveConverter
import com.z_company.entity_converter.PassengerConverter
import com.z_company.entity_converter.PhotoConverter
import com.z_company.entity_converter.TrainConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RemoteRouteUseCase(private val repository: RemoteRouteRepository) : KoinComponent {
    private val routeUseCase: RouteUseCase by inject()
    suspend fun loadingRoutesFromRemote() = withContext(Dispatchers.IO){
        repository.getAllBasicData().collect { result ->
            if (result is ResultState.Success) {
                val basicDataList = result.data

                basicDataList?.forEach { basicData ->
                    var route = Route()
                    CoroutineScope(Dispatchers.IO).launch {
                        route = route.copy(basicData = BasicDataConverter.toData(basicData))
                        this.launch {
                            repository.loadLocomotiveFromRemote(basicData.id).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data?.let { locomotives ->
                                        route = route.copy(
                                            locomotives = LocomotiveConverter.toDataList(locomotives)
                                        )
                                    }
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            repository.loadTrainFromRemote(basicData.id).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data?.let { trains ->
                                        route = route.copy(
                                            trains = TrainConverter.fromRemoteList(trains)
                                        )
                                    }
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            repository.loadPassengerFromRemote(basicData.id).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data?.let { passengers ->
                                        route = route.copy(
                                            passengers = PassengerConverter.fromRemoteList(
                                                passengers
                                            )
                                        )
                                    }
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            routeUseCase.saveRoute(route).collect {
                                if (it is ResultState.Success) {
                                    this.cancel()
                                }
                            }
                        }.join()

                        this.launch {
                            repository.loadPhotoFromRemote(basicData.id).collect { result ->
                                if (result is ResultState.Success) {
                                    result.data?.let { photos ->
                                        route =
                                            route.copy(photos = PhotoConverter.fromRemoteList(photos))
                                    }
                                    this.cancel()
                                }
                            }
                        }.join()
                    }
                }
            }
        }
    }

    private suspend fun saveBasicData(route: Route) {
        repository.saveRoute(route)
    }

    suspend fun syncBasicData(): Flow<ResultState<Unit>> {
        return repository.synchronizedRouteOneTime()
    }

    suspend fun syncBasicDataPeriodic(): Flow<ResultState<Unit>> {
        return repository.synchronizedRoutePeriodic()
    }

    private suspend fun removeBasicData(remoteObjectId: String) {
        repository.removeBasicData(remoteObjectId)
    }

    suspend fun saveLocomotive(locomotive: Locomotive) {
        repository.saveLocomotive(locomotive)
    }
}